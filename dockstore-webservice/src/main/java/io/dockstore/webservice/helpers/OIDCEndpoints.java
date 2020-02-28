package io.dockstore.webservice.helpers;

import java.net.URL;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

public class OIDCEndpoints {

    @SerializedName("token_endpoint")
    private URL tokenEndpoint;

    @SerializedName("authorization_endpoint")
    private URL authorizationEndpoint;

    @SerializedName("userinfo_endpoint")
    private URL userInfoEndpoint;

    @SerializedName("logout_endpoint")
    private URL logoutEndpoint;

    @SerializedName("jwks_uri")
    private String jwksUri;

    @SerializedName("id_token_signing_alg_values_supported")
    private Set<String> algorithms;

    @SerializedName("issuer")
    private String issuer;

    public URL getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(URL authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public URL getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(URL tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public URL getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    public void setUserInfoEndpoint(URL userInfoEndpoint) {
        this.userInfoEndpoint = userInfoEndpoint;
    }

    public URL getLogoutEndpoint() {
        return logoutEndpoint;
    }

    public void setLogoutEndpoint(URL logoutEndpoint) {
        this.logoutEndpoint = logoutEndpoint;
    }

    public Set<String> getAlgorithms() {
        return algorithms;
    }

    public void setAlgorithms(Set<String> algorithms) {
        this.algorithms = algorithms;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }
}
