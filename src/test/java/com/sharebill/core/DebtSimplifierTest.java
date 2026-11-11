package com.sharebill.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebtSimplifierTest {

    @Test
    void exampleFromIdea() {
        // An +500k, Bình +100k, Chi -300k, Dũng -300k
        Map<Long, Long> balances = new LinkedHashMap<>();
        balances.put(1L, 500_000L);
        balances.put(2L, 100_000L);
        balances.put(3L, -300_000L);
        balances.put(4L, -300_000L);

        List<Transfer> transfers = DebtSimplifier.simplify(balances);

        assertEquals(List.of(
                new Transfer(3L, 1L, 300_000),
                new Transfer(4L, 1L, 200_000),
                new Transfer(4L, 2L, 100_000)
        ), transfers);
        assertSettlesEverything(balances, transfers);
    }

    @Test
    void nothingToDoWhenAllSettled() {
        assertTrue(DebtSimplifier.simplify(Map.of(1L, 0L, 2L, 0L)).isEmpty());
    }

    @Test
    void rejectsUnbalancedInput() {
        assertThrows(IllegalArgumentException.class, () -> DebtSimplifier.simplify(Map.of(1L, 10L, 2L, -5L)));
    }

    @Test
    void randomBalancesAlwaysSettleWithAtMostNMinusOneTransfers() {
        Random random = new Random(42);
        for (int round = 0; round < 500; round++) {
            int n = 2 + random.nextInt(12);
            Map<Long, Long> balances = new HashMap<>();
            long sum = 0;
            for (long id = 1; id < n; id++) {
                long b = random.nextInt(2_000_001) - 1_000_000;
                balances.put(id, b);
                sum += b;
            }
            balances.put((long) n, -sum);

            List<Transfer> transfers = DebtSimplifier.simplify(balances);

            assertSettlesEverything(balances, transfers);
            assertTrue(transfers.size() <= n - 1, "Quá nhiều giao dịch: " + transfers.size());
            transfers.forEach(t -> assertTrue(t.amount() > 0));
        }
    }

    private static void assertSettlesEverything(Map<Long, Long> balances, List<Transfer> transfers) {
        Map<Long, Long> remaining = new HashMap<>(balances);
        for (Transfer t : transfers) {
            remaining.merge(t.from(), t.amount(), Long::sum);
            remaining.merge(t.to(), -t.amount(), Long::sum);
        }
        remaining.values().forEach(v -> assertEquals(0L, v));
    }
}
