package com.group16.aetherxmlbridge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.group16.aetherxmlbridge.service.AppUserService;

@Controller
public class AuthController {

  private final AppUserService appUserService;

  public AuthController(AppUserService appUserService) {
    this.appUserService = appUserService;
  }

  // handle registration form submissions validation
  @PostMapping("/register")
  public String register(
      @RequestParam("fullName") String fullName,
      @RequestParam("email") String email,
      @RequestParam("password") String password,
      @RequestParam("confirmPassword") String confirmPassword,
      RedirectAttributes redirectAttributes
  ) {
    if (fullName == null || fullName.isBlank()) {
      redirectAttributes.addFlashAttribute("error", "Full name is required");
      return "redirect:/register";
    }

    if (email == null || email.isBlank()) {
      redirectAttributes.addFlashAttribute("error", "Email is required");
      return "redirect:/register";
    }

    if (password == null || password.isBlank()) {
      redirectAttributes.addFlashAttribute("error", "Password is required");
      return "redirect:/register";
    }

    if (password.length() < 8) {
      redirectAttributes.addFlashAttribute("error", "Password must be at least 8 characters");
      return "redirect:/register";
    }

    if (!password.equals(confirmPassword)) {
      redirectAttributes.addFlashAttribute("error", "Passwords do not match");
      return "redirect:/register";
    }

    try {
      appUserService.registerUser(fullName.trim(), email.trim().toLowerCase(), password);
      redirectAttributes.addFlashAttribute("success", "Account created. Please log in.");
      return "redirect:/login";
    } catch (IllegalArgumentException ex) {
      redirectAttributes.addFlashAttribute("error", ex.getMessage());
      return "redirect:/register";
    }
  }
}
