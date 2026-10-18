package com.coupon.rush.coupon.issued;

import com.coupon.rush.IntegrationTestSupport;
import com.coupon.rush.coupon.issued.adapter.in.web.IssueCouponRequest;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IssuedCouponApiIT extends IntegrationTestSupport {

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private IssueRoundRepository issueRoundRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @Test
    void 발급을_요청하면_202_로_응답한다() throws Exception {
        Long roundId = createOpenRound(100);
        Long usersId = createUser();

        issue(roundId, usersId)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.roundId").value(roundId))
                .andExpect(jsonPath("$.usersId").value(usersId));
    }

    // 발급 기록은 처리 서버가 저장한다. 여기서는 저장된 기록을 조회·사용하는 흐름만 확인한다
    @Test
    void 저장된_발급_기록의_조회_목록_사용_흐름() throws Exception {
        Long roundId = createOpenRound(100);
        Long usersId = createUser();
        Long id = issuedCouponRepository.save(IssuedCoupon.issue(roundId, usersId)).getId();

        mockMvc.perform(get("/api/issued-coupons/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.roundId").value(roundId))
                .andExpect(jsonPath("$.usersId").value(usersId))
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.createdAt").exists());

        mockMvc.perform(get("/api/issued-coupons").param("usersId", usersId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(patch("/api/issued-coupons/{id}/use", id))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/issued-coupons/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));

        mockMvc.perform(patch("/api/issued-coupons/{id}/use", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COUPON_NOT_USABLE"));
    }

    @Test
    void 같은_사용자가_같은_회차를_두번_받으면_ALREADY_ISSUED() throws Exception {
        Long roundId = createOpenRound(100);
        Long usersId = createUser();
        issue(roundId, usersId).andExpect(status().isAccepted());

        issue(roundId, usersId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_ISSUED"));
    }

    @Test
    void 수량이_소진되면_SOLD_OUT() throws Exception {
        Long roundId = createOpenRound(1);
        issue(roundId, createUser()).andExpect(status().isAccepted());

        issue(roundId, createUser())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SOLD_OUT"));
    }

    @Test
    void 오픈_전_회차면_ROUND_NOT_OPEN() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Long roundId = createRound(now.plusHours(1), now.plusHours(2), 100);

        issue(roundId, createUser())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROUND_NOT_OPEN"));
    }

    @Test
    void 회차_id_가_없으면_INVALID_REQUEST() throws Exception {
        mockMvc.perform(post("/api/issued-coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usersId\": " + createUser() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 없는_쿠폰을_조회하면_COUPON_NOT_FOUND() throws Exception {
        mockMvc.perform(get("/api/issued-coupons/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COUPON_NOT_FOUND"));
    }

    private ResultActions issue(Long roundId, Long usersId) throws Exception {
        return mockMvc.perform(post("/api/issued-coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(new IssueCouponRequest(roundId, usersId))));
    }

    // 실제 Clock 빈을 쓰므로 회차 기간은 현재 시각 기준으로 잡는다.
    private Long createOpenRound(int totalQuantity) {
        LocalDateTime now = LocalDateTime.now();
        return createRound(now.minusHours(1), now.plusHours(1), totalQuantity);
    }

    private Long createRound(LocalDateTime openedAt, LocalDateTime closedAt, int totalQuantity) {
        CouponPolicy policy = couponPolicyRepository.save(
                CouponPolicy.create("테스트 정책", 1000, 5000, LocalDateTime.now().plusDays(30)));
        return issueRoundRepository.save(IssueRound.create(policy.getId(), openedAt, closedAt, totalQuantity)).getId();
    }

    private Long createUser() {
        return userRepository.save(User.from("user-" + UUID.randomUUID(), "password")).getId();
    }
}
