package com.covenantiq.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CovenantManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void covenantCanBeListedAndUpdated() throws Exception {
        String accessToken = loginAsAnalyst();

        MvcResult createLoanResult = mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "borrowerName": "Covenant Management Integration",
                                  "principalAmount": 1100000,
                                  "startDate": "2026-02-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        long loanId = objectMapper.readTree(createLoanResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult createCovenantResult = mockMvc.perform(post("/api/v1/loans/{loanId}/covenants", loanId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "CURRENT_RATIO",
                                  "thresholdValue": 1.20,
                                  "comparisonType": "GREATER_THAN_EQUAL",
                                  "severityLevel": "HIGH"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createCovenantResult.getResponse().getContentAsString());
        long covenantId = created.get("id").asLong();

        mockMvc.perform(get("/api/v1/loans/{loanId}/covenants", loanId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(covenantId))
                .andExpect(jsonPath("$[0].type").value("CURRENT_RATIO"));

        mockMvc.perform(patch("/api/v1/loans/{loanId}/covenants/{covenantId}", loanId, covenantId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "thresholdValue": 1.35,
                                  "comparisonType": "GREATER_THAN_EQUAL",
                                  "severityLevel": "MEDIUM"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thresholdValue").value(1.35))
                .andExpect(jsonPath("$.severityLevel").value("MEDIUM"));
    }

    private String loginAsAnalyst() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "analyst@demo.com",
                                  "password": "Demo123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginJson.get("accessToken").asText();
    }
}
