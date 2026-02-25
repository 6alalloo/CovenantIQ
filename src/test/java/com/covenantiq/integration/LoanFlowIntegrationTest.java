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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoanFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullLoanFlowWorks() throws Exception {
        String accessToken = loginAsAnalyst();

        String createLoanBody = """
                {
                  "borrowerName": "Integration Borrower",
                  "principalAmount": 1000000,
                  "startDate": "2025-01-10"
                }
                """;

        MvcResult loanResult = mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createLoanBody))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode loanJson = objectMapper.readTree(loanResult.getResponse().getContentAsString());
        long loanId = loanJson.get("id").asLong();

        String covenantBody = """
                {
                  "type": "CURRENT_RATIO",
                  "thresholdValue": 1.20,
                  "comparisonType": "GREATER_THAN_EQUAL",
                  "severityLevel": "HIGH"
                }
                """;

        mockMvc.perform(post("/api/v1/loans/{loanId}/covenants", loanId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(covenantBody))
                .andExpect(status().isCreated());

        String statementBody = """
                {
                  "periodType": "QUARTERLY",
                  "fiscalYear": 2025,
                  "fiscalQuarter": 1,
                  "currentAssets": 100,
                  "currentLiabilities": 100,
                  "totalDebt": 200,
                  "totalEquity": 100,
                  "ebit": 50,
                  "interestExpense": 10
                }
                """;

        mockMvc.perform(post("/api/v1/loans/{loanId}/financial-statements", loanId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statementBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/loans/{loanId}/risk-summary", loanId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/loans/{loanId}/alerts", loanId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/loans/{loanId}/close", loanId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
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
