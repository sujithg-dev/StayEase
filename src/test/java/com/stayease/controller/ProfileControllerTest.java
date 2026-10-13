package com.stayease.controller;

import com.stayease.config.SecurityConfig;
import com.stayease.dto.ChangePasswordRequest;
import com.stayease.dto.ProfileRequest;
import com.stayease.model.User;
import com.stayease.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class ProfileControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private User mockUser()
    {
        User user = new User();
        user.setUserId("customer-1");
        user.setUserName("Test Customer");
        user.setUserEmail("customer@example.com");
        return user;
    }

    @BeforeEach
    void setUp()
    {
        when(userService.getUserByUserEmail("customer@example.com")).thenReturn(mockUser());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void profilePageReturnsProfileView() throws Exception
    {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/profile"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("profileRequest"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void changePasswordPageReturnsPasswordView() throws Exception
    {
        mockMvc.perform(get("/profile/password"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/change-password"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("changePasswordRequest"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void updateProfileRedirectsWhenSuccessful() throws Exception
    {
        mockMvc.perform(post("/profile/update")
                        .param("userName", "Updated Customer")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?updated"));

        verify(userService).updateProfile(eq("customer@example.com"), any(ProfileRequest.class));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void updateProfileReturnsProfileViewWhenValidationFails() throws Exception
    {
        mockMvc.perform(post("/profile/update")
                        .param("userName", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/profile"))
                .andExpect(model().attributeHasFieldErrors("profileRequest", "userName"));

        verify(userService, never()).updateProfile(anyString(), any(ProfileRequest.class));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void changePasswordRedirectsWhenSuccessful() throws Exception
    {
        mockMvc.perform(post("/profile/password")
                        .param("currentPassword", "OldPass123!")
                        .param("newPassword", "NewPass123!")
                        .param("confirmPassword", "NewPass123!")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/password?updated"));

        verify(userService).changePassword(eq("customer@example.com"), any(ChangePasswordRequest.class));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void changePasswordReturnsViewWhenValidationFails() throws Exception
    {
        mockMvc.perform(post("/profile/password")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/change-password"))
                .andExpect(model().attributeHasFieldErrors("changePasswordRequest"));

        verify(userService, never()).changePassword(anyString(), any(ChangePasswordRequest.class));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    void changePasswordReturnsErrorWhenServiceThrows() throws Exception
    {
        doThrow(new RuntimeException("Current password is incorrect"))
                .when(userService)
                .changePassword(eq("customer@example.com"), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/profile/password")
                        .param("currentPassword", "WrongPass123!")
                        .param("newPassword", "NewPass123!")
                        .param("confirmPassword", "NewPass123!")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/change-password"))
                .andExpect(model().attribute("error", "Current password is incorrect"));
    }
}