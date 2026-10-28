package com.victor.MovieDownloader.repository;

import com.victor.MovieDownloader.entity.DownloadRecord;
import com.victor.MovieDownloader.entity.DownloadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface DownloadRecordRepository extends JpaRepository<DownloadRecord, String> {

    List<DownloadRecord> findByExpiresAtBeforeAndStatusNot(Instant instant, DownloadStatus status);
}
