package com.coupon.rush.coupon.issued.adapter.out.persistence;

import com.coupon.rush._config.exception.BusinessException;
import com.coupon.rush._config.exception.ErrorCode;
import com.coupon.rush.coupon.issued.application.port.out.IssuedCouponRepository;
import com.coupon.rush.coupon.issued.domain.IssuedCoupon;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class IssuedCouponPersistenceAdapter implements IssuedCouponRepository {
    private static final String UNIQUE_ROUND_USERS = "uk_issued_coupon_round_users";

    private final IssuedCouponJpaRepository issuedCouponJpaRepository;

    @Override
    public IssuedCoupon save(IssuedCoupon issuedCoupon) {
        try {
            return issuedCouponJpaRepository.save(issuedCoupon);
        } catch (DataIntegrityViolationException e) {
            // FK 위반도 같은 예외로 올라오므로 제약 이름으로 중복 발급만 골라낸다
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains(UNIQUE_ROUND_USERS)) {
                throw new BusinessException(ErrorCode.ALREADY_ISSUED);
            }
            throw e;
        }
    }

    @Override
    public Optional<IssuedCoupon> findById(Long id) {
        return issuedCouponJpaRepository.findById(id);
    }

    @Override
    public List<IssuedCoupon> findAllByUsersId(Long usersId) {
        return issuedCouponJpaRepository.findAllByUsersId(usersId);
    }

    @Override
    public boolean existsByRoundIdAndUsersId(Long roundId, Long usersId) {
        return issuedCouponJpaRepository.existsByRoundIdAndUsersId(roundId, usersId);
    }

    @Override
    public boolean existsByRoundId(Long roundId) {
        return issuedCouponJpaRepository.existsByRoundId(roundId);
    }

    @Override
    public long countByRoundId(Long roundId) {
        return issuedCouponJpaRepository.countByRoundId(roundId);
    }
}
