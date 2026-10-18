package com.coupon.rush.coupon.round.adapter.out.persistence;

import com.coupon.rush.coupon.round.domain.IssueRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface IssueRoundJpaRepository extends JpaRepository<IssueRound, Long> {
    List<IssueRound> findAllByPolicyId(Long policyId);
    boolean existsByPolicyId(Long policyId);
}
