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
import com.group16.aetherxmlbridge.model.ZohoScopeData;
import com.group16.aetherxmlbridge.service.ZohoApiService;

import org.springframework.web.bind.annotation.RequestParam;

import com.group16.aetherxmlbridge.service.PhoneMaskingService;

/**
 * This file is for returning thymeleaf templates to the user when accessing a certain
 * path, do not put auth & data processing endpoints in here please
 */
@Controller
public class PageController {
  private final AppUserRepository appUserRepository;
  private final ZohoApiService zohoApiService;
  private final PhoneMaskingService phoneMaskingService;

public PageController(
    AppUserRepository appUserRepository,
    ZohoApiService zohoApiService,
    PhoneMaskingService phoneMaskingService
) {
  this.appUserRepository = appUserRepository;
  this.zohoApiService = zohoApiService;
  this.phoneMaskingService = phoneMaskingService;
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

    model.addAttribute("activePage", "dashboard");
    return "dashboard";
  }

      @GetMapping("/profile")
      public String getProfilePage(
          Model model,
          Principal principal,
          @RequestParam(value = "passwordError", required = false) String passwordError,
          @RequestParam(value = "passwordSuccess", required = false) String passwordSuccess,
          @RequestParam(value = "deleteError", required = false) String deleteError,
          @RequestParam(value = "phoneError", required = false) String phoneError,
          @RequestParam(value = "phoneSuccess", required = false) String phoneSuccess,
          @RequestParam(value = "nameSuccess", required = false) String nameSuccess
      ) {

      if (nameSuccess != null) {
        model.addAttribute("nameSuccess", true);
      }  

      if (phoneError != null) {
        model.addAttribute("phoneError", phoneError);
    }
    
    if (phoneSuccess != null) {
        model.addAttribute("phoneSuccess", "Phone number updated successfully");
    }

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
      
        if (user != null && user.getPhoneNumber() != null) {
          model.addAttribute("maskedPhoneNumber",
              phoneMaskingService.mask(user.getPhoneNumber()));
      
          model.addAttribute("formattedPhoneNumber",
              phoneMaskingService.format(user.getPhoneNumber()));
      }
      }
      
    }
  
    if (passwordError != null) {
      model.addAttribute("passwordError", passwordError);
    }
  
    if (passwordSuccess != null) {
      model.addAttribute("passwordSuccess", "Password updated successfully");
    }
  
    if (deleteError != null) {
      model.addAttribute("deleteError", "Incorrect password");
    }
  
    model.addAttribute("activePage", "profile");
    return "profile";
  }

  @GetMapping("/forgot-password")
  public String getForgotPasswordPage() {
      return "forgot-password";
  }

  @GetMapping("/reset-password")
  public String getResetPasswordPage(
      @RequestParam(value = "token", required = false) String token,
      @RequestParam(value = "error", required = false) String error,
      Model model
  ) {
      model.addAttribute("token", token);
  
      if (error != null) {
          model.addAttribute("error", error);
      }
  
      return "reset-password";
  }
  /**
   * Avoid displaying all Zoho projects on dashboard,
   * Display a few, then allow user to view the rest in /projects page
   * where there is more detail
   */
  @GetMapping("/projects")
  public String getAllProjects(Model model, Principal principal){
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
    model.addAttribute("activePage", "projects");
    return "projects";
  }

  /**
   * Automation Scope page - displays scope data and allows generating
   * automation flow diagrams with triggers, conditions, and actions.
   */
  @GetMapping("/automation-scope")
  public String getAutomationScope(Model model, Principal principal) {
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

        boolean zohoConnected = user != null && user.getZohoAccessToken() != null;
        model.addAttribute("zohoConnected", zohoConnected);

        if (zohoConnected) {
          List<ZohoScopeData> scopeData = zohoApiService.fetchScopeData(user, null);
          model.addAttribute("scopeData", scopeData);
        }
      }
    }
    model.addAttribute("activePage", "automation-scope");
    return "automation-scope";
  }

}

