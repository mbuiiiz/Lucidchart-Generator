import { test, expect } from '@playwright/test';
import { registerNewUser } from '../helpers/auth';

test('projects page shows empty state for user without zoho', async ({ page }) => {
  await registerNewUser(page);

  await page.goto('/projects');

  await expect(page).toHaveTitle(/Projects/);
  await expect(page.getByRole('heading', { name: 'No Projects Found' })).toBeVisible();
});

test('sidebar projects link navigates to projects page', async ({ page }) => {
  await registerNewUser(page);

  await page.getByRole('link', { name: 'Projects' }).click();

  await expect(page).toHaveURL(/\/projects/);
  await expect(page).toHaveTitle(/Projects - AetherGen/);
});
