package com.coupon.rush.user;

import com.coupon.rush.IntegrationTestSupport;
import com.coupon.rush.user.adapter.in.web.UserCreateRequest;
import com.coupon.rush.user.adapter.in.web.UserCreateResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserApiIT extends IntegrationTestSupport {

    @Test
    void 가입하면_201과_Location_을_주고_조회시_비밀번호는_노출하지_않는다() throws Exception {
        String userId = uniqueUserId();

        MvcResult result = register(userId, "password")
                .andExpect(status().isCreated())
                .andReturn();
        Long id = jsonMapper.readValue(result.getResponse().getContentAsString(), UserCreateResponse.class).id();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/users/" + id);

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void 이미_있는_아이디로_가입하면_DUPLICATE_USER_ID() throws Exception {
        String userId = uniqueUserId();
        register(userId, "password").andExpect(status().isCreated());

        register(userId, "other")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_USER_ID"));
    }

    @Test
    void 아이디가_비어있으면_INVALID_REQUEST() throws Exception {
        register(" ", "password")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 비밀번호가_비어있으면_INVALID_REQUEST() throws Exception {
        register(uniqueUserId(), "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 없는_사용자를_조회하면_USER_NOT_FOUND() throws Exception {
        mockMvc.perform(get("/api/users/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    private ResultActions register(String userId, String password) throws Exception {
        return mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(new UserCreateRequest(userId, password))));
    }

    private static String uniqueUserId() {
        return "user-" + UUID.randomUUID();
    }
}
