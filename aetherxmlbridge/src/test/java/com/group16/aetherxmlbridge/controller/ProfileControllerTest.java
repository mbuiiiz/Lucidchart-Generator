package com.group16.aetherxmlbridge.controller;

import com.group16.aetherxmlbridge.service.AppUserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.security.Principal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProfileControllerTest {

    private MockMvc mockMvc;
    private AppUserService appUserService;

    @BeforeEach
    void setUp() {
        appUserService = mock(AppUserService.class);
        ProfileController controller = new ProfileController(appUserService);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /profile/update-name
    // -------------------------------------------------------------------------

    @Test
    void updateName_validInput_callsServiceAndRedirects() throws Exception {
        mockMvc.perform(post("/profile/update-name")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("fullName", "New Name")
                .principal(() -> "user@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?nameSuccess=1"));

        verify(appUserService).updateFullName("user@example.com", "New Name");
    }

    @Test
    void updateName_blankName_skipsServiceAndRedirects() throws Exception {
        mockMvc.perform(post("/profile/update-name")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("fullName", "   ")
                .principal(() -> "user@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?nameSuccess=1"));

        verify(appUserService, never()).updateFullName(anyString(), anyString());
    }

    @Test
    void updateName_nullPrincipal_skipsServiceAndRedirects() throws Exception {
        mockMvc.perform(post("/profile/update-name")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("fullName", "New Name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?nameSuccess=1"));

        verify(appUserService, never()).updateFullName(anyString(), anyString());
    }

    @Test
    void updateName_trimsWhitespaceBeforeSaving() throws Exception {
        mockMvc.perform(post("/profile/update-name")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("fullName", "  Padded Name  ")
                .principal(() -> "user@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?nameSuccess=1"));

        verify(appUserService).updateFullName("user@example.com", "Padded Name");
    }

    // -------------------------------------------------------------------------
    // POST /profile/update-phone
    // -------------------------------------------------------------------------

    @Test
    void updatePhone_validPhone_callsServiceAndRedirects() throws Exception {
        mockMvc.perform(post("/profile/update-phone")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("phoneNumber", "+12345678901")
                .principal(() -> "user@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?phoneSuccess=1"));

        verify(appUserService).updatePhoneNumber("user@example.com", "+12345678901");
    }

    @Test
    void updatePhone_nullPrincipal_skipsServiceAndRedirects() throws Exception {
        mockMvc.perform(post("/profile/update-phone")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("phoneNumber", "+12345678901"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        verify(appUserService, never()).updatePhoneNumber(anyString(), any());
    }

    @Test
    void updatePhone_invalidFormat_redirectsWithPhoneError() throws Exception {
        doThrow(new IllegalArgumentException("Invalid phone number format"))
                .when(appUserService).updatePhoneNumber("user@example.com", "not-a-number");

        mockMvc.perform(post("/profile/update-phone")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("phoneNumber", "not-a-number")
                .principal(() -> "user@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?phoneError=Invalid+phone+number+format"));
    }

    @Test
    void updatePhone_emptyPhone_redirectsWithPhoneError() throws Exception {
        doThrow(new IllegalArgumentException("Phone number is required"))
                .when(appUserService).updatePhoneNumber("user@example.com", "");

        mockMvc.perform(post("/profile/update-phone")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("phoneNumber", "")
                .principal(() -> "user@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?phoneError=Phone+number+is+required"));
    }

    // -------------------------------------------------------------------------
    // POST /profile/delete-account
    // -------------------------------------------------------------------------

    @Test
    void deleteAccount_correctPassword_logsOutAndRedirectsToRoot() throws Exception {
        mockMvc.perform(post("/profile/delete-account")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("password", "correctPass")
                .principal(() -> "user@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(appUserService).deleteAccount("user@example.com", "correctPass");
    }

    @Test
    void deleteAccount_wrongPassword_redirectsToProfileWithDeleteError() throws Exception {
        doThrow(new IllegalArgumentException("Incorrect password"))
                .when(appUserService).deleteAccount("user@example.com", "wrongPass");

        mockMvc.perform(post("/profile/delete-account")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("password", "wrongPass")
                .principal(() -> "user@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?deleteError=1"));
    }

    @Test
    void deleteAccount_blankPassword_skipsServiceAndRedirectsToProfile() throws Exception {
        mockMvc.perform(post("/profile/delete-account")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("password", "   ")
                .principal(() -> "user@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        verify(appUserService, never()).deleteAccount(anyString(), anyString());
    }

    @Test
    void deleteAccount_nullPrincipal_skipsServiceAndRedirectsToProfile() throws Exception {
        mockMvc.perform(post("/profile/delete-account")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("password", "correctPass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        verify(appUserService, never()).deleteAccount(anyString(), anyString());
    }

    // -------------------------------------------------------------------------
    // POST /profile/change-password
    // -------------------------------------------------------------------------

    @Test
    void changePassword_validInputs_redirectsToProfileWithSuccess() throws Exception {
        mockMvc.perform(post("/profile/change-password")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("oldPassword", "oldPass123")
                .param("newPassword", "newPass456")
                .param("repeatPassword", "newPass456")
                .principal(() -> "user@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?passwordSuccess=1"));

        verify(appUserService).changePassword("user@example.com", "oldPass123", "newPass456");
    }

    @Test
    void changePassword_passwordMismatch_redirectsWithError() throws Exception {
        mockMvc.perform(post("/profile/change-password")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("oldPassword", "oldPass123")
                .param("newPassword", "newPass456")
                .param("repeatPassword", "different789")
                .principal(() -> "user@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?passwordError=Passwords+do+not+match"));

        verify(appUserService, never()).changePassword(anyString(), anyString(), anyString());
    }

    @Test
    void changePassword_wrongOldPassword_redirectsWithServiceError() throws Exception {
        doThrow(new IllegalArgumentException("Incorrect old password"))
                .when(appUserService).changePassword("user@example.com", "wrongOld", "newPass456");

        mockMvc.perform(post("/profile/change-password")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("oldPassword", "wrongOld")
                .param("newPassword", "newPass456")
                .param("repeatPassword", "newPass456")
                .principal(() -> "user@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?passwordError=Incorrect+old+password"));
    }

    @Test
    void changePassword_newPasswordTooShort_redirectsWithServiceError() throws Exception {
        doThrow(new IllegalArgumentException("Password must be at least 8 characters"))
                .when(appUserService).changePassword("user@example.com", "oldPass123", "short");

        mockMvc.perform(post("/profile/change-password")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("oldPassword", "oldPass123")
                .param("newPassword", "short")
                .param("repeatPassword", "short")
                .principal(() -> "user@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?passwordError=Password+must+be+at+least+8+characters"));
    }

    @Test
    void changePassword_sameAsOldPassword_redirectsWithServiceError() throws Exception {
        doThrow(new IllegalArgumentException("New password must be different from old password"))
                .when(appUserService).changePassword("user@example.com", "oldPass123", "oldPass123");

        mockMvc.perform(post("/profile/change-password")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("oldPassword", "oldPass123")
                .param("newPassword", "oldPass123")
                .param("repeatPassword", "oldPass123")
                .principal(() -> "user@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?passwordError=New+password+must+be+different+from+old+password"));
    }

    // -------------------------------------------------------------------------
    // POST /forgot-password
    // -------------------------------------------------------------------------

    @Test
    void forgotPassword_existingEmail_redirectsToResetPasswordWithToken() throws Exception {
        when(appUserService.createPasswordResetToken("user@example.com")).thenReturn("abc-token-123");

        mockMvc.perform(post("/forgot-password")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "user@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reset-password?token=abc-token-123"));
    }

    @Test
    void forgotPassword_unknownEmail_returnsViewWithMessage() throws Exception {
        when(appUserService.createPasswordResetToken("nobody@example.com")).thenReturn(null);

        mockMvc.perform(post("/forgot-password")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "nobody@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"))
                .andExpect(model().attribute("message", "If this email exists, reset instructions were sent"));
    }

    // -------------------------------------------------------------------------
    // POST /reset-password
    // -------------------------------------------------------------------------

    @Test
    void resetPassword_validToken_redirectsToLoginWithSuccess() throws Exception {
        mockMvc.perform(post("/reset-password")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "valid-token")
                .param("newPassword", "brandNewPass")
                .param("repeatPassword", "brandNewPass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?resetSuccess=1"));

        verify(appUserService).resetPasswordByToken("valid-token", "brandNewPass");
    }

    @Test
    void resetPassword_passwordMismatch_returnsViewWithError() throws Exception {
        mockMvc.perform(post("/reset-password")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "valid-token")
                .param("newPassword", "newPass123")
                .param("repeatPassword", "different456"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attribute("error", "Passwords do not match"))
                .andExpect(model().attribute("token", "valid-token"));

        verify(appUserService, never()).resetPasswordByToken(anyString(), anyString());
    }

    @Test
    void resetPassword_invalidToken_returnsViewWithError() throws Exception {
        doThrow(new IllegalArgumentException("Invalid token"))
                .when(appUserService).resetPasswordByToken("bad-token", "newPass123");

        mockMvc.perform(post("/reset-password")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "bad-token")
                .param("newPassword", "newPass123")
                .param("repeatPassword", "newPass123"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attribute("error", "Invalid token"))
                .andExpect(model().attribute("token", "bad-token"));
    }

    @Test
    void resetPassword_expiredToken_returnsViewWithError() throws Exception {
        doThrow(new IllegalArgumentException("Token expired"))
                .when(appUserService).resetPasswordByToken("expired-token", "newPass123");

        mockMvc.perform(post("/reset-password")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "expired-token")
                .param("newPassword", "newPass123")
                .param("repeatPassword", "newPass123"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attribute("error", "Token expired"))
                .andExpect(model().attribute("token", "expired-token"));
    }

    @Test
    void resetPassword_passwordTooShort_returnsViewWithError() throws Exception {
        doThrow(new IllegalArgumentException("Password must be at least 8 characters"))
                .when(appUserService).resetPasswordByToken("valid-token", "short");

        mockMvc.perform(post("/reset-password")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "valid-token")
                .param("newPassword", "short")
                .param("repeatPassword", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attribute("error", "Password must be at least 8 characters"));
    }
}
