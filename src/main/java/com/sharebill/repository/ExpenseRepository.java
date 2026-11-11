package com.sharebill.repository;

import com.sharebill.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    Optional<Expense> findByIdAndGroupId(Long id, Long groupId);
}
