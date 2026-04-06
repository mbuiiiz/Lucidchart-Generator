package com.group16.aetherxmlbridge.controller;

import java.security.Principal;

import com.group16.aetherxmlbridge.model.AppUser;
import com.group16.aetherxmlbridge.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final AppUserRepository appUserRepository;

    @GetMapping("/admin/users")
    public String getAllUsers(Model model, Principal principal) {
        model.addAttribute("users", appUserRepository.findAll());

        if (principal != null) {
            AppUser currentUser = appUserRepository.findByEmail(principal.getName()).orElse(null);
            model.addAttribute("currentUser", currentUser);
        }

        model.addAttribute("activePage", "admin-users");
        return "admin-users";
    }
}
