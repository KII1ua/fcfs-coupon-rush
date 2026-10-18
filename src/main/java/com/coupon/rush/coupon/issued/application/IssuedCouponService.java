package com.coupon.rush.coupon.issued.application;

import com.coupon.rush._config.exception.BusinessException;
import com.coupon.rush._config.exception.ErrorCode;
import com.coupon.rush.coupon.issued.application.port.out.IssueRoster;
import com.coupon.rush.coupon.issued.application.port.out.IssuedCouponRepository;
import com.coupon.rush.coupon.issued.domain.IssuedCoupon;
import com.coupon.rush.coupon.round.application.port.out.IssueRoundRepository;
import com.coupon.rush.coupon.round.domain.IssueRound;
import com.coupon.rush.user.application.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IssuedCouponService {

    private final IssuedCouponRepository issuedCouponRepository;
    private final IssueRoundRepository issueRoundRepository;
    private final UserRepository userRepository;
    private final IssueRoster issueRoster;
    private final Clock clock;

    // 판정은 Redis 명단(수량, 1인 1장)이 하고 DB 에는 저장만 한다.
    // DB 저장이 롤백되면 Redis 명단에서도 빼서 둘을 맞춘다. 커밋 단계 실패까지 잡으려고 트랜잭션 종료 시점에 확인한다.
    @Transactional
    public Long issue(Long roundId, Long usersId) {
        if (!userRepository.existsById(usersId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        IssueRound round = issueRoundRepository.findById(roundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUND_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now(clock);
        if (round.isBeforeOpen(now)) {
            throw new BusinessException(ErrorCode.ROUND_NOT_OPEN);
        }
        if (round.isClosed(now)) {
            throw new BusinessException(ErrorCode.ROUND_CLOSED);
        }

        switch (issueRoster.tryIssue(roundId, usersId, round.getTotalQuantity())) {
            case DUPLICATED -> throw new BusinessException(ErrorCode.ALREADY_ISSUED);
            case SOLD_OUT -> throw new BusinessException(ErrorCode.SOLD_OUT);
            case ACCEPTED -> { }
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    issueRoster.cancel(roundId, usersId);
                }
            }
        });

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
