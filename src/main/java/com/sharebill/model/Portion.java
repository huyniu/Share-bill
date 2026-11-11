package com.sharebill.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Một phần tiền gắn với một thành viên: số tiền người đó đã trả, hoặc phải chịu. */
@Embeddable
public class Portion {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private long amount;

    protected Portion() {
    }

    public Portion(Long memberId, long amount) {
        this.memberId = memberId;
        this.amount = amount;
    }

    public Long getMemberId() {
        return memberId;
    }

    public long getAmount() {
        return amount;
    }
}
