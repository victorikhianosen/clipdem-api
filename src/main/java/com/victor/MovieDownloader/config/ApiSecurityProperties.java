package com.victor.MovieDownloader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api-security")
public class ApiSecurityProperties {

    /** Shared secret that trusted clients (e.g. the Next.js server) must send in the X-API-Key header. */
    private String apiKey = "";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
