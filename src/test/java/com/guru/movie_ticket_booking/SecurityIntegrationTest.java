package com.guru.movie_ticket_booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:securitydb")
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired
    MockMvc mvc;

    @Test
    void anonymousCannotBrowse() throws Exception {
        mvc.perform(get("/api/shows")).andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotAdminister() throws Exception {
        mvc.perform(post("/api/admin/cities").with(httpBasic("customer", "customer"))
                .contentType("application/json").content("{\"name\":\"Delhi\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateCity() throws Exception {
        mvc.perform(post("/api/admin/cities").with(httpBasic("admin", "admin"))
                .contentType("application/json").content("{\"name\":\"Delhi\"}"))
                .andExpect(status().isCreated());
    }
}
