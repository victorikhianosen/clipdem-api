package com.victor.MovieDownloader.dto;

import jakarta.validation.constraints.NotBlank;

public class VideoInfoRequest {

    @NotBlank(message = "url must not be blank")
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
