package io.kineticedge.koffset;


import io.kineticedge.koffset.config.AutoAdjustConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

        final long intervalMs = 100L;

        AutoAdjustConfig config = new AutoAdjustConfig(false, 3, 0.10, intervalMs);
        AutoAdjuster adjuster = new AutoAdjuster(config, lagAnalyzer, () -> clock);

        Mockito.when(lagAnalyzer.getRefreshedAt()).thenReturn(clock);
        clock += intervalMs;
        adjuster.trackScrapeCadence();
        clock += intervalMs;
        adjuster.trackScrapeCadence();
        clock += intervalMs;
        // but here we test the logic trigger
        adjuster.trackScrapeCadence();

        Mockito.verify(lagAnalyzer, never()).reschedule(anyLong(), anyLong());
    }

    @ParameterizedTest
    @CsvSource({
            "100, 500, 500", // expect 100 x 5 (multiplier) to get 500
            "100, 505, 600", // expect 100 x 6 (multiplier) to get 600 which is the first multiplier > than 505
            "100, 50, 100",  // expect 100, since it is already > than minimalRefreshMs.
            "101, 102, 202"  // expect 101 x 2 (multiplier) to get 202
    })
    void reschedule(long intervalMs, long minRefreshMs, long expected) throws InterruptedException {

        config.setMinRefreshMs(minRefreshMs);

        Mockito.when(lagAnalyzer.getRefreshedAt()).thenReturn(clock);

        clock += intervalMs;
        autoAdjuster.trackScrapeCadence(); // t=0
        clock += intervalMs;
        autoAdjuster.trackScrapeCadence(); // Manual simulated time needed in real impl,
        clock += intervalMs;
        // but here we test the logic trigger
        autoAdjuster.trackScrapeCadence();

        // can we verify/test to what should be supplied
        Mockito.verify(lagAnalyzer).reschedule(longThat(
                targetIntervaMs -> {
                    Assertions.assertEquals(expected, targetIntervaMs);
                    return targetIntervaMs > 0;
                }
        ), longThat(
                delayMs -> {
                    Assertions.assertEquals(expected, delayMs);
                    return delayMs > 0;
                }
        ));
    }

    @ParameterizedTest
    @CsvSource({
            "101, 0.10, 500, 100, true",
            "33, 0.10, 500, 100, false",
            "100, 0.10, 1000, 100, false",
            "101, 0.10, 1000, 100, true"
    })
    void rescheduleAndSafetyZone(long dataAge, double tolerance, long detectedInterval, long lastRefreshDuration, boolean reschedule) {
        // Target 10s, current data age 500ms, tolerance 10% (1s).
        // 500ms is < 1s, so it should SKIP rescheduling.

        Mockito.when(lagAnalyzer.lastRefreshDurationMs()).thenReturn(lastRefreshDuration);

        when(lagAnalyzer.getRefreshedAt()).thenReturn(clock - dataAge);

        config.setTolerance(tolerance);
        autoAdjuster.alignToScrape(detectedInterval);

        verify(lagAnalyzer, times(reschedule ? 1 : 0)).reschedule(anyLong(), anyLong());
    }


