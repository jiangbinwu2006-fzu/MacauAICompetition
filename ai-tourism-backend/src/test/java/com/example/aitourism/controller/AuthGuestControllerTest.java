package com.example.aitourism.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthGuestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsTemporaryGuestSessionWithoutCredentials() throws Exception {
        mockMvc.perform(post("/auth/guest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.expires_in").value(7200))
                .andExpect(jsonPath("$.data.session_id").isNotEmpty())
                .andExpect(jsonPath("$.data.user.user_id").value(org.hamcrest.Matchers.startsWith("guest:")))
                .andExpect(jsonPath("$.data.user.guest").value(true))
                .andExpect(jsonPath("$.data.user.persistence_authorized").value(false));
    }
}
