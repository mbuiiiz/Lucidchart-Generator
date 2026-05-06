import { test, expect } from '@playwright/test';
import { registerNewUser, TEST_FULL_NAME, TEST_PASSWORD } from '../helpers/auth';

test('profile page shows current user info', async ({ page }) => {
  const email = await registerNewUser(page);

  await page.goto('/profile');

  await expect(page).toHaveTitle(/Profile/);
  await expect(page.getByRole('heading', { name: TEST_FULL_NAME })).toBeVisible();
  await expect(page.getByText(email).first()).toBeVisible();
});

test('user can update full name', async ({ page }) => {
  await registerNewUser(page);
  await page.goto('/profile');

  const newName = 'Updated Test Name';
  const fullNameCard = page.locator('.editable-card').filter({ hasText: 'Full Name' });

  await fullNameCard.getByRole('button', { name: 'Edit' }).click();
  await fullNameCard.getByRole('textbox').fill(newName);
  await fullNameCard.getByRole('button', { name: 'Save' }).click();

  await expect(page.getByText('Name updated successfully')).toBeVisible();
  await expect(page.getByRole('heading', { name: newName })).toBeVisible();
});

test('user can change password and log in with new one', async ({ page }) => {
  const email = await registerNewUser(page);
  const newPassword = 'newpassword456';

  await page.goto('/profile');
  await page.getByRole('button', { name: 'Change Password' }).click();

  const modal = page.locator('#passwordModal');
  await modal.getByPlaceholder('Old password').fill(TEST_PASSWORD);
  await modal.getByPlaceholder('New password', { exact: true }).fill(newPassword);
  await modal.getByPlaceholder('Repeat new password').fill(newPassword);
  await modal.getByRole('button', { name: 'Save' }).click();

  await expect(page.locator('#passwordSuccessMessage')).toBeVisible();

  await page.getByRole('button', { name: 'Logout' }).click();
  await expect(page).toHaveURL(/\/login/);

  await page.getByPlaceholder('johnpork123@gmail.com').fill(email);
  await page.getByPlaceholder('123456').fill(newPassword);
  await page.getByRole('button', { name: 'Login' }).click();

  await expect(page).toHaveURL(/\/dashboard/);
});

test('phone number shows "Not set" for new user and toggles edit form', async ({ page }) => {
  await registerNewUser(page);
  await page.goto('/profile');

  const phoneCard = page.locator('.editable-card').filter({ hasText: 'Phone Number' });

  await expect(phoneCard.getByText('Not set')).toBeVisible();
  await expect(phoneCard.getByPlaceholder('(123) 456-7890')).toBeHidden();

  await phoneCard.getByRole('button', { name: 'Add' }).click();
  await expect(phoneCard.getByPlaceholder('(123) 456-7890')).toBeVisible();

  await phoneCard.getByRole('button', { name: 'Cancel' }).click();
  await expect(phoneCard.getByPlaceholder('(123) 456-7890')).toBeHidden();
  await expect(phoneCard.getByText('Not set')).toBeVisible();
});

test('deleted account can no longer log in', async ({ page }) => {
  const email = await registerNewUser(page);

  await page.goto('/profile');
  await page.getByRole('button', { name: 'Delete Account' }).click();

  const confirm = page.locator('#deleteAccountConfirm');
  await confirm.getByPlaceholder('Enter your password to confirm').fill(TEST_PASSWORD);
  await confirm.getByRole('button', { name: 'Permanently Delete' }).click();

  await expect(page).toHaveURL(/\/$|\/login/);

  await page.goto('/dashboard');
  await expect(page).toHaveURL(/\/login/);

  await page.getByPlaceholder('johnpork123@gmail.com').fill(email);
  await page.getByPlaceholder('123456').fill(TEST_PASSWORD);
  await page.getByRole('button', { name: 'Login' }).click();

  await expect(page).toHaveURL(/\/login\?error/);
});
