package com.victor.MovieDownloader.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
@EnableConfigurationProperties({DownloaderProperties.class, ApiSecurityProperties.class})
@EnableAsync
@EnableScheduling
public class AppConfig {

    @Bean(name = "downloadExecutor")
    public Executor downloadExecutor(DownloaderProperties properties) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                properties.getMaxConcurrentDownloads(),
                properties.getMaxConcurrentDownloads(),
                60L, java.util.concurrent.TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
        return executor;
    }

    @Bean
    public FilterRegistrationBean<ApiKeyFilter> apiKeyFilter(ApiSecurityProperties securityProperties) {
        FilterRegistrationBean<ApiKeyFilter> registration =
                new FilterRegistrationBean<>(new ApiKeyFilter(securityProperties.getApiKey()));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}
