package com.group16.aetherxmlbridge.service;

import com.group16.aetherxmlbridge.model.AppUser;
import com.group16.aetherxmlbridge.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AppUserService implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    @Transactional
    public AppUser registerUser(String fullName, String email, String rawPassword) {
        if (appUserRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }
    
        // DEV ONLY 
        
        String assignedRole = email.equalsIgnoreCase("a@a.com")
                ? "ROLE_ADMIN"
                : "ROLE_USER";
        
    
        // PROD VERSION
        //String assignedRole = "ROLE_USER";
    
        AppUser newUser = AppUser.builder()
                .fullName(fullName)
                .email(email)
                .role(assignedRole)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .build();
    
        return appUserRepository.save(newUser);
    }

    @Transactional
    public void updateFullName(String email, String newFullName) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        user.setFullName(newFullName);
        appUserRepository.save(user);
    }

    @Transactional
    public void updatePhoneNumber(String email, String phoneNumber) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        user.setPhoneNumber(phoneNumber);
        appUserRepository.save(user);
    }

    @Transactional
    public void deleteAccount(String email, String rawPassword) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect password");
        }

        appUserRepository.delete(user);
    }
    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect old password");
        }

        if (newPassword == null || newPassword.trim().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from old password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
        appUserRepository.save(user);
    }
    
    @Transactional
    public String createPasswordResetToken(String email) {
        AppUser user = appUserRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return null;
        }

        String token = UUID.randomUUID().toString();

        user.setResetToken(token);
        user.setResetTokenExpiry(Instant.now().plusSeconds(30 * 60)); // 30 minutes

        appUserRepository.save(user);

        return token;
    }

    @Transactional
    public void resetPasswordByToken(String token, String newPassword) {
        AppUser user = appUserRepository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
    
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expired");
        }
    
        if (newPassword == null || newPassword.trim().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
    
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from the old password");
        }
    
        user.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
    
        appUserRepository.save(user);
    }
}