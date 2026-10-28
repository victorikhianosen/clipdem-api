package com.victor.MovieDownloader.service;

import com.victor.MovieDownloader.config.DownloaderProperties;
import com.victor.MovieDownloader.entity.DownloadRecord;
import com.victor.MovieDownloader.entity.DownloadStatus;
import com.victor.MovieDownloader.repository.DownloadRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Runs the actual yt-dlp download + FFmpeg merge off the request thread. This must live in its
 * own bean: Spring's @Async is implemented as a proxy around the bean, so a method can only run
 * asynchronously when called from OUTSIDE the bean (e.g. VideoDownloaderService -> DownloadWorker).
 * Calling an @Async method on `this` from within the same class is a self-invocation that bypasses
 * the proxy entirely and runs synchronously, which silently blocked the download API request on the
 * full download+merge duration until this was split out.
 */
@Service
public class DownloadWorker {

    private static final Logger log = LoggerFactory.getLogger(DownloadWorker.class);

    private final YtDlpService ytDlpService;
    private final DownloadRecordRepository repository;
    private final DownloaderProperties properties;
    private final DownloadProgressTracker progressTracker;

    public DownloadWorker(YtDlpService ytDlpService, DownloadRecordRepository repository,
                           DownloaderProperties properties, DownloadProgressTracker progressTracker) {
        this.ytDlpService = ytDlpService;
        this.repository = repository;
        this.properties = properties;
        this.progressTracker = progressTracker;
    }

    @Async("downloadExecutor")
    public void processDownload(String downloadId, String url, String formatId, boolean needsAudioMerge) {
        DownloadRecord record = repository.findById(downloadId).orElse(null);
        if (record == null) {
            log.warn("Download record {} disappeared before processing started", downloadId);
            return;
        }

        try {
            record.setStatus(DownloadStatus.IN_PROGRESS);
            repository.save(record);

            Path storageDir = Path.of(properties.getStorageDir());
            Files.createDirectories(storageDir);

            String formatSelector = VideoDownloaderService.buildFormatSelector(formatId, needsAudioMerge);
            String outputTemplate = storageDir.resolve(downloadId + ".%(ext)s").toString();
            int expectedStreamCount = needsAudioMerge ? 2 : 1;

            YtDlpService.ProcessResult result = ytDlpService.download(
                    url, formatSelector, outputTemplate, properties.getFfmpegPath(), expectedStreamCount,
                    percent -> progressTracker.update(downloadId, percent));

            if (result.exitCode() != 0) {
                record.setStatus(DownloadStatus.FAILED);
                record.setErrorMessage("Download failed. The format may no longer be available.");
                repository.save(record);
                progressTracker.clear(downloadId);
                log.warn("Download {} failed: {}", downloadId, truncate(result.stderr()));
                return;
            }

            File outputFile = findOutputFile(storageDir, downloadId);
            if (outputFile == null) {
                record.setStatus(DownloadStatus.FAILED);
                record.setErrorMessage("Download completed but the output file could not be located.");
                repository.save(record);
                return;
            }

            record.setStatus(DownloadStatus.COMPLETED);
            record.setFilePath(outputFile.getAbsolutePath());
            record.setFileName(outputFile.getName());
            record.setFileSizeBytes(outputFile.length());
            record.setCompletedAt(Instant.now());
            repository.save(record);
            progressTracker.clear(downloadId);
        } catch (Exception e) {
            log.error("Unexpected error processing download {}", downloadId, e);
            record.setStatus(DownloadStatus.FAILED);
            record.setErrorMessage("An unexpected error occurred while processing the download.");
            repository.save(record);
            progressTracker.clear(downloadId);
        }
    }

    private File findOutputFile(Path storageDir, String downloadId) {
        File dir = storageDir.toFile();
        File[] matches = dir.listFiles((d, name) -> name.startsWith(downloadId + "."));
        if (matches == null || matches.length == 0) {
            return null;
        }
        return matches[0];
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
