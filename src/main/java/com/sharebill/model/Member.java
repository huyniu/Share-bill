package com.sharebill.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** Thành viên trong một nhóm. */
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private BillGroup group;

    @Column(nullable = false)
    private String name;

    protected Member() {
    }

    public Member(BillGroup group, String name) {
        this.group = group;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public BillGroup getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }
}
