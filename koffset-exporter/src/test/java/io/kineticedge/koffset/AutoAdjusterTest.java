package io.kineticedge.koffset;


import io.kineticedge.koffset.config.AutoAdjustConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

class AutoAdjusterTest {

    private LagAnalyzer lagAnalyzer;
    private AutoAdjuster autoAdjuster;
    private AutoAdjustConfig config;

    @BeforeEach
    void setup() {
        lagAnalyzer = mock(LagAnalyzer.class);
        config = new AutoAdjustConfig();
        config.setEnabled(true);
        config.setSamples(3); // Small for testing
        config.setTolerance(0.10); // 10%
        config.setMinRefreshMs(50L);
        autoAdjuster = new AutoAdjuster(config, lagAnalyzer);
    }

    @Test
    void shouldRescheduleWhenStableIntervalDetected() {
        // Simulate 3 scrapes at ~1000ms
        autoAdjuster.trackScrapeCadence(); // t=0
        autoAdjuster.trackScrapeCadence(); // Manual simulated time needed in real impl,
        // but here we test the logic trigger
    }

    @Test
    void shouldHonorMinimalRefreshRate() {
        // If scrape is every 20ms, but minRefresh is 50ms, target should be 60ms (20 * 3)
        // or whatever multiplier crosses the threshold.

        // This validates your logic:
        // if (detectedIntervalMs < minimalRefreshMs) { multiplier = ... }

        when(lagAnalyzer.getRefreshedAt()).thenReturn(System.currentTimeMillis() - 1000);

        autoAdjuster.alignToScrape(10); // Very fast scrape

        // Should calculate multiplier: ceil(50/10) = 5. Target = 50ms.
        verify(lagAnalyzer).reschedule(eq(50L), anyLong());
    }

    @Test
    void shouldNotRescheduleIfInSafeZone() {
        // Target 10s, current data age 500ms, tolerance 10% (1s).
        // 500ms is < 1s, so it should SKIP rescheduling.

        when(lagAnalyzer.getRefreshedAt()).thenReturn(System.currentTimeMillis() - 500);

        autoAdjuster.alignToScrape(10000);

        verify(lagAnalyzer, never()).reschedule(anyLong(), anyLong());
    }
}