package com.group16.aetherxmlbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group16.aetherxmlbridge.model.AppUser;
import com.group16.aetherxmlbridge.model.AutomationDiagramData;
import com.group16.aetherxmlbridge.model.ZohoScopeData;
import com.group16.aetherxmlbridge.repository.AppUserRepository;
import com.group16.aetherxmlbridge.service.ZohoApiService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;

/**
 * Controller for generating automation flow diagrams from Zoho scope data.
 * Creates LucidChart-compatible XML with:
 * - White boxes for triggers
 * - Blue diamonds for conditions
 * - Blue boxes for actions
 */
@RestController
public class AutomationDiagramController {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final AppUserRepository appUserRepository;
    private final ZohoApiService zohoApiService;

    public AutomationDiagramController(
            ChatClient.Builder builder,
            ObjectMapper objectMapper,
            AppUserRepository appUserRepository,
            ZohoApiService zohoApiService
    ) throws IOException {
        String systemPrompt = new ClassPathResource("prompts/automation-diagram.txt")
                .getContentAsString(StandardCharsets.UTF_8);

        this.chatClient = builder
                .defaultSystem(systemPrompt)
                .build();

        this.objectMapper = objectMapper;
        this.appUserRepository = appUserRepository;
        this.zohoApiService = zohoApiService;
    }

    @PostMapping(value = "/generate-automation-diagram", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> generateAutomationDiagram(
            Principal principal,
            @RequestParam String scopeId
    ) {
        try {
            AppUser user = getCurrentUser(principal);
            if (user == null) {
                return errorResponse("User not found or not authenticated");
            }

            if (user.getZohoAccessToken() == null) {
                return errorResponse("User not logged in via Zoho OAuth");
            }

            List<ZohoScopeData> scopeDataList = zohoApiService.fetchScopeData(user, null);
            
            // Find the specific scope record
            ZohoScopeData targetScope = scopeDataList.stream()
                    .filter(s -> s.getId().equals(scopeId))
                    .findFirst()
                    .orElse(null);

            if (targetScope == null) {
                return errorResponse("Scope record not found with ID: " + scopeId);
            }

            String prompt = buildPrompt(targetScope);

            // Call AI to analyze and structure the automation
            String aiResponse = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            String cleanedResponse = cleanJson(aiResponse);
            AutomationDiagramData diagramData = objectMapper.readValue(cleanedResponse, AutomationDiagramData.class);

            String drawio = buildAutomationDrawio(diagramData, targetScope);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + getFileName(diagramData.getTitle()) + ".drawio\"");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(drawio);

        } catch (Exception e) {
            return errorResponse("Error generating diagram: " + e.getMessage());
        }
    }

    private AppUser getCurrentUser(Principal principal) {
        if (principal == null) return null;

        String email;
        if (principal instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User oauthUser = oauthToken.getPrincipal();
            email = oauthUser.getAttribute("Email");
            if (email == null) {
                email = oauthUser.getAttribute("email");
            }
        } else {
            email = principal.getName();
        }

        if (email == null) return null;
        return appUserRepository.findByEmail(email).orElse(null);
    }

    private String buildPrompt(ZohoScopeData scope) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze this automation scope and extract the workflow components:\n\n");
        
        if (scope.getProductDescription() != null && !scope.getProductDescription().isEmpty()) {
            prompt.append("Product: ").append(scope.getProductDescription()).append("\n");
        }
        if (scope.getSelectTrigger() != null && !scope.getSelectTrigger().isEmpty()) {
            prompt.append("Type: ").append(scope.getSelectTrigger()).append("\n");
        }
        if (scope.getTriggerApplicationName() != null && !scope.getTriggerApplicationName().isEmpty()) {
            prompt.append("Triggered in: ").append(scope.getTriggerApplicationName()).append("\n");
        }
        if (scope.getTriggerEventDescription() != null && !scope.getTriggerEventDescription().isEmpty()) {
            prompt.append("Trigger: ").append(scope.getTriggerEventDescription()).append("\n");
        }
        if (scope.getDetailedDescription() != null && !scope.getDetailedDescription().isEmpty()) {
            prompt.append("Details: ").append(scope.getDetailedDescription()).append("\n");
        }

