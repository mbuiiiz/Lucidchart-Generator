package com.group16.aetherxmlbridge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

  @GetMapping("/")
  public String getLanding(){
    return "index";
  }

}
