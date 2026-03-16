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
class RbacEnforcementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void riskLeadCannotCreateLoanAndAnalystCannotResolveAlert() throws Exception {
        String analystToken = accessTokenFor("analyst@demo.com", "Demo123!");
        String riskLeadToken = accessTokenFor("risklead@demo.com", "Demo123!");

        mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + riskLeadToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "borrowerName": "RBAC Forbidden Borrower",
                                  "principalAmount": 1200000,
                                  "startDate": "2026-01-10"
                                }
                                """))
                .andExpect(status().isForbidden());

        long loanId = createLoan(analystToken, "RBAC Analyst Borrower");
        addBreachingCovenant(analystToken, loanId);
        submitBreachingStatement(analystToken, loanId);
        long alertId = firstAlertId(analystToken, loanId);

        mockMvc.perform(patch("/api/v1/alerts/{alertId}/status", alertId)
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ACKNOWLEDGED"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/alerts/{alertId}/status", alertId)
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED",
                                  "resolutionNotes": "Analyst should not resolve."
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void analystCanAccessPortfolioSummary() throws Exception {
        String analystToken = accessTokenFor("analyst@demo.com", "Demo123!");
        String riskLeadToken = accessTokenFor("risklead@demo.com", "Demo123!");

        mockMvc.perform(get("/api/v1/portfolio/summary")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/portfolio/summary")
                        .header("Authorization", "Bearer " + riskLeadToken))
                .andExpect(status().isOk());
    }

    private String accessTokenFor(String username, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private long createLoan(String token, String borrowerName) throws Exception {
        MvcResult createLoanResult = mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "borrowerName": "%s",
                                  "principalAmount": 1800000,
                                  "startDate": "2026-01-10"
                                }
                                """.formatted(borrowerName)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(createLoanResult.getResponse().getContentAsString()).get("id").asLong();
    }

    private void addBreachingCovenant(String token, long loanId) throws Exception {
        mockMvc.perform(post("/api/v1/loans/{loanId}/covenants", loanId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "CURRENT_RATIO",
                                  "thresholdValue": 2.00,
                                  "comparisonType": "GREATER_THAN_EQUAL",
                                  "severityLevel": "HIGH"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    private void submitBreachingStatement(String token, long loanId) throws Exception {
        mockMvc.perform(post("/api/v1/loans/{loanId}/financial-statements", loanId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "periodType": "QUARTERLY",
                                  "fiscalYear": 2026,
                                  "fiscalQuarter": 1,
                                  "currentAssets": 100,
                                  "currentLiabilities": 100,
                                  "totalDebt": 200,
                                  "totalEquity": 100,
                                  "ebit": 40,
                                  "interestExpense": 10
                                }
                                """))
                .andExpect(status().isCreated());
    }

    private long firstAlertId(String token, long loanId) throws Exception {
        MvcResult alertsResult = mockMvc.perform(get("/api/v1/loans/{loanId}/alerts", loanId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(alertsResult.getResponse().getContentAsString());
        return json.get("content").get(0).get("id").asLong();
    }
}
