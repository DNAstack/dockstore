package io.dockstore.webservice.helpers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.GenericUrl;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.dockstore.webservice.CustomWebApplicationException;
import io.dockstore.webservice.core.OIDCProvider;
import io.dockstore.webservice.core.Token;
import io.dockstore.webservice.core.User;
import io.dockstore.webservice.resources.ResourceUtilities;
import io.dockstore.webservice.resources.TokenResource;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OidcHelper {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final Logger LOG = LoggerFactory.getLogger(OidcHelper.class);
    private static HttpClient httpClient;
    private static OIDCProvider oidcProvider;
    private static OIDCEndpoints oidcEndpoints;

    private OidcHelper() {
    }

    public static OIDCEndpoints getOidcEndpoints() {
        if (oidcEndpoints == null) {
            oidcEndpoints = ResourceUtilities.asString(oidcProvider.getDiscoveryURL().toString(), null, httpClient)
                                             .map(json -> GSON.fromJson(json, OIDCEndpoints.class))
                                             .orElseThrow(() -> new OidcClientException(
                                                     "OIDC Discovery endpoint is not available for " + oidcProvider.getProviderName()));
        }
        return oidcEndpoints;
    }

    public static void setHttpClient(HttpClient httpClient) {
        OidcHelper.httpClient = httpClient;
    }

    public static void setOidcProvider(OIDCProvider oidcProvider) {
        OidcHelper.oidcProvider = oidcProvider;
    }

    public static OIDCProvider getOidcProvider() {
        return oidcProvider;
    }

    @Deprecated
    // Returns a User.Profile using values obtained from an OIDC userinfo response.
    private static User.Profile getUserProfileFromUserInfo(Map<String, Object> userInfoMap) {
        String avatar = (String) userInfoMap.get(oidcProvider.getAvatarKey());
        String email = (String) userInfoMap.get(oidcProvider.getEmailKey());
        String name = (String) userInfoMap.get(oidcProvider.getNameKey());
        String username = (String) userInfoMap.get(oidcProvider.getUsernameKey());

        User.Profile userProfile = new User.Profile();
        userProfile.username = username;
        userProfile.name = name;
        userProfile.email = email;
        userProfile.avatarURL = avatar;
        return userProfile;
    }

    public static User.Profile getUserProfile(DecodedJWT jwt) {
        User.Profile userProfile = new User.Profile();
        userProfile.username = jwt.getClaim(oidcProvider.getUsernameKey()).asString();
        userProfile.name = jwt.getClaim(oidcProvider.getNameKey()).asString();
        userProfile.email = jwt.getClaim(oidcProvider.getEmailKey()).asString();
        userProfile.avatarURL = jwt.getClaim(oidcProvider.getAvatarKey()).asString();
        return userProfile;
    }

    //This method will be deleted if the OIDC callback endpoint is deleted.
    @Deprecated
    public static Optional<User.Profile> getUserProfile(String accessToken) {
        TypeToken typeToken = new TypeToken<Map<String, Object>>() {

        };
        try {

            return Optional.of(ResourceUtilities.asString(getOidcEndpoints().getUserInfoEndpoint().toString(),
                                                          accessToken,
                                                          httpClient)
                                                .map(json -> (Map) GSON.fromJson(json, typeToken.getType()))
                                                .map(OidcHelper::getUserProfileFromUserInfo)
                                                .orElseThrow(() -> new OidcClientException(
                                                        "OIDC Userinfo endpoint is not available for " + oidcProvider.getProviderName())));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public static TokenResponse completeAuthorizationCodeFlow(String code, String redirectUri) {
        final AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(BearerToken.authorizationHeaderAccessMethod(),
                                                                             TokenResource.HTTP_TRANSPORT,
                                                                             TokenResource.JSON_FACTORY,
                                                                             new GenericUrl(getOidcEndpoints().getTokenEndpoint()),
                                                                             new ClientParametersAuthentication(
                                                                                     oidcProvider.getClientId(),
                                                                                     oidcProvider.getClientSecret()),
                                                                             oidcProvider.getClientId(),
                                                                             getOidcEndpoints().getAuthorizationEndpoint()
                                                                                               .toString()).build();
        try {
            return flow.newTokenRequest(code)
                       .setRedirectUri(redirectUri)
                       .setRequestInitializer(request -> request.getHeaders().setAccept("application/json"))
                       .execute();
        } catch (IOException e) {
            LOG.error("Retrieving accessToken was unsuccessful", e);
            throw new CustomWebApplicationException("Could not retrieve google token based on code",
                                                    HttpStatus.SC_BAD_REQUEST);
        }
    }

    public static Optional<String> getValidAccessToken(Token tokenRecord) {
        if (tokenRecord.getTokenExpiry().after(Timestamp.from(Instant.now()))) {
            if (tokenRecord.getRefreshToken() != null) {
                //token needs refreshing.
                TokenResponse tokenResponse = new TokenResponse();
                try {
                    tokenResponse.setRefreshToken(tokenRecord.getRefreshToken());
                    GoogleCredential credential = new GoogleCredential.Builder().setTransport(TokenResource.HTTP_TRANSPORT)
                                                                                .setJsonFactory(TokenResource.JSON_FACTORY)
                                                                                .setClientSecrets(oidcProvider.getClientId(),
                                                                                                  oidcProvider.getClientSecret())
                                                                                .setTokenServerUrl(new GenericUrl(
                                                                                        getOidcEndpoints().getTokenEndpoint()))
                                                                                .build()
                                                                                .setFromTokenResponse(tokenResponse);
                    credential.refreshToken();
                    return Optional.ofNullable(credential.getAccessToken());
                } catch (IOException e) {
                    LOG.error("Error refreshing token", e);
                }
            }
            return Optional.empty();
        }
        return Optional.of(tokenRecord.getContent());
    }

    /**
     * Asserts that the given jwt contains ALL the requiredScopes given.
     *
     * @param jwt            The jwt to check
     * @param requiredScopes The requiredScopes that must be present in the JWT.
     * @throws MissingScopesException If the JWT is missing one or more of the required requiredScopes.
     */
    public static void assertScopes(DecodedJWT jwt, String[] requiredScopes) {
        if (requiredScopes == null || requiredScopes.length == 0) {
            return;
        }

        Claim jwtScopes = jwt.getClaim("scp");
        if (jwtScopes.isNull()) {
            jwtScopes = jwt.getClaim("scope");
            if (jwtScopes.isNull()) {
                jwtScopes = jwt.getClaim("scopes");

            }
        }

        if (jwtScopes.isNull()) {
            throw new MissingScopesException("Token is missing required scope");
        }

        String[] jwtScopesArray = jwtScopes.asArray(String.class);
        if (jwtScopesArray == null) {
            jwtScopesArray = jwtScopes.asString().split(" ");
        }

        Set<String> jwtScopesSet = new HashSet<>();
        jwtScopesSet.addAll(Arrays.asList(jwtScopesArray));
        if (!jwtScopesSet.containsAll(Arrays.asList(requiredScopes))) {
            throw new MissingScopesException("Token is missing one or more required requiredScopes");
        }
    }

    /**
     * Asserts that the jwt contains an "aud" claim matching one or more of the required audiences.
     *
     * @param jwt            The JWT to verify.
     * @param validAudiences Array of audiences, at least one of which must match a JWT aud claim.
     * @throws IllegalArgumentException if the JWT does not contain a valid audience.
     */
    public static void assertAudience(DecodedJWT jwt, List<String> validAudiences) {
        if (!jwt.getAudience().stream().anyMatch(validAudiences::contains)) {
            throw new IllegalArgumentException("Token does not contain a valid audience for this service");
        }
    }

    /**
     * Verifies the given JWT token.
     *
     * @param token          The JWT token to verify.
     * @param requiredScopes The scopes required by the JWT.
     * @param validAudiences The audience required in the JWT.  If more than one audience is given, than the JWT is considered valid if any of the audiences match.
     * @throws JWTVerificationException If the token is invalid.
     * @throws JwkException             If there is a problem with the JWKS endpoint (obtained from the discovery endpoint associated with the OIDC Provider)
     */
    public static DecodedJWT verifyAndDecodeJwt(String token, String[] requiredScopes, String[] validAudiences) throws JWTVerificationException, JwkException {
        try {
            DecodedJWT jwt = JWT.decode(token);
            JwkProvider provider = new UrlJwkProvider(new URL(getOidcEndpoints().getJwksUri()));
            Jwk jwk = provider.get(jwt.getKeyId());
            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);

            LOG.warn("DEbug code here needs removal in OidcHelper.java!");
            getOidcEndpoints().setIssuer("https://sts.windows.net/994ece9f-47e6-4cfa-9b64-a6971ea169fa/"); //todo; remove this

            //verifies audience, issuer, signature, and expiry.
            JWTVerifier verifier = JWT.require(algorithm).withIssuer(getOidcEndpoints().getIssuer())
                                      //.withAudience(getOidcProvider().getValidAudiences())
                                      .build(); //Reusable verifier instance
            jwt = verifier.verify(jwt);
            assertAudience(jwt, Arrays.asList(validAudiences));
            assertScopes(jwt, requiredScopes);
            return jwt;
        } catch (MalformedURLException mue) {
            throw new JwkException("OIDC provider discovery endpoint has malformed jwks URI");
        }
    }
}
