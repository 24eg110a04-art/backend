package com.expensetracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private void register(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Test User", "email", email, "password", password))))
                .andExpect(status().isCreated());
    }

    @Test
    void register_createsUser_andNeverReturnsPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Kiran", "email", "kiran@test.com", "password", "Secret123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("kiran@test.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        register("dup@test.com", "Secret123");
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Other", "email", "DUP@test.com", "password", "Secret123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_weakPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Weak", "email", "weak@test.com", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Password")));
    }

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        register("login@test.com", "Secret123");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "login@test.com", "password", "Secret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        register("wrong@test.com", "Secret123");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "wrong@test.com", "password", "Nope12345"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
