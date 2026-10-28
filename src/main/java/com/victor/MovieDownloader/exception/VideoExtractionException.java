package com.victor.MovieDownloader.exception;

public class VideoExtractionException extends RuntimeException {
    public VideoExtractionException(String message) {
        super(message);
    }

    public VideoExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
