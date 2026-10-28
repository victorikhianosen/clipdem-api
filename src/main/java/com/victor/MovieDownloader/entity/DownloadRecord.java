package com.victor.MovieDownloader.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "download_records")
public class DownloadRecord {

    @Id
    @Column(length = 36)
    private String id;

    @Column(length = 2048, nullable = false)
    private String sourceUrl;

    @Column(length = 64, nullable = false)
    private String requestedFormatId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DownloadStatus status;

    @Column
    private String filePath;

    @Column
    private String fileName;

    @Column
    private Long fileSizeBytes;

    @Column
    private boolean audioMerged;

    @Column
    private boolean qualityDowngraded;

    @Lob
    @Column(length = 4000)
    private String errorMessage;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant completedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getRequestedFormatId() {
        return requestedFormatId;
    }

    public void setRequestedFormatId(String requestedFormatId) {
        this.requestedFormatId = requestedFormatId;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public void setStatus(DownloadStatus status) {
        this.status = status;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