//    @Test
//    void shouldAdjustIfDataIsTooOld() {
//
//        config.setTolerance(0.10);
//
//        // Data age is 150ms (Older than max age of 100ms)
//        when(lagAnalyzer.getRefreshedAt()).thenReturn(clock - 150L);
//
//        autoAdjuster.alignToScrape(1000);
//
//        verify(lagAnalyzer).reschedule(eq(1000L), anyLong());
//    }
//
//
//    @Test
//    void shouldCapSafetyMarginBasedOnTolerance() {
//        // targetInterval = 1000, tolerance = 0.10 (100ms)
//        // maxSafetyMargin = 50ms (0.5 * 100ms)
//        config.setTolerance(0.10);
//
//        // Even if refresh takes 400ms (suggesting 440ms margin), it must be capped at 50ms
//        when(lagAnalyzer.lastRefreshDurationMs()).thenReturn(400L);
//        when(lagAnalyzer.getRefreshedAt()).thenReturn(clock - 40L); // Age = 40 (inside 50..100?) No.
//
//        autoAdjuster.alignToScrape(1000);
//
//        // Verify reschedule happened because age 40 < safetyMargin 50
//        verify(lagAnalyzer).reschedule(eq(1000L), anyLong());
//    }
//
//    @ParameterizedTest
//    @CsvSource({
//            "100, 500, 500", // expect 100 x 5 (multiplier) to get 500
//            "100, 505, 600", // expect 100 x 6 (multiplier) to get 600 which is the first multiplier > than 505
//            "100, 50, 100",  // expect 100, since it is already > than minimalRefreshMs.
//            "101, 102, 202"  // expect 101 x 2 (multiplier) to get 202
//    })
//    void rescheduleNoRes(long intervalMs, long minRefreshMs, long expected) throws InterruptedException {
//
//        config.setTolerance(1);
//        config.setMinRefreshMs(minRefreshMs);
//
//
//        //TBD
//        Mockito.when(lagAnalyzer.lastRefreshDurationMs()).thenReturn(123L);
//
//
//        autoAdjuster.trackScrapeCadence(); // t=0
//        clock += intervalMs;
//        autoAdjuster.trackScrapeCadence(); // Manual simulated time needed in real impl,
//        clock += intervalMs;
//
//        Mockito.when(lagAnalyzer.getRefreshedAt()).thenReturn(clock - 5);
//
//        // but here we test the logic trigger
//        autoAdjuster.trackScrapeCadence();
//
////        // can we verify/test to what should be supplied
////        Mockito.verify(lagAnalyzer).reschedule(longThat(
////                targetIntervaMs -> {
////                    Assertions.assertEquals(expected, targetIntervaMs);
////                    return targetIntervaMs > 0;
////                }
////        ), longThat(
////                delayMs -> {
////                    Assertions.assertEquals(expected, delayMs);
////                    return delayMs > 0;
////                }
////        ));
//    }
//
//    @Test
//    void shouldHonorMinimalRefreshRate() {
//        // If scrape is every 20ms, but minRefresh is 50ms, target should be 60ms (20 * 3)
//        // or whatever multiplier crosses the threshold.
//
//        // This validates your logic:
//        // if (detectedIntervalMs < minimalRefreshMs) { multiplier = ... }
//
//        when(lagAnalyzer.getRefreshedAt()).thenReturn(clock);
//
//        clock += 1000L;
//        autoAdjuster.alignToScrape(10); // Very fast scrape
//
//        // Should calculate multiplier: ceil(50/10) = 5. Target = 50ms.
//        verify(lagAnalyzer).reschedule(anyLong(), anyLong());
//    }
//
////    @Test
////    void shouldVerifySafetyMarginCapping() {
////        // interval = 60,000ms (1 min)
////        // ratio = 0.05 (5%) -> calculated would be 3000ms
////        // cap = 500ms
////        config.setSafetyMarginRatio(0.05);
////        config.setMaxSafetyMarginMs(500L);
////
////        // In your test, you can now use a custom cap to see if the math holds
////        config.setMaxSafetyMarginMs(2000L);
////        // Now safetyMargin will be 2000ms instead of 500ms.
////    }
////
////    @Test
////    void shouldNotRescheduleIfInSafeZone() {
////        // Target 1000ms.
////        // Safety Margin (5%) = 50ms.
////        // Max Age (10%) = 100ms.
////        config.setSafetyMarginRatio(0.05);
////        config.setTolerance(0.10);
////
////        // Data age is 75ms (exactly in the middle of 50ms and 100ms)
////        when(lagAnalyzer.getRefreshedAt()).thenReturn(clock - 75L);
////
////        autoAdjuster.alignToScrape(1000);
////
////        verify(lagAnalyzer, never()).reschedule(anyLong(), anyLong());
////    }

}