package com.stayease.controller;

import com.stayease.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MainController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class MainControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void dashboard_whenAdmin_redirectsToAdminDashboard() throws Exception
    {
        mockMvc.perform(get("/main"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void dashboard_whenManager_redirectsToManagerDashboard() throws Exception
    {
        mockMvc.perform(get("/main"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/dashboard"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void dashboard_whenCustomer_redirectsToHome() throws Exception
    {
        mockMvc.perform(get("/main"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "CUSTOMER")
    void accessDenied_returnsForbiddenPage() throws Exception
    {
        mockMvc.perform(get("/error/403"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/403"));
    }
}