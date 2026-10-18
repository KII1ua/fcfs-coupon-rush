package com.coupon.rush.coupon.policy;

import com.coupon.rush.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CouponPolicyApiIT extends IntegrationTestSupport {

    private static final String VALID_UNTIL = "2026-12-31T23:59:59";

    @Test
    void 생성_후_단건_조회() throws Exception {
        String name = uniqueName();

        MvcResult result = mockMvc.perform(post("/api/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(name, 10, 5000, VALID_UNTIL)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = readId(result);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/policies/" + id);

        mockMvc.perform(get("/api/policies/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.discountValue").value(10))
                .andExpect(jsonPath("$.maxDiscountAmount").value(5000))
                .andExpect(jsonPath("$.validUntil").value(VALID_UNTIL))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void 목록에_생성한_정책이_포함된다() throws Exception {
        String name = uniqueName();
        long id = create(name);

        String content = mockMvc.perform(get("/api/policies"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode list = jsonMapper.readTree(content);
        assertThat(list.isArray()).isTrue();
        boolean found = false;
        for (JsonNode node : list) {
            if (node.get("id").asLong() == id) {
                assertThat(node.get("name").asString()).isEqualTo(name);
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void 수정하면_필드가_바뀐다() throws Exception {
        long id = create(uniqueName());
        String newName = uniqueName();

        mockMvc.perform(put("/api/policies/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(newName, 20, 10000, "2027-06-30T12:00:00")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/policies/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.discountValue").value(20))
                .andExpect(jsonPath("$.maxDiscountAmount").value(10000))
                .andExpect(jsonPath("$.validUntil").value("2027-06-30T12:00:00"));
    }

    @Test
    void 삭제하면_이후_조회는_404() throws Exception {
        long id = create(uniqueName());

        mockMvc.perform(delete("/api/policies/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/policies/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POLICY_NOT_FOUND"));
    }

    @Test
    void 잘못된_요청_본문은_400() throws Exception {
        mockMvc.perform(post("/api/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", 10, 5000, VALID_UNTIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(uniqueName(), 0, 5000, VALID_UNTIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "discountValue": 10, "maxDiscountAmount": 5000}
                                """.formatted(uniqueName())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        long id = create(uniqueName());
        mockMvc.perform(put("/api/policies/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(uniqueName(), 10, -1, VALID_UNTIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 없는_정책_조회는_404() throws Exception {
        mockMvc.perform(get("/api/policies/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POLICY_NOT_FOUND"));
    }

    private long create(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(name, 10, 5000, VALID_UNTIL)))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
    }

    private long readId(MvcResult result) throws Exception {
        return jsonMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private static String body(String name, int discountValue, int maxDiscountAmount, String validUntil) {
        return """
                {"name": "%s", "discountValue": %d, "maxDiscountAmount": %d, "validUntil": "%s"}
                """.formatted(name, discountValue, maxDiscountAmount, validUntil);
    }

    private static String uniqueName() {
        return "policy-" + UUID.randomUUID();
    }
}
