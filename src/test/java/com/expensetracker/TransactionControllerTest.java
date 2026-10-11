package com.expensetracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransactionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String tokenFor(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("name", "Tester", "email", email, "password", "Secret123"))));
        MvcResult login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "Secret123"))))
                .andExpect(status().isOk()).andReturn();
        JsonNode node = objectMapper.readTree(login.getResponse().getContentAsString());
        return "Bearer " + node.get("token").asText();
    }

    private Map<String, Object> body(String type, Object amount, String category) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("amount", amount);
        m.put("category", category);
        m.put("date", LocalDate.now().toString());
        m.put("paymentMethod", "UPI");
        m.put("description", "test");
        return m;
    }

    private long createTransaction(String token, Map<String, Object> body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/transactions").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void requestsWithoutToken_areRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/transactions")).andExpect(status().isUnauthorized());
    }

    @Test
    void addTransaction_returns201_andAppearsInList() throws Exception {
        String token = tokenFor("add@test.com");
        createTransaction(token, body("EXPENSE", 250.50, "Food"));
        mockMvc.perform(get("/api/transactions").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].category").value("Food"));
    }

    @Test
    void addTransaction_withZeroAmount_returns400() throws Exception {
        String token = tokenFor("zero@test.com");
        mockMvc.perform(post("/api/transactions").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("EXPENSE", 0, "Food"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount must be greater than 0"));
    }

    @Test
    void updateTransaction_changesValues() throws Exception {
        String token = tokenFor("update@test.com");
        long id = createTransaction(token, body("EXPENSE", 100, "Food"));
        mockMvc.perform(put("/api/transactions/" + id).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("EXPENSE", 300, "Travel"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Travel"))
                .andExpect(jsonPath("$.amount").value(300.0));
    }

    @Test
    void deleteTransaction_removesIt() throws Exception {
        String token = tokenFor("delete@test.com");
        long id = createTransaction(token, body("INCOME", 5000, "Salary"));
        mockMvc.perform(delete("/api/transactions/" + id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(get("/api/transactions/" + id).header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction not found"));
    }

    @Test
    void users_cannotSeeOrChangeEachOthersTransactions() throws Exception {
        String owner = tokenFor("owner@test.com");
        String intruder = tokenFor("intruder@test.com");
        long id = createTransaction(owner, body("EXPENSE", 100, "Food"));
        mockMvc.perform(get("/api/transactions/" + id).header("Authorization", intruder))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/transactions/" + id).header("Authorization", intruder))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/transactions").header("Authorization", intruder))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
