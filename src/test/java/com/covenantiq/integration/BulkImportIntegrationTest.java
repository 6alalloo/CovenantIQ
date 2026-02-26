package com.covenantiq.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BulkImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void bulkImportCsvSuccessAndPartialFailure() throws Exception {
        String token = loginAsAnalyst();
        long loanId = createLoan(token);

        String csv = """
                periodEndDate,currentAssets,currentLiabilities,totalDebt,totalEquity,ebit,interestExpense
                2025-03-31,100,80,120,100,40,10
                2025-03-31,120,80,130,100,45,10
                """;
        MockMultipartFile file = new MockMultipartFile("file", "statements.csv", "text/csv", csv.getBytes());

        MvcResult result = mockMvc.perform(multipart("/api/v1/loans/{loanId}/financial-statements/bulk-import", loanId)
                        .file(file)
                        .with(req -> {
                            req.setMethod("POST");
                            return req;
                        })
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(2, json.get("totalRows").asInt());
        assertEquals(1, json.get("successCount").asInt());
        assertEquals(1, json.get("failureCount").asInt());
    }

    @Test
    void bulkImportRejectsLargeFile() throws Exception {
        String token = loginAsAnalyst();
        long loanId = createLoan(token);

        byte[] large = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "large.csv", "text/csv", large);

        mockMvc.perform(multipart("/api/v1/loans/{loanId}/financial-statements/bulk-import", loanId)
                        .file(file)
                        .with(req -> {
                            req.setMethod("POST");
                            return req;
                        })
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isPayloadTooLarge());
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

        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private long createLoan(String token) throws Exception {
        MvcResult createLoan = mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "borrowerName": "Bulk Import Loan",
                                  "principalAmount": 2000000,
                                  "startDate": "2025-01-05"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(createLoan.getResponse().getContentAsString()).get("id").asLong();
    }
}
