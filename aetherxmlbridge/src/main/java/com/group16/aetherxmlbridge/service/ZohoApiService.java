package com.group16.aetherxmlbridge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group16.aetherxmlbridge.model.AppUser;
import com.group16.aetherxmlbridge.model.ZohoProject;
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
    @Value("${zoho.creator.base-url:https://creator.zoho.com}")
    private String creatorBaseUrl;

    @Value("${zoho.creator.owner-name:}")
    private String ownerName;

    @Value("${zoho.creator.app-link-name:}")
    private String appLinkName;

    @Value("${zoho.creator.report-link-name:All_Projects}")
    private String reportLinkName;

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
        String url = creatorBaseUrl + "/api/v2/" + ownerName + "/" + appLinkName + "/report/" + reportLinkName;

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
                for (JsonNode node : data) {
                    ZohoProject project = new ZohoProject();
                    project.setId(str(node, "ID"));
                    project.setDealName(str(node, "Deal_Name"));
                    project.setStage(str(node, "Stage"));
                    project.setCompanyWebsite(str(node, "Company_website"));
                    project.setCompanyContext(str(node, "What_is_the_company_about_for_context_"));
                    project.setProductPurpose(str(node, "What_is_the_purpose_of_this_product_"));
                    project.setProductionNotes(str(node, "Production_Notes"));
                    project.setCustomerConcerns(str(node, "Customer_Concerns"));
                    projects.add(project);
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
        if (f.isObject()) return f.path("display_value").asText("");
        return f.asText("");
    }
}
