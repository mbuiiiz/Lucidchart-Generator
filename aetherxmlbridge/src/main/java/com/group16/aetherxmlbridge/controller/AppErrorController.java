package com.group16.aetherxmlbridge.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AppErrorController implements ErrorController {

  @RequestMapping("/error")
  public String handleError(HttpServletRequest request, Model model) {
    Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    Object errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

    model.addAttribute("statusCode", statusCode != null ? statusCode.toString() : "Unknown");
    model.addAttribute("errorMessage", errorMessage != null ? errorMessage.toString() : "An unexpected error occurred.");

    return "error";
  }
}
