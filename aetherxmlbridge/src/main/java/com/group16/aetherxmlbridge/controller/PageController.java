package com.group16.aetherxmlbridge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;



/**
 * This file is for returning thymeleaf templates to the user when accessing a certain
 * path, do not put auth & data processing endpoints in here please
 */
@Controller
public class PageController {

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
  public String getUserDashboard(){
    return "dashboard";
  }  

}
