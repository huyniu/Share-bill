package com.sharebill.repository;

import com.sharebill.model.BillGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillGroupRepository extends JpaRepository<BillGroup, Long> {
}
