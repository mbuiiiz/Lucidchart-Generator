package com.group16.aetherxmlbridge.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/ai")
public class ChatController {
  
  private final ChatClient chatClient;

  public ChatController(ChatClient.Builder builder){
    this.chatClient = builder.build();
  }

  // Testing endpoints
  @GetMapping("/pirates")
  public String getPirates() {
      return chatClient.prompt()
        .user("Generate the names of 5 famous pirates.")
        .call()
        .content();
  }
}
