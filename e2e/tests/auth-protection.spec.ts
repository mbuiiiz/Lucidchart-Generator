import { test, expect } from '@playwright/test';

// Requires Spring Boot running under a non-dev profile (e.g. SPRING_PROFILES_ACTIVE=oauth).
// Under the `dev` profile, ProdSecurityConfig is inactive and all routes are permitAll(),
// so no redirect happens and these tests will fail.

test('dashboard redirects to login when unauthenticated', async ({ page }) => {
  await page.goto('/dashboard');
  await expect(page).toHaveURL(/\/login/);
});

test('projects redirects to login when unauthenticated', async ({ page }) => {
  await page.goto('/projects');
  await expect(page).toHaveURL(/\/login/);
});

test('profile redirects to login when unauthenticated', async ({ page }) => {
  await page.goto('/profile');
  await expect(page).toHaveURL(/\/login/);
});
