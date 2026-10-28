package com.victor.MovieDownloader.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.victor.MovieDownloader.dto.VideoFormatDto;
import com.victor.MovieDownloader.dto.VideoInfoResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoMetadataMapperTest {

    private final VideoMetadataMapper mapper = new VideoMetadataMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SAMPLE_JSON = """
            {
              "title": "Sample Video",
              "thumbnail": "https://example.com/thumb.jpg",
              "duration": 125.5,
              "extractor_key": "Youtube",
              "webpage_url": "https://www.youtube.com/watch?v=abc123",
              "formats": [
                {"format_id": "140", "ext": "m4a", "vcodec": "none", "acodec": "mp4a.40.2", "filesize": 2048000},
                {"format_id": "18", "ext": "mp4", "height": 360, "width": 640, "fps": 30.0, "vcodec": "avc1", "acodec": "mp4a.40.2", "filesize": 10485760},
                {"format_id": "299", "ext": "mp4", "height": 1080, "width": 1920, "fps": 60.0, "vcodec": "avc1", "acodec": "none", "filesize_approx": 104857600},
                {"format_id": "315", "ext": "webm", "height": 2160, "width": 3840, "fps": 30.0, "vcodec": "vp9", "acodec": "none", "filesize_approx": 524288000}
              ]
            }
            """;

    @Test
    void mapsVideoMetadataFields() throws Exception {
        JsonNode root = objectMapper.readTree(SAMPLE_JSON);
        VideoInfoResponse response = mapper.toVideoInfoResponse(root);

        assertEquals("Sample Video", response.getTitle());
        assertEquals("https://example.com/thumb.jpg", response.getThumbnail());
        assertEquals(125.5, response.getDurationSeconds());
        assertEquals("Youtube", response.getSourceWebsite());
        // curated down to the standard-resolution video formats (360p, 1080p, 2160p); the
        // audio-only 140 format is dropped since it has no video stream.
        assertEquals(3, response.getFormats().size());
    }

    @Test
    void identifiesAudioOnlyFormat() throws Exception {
        JsonNode root = objectMapper.readTree(SAMPLE_JSON);
        List<VideoFormatDto> formats = mapper.extractFormats(root);

        VideoFormatDto audioOnly = findFormat(formats, "140");
        assertFalse(audioOnly.isHasVideo());
        assertTrue(audioOnly.isHasAudio());
    }

    @Test
    void identifiesProgressiveFormatWithAudioAndVideo() throws Exception {
        JsonNode root = objectMapper.readTree(SAMPLE_JSON);
        List<VideoFormatDto> formats = mapper.extractFormats(root);

        VideoFormatDto progressive = findFormat(formats, "18");
        assertTrue(progressive.isHasVideo());
        assertTrue(progressive.isHasAudio());
        assertEquals(360, progressive.getHeight());
    }

    @Test
    void identifiesVideoOnly4kFormatRequiringMerge() throws Exception {
        JsonNode root = objectMapper.readTree(SAMPLE_JSON);
        List<VideoFormatDto> formats = mapper.extractFormats(root);

        VideoFormatDto fourK = findFormat(formats, "315");
        assertTrue(fourK.isHasVideo());
        assertFalse(fourK.isHasAudio());
        assertEquals(2160, fourK.getHeight());
        assertTrue(fourK.isFileSizeEstimated());
    }

    @Test
    void curatesOneBestFormatPerStandardResolution() throws Exception {
        JsonNode root = objectMapper.readTree(SAMPLE_JSON);
        VideoInfoResponse response = mapper.toVideoInfoResponse(root);

        // 360p, 1080p, and 2160p are the standard-resolution video formats in SAMPLE_JSON,
        // highest first; the audio-only 140 format is dropped since it has no video stream.
        assertEquals(3, response.getFormats().size());
        assertEquals(2160, response.getFormats().get(0).getHeight());
        assertEquals(1080, response.getFormats().get(1).getHeight());
        assertEquals(360, response.getFormats().get(2).getHeight());
    }

    @Test
    void curationPrefersMp4AndLargerFileSizeAtSameHeight() {
        List<VideoFormatDto> duplicatesAt720p = List.of(
                new VideoFormatDto("232", "mp4", "1280x720", 720, 1280, 30.0, null, false, false, true, null),
                new VideoFormatDto("136", "mp4", "1280x720", 720, 1280, 30.0, 17_000_000L, false, false, true, "720p"),
                new VideoFormatDto("247", "webm", "1280x720", 720, 1280, 30.0, 20_000_000L, false, false, true, "720p")
        );

        List<VideoFormatDto> curated = mapper.curateFormats(duplicatesAt720p);

        assertEquals(1, curated.size());
        assertEquals("136", curated.get(0).getFormatId());
    }

    @Test
    void curatesPortraitFormatsByShorterSide() {
        // Instagram Reels/TikTok/YouTube Shorts report height as the long side for portrait
        // video (e.g. a 720x1280 clip has height=1280), so curation must bucket by the shorter
        // side (720) to land it in the "720p" tier instead of being dropped as height=1280.
        List<VideoFormatDto> portraitFormats = List.of(
                new VideoFormatDto("dash-1", "mp4", "480x854", 854, 480, 30.0, null, false, false, true, null),
                new VideoFormatDto("dash-2", "mp4", "720x1280", 1280, 720, 30.0, null, false, false, true, null)
        );

        List<VideoFormatDto> curated = mapper.curateFormats(portraitFormats);

        assertEquals(2, curated.size());
        assertEquals("dash-2", curated.get(0).getFormatId());
        assertEquals("dash-1", curated.get(1).getFormatId());
    }

    private VideoFormatDto findFormat(List<VideoFormatDto> formats, String id) {
        return formats.stream().filter(f -> f.getFormatId().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("Format " + id + " not found"));
    }
}
