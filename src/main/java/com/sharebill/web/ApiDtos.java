package com.sharebill.web;

import com.sharebill.core.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;

/** Dữ liệu vào/ra của REST API. Mọi số tiền tính bằng đồng (số nguyên). */
public final class ApiDtos {

    private ApiDtos() {
    }

    // ---------- Request ----------

    public record CreateGroupRequest(
            @NotBlank String name,
            List<@NotBlank String> members) {
    }

    public record AddMemberRequest(@NotBlank String name) {
    }

    /**
     * @param memberId người trả
     * @param amount   số tiền người đó trả; có thể bỏ trống nếu chỉ có một người trả toàn bộ
     */
    public record PayerInput(@NotNull Long memberId, @Positive Long amount) {
    }

    /**
     * @param memberId người chịu tiền
     * @param value    EQUAL: bỏ qua; EXACT: số tiền; SHARES: số phần
     */
    public record SplitInput(@NotNull Long memberId, Long value) {
    }

    /**
     * @param splitAmong để trống khi chia đều thì chia cho cả nhóm
     */
    public record CreateExpenseRequest(
            @NotBlank String description,
            @NotNull @Positive Long amount,
            @NotEmpty List<@Valid PayerInput> paidBy,
            @NotNull SplitType splitType,
            List<@Valid SplitInput> splitAmong) {
    }

    public record CreatePaymentRequest(
            @NotNull Long fromMemberId,
            @NotNull Long toMemberId,
            @NotNull @Positive Long amount) {
    }

    // ---------- Response ----------

    public record MemberView(Long id, String name) {
    }

    public record GroupView(Long id, String name, List<MemberView> members, long totalSpent, Instant createdAt) {
    }

    public record PortionView(Long memberId, String name, long amount) {
    }

    public record ExpenseView(
            Long id,
            String description,
            long amount,
            SplitType splitType,
            List<PortionView> paidBy,
            List<PortionView> splitAmong,
            Instant createdAt) {
    }

    /**
     * @param paid     tổng tiền đã trả cho các hóa đơn
     * @param owed     tổng tiền phải chịu
     * @param sent     tổng tiền đã chuyển trả nợ cho người khác
     * @param received tổng tiền đã nhận trả nợ từ người khác
     * @param balance  dương: còn được nhận lại; âm: còn phải trả
     */
    public record BalanceView(Long memberId, String name, long paid, long owed, long sent, long received, long balance) {
    }

    public record TransferView(Long fromMemberId, String fromName, Long toMemberId, String toName, long amount) {
    }

    public record PaymentView(Long id, Long fromMemberId, String fromName, Long toMemberId, String toName,
                              long amount, Instant createdAt) {
    }
}
