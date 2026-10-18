package com.coupon.rush.coupon;

import com.coupon.rush.IntegrationTestSupport;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 정책 → 회차 → 발급 기록으로 이어지는 삭제 가드. 하위 데이터가 있으면 FK 에러(500) 대신 409 로 막는다.
class DeleteGuardIT extends IntegrationTestSupport {

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private IssueRoundRepository issueRoundRepository;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 회차가_연결된_정책은_삭제할_수_없다() throws Exception {
        Long policyId = savePolicy();
        saveRound(policyId);

        mockMvc.perform(delete("/api/policies/{id}", policyId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POLICY_IN_USE"));
    }

    @Test
    void 발급_기록이_있는_회차는_삭제할_수_없다() throws Exception {
        Long roundId = saveRound(savePolicy());
        Long usersId = userRepository.save(User.from("guard-" + UUID.randomUUID(), "pw")).getId();
        issuedCouponRepository.save(IssuedCoupon.issue(roundId, usersId));

        mockMvc.perform(delete("/api/rounds/{id}", roundId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROUND_IN_USE"));
    }

    private Long savePolicy() {
        return couponPolicyRepository.save(
                CouponPolicy.create("삭제 가드 정책", 1000, 5000, LocalDateTime.now().plusDays(30))).getId();
    }

    private Long saveRound(Long policyId) {
        LocalDateTime now = LocalDateTime.now();
        return issueRoundRepository.save(IssueRound.create(policyId, now.minusHours(1), now.plusHours(1), 10)).getId();
    }
}
