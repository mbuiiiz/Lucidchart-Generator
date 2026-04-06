# Service Test Overview

**Last updated:** 2026-04-05

These are unit tests I added for the core services - they test one piece of code at a time by faking (mocking) everything that piece depends on. For example, instead of using a real database we create a fake one that just returns whatever we tell it to. This way tests run fast and work on any machine without any setup.

---

## AppUserServiceTest

**File:** `src/test/java/.../service/AppUserServiceTest.java`

`AppUserService` is the code that handles everything to do with user accounts - registering, logging in, changing passwords, etc. These tests check that this code behaves correctly in different situations.

The database and the password hasher are faked, so no real data is stored.

**What each test checks:**

- **loadUserByUsername** - Does it return the right user when the email exists? Does it throw an error when the email does not exist?
- **registerUser** - Does a new user get saved? Does `a@a.com` get assigned admin role? Does it reject an email that is already registered?
- **updateFullName** - Does it save the new name? Does it throw an error if the email does not exist?
- **updatePhoneNumber** - Does it save the new phone number? Does it handle a null phone number? Does it throw an error if the email does not exist?
- **deleteAccount** - Does it delete the account when the password is correct? Does it throw an error when the password is wrong? Does it throw an error if the email does not exist?
- **changePassword** - Does it update the password when everything is valid? Does it reject a wrong old password? Does it reject a password shorter than 8 characters? Does it reject a null password? Does it reject a new password that is the same as the old one? Does it throw an error if the email does not exist?
- **createPasswordResetToken** - Does it create a token and save it? Is the token set to expire in about 30 minutes? Does it return null if the email does not exist?
- **resetPasswordByToken** - Does it update the password and clear the token when the token is valid? Does it reject a token that does not exist? Does it reject a token that has expired? Does it reject a null expiry? Does it reject a password shorter than 8 characters? Does it reject a new password that is the same as the old one?

---

## ProfileControllerTest

**File:** `src/test/java/.../controller/ProfileControllerTest.java`

`ProfileController` handles the buttons and forms on the profile page. When a user submits a form (like changing their name), the controller receives that request and decides what to do. These tests simulate a user submitting each form and check that the controller responds correctly.

`AppUserService` is faked so we are only testing the controller's decision-making, not the database logic.

**What each test checks:**

- **Update name form** - Does it save the name when the input is valid? Does it skip saving if the name is blank? Does it skip saving if no one is logged in? Does it trim extra spaces before saving?
- **Update phone form** - Does it save the phone number? Does it skip saving if no one is logged in? Does it pass through an empty phone number?
- **Delete account form** - Does it log the user out and send them to the home page when the password is correct? Does it send them back to the profile page with an error when the password is wrong? Does it skip deleting if the password field is blank or no one is logged in?
- **Change password form** - Does it send the user to a success page when everything is valid? Does it show an error when the two new passwords do not match? Does it show an error when the old password is wrong? Does it show an error when the new password is too short? Does it show an error when the new password is the same as the old one?
- **Forgot password form** - Does it redirect to the reset page with a token when the email exists? Does it show a generic message (not reveal if the email exists) when the email is not found?
- **Reset password form** - Does it redirect to the login page on success? Does it show an error when the two passwords do not match? Does it show an error when the token is invalid or expired? Does it show an error when the new password is too short?

---

## GenerateDiagramControllerTest

**File:** `src/test/java/.../controller/GenerateDiagramControllerTest.java`

`GenerateDiagramController` is the code that takes user input, sends it to the AI, gets back a JSON response, and turns it into a diagram file the user can download. These tests check everything that happens **after** the AI responds - we are not testing the AI itself.

The AI is faked to always return a fixed JSON string we control. This means the tests are predictable and do not cost any API credits.

**What each test checks:**

- **Normal case** - Does it return a valid diagram file? Is the deal name shown in the diagram? Is the project ID shown in the diagram? Is the filename in the download header correct?
- **AI returns JSON wrapped in code blocks** - Sometimes the AI wraps its response in backticks like ` ```json `. Does the code strip those off before trying to read the JSON?
- **AI returns something unusable** - If the AI returns text that is not valid JSON, does the code return a clean error message instead of crashing? What if the AI throws an error entirely? What if it returns nothing?
- **Empty inputs** - If the user leaves fields blank, does the diagram still show placeholder text instead of breaking?
- **AI leaves fields out of the JSON** - If the AI does not include a trigger or module in its response, does the diagram show a fallback label instead of a blank?
- **Special characters** - If the deal name or content contains characters like `&`, `<`, or `"`, are they safely escaped so they do not break the XML file?
- **Filename special characters** - If the deal name has characters like `/` or `&`, are they replaced with `_` in the download filename?
- **Long text** - If the user types more than 220 characters into a field, is it cut off with `...` so it fits in the diagram box? If it is exactly 220 characters, is it left alone?
- **Diagram structure** - Does the output XML contain the correct building blocks (cells and edges) that draw.io needs to render the diagram?
