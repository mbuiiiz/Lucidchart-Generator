package com.group16.aetherxmlbridge.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/ai")
public class ChatController {
  
  private final ChatClient chatClient;

  public ChatController(ChatClient.Builder builder){
    this.chatClient = builder.build();
  }

  // Testing endpoints
  @PostMapping("/chat")
  public String chat(@RequestBody String userMessage) {
      return chatClient.prompt()
        .user(userMessage)
        .call()
        .content();
  }
}
