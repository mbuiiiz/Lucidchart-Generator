import { test, expect } from '@playwright/test';
import { registerNewUser, TEST_PASSWORD } from '../helpers/auth';

test('logout returns user to login with success banner', async ({ page }) => {
  await registerNewUser(page);

  await page.getByRole('button', { name: 'Logout' }).click();

  await expect(page).toHaveURL(/\/login\?logout/);
  await expect(page.getByText('You have been logged out.')).toBeVisible();
});

test('logout invalidates session', async ({ page }) => {
  await registerNewUser(page);

  await page.getByRole('button', { name: 'Logout' }).click();
  await expect(page).toHaveURL(/\/login/);

  await page.goto('/dashboard');
  await expect(page).toHaveURL(/\/login/);
});

test('existing user can log in with correct password', async ({ page }) => {
  const email = await registerNewUser(page);
  await page.getByRole('button', { name: 'Logout' }).click();
  await expect(page).toHaveURL(/\/login/);

  await page.getByPlaceholder('johnpork123@gmail.com').fill(email);
  await page.getByPlaceholder('123456').fill(TEST_PASSWORD);
  await page.getByRole('button', { name: 'Login' }).click();

  await expect(page).toHaveURL(/\/dashboard/);
});
