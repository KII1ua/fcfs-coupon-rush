package com.coupon.rush.coupon.issued;

import com.coupon.rush.IntegrationTestSupport;
import com.coupon.rush._config.exception.BusinessException;
import com.coupon.rush._config.exception.ErrorCode;
import com.coupon.rush.coupon.issued.application.port.out.IssuedCouponRepository;
import com.coupon.rush.coupon.issued.domain.IssuedCoupon;
import com.coupon.rush.coupon.policy.application.port.out.CouponPolicyRepository;
import com.coupon.rush.coupon.policy.domain.CouponPolicy;
import com.coupon.rush.coupon.round.application.port.out.IssueRoundRepository;
import com.coupon.rush.coupon.round.domain.IssueRound;
import com.coupon.rush.user.application.port.out.UserRepository;
import com.coupon.rush.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Redis 명단을 거치지 않고 같은 회차·같은 유저로 두 번 저장하면 DB Unique 제약이 막는다 (최후 방어선)
class IssuedCouponUniqueGuardIT extends IntegrationTestSupport {

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private IssueRoundRepository issueRoundRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 같은_회차_같은_유저를_두_번_저장하면_ALREADY_ISSUED() {
        LocalDateTime now = LocalDateTime.now();
        Long policyId = couponPolicyRepository.save(CouponPolicy.create("Unique 가드 정책", 1000, 1000, now.plusDays(30))).getId();
        Long roundId = issueRoundRepository.save(IssueRound.create(policyId, now.minusHours(1), now.plusHours(1), 10)).getId();
        Long usersId = userRepository.save(User.from("unique-" + UUID.randomUUID(), "pw")).getId();
        issuedCouponRepository.save(IssuedCoupon.issue(roundId, usersId));

        assertThatThrownBy(() -> issuedCouponRepository.save(IssuedCoupon.issue(roundId, usersId)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALREADY_ISSUED);
    }
}
