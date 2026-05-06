import { test, expect } from '@playwright/test';

test('register page renders', async ({ page }) => {
  await page.goto('/register');

  await expect(page).toHaveTitle(/Sign Up/);
  await expect(page.getByPlaceholder('John Pork')).toBeVisible();
  await expect(page.getByPlaceholder('johnpork123@gmail.com')).toBeVisible();
  await expect(page.getByPlaceholder('Min. 8 characters')).toBeVisible();
  await expect(page.getByPlaceholder('Repeat your password')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Create Account' })).toBeVisible();
});

test('mismatched passwords show error', async ({ page }) => {
  await page.goto('/register');

  await page.getByPlaceholder('John Pork').fill('Test User');
  await page.getByPlaceholder('johnpork123@gmail.com').fill(`test-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@example.com`);
  await page.getByPlaceholder('Min. 8 characters').fill('password123');
  await page.getByPlaceholder('Repeat your password').fill('different456');
  await page.getByRole('button', { name: 'Create Account' }).click();

  await expect(page).toHaveURL(/\/register/);
  await expect(page.locator('.alert-error')).toBeVisible();
});

test('login link goes back to login page', async ({ page }) => {
  await page.goto('/register');

  await page.getByRole('link', { name: /sign in|log in/i }).click();

  await expect(page).toHaveURL(/\/login/);
});

const invalidCases = [
  {
    name: 'blank full name',
    fullName: '',
    email: 'valid@example.com',
    password: 'password123',
  },
  {
    name: 'blank email',
    fullName: 'Test User',
    email: '',
    password: 'password123',
  },
  {
    name: 'password under 8 characters',
    fullName: 'Test User',
    email: 'valid@example.com',
    password: 'short',
  },
];

for (const { name, fullName, email, password } of invalidCases) {
  test(`register fails on ${name}`, async ({ page }) => {
    await page.goto('/register');
    await page.getByPlaceholder('John Pork').fill(fullName);
    await page.getByPlaceholder('johnpork123@gmail.com').fill(email);
    await page.getByPlaceholder('Min. 8 characters').fill(password);
    await page.getByPlaceholder('Repeat your password').fill(password);
    await page.getByRole('button', { name: 'Create Account' }).click();

    await expect(page).toHaveURL(/\/register/);
    await expect(page.locator('.alert-error')).toBeVisible();
  });
}

test('successful registration auto-logs in and lands on dashboard', async ({ page }) => {
  const email = `test-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@example.com`;

  await page.goto('/register');
  await page.getByPlaceholder('John Pork').fill('Playwright Test User');
  await page.getByPlaceholder('johnpork123@gmail.com').fill(email);
  await page.getByPlaceholder('Min. 8 characters').fill('password123');
  await page.getByPlaceholder('Repeat your password').fill('password123');
  await page.getByRole('button', { name: 'Create Account' }).click();

  await expect(page).toHaveURL(/\/dashboard/);
});
