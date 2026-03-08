package com.group16.aetherxmlbridge.service;

import com.group16.aetherxmlbridge.model.AppUser;
import com.group16.aetherxmlbridge.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
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

        AppUser newUser = AppUser.builder() 
                .fullName(fullName)
                .email(email)
                // 
                .passwordHash(passwordEncoder.encode(rawPassword))
                .build();

        return appUserRepository.save(newUser);
    }
}