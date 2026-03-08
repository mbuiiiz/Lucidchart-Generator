package com.group16.aetherxmlbridge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// adding BCrypt instead of sha256

/**
 * notes: BCrypt hash adds random salts for security in case db gets leaked
 * 
 */

@Configuration
public class AuthBeansConfig {

  // password hashing bean used by authentication and registration.
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
