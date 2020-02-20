package io.dockstore.webservice.helpers;

import java.net.URL;

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
}
