package com.group16.aetherxmlbridge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group16.aetherxmlbridge.model.AppUser;
import com.group16.aetherxmlbridge.model.ZohoProject;
import com.group16.aetherxmlbridge.model.ZohoScopeData;
import com.group16.aetherxmlbridge.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ZohoApiService {

    // Enable by setting logging.level.com.group16.aetherxmlbridge.service.ZohoApiService=DEBUG
    private static final Logger log = LoggerFactory.getLogger(ZohoApiService.class);

    // zoho creator api config loaded from application.properties / env vars
    // Note: Zoho Creator API has two possible base URLs:
    // - Legacy: https://creator.zoho.com (with /api/v2/ path)
    // - Official v2.1: https://www.zohoapis.com (with /creator/v2.1/data/ path)
    @Value("${zoho.creator.api-base-url:https://creator.zoho.com}")
    private String apiBaseUrl;

    // Set to true to use new v2.1 API format, false for legacy v2 format
    @Value("${zoho.creator.use-v21-api:false}")
    private boolean useV21Api;

    @Value("${zoho.creator.owner-name:}")
    private String ownerName;

    @Value("${zoho.creator.app-link-name:}")
    private String appLinkName;

    @Value("${zoho.creator.report-link-name:All_Projects}")
    private String reportLinkName;

    @Value("${zoho.creator.scope-report-link-name:Automation_Product_Estimate_Scope_Tool_Report}")
    private String scopeReportLinkName;

    // zoho oauth config for token refresh
    @Value("${zoho.oauth.token-url:https://accounts.zoho.com/oauth/v2/token}")
    private String tokenUrl;

    @Value("${ZOHO_CLIENT_ID:}")
    private String clientId;

    @Value("${ZOHO_CLIENT_SECRET:}")
    private String clientSecret;

    // token refreshes frequently, we need to update user row database
    private final AppUserRepository appUserRepository;
    private final RestTemplate restTemplate;
    // parsing json
    private final ObjectMapper objectMapper;

    public ZohoApiService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // fetch all projects from zoho creator for the given user
    public List<ZohoProject> fetchProjects(AppUser user) {
        // skip if user has no zoho token (not logged in via zoho)
        if (user == null || user.getZohoAccessToken() == null) {
            return Collections.emptyList();
        }

        // refresh token if it expires within the next 60 seconds just in case of race condition
        if (user.getZohoTokenExpiry() != null && Instant.now().isAfter(user.getZohoTokenExpiry().minusSeconds(60))) {
            refreshAccessToken(user);
        }

        // build zoho creator report api url
        // Legacy v2: /api/v2/{owner}/{app}/report/{report}
        // v2.1: /creator/v2.1/data/{owner}/{app}/report/{report}
        String url;
        if (useV21Api) {
            url = apiBaseUrl + "/creator/v2.1/data/" + ownerName + "/" + appLinkName + "/report/" + reportLinkName;
        } else {
            url = apiBaseUrl + "/api/v2/" + ownerName + "/" + appLinkName + "/report/" + reportLinkName;
        }

        // attach bearer token to request header
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Zoho-oauthtoken " + user.getZohoAccessToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            log.info("Zoho API Response Status: {}", response.getStatusCode());
            log.debug("Zoho API Raw JSON: {}", response.getBody());
            return parseProjects(response.getBody());
        } catch (Exception e) {
            log.error("Failed to fetch projects from Zoho API", e);
            log.error("URL attempted: {}", url);
            return Collections.emptyList();
        }
    }

    // refresh new access token and save it to the database
    private void refreshAccessToken(AppUser user) {
        if (user.getZohoRefreshToken() == null) return;

        // build token refresh request body from application/x-www-form-urlencoded format
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", user.getZohoRefreshToken());

        // turn the mapping into form-ended format
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        try {
            // post to zoho token endpoint and get response
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, entity, String.class);
            log.debug("Token refresh response: {}", response.getBody());
            // parse the json response body
            JsonNode root = objectMapper.readTree(response.getBody());
            // extract new access token and expiry from response
            String newAccessToken = root.path("access_token").asText();
            long expiresIn = root.path("expires_in").asLong(3600); // default 1 hour if not provided

            // update token and expiry in the database
            if (!newAccessToken.isEmpty()) {
                user.setZohoAccessToken(newAccessToken);
                user.setZohoTokenExpiry(Instant.now().plusSeconds(expiresIn));
                appUserRepository.save(user);
            }
        } catch (Exception e) {
            log.error("Failed to refresh Zoho access token for user: {}", user.getEmail(), e);
            // continue with existing token
        }
    }

    // parse the zoho creator json response into a list of ZohoProject objects
    private List<ZohoProject> parseProjects(String json) {
        List<ZohoProject> projects = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.path("data");
            if (data.isArray()) {
                int projectIndex = 1;
                for (JsonNode node : data) {
                    ZohoProject project = new ZohoProject();
                    String id = str(node, "ID");
                    project.setId(id);
                    
                    // Use Deal_Name, or fall back to a default name if empty
                    String dealName = str(node, "Deal_Name");
                    if (dealName == null || dealName.isBlank()) {
                        dealName = "Zoho Project #" + (id.isEmpty() ? projectIndex : id);
                    }
                    project.setDealName(dealName);
                    
                    project.setStage(str(node, "Stage"));
                    project.setCompanyWebsite(str(node, "Company_website"));
                    project.setCompanyContext(str(node, "What_is_the_company_about_for_context_"));
                    project.setProductPurpose(str(node, "What_is_the_purpose_of_this_product_"));
                    project.setProductionNotes(str(node, "Production_Notes"));
                    project.setCustomerConcerns(str(node, "Customer_Concerns"));
                    projects.add(project);
                    projectIndex++;
                }
            }
            log.info("Successfully parsed {} projects from Zoho response", projects.size());
        } catch (Exception e) {
            log.error("Raw JSON that failed to parse: {}", json);
        }
        return projects;
    }

    // zoho creator fields can be plain strings or {display_value, value} objects
    private String str(JsonNode node, String field) {
        JsonNode f = node.path(field);
        if (f.isMissingNode() || f.isNull()) return "";
        // v2.1 API uses zc_display_value for some fields
        if (f.isObject()) {
            String displayValue = f.path("zc_display_value").asText("");
            if (displayValue.isEmpty()) {
                displayValue = f.path("display_value").asText("");
            }
            return displayValue;
        }
        return f.asText("");
    }

    /**
     * Fetch scope/trigger data from the scope report for a specific project.
     * This data is used for generating automation diagrams.
     * 
     * @param user The authenticated user
     * @param projectId The project ID to filter by (optional)
     * @return List of ZohoScopeData records
     */
    public List<ZohoScopeData> fetchScopeData(AppUser user, String projectId) {
        if (user == null || user.getZohoAccessToken() == null) {
            return Collections.emptyList();
        }

        // refresh token if needed
        if (user.getZohoTokenExpiry() != null && Instant.now().isAfter(user.getZohoTokenExpiry().minusSeconds(60))) {
            refreshAccessToken(user);
        }

        // build zoho creator scope report api url
        String url;
        if (useV21Api) {
            url = apiBaseUrl + "/creator/v2.1/data/" + ownerName + "/" + appLinkName + "/report/" + scopeReportLinkName;
        } else {
            url = apiBaseUrl + "/api/v2/" + ownerName + "/" + appLinkName + "/report/" + scopeReportLinkName;
        }
        
        // add criteria filter if projectId provided
        if (projectId != null && !projectId.isEmpty()) {
            url += "?criteria=Project_ID==\"" + projectId + "\"";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Zoho-oauthtoken " + user.getZohoAccessToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            log.info("Zoho Scope API Response Status: {}", response.getStatusCode());
            
            return parseScopeData(response.getBody());
        } catch (Exception e) {
            log.error("Failed to fetch scope data from Zoho API", e);
            log.error("URL attempted: {}", url);
            return Collections.emptyList();
        }
    }



    /**
     * Parse scope data from Zoho API response.
     * Field names discovered from actual API response:
     * - If_automation_in_what_application_is_the_process_triggered (trigger app)
     * - Trigger_Description (trigger event description)
     * - product_type (Automation, Data Migration, Merge Document, etc.)
     * - Deal_ID (reference to deal)
     * - Product_Name (name of the product)
     * - Name (object with first_name, last_name)
     */
    private List<ZohoScopeData> parseScopeData(String json) {
        List<ZohoScopeData> scopeDataList = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.path("data");
            if (data.isArray()) {
                for (JsonNode node : data) {
                    ZohoScopeData scopeData = new ZohoScopeData();
                    scopeData.setId(str(node, "ID"));
                    scopeData.setProjectId(str(node, "Deal_ID"));
                    
                    // Store raw JSON for debugging
                    scopeData.setRawJson(node.toString());
                    
                    // Map actual fields from the scope report
                    scopeData.setProductDescription(str(node, "Product_Name"));
                    scopeData.setTriggerEventDescription(str(node, "Trigger_Description"));
                    scopeData.setTriggerApplicationName(str(node, "If_automation_in_what_application_is_the_process_triggered"));
                    
                    // product_type can help identify what kind of automation this is
                    String productType = str(node, "product_type");
                    scopeData.setSelectTrigger(productType);
                    
                    // Build a detailed description from available fields
                    StringBuilder detailedDesc = new StringBuilder();
                    String productName = str(node, "Product_Name");
                    String triggerDesc = str(node, "Trigger_Description");
                    String triggerApp = str(node, "If_automation_in_what_application_is_the_process_triggered");
                    
                    if (!productName.isEmpty()) {
                        detailedDesc.append("Product: ").append(productName).append(". ");
                    }
                    if (!triggerApp.isEmpty()) {
                        detailedDesc.append("Triggered in: ").append(triggerApp).append(". ");
                    }
                    if (!triggerDesc.isEmpty()) {
                        detailedDesc.append("Trigger: ").append(triggerDesc);
                    }
                    scopeData.setDetailedDescription(detailedDesc.toString().trim());
                    
                    scopeDataList.add(scopeData);
                }
            }
            log.info("Successfully parsed {} scope records from Zoho response", scopeDataList.size());
        } catch (Exception e) {
            log.error("Failed to parse scope data. Raw JSON: {}", json, e);
        }
        return scopeDataList;
    }
}