        return prompt.toString();
    }

    /**
     * Build draw.io XML for automation diagram.
     * Visual specification (per client example):
     * - Start: green rounded pill (#d5e8d4)
     * - Triggers: white boxes with black border (#ffffff)
     * - Actions: light blue boxes (#dae8fc)
     * - Conditions: light blue diamonds (#dae8fc) with Yes/No labels on arrows
     * - Terminator: red rounded pill (#f8cecc)
     */
    private String buildAutomationDrawio(AutomationDiagramData data, ZohoScopeData scope) {
        StringBuilder cells = new StringBuilder();
        int cellId = 2; // Start after root cells 0 and 1
        int xPosition = 120;
        int yPosition = 40;
        int prevCellId = -1;

        // Start node (green pill)
        int startId = cellId++;
        cells.append(createStartCell(startId, xPosition, yPosition));
        prevCellId = startId;
        xPosition += 140;

        // Triggers (white boxes with black border)
        for (AutomationDiagramData.Trigger trigger : data.getTriggers()) {
            int currentId = cellId++;
            cells.append(createTriggerCell(currentId, trigger, xPosition, yPosition));
            cells.append(createArrow(cellId++, prevCellId, currentId, null));
            prevCellId = currentId;
            xPosition += 200;
        }

        List<AutomationDiagramData.Action> actions = data.getActions();
        List<AutomationDiagramData.Condition> conditions = data.getConditions();
        
        int actionIndex = 0;
        int conditionIndex = 0;
        
        // Process actions first, then conditions (simplified linear flow)
        for (AutomationDiagramData.Action action : actions) {
            int currentId = cellId++;
            cells.append(createActionCell(currentId, action, xPosition, yPosition));
            cells.append(createArrow(cellId++, prevCellId, currentId, null));
            prevCellId = currentId;
            xPosition += 200;
            
            // Add a condition after every 2-3 actions if we have conditions
            if (conditionIndex < conditions.size() && (actionIndex + 1) % 2 == 0) {
                AutomationDiagramData.Condition condition = conditions.get(conditionIndex++);
                int condId = cellId++;
                cells.append(createConditionCell(condId, condition, xPosition, yPosition));
                cells.append(createArrow(cellId++, prevCellId, condId, null));

                prevCellId = condId;
                xPosition += 180;
            }
            actionIndex++;
        }
        
        // Add remaining conditions at the end
        while (conditionIndex < conditions.size()) {
            AutomationDiagramData.Condition condition = conditions.get(conditionIndex++);
            int currentId = cellId++;
            cells.append(createConditionCell(currentId, condition, xPosition, yPosition));
            cells.append(createArrow(cellId++, prevCellId, currentId, null));
            prevCellId = currentId;
            xPosition += 180;
        }

        // Terminator node (red pill)
        int terminatorId = cellId++;
        cells.append(createTerminatorCell(terminatorId, xPosition, yPosition));
        cells.append(createArrow(cellId++, prevCellId, terminatorId, null));

        return """
            <mxfile host="app.diagrams.net" modified="2026-03-31T00:00:00.000Z" agent="AetherGen" version="24.7.17">
              <diagram name="Automation Flow">
                <mxGraphModel dx="1800" dy="1000" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1600" pageHeight="1200" math="0" shadow="0">
                  <root>
                    <mxCell id="0"/>
                    <mxCell id="1" parent="0"/>
                    %s
                  </root>
                </mxGraphModel>
              </diagram>
            </mxfile>
            """.formatted(cells.toString());
    }

    // Start: green rounded pill
    private String createStartCell(int id, int x, int y) {
        return """
            <mxCell id="%d" value="Start" style="ellipse;whiteSpace=wrap;html=1;fillColor=#d5e8d4;strokeColor=#82b366;fontStyle=1;fontSize=14;" vertex="1" parent="1">
              <mxGeometry x="%d" y="%d" width="80" height="50" as="geometry"/>
            </mxCell>
            """.formatted(id, x, y);
    }

    // Terminator: red rounded pill
    private String createTerminatorCell(int id, int x, int y) {
        return """
            <mxCell id="%d" value="Terminator" style="ellipse;whiteSpace=wrap;html=1;fillColor=#f8cecc;strokeColor=#b85450;fontStyle=1;fontSize=14;" vertex="1" parent="1">
              <mxGeometry x="%d" y="%d" width="100" height="50" as="geometry"/>
            </mxCell>
            """.formatted(id, x, y);
    }

    // Trigger: white box with black border
    private String createTriggerCell(int id, AutomationDiagramData.Trigger trigger, int x, int y) {
        String text = nullSafe(trigger.getName());
        if (trigger.getApplication() != null && !trigger.getApplication().isEmpty()) {
            text += "\\n(" + trigger.getApplication() + ")";
        }
        if (trigger.getDescription() != null && !trigger.getDescription().isEmpty()) {
            text += "\\n" + shortText(trigger.getDescription(), 80);
        }
        return """
            <mxCell id="%d" value="%s" style="rounded=0;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#000000;strokeWidth=2;fontSize=11;" vertex="1" parent="1">
              <mxGeometry x="%d" y="%d" width="160" height="80" as="geometry"/>
            </mxCell>
            """.formatted(id, cellTextSafe(text), x, y);
    }

    // Condition: light blue diamond
    private String createConditionCell(int id, AutomationDiagramData.Condition condition, int x, int y) {
        String text = nullSafe(condition.getName());
        return """
            <mxCell id="%d" value="%s" style="rhombus;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;fontSize=11;" vertex="1" parent="1">
              <mxGeometry x="%d" y="%d" width="120" height="80" as="geometry"/>
            </mxCell>
            """.formatted(id, cellTextSafe(text), x, y - 15);
    }

    // Action: light blue box
    private String createActionCell(int id, AutomationDiagramData.Action action, int x, int y) {
        String text = nullSafe(action.getName());
        if (action.getDescription() != null && !action.getDescription().isEmpty()) {
            text += "\\n" + shortText(action.getDescription(), 60);
        }
        return """
            <mxCell id="%d" value="%s" style="rounded=0;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;fontSize=11;" vertex="1" parent="1">
              <mxGeometry x="%d" y="%d" width="160" height="80" as="geometry"/>
            </mxCell>
            """.formatted(id, cellTextSafe(text), x, y);
    }

    // Arrow with optional label (Yes/No for conditions)
    private String createArrow(int id, int sourceId, int targetId, String label) {
        String labelAttr = "";
        if (label != null && !label.isEmpty()) {
            labelAttr = "value=\"" + cellTextSafe(label) + "\" ";
        }
        return """
            <mxCell id="%d" %sstyle="endArrow=classic;html=1;rounded=0;strokeWidth=1;exitX=1;exitY=0.5;exitDx=0;exitDy=0;entryX=0;entryY=0.5;entryDx=0;entryDy=0;" edge="1" parent="1" source="%d" target="%d">
              <mxGeometry relative="1" as="geometry"/>
            </mxCell>
            """.formatted(id, labelAttr, sourceId, targetId);
    }

    private String shortText(String text, int max) {
        String value = nullSafe(text).trim();
        if (value.isEmpty()) return "";
        if (value.length() <= max) return value;
        return value.substring(0, max - 3) + "...";
    }

    private String cleanJson(String text) {
        if (text == null) return "";
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }
        return cleaned;
    }

    private String cellTextSafe(String text) {
        return xmlSafe(nullSafe(text))
                .replace("\r\n", "&#xa;")
                .replace("\n", "&#xa;")
                .replace("\r", "&#xa;")
                .replace("\\n", "&#xa;");
    }

    private String xmlSafe(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String nullSafe(String text) {
        return text == null ? "" : text;
    }

    private String getFileName(String title) {
        String name = nullSafe(title).trim();
        if (name.isEmpty()) return "automation-diagram";
        return name.replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    private ResponseEntity<String> errorResponse(String message) {
        String xml = "<error>%s</error>".formatted(xmlSafe(message));
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }
}
