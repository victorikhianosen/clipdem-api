package com.victor.MovieDownloader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "downloader")
public class DownloaderProperties {

    private String ytDlpPath = "yt-dlp";
    private String ffmpegPath = "ffmpeg";
    private String storageDir = System.getProperty("java.io.tmpdir") + "/movie-downloader";
    private long inspectTimeoutSeconds = 30;
    private long downloadTimeoutSeconds = 1800;
    private long maxFileSizeBytes = 5L * 1024 * 1024 * 1024; // 5GB
    private int maxConcurrentDownloads = 3;
    private long fileRetentionHours = 24;

    public String getYtDlpPath() {
        return ytDlpPath;
    }

    public void setYtDlpPath(String ytDlpPath) {
        this.ytDlpPath = ytDlpPath;
    }

    public String getFfmpegPath() {
        return ffmpegPath;
    }

    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    public String getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
    }

    public long getInspectTimeoutSeconds() {
        return inspectTimeoutSeconds;
    }

    public void setInspectTimeoutSeconds(long inspectTimeoutSeconds) {
        this.inspectTimeoutSeconds = inspectTimeoutSeconds;
    }

    public long getDownloadTimeoutSeconds() {
        return downloadTimeoutSeconds;
    }

    public void setDownloadTimeoutSeconds(long downloadTimeoutSeconds) {
        this.downloadTimeoutSeconds = downloadTimeoutSeconds;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public int getMaxConcurrentDownloads() {
        return maxConcurrentDownloads;
    }

    public void setMaxConcurrentDownloads(int maxConcurrentDownloads) {
        this.maxConcurrentDownloads = maxConcurrentDownloads;
    }

    public long getFileRetentionHours() {
        return fileRetentionHours;
    }

    public void setFileRetentionHours(long fileRetentionHours) {
        this.fileRetentionHours = fileRetentionHours;
    }
}
