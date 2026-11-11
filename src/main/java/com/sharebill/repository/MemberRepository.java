package com.sharebill.repository;

import com.sharebill.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByGroupIdOrderById(Long groupId);
}
