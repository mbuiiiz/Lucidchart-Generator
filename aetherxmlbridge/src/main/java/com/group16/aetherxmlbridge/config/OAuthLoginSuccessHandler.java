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
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AppUserRepository appUserRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public OAuthLoginSuccessHandler(AppUserRepository appUserRepository,
                                    @org.jspecify.annotations.Nullable OAuth2AuthorizedClientService authorizedClientService) {
        super("/dashboard");
        this.appUserRepository = appUserRepository;
        this.authorizedClientService = authorizedClientService;
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

            AppUser user = appUserRepository.findByEmail(email).orElseGet(() -> {
                AppUser newUser = AppUser.builder()
                        .email(finalEmail)
                        .fullName(finalFullName)
                        .passwordHash("") // oAuth users don't use password-based login
                        .role("ROLE_USER")
                        .build();
                return appUserRepository.save(newUser);
            });

            // save Zoho tokens so we can call Zoho Creator API later
            if (authorizedClientService != null
                    && authentication instanceof OAuth2AuthenticationToken oauthToken
                    && "zoho".equals(oauthToken.getAuthorizedClientRegistrationId())) {
                // retrieve Zoho token data saved in Spring memory after Zoho oauth
                OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("zoho", oauthToken.getName());
                
                if (client != null && client.getAccessToken() != null) {
                    // access token
                    user.setZohoAccessToken(client.getAccessToken().getTokenValue());
                    // expiry
                    if(client.getAccessToken().getExpiresAt() != null){
                        user.setZohoTokenExpiry(client.getAccessToken().getExpiresAt());
                    }else{
                        user.setZohoTokenExpiry(Instant.now().plusSeconds(3600));
                    }
                    // refresh token
                    if (client.getRefreshToken() != null) {
                        user.setZohoRefreshToken(client.getRefreshToken().getTokenValue());
                    }
                    // save user
                    appUserRepository.save(user);
                }
            }
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
