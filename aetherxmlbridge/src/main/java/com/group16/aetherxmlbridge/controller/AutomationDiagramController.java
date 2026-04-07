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
import java.util.ArrayList;
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
            @RequestParam String scopeId,
            @RequestParam(defaultValue = "") String customPrompt
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

            String prompt = buildPrompt(targetScope, customPrompt);

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

    private String buildPrompt(ZohoScopeData scope, String customPrompt) {
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

        if (customPrompt != null && !customPrompt.trim().isEmpty()) {
            prompt.append("\n");
            prompt.append("Custom Prompt: ").append(customPrompt.trim()).append("\n");
            prompt.append("Use this custom prompt to regenerate the same automation diagram in a refined way.\n");
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
        int cellId = 2;

        // ── Layout constants ──────────────────────────────────────────────────
        final int NW     = 180;  // action/trigger node width
        final int NH     = 80;   // action/trigger node height
        final int DW     = 130;  // diamond width
        final int DH     = 90;   // diamond height
        final int PILL_W = 100;  // start/terminator width
        final int PILL_H = 50;   // start/terminator height
        final int HGAP   = 30;   // horizontal gap between nodes on spine
        final int VGAP   = 30;   // vertical gap between nodes in body
        final int SPINE_Y = 80;  // Y of the horizontal top spine (node top edge)

        // Current position on horizontal spine
        int hx = 40;
        int prevId = -1;
        String pendingLabel = null;

        // Vertical body (activated after first Search/Loop condition)
        boolean inBody  = false;
        int vx          = 0;   // left edge X of vertical column
        int vy          = 0;   // current Y in vertical column
        int loopActionId = -1;
        AutomationDiagramData.Condition deferredLoopCond = null;

        List<AutomationDiagramData.Condition> aiConditions = new ArrayList<>(data.getConditions());
        int condIdx = 0;

        // ── Start (horizontal spine) ──────────────────────────────────────────
        int startId = cellId++;
        cells.append(createStartCell(startId, hx, SPINE_Y + (NH - PILL_H) / 2));
        prevId = startId;
        hx += PILL_W + HGAP;

        // ── Triggers (horizontal spine) ───────────────────────────────────────
        for (AutomationDiagramData.Trigger trigger : data.getTriggers()) {
            int id = cellId++;
            cells.append(createTriggerCell(id, trigger, hx, SPINE_Y));
            cells.append(createRightArrow(cellId++, prevId, id, pendingLabel));
            pendingLabel = null;
            prevId = id;
            hx += NW + HGAP;
        }

        // ── Actions ───────────────────────────────────────────────────────────
        for (AutomationDiagramData.Action action : data.getActions()) {
            String type = nullSafe(action.getType()).toLowerCase();

            if (!inBody) {
                // ── Horizontal spine phase ────────────────────────────────
                int id = cellId++;
                cells.append(createActionCell(id, action, hx, SPINE_Y));
                cells.append(createRightArrow(cellId++, prevId, id, pendingLabel));
                pendingLabel = null;
                prevId = id;

                if (type.equals("loop")) {
                    loopActionId = id;
                    // Pick/auto-generate the loop condition
                    if (condIdx < aiConditions.size()) {
                        deferredLoopCond = aiConditions.get(condIdx++);
                    } else {
                        deferredLoopCond = new AutomationDiagramData.Condition();
                        deferredLoopCond.setName("More Items?");
                        deferredLoopCond.setYesPath("Back to loop");
                        deferredLoopCond.setNoPath("Continue");
                    }
                    // Place More Items? diamond on the spine
                    hx += NW + HGAP;
                    int condId = cellId++;
                    cells.append(createConditionCellH(condId, deferredLoopCond,
                            hx, SPINE_Y + (NH - DH) / 2));
                    cells.append(createRightArrow(cellId++, prevId, condId, null));

                    // No → Terminator continuing right on spine
                    int termSpineId = cellId++;
                    cells.append(createTerminatorCell(termSpineId,
                            hx + DW + HGAP, SPINE_Y + (NH - PILL_H) / 2));
                    cells.append(createRightArrow(cellId++, condId, termSpineId, "No"));

                    // Yes → drops down into body below the diamond
                    vx = hx + (DW - NW) / 2;
                    vy = SPINE_Y + DH + VGAP;
                    inBody = true;
                    prevId = condId;
                    pendingLabel = "Yes";
                    deferredLoopCond = null; // already rendered

                } else if (type.equals("search")) {
                    // Place Found? diamond on the spine
                    hx += NW + HGAP;
                    AutomationDiagramData.Condition cond;
                    if (condIdx < aiConditions.size()) {
                        cond = aiConditions.get(condIdx++);
                    } else {
                        cond = new AutomationDiagramData.Condition();
                        cond.setName("Found?");
                        cond.setYesPath("Continue");
                        cond.setNoPath("End");
                    }
                    int condId = cellId++;
                    cells.append(createConditionCellH(condId, cond,
                            hx, SPINE_Y + (NH - DH) / 2));
                    cells.append(createRightArrow(cellId++, prevId, condId, null));

                    // No → small End pill to the right
                    int endId = cellId++;
                    cells.append(createLocalEndCell(endId,
                            hx + DW + HGAP, SPINE_Y + (DH - 40) / 2));
                    cells.append(createRightArrow(cellId++, condId, endId, "No"));

                    // Yes → drops down into body
                    vx = hx + (DW - NW) / 2;
                    vy = SPINE_Y + DH + VGAP;
                    inBody = true;
                    prevId = condId;
                    pendingLabel = "Yes";

                } else {
                    hx += NW + HGAP;
                }

            } else {
                // ── Vertical body phase ───────────────────────────────────
                int id = cellId++;
                cells.append(createActionCell(id, action, vx, vy));
                cells.append(createDownArrow(cellId++, prevId, id, pendingLabel));
                pendingLabel = null;
                prevId = id;
                vy += NH + VGAP;

                if (type.equals("loop")) {
                    loopActionId = id;
                    if (condIdx < aiConditions.size()) {
                        deferredLoopCond = aiConditions.get(condIdx++);
                    } else {
                        deferredLoopCond = new AutomationDiagramData.Condition();
                        deferredLoopCond.setName("More Items?");
                        deferredLoopCond.setYesPath("Back to loop");
                        deferredLoopCond.setNoPath("Continue");
                    }
                } else if (type.equals("search")) {
                    AutomationDiagramData.Condition cond;
                    if (condIdx < aiConditions.size()) {
                        cond = aiConditions.get(condIdx++);
                    } else {
                        cond = new AutomationDiagramData.Condition();
                        cond.setName("Found?");
                        cond.setYesPath("Continue");
                        cond.setNoPath("End");
                    }
                    int condId = cellId++;
                    cells.append(createConditionCellV(condId, cond,
                            vx + (NW - DW) / 2, vy));
                    cells.append(createDownArrow(cellId++, prevId, condId, null));
                    int endId = cellId++;
                    cells.append(createLocalEndCell(endId,
                            vx + NW + HGAP, vy + (DH - 40) / 2));
                    cells.append(createRightArrow(cellId++, condId, endId, "No"));
                    pendingLabel = "Yes";
                    prevId = condId;
                    vy += DH + VGAP;
                }
            }
        }

        // ── Deferred loop condition (end of vertical body) ────────────────────
        if (deferredLoopCond != null && loopActionId != -1 && inBody) {
            int condId = cellId++;
            cells.append(createConditionCellV(condId, deferredLoopCond,
                    vx + (NW - DW) / 2, vy));
            cells.append(createDownArrow(cellId++, prevId, condId, pendingLabel));
            pendingLabel = null;
            cells.append(createLoopBackArrow(cellId++, condId, loopActionId, "Yes"));
            pendingLabel = "No";
            prevId = condId;
            vy += DH + VGAP;
        }

        // ── Terminator ────────────────────────────────────────────────────────
        if (inBody) {
            int termId = cellId++;
            cells.append(createTerminatorCell(termId,
                    vx + (NW - PILL_W) / 2, vy));
            cells.append(createDownArrow(cellId++, prevId, termId, pendingLabel));
        } else {
            // No conditions at all — terminator at end of horizontal spine
            int termId = cellId++;
            cells.append(createTerminatorCell(termId,
                    hx, SPINE_Y + (NH - PILL_H) / 2));
            cells.append(createRightArrow(cellId++, prevId, termId, pendingLabel));
        }

        int pageW = Math.max(1400, hx + 400);
        int pageH = inBody ? Math.max(900, vy + 200) : 400;

        return """
            <mxfile host="app.diagrams.net" modified="2026-03-31T00:00:00.000Z" agent="AetherGen" version="24.7.17">
              <diagram name="Automation Flow">
                <mxGraphModel dx="1400" dy="900" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="%d" pageHeight="%d" math="0" shadow="0">
                  <root>
                    <mxCell id="0"/>
                    <mxCell id="1" parent="0"/>
                    %s
                  </root>
                </mxGraphModel>
              </diagram>
            </mxfile>
            """.formatted(pageW, pageH, cells.toString());
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
        String app = nullSafe(trigger.getApplication());
        String name = nullSafe(trigger.getName()) + (app.isEmpty() ? "" : " (" + app + ")");
        String desc = nullSafe(trigger.getDescription());
        String value = htmlCell(name, desc);
        return """
            <mxCell id="%d" value="%s" style="rounded=0;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#000000;strokeWidth=2;fontSize=11;" vertex="1" parent="1">
              <mxGeometry x="%d" y="%d" width="200" height="90" as="geometry"/>
            </mxCell>
            """.formatted(id, value, x, y);
    }

    // Condition: light blue diamond (legacy horizontal layout)
    private String createConditionCell(int id, AutomationDiagramData.Condition condition, int x, int y) {
        String text = nullSafe(condition.getName());
        return """
            <mxCell id="%d" value="%s" style="rhombus;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;fontSize=11;" vertex="1" parent="1">
              <mxGeometry x="%d" y="%d" width="120" height="80" as="geometry"/>
            </mxCell>
            """.formatted(id, cellTextSafe(text), x, y - 15);
    }

    // Condition: light blue diamond — horizontal spine (exits right/left, enters right/left)
    private String createConditionCellH(int id, AutomationDiagramData.Condition condition, int x, int y) {
        String text = nullSafe(condition.getName());
        return """
            <mxCell id="%d" value="%s" style="rhombus;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;fontSize=12;fontStyle=1;" vertex="1" parent="1">
              <mxGeometry x="%d" y="%d" width="130" height="90" as="geometry"/>
            </mxCell>
            """.formatted(id, cellTextSafe(text), x, y);
    }

    // Condition: light blue diamond — vertical layout (no y offset, larger)
    private String createConditionCellV(int id, AutomationDiagramData.Condition condition, int x, int y) {
        String text = nullSafe(condition.getName());
        return """
            <mxCell id="%d" value="%s" style="rhombus;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;fontSize=12;fontStyle=1;" vertex="1" parent="1">
              <mxGeometry x="%d" y="%d" width="140" height="100" as="geometry"/>
            </mxCell>
            """.formatted(id, cellTextSafe(text), x, y);
    }

    // Down arrow — exits bottom center, enters top center (main vertical flow)
    private String createDownArrow(int id, int sourceId, int targetId, String label) {
        String labelAttr = (label != null && !label.isEmpty())
                ? "value=\"" + cellTextSafe(label) + "\" " : "";
        return """
            <mxCell id="%d" %sstyle="endArrow=classic;html=1;rounded=0;exitX=0.5;exitY=1;exitDx=0;exitDy=0;entryX=0.5;entryY=0;entryDx=0;entryDy=0;" edge="1" parent="1" source="%d" target="%d">
              <mxGeometry relative="1" as="geometry"/>
            </mxCell>
            """.formatted(id, labelAttr, sourceId, targetId);
    }

    // Right arrow — exits right center, enters left center (No branch)
    private String createRightArrow(int id, int sourceId, int targetId, String label) {
        String labelAttr = (label != null && !label.isEmpty())
                ? "value=\"" + cellTextSafe(label) + "\" " : "";
        return """
            <mxCell id="%d" %sstyle="endArrow=classic;html=1;rounded=0;exitX=1;exitY=0.5;exitDx=0;exitDy=0;entryX=0;entryY=0.5;entryDx=0;entryDy=0;" edge="1" parent="1" source="%d" target="%d">
              <mxGeometry relative="1" as="geometry"/>
            </mxCell>
            """.formatted(id, labelAttr, sourceId, targetId);
    }

    // Loop-back arrow — exits left, curves up and re-enters left side of loop action
    private String createLoopBackArrow(int id, int sourceId, int targetId, String label) {
        String labelAttr = (label != null && !label.isEmpty())
                ? "value=\"" + cellTextSafe(label) + "\" " : "";
        return """
            <mxCell id="%d" %sstyle="rounded=1;orthogonalLoop=1;jettySize=auto;exitX=0;exitY=0.5;exitDx=0;exitDy=0;entryX=0;entryY=0.5;entryDx=0;entryDy=0;endArrow=classic;html=1;" edge="1" parent="1" source="%d" target="%d">
              <mxGeometry relative="1" as="geometry"/>
            </mxCell>
            """.formatted(id, labelAttr, sourceId, targetId);
    }

    // No-branch stub — yellow box showing what happens on the No path
    private String createNoStubCell(int id, int x, int y, String text) {
        return """
            <mxCell id="%d" value="%s" style="rounded=0;whiteSpace=wrap;html=1;fillColor=#fff2cc;strokeColor=#d6b656;fontSize=11;" vertex="1" parent="1">
              <mxGeometry x="%d" y="%d" width="160" height="60" as="geometry"/>
            </mxCell>
            """.formatted(id, cellTextSafe(text), x, y);
    }

    // Local End pill — small red terminator placed beside a No branch
    private String createLocalEndCell(int id, int x, int y) {
        return """
            <mxCell id="%d" value="End" style="ellipse;whiteSpace=wrap;html=1;fillColor=#f8cecc;strokeColor=#b85450;fontStyle=1;fontSize=11;" vertex="1" parent="1">
              <mxGeometry x="%d" y="%d" width="70" height="40" as="geometry"/>
            </mxCell>
            """.formatted(id, x, y);
    }

    // Action: light blue box
    private String createActionCell(int id, AutomationDiagramData.Action action, int x, int y) {
        String value = htmlCell(nullSafe(action.getName()), nullSafe(action.getDescription()));
        return """
            <mxCell id="%d" value="%s" style="rounded=0;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;fontSize=11;" vertex="1" parent="1">
              <mxGeometry x="%d" y="%d" width="200" height="90" as="geometry"/>
            </mxCell>
            """.formatted(id, value, x, y);
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

    // Build an HTML cell value: bold name on top, smaller description below
    private String htmlCell(String name, String description) {
        String safeName = xmlSafe(nullSafe(name));
        String safeDesc = xmlSafe(nullSafe(description));
        if (safeDesc.isEmpty()) {
            return "&lt;b&gt;" + safeName + "&lt;/b&gt;";
        }
        return "&lt;b&gt;" + safeName + "&lt;/b&gt;&lt;br/&gt;"
                + "&lt;font style=&quot;font-size:10px;&quot;&gt;" + safeDesc + "&lt;/font&gt;";
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
