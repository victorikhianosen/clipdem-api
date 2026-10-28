package com.victor.MovieDownloader.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.victor.MovieDownloader.config.DownloaderProperties;
import com.victor.MovieDownloader.dto.VideoFormatDto;
import com.victor.MovieDownloader.entity.DownloadRecord;
import com.victor.MovieDownloader.entity.DownloadStatus;
import com.victor.MovieDownloader.exception.DownloadNotFoundException;
import com.victor.MovieDownloader.exception.FormatNotFoundException;
import com.victor.MovieDownloader.repository.DownloadRecordRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class VideoDownloaderService {

    private final YtDlpService ytDlpService;
    private final VideoMetadataMapper metadataMapper;
    private final UrlValidationService urlValidationService;
    private final DownloadRecordRepository repository;
    private final DownloaderProperties properties;
    private final DownloadWorker downloadWorker;

    public VideoDownloaderService(YtDlpService ytDlpService,
                                   VideoMetadataMapper metadataMapper,
                                   UrlValidationService urlValidationService,
                                   DownloadRecordRepository repository,
                                   DownloaderProperties properties,
                                   DownloadWorker downloadWorker) {
        this.ytDlpService = ytDlpService;
        this.metadataMapper = metadataMapper;
        this.urlValidationService = urlValidationService;
        this.repository = repository;
        this.properties = properties;
        this.downloadWorker = downloadWorker;
    }

    public com.victor.MovieDownloader.dto.VideoInfoResponse inspect(String url) {
        urlValidationService.validate(url);
        JsonNode root = ytDlpService.inspect(url);
        return metadataMapper.toVideoInfoResponse(root);
    }

    /**
     * Validates the requested format against a fresh inspection of the video (never trusts the
     * client-supplied formatId blindly), creates a PENDING record, and kicks off the async download.
     */
    public DownloadRecord startDownload(String url, String requestedFormatId) {
        urlValidationService.validate(url);

        JsonNode root = ytDlpService.inspect(url);
        List<VideoFormatDto> formats = metadataMapper.extractFormats(root);

        VideoFormatDto selected = formats.stream()
                .filter(f -> f.getFormatId().equals(requestedFormatId))
                .findFirst()
                .orElseThrow(() -> new FormatNotFoundException(
                        "The requested formatId does not belong to this video. Call /api/get-video first."));

        boolean needsAudioMerge = requiresAudioMerge(selected);

        DownloadRecord record = new DownloadRecord();
        record.setId(UUID.randomUUID().toString());
        record.setSourceUrl(url);
        record.setRequestedFormatId(requestedFormatId);
        record.setStatus(DownloadStatus.PENDING);
        record.setAudioMerged(needsAudioMerge);
        record.setQualityDowngraded(false);
        record.setCreatedAt(Instant.now());
        record.setExpiresAt(Instant.now().plus(properties.getFileRetentionHours(), ChronoUnit.HOURS));
        repository.save(record);

        // Delegating to a separate bean is required for @Async to actually take effect --
        // see DownloadWorker's class-level note on self-invocation.
        downloadWorker.processDownload(record.getId(), url, requestedFormatId, needsAudioMerge);

        return record;
    }

    /**
     * Builds the yt-dlp format-selection expression. Video-only formats keep the exact requested
     * video stream and attempt to merge the best available audio, falling back to video-only if no
     * audio track exists at all -- the requested video quality is never silently downgraded.
     */
    static String buildFormatSelector(String formatId, boolean needsAudioMerge) {
        return needsAudioMerge
                ? formatId + "+bestaudio[ext=m4a]/" + formatId + "+bestaudio/" + formatId
                : formatId;
    }

    static boolean requiresAudioMerge(VideoFormatDto format) {
        return format.isHasVideo() && !format.isHasAudio();
    }

    public DownloadRecord getRecord(String downloadId) {
        return repository.findById(downloadId)
                .orElseThrow(() -> new DownloadNotFoundException("No download found with id " + downloadId));
    }

    public File getCompletedFile(String downloadId) {
        DownloadRecord record = getRecord(downloadId);
        if (record.getStatus() != DownloadStatus.COMPLETED || record.getFilePath() == null) {
            throw new DownloadNotFoundException("Download is not ready yet");
        }
        File file = new File(record.getFilePath());
        if (!file.exists()) {
            throw new DownloadNotFoundException("The requested file is no longer available");
        }
        return file;
    }
}
