package com.coupon.rush.coupon.issued.adapter.out.persistence;

import com.coupon.rush.coupon.issued.application.port.out.IssuedCouponRepository;
import com.coupon.rush.coupon.issued.domain.IssuedCoupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class IssuedCouponPersistenceAdapter implements IssuedCouponRepository {
    private final IssuedCouponJpaRepository issuedCouponJpaRepository;

    @Override
    public IssuedCoupon save(IssuedCoupon issuedCoupon) {
        return issuedCouponJpaRepository.save(issuedCoupon);
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
