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

class GenerateDiagramControllerTest {

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

        GenerateDiagramController controller = new GenerateDiagramController(builder, new ObjectMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    void generateDiagram_validInputs_returnsDrawioXml() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/generate-diagram")
                .param("projectId", "P-001")
                .param("dealName", "Acme Corp")
                .param("stage", "Proposal")
                .param("companyContext", "B2B SaaS company")
                .param("productPurpose", "Project management tool")
                .param("productionNotes", "Needs SSO")
                .param("customerConcerns", "Data security"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<mxfile")))
                .andExpect(content().string(containsString("<diagram")))
                .andExpect(content().string(containsString("Auth Module")))
                .andExpect(content().string(containsString("User clicks Login")))
                .andExpect(content().string(containsString("Validates and logs in user")));
    }

    @Test
    void generateDiagram_validInputs_contentDispositionHeaderContainsDealName() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/generate-diagram")
                .param("dealName", "Acme Corp"))
                .andExpect(header().string("Content-Disposition", containsString("Acme_Corp.drawio")));
    }

    @Test
    void generateDiagram_validInputs_dealNameAppearsInDiagram() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/generate-diagram")
                .param("dealName", "Acme Corp"))
                .andExpect(content().string(containsString("Deal: Acme Corp")));
    }

    @Test
    void generateDiagram_validInputs_projectIdAppearsInDiagram() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/generate-diagram")
                .param("projectId", "ZP-999"))
                .andExpect(content().string(containsString("Project ID: ZP-999")));
    }

    // -------------------------------------------------------------------------
    // Fenced JSON from AI
    // -------------------------------------------------------------------------

    @Test
    void generateDiagram_aiFencedJsonResponse_stripsBackticksAndParses() throws Exception {
        String fenced = "```json\n" + VALID_AI_JSON + "\n```";
        when(callResponseSpec.content()).thenReturn(fenced);

        mockMvc.perform(post("/generate-diagram"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Auth Module")));
    }

    @Test
    void generateDiagram_aiFencedNoLangResponse_stripsBackticksAndParses() throws Exception {
        String fenced = "```\n" + VALID_AI_JSON + "\n```";
        when(callResponseSpec.content()).thenReturn(fenced);

        mockMvc.perform(post("/generate-diagram"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Auth Module")));
    }

    // -------------------------------------------------------------------------
    // AI error / invalid JSON
    // -------------------------------------------------------------------------

    @Test
    void generateDiagram_aiReturnsInvalidJson_returnsBadRequestWithErrorXml() throws Exception {
        when(callResponseSpec.content()).thenReturn("not valid json at all");

        mockMvc.perform(post("/generate-diagram"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<error>")));
    }

    @Test
    void generateDiagram_aiThrowsException_returnsBadRequestWithErrorXml() throws Exception {
        when(callResponseSpec.content()).thenThrow(new RuntimeException("AI service unavailable"));

        mockMvc.perform(post("/generate-diagram"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("<error>")))
                .andExpect(content().string(containsString("AI service unavailable")));
    }

    @Test
    void generateDiagram_aiReturnsNullContent_returnsBadRequest() throws Exception {
        when(callResponseSpec.content()).thenReturn(null);

        mockMvc.perform(post("/generate-diagram"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Default/empty params
    // -------------------------------------------------------------------------

    @Test
    void generateDiagram_allParamsOmitted_usesFallbacksInDiagram() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        // When params are empty strings (the @RequestParam defaults), the prefix
        // "Deal: " / "Project ID: " are non-empty so getBoxText returns them as-is.
        // The "Trigger not found" / etc. fallbacks only kick in when AI fields are null/blank.
        mockMvc.perform(post("/generate-diagram"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Deal:")))
                .andExpect(content().string(containsString("Project ID:")))
                .andExpect(content().string(containsString("Auth Module")))
                .andExpect(content().string(containsString("User clicks Login")));
    }

    @Test
    void generateDiagram_emptyDealName_filenameDefaultsToDiagram() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/generate-diagram")
                .param("dealName", ""))
                .andExpect(header().string("Content-Disposition", containsString("diagram.drawio")));
    }

    @Test
    void generateDiagram_missingAiFields_usesFallbacksForTriggerModuleDescription() throws Exception {
        when(callResponseSpec.content()).thenReturn("{}");

        mockMvc.perform(post("/generate-diagram"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trigger not found")))
                .andExpect(content().string(containsString("Main process not found")))
                .andExpect(content().string(containsString("Final result not found")));
    }

    // -------------------------------------------------------------------------
    // XML / special character escaping
    // -------------------------------------------------------------------------

    @Test
    void generateDiagram_specialCharsInDealName_escapedInDiagram() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/generate-diagram")
                .param("dealName", "A&B <Corp>"))
                .andExpect(content().string(containsString("A&amp;B &lt;Corp&gt;")));
    }

    @Test
    void generateDiagram_specialCharsInDealName_sanitizedInFilename() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/generate-diagram")
                .param("dealName", "A&B <Corp>"))
                .andExpect(header().string("Content-Disposition", containsString("A_B__Corp_.drawio")));
    }

    @Test
    void generateDiagram_doubleQuotesInContent_escapedAsHtmlEntity() throws Exception {
        String json = """
                {"trigger":"Say \\"Hello\\"","module":"Greeter","description":"Greets user"}
                """;
        when(callResponseSpec.content()).thenReturn(json);

        mockMvc.perform(post("/generate-diagram")
                .param("dealName", "Deal with \"quotes\""))
                .andExpect(content().string(containsString("&quot;")));
    }

    @Test
    void generateDiagram_newlinesInContent_convertedToXmlLineBreak() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/generate-diagram")
                .param("companyContext", "Line one\nLine two"))
                .andExpect(content().string(containsString("&#xa;")));
    }

    // -------------------------------------------------------------------------
    // Long text truncation (shortText capped at 220 chars)
    // -------------------------------------------------------------------------

    @Test
    void generateDiagram_longCompanyContext_truncatedTo220Chars() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        String longText = "A".repeat(300);

        mockMvc.perform(post("/generate-diagram")
                .param("companyContext", longText))
                .andExpect(content().string(containsString("...")))
                .andExpect(content().string(not(containsString("A".repeat(221)))));
    }

    @Test
    void generateDiagram_contextExactly220Chars_notTruncated() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        String exactText = "B".repeat(220);

        mockMvc.perform(post("/generate-diagram")
                .param("companyContext", exactText))
                .andExpect(content().string(not(containsString("..."))));
    }

    // -------------------------------------------------------------------------
    // Diagram XML structure
    // -------------------------------------------------------------------------

    @Test
    void generateDiagram_response_containsExpectedMxCellIds() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/generate-diagram"))
                .andExpect(content().string(containsString("mxCell id=\"0\"")))
                .andExpect(content().string(containsString("mxCell id=\"1\"")))
                .andExpect(content().string(containsString("mxCell id=\"2\"")));
    }

    @Test
    void generateDiagram_response_containsEdgesBetweenNodes() throws Exception {
        when(callResponseSpec.content()).thenReturn(VALID_AI_JSON);

        mockMvc.perform(post("/generate-diagram"))
                .andExpect(content().string(containsString("edge=\"1\"")))
                .andExpect(content().string(containsString("source=\"2\" target=\"4\"")))
                .andExpect(content().string(containsString("source=\"4\" target=\"5\"")));
    }
}
