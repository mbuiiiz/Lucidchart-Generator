package com.group16.aetherxmlbridge.controller;

import com.group16.aetherxmlbridge.service.AppUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final AppUserService appUserService;

    public AuthController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @PostMapping("/register")
    public String register(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model,
            HttpServletRequest request
    ) {
        fullName = fullName == null ? "" : fullName.trim();
        email = email == null ? "" : email.trim().toLowerCase();

        if (fullName.isBlank()) {
            model.addAttribute("error", "Full name is required");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }

        if (email.isBlank()) {
            model.addAttribute("error", "Email is required");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }

        if (password == null || password.isBlank()) {
            model.addAttribute("error", "Password is required");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }

        if (password.length() < 8) {
            model.addAttribute("error", "Password must be at least 8 characters");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }

        try {
            appUserService.registerUser(fullName, email, password);

            UserDetails userDetails = appUserService.loadUserByUsername(email);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", context);

            return "redirect:/dashboard";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }
    }
}
