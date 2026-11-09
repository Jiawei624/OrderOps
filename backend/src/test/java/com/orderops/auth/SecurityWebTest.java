package com.orderops.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IdentityController.class)
@Import(SecurityConfiguration.class)
class SecurityWebTest {
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @MockitoBean AccountMapper accounts;

    @BeforeEach
    void configureAccount() {
        Account buyer = new Account();
        buyer.setId(10); buyer.setUsername("buyer"); buyer.setRole("BUYER"); buyer.setEnabled(true);
        buyer.setPasswordHash(encoder.encode("buyer-password"));
        when(accounts.findByUsername(anyString())).thenReturn(null);
        when(accounts.findByUsername("buyer")).thenReturn(buyer);
    }

    @Test
    void loginRequiresCsrfAndRejectsInvalidCredentials() throws Exception {
        mvc.perform(post("/api/auth/login").param("username", "buyer").param("password", "buyer-password"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").with(csrf()).param("username", "buyer").param("password", "wrong"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
        mvc.perform(post("/api/auth/login").with(csrf()).param("username", "buyer").param("password", "buyer-password"))
                .andExpect(status().isOk());
    }

    @Test
    void endpointsEnforceAuthenticationAndRole() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
        mvc.perform(get("/api/buyer/me").with(user("buyer").roles("BUYER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(10));
        mvc.perform(get("/api/operator/me").with(user("buyer").roles("BUYER")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mvc.perform(post("/api/auth/logout").with(user("buyer").roles("BUYER"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").with(user("buyer").roles("BUYER")).with(csrf())).andExpect(status().isNoContent());
    }
}
