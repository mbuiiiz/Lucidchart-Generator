import { expect, type Page } from '@playwright/test';

export const TEST_PASSWORD = 'password123';
export const TEST_FULL_NAME = 'Playwright Test User';

export async function registerNewUser(page: Page): Promise<string> {
  const email = `test-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@example.com`;

  await page.goto('/register');
  await page.getByPlaceholder('John Pork').fill(TEST_FULL_NAME);
  await page.getByPlaceholder('johnpork123@gmail.com').fill(email);
  await page.getByPlaceholder('Min. 8 characters').fill(TEST_PASSWORD);
  await page.getByPlaceholder('Repeat your password').fill(TEST_PASSWORD);
  await page.getByRole('button', { name: 'Create Account' }).click();

  await expect(page).toHaveURL(/\/dashboard/);
  return email;
}
