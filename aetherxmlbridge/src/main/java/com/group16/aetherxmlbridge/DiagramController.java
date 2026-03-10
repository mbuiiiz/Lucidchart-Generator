package com.group16.aetherxmlbridge;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

@RestController
public class DiagramController {

    private final AiDiagramService aiDiagramService;

    public DiagramController(AiDiagramService aiDiagramService) {
        this.aiDiagramService = aiDiagramService;
    }

    @PostMapping("/generate-xml")
    public ResponseEntity<String> generateXML(@RequestBody ProjectData projectData)
    {

        String xml = aiDiagramService.generateXml(projectData);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }
}