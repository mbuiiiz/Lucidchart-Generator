package com.group16.aetherxmlbridge.controller;

import java.security.Principal;
import org.springframework.ui.Model;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.group16.aetherxmlbridge.repository.AppUserRepository;
import com.group16.aetherxmlbridge.model.AppUser;




/**
 * This file is for returning thymeleaf templates to the user when accessing a certain
 * path, do not put auth & data processing endpoints in here please
 */
@Controller
public class PageController {
  private final AppUserRepository appUserRepository;

  public PageController(AppUserRepository appUserRepository) {
    this.appUserRepository = appUserRepository;
  }
  
  @GetMapping("/")
  public String getLanding(){
    return "index";
  }

  @GetMapping("/login")
  public String getLoginPage() {
    return "login";
  }

  @GetMapping("/register")
  public String getRegisterPage() {
    return "register";
  }

  @GetMapping("/dashboard")
  public String getUserDashboard(Model model, Principal principal){

    if (principal != null) {
      AppUser user = appUserRepository.findByEmail(principal.getName()).orElse(null);
      model.addAttribute("currentUser", user);
    }

    return "dashboard";
  }

}

