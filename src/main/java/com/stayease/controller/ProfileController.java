package com.stayease.controller;

import com.stayease.dto.ChangePasswordRequest;
import com.stayease.dto.ProfileRequest;
import com.stayease.model.User;
import com.stayease.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController
{
    private final UserService userService;

    @ModelAttribute("user")
    public User getUser(Authentication authentication)
    {
        return userService.getUserByUserEmail(authentication.getName());
    }

    @GetMapping
    public String profile(@ModelAttribute("user") User user, Model model)
    {
        ProfileRequest profileRequest = new ProfileRequest();
        profileRequest.setUserName(user.getUserName());

        model.addAttribute("user", user);
        model.addAttribute("profileRequest", profileRequest);

        return "profile/profile";
    }

    @GetMapping("/password")
    public String changePasswordPage(Model model)
    {
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());

        return "profile/change-password";
    }

    @PostMapping("/update")
    public String updateProfile(
            @ModelAttribute("user") User user,
            @Valid @ModelAttribute("profileRequest")
            ProfileRequest profileRequest,
            BindingResult bindingResult,
            Authentication authentication,
            Model model)
    {
        if (bindingResult.hasErrors())
        {
            model.addAttribute("user", user);

            return "profile/profile";
        }

        userService.updateProfile(authentication.getName(), profileRequest);

        return "redirect:/profile?updated";
    }

    @PostMapping("/password")
    public String changePassword(
            @ModelAttribute("user") User user,
            @Valid @ModelAttribute("changePasswordRequest")
            ChangePasswordRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors())
        {
            return "profile/change-password";
        }

        try
        {
            userService.changePassword(user.getUserEmail(), request);

        }
        catch (RuntimeException exception)
        {
            model.addAttribute("error", exception.getMessage());

            return "profile/change-password";
        }

        return "redirect:/profile/password?updated";
    }
}