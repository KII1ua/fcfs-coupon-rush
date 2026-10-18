package com.coupon.rush.coupon.round.adapter.out.persistence;

import com.coupon.rush.coupon.round.domain.IssueRound;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface IssueRoundJpaRepository extends JpaRepository<IssueRound, Long> {
    List<IssueRound> findAllByPolicyId(Long policyId);
    boolean existsByPolicyId(Long policyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ir from IssueRound ir where ir.id = :id")
    Optional<IssueRound> findByIdForUpdate(@Param("id") Long id);
}
