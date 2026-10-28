package com.victor.MovieDownloader.dto;

import jakarta.validation.constraints.NotBlank;

public class DownloadRequest {

    @NotBlank(message = "url must not be blank")
    private String url;

    @NotBlank(message = "formatId must not be blank")
    private String formatId;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFormatId() {
        return formatId;
    }

    public void setFormatId(String formatId) {
        this.formatId = formatId;
    }
}
