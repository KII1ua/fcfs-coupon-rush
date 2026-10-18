package com.coupon.rush.coupon.round.application;

import com.coupon.rush._config.exception.BusinessException;
import com.coupon.rush._config.exception.ErrorCode;
import com.coupon.rush.coupon.issued.application.port.out.IssuedCouponRepository;
import com.coupon.rush.coupon.policy.application.port.out.CouponPolicyRepository;
import com.coupon.rush.coupon.round.application.port.out.IssueRoundCache;
import com.coupon.rush.coupon.round.application.port.out.IssueRoundRepository;
import com.coupon.rush.coupon.round.domain.IssueRound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IssueRoundService {

    private final IssueRoundRepository issueRoundRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final IssueRoundCache issueRoundCache;

    @Transactional
    public Long create(Long policyId, LocalDateTime openedAt, LocalDateTime closedAt, int totalQuantity) {
        if (!couponPolicyRepository.existsById(policyId)) {
            throw new BusinessException(ErrorCode.POLICY_NOT_FOUND);
        }
        IssueRound round = issueRoundRepository.save(IssueRound.create(policyId, openedAt, closedAt, totalQuantity));

        issueRoundCache.save(round);

        return round.getId();
    }

    @Transactional(readOnly = true)
    public IssueRound get(Long id) {
        return findRound(id);
    }

    @Transactional(readOnly = true)
    public List<IssueRound> getAllByPolicyId(Long policyId) {
        return issueRoundRepository.findAllByPolicyId(policyId);
    }

    @Transactional
    public void update(Long id, LocalDateTime openedAt, LocalDateTime closedAt, int totalQuantity) {
        IssueRound round = findRound(id);
        round.update(openedAt, closedAt, totalQuantity);
        issueRoundCache.save(round);
    }

    @Transactional
    public void delete(Long id) {
        IssueRound round = findRound(id);
        if (issuedCouponRepository.existsByRoundId(id)) {
            throw new BusinessException(ErrorCode.ROUND_IN_USE);
        }
        issueRoundRepository.delete(round);
    }

    private IssueRound findRound(Long id) {
        return issueRoundRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUND_NOT_FOUND));
    }
}
