package com.coupon.rush.coupon.round.adapter.out.persistence;

import com.coupon.rush.coupon.round.application.port.out.IssueRoundRepository;
import com.coupon.rush.coupon.round.domain.IssueRound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class IssueRoundPersistenceAdapter implements IssueRoundRepository {
    private final IssueRoundJpaRepository issueRoundJpaRepository;

    @Override
    public IssueRound save(IssueRound round) {
        return issueRoundJpaRepository.save(round);
    }

    @Override
    public Optional<IssueRound> findById(Long id) {
        return issueRoundJpaRepository.findById(id);
    }

    @Override
    public List<IssueRound> findAllByPolicyId(Long policyId) {
        return issueRoundJpaRepository.findAllByPolicyId(policyId);
    }

    @Override
    public boolean existsByPolicyId(Long policyId) {
        return issueRoundJpaRepository.existsByPolicyId(policyId);
    }

    @Override
    public void delete(IssueRound round) {
        issueRoundJpaRepository.delete(round);
    }

    @Override
    public Optional<IssueRound> findByIdForUpdate(Long id) {
        return issueRoundJpaRepository.findByIdForUpdate(id);
    }
}
