package com.group16.aetherxmlbridge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiagramController.class)
class DiagramControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void generateXml_validProjectData_returnsExpectedXml() throws Exception {
        String requestBody = """
            {
              "module": "User Login",
              "trigger": "Submit Button",
              "description": "Validates credentials and logs in the user"
            }
            """;

        mockMvc.perform(post("/generate-xml")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<diagram>")))
                .andExpect(content().string(containsString("<node>User Login</node>")))
                .andExpect(content().string(containsString("<node>Submit Button</node>")))
                .andExpect(content().string(containsString("<description>Validates credentials and logs in the user</description>")))
                .andExpect(content().string(containsString("<connection from=\"Submit Button\" to=\"User Login\"/>")));
    }

    @Test
    @WithMockUser
    void generateXml_missingBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/generate-xml")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void generateXml_malformedJson_returnsBadRequest() throws Exception {
        String badJson = """
            {
              "module": "User Login",
              "trigger": "Submit Button",
              "description": "Missing ending quote
            }
            """;

        mockMvc.perform(post("/generate-xml")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(badJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void generateXml_emptyFields_returnsXmlWithoutCrashing() throws Exception {
        String requestBody = """
            {
              "module": "",
              "trigger": "",
              "description": ""
            }
            """;

        mockMvc.perform(post("/generate-xml")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<diagram>")))
                .andExpect(content().string(containsString("<node></node>")))
                .andExpect(content().string(containsString("<description></description>")))
                .andExpect(content().string(containsString("<connection from=\"\" to=\"\"/>")));
    }
}