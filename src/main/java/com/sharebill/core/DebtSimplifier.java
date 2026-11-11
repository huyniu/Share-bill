package com.sharebill.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Tối giản số lần chuyển tiền trong nhóm.
 * <p>
 * Đầu vào là số dư của từng người: dương nghĩa là được nhận lại tiền, âm nghĩa là phải trả.
 * Thuật toán tham lam: luôn ghép người nợ nhiều nhất với người được nhận nhiều nhất.
 * Kết quả có tối đa (n - 1) giao dịch với n là số người có số dư khác 0.
 */
public final class DebtSimplifier {

    private DebtSimplifier() {
    }

    public static List<Transfer> simplify(Map<Long, Long> balances) {
        long total = 0;
        // Sắp xếp theo số tiền giảm dần; hòa thì theo id để kết quả luôn ổn định.
        Comparator<long[]> order = Comparator.<long[]>comparingLong(a -> -a[1]).thenComparingLong(a -> a[0]);
        PriorityQueue<long[]> creditors = new PriorityQueue<>(order); // [memberId, số tiền được nhận]
        PriorityQueue<long[]> debtors = new PriorityQueue<>(order);   // [memberId, số tiền phải trả]

        for (Map.Entry<Long, Long> e : balances.entrySet()) {
            long b = e.getValue();
            total += b;
            if (b > 0) {
                creditors.add(new long[]{e.getKey(), b});
            } else if (b < 0) {
                debtors.add(new long[]{e.getKey(), -b});
            }
        }
        if (total != 0) {
            throw new IllegalArgumentException("Tổng số dư phải bằng 0, hiện là " + total);
        }

        List<Transfer> transfers = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            long[] creditor = creditors.poll();
            long[] debtor = debtors.poll();
            long amount = Math.min(creditor[1], debtor[1]);
            transfers.add(new Transfer(debtor[0], creditor[0], amount));

            creditor[1] -= amount;
            debtor[1] -= amount;
            if (creditor[1] > 0) {
                creditors.add(creditor);
            }
            if (debtor[1] > 0) {
                debtors.add(debtor);
            }
        }
        return transfers;
    }
}
