package com.group16.aetherxmlbridge.controller;

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
    public String getAllUsers(Model model) {
        model.addAttribute("users", appUserRepository.findAll());
        return "admin-users";
    }
}