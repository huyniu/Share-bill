package com.sharebill.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tính phần tiền mỗi người phải chịu cho một khoản chi.
 * <p>
 * Mọi số tiền là số nguyên (đơn vị: đồng) để tránh sai số dấu phẩy động.
 * Khi chia không hết (ví dụ 100.000đ / 3 người), phần dư được phân bổ theo
 * phương pháp "số dư lớn nhất" nên tổng các phần luôn đúng bằng tổng hóa đơn.
 */
public final class SplitCalculator {

    private SplitCalculator() {
    }

    /** Chia đều {@code total} cho các thành viên (giữ nguyên thứ tự). */
    public static Map<Long, Long> equal(long total, Collection<Long> memberIds) {
        Map<Long, Long> weights = new LinkedHashMap<>();
        for (Long id : memberIds) {
            weights.put(id, 1L);
        }
        return byShares(total, weights);
    }

    /** Chia {@code total} theo số phần của từng thành viên. */
    public static Map<Long, Long> byShares(long total, Map<Long, Long> weights) {
        requirePositive(total);
        if (weights.isEmpty()) {
            throw new IllegalArgumentException("Cần ít nhất một người để chia tiền");
        }
        long sumWeights = 0;
        for (Map.Entry<Long, Long> e : weights.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) {
                throw new IllegalArgumentException("Số phần của thành viên " + e.getKey() + " phải lớn hơn 0");
            }
            sumWeights = Math.addExact(sumWeights, e.getValue());
        }

        Map<Long, Long> result = new LinkedHashMap<>();
        List<long[]> remainders = new ArrayList<>(); // [index, remainder]
        List<Long> ids = new ArrayList<>(weights.keySet());
        long allocated = 0;
        for (int i = 0; i < ids.size(); i++) {
            long scaled = Math.multiplyExact(total, weights.get(ids.get(i)));
            long base = scaled / sumWeights;
            result.put(ids.get(i), base);
            allocated += base;
            remainders.add(new long[]{i, scaled % sumWeights});
        }

        // Phân bổ phần dư (từng đồng) cho người có số dư lớn nhất; hòa thì ưu tiên người đứng trước.
        remainders.sort((a, b) -> a[1] != b[1] ? Long.compare(b[1], a[1]) : Long.compare(a[0], b[0]));
        long leftover = total - allocated;
        for (int k = 0; k < leftover; k++) {
            Long id = ids.get((int) remainders.get(k)[0]);
            result.merge(id, 1L, Long::sum);
        }
        return result;
    }

    /** Mỗi người chịu đúng số tiền đã nhập; tổng phải khớp với {@code total}. */
    public static Map<Long, Long> exact(long total, Map<Long, Long> amounts) {
        requirePositive(total);
        if (amounts.isEmpty()) {
            throw new IllegalArgumentException("Cần ít nhất một người để chia tiền");
        }
        long sum = 0;
        for (Map.Entry<Long, Long> e : amounts.entrySet()) {
            if (e.getValue() == null || e.getValue() < 0) {
                throw new IllegalArgumentException("Số tiền của thành viên " + e.getKey() + " không được âm");
            }
            sum = Math.addExact(sum, e.getValue());
        }
        if (sum != total) {
            throw new IllegalArgumentException(
                    "Tổng số tiền chia (" + sum + ") phải bằng số tiền hóa đơn (" + total + ")");
        }
        return new LinkedHashMap<>(amounts);
    }

    private static void requirePositive(long total) {
        if (total <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0");
        }
    }
}
