# Zoho Integration - Summary

This feature connects the app to Zoho Creator so logged-in Zoho users can see their projects on the dashboard.

---

## What was added

**AppUser model** - 3 new nullable columns to store Zoho OAuth tokens per user: `zoho_access_token`, `zoho_refresh_token`, and `zoho_token_expiry`. Users who log in via Google or form login will have these as null.

**ZohoProject DTO** - a plain Java object (not a JPA entity) that maps fields from the Zoho Creator API response: ID, Deal Name, Stage, Company Website, Company Context, Product Purpose, Production Notes, and Customer Concerns.

**ZohoScopeData DTO** - a model for scope/trigger data used in diagram generation. Contains automation triggers, conditions, actions, and related fields from the scope report.

**ZohoApiService** - fetches projects and scope data from the Zoho Creator report API v2.1 using the user's stored access token. 
- Uses the correct Zoho API base URL (`https://www.zohoapis.com/creator/v2.1/data/...`)
- Automatically refreshes the token if it expires within 60 seconds
- Includes `fetchProjects()` for All_Projects report
- Includes `fetchScopeData()` for Automation_Product_Estimate_Scope_Tool_Report
- Logs field names from API responses for debugging

**OAuthLoginSuccessHandler** - after a successful Zoho OAuth login, saves the access token, refresh token, and expiry to the user's database row. The `OAuth2AuthorizedClientService` dependency is marked `@Nullable` so the app still starts under the `dev` profile. Fixed a bug where the fallback token expiry (`Instant.now().plusSeconds(3600)`) was computed but never assigned.

**ProdSecurityConfig** - added a custom `OAuth2AuthorizationRequestResolver` that injects `access_type=offline` into the Zoho authorization redirect. This tells Zoho to include a refresh token in the login response, which is required for the token refresh flow to work. Without it, Zoho only returns a short-lived access token with nothing to renew it. More info can be found here https://www.zoho.com/apptics/resources/api/oauthtoken.html

**PageController / dashboard.html** - the dashboard endpoint now resolves the logged-in user, calls `ZohoApiService.fetchProjects()`, and passes the result to the template showing all of their available projects

**application.properties / application-oauth.properties** - added Zoho Creator API config properties and added `ZohoCreator.report.READ` to the OAuth scope so the API can be called after login.

---

## API Configuration

The Zoho Creator API v2.1 is configured in `application.properties`:

```properties
# Zoho Creator API v2.1 configuration
zoho.creator.api-base-url=https://www.zohoapis.com
zoho.creator.owner-name=${ZOHO_OWNER_NAME:}
zoho.creator.app-link-name=${ZOHO_APP_LINK_NAME:}
zoho.creator.report-link-name=${ZOHO_REPORT_LINK_NAME:All_Projects}
zoho.creator.scope-report-link-name=${ZOHO_SCOPE_REPORT_LINK_NAME:Automation_Product_Estimate_Scope_Tool_Report}
```

---

## What still needs to be done

- Update AI prompt template for triggers/conditions/actions diagram generation
- Update `/generate-diagram` endpoint to use scope data
- Result page with XML preview and download button
- Re-enable CSRF (currently disabled in `ProdSecurityConfig` but not in rush to change)
- Change `frameOptions` from `disable()` to `sameOrigin()` before production

👍👍👍

