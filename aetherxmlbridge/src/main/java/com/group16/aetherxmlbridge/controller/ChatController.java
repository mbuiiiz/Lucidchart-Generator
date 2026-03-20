package com.group16.aetherxmlbridge.controller;

import com.google.api.client.util.Value;
import org.aspectj.apache.bcel.util.ClassPath;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/ai")
public class ChatController {
    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) throws IOException {
        String systemPrompt = new ClassPathResource("prompts/chat.txt")
                .getContentAsString(StandardCharsets.UTF_8);

        this.chatClient = builder
                .defaultSystem(systemPrompt)
                .build();
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
