package com.group16.aetherxmlbridge.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatControllerTest {

    private MockMvc mockMvc;
    private ChatClient.CallResponseSpec callResponseSpec;

    @BeforeEach
    void setUp() throws Exception {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient.ChatClientRequestSpec promptSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(builder.defaultSystem(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.call()).thenReturn(callResponseSpec);

        ChatController controller = new ChatController(builder);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void chat_validMessage_returnsAiResponse() throws Exception {
        when(callResponseSpec.content()).thenReturn("Hello from AI");

        mockMvc.perform(post("/api/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"How do I generate a diagram?\""))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello from AI"));
    }

    @Test
    void chat_aiThrowsException_returnsFallbackErrorMessage() throws Exception {
        when(callResponseSpec.content()).thenThrow(new RuntimeException("API quota exceeded"));

        mockMvc.perform(post("/api/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"Help me with the dashboard\""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Something went wrong: API quota exceeded")));
    }

    @Test
    void chat_emptyMessage_stillReturnsMockedAiResponse() throws Exception {
        when(callResponseSpec.content()).thenReturn("Please enter more details");

        mockMvc.perform(post("/api/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"\""))
                .andExpect(status().isOk())
                .andExpect(content().string("Please enter more details"));
    }
}