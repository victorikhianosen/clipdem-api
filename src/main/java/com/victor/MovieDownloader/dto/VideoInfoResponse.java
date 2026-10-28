package com.victor.MovieDownloader.dto;

import java.util.List;

public class VideoInfoResponse {

    private String title;
    private String thumbnail;
    private Double durationSeconds;
    private String sourceWebsite;
    private String canonicalUrl;
    private List<VideoFormatDto> formats;

    public VideoInfoResponse() {
    }

    public VideoInfoResponse(String title, String thumbnail, Double durationSeconds, String sourceWebsite,
                              String canonicalUrl, List<VideoFormatDto> formats) {
        this.title = title;
        this.thumbnail = thumbnail;
        this.durationSeconds = durationSeconds;
        this.sourceWebsite = sourceWebsite;
        this.canonicalUrl = canonicalUrl;
        this.formats = formats;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getSourceWebsite() {
        return sourceWebsite;
    }

    public void setSourceWebsite(String sourceWebsite) {
        this.sourceWebsite = sourceWebsite;
    }

    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    public void setCanonicalUrl(String canonicalUrl) {
        this.canonicalUrl = canonicalUrl;
    }

    public List<VideoFormatDto> getFormats() {
        return formats;
    }

    public void setFormats(List<VideoFormatDto> formats) {
        this.formats = formats;
    }
}
