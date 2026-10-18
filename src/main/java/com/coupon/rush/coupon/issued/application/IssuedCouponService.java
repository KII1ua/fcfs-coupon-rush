package com.coupon.rush.coupon.issued.application;

import com.coupon.rush._config.exception.BusinessException;
import com.coupon.rush._config.exception.ErrorCode;
import com.coupon.rush.coupon.issued.application.port.out.IssuedCouponRepository;
import com.coupon.rush.coupon.issued.domain.IssuedCoupon;
import com.coupon.rush.coupon.round.application.port.out.IssueRoundRepository;
import com.coupon.rush.coupon.round.domain.IssueRound;
import com.coupon.rush.user.application.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IssuedCouponService {

    private final IssuedCouponRepository issuedCouponRepository;
    private final IssueRoundRepository issueRoundRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Long issue(Long roundId, Long usersId) {
        if (!userRepository.existsById(usersId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        IssueRound round = issueRoundRepository.findByIdForUpdate(roundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUND_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now(clock);
        if (round.isBeforeOpen(now)) {
            throw new BusinessException(ErrorCode.ROUND_NOT_OPEN);
        }
        if (round.isClosed(now)) {
            throw new BusinessException(ErrorCode.ROUND_CLOSED);
        }
        if (issuedCouponRepository.existsByRoundIdAndUsersId(roundId, usersId)) {
            throw new BusinessException(ErrorCode.ALREADY_ISSUED);
        }
        if (round.isSoldOut(issuedCouponRepository.countByRoundId(roundId))) {
            throw new BusinessException(ErrorCode.SOLD_OUT);
        }

        return issuedCouponRepository.save(IssuedCoupon.issue(roundId, usersId)).getId();
    }

    @Transactional(readOnly = true)
    public IssuedCoupon get(Long id) {
        return issuedCouponRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<IssuedCoupon> getAllByUsersId(Long usersId) {
        return issuedCouponRepository.findAllByUsersId(usersId);
    }

    @Transactional
    public void use(Long id) {
        IssuedCoupon coupon = issuedCouponRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
        coupon.use();
    }
}
