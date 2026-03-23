package com.group16.aetherxmlbridge.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

import com.group16.aetherxmlbridge.service.AppUserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;


@Controller
public class ProfileController {

    private final AppUserService appUserService;

    public ProfileController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @PostMapping("/profile/update-name")
    public String updateName(@RequestParam("fullName") String fullName, Principal principal) {
        if (principal != null && fullName != null && !fullName.trim().isEmpty()) {
            appUserService.updateFullName(principal.getName(), fullName.trim());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/update-phone")
    public String updatePhone(@RequestParam("phoneNumber") String phoneNumber, Principal principal) {
        if (principal != null) {
            appUserService.updatePhoneNumber(principal.getName(), phoneNumber == null ? null : phoneNumber.trim());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/delete-account")
    public String deleteAccount(@RequestParam("password") String password,
                                Principal principal,
                                HttpServletRequest request) throws ServletException {
    
        try {
            if (principal != null && password != null && !password.trim().isEmpty()) {
                appUserService.deleteAccount(principal.getName(), password.trim());
                request.logout();
                return "redirect:/";
            }
        } catch (IllegalArgumentException e) {
            return "redirect:/profile?deleteError=1";
        }
    
        return "redirect:/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(
            @RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("repeatPassword") String repeatPassword,
            Principal principal) {
    
        try {
            if (!newPassword.equals(repeatPassword)) {
                return "redirect:/profile?passwordError=Passwords+do+not+match";
            }
    
            appUserService.changePassword(principal.getName(), oldPassword, newPassword);
            return "redirect:/profile?passwordSuccess=1";
    
        } catch (IllegalArgumentException e) {
            return "redirect:/profile?passwordError=" + e.getMessage().replace(" ", "+");
        }
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email, Model model) {
        String token = appUserService.createPasswordResetToken(email);
    
        if (token == null) {
            model.addAttribute("message", "If this email exists, reset instructions were sent");
            return "forgot-password";
        }
    
        return "redirect:/reset-password?token=" + token;
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(
            @RequestParam("token") String token,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("repeatPassword") String repeatPassword,
            Model model) {
    
        try {
            if (!newPassword.equals(repeatPassword)) {
                model.addAttribute("token", token);
                model.addAttribute("error", "Passwords do not match");
                return "reset-password";
            }
    
            appUserService.resetPasswordByToken(token, newPassword);
            return "redirect:/login?resetSuccess=1";
    
        } catch (IllegalArgumentException e) {
            model.addAttribute("token", token);
            model.addAttribute("error", e.getMessage());
            return "reset-password";
        }
    }
}
