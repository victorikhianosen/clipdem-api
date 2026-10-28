package com.victor.MovieDownloader.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.victor.MovieDownloader.config.DownloaderProperties;
import com.victor.MovieDownloader.exception.VideoExtractionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class YtDlpService {

    private static final Logger log = LoggerFactory.getLogger(YtDlpService.class);

    /**
     * How long a yt-dlp inspection result stays reusable. /api/download re-validates the
     * requested formatId by inspecting the URL again -- without this cache, every download would
     * pay for the full extraction twice back-to-back (it's the slow part; actual byte transfer is
     * already network-bound and can't be sped up further).
     */
    private static final long INSPECT_CACHE_TTL_SECONDS = 120;

    private final DownloaderProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CachedInspection> inspectCache = new ConcurrentHashMap<>();

    public YtDlpService(DownloaderProperties properties) {
        this.properties = properties;
    }

    /**
     * Runs yt-dlp in metadata-only mode (-J, --skip-download) and returns the parsed JSON.
     * Recent results for the same URL are served from a short-lived cache.
     */
    public JsonNode inspect(String url) {
        CachedInspection cached = inspectCache.get(url);
        if (cached != null && !cached.isExpired()) {
            return cached.json;
        }

        List<String> command = List.of(
                properties.getYtDlpPath(),
                "-J",
                "--no-warnings",
                "--skip-download",
                "--no-playlist",
                url
        );
        ProcessResult result = run(command, properties.getInspectTimeoutSeconds());
        if (result.exitCode != 0) {
            throw new VideoExtractionException(classifyFailure(result.stderr));
        }
        try {
            JsonNode parsed = objectMapper.readTree(result.stdout);
            inspectCache.put(url, new CachedInspection(parsed, Instant.now()));
            evictExpired();
            return parsed;
        } catch (IOException e) {
            throw new VideoExtractionException("Failed to parse video metadata", e);
        }
    }

    private void evictExpired() {
        inspectCache.values().removeIf(CachedInspection::isExpired);
    }

    private record CachedInspection(JsonNode json, Instant fetchedAt) {
        boolean isExpired() {
            return Instant.now().isAfter(fetchedAt.plusSeconds(INSPECT_CACHE_TTL_SECONDS));
        }
    }

    private static final Pattern DESTINATION_LINE = Pattern.compile("^\\[download] Destination:");
    private static final Pattern PERCENT_LINE = Pattern.compile("\\[download]\\s+([0-9]{1,3}(?:\\.[0-9]+)?)%");
    private static final Pattern MERGER_LINE = Pattern.compile("^\\[Merger]");

    /**
     * Runs yt-dlp to download/merge the requested format into the given output directory,
     * reporting live progress (0-100) as it goes. The outputTemplate must already be a safe,
     * server-controlled absolute path. {@code expectedStreamCount} is 1 for a single format
     * (already has audio) or 2 for video+audio that will be merged -- yt-dlp downloads each
     * stream as its own independent 0-100% sequence, so this scales them into one overall
     * percentage instead of the bar appearing to restart partway through.
     */
    public ProcessResult download(String url, String formatSelector, String outputTemplate, String ffmpegPath,
                                   int expectedStreamCount, IntConsumer onProgress) {
        List<String> command = new ArrayList<>(List.of(
                properties.getYtDlpPath(),
                "--no-warnings",
                "--no-playlist",
                "--newline",
                "-f", formatSelector,
                "--merge-output-format", "mp4",
                "--ffmpeg-location", ffmpegPath,
                "-o", outputTemplate,
                url
        ));
        return runWithProgress(command, properties.getDownloadTimeoutSeconds(), Math.max(1, expectedStreamCount),
                onProgress);
    }

    private ProcessResult runWithProgress(List<String> command, long timeoutSeconds, int expectedStreamCount,
                                           IntConsumer onProgress) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(false);
            Process process = builder.start();

            Future<String> stderrFuture = executor.submit(() -> readStream(process.getErrorStream()));
            Future<Void> stdoutFuture = executor.submit(() -> {
                parseProgress(process.getInputStream(), expectedStreamCount, onProgress);
                return null;
            });

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new VideoExtractionException("Operation timed out");
            }

            stdoutFuture.get(5, TimeUnit.SECONDS);
            String stderr = stderrFuture.get(5, TimeUnit.SECONDS);
            int exitCode = process.exitValue();

            if (exitCode != 0) {
                log.warn("yt-dlp exited with code {}: {}", exitCode, truncate(stderr));
            } else {
                onProgress.accept(100);
            }

            return new ProcessResult(exitCode, "", stderr);
        } catch (IOException e) {
            throw new VideoExtractionException("Failed to start yt-dlp process. Is it installed and accessible?", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VideoExtractionException("Operation was interrupted", e);
        } catch (Exception e) {
            if (e instanceof VideoExtractionException) {
                throw (VideoExtractionException) e;
            }
            throw new VideoExtractionException("Failed to execute video processing command", e);
        } finally {
            executor.shutdownNow();
        }
    }

    private void parseProgress(InputStream inputStream, int expectedStreamCount, IntConsumer onProgress)
            throws IOException {
        int streamsStarted = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (DESTINATION_LINE.matcher(line).find()) {
                    streamsStarted++;
                    continue;
                }
                if (MERGER_LINE.matcher(line).find()) {
                    onProgress.accept(99);
                    continue;
                }
                Matcher percentMatcher = PERCENT_LINE.matcher(line);
                if (percentMatcher.find() && streamsStarted > 0) {
                    double streamPercent = Double.parseDouble(percentMatcher.group(1));
                    onProgress.accept(scaleStreamProgress(streamsStarted, streamPercent, expectedStreamCount));
                }
            }
        }
    }

    /**
     * Scales one stream's own 0-100% progress into an overall percentage across all expected
     * streams (e.g. stream 1 of 2 at 50% -> 25% overall), capped below 100 so the caller's final
     * onProgress.accept(100) on success is always the one true "fully done" signal.
     */
    static int scaleStreamProgress(int streamsStarted, double streamPercent, int expectedStreamCount) {
        double overall = ((streamsStarted - 1) + streamPercent / 100.0) / expectedStreamCount * 100.0;
        return (int) Math.min(99, Math.round(overall));
    }

    private String classifyFailure(String stderr) {
        String lower = stderr == null ? "" : stderr.toLowerCase();
        if (lower.contains("unsupported url")) {
            return "The provided URL is not supported";
        }
        if (lower.contains("private video")) {
            return "This video is private and cannot be accessed";
        }
        if (lower.contains("video unavailable") || lower.contains("this video is not available")) {
            return "This video is unavailable";
        }
        if (lower.contains("sign in") || lower.contains("age-restricted")) {
            return "This video requires authentication and cannot be accessed";
        }
        if (lower.contains("unable to download webpage") || lower.contains("name or service not known")) {
            return "Unable to reach the source website";
        }
        return "Failed to extract video information from the provided URL";
    }

    private ProcessResult run(List<String> command, long timeoutSeconds) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(false);
            Process process = builder.start();

            Future<String> stdoutFuture = executor.submit(() -> readStream(process.getInputStream()));
            Future<String> stderrFuture = executor.submit(() -> readStream(process.getErrorStream()));

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new VideoExtractionException("Operation timed out");
            }

            String stdout = stdoutFuture.get(5, TimeUnit.SECONDS);
            String stderr = stderrFuture.get(5, TimeUnit.SECONDS);
            int exitCode = process.exitValue();

            if (exitCode != 0) {
                log.warn("yt-dlp exited with code {}: {}", exitCode, truncate(stderr));
            }

            return new ProcessResult(exitCode, stdout, stderr);
        } catch (IOException e) {
            throw new VideoExtractionException("Failed to start yt-dlp process. Is it installed and accessible?", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VideoExtractionException("Operation was interrupted", e);
        } catch (Exception e) {
            if (e instanceof VideoExtractionException) {
                throw (VideoExtractionException) e;
            }
            throw new VideoExtractionException("Failed to execute video processing command", e);
        } finally {
            executor.shutdownNow();
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 500 ? s.substring(0, 500) : s;
    }

    public record ProcessResult(int exitCode, String stdout, String stderr) {
    }
}
