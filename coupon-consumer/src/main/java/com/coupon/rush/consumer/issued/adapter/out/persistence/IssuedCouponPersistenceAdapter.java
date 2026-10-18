package com.coupon.rush.consumer.issued.adapter.out.persistence;

import com.coupon.rush.consumer.issued.application.port.out.DuplicateIssueException;
import com.coupon.rush.consumer.issued.application.port.out.IssuedCouponRepository;
import com.coupon.rush.consumer.issued.domain.IssuedCoupon;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class IssuedCouponPersistenceAdapter implements IssuedCouponRepository {

    private final IssuedCouponJpaRepository issuedCouponJpaRepository;

    @Override
    public boolean existsByRoundIdAndUsersId(Long roundId, Long usersId) {
        return issuedCouponJpaRepository.existsByRoundIdAndUsersId(roundId, usersId);
    }

    @Override
    public IssuedCoupon save(IssuedCoupon issuedCoupon) {
        try {
            return issuedCouponJpaRepository.save(issuedCoupon);
        } catch (DataIntegrityViolationException e) {
            // FK 위반(없는 회차·유저)도 같은 예외로 올라온다. 중복만 멱등 처리하고 나머지는 재시도·DLT 로 넘긴다
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains(IssuedCoupon.UNIQUE_ROUND_USERS)) {
                throw new DuplicateIssueException(issuedCoupon.getRoundId(), issuedCoupon.getUsersId());
            }
            throw e;
        }
    }
}
