package com.group16.aetherxmlbridge.service;

import com.group16.aetherxmlbridge.model.AppUser;
import com.group16.aetherxmlbridge.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AppUserServiceTest {

    private AppUserRepository appUserRepository;
    private PasswordEncoder passwordEncoder;
    private AppUserService appUserService;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        appUserService = new AppUserService(appUserRepository, passwordEncoder);

        when(passwordEncoder.encode(anyString())).thenAnswer(i -> "hashed_" + i.getArgument(0));
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenAnswer(i -> ("hashed_" + i.getArgument(0)).equals(i.getArgument(1)));
    }

    // -------------------------------------------------------------------------
    // loadUserByUsername
    // -------------------------------------------------------------------------

    @Test
    void loadUserByUsername_existingEmail_returnsUser() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThat(appUserService.loadUserByUsername("user@example.com")).isEqualTo(user);
    }

    @Test
    void loadUserByUsername_unknownEmail_throwsUsernameNotFoundException() {
        when(appUserRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("nobody@example.com");
    }

    // -------------------------------------------------------------------------
    // registerUser
    // -------------------------------------------------------------------------

    @Test
    void registerUser_newEmail_savesAndReturnsUser() {
        when(appUserRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        AppUser result = appUserService.registerUser("Alice", "new@example.com", "password123");

        assertThat(result.getFullName()).isEqualTo("Alice");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getRole()).isEqualTo("ROLE_USER");
        assertThat(result.getPasswordHash()).isEqualTo("hashed_password123");
        verify(appUserRepository).save(any(AppUser.class));
    }

    @Test
    void registerUser_adminEmail_assignsRoleAdmin() {
        when(appUserRepository.findByEmail("a@a.com")).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        AppUser result = appUserService.registerUser("Admin", "a@a.com", "password123");

        assertThat(result.getRole()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void registerUser_duplicateEmail_throwsIllegalArgumentException() {
        AppUser existing = buildUser("dupe@example.com", "ROLE_USER");
        when(appUserRepository.findByEmail("dupe@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> appUserService.registerUser("Bob", "dupe@example.com", "password123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");

        verify(appUserRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // updateFullName
    // -------------------------------------------------------------------------

    @Test
    void updateFullName_existingUser_savesNewName() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        appUserService.updateFullName("user@example.com", "New Name");

        assertThat(user.getFullName()).isEqualTo("New Name");
        verify(appUserRepository).save(user);
    }

    @Test
    void updateFullName_unknownEmail_throwsUsernameNotFoundException() {
        when(appUserRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.updateFullName("ghost@example.com", "Name"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // updatePhoneNumber
    // -------------------------------------------------------------------------

    @Test
    void updatePhoneNumber_validInternationalFormat_normalizesAndSaves() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        // Dashes are stripped during normalization, leaving +12345678901
        appUserService.updatePhoneNumber("user@example.com", "+1-234-567-8901");

        assertThat(user.getPhoneNumber()).isEqualTo("+12345678901");
        verify(appUserRepository).save(user);
    }

    @Test
    void updatePhoneNumber_nullPhone_throwsIllegalArgumentException() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.updatePhoneNumber("user@example.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phone number is required");
    }

    @Test
    void updatePhoneNumber_blankPhone_throwsIllegalArgumentException() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.updatePhoneNumber("user@example.com", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phone number is required");
    }

    @Test
    void updatePhoneNumber_invalidFormat_throwsIllegalArgumentException() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        // Missing leading + so it won't match ^\\+[0-9]{11,15}$
        assertThatThrownBy(() -> appUserService.updatePhoneNumber("user@example.com", "12345678901"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid phone number format");
    }

    @Test
    void updatePhoneNumber_unknownEmail_throwsUsernameNotFoundException() {
        when(appUserRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.updatePhoneNumber("ghost@example.com", "+12345678901"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // deleteAccount
    // -------------------------------------------------------------------------

    @Test
    void deleteAccount_correctPassword_deletesUser() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        user.setPasswordHash("hashed_correctPass");
        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        appUserService.deleteAccount("user@example.com", "correctPass");

        verify(appUserRepository).delete(user);
    }

    @Test
    void deleteAccount_wrongPassword_throwsIllegalArgumentException() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        user.setPasswordHash("hashed_correctPass");
        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.deleteAccount("user@example.com", "wrongPass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Incorrect password");

        verify(appUserRepository, never()).delete(any());
    }

    @Test
    void deleteAccount_unknownEmail_throwsUsernameNotFoundException() {
        when(appUserRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.deleteAccount("ghost@example.com", "anyPass"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // changePassword
    // -------------------------------------------------------------------------

    @Test
    void changePassword_validInputs_updatesPasswordHash() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        user.setPasswordHash("hashed_oldPass123");
        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        appUserService.changePassword("user@example.com", "oldPass123", "newPass456");

        assertThat(user.getPasswordHash()).isEqualTo("hashed_newPass456");
        verify(appUserRepository).save(user);
    }

    @Test
    void changePassword_wrongOldPassword_throwsIllegalArgumentException() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        user.setPasswordHash("hashed_correctOld");
        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.changePassword("user@example.com", "wrongOld", "newPass456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Incorrect old password");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void changePassword_newPasswordTooShort_throwsIllegalArgumentException() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        user.setPasswordHash("hashed_oldPass123");
        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.changePassword("user@example.com", "oldPass123", "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    void changePassword_newPasswordNull_throwsIllegalArgumentException() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        user.setPasswordHash("hashed_oldPass123");
        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.changePassword("user@example.com", "oldPass123", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    void changePassword_newPasswordSameAsOld_throwsIllegalArgumentException() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        user.setPasswordHash("hashed_oldPass123");
        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.changePassword("user@example.com", "oldPass123", "oldPass123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different from old password");
    }

    @Test
    void changePassword_unknownEmail_throwsUsernameNotFoundException() {
        when(appUserRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.changePassword("ghost@example.com", "old", "newPass456"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // createPasswordResetToken
    // -------------------------------------------------------------------------

    @Test
    void createPasswordResetToken_existingUser_returnsTokenAndSavesUser() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        String token = appUserService.createPasswordResetToken("user@example.com");

        assertThat(token).isNotNull().isNotBlank();
        assertThat(user.getResetToken()).isEqualTo(token);
        assertThat(user.getResetTokenExpiry()).isAfter(Instant.now());
        verify(appUserRepository).save(user);
    }

    @Test
    void createPasswordResetToken_tokenExpiryIsApprox30Minutes() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        appUserService.createPasswordResetToken("user@example.com");

        Instant expectedExpiry = Instant.now().plusSeconds(29 * 60); // at least 29 min from now
        assertThat(user.getResetTokenExpiry()).isAfter(expectedExpiry);
    }

    @Test
    void createPasswordResetToken_unknownEmail_returnsNull() {
        when(appUserRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        String token = appUserService.createPasswordResetToken("nobody@example.com");

        assertThat(token).isNull();
        verify(appUserRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // resetPasswordByToken
    // -------------------------------------------------------------------------

    @Test
    void resetPasswordByToken_validToken_updatesPasswordAndClearsToken() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        user.setPasswordHash("hashed_oldPass123");
        user.setResetToken("valid-token");
        user.setResetTokenExpiry(Instant.now().plusSeconds(600));
        when(appUserRepository.findByResetToken("valid-token")).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        appUserService.resetPasswordByToken("valid-token", "brandNewPass");

        assertThat(user.getPasswordHash()).isEqualTo("hashed_brandNewPass");
        assertThat(user.getResetToken()).isNull();
        assertThat(user.getResetTokenExpiry()).isNull();
        verify(appUserRepository).save(user);
    }

    @Test
    void resetPasswordByToken_invalidToken_throwsIllegalArgumentException() {
        when(appUserRepository.findByResetToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.resetPasswordByToken("bad-token", "newPass123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid token");
    }

    @Test
    void resetPasswordByToken_expiredToken_throwsIllegalArgumentException() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        user.setResetToken("expired-token");
        user.setResetTokenExpiry(Instant.now().minusSeconds(60)); // already expired
        when(appUserRepository.findByResetToken("expired-token")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.resetPasswordByToken("expired-token", "newPass123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token expired");
    }

    @Test
    void resetPasswordByToken_nullExpiry_throwsIllegalArgumentException() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        user.setResetToken("token-no-expiry");
        user.setResetTokenExpiry(null);
        when(appUserRepository.findByResetToken("token-no-expiry")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.resetPasswordByToken("token-no-expiry", "newPass123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token expired");
    }

    @Test
    void resetPasswordByToken_newPasswordTooShort_throwsIllegalArgumentException() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        user.setPasswordHash("hashed_oldPass");
        user.setResetToken("valid-token");
        user.setResetTokenExpiry(Instant.now().plusSeconds(600));
        when(appUserRepository.findByResetToken("valid-token")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.resetPasswordByToken("valid-token", "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    void resetPasswordByToken_newPasswordSameAsOld_throwsIllegalArgumentException() {
        AppUser user = buildUser("user@example.com", "ROLE_USER");
        user.setPasswordHash("hashed_oldPass123");
        user.setResetToken("valid-token");
        user.setResetTokenExpiry(Instant.now().plusSeconds(600));
        when(appUserRepository.findByResetToken("valid-token")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.resetPasswordByToken("valid-token", "oldPass123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different from the old password");
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private AppUser buildUser(String email, String role) {
        return AppUser.builder()
                .fullName("Test User")
                .email(email)
                .passwordHash("hashed_password")
                .role(role)
                .build();
    }
}
