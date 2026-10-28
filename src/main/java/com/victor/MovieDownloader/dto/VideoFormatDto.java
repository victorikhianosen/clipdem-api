package com.victor.MovieDownloader.dto;

public class VideoFormatDto {

    private String formatId;
    private String extension;
    private String resolution;
    private Integer height;
    private Integer width;
    private Double fps;
    private Long fileSizeBytes;
    private boolean fileSizeEstimated;
    private boolean hasAudio;
    private boolean hasVideo;
    private String note;

    public VideoFormatDto() {
    }

    public VideoFormatDto(String formatId, String extension, String resolution, Integer height, Integer width,
                           Double fps, Long fileSizeBytes, boolean fileSizeEstimated, boolean hasAudio,
                           boolean hasVideo, String note) {
        this.formatId = formatId;
        this.extension = extension;
        this.resolution = resolution;
        this.height = height;
        this.width = width;
        this.fps = fps;
        this.fileSizeBytes = fileSizeBytes;
        this.fileSizeEstimated = fileSizeEstimated;
        this.hasAudio = hasAudio;
        this.hasVideo = hasVideo;
        this.note = note;
    }

    public String getFormatId() {
        return formatId;
    }

    public void setFormatId(String formatId) {
        this.formatId = formatId;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Double getFps() {
        return fps;
    }

    public void setFps(Double fps) {
        this.fps = fps;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public boolean isFileSizeEstimated() {
        return fileSizeEstimated;
    }

    public void setFileSizeEstimated(boolean fileSizeEstimated) {
        this.fileSizeEstimated = fileSizeEstimated;
    }

    public boolean isHasAudio() {
        return hasAudio;
    }

    public void setHasAudio(boolean hasAudio) {
        this.hasAudio = hasAudio;
    }

    public boolean isHasVideo() {
        return hasVideo;
    }

    public void setHasVideo(boolean hasVideo) {
        this.hasVideo = hasVideo;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
