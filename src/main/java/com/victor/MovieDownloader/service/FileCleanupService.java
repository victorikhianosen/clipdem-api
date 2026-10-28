package com.victor.MovieDownloader.service;

import com.victor.MovieDownloader.entity.DownloadRecord;
import com.victor.MovieDownloader.entity.DownloadStatus;
import com.victor.MovieDownloader.repository.DownloadRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.util.List;

@Service
public class FileCleanupService {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupService.class);

    private final DownloadRecordRepository repository;

    public FileCleanupService(DownloadRecordRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedRate = 30, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    public void removeExpiredDownloads() {
        List<DownloadRecord> expired = repository.findByExpiresAtBeforeAndStatusNot(Instant.now(), DownloadStatus.EXPIRED);
        for (DownloadRecord record : expired) {
            if (record.getFilePath() != null) {
                File file = new File(record.getFilePath());
                if (file.exists() && !file.delete()) {
                    log.warn("Failed to delete expired file for download {}", record.getId());
                }
            }
            record.setStatus(DownloadStatus.EXPIRED);
            record.setFilePath(null);
            repository.save(record);
        }
        if (!expired.isEmpty()) {
            log.info("Cleaned up {} expired download(s)", expired.size());
        }
    }
}
