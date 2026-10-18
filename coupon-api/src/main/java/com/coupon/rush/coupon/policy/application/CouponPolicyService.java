package com.coupon.rush.coupon.policy.application;

import com.coupon.rush._config.exception.BusinessException;
import com.coupon.rush._config.exception.ErrorCode;
import com.coupon.rush.coupon.policy.application.port.out.CouponPolicyRepository;
import com.coupon.rush.coupon.policy.domain.CouponPolicy;
import com.coupon.rush.coupon.round.application.port.out.IssueRoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponPolicyService {

    private final CouponPolicyRepository couponPolicyRepository;
    private final IssueRoundRepository issueRoundRepository;

    @Transactional
    public Long create(String name, int discountValue, int maxDiscountAmount, LocalDateTime validUntil) {
        CouponPolicy policy = CouponPolicy.create(name, discountValue, maxDiscountAmount, validUntil);
        return couponPolicyRepository.save(policy).getId();
    }

    @Transactional(readOnly = true)
    public CouponPolicy get(Long id) {
        return findOrThrow(id);
    }

    @Transactional(readOnly = true)
    public List<CouponPolicy> getAll() {
        return couponPolicyRepository.findAll();
    }

    @Transactional
    public void update(Long id, String name, int discountValue, int maxDiscountAmount, LocalDateTime validUntil) {
        findOrThrow(id).update(name, discountValue, maxDiscountAmount, validUntil);
    }

    @Transactional
    public void delete(Long id) {
        CouponPolicy policy = findOrThrow(id);
        if (issueRoundRepository.existsByPolicyId(id)) {
            throw new BusinessException(ErrorCode.POLICY_IN_USE);
        }
        couponPolicyRepository.delete(policy);
    }

    private CouponPolicy findOrThrow(Long id) {
        return couponPolicyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));
    }
}
