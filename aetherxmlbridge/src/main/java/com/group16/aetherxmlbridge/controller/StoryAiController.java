package com.group16.aetherxmlbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group16.aetherxmlbridge.ProjectData;
import com.group16.aetherxmlbridge.UserStoryRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/ai")
public class StoryAiController
{
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public StoryAiController(ChatClient.Builder builder, ObjectMapper objectMapper) throws IOException
    {
        String systemPrompt = new ClassPathResource("prompts/extract.txt")
                .getContentAsString(StandardCharsets.UTF_8);

        this.chatClient = builder
                .defaultSystem(systemPrompt)
                .build();

        this.objectMapper = objectMapper;
    }

    @PostMapping("/extract")
    public ResponseEntity<?> extract(@RequestBody UserStoryRequest request)
    {
        try
        {
            if (request == null || request.getUserStory() == null || request.getUserStory().isBlank())
            {
                return ResponseEntity.badRequest().body("userStory is required");
            }

            ProjectData projectData = extractProjectData(request.getUserStory());

            return ResponseEntity.ok(projectData);
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest().body("Something went wrong: " + e.getMessage());
        }
    }

    @PostMapping(value = "/story-to-xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> storyToXml(@RequestBody UserStoryRequest request)
    {
        try
        {
            if (request == null || request.getUserStory() == null || request.getUserStory().isBlank())
            {
                return ResponseEntity.badRequest()
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("userStory is required");
            }

            ProjectData projectData = extractProjectData(request.getUserStory());

            String module = xmlSafe(projectData.getModule());
            String trigger = xmlSafe(projectData.getTrigger());
            String description = xmlSafe(projectData.getDescription());

            String xml = """
            <diagram>
                <node>%s</node>
                <node>%s</node>
                <description>%s</description>
                <connection from="%s" to="%s"/>
            </diagram>
            """.formatted(
                    module,
                    trigger,
                    description,
                    trigger,
                    module
            );

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xml);
        }
        catch (Exception e)
        {
            String error = xmlSafe(e.getMessage());
            String xml = """
            <error>%s</error>
            """.formatted(error);

            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xml);
        }
    }

    private ProjectData extractProjectData(String userStory) throws Exception
    {
        String aiResponse = chatClient.prompt()
                .user(userStory)
                .call()
                .content();

        String cleanedResponse = cleanJson(aiResponse);

        return objectMapper.readValue(cleanedResponse, ProjectData.class);
    }

    private String cleanJson(String text)
    {
        if (text == null)
        {
            return "";
        }

        String cleaned = text.trim();

        if (cleaned.startsWith("```json"))
        {
            cleaned = cleaned.substring(7).trim();
        }
        else if (cleaned.startsWith("```"))
        {
            cleaned = cleaned.substring(3).trim();
        }

        if (cleaned.endsWith("```"))
        {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        return cleaned;
    }

    private String xmlSafe(String text)
    {
        if (text == null)
        {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}