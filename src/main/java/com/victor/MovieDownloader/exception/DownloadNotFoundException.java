package com.victor.MovieDownloader.exception;

public class DownloadNotFoundException extends RuntimeException {
    public DownloadNotFoundException(String message) {
        super(message);
    }
}
