# End-to-End Tests

Playwright suite covering authentication, account management, and the main navigation flows of the Lucidchart Generator app.

Tests run against a live Spring Boot instance. Playwright's `webServer` config auto-starts the backend before tests, so one command boots the app, runs everything, and shuts it down.

## Running locally

From this directory:

```bash
npm ci
npx playwright install --with-deps   # first time only
npx playwright test                  # headless, all browsers
npx playwright test --ui             # interactive mode, easier when writing tests
npx playwright show-report           # open the HTML report after a run
```

Tests target `http://localhost:8080`. If a backend is already running there, Playwright reuses it. Otherwise it starts one with `./mvnw spring-boot:run`.

The auth-protection tests need a non-`dev` Spring profile so `ProdSecurityConfig` is active. The project's `.env` already sets `SPRING_PROFILES_ACTIVE=oauth`, so this is automatic in normal use.

## Continuous integration

Runs on every push and pull request to `main` via `.github/workflows/playwright.yml`. The HTML report is uploaded as an artifact so failures can be inspected without re-running locally.

## Test coverage

28 tests across eight spec files. Every test runs three times (Chromium, Firefox, WebKit), so any failure shows up consistently across rendering engines.

### `login.spec.ts`

- Login page renders with the expected fields and submit button.
- Invalid credentials redirect to `/login?error` and show the error alert.
- The "Sign up" link routes to the registration page.

### `register.spec.ts`

- Registration page renders all required fields.
- Mismatched passwords keep the user on `/register` with an error alert.
- Three parameterized validation cases (blank name, blank email, password under 8 characters) each surface a distinct error.
- The "Login" link goes back to `/login`.
- A successful registration auto-authenticates and lands on `/dashboard`.

### `auth-protection.spec.ts`

- Unauthenticated requests to `/dashboard`, `/projects`, and `/profile` each redirect to `/login`. Confirms `ProdSecurityConfig` is actually enforcing auth on protected routes.

### `auth-flow.spec.ts`

Multi-page user journeys:

- Logout returns the user to `/login?logout` with the "You have been logged out." banner.
- Logout invalidates the session. Visiting `/dashboard` after logout redirects back to `/login`.
- A registered user can log out and then log back in with their original credentials.

### `profile.spec.ts`

- Profile page renders the authenticated user's full name and email.
- Updating the full name persists, and the page heading reflects the new value.
- Changing the password through the modal works, and the user can log in with the new password.
- The phone-number card shows "Not set" for new users, and the edit form toggles open and closed correctly.
- Deleting the account invalidates the session, and the deleted credentials no longer authenticate.

### `password-reset.spec.ts`

- A reset request for a non-existent email shows the same generic message as a valid one. No email-enumeration leak.
- Full reset journey: request reset, set a new password, log in with it.
- The old password is rejected after a successful reset, so the previous credential really is gone.

### `dashboard.spec.ts`

- A new user with no Zoho connection sees the dashboard stat cards and the "Zoho Account Not Connected" prompt.
- The "Connect Zoho" link's `href` points at the correct Spring Security OAuth entry point. Tests don't follow the link, they just verify the target.

### `projects.spec.ts`

- A user without a Zoho connection sees the "No Projects Found" empty state.
- The sidebar nav link routes correctly to `/projects`.

## Patterns used

**Shared helpers.** `helpers/auth.ts` holds the recurring setup (registering a fresh user, the test password, the test name). Spec files import what they need so test bodies stay focused on what's actually being verified.

**Parameterized tests.** `register.spec.ts` generates three validation tests from one array using a top-level `for...of` loop. Adding a new case is one entry in the array.

**Scoped locators.** The profile page has multiple cards with the same role/text patterns. `.locator('.editable-card').filter({ hasText: 'Full Name' })` anchors subsequent queries to a specific section so strict-mode collisions don't happen.

**Unique test data.** User emails include both `Date.now()` and a random suffix. Without the random part, three browser projects running the same test in parallel can land on the same millisecond and collide on a duplicate email.

**Boundary-only network assertions.** External integrations like Zoho OAuth are tested by asserting the link target, not by following the redirect. Keeps tests deterministic and free of third-party dependencies.

## Deliberately out of scope

A few things aren't covered, on purpose.

**Diagram generation.** The headline product feature needs a real Zoho connection and a real Gemini API key. End-to-end testing it would require mocking both providers at the request level, which is better handled at the backend integration-test layer than from Playwright.

**Admin pages.** They're gated behind an `ADMIN` role that normal registration doesn't grant. Testing would mean either seeding the DB directly or adding an admin-creation endpoint, neither of which felt worth the cost.

**Phone number save.** The form has client-side validation that gates the save button. The toggle test covers the user-visible interaction without coupling to the JS validation logic, which can change.

**Email-driven flows.** The current password-reset implementation puts the reset token in a redirect URL rather than emailing it, so it's testable as-is. A production reset flow would normally email the token, which would need either an email-capture service or a backend hook to retrieve it.

## Stack

- Playwright with its bundled TypeScript runner.
- TypeScript, `node16` module resolution. No build step. Playwright transpiles tests on the fly.
- GitHub Actions for CI, running on Ubuntu against all three browser engines.
