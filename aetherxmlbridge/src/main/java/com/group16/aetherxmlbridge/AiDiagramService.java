package com.group16.aetherxmlbridge;

import org.springframework.stereotype.Service;

@Service
public class AiDiagramService {

    public String generateXml(ProjectData projectData) {

        //simulated ai response for iteration 1
        //currently builds XML using project data
        //in future iteration, will be replaced by real ai api call
        //that analyzes zoho project data and generated diagram

        return """

        <diagram>
            <node>%s</node>
            <node>AI Processor</node>
            <node>%s</node>
            <description>%s</description>
            <connection from="%s" to="AI Processor"/>
            <connection from="AI Processor" to="%s"/>
        </diagram>
        """.formatted(
                projectData.getModule(),
                projectData.getTrigger(),
                projectData.getDescription(),
                projectData.getModule(),
                projectData.getTrigger()
        );
    }
}