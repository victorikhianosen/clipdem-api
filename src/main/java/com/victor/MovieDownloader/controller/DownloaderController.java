package com.victor.MovieDownloader.controller;

import com.victor.MovieDownloader.dto.ApiResponse;
import com.victor.MovieDownloader.dto.DownloadRequest;
import com.victor.MovieDownloader.dto.DownloadResponse;
import com.victor.MovieDownloader.dto.DownloadStatusResponse;
import com.victor.MovieDownloader.dto.VideoInfoRequest;
import com.victor.MovieDownloader.dto.VideoInfoResponse;
import com.victor.MovieDownloader.entity.DownloadRecord;
import com.victor.MovieDownloader.entity.DownloadStatus;
import com.victor.MovieDownloader.service.DownloadProgressTracker;
import com.victor.MovieDownloader.service.VideoDownloaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
@RequestMapping("/api")
@Tag(name = "Video Downloader", description = "Inspect and download videos via yt-dlp/FFmpeg")
public class DownloaderController {

    private final VideoDownloaderService videoDownloaderService;
    private final DownloadProgressTracker progressTracker;

    public DownloaderController(VideoDownloaderService videoDownloaderService,
                                 DownloadProgressTracker progressTracker) {
        this.videoDownloaderService = videoDownloaderService;
        this.progressTracker = progressTracker;
    }

    @Operation(summary = "Inspect a video URL and list available formats without downloading")
    @PostMapping("/get-video")
    public ResponseEntity<ApiResponse<VideoInfoResponse>> getVideo(@Valid @RequestBody VideoInfoRequest request) {
        VideoInfoResponse info = videoDownloaderService.inspect(request.getUrl());
        return ResponseEntity.ok(ApiResponse.ok("Video information retrieved successfully", info));
    }

    @Operation(summary = "Start downloading a video in the requested format")
    @PostMapping("/download")
    public ResponseEntity<ApiResponse<DownloadResponse>> download(@Valid @RequestBody DownloadRequest request) {
        DownloadRecord record = videoDownloaderService.startDownload(request.getUrl(), request.getFormatId());

        DownloadResponse response = new DownloadResponse(
                record.getId(),
                record.getStatus().name(),
                "Download started. Poll /api/download/" + record.getId() + "/status for progress.",
                record.getRequestedFormatId(),
                record.isAudioMerged(),
                record.isQualityDowngraded());

        return ResponseEntity.accepted().body(ApiResponse.ok("Download accepted", response));
    }

    @Operation(summary = "Check the status of a previously started download")
    @GetMapping("/download/{downloadId}/status")
    public ResponseEntity<ApiResponse<DownloadStatusResponse>> getStatus(@PathVariable String downloadId) {
        DownloadRecord record = videoDownloaderService.getRecord(downloadId);

        DownloadStatusResponse response = new DownloadStatusResponse();
        response.setDownloadId(record.getId());
        response.setStatus(record.getStatus().name());
        response.setFileName(record.getFileName());
        response.setFileSizeBytes(record.getFileSizeBytes());
        response.setAudioMerged(record.isAudioMerged());
        response.setQualityDowngraded(record.isQualityDowngraded());
        response.setErrorMessage(record.getStatus() == DownloadStatus.FAILED ? record.getErrorMessage() : null);
        response.setFileReady(record.getStatus() == DownloadStatus.COMPLETED);

        if (record.getStatus() == DownloadStatus.COMPLETED) {
            response.setProgressPercent(100);
        } else if (record.getStatus() == DownloadStatus.IN_PROGRESS) {
            response.setProgressPercent(progressTracker.get(downloadId));
        }

        return ResponseEntity.ok(ApiResponse.ok("Status retrieved", response));
    }

    @Operation(summary = "Download the completed video file")
    @GetMapping("/download/{downloadId}/file")
    public ResponseEntity<FileSystemResource> getFile(@PathVariable String downloadId) {
        File file = videoDownloaderService.getCompletedFile(downloadId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .contentLength(file.length())
                .body(new FileSystemResource(file));
    }
}
