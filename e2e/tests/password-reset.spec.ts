import { test, expect } from '@playwright/test';
import { registerNewUser, TEST_PASSWORD } from '../helpers/auth';

test('forgot-password with unknown email shows generic message', async ({ page }) => {
  await page.goto('/forgot-password');

  await page.getByPlaceholder('johnpork123@gmail.com').fill('nobody@example.com');
  await page.getByRole('button', { name: 'Continue' }).click();

  await expect(page.getByText(/if this email exists/i)).toBeVisible();
});

test('user can reset password and log in with new one', async ({ page }) => {
  const email = await registerNewUser(page);
  const newPassword = 'resetpassword789';

  await page.getByRole('button', { name: 'Logout' }).click();
  await expect(page).toHaveURL(/\/login/);

  await page.getByRole('link', { name: /forgot password/i }).click();
  await expect(page).toHaveURL(/\/forgot-password/);

  await page.getByPlaceholder('johnpork123@gmail.com').fill(email);
  await page.getByRole('button', { name: 'Continue' }).click();

  await expect(page).toHaveURL(/\/reset-password\?token=/);

  await page.getByPlaceholder('At least 8 characters').fill(newPassword);
  await page.getByPlaceholder('Repeat your new password').fill(newPassword);
  await page.getByRole('button', { name: 'Reset Password' }).click();

  await expect(page).toHaveURL(/\/login\?resetSuccess/);
  await expect(page.getByText('Password was reset successfully. Please sign in.')).toBeVisible();

  await page.getByPlaceholder('johnpork123@gmail.com').fill(email);
  await page.getByPlaceholder('123456').fill(newPassword);
  await page.getByRole('button', { name: 'Login' }).click();

  await expect(page).toHaveURL(/\/dashboard/);
});

test('old password no longer works after reset', async ({ page }) => {
  const email = await registerNewUser(page);
  const newPassword = 'resetpassword789';

  await page.getByRole('button', { name: 'Logout' }).click();
  await page.goto('/forgot-password');
  await page.getByPlaceholder('johnpork123@gmail.com').fill(email);
  await page.getByRole('button', { name: 'Continue' }).click();

  await page.getByPlaceholder('At least 8 characters').fill(newPassword);
  await page.getByPlaceholder('Repeat your new password').fill(newPassword);
  await page.getByRole('button', { name: /reset|save|submit/i }).click();
  await expect(page).toHaveURL(/\/login\?resetSuccess/);

  await page.getByPlaceholder('johnpork123@gmail.com').fill(email);
  await page.getByPlaceholder('123456').fill(TEST_PASSWORD);
  await page.getByRole('button', { name: 'Login' }).click();

  await expect(page).toHaveURL(/\/login\?error/);
});
