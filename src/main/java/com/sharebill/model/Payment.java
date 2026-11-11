package com.sharebill.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/** Một lần trả nợ thực tế: {@code fromMemberId} đã chuyển cho {@code toMemberId}. */
@Entity
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private BillGroup group;

    @Column(nullable = false)
    private Long fromMemberId;

    @Column(nullable = false)
    private Long toMemberId;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Payment() {
    }

    public Payment(BillGroup group, Long fromMemberId, Long toMemberId, long amount) {
        this.group = group;
        this.fromMemberId = fromMemberId;
        this.toMemberId = toMemberId;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public Long getFromMemberId() {
        return fromMemberId;
    }

    public Long getToMemberId() {
        return toMemberId;
    }

    public long getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
