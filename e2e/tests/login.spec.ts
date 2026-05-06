import { test, expect } from '@playwright/test';

test('login page renders', async ({ page }) => {
  await page.goto('/login');

  await expect(page).toHaveTitle(/Login/);
  await expect(page.getByPlaceholder('johnpork123@gmail.com')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Login' })).toBeVisible();
});

test('shows error on invalid credentials', async ({ page }) => {
  await page.goto('/login');

  await page.getByPlaceholder('johnpork123@gmail.com').fill('nobody@example.com');
  await page.getByPlaceholder('123456').fill('wrong-password');
  await page.getByRole('button', { name: 'Login' }).click();

  await expect(page).toHaveURL(/\/login\?error/);
  await expect(page.getByText('Invalid email or password.')).toBeVisible();
});

test('sign up link goes to register page', async ({ page }) => {
  await page.goto('/login');

  await page.getByRole('link', { name: 'Sign up' }).click();

  await expect(page).toHaveURL(/\/register/);
});
