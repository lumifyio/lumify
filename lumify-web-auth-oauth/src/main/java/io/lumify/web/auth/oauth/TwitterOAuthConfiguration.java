package io.lumify.web.auth.oauth;

import io.lumify.core.config.Configurable;

public class TwitterOAuthConfiguration {
    private String apiKey;
    private String apiSecret;

    public String getApiKey() {
        return apiKey;
    }

    @Configurable(name = "apiKey")
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    @Configurable(name = "apiSecret")
    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }
}
