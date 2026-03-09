package com.group16.aetherxmlbridge.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("!dev")
public class ProdSecurityConfig {

  // security chain of form + OAuth login
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository
  ) throws Exception {
    http
        .csrf(csrf -> csrf.disable()) // enable this for future production
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
            .requestMatchers("/h2-console/**").permitAll()
            .requestMatchers("/", "/login", "/register").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )

        // disabling iframe bc h2 uses iframe for development change to 
        // .frameOptions(frame -> frame.sameOrigin()) 
        .headers(headers -> headers
            .frameOptions(frame -> frame.disable())
        )

        // form config
        .formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .usernameParameter("email")
            .passwordParameter("password")
            .defaultSuccessUrl("/dashboard", true)
            .failureUrl("/login?error")
            .permitAll()
        )

        // logout endpoint + redirection
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login?logout")
        );


    // check if oauth client id and secret token is available
    if (clientRegistrationRepository.getIfAvailable() != null) {
      http
          .authorizeHttpRequests(auth -> auth
              .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
          )
          .oauth2Login(oauth -> oauth
              .loginPage("/login")
              .defaultSuccessUrl("/dashboard", true)
          );
    }

    return http.build();
  }
}
