/**
 * this feature helps oauth creating an entry in app_user database
 * 
 * notes: i haven't implement account linking yet -> login with google and zoho will cause a conflict (no small email constraint)
 */

package com.group16.aetherxmlbridge.config;

import com.group16.aetherxmlbridge.model.AppUser;
import com.group16.aetherxmlbridge.repository.AppUserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AppUserRepository appUserRepository;

    public OAuthLoginSuccessHandler(AppUserRepository appUserRepository) {
        super("/dashboard");
        this.appUserRepository = appUserRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // store current oauth login object
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        // zoho uses "Email", google uses "email"
        String email = oauthUser.getAttribute("Email");
        if (email == null) {
            email = oauthUser.getAttribute("email");
        }

        // build full name from Zoho First_Name/Last_Name, or Google "name"
        String firstName = oauthUser.getAttribute("First_Name");
        String lastName = oauthUser.getAttribute("Last_Name");
        String fullName;

        // construct user name
        if (firstName != null || lastName != null) {
            fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
        } else {
            String name = oauthUser.getAttribute("name");
            fullName = (name != null) ? name : email;
        }

        // only save to DB if email was successfully extracted from the OAuth token
        if (email != null) {
            final String finalEmail = email;
            final String finalFullName = fullName;
            appUserRepository.findByEmail(email).orElseGet(() -> {
                AppUser newUser = AppUser.builder()
                        .email(finalEmail)
                        .fullName(finalFullName)
                        .passwordHash("") // oAuth users don't use password-based login
                        .role("ROLE_USER")
                        .build();
                return appUserRepository.save(newUser);
            });
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
