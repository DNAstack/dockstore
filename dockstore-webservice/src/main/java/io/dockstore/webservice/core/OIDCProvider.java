package io.dockstore.webservice.core;

import java.net.URL;

public class OIDCProvider {
    private URL discoveryURL;

    private String providerName;

    private String clientId;

    private String clientSecret;

    private URL redirectURL;

    private String emailKey;

    private String usernameKey;

    private String nameKey;

    private String avatarKey;

    private String scopes;

    public URL getDiscoveryURL() {
        return discoveryURL;
    }

    public void setDiscoveryURL(URL discoveryURL) {
        this.discoveryURL = discoveryURL;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public URL getRedirectURL() {
        return redirectURL;
    }

    public void setRedirectURL(URL redirectURL) {
        this.redirectURL = redirectURL;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public String getEmailKey() {
        return emailKey;
    }

    public void setEmailKey(String emailKey) {
        this.emailKey = emailKey;
    }

    public String getUsernameKey() {
        return usernameKey;
    }

    public void setUsernameKey(String usernameKey) {
        this.usernameKey = usernameKey;
    }

    public String getNameKey() {
        return nameKey;
    }

    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }

    public String getAvatarKey() {
        return avatarKey;
    }

    public void setAvatarKey(String avatarKey) {
        this.avatarKey = avatarKey;
    }
}
