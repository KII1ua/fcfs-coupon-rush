package com.coupon.rush.coupon.round;

import com.coupon.rush.IntegrationTestSupport;
import com.coupon.rush.coupon.policy.application.port.out.CouponPolicyRepository;
import com.coupon.rush.coupon.policy.domain.CouponPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IssueRoundApiIT extends IntegrationTestSupport {

    private static final LocalDateTime OPENED_AT = LocalDateTime.of(2026, 11, 1, 10, 0, 0);
    private static final LocalDateTime CLOSED_AT = LocalDateTime.of(2026, 11, 1, 12, 0, 0);

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    private Long policyId;

    @BeforeEach
    void setUp() {
        policyId = couponPolicyRepository.save(
                CouponPolicy.create("회차 테스트 정책", 1000, 5000, LocalDateTime.of(2026, 12, 31, 23, 59, 59))
        ).getId();
    }

    @Test
    void 회차를_생성하고_조회한다() throws Exception {
        Long id = createRound(policyId, OPENED_AT, CLOSED_AT, 100);

        mockMvc.perform(get("/api/rounds/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.policyId").value(policyId))
                .andExpect(jsonPath("$.openedAt").value("2026-11-01T10:00:00"))
                .andExpect(jsonPath("$.closedAt").value("2026-11-01T12:00:00"))
                .andExpect(jsonPath("$.totalQuantity").value(100))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void 정책별_회차_목록을_조회한다() throws Exception {
        Long first = createRound(policyId, OPENED_AT, CLOSED_AT, 100);
        Long second = createRound(policyId, OPENED_AT.plusDays(1), CLOSED_AT.plusDays(1), 200);

        // 다른 정책의 회차는 섞이지 않아야 한다
        Long otherPolicyId = couponPolicyRepository.save(
                CouponPolicy.create("다른 정책", 1000, 5000, LocalDateTime.of(2026, 12, 31, 23, 59, 59))
        ).getId();
        createRound(otherPolicyId, OPENED_AT, CLOSED_AT, 10);

        mockMvc.perform(get("/api/rounds").param("policyId", String.valueOf(policyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == %d)]", first).exists())
                .andExpect(jsonPath("$[?(@.id == %d)]", second).exists());
    }

    @Test
    void 회차를_수정한다() throws Exception {
        Long id = createRound(policyId, OPENED_AT, CLOSED_AT, 100);

        mockMvc.perform(put("/api/rounds/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of(
                                "openedAt", "2026-11-02T10:00:00",
                                "closedAt", "2026-11-02T13:00:00",
                                "totalQuantity", 50))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/rounds/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openedAt").value("2026-11-02T10:00:00"))
                .andExpect(jsonPath("$.closedAt").value("2026-11-02T13:00:00"))
                .andExpect(jsonPath("$.totalQuantity").value(50));
    }

    @Test
    void 회차를_삭제하면_이후_조회는_404다() throws Exception {
        Long id = createRound(policyId, OPENED_AT, CLOSED_AT, 100);

        mockMvc.perform(delete("/api/rounds/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/rounds/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROUND_NOT_FOUND"));
    }

    @Test
    void 존재하지_않는_정책으로_생성하면_404다() throws Exception {
        mockMvc.perform(post("/api/rounds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(Long.MAX_VALUE, OPENED_AT, CLOSED_AT, 100)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POLICY_NOT_FOUND"));
    }

    @Test
    void 오픈_시각이_마감_시각_이후면_400이다() throws Exception {
        mockMvc.perform(post("/api/rounds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(policyId, CLOSED_AT, OPENED_AT, 100)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROUND"));
    }

    @Test
    void 오픈_시각과_마감_시각이_같으면_400이다() throws Exception {
        mockMvc.perform(post("/api/rounds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(policyId, OPENED_AT, OPENED_AT, 100)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROUND"));
    }

    @Test
    void 필수_필드가_없으면_400이다() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("policyId", policyId);
        body.put("openedAt", OPENED_AT.toString());
        body.put("closedAt", CLOSED_AT.toString());

        mockMvc.perform(post("/api/rounds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private Long createRound(Long policyId, LocalDateTime openedAt, LocalDateTime closedAt, int totalQuantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/rounds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(policyId, openedAt, closedAt, totalQuantity)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/rounds/")))
                .andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String createBody(Long policyId, LocalDateTime openedAt, LocalDateTime closedAt, int totalQuantity) {
        return jsonMapper.writeValueAsString(Map.of(
                "policyId", policyId,
                "openedAt", openedAt.toString(),
                "closedAt", closedAt.toString(),
                "totalQuantity", totalQuantity));
    }
}
