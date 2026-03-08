package com.group16.aetherxmlbridge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("!dev")
public class ProdSecurityConfig {

  // security chain of form + OAuth login.
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable()) // enable this for future production
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
            .requestMatchers("/h2-console/**").permitAll()
            .requestMatchers("/", "/login", "/register").permitAll()
            .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
            .anyRequest().authenticated()
        )

        
        .headers(headers -> headers
            .frameOptions(frame -> frame.disable())
        )
        // disabling iframe bc h2 uses iframe for development change to 
        // .frameOptions(frame -> frame.sameOrigin()) 


        // form config
        .formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .usernameParameter("email")
            .passwordParameter("password")
            .defaultSuccessUrl("/", true)
            .failureUrl("/login?error")
            .permitAll()
        )

        // oauth login + redirection
        .oauth2Login(oauth -> oauth
            .loginPage("/login")
            .defaultSuccessUrl("/", true)
        )

        // logout end point + redirection
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login?logout")
        );
    return http.build();
  }
}
