package com.sharebill.model;

import com.sharebill.core.SplitType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Một khoản chi. Có thể nhiều người cùng trả ({@link #payers}),
 * và được chia cho một số người ({@link #shares}, đã quy ra số tiền cụ thể).
 */
@Entity
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private BillGroup group;

    @Column(nullable = false)
    private String description;

    /** Tổng số tiền, đơn vị đồng. */
    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SplitType splitType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expense_payer", joinColumns = @JoinColumn(name = "expense_id"))
    @OrderColumn(name = "position")
    private List<Portion> payers = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "expense_share", joinColumns = @JoinColumn(name = "expense_id"))
    @OrderColumn(name = "position")
    private List<Portion> shares = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Expense() {
    }

    public Expense(BillGroup group, String description, long amount, SplitType splitType,
                   List<Portion> payers, List<Portion> shares) {
        this.group = group;
        this.description = description;
        this.amount = amount;
        this.splitType = splitType;
        this.payers = new ArrayList<>(payers);
        this.shares = new ArrayList<>(shares);
    }

    public Long getId() {
        return id;
    }

    public BillGroup getGroup() {
        return group;
    }

    public String getDescription() {
        return description;
    }

    public long getAmount() {
        return amount;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public List<Portion> getPayers() {
        return payers;
    }

    public List<Portion> getShares() {
        return shares;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
