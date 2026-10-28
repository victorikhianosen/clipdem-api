package com.victor.MovieDownloader.dto;

public class DownloadStatusResponse {

    private String downloadId;
    private String status;
    private String fileName;
    private Long fileSizeBytes;
    private boolean audioMerged;
    private boolean qualityDowngraded;
    private String errorMessage;
    private boolean fileReady;
    private Integer progressPercent;

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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isFileReady() {
        return fileReady;
    }

    public void setFileReady(boolean fileReady) {
        this.fileReady = fileReady;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }
}
