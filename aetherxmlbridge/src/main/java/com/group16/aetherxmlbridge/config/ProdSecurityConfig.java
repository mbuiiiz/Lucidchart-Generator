package com.group16.aetherxmlbridge.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("!dev")
public class ProdSecurityConfig {

    // security chain of form + OAuth login
@Bean
public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
        OAuthLoginSuccessHandler oAuthLoginSuccessHandler
) throws Exception {
    boolean oauthEnabled = clientRegistrationRepository.getIfAvailable() != null;

    http
            .csrf(csrf -> csrf.disable()) // enable this for future production
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        // .requestMatchers("/h2-console/**").permitAll() // dev only — do not expose in production
                        .requestMatchers("/", "/login", "/register", "/error", "/error/**", "/forgot-password", "/reset-password").permitAll()
                        .requestMatchers("/api/ai/chat").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN");

                if (oauthEnabled) {
                    auth.requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll();
                }

                auth.anyRequest().authenticated();
            })

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

    if (oauthEnabled) {
        ClientRegistrationRepository repo = clientRegistrationRepository.getIfAvailable();

        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(
                customizer -> customizer.additionalParameters(params -> params.put("access_type", "offline"))
        );

        http.oauth2Login(oauth -> oauth
                .loginPage("/login")
                .authorizationEndpoint(ep -> ep.authorizationRequestResolver(resolver))
                .successHandler(oAuthLoginSuccessHandler)
        );
    }

    return http.build();
    }
}
