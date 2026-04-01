package com.group16.aetherxmlbridge.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for Zoho Creator scope/trigger data used in diagram generation.
 * Contains automation triggers, conditions, and actions.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZohoScopeData {
    private String id;
    private String projectId;
    private String productDescription;
    
    // Automation trigger fields
    private String triggerEventDescription;
    private String triggerApplicationName;

    private String selectTrigger;
    private String detailedDescription;
    
    // Lists for multiple triggers/conditions/actions
    private List<AutomationTrigger> triggers = new ArrayList<>();
    private List<Condition> conditions = new ArrayList<>();
    private List<Action> actions = new ArrayList<>();

    private String rawJson;
    
    /**
     * Represents an automation trigger
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AutomationTrigger {
        private String name;
        private String eventDescription;
        private String applicationName;
    }
    
    /**
     * Represents a condition/decision point
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Condition {
        private String description;
        private String type;
    }
    
    /**
     * Represents an action/output
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Action {
        private String description;
        private String type;
    }
}
