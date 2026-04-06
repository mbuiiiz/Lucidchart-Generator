package com.group16.aetherxmlbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StoryAiControllerTest {

    private MockMvc mockMvc;
    private ChatClient.Builder builder;
    private ChatClient chatClient;

    @BeforeEach
    void setUp() throws Exception {
        builder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);

        when(builder.defaultSystem(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);

        StoryAiController controller = new StoryAiController(builder, new ObjectMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void extract_validStory_returnsProjectDataJson() throws Exception {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("""
            {"module":"User Login","trigger":"Submit Button","description":"Validates credentials and logs in the user"}
            """);

        String requestBody = """
            {
              "userStory": "As a user, I want to log in so that I can access my account"
            }
            """;

        mockMvc.perform(post("/api/ai/extract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"module\":\"User Login\"")))
                .andExpect(content().string(containsString("\"trigger\":\"Submit Button\"")))
                .andExpect(content().string(containsString("\"description\":\"Validates credentials and logs in the user\"")));
    }

    @Test
    void extract_fencedJsonResponse_stillParsesSuccessfully() throws Exception {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("""
            ```json
            {"module":"Payments","trigger":"Checkout Click","description":"Creates payment workflow"}
            ```
            """);

        String requestBody = """
            {
              "userStory": "As a customer, I want checkout to trigger payment processing"
            }
            """;

        mockMvc.perform(post("/api/ai/extract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"module\":\"Payments\"")))
                .andExpect(content().string(containsString("\"trigger\":\"Checkout Click\"")))
                .andExpect(content().string(containsString("\"description\":\"Creates payment workflow\"")));
    }

    @Test
    void extract_blankUserStory_returnsBadRequest() throws Exception {
        String requestBody = """
            {
              "userStory": "   "
            }
            """;

        mockMvc.perform(post("/api/ai/extract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("userStory is required")));
    }

    @Test
    void extract_invalidAiJson_returnsBadRequest() throws Exception {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("not json");

        String requestBody = """
            {
              "userStory": "As a user, I want to log in so that I can access my account"
            }
            """;

        mockMvc.perform(post("/api/ai/extract")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Something went wrong")));
    }

    @Test
    void storyToXml_validStory_returnsXml() throws Exception {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("""
            {"module":"Payments","trigger":"Checkout Click","description":"Creates payment workflow"}
            """);

        String requestBody = """
            {
              "userStory": "As a customer, I want checkout to trigger payment processing"
            }
            """;

        mockMvc.perform(post("/api/ai/story-to-xml")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<diagram>")))
                .andExpect(content().string(containsString("<node>Payments</node>")))
                .andExpect(content().string(containsString("<node>Checkout Click</node>")))
                .andExpect(content().string(containsString("<description>Creates payment workflow</description>")))
                .andExpect(content().string(containsString("<connection from=\"Checkout Click\" to=\"Payments\"/>")));
    }

    @Test
    void storyToXml_blankUserStory_returnsBadRequest() throws Exception {
        String requestBody = """
            {
              "userStory": ""
            }
            """;

        mockMvc.perform(post("/api/ai/story-to-xml")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("userStory is required")));
    }

    @Test
    void storyToXml_invalidAiJson_returnsXmlErrorResponse() throws Exception {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("not json");

        String requestBody = """
            {
              "userStory": "As a customer, I want checkout to trigger payment processing"
            }
            """;

        mockMvc.perform(post("/api/ai/story-to-xml")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<error>")));
    }

    @Test
    void storyToXml_escapesXmlSpecialCharacters() throws Exception {
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("""
            {"module":"Payments & Billing","trigger":"Checkout <Click>","description":"Uses \\"card\\" validation & checks"}
            """);

        String requestBody = """
            {
              "userStory": "As a customer, I want checkout to safely process payments"
            }
            """;

        mockMvc.perform(post("/api/ai/story-to-xml")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<node>Payments &amp; Billing</node>")))
                .andExpect(content().string(containsString("<node>Checkout &lt;Click&gt;</node>")))
                .andExpect(content().string(containsString("<description>Uses &quot;card&quot; validation &amp; checks</description>")));
    }
}