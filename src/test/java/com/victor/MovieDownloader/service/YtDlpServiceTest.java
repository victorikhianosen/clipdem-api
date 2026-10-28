package com.victor.MovieDownloader.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YtDlpServiceTest {

    @Test
    void singleStreamProgressMapsDirectly() {
        assertEquals(0, YtDlpService.scaleStreamProgress(1, 0.0, 1));
        assertEquals(50, YtDlpService.scaleStreamProgress(1, 50.0, 1));
        // capped below 100 so the caller's own onProgress.accept(100) is the sole "done" signal
        assertEquals(99, YtDlpService.scaleStreamProgress(1, 100.0, 1));
    }

    @Test
    void twoStreamProgressScalesIntoHalves() {
        // first stream (video) at 0-100% maps to overall 0-50%
        assertEquals(0, YtDlpService.scaleStreamProgress(1, 0.0, 2));
        assertEquals(25, YtDlpService.scaleStreamProgress(1, 50.0, 2));
        assertEquals(49, YtDlpService.scaleStreamProgress(1, 98.0, 2));

        // second stream (audio) at 0-100% maps to overall 50-100%, still capped at 99
        assertEquals(50, YtDlpService.scaleStreamProgress(2, 0.0, 2));
        assertEquals(75, YtDlpService.scaleStreamProgress(2, 50.0, 2));
        assertEquals(99, YtDlpService.scaleStreamProgress(2, 100.0, 2));
    }

    @Test
    void progressNeverDecreasesAcrossAStreamTransition() {
        int endOfStreamOne = YtDlpService.scaleStreamProgress(1, 100.0, 2);
        int startOfStreamTwo = YtDlpService.scaleStreamProgress(2, 0.0, 2);
        assertTrue(startOfStreamTwo >= endOfStreamOne,
                "progress bar must not visibly jump backwards when the audio stream starts");
    }
}
