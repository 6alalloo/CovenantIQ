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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommentAndActivityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void commentCreateDeleteAndLoanActivityWorks() throws Exception {
        String analyst = login("analyst@demo.com", "Demo123!");
        long loanId = createLoan(analyst);

        MvcResult createComment = mockMvc.perform(post("/api/v1/loans/{loanId}/comments", loanId)
                        .header("Authorization", "Bearer " + analyst)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commentText":"Initial review note"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long commentId = objectMapper.readTree(createComment.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/loans/{loanId}/comments", loanId)
                        .header("Authorization", "Bearer " + analyst))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/loans/{loanId}/comments/{commentId}", loanId, commentId)
                        .header("Authorization", "Bearer " + analyst))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/loans/{loanId}/activity", loanId)
                        .header("Authorization", "Bearer " + analyst))
                .andExpect(status().isOk());
    }

    @Test
    void globalActivityEndpointRequiresRiskLeadOrAdmin() throws Exception {
        String analyst = login("analyst@demo.com", "Demo123!");
        String riskLead = login("risklead@demo.com", "Demo123!");

        mockMvc.perform(get("/api/v1/activity")
                        .param("start", "2025-01-01")
                        .param("end", "2027-01-01")
                        .header("Authorization", "Bearer " + analyst))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/activity")
                        .param("start", "2025-01-01")
                        .param("end", "2027-01-01")
                        .header("Authorization", "Bearer " + riskLead))
                .andExpect(status().isOk());
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
                                {"borrowerName":"Comments Loan","principalAmount":1000000,"startDate":"2025-02-01"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }
}
