package com.group16.aetherxmlbridge.controller;

import java.security.Principal;
import java.util.List;
import org.springframework.ui.Model;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.group16.aetherxmlbridge.repository.AppUserRepository;
import com.group16.aetherxmlbridge.model.AppUser;
import com.group16.aetherxmlbridge.model.ZohoProject;
import com.group16.aetherxmlbridge.service.ZohoApiService;


/**
 * This file is for returning thymeleaf templates to the user when accessing a certain
 * path, do not put auth & data processing endpoints in here please
 */
@Controller
public class PageController {
  private final AppUserRepository appUserRepository;
  private final ZohoApiService zohoApiService;

  public PageController(AppUserRepository appUserRepository, ZohoApiService zohoApiService) {
    this.appUserRepository = appUserRepository;
    this.zohoApiService = zohoApiService;
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
      String email;
      if (principal instanceof OAuth2AuthenticationToken oauthToken) {
        // OAuth2 users: principal.getName() returns the subject ID, not email
        OAuth2User oauthUser = oauthToken.getPrincipal();
        email = oauthUser.getAttribute("Email"); // Zoho uses "Email"
        if (email == null) {
          email = oauthUser.getAttribute("email"); // Google uses "email"
        }
      } else {
        email = principal.getName(); // form login uses email as username
      }

      if (email != null) {
        AppUser user = appUserRepository.findByEmail(email).orElse(null);
        model.addAttribute("currentUser", user);
        
        // is zoho connected attribute
        model.addAttribute("zohoConnected", user != null && user.getZohoAccessToken() != null);
        // fetch projects from zoho 
        List<ZohoProject> projects = zohoApiService.fetchProjects(user); 
        // project attribute for render in dashboard 
        model.addAttribute("projects", projects);
      }
    }

    return "dashboard";
  }

@GetMapping("/profile")
public String getProfilePage(Model model, Principal principal) {

  if (principal != null) {
    String email;
    if (principal instanceof OAuth2AuthenticationToken oauthToken) {
      OAuth2User oauthUser = oauthToken.getPrincipal();
      email = oauthUser.getAttribute("Email");
      if (email == null) {
        email = oauthUser.getAttribute("email");
      }
    } else {
      email = principal.getName();
    }

    if (email != null) {
      AppUser user = appUserRepository.findByEmail(email).orElse(null);
      model.addAttribute("currentUser", user);
    }
  }

  return "profile";
}

}

