package com.stayease.controller;

import com.stayease.dto.RegisterRequest;
import com.stayease.dto.ResetPasswordRequest;
import com.stayease.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController
{
    private final AuthService authService;

    @GetMapping("/register")
    public String showRegisterPage(Model model)
    {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String registered,
            @RequestParam(required = false) String reset,
            Model model)
    {
        if (error != null) { model.addAttribute("error", "Invalid email, password, or your account is disabled!"); }
        if (logout != null) { model.addAttribute("message", "You have been logged out successfully."); }
        if (registered != null) { model.addAttribute("message", "Registration successful. Please login."); }
        if (reset != null) { model.addAttribute("message", "Password reset successfully. Please login."); }

        return "auth/login";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword()
    {
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model)
    {
        if (!authService.isValidToken(token))
        {
            model.addAttribute("error", "This password reset link is invalid or has expired.");

            return "auth/reset-password";
        }

        model.addAttribute("token", token);
        model.addAttribute("resetPasswordRequest", new ResetPasswordRequest());

        return "auth/reset-password";
    }

    @PostMapping("/register")
    public String register(@Valid RegisterRequest request, BindingResult result, Model model)
    {
        if (result.hasErrors()) { return "auth/register"; }

        try
        {
            authService.userRegister(request);

            return "redirect:/login?registered";
        }
        catch (RuntimeException e)
        {
            model.addAttribute("error", e.getMessage());

            return "auth/register";
        }
    }
    @PostMapping("/forgot-password")
    public String sendResetLink(@RequestParam String email, Model model)
    {
        authService.createResetToken(email.trim());

        model.addAttribute("message", "If an account exists with that email, " + email + " a password reset link has been sent.");

        return "auth/forgot-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String token,
            @ModelAttribute("resetPasswordRequest")
            @Valid ResetPasswordRequest request,
            BindingResult bindingResult,
            Model model)
    {
        if (!authService.isValidToken(token))
        {
            model.addAttribute("error", "This password reset link is invalid or has expired.");

            return "auth/reset-password";
        }

        if (bindingResult.hasErrors())
        {
            model.addAttribute("token", token);

            return "auth/reset-password";
        }

        try
        {
            authService.resetPassword(token, request);
        }
        catch (RuntimeException exception)
        {
            model.addAttribute("token", token);
            model.addAttribute("error", exception.getMessage());

            return "auth/reset-password";
        }

        return "redirect:/login?reset";
    }
}