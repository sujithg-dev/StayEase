package com.stayease.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Objects;

@Controller
public class MainController
{
    @GetMapping("/main")
    public String dashboard(Authentication authentication)
    {
        String role = authentication
                .getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        if (Objects.equals(role, "ROLE_ADMIN"))
        {
            return "redirect:/admin/dashboard";
        }

        if (Objects.equals(role, "ROLE_MANAGER"))
        {
            return "redirect:/manager/dashboard";
        }

        return "redirect:/home";
    }

    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }
}