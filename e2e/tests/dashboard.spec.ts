import { test, expect } from '@playwright/test';
import { registerNewUser } from '../helpers/auth';

test('new user dashboard shows stat cards and zoho connect prompt', async ({ page }) => {
  await registerNewUser(page);

  await expect(page).toHaveTitle(/Dashboard/);
  await expect(page.getByRole('heading', { name: 'Total Projects' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Diagrams Generated' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'API Status' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Zoho Account Not Connected' })).toBeVisible();
});

test('connect zoho link points at oauth endpoint', async ({ page }) => {
  await registerNewUser(page);

  const connectLink = page.getByRole('link', { name: /connect.*zoho|continue with zoho/i });
  await expect(connectLink).toHaveAttribute('href', /\/oauth2\/authorization\/zoho/);
});
