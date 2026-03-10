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

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void adminPreviewRejectsMalformedCsv() throws Exception {
        String admin = login("admin@demo.com", "Demo123!");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "loan-import.csv",
                "text/csv",
                (
                        "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate,status\n" +
                        "CORE_BANKING,\"unterminated,Bad Row,1250000.00,2025-01-15,ACTIVE\n"
                ).getBytes()
        );

        mockMvc.perform(multipart("/api/v1/admin/loan-imports/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.detail").value("Unable to read import CSV"));
    }

    @Test
    void adminPreviewRejectsEmptyFile() throws Exception {
        String admin = login("admin@demo.com", "Demo123!");
        MockMultipartFile file = new MockMultipartFile("file", "loan-import.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/api/v1/admin/loan-imports/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.detail").value("CSV file is required"));
    }

    @Test
    void adminPreviewRejectsMissingHeaders() throws Exception {
        String admin = login("admin@demo.com", "Demo123!");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "loan-import.csv",
                "text/csv",
                (
                        "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate\n" +
                        "CORE_BANKING,LN-400002,Missing Status,900000.00,2025-02-01\n"
                ).getBytes()
        );

        mockMvc.perform(multipart("/api/v1/admin/loan-imports/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.detail").value("Missing required column: status"));
    }

    @Test
    void adminPreviewFlagsInvalidRowValues() throws Exception {
        String admin = login("admin@demo.com", "Demo123!");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "loan-import.csv",
                "text/csv",
                (
                        "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate,status,sourceUpdatedAt\n" +
                        "CORE_BANKING,LN-400003,Invalid Decimal,not-a-number,2025-01-15,ACTIVE,2026-03-08T10:15:00Z\n"
                ).getBytes()
        );

        mockMvc.perform(multipart("/api/v1/admin/loan-imports/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch.validRows").value(0))
                .andExpect(jsonPath("$.batch.invalidRows").value(1))
                .andExpect(jsonPath("$.rows[0].action").value("ERROR"))
                .andExpect(jsonPath("$.rows[0].validationMessage").value("Invalid decimal value for principalAmount"));
    }

    @Test
    void adminCanExecuteBatchWithMixedValidAndInvalidRows() throws Exception {
        String admin = login("admin@demo.com", "Demo123!");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "loan-import-mixed.csv",
                "text/csv",
                (
                        "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate,status,sourceUpdatedAt\n" +
                        "CORE_BANKING,LN-500001,First Borrower,1250000.00,2025-01-15,ACTIVE,2026-03-08T10:15:00Z\n" +
                        "CORE_BANKING,LN-500001,Duplicate Borrower,1250000.00,2025-01-15,ACTIVE,2026-03-08T10:15:00Z\n"
                ).getBytes()
        );

        MvcResult preview = mockMvc.perform(multipart("/api/v1/admin/loan-imports/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch.validRows").value(1))
                .andExpect(jsonPath("$.batch.invalidRows").value(1))
                .andExpect(jsonPath("$.rows[0].action").value("CREATE"))
                .andExpect(jsonPath("$.rows[1].action").value("ERROR"))
                .andReturn();

        long batchId = objectMapper.readTree(preview.getResponse().getContentAsString()).get("batch").get("id").asLong();

        mockMvc.perform(post("/api/v1/admin/loan-imports/{batchId}/execute", batchId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch.status").value("COMPLETED"))
                .andExpect(jsonPath("$.batch.createdCount").value(1))
                .andExpect(jsonPath("$.batch.failedCount").value(1))
                .andExpect(jsonPath("$.rows[0].action").value("CREATE"))
                .andExpect(jsonPath("$.rows[1].action").value("ERROR"));
    }

    @Test
    void adminCanListLoanImportHistory() throws Exception {
        String admin = login("admin@demo.com", "Demo123!");
        preview(admin, "history-one.csv", "LN-600001", "History Borrower One", "ACTIVE");
        preview(admin, "history-two.csv", "LN-600002", "History Borrower Two", "ACTIVE");

        MvcResult result = mockMvc.perform(get("/api/v1/admin/loan-imports")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(json.isArray());
        assertTrue(json.size() >= 2);
        Set<String> fileNames = new HashSet<>();
        for (JsonNode item : json) {
            fileNames.add(item.get("fileName").asText());
        }
        assertTrue(fileNames.contains("history-one.csv"));
        assertTrue(fileNames.contains("history-two.csv"));
    }

    @Test
    void adminCanFetchLoanImportBatchDetail() throws Exception {
        String admin = login("admin@demo.com", "Demo123!");
        long batchId = preview(admin, "detail.csv", "LN-700001", "Detail Borrower", "ACTIVE");

        mockMvc.perform(get("/api/v1/admin/loan-imports/{batchId}", batchId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(batchId))
                .andExpect(jsonPath("$.fileName").value("detail.csv"))
                .andExpect(jsonPath("$.status").value("PREVIEW_READY"))
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.validRows").value(1));
    }

    @Test
    void reimportOfSameDataReturnsUnchangedAndDoesNotCreateDuplicateLoan() throws Exception {
        String admin = login("admin@demo.com", "Demo123!");
        String externalLoanId = "LN-800001";
        String borrowerName = "Stable Borrower";

        long firstBatchId = preview(admin, "unchanged-first.csv", externalLoanId, borrowerName, "ACTIVE");
        mockMvc.perform(post("/api/v1/admin/loan-imports/{batchId}/execute", firstBatchId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch.createdCount").value(1));

        MvcResult secondPreview = mockMvc.perform(multipart("/api/v1/admin/loan-imports/preview")
                        .file(importFile("unchanged-second.csv", externalLoanId, borrowerName, "ACTIVE"))
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].action").value("UNCHANGED"))
                .andReturn();

        long secondBatchId = objectMapper.readTree(secondPreview.getResponse().getContentAsString()).get("batch").get("id").asLong();

        mockMvc.perform(post("/api/v1/admin/loan-imports/{batchId}/execute", secondBatchId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch.status").value("COMPLETED"))
                .andExpect(jsonPath("$.batch.createdCount").value(0))
                .andExpect(jsonPath("$.batch.updatedCount").value(0))
                .andExpect(jsonPath("$.batch.unchangedCount").value(1))
                .andExpect(jsonPath("$.rows[0].action").value("UNCHANGED"));

        JsonNode loans = loanContent(admin);
        assertEquals(1, countLoans(loans, externalLoanId));
        assertEquals(1, countBorrowers(loans, borrowerName));
    }

    @Test
    void reimportUpdatesExistingLoanInsteadOfCreatingAnotherLoan() throws Exception {
        String admin = login("admin@demo.com", "Demo123!");
        String externalLoanId = "LN-800002";

        long firstBatchId = preview(admin, "update-first.csv", externalLoanId, "Original Borrower", "ACTIVE");
        mockMvc.perform(post("/api/v1/admin/loan-imports/{batchId}/execute", firstBatchId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch.createdCount").value(1));

        MvcResult secondPreview = mockMvc.perform(multipart("/api/v1/admin/loan-imports/preview")
                        .file(importFile("update-second.csv", externalLoanId, "Updated Borrower", "ACTIVE"))
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].action").value("UPDATE"))
                .andReturn();

        long secondBatchId = objectMapper.readTree(secondPreview.getResponse().getContentAsString()).get("batch").get("id").asLong();

        mockMvc.perform(post("/api/v1/admin/loan-imports/{batchId}/execute", secondBatchId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch.status").value("COMPLETED"))
                .andExpect(jsonPath("$.batch.createdCount").value(0))
                .andExpect(jsonPath("$.batch.updatedCount").value(1))
                .andExpect(jsonPath("$.batch.unchangedCount").value(0))
                .andExpect(jsonPath("$.rows[0].action").value("UPDATE"));

        JsonNode loans = loanContent(admin);
        assertEquals(1, countLoans(loans, externalLoanId));
        assertEquals(1, countBorrowers(loans, "Updated Borrower"));
        assertEquals(0, countBorrowers(loans, "Original Borrower"));
    }

    @Test
    void previewRejectsForbiddenClosedToActiveReopen() throws Exception {
        String admin = login("admin@demo.com", "Demo123!");
        String externalLoanId = "LN-900001";

        long firstBatchId = preview(admin, "closed-first.csv", externalLoanId, "Closed Borrower", "CLOSED");
        mockMvc.perform(post("/api/v1/admin/loan-imports/{batchId}/execute", firstBatchId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch.createdCount").value(1));

        mockMvc.perform(multipart("/api/v1/admin/loan-imports/preview")
                        .file(importFile("reopen.csv", externalLoanId, "Closed Borrower", "ACTIVE"))
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch.validRows").value(0))
                .andExpect(jsonPath("$.batch.invalidRows").value(1))
                .andExpect(jsonPath("$.rows[0].action").value("ERROR"))
                .andExpect(jsonPath("$.rows[0].validationMessage").value("Imported loan LN-900001 cannot transition from CLOSED to ACTIVE automatically"));
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

    private long preview(String token, String fileName, String externalLoanId, String borrowerName, String status) throws Exception {
        MvcResult preview = mockMvc.perform(multipart("/api/v1/admin/loan-imports/preview")
                        .file(importFile(fileName, externalLoanId, borrowerName, status))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(preview.getResponse().getContentAsString()).get("batch").get("id").asLong();
    }

    private MockMultipartFile importFile(String fileName, String externalLoanId, String borrowerName, String status) {
        return new MockMultipartFile(
                "file",
                fileName,
                "text/csv",
                (
                        "sourceSystem,externalLoanId,borrowerName,principalAmount,startDate,status,sourceUpdatedAt\n" +
                        "CORE_BANKING,%s,%s,1250000.00,2025-01-15,%s,2026-03-08T10:15:00Z\n"
                ).formatted(externalLoanId, borrowerName, status).getBytes()
        );
    }

    private JsonNode loanContent(String token) throws Exception {
        MvcResult loans = mockMvc.perform(get("/api/v1/loans")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loans.getResponse().getContentAsString()).get("content");
    }

    private int countLoans(JsonNode loans, String externalLoanId) {
        int count = 0;
        for (JsonNode loan : loans) {
            JsonNode externalLoanIdNode = loan.get("externalLoanId");
            if (externalLoanIdNode != null && externalLoanId.equals(externalLoanIdNode.asText())) {
                count++;
            }
        }
        return count;
    }

    private int countBorrowers(JsonNode loans, String borrowerName) {
        int count = 0;
        for (JsonNode loan : loans) {
            JsonNode borrowerNode = loan.get("borrowerName");
            if (borrowerNode != null && borrowerName.equals(borrowerNode.asText())) {
                count++;
            }
        }
        return count;
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
