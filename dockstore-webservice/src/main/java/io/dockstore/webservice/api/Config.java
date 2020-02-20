package io.dockstore.webservice.api;

import java.lang.reflect.InvocationTargetException;

import io.dockstore.webservice.DockstoreWebserviceConfiguration;
import io.dockstore.webservice.core.OIDCProvider;
import io.dockstore.webservice.helpers.OIDCEndpoints;
import io.dockstore.webservice.helpers.OidcHelper;
import io.swagger.annotations.ApiModel;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.http.client.HttpClient;

@ApiModel(description = "Configuration information for UI clients of the Dockstore webservice.")
public final class Config extends DockstoreWebserviceConfiguration.UIConfig {

    /**
     * Properties that aren't in UIConfig
     */

    private String githubClientId;
    private String quayIoClientId;
    private String bitBucketClientId;
    private String gitlabClientId;
    private String zenodoClientId;
    private String discourseUrl;

    private Boolean autoRegister;
    private OidcProviderConfig oidcProvider;





    private Config() {
    }

    public static Config fromWebConfig(DockstoreWebserviceConfiguration webConfig, HttpClient httpClient)
            throws InvocationTargetException, IllegalAccessException {
        final Config config = new Config();
        config.githubClientId = webConfig.getGithubClientID();
        config.quayIoClientId = webConfig.getQuayClientID();
        config.bitBucketClientId = webConfig.getBitbucketClientID();
        config.gitlabClientId = webConfig.getGitlabClientID();
        config.zenodoClientId = webConfig.getZenodoClientID();
        config.discourseUrl = webConfig.getDiscourseUrl();
        config.autoRegister = webConfig.getAutoRegister();
        config.oidcProvider = new OidcProviderConfig(webConfig.getOidcProvider(), httpClient);
        BeanUtils.copyProperties(config, webConfig.getUiConfig());
        return config;
    }

    public String getGithubClientId() {
        return githubClientId;
    }

    public String getQuayIoClientId() {
        return quayIoClientId;
    }

    public String getBitBucketClientId() {
        return bitBucketClientId;
    }

    public String getGitlabClientId() {
        return gitlabClientId;
    }

    public String getZenodoClientId() {
        return zenodoClientId;
    }


    public String getDiscourseUrl() {
        return discourseUrl;
    }

    public Boolean getAutoRegister() {
        return autoRegister;
    }

    public OidcProviderConfig getOidcProvider() {
        return oidcProvider;
    }

    @ApiModel(description = "Configuration information for an OIDC Provider for UI clients of the Dockstore webservice.")
    public static class OidcProviderConfig {
        private String providerName;
        private String clientId;
        private String authorizationEndpoint;
        private String scope;

        public OidcProviderConfig(String providerName, String clientId, String authorizationEndpoint, String scope) {
            this.providerName = providerName;
            this.clientId = clientId;
            this.authorizationEndpoint = authorizationEndpoint;
            this.scope = scope;
        }

        public OidcProviderConfig(OIDCProvider oidcProvider, HttpClient httpClient) {
            this.providerName = oidcProvider.getProviderName();
            this.clientId = oidcProvider.getClientId();
            this.scope = oidcProvider.getScopes();
            OIDCEndpoints endpoints = OidcHelper.getOidcEndpoints();
            this.authorizationEndpoint = endpoints.getAuthorizationEndpoint().toString();
        }

        public String getClientId() {
            return clientId;
        }

        public String getProviderName() {
            return providerName;
        }

        public String getAuthorizationEndpoint() {
            return authorizationEndpoint;
        }

        public String getScope() {
            return scope;
        }
    }

}
