package com.victor.MovieDownloader.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.victor.MovieDownloader.dto.VideoFormatDto;
import com.victor.MovieDownloader.dto.VideoInfoResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VideoMetadataMapper {

    /** Standard resolution tiers to surface, highest first. Anything else (storyboards, dubs, etc.) is dropped. */
    private static final List<Integer> STANDARD_HEIGHTS = List.of(2160, 1440, 1080, 720, 480, 360, 240, 144);

    public VideoInfoResponse toVideoInfoResponse(JsonNode root) {
        VideoInfoResponse response = new VideoInfoResponse();
        response.setTitle(textOrNull(root, "title"));
        response.setThumbnail(textOrNull(root, "thumbnail"));
        response.setDurationSeconds(root.hasNonNull("duration") ? root.get("duration").asDouble() : null);
        response.setSourceWebsite(textOrNull(root, "extractor_key") != null
                ? textOrNull(root, "extractor_key")
                : textOrNull(root, "extractor"));
        response.setCanonicalUrl(textOrNull(root, "webpage_url"));
        response.setFormats(curateFormats(extractFormats(root)));
        return response;
    }

    /**
     * Collapses yt-dlp's raw format list (which includes storyboards, dubbed-audio tracks, and
     * several near-duplicate entries per resolution) down to a single best video format per
     * standard resolution tier, highest resolution first.
     */
    public List<VideoFormatDto> curateFormats(List<VideoFormatDto> all) {
        Map<Integer, VideoFormatDto> bestByTier = new LinkedHashMap<>();

        for (VideoFormatDto format : all) {
            Integer tier = qualityTier(format);
            if (!format.isHasVideo() || tier == null || !STANDARD_HEIGHTS.contains(tier)) {
                continue;
            }

            VideoFormatDto current = bestByTier.get(tier);
            if (current == null || isBetter(format, current)) {
                bestByTier.put(tier, format);
            }
        }

        List<VideoFormatDto> curated = new ArrayList<>(bestByTier.values());
        curated.sort(Comparator.comparing(this::qualityTier).reversed());
        return curated;
    }

    /**
     * The quality tier a format belongs to, matched against {@link #STANDARD_HEIGHTS}. For
     * landscape video this is just the height; for portrait video (Reels/Shorts/TikTok, where
     * yt-dlp reports height as the long side, e.g. 1280 for a 720x1280 clip) it's the shorter of
     * width/height, which is what the "720p"/"1080p" label actually refers to either way.
     */
    private Integer qualityTier(VideoFormatDto format) {
        Integer height = format.getHeight();
        Integer width = format.getWidth();
        if (height == null) {
            return null;
        }
        if (width == null) {
            return height;
        }
        return Math.min(height, width);
    }

    private boolean isBetter(VideoFormatDto candidate, VideoFormatDto current) {
        boolean candidateMp4 = "mp4".equalsIgnoreCase(candidate.getExtension());
        boolean currentMp4 = "mp4".equalsIgnoreCase(current.getExtension());
        if (candidateMp4 != currentMp4) {
            return candidateMp4;
        }

        long candidateSize = candidate.getFileSizeBytes() == null ? -1 : candidate.getFileSizeBytes();
        long currentSize = current.getFileSizeBytes() == null ? -1 : current.getFileSizeBytes();
        return candidateSize > currentSize;
    }

    public List<VideoFormatDto> extractFormats(JsonNode root) {
        List<VideoFormatDto> formats = new ArrayList<>();
        JsonNode formatsNode = root.get("formats");
        if (formatsNode == null || !formatsNode.isArray()) {
            return formats;
        }

        for (JsonNode f : formatsNode) {
            String formatId = textOrNull(f, "format_id");
            if (formatId == null) {
                continue;
            }
            String ext = textOrNull(f, "ext");
            Integer height = f.hasNonNull("height") ? f.get("height").asInt() : null;
            Integer width = f.hasNonNull("width") ? f.get("width").asInt() : null;
            Double fps = f.hasNonNull("fps") ? f.get("fps").asDouble() : null;

            Long fileSize = f.hasNonNull("filesize") ? f.get("filesize").asLong() : null;
            boolean estimated = false;
            if (fileSize == null && f.hasNonNull("filesize_approx")) {
                fileSize = f.get("filesize_approx").asLong();
                estimated = true;
            }

            String vcodec = textOrNull(f, "vcodec");
            String acodec = textOrNull(f, "acodec");
            boolean hasVideo = vcodec != null && !vcodec.equalsIgnoreCase("none");
            boolean hasAudio = acodec != null && !acodec.equalsIgnoreCase("none");

            String resolution;
            if (!hasVideo) {
                resolution = "audio only";
            } else if (height != null && width != null) {
                resolution = width + "x" + height;
            } else {
                resolution = textOrNull(f, "resolution");
            }

            String note = textOrNull(f, "format_note");

            formats.add(new VideoFormatDto(formatId, ext, resolution, height, width, fps, fileSize, estimated,
                    hasAudio, hasVideo, note));
        }
        return formats;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }
}
