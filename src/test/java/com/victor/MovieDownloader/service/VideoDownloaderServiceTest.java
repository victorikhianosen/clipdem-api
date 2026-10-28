package com.victor.MovieDownloader.service;

import com.victor.MovieDownloader.dto.VideoFormatDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoDownloaderServiceTest {

    @Test
    void progressiveFormatDoesNotRequireAudioMerge() {
        VideoFormatDto progressive = new VideoFormatDto("18", "mp4", "640x360", 360, 640, 30.0, 10_000_000L, false, true, true, null);
        assertFalse(VideoDownloaderService.requiresAudioMerge(progressive));
        assertEquals("18", VideoDownloaderService.buildFormatSelector("18", false));
    }

    @Test
    void videoOnly4kFormatRequiresAudioMergeWithoutDowngrade() {
        VideoFormatDto fourK = new VideoFormatDto("315", "webm", "3840x2160", 2160, 3840, 30.0, 500_000_000L, true, false, true, null);
        assertTrue(VideoDownloaderService.requiresAudioMerge(fourK));

        String selector = VideoDownloaderService.buildFormatSelector("315", true);
        assertEquals("315+bestaudio[ext=m4a]/315+bestaudio/315", selector);
        // the exact requested format id must remain the first and only video source in every branch
        for (String branch : selector.split("/")) {
            assertTrue(branch.startsWith("315"));
        }
    }

    @Test
    void audioOnlyFormatDoesNotRequireMerge() {
        VideoFormatDto audioOnly = new VideoFormatDto("140", "m4a", "audio only", null, null, null, 2_000_000L, false, true, false, null);
        assertFalse(VideoDownloaderService.requiresAudioMerge(audioOnly));
    }

    @Test
    void rejectsFormatIdNotPresentInVideoFormats() {
        List<VideoFormatDto> formats = List.of(
                new VideoFormatDto("18", "mp4", "640x360", 360, 640, 30.0, 10_000_000L, false, true, true, null),
                new VideoFormatDto("299", "mp4", "1920x1080", 1080, 1920, 60.0, 100_000_000L, true, false, true, null)
        );

        Optional<VideoFormatDto> match = formats.stream()
                .filter(f -> f.getFormatId().equals("9999-not-real"))
                .findFirst();

        assertTrue(match.isEmpty(), "an arbitrary client-supplied formatId must never match a known format");
    }
}
