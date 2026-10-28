package com.victor.MovieDownloader.dto;

public class DownloadResponse {

    private String downloadId;
    private String status;
    private String message;
    private String requestedFormatId;
    private boolean audioMerged;
    private boolean qualityDowngraded;

    public DownloadResponse() {
    }

    public DownloadResponse(String downloadId, String status, String message, String requestedFormatId,
                             boolean audioMerged, boolean qualityDowngraded) {
        this.downloadId = downloadId;
        this.status = status;
        this.message = message;
        this.requestedFormatId = requestedFormatId;
        this.audioMerged = audioMerged;
        this.qualityDowngraded = qualityDowngraded;
    }

    public String getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(String downloadId) {
        this.downloadId = downloadId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestedFormatId() {
        return requestedFormatId;
    }

    public void setRequestedFormatId(String requestedFormatId) {
        this.requestedFormatId = requestedFormatId;
    }

    public boolean isAudioMerged() {
        return audioMerged;
    }

    public void setAudioMerged(boolean audioMerged) {
        this.audioMerged = audioMerged;
    }

    public boolean isQualityDowngraded() {
        return qualityDowngraded;
    }

    public void setQualityDowngraded(boolean qualityDowngraded) {
        this.qualityDowngraded = qualityDowngraded;
    }
}
