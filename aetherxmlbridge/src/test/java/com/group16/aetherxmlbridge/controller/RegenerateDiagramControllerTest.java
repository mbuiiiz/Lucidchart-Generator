package com.group16.aetherxmlbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RegenerateDiagramControllerTest {

    private MockMvc mockMvc;
    private ChatClient.CallResponseSpec callResponseSpec;

    private static final String VALID_AI_JSON = """
            {"trigger":"User clicks Login","module":"Auth Module","description":"Validates and logs in user"}
            """;

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

        RegenerateDiagramController controller = new RegenerateDiagramController(builder, new ObjectMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    void regenerateDiagram_validInputs_returnsDrawioXml() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/regenerate-diagram")
                .param("projectId", "P-001")
                .param("dealName", "Acme Corp")
                .param("stage", "Proposal")
                .param("companyContext", "B2B SaaS company")
                .param("productPurpose", "Project management tool")
                .param("productionNotes", "Needs SSO")
                .param("customerConcerns", "Data security")
                .param("customPrompt", "Make it more detailed"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<mxfile")))
                .andExpect(content().string(containsString("<diagram")))
                .andExpect(content().string(containsString("Auth Module")))
                .andExpect(content().string(containsString("User clicks Login")))
                .andExpect(content().string(containsString("Validates and logs in user")));
    }

    @Test
    void regenerateDiagram_validInputs_contentDispositionHeaderContainsRegeneratedFilename() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/regenerate-diagram")
                .param("dealName", "Acme Corp"))
                .andExpect(header().string("Content-Disposition",
                        containsString("Acme_Corp_regenerated.drawio")));
    }

    @Test
    void regenerateDiagram_validInputs_dealNameAppearsInDiagram() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/regenerate-diagram")
                .param("dealName", "Acme Corp"))
                .andExpect(content().string(containsString("Deal: Acme Corp")));
    }

    @Test
    void regenerateDiagram_validInputs_projectIdAppearsInDiagram() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/regenerate-diagram")
                .param("projectId", "ZP-999"))
                .andExpect(content().string(containsString("Project ID: ZP-999")));
    }

    // -------------------------------------------------------------------------
    // Fenced JSON from AI
    // -------------------------------------------------------------------------

    @Test
    void regenerateDiagram_aiFencedJsonResponse_stripsBackticksAndParses() throws Exception {
        String fenced = "```json\n" + VALID_AI_JSON + "\n```";
        when(callResponseSpec.content()).thenReturn(fenced);

        mockMvc.perform(post("/regenerate-diagram"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Auth Module")));
    }

    @Test
    void regenerateDiagram_aiFencedNoLangResponse_stripsBackticksAndParses() throws Exception {
        String fenced = "```\n" + VALID_AI_JSON + "\n```";
        when(callResponseSpec.content()).thenReturn(fenced);

        mockMvc.perform(post("/regenerate-diagram"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Auth Module")));
    }

    // -------------------------------------------------------------------------
    // AI error / invalid JSON
    // -------------------------------------------------------------------------

    @Test
    void regenerateDiagram_aiReturnsInvalidJson_returnsBadRequestWithErrorXml() throws Exception {
        when(callResponseSpec.content()).thenReturn("not valid json at all");

        mockMvc.perform(post("/regenerate-diagram"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<error>")));
    }

    @Test
    void regenerateDiagram_aiThrowsException_returnsBadRequestWithErrorXml() throws Exception {
        when(callResponseSpec.content()).thenThrow(new RuntimeException("AI service unavailable"));

        mockMvc.perform(post("/regenerate-diagram"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("<error>")))
                .andExpect(content().string(containsString("AI service unavailable")));
    }

    @Test
    void regenerateDiagram_aiReturnsNullContent_returnsBadRequest() throws Exception {
        when(callResponseSpec.content()).thenReturn(null);

        mockMvc.perform(post("/regenerate-diagram"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Default/empty params
    // -------------------------------------------------------------------------

    @Test
    void regenerateDiagram_allParamsOmitted_usesFallbacksInDiagram() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/regenerate-diagram"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Deal:")))
                .andExpect(content().string(containsString("Project ID:")))
                .andExpect(content().string(containsString("Auth Module")))
                .andExpect(content().string(containsString("User clicks Login")));
    }

    @Test
    void regenerateDiagram_emptyDealName_filenameDefaultsToDiagramRegenerated() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/regenerate-diagram")
                .param("dealName", ""))
                .andExpect(header().string("Content-Disposition",
                        containsString("diagram_regenerated.drawio")));
    }

    @Test
    void regenerateDiagram_missingAiFields_usesFallbacksForTriggerModuleDescription() throws Exception {
        when(callResponseSpec.content()).thenReturn("{}");

        mockMvc.perform(post("/regenerate-diagram"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trigger not found")))
                .andExpect(content().string(containsString("Main process not found")))
                .andExpect(content().string(containsString("Final result not found")));
    }

    // -------------------------------------------------------------------------
    // XML / special character escaping
    // -------------------------------------------------------------------------

    @Test
    void regenerateDiagram_specialCharsInDealName_escapedInDiagram() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/regenerate-diagram")
                .param("dealName", "A&B <Corp>"))
                .andExpect(content().string(containsString("A&amp;B &lt;Corp&gt;")));
    }

    @Test
    void regenerateDiagram_specialCharsInDealName_sanitizedInFilename() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/regenerate-diagram")
                .param("dealName", "A&B <Corp>"))
                .andExpect(header().string("Content-Disposition",
                        containsString("A_B__Corp__regenerated.drawio")));
    }

    @Test
    void regenerateDiagram_doubleQuotesInContent_escapedAsHtmlEntity() throws Exception {
        String json = """
                {"trigger":"Say \\"Hello\\"","module":"Greeter","description":"Greets user"}
                """;
        when(callResponseSpec.content()).thenReturn(json);

        mockMvc.perform(post("/regenerate-diagram")
                .param("dealName", "Deal with \"quotes\""))
                .andExpect(content().string(containsString("&quot;")));
    }

    @Test
    void regenerateDiagram_newlinesInContent_convertedToXmlLineBreak() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/regenerate-diagram")
                .param("companyContext", "Line one\nLine two"))
                .andExpect(content().string(containsString("&#xa;")));
    }

    // -------------------------------------------------------------------------
    // Long text truncation (shortText capped at 220 chars)
    // -------------------------------------------------------------------------

    @Test
    void regenerateDiagram_longCompanyContext_truncatedTo220Chars() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        String longText = "A".repeat(300);

        mockMvc.perform(post("/regenerate-diagram")
                .param("companyContext", longText))
                .andExpect(content().string(containsString("...")))
                .andExpect(content().string(not(containsString("A".repeat(221)))));
    }

    @Test
    void regenerateDiagram_contextExactly220Chars_notTruncated() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        String exactText = "B".repeat(220);

        mockMvc.perform(post("/regenerate-diagram")
                .param("companyContext", exactText))
                .andExpect(content().string(not(containsString("..."))));
    }
}