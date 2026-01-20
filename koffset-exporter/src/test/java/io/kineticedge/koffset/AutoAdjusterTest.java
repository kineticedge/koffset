package io.kineticedge.koffset;


import io.kineticedge.koffset.config.AutoAdjustConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutoAdjusterTest {

    private LagAnalyzer lagAnalyzer;
    private AutoAdjuster autoAdjuster;
    private AutoAdjustConfig config;

    private long clock;

    @BeforeEach
    void setup() {
        clock = System.currentTimeMillis();

        lagAnalyzer = mock(LagAnalyzer.class);
        config = new AutoAdjustConfig();
        config.setEnabled(true);
        config.setSamples(3); // Small for testing
        config.setTolerance(0.10); // 10%
        config.setMinRefreshMs(500L);

        autoAdjuster = new AutoAdjuster(config, lagAnalyzer, () -> clock);
    }

    @Test
    void disabled() {

        LagAnalyzer lagAnalyzer = Mockito.mock(LagAnalyzer.class);

        Mockito.when(lagAnalyzer.getRefreshedAt()).thenReturn(System.currentTimeMillis());


//        Mockito.when(lagAnalyzer.get]]()).thenReturn(System.currentTimeMillis());

        AutoAdjustConfig config = new AutoAdjustConfig(true, 4, 0.01, 1L);
        AutoAdjuster adjuster = new AutoAdjuster(config, lagAnalyzer);

        adjuster.alignToScrape(200);

        //Mockito.verify(lagAnalyzer, never()).reschedule(anyLong(), anyLong());
    }

    @Test
    void shouldRescheduleWhenStableIntervalDetected() throws InterruptedException {

        Mockito.when(lagAnalyzer.getRefreshedAt()).thenReturn(clock);

        clock += 100L;
        autoAdjuster.trackScrapeCadence(); // t=0
        clock += 100L;
        autoAdjuster.trackScrapeCadence(); // Manual simulated time needed in real impl,
        clock += 100L;
        // but here we test the logic trigger
        autoAdjuster.trackScrapeCadence();

        // can we verify/test to what should be supplied
        Mockito.verify(lagAnalyzer).reschedule(anyLong(), anyLong());
    }

    @Test
    void shouldHonorMinimalRefreshRate() {
        // If scrape is every 20ms, but minRefresh is 50ms, target should be 60ms (20 * 3)
        // or whatever multiplier crosses the threshold.

        // This validates your logic:
        // if (detectedIntervalMs < minimalRefreshMs) { multiplier = ... }

        when(lagAnalyzer.getRefreshedAt()).thenReturn(clock);

        clock += 1000L;
        autoAdjuster.alignToScrape(10); // Very fast scrape

        // Should calculate multiplier: ceil(50/10) = 5. Target = 50ms.
        verify(lagAnalyzer).reschedule(anyLong(), anyLong());
    }

    @Test
    void shouldNotRescheduleIfInSafeZone() {
        // Target 10s, current data age 500ms, tolerance 10% (1s).
        // 500ms is < 1s, so it should SKIP rescheduling.

        when(lagAnalyzer.getRefreshedAt()).thenReturn(System.currentTimeMillis() - 100L);

        autoAdjuster.alignToScrape(1000);

        verify(lagAnalyzer, never()).reschedule(anyLong(), anyLong());
    }
}