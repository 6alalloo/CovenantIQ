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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdditionalCovenantTypesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void evaluatesAllAdditionalCovenantTypes() throws Exception {
        String accessToken = loginAsAnalyst();
        long loanId = createLoan(accessToken);

        addCovenant(loanId, accessToken, "DSCR", "1.20", "GREATER_THAN_EQUAL");
        addCovenant(loanId, accessToken, "INTEREST_COVERAGE", "2.00", "GREATER_THAN_EQUAL");
        addCovenant(loanId, accessToken, "TANGIBLE_NET_WORTH", "3000.00", "GREATER_THAN_EQUAL");
        addCovenant(loanId, accessToken, "DEBT_TO_EBITDA", "4.00", "LESS_THAN_EQUAL");
        addCovenant(loanId, accessToken, "FIXED_CHARGE_COVERAGE", "1.50", "GREATER_THAN_EQUAL");
        addCovenant(loanId, accessToken, "QUICK_RATIO", "1.00", "GREATER_THAN_EQUAL");

        String statementBody = """
                {
                  "periodType": "QUARTERLY",
                  "fiscalYear": 2025,
                  "fiscalQuarter": 1,
                  "currentAssets": 1000,
                  "currentLiabilities": 400,
                  "totalDebt": 1200,
                  "totalEquity": 600,
                  "ebit": 300,
                  "interestExpense": 100,
                  "netOperatingIncome": 500,
                  "totalDebtService": 250,
                  "intangibleAssets": 150,
                  "ebitda": 400,
                  "fixedCharges": 50,
                  "inventory": 300,
                  "totalAssets": 5000,
                  "totalLiabilities": 1800
                }
                """;

        mockMvc.perform(post("/api/v1/loans/{loanId}/financial-statements", loanId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statementBody))
                .andExpect(status().isCreated());

        MvcResult resultsResponse = mockMvc.perform(get("/api/v1/loans/{loanId}/covenant-results", loanId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode resultsJson = objectMapper.readTree(resultsResponse.getResponse().getContentAsString());
        assertEquals(6, resultsJson.get("content").size());
    }

    private long createLoan(String accessToken) throws Exception {
        String createLoanBody = """
                {
                  "borrowerName": "Covenant Types Borrower",
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

    private void addCovenant(long loanId, String accessToken, String type, String threshold, String comparison) throws Exception {
        String covenantBody = """
                {
                  "type": "%s",
                  "thresholdValue": %s,
                  "comparisonType": "%s",
                  "severityLevel": "MEDIUM"
                }
                """.formatted(type, threshold, comparison);

        mockMvc.perform(post("/api/v1/loans/{loanId}/covenants", loanId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(covenantBody))
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
