package com.group16.aetherxmlbridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group16.aetherxmlbridge.ProjectData;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class RegenerateDiagramController
{
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public RegenerateDiagramController(ChatClient.Builder builder, ObjectMapper objectMapper) throws IOException
    {
        String systemPrompt = new ClassPathResource("prompts/regenerate-diagram.txt")
                .getContentAsString(StandardCharsets.UTF_8);

        this.chatClient = builder
                .defaultSystem(systemPrompt)
                .build();

        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/regenerate-diagram", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> regenerateDiagram(
            @RequestParam(defaultValue = "") String projectId,
            @RequestParam(defaultValue = "") String dealName,
            @RequestParam(defaultValue = "") String stage,
            @RequestParam(defaultValue = "") String companyContext,
            @RequestParam(defaultValue = "") String productPurpose,
            @RequestParam(defaultValue = "") String productionNotes,
            @RequestParam(defaultValue = "") String customerConcerns,
            @RequestParam(defaultValue = "") String customPrompt
    )
    {
        try
        {
            String projectText = buildProjectText(
                    projectId,
                    dealName,
                    stage,
                    companyContext,
                    productPurpose,
                    productionNotes,
                    customerConcerns,
                    customPrompt
            );

            String aiResponse = chatClient.prompt()
                    .user(projectText)
                    .call()
                    .content();

            String cleanedResponse = cleanJson(aiResponse);
            ProjectData projectData = objectMapper.readValue(cleanedResponse, ProjectData.class);

            String drawio = buildDrawio(
                    projectData,
                    projectId,
                    dealName,
                    stage,
                    companyContext,
                    productPurpose,
                    productionNotes,
                    customerConcerns
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + getFileName(dealName) + "_regenerated.drawio\"");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(drawio);
        }
        catch (Exception e)
        {
            String xml = """
            <error>%s</error>
            """.formatted(xmlSafe(e.getMessage()));

            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xml);
        }
    }

    private String buildProjectText(
            String projectId,
            String dealName,
            String stage,
            String companyContext,
            String productPurpose,
            String productionNotes,
            String customerConcerns,
            String customPrompt
    )
    {
        StringBuilder text = new StringBuilder();

        text.append("Project ID: ").append(nullSafe(projectId)).append("\n");
        text.append("Deal Name: ").append(nullSafe(dealName)).append("\n");
        text.append("Stage: ").append(nullSafe(stage)).append("\n");
        text.append("Company Context: ").append(nullSafe(companyContext)).append("\n");
        text.append("Product Purpose: ").append(nullSafe(productPurpose)).append("\n");
        text.append("Production Notes: ").append(nullSafe(productionNotes)).append("\n");
        text.append("Customer Concerns: ").append(nullSafe(customerConcerns)).append("\n");
        text.append("Custom Prompt: ").append(nullSafe(customPrompt)).append("\n");

        return text.toString();
    }

    private String buildDrawio(
            ProjectData projectData,
            String projectId,
            String dealName,
            String stage,
            String companyContext,
            String productPurpose,
            String productionNotes,
            String customerConcerns
    )
    {
        String title = getBoxText("Deal: " + nullSafe(dealName).trim(), "Deal: Regenerated Diagram");
        String trigger = getBoxText(projectData.getTrigger(), "Trigger not found");
        String module = getBoxText(projectData.getModule(), "Main process not found");
        String description = getBoxText(projectData.getDescription(), "Final result not found");
        String context = getBoxText(shortText(nullSafe(companyContext).trim(), 220), "No company context provided");
        String purpose = getBoxText(shortText(nullSafe(productPurpose).trim(), 220), "No product purpose provided");
        String notes = getBoxText(shortText(nullSafe(productionNotes).trim(), 220), "No production notes provided");
        String concerns = getBoxText(shortText(nullSafe(customerConcerns).trim(), 220), "No customer concerns provided");
        String project = getBoxText("Project ID: " + nullSafe(projectId).trim(), "Project ID: Not Provided");
        String stageText = getBoxText("Stage: " + nullSafe(stage).trim(), "Stage: Not Provided");

        return """
        <mxfile host="app.diagrams.net" modified="2026-03-22T00:00:00.000Z" agent="Mozilla/5.0" version="24.7.17">
          <diagram name="Page-1">
            <mxGraphModel dx="1600" dy="900" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1600" pageHeight="1200" math="0" shadow="0">
              <root>
                <mxCell id="0"/>
                <mxCell id="1" parent="0"/>

                <mxCell id="2" value="%s" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;fontSize=16;fontStyle=1;" vertex="1" parent="1">
                  <mxGeometry x="560" y="20" width="420" height="70" as="geometry"/>
                </mxCell>

                <mxCell id="3" value="%s" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#f8cecc;strokeColor=#b85450;fontSize=14;" vertex="1" parent="1">
                  <mxGeometry x="130" y="20" width="320" height="70" as="geometry"/>
                </mxCell>

                <mxCell id="4" value="%s" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#fff2cc;strokeColor=#d6b656;fontSize=14;" vertex="1" parent="1">
                  <mxGeometry x="1090" y="20" width="260" height="70" as="geometry"/>
                </mxCell>

                <mxCell id="5" value="%s" style="ellipse;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#666666;fontSize=14;" vertex="1" parent="1">
                  <mxGeometry x="660" y="150" width="220" height="90" as="geometry"/>
                </mxCell>

                <mxCell id="6" value="%s" style="rhombus;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;fontSize=14;" vertex="1" parent="1">
                  <mxGeometry x="670" y="300" width="200" height="120" as="geometry"/>
                </mxCell>

                <mxCell id="7" value="%s" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#dae8fc;strokeColor=#6c8ebf;fontSize=14;" vertex="1" parent="1">
                  <mxGeometry x="660" y="500" width="220" height="100" as="geometry"/>
                </mxCell>

                <mxCell id="8" value="%s" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#f5f5f5;strokeColor=#666666;fontSize=13;" vertex="1" parent="1">
                  <mxGeometry x="130" y="180" width="340" height="130" as="geometry"/>
                </mxCell>

                <mxCell id="9" value="%s" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#f5f5f5;strokeColor=#666666;fontSize=13;" vertex="1" parent="1">
                  <mxGeometry x="1070" y="180" width="340" height="130" as="geometry"/>
                </mxCell>

                <mxCell id="10" value="%s" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#f5f5f5;strokeColor=#666666;fontSize=13;" vertex="1" parent="1">
                  <mxGeometry x="130" y="390" width="340" height="150" as="geometry"/>
                </mxCell>

                <mxCell id="11" value="%s" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#f5f5f5;strokeColor=#666666;fontSize=13;" vertex="1" parent="1">
                  <mxGeometry x="1070" y="390" width="340" height="150" as="geometry"/>
                </mxCell>

                <mxCell id="12" value="" style="endArrow=block;html=1;rounded=0;strokeWidth=2;" edge="1" parent="1" source="2" target="5">
                  <mxGeometry relative="1" as="geometry"/>
                </mxCell>

                <mxCell id="13" value="" style="endArrow=block;html=1;rounded=0;strokeWidth=2;" edge="1" parent="1" source="5" target="6">
                  <mxGeometry relative="1" as="geometry"/>
                </mxCell>

                <mxCell id="14" value="" style="endArrow=block;html=1;rounded=0;strokeWidth=2;" edge="1" parent="1" source="6" target="7">
                  <mxGeometry relative="1" as="geometry"/>
                </mxCell>

                <mxCell id="15" value="" style="endArrow=block;html=1;rounded=0;strokeWidth=2;dashed=1;" edge="1" parent="1" source="8" target="6">
                  <mxGeometry relative="1" as="geometry"/>
                </mxCell>

                <mxCell id="16" value="" style="endArrow=block;html=1;rounded=0;strokeWidth=2;dashed=1;" edge="1" parent="1" source="9" target="6">
                  <mxGeometry relative="1" as="geometry"/>
                </mxCell>

                <mxCell id="17" value="" style="endArrow=block;html=1;rounded=0;strokeWidth=2;dashed=1;" edge="1" parent="1" source="10" target="6">
                  <mxGeometry relative="1" as="geometry"/>
                </mxCell>

                <mxCell id="18" value="" style="endArrow=block;html=1;rounded=0;strokeWidth=2;dashed=1;" edge="1" parent="1" source="11" target="6">
                  <mxGeometry relative="1" as="geometry"/>
                </mxCell>

                <mxCell id="19" value="" style="endArrow=block;html=1;rounded=0;strokeWidth=2;dashed=1;" edge="1" parent="1" source="3" target="2">
                  <mxGeometry relative="1" as="geometry"/>
                </mxCell>

                <mxCell id="20" value="" style="endArrow=block;html=1;rounded=0;strokeWidth=2;dashed=1;" edge="1" parent="1" source="4" target="2">
                  <mxGeometry relative="1" as="geometry"/>
                </mxCell>

              </root>
            </mxGraphModel>
          </diagram>
        </mxfile>
        """.formatted(
                cellTextSafe(title),
                cellTextSafe(project),
                cellTextSafe(stageText),
                cellTextSafe(trigger),
                cellTextSafe(module),
                cellTextSafe(description),
                cellTextSafe("Company Context&#xa;" + context),
                cellTextSafe("Product Purpose&#xa;" + purpose),
                cellTextSafe("Production Notes&#xa;" + notes),
                cellTextSafe("Customer Concerns&#xa;" + concerns)
        );
    }

    private String shortText(String text, int max)
    {
        String value = nullSafe(text).trim();

        if (value.isEmpty())
        {
            return "";
        }

        if (value.length() <= max)
        {
            return value;
        }

        return value.substring(0, max - 3) + "...";
    }

    private String getBoxText(String text, String fallback)
    {
        String value = nullSafe(text).trim();

        if (value.isEmpty())
        {
            return fallback;
        }

        return value;
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

    private String cellTextSafe(String text)
    {
        return xmlSafe(nullSafe(text))
                .replace("\r\n", "&#xa;")
                .replace("\n", "&#xa;")
                .replace("\r", "&#xa;");
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

    private String nullSafe(String text)
    {
        if (text == null)
        {
            return "";
        }

        return text;
    }

    private String getFileName(String dealName)
    {
        String name = nullSafe(dealName).trim();

        if (name.isEmpty())
        {
            return "diagram";
        }

        return name.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
}