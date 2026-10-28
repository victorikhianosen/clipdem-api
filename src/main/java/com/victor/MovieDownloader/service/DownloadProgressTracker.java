package com.victor.MovieDownloader.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds live 0-100 progress for in-flight downloads, keyed by downloadId. Intentionally
 * in-memory only (not persisted) -- progress updates arrive many times a second while yt-dlp
 * runs, and writing each tick to MySQL would be wasteful; only the terminal state
 * (COMPLETED/FAILED) is persisted to DownloadRecord.
 */
@Service
public class DownloadProgressTracker {

    private final Map<String, Integer> progressByDownloadId = new ConcurrentHashMap<>();

    public void update(String downloadId, int percent) {
        progressByDownloadId.put(downloadId, percent);
    }

    public Integer get(String downloadId) {
        return progressByDownloadId.get(downloadId);
    }

    public void clear(String downloadId) {
        progressByDownloadId.remove(downloadId);
    }
}
