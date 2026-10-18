package com.coupon.rush.coupon.round.application.port.out;

import com.coupon.rush.coupon.round.domain.IssueRound;

import java.util.List;
import java.util.Optional;

public interface IssueRoundRepository {
    IssueRound save(IssueRound round);
    Optional<IssueRound> findById(Long id);
    List<IssueRound> findAllByPolicyId(Long policyId);
    boolean existsByPolicyId(Long policyId);
    void delete(IssueRound round);
}
