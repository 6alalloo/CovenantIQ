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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoanImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCanPreviewAndExecuteLoanImport() throws Exception {
        String admin = login("admin@demo.com", "Demo123!");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "loan-import.csv",
                "text/csv",
                (
                        "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate,status,sourceUpdatedAt\n" +
                        "CORE_BANKING,LN-200001,Imported Demo Borrower,1250000.00,2025-01-15,ACTIVE,2026-03-08T10:15:00Z\n"
                ).getBytes()
        );

        MvcResult preview = mockMvc.perform(multipart("/api/v1/admin/loan-imports/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch.status").value("PREVIEW_READY"))
                .andExpect(jsonPath("$.batch.createdCount").value(1))
                .andExpect(jsonPath("$.rows[0].action").value("CREATE"))
                .andReturn();

        JsonNode previewJson = objectMapper.readTree(preview.getResponse().getContentAsString());
        long batchId = previewJson.get("batch").get("id").asLong();

        mockMvc.perform(post("/api/v1/admin/loan-imports/{batchId}/execute", batchId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch.status").value("COMPLETED"))
                .andExpect(jsonPath("$.batch.createdCount").value(1))
                .andExpect(jsonPath("$.rows[0].action").value("CREATE"))
                .andExpect(jsonPath("$.rows[0].loanId").isNumber());

        MvcResult loans = mockMvc.perform(get("/api/v1/loans")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode loansJson = objectMapper.readTree(loans.getResponse().getContentAsString()).get("content");
        assertThat(loansJson.toString()).contains("Imported Demo Borrower");

        mockMvc.perform(get("/api/v1/admin/loan-imports/{batchId}/rows", batchId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].externalLoanId").value("LN-200001"));
    }

    @Test
    void analystCannotAccessLoanImportEndpoints() throws Exception {
        String analyst = login("analyst@demo.com", "Demo123!");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "loan-import.csv",
                "text/csv",
                (
                        "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate,status\n" +
                        "CORE_BANKING,LN-300001,Forbidden Borrower,900000.00,2025-02-01,ACTIVE\n"
                ).getBytes()
        );

        mockMvc.perform(multipart("/api/v1/admin/loan-imports/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + analyst)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/loan-imports")
                        .header("Authorization", "Bearer " + analyst))
                .andExpect(status().isForbidden());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }
}
