# Authentication Guide

This document describes how authentication is configured in `aetherxmlbridge`, how to run OAuth locally, and how to troubleshoot when OAuth appears to do nothing.

## Auth Modes

- `dev` profile:
  - Uses permissive security (`anyRequest().permitAll()`).
  - Intended for frontend/page development.
- Non-`dev` profiles:
  - Uses form login and OAuth2 login if OAuth client registrations are available.
  - This is the flow used for real auth behavior.

## OAuth Configuration Files

- Base config: `src/main/resources/application.properties`
- OAuth profile config: `src/main/resources/application-oauth.properties`
- Local environment values: `.env`

OAuth provider registrations (Google/Zoho) are defined in `application-oauth.properties`, so the `oauth` profile must be active for OAuth endpoints to work.

## Important Quirk: `.env` and Profile Activation

This project uses `springboot3-dotenv` to load `.env` values. A known behavior in this setup is:

- `.env` values can be loaded for regular properties.
- `SPRING_PROFILES_ACTIVE` from `.env` may not be applied early enough to activate profile-specific files like `application-oauth.properties`.

Result:

- App starts on default profile.
- `/oauth2/authorization/google` redirects back to `/login`.
- UI looks like "Continue with Google" does nothing.

## Local Run Command (Recommended for OAuth)

If OAuth does not work locally, run from the `aetherxmlbridge/` directory with the profile explicitly set in the shell:

```bash
SPRING_PROFILES_ACTIVE=oauth ./mvnw spring-boot:run
```

This ensures `application-oauth.properties` is loaded and OAuth routes are active.

## Quick Verification

After starting with the command above:

- Startup logs should indicate active profile includes `oauth`.
- Visiting `/oauth2/authorization/google` should redirect to Google, not back to `/login`.

## Typical OAuth Failure Checklist

1. Confirm app was started with `SPRING_PROFILES_ACTIVE=oauth`.
2. Confirm `.env` has non-empty values for:
   - `GOOGLE_CLIENT_ID`
   - `GOOGLE_CLIENT_SECRET`
3. Confirm you are not running with `SPRING_PROFILES_ACTIVE=dev` when testing auth behavior.
