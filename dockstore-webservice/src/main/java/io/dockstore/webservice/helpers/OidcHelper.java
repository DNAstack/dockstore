package io.dockstore.webservice.helpers;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

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

    private OidcHelper() { }

    public static OIDCEndpoints getOidcEndpoints() {
        if (oidcEndpoints == null) {
            oidcEndpoints =  ResourceUtilities.asString(oidcProvider.getDiscoveryURL().toString(),
                                                              null,
                                                              httpClient)
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
        final AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(BearerToken.authorizationHeaderAccessMethod(), TokenResource.HTTP_TRANSPORT,
                                                                             TokenResource.JSON_FACTORY, new GenericUrl(getOidcEndpoints().getTokenEndpoint()),
                                                                             new ClientParametersAuthentication(oidcProvider.getClientId(), oidcProvider.getClientSecret()), oidcProvider.getClientId(),
                                                                             getOidcEndpoints().getAuthorizationEndpoint().toString()).build();
        try {
            return flow.newTokenRequest(code).setRedirectUri(redirectUri)
                       .setRequestInitializer(request -> request.getHeaders().setAccept("application/json")).execute();
        } catch (IOException e) {
            LOG.error("Retrieving accessToken was unsuccessful", e);
            throw new CustomWebApplicationException("Could not retrieve google token based on code", HttpStatus.SC_BAD_REQUEST);
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
    //    public static void updateUserFromGoogleUserinfoplus(Userinfoplus userinfo, User user) {
    //        User.Profile profile = new User.Profile();
    //        profile.avatarURL = userinfo.getPicture();
    //        profile.email = userinfo.getEmail();
    //        profile.name = userinfo.getName();
    //        profile.username = userinfo.getEmail();
    //        user.setAvatarUrl(userinfo.getPicture());
    //        Map<String, User.Profile> userProfile = user.getUserProfiles();
    //        userProfile.put(TokenType.OIDC.toString(), profile);
    //    }
}
