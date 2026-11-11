package com.sharebill.repository;

import com.sharebill.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    Optional<Payment> findByIdAndGroupId(Long id, Long groupId);
}
