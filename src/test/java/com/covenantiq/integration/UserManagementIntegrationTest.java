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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCanManageUsersAndAnalystCannot() throws Exception {
        String admin = login("admin@demo.com", "Demo123!");
        String analyst = login("analyst@demo.com", "Demo123!");

        MvcResult create = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"new.user@demo.com",
                                  "password":"Strong123!",
                                  "email":"new.user@demo.com",
                                  "roles":["ANALYST"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long userId = objectMapper.readTree(create.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + analyst))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/users/{id}/roles", userId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roles":["RISK_LEAD"]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/users/{id}", userId)
                        .header("Authorization", "Bearer " + admin))
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
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }
}
