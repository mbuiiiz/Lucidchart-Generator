# Bugfix: OAuth2 Principal Resolution in Dashboard Controller

## Observed Error (found on Render Log)

```
2026-03-20T22:25:17.005Z ERROR 1 --- [aetherxmlbridge] [nio-8080-exec-8] o.a.c.c.C.[.[.[/].[dispatcherServlet] : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: org.thymeleaf.exceptions.TemplateInputException: An error happened during template parsing (template: "class path resource [templates/dashboard.html]")] with root cause
org.springframework.expression.spel.SpelEvaluationException: EL1007E: Property or field 'fullName' cannot be found on null
```

## Root Cause

`PageController#getUserDashboard` called `principal.getName()` to look up the authenticated user by email. For form-login users this works - Spring Security sets the username (email) as the principal name. For OAuth2 users, `principal.getName()` returns the **provider's subject ID** (e.g. Google's `sub`: `111177188592592308644`), not the email. This caused `findByEmail()` to return empty, `currentUser` to be `null`, and Thymeleaf to throw a `SpelEvaluationException` on `currentUser.fullName`.

## Fix

Cast the principal to `OAuth2AuthenticationToken` when applicable and extract the email directly from the OAuth2 user attributes, consistent with how `OAuthLoginSuccessHandler` resolves it.

```java
if (principal instanceof OAuth2AuthenticationToken oauthToken) {
    OAuth2User oauthUser = oauthToken.getPrincipal();
    email = oauthUser.getAttribute("Email"); // Zoho
    if (email == null) email = oauthUser.getAttribute("email"); // Google
} else {
    email = principal.getName(); // form login
}
```

## Secondary Issue (Not Fixed) but just keep an eye on it because that how it is supposed to be

On Render's free tier, the app spins down between deploys. If a user initiates an OAuth flow just before a redeploy, the in-memory session holding the OAuth2 state is lost. On callback, Spring Security cannot validate the state parameter and fails to `/login?error`.
