package com.group16.aetherxmlbridge;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

@RestController
public class DiagramController {

    @PostMapping("/generate-xml")
    public ResponseEntity<String> generateXML(@RequestBody ProjectData projectData)
    {

        String xml = """
        <diagram>
            <node>%s</node>
            <node>%s</node>
            <description>%s</description>
            <connection from="%s" to="%s"/>
        </diagram>
        """.formatted(
                projectData.getModule(),
                projectData.getTrigger(),
                projectData.getDescription(),
                projectData.getTrigger(),
                projectData.getModule()
        );

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }
}