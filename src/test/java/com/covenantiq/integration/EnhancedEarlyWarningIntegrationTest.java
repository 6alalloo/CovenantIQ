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

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EnhancedEarlyWarningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsVolatilityAndSeasonalAnomalyAlerts() throws Exception {
        String accessToken = loginAsAnalyst();
        long loanId = createLoan(accessToken);
        addCurrentRatioCovenant(loanId, accessToken);

        submitQuarterlyStatement(loanId, accessToken, 2024, 1, 60, 100);
        submitQuarterlyStatement(loanId, accessToken, 2024, 2, 100, 100);
        submitQuarterlyStatement(loanId, accessToken, 2024, 3, 140, 100);
        submitQuarterlyStatement(loanId, accessToken, 2025, 1, 220, 100);

        MvcResult alertsResult = mockMvc.perform(get("/api/v1/loans/{loanId}/alerts", loanId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode alertsJson = objectMapper.readTree(alertsResult.getResponse().getContentAsString());
        Set<String> ruleCodes = new HashSet<>();
        for (JsonNode alertNode : alertsJson.get("content")) {
            ruleCodes.add(alertNode.get("alertRuleCode").asText());
        }

        assertTrue(ruleCodes.contains("CURRENT_RATIO_VOLATILITY"));
        assertTrue(ruleCodes.contains("CURRENT_RATIO_SEASONAL_ANOMALY"));
    }

    private long createLoan(String accessToken) throws Exception {
        String createLoanBody = """
                {
                  "borrowerName": "Enhanced Warning Borrower",
                  "principalAmount": 1200000,
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
        return loanJson.get("id").asLong();
    }

    private void addCurrentRatioCovenant(long loanId, String accessToken) throws Exception {
        String covenantBody = """
                {
                  "type": "CURRENT_RATIO",
                  "thresholdValue": 0.50,
                  "comparisonType": "GREATER_THAN_EQUAL",
                  "severityLevel": "LOW"
                }
                """;

        mockMvc.perform(post("/api/v1/loans/{loanId}/covenants", loanId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(covenantBody))
                .andExpect(status().isCreated());
    }

    private void submitQuarterlyStatement(
            long loanId,
            String accessToken,
            int fiscalYear,
            int fiscalQuarter,
            int currentAssets,
            int currentLiabilities
    ) throws Exception {
        String statementBody = """
                {
                  "periodType": "QUARTERLY",
                  "fiscalYear": %d,
                  "fiscalQuarter": %d,
                  "currentAssets": %d,
                  "currentLiabilities": %d,
                  "totalDebt": 200,
                  "totalEquity": 100,
                  "ebit": 60,
                  "interestExpense": 10
                }
                """.formatted(fiscalYear, fiscalQuarter, currentAssets, currentLiabilities);

        mockMvc.perform(post("/api/v1/loans/{loanId}/financial-statements", loanId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statementBody))
                .andExpect(status().isCreated());
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
