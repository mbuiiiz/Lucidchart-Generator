/* 
    this config file is created for developing TWO SEPARATED epics 
    
    FOR FRONTEND UI 
    to work on every page run THIS to gain access to every page
    docker run -p 8080:8080 aether-bridge-local

    FOR OAUTH SECURITY
    to work on security run THIS so you will be locked out of the program
    docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev aether-bridge-local
*/
package com.group16.aetherxmlbridge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile("dev")
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) 
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()); 
        return http.build();
    }
}

