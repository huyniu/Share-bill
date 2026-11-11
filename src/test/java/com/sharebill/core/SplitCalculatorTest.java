package com.sharebill.core;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SplitCalculatorTest {

    @Test
    void equalSplitDividesEvenly() {
        assertEquals(Map.of(1L, 300_000L, 2L, 300_000L, 3L, 300_000L, 4L, 300_000L),
                SplitCalculator.equal(1_200_000, List.of(1L, 2L, 3L, 4L)));
    }

    @Test
    void equalSplitDistributesRemainderSoTotalIsExact() {
        Map<Long, Long> r = SplitCalculator.equal(100_000, List.of(1L, 2L, 3L));
        assertEquals(100_000L, r.values().stream().mapToLong(Long::longValue).sum());
        assertEquals(33_334L, r.get(1L));
        assertEquals(33_333L, r.get(2L));
        assertEquals(33_333L, r.get(3L));
    }

    @Test
    void sharesSplitFollowsWeights() {
        Map<Long, Long> weights = new LinkedHashMap<>();
        weights.put(1L, 2L); // uống bia, chịu 2 phần
        weights.put(2L, 1L);
        weights.put(3L, 1L);
        Map<Long, Long> r = SplitCalculator.byShares(400_000, weights);
        assertEquals(200_000L, r.get(1L));
        assertEquals(100_000L, r.get(2L));
        assertEquals(100_000L, r.get(3L));
    }

    @Test
    void sharesSplitWithRemainderStillSumsToTotal() {
        Map<Long, Long> weights = new LinkedHashMap<>();
        weights.put(1L, 1L);
        weights.put(2L, 2L);
        weights.put(3L, 4L);
        Map<Long, Long> r = SplitCalculator.byShares(100, weights);
        assertEquals(100L, r.values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    void exactSplitMustMatchTotal() {
        assertThrows(IllegalArgumentException.class,
                () -> SplitCalculator.exact(100, Map.of(1L, 30L, 2L, 60L)));
        assertEquals(Map.of(1L, 40L, 2L, 60L), SplitCalculator.exact(100, Map.of(1L, 40L, 2L, 60L)));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> SplitCalculator.equal(0, List.of(1L)));
        assertThrows(IllegalArgumentException.class, () -> SplitCalculator.equal(100, List.of()));
        assertThrows(IllegalArgumentException.class, () -> SplitCalculator.byShares(100, Map.of(1L, 0L)));
    }
}
