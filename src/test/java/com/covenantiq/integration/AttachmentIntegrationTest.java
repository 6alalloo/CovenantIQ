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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AttachmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void uploadListDownloadDeleteAttachmentFlow() throws Exception {
        String token = login("analyst@demo.com", "Demo123!");
        long loanId = createLoan(token);
        long statementId = submitStatement(token, loanId);

        MockMultipartFile pdf = new MockMultipartFile("file", "statement.pdf", "application/pdf", "PDF".getBytes());
        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/financial-statements/{id}/attachments", statementId)
                        .file(pdf)
                        .with(req -> {
                            req.setMethod("POST");
                            return req;
                        })
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        long attachmentId = objectMapper.readTree(uploadResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/financial-statements/{id}/attachments", statementId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        MvcResult downloadResult = mockMvc.perform(get("/api/v1/attachments/{id}", attachmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(downloadResult.getResponse().getContentAsByteArray().length > 0);

        mockMvc.perform(delete("/api/v1/attachments/{id}", attachmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private long createLoan(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"borrowerName":"Attachment Loan","principalAmount":1000000,"startDate":"2025-01-01"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long submitStatement(String token, long loanId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/loans/{loanId}/financial-statements", loanId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"periodType":"QUARTERLY","fiscalYear":2025,"fiscalQuarter":1,"currentAssets":100,"currentLiabilities":100,"totalDebt":120,"totalEquity":100,"ebit":20,"interestExpense":10}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }
}
