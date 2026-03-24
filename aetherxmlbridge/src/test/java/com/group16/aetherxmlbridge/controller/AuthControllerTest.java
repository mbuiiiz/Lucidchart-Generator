package com.group16.aetherxmlbridge.controller;

import com.group16.aetherxmlbridge.model.AppUser;
import com.group16.aetherxmlbridge.service.AppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private MockMvc mockMvc;
    private AppUserService appUserService;

    @BeforeEach
    void setUp() {
        appUserService = mock(AppUserService.class);
        AuthController controller = new AuthController(appUserService);

        org.springframework.web.servlet.view.InternalResourceViewResolver viewResolver =
            new org.springframework.web.servlet.view.InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setViewResolvers(viewResolver)
            .build();
    }

    @Test
    void register_validInput_redirectsToDashboardAndAuthenticatesUser() throws Exception {
        AppUser user = AppUser.builder()
                .email("test@example.com")
                .fullName("Test User")
                .passwordHash("hashed")
                .role("ROLE_USER")
                .build();

        when(appUserService.registerUser("Test User", "test@example.com", "password123")).thenReturn(user);
        when(appUserService.loadUserByUsername("test@example.com")).thenReturn(user);

        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("fullName", "Test User")
                .param("email", "test@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(request().sessionAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    void register_blankFullName_returnsRegisterWithError() throws Exception {
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("fullName", "   ")
                .param("email", "test@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Full name is required"));
    }

    @Test
    void register_blankEmail_returnsRegisterWithError() throws Exception {
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("fullName", "Test User")
                .param("email", "   ")
                .param("password", "password123")
                .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Email is required"));
    }

    @Test
    void register_blankPassword_returnsRegisterWithError() throws Exception {
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("fullName", "Test User")
                .param("email", "test@example.com")
                .param("password", "   ")
                .param("confirmPassword", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Password is required"));
    }

    @Test
    void register_shortPassword_returnsRegisterWithError() throws Exception {
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("fullName", "Test User")
                .param("email", "test@example.com")
                .param("password", "short")
                .param("confirmPassword", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Password must be at least 8 characters"));
    }

    @Test
    void register_passwordMismatch_returnsRegisterWithError() throws Exception {
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("fullName", "Test User")
                .param("email", "test@example.com")
                .param("password", "password123")
                .param("confirmPassword", "different123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Passwords do not match"));
    }

    @Test
    void register_duplicateEmail_returnsRegisterWithServiceError() throws Exception {
        when(appUserService.registerUser("Test User", "test@example.com", "password123"))
                .thenThrow(new IllegalArgumentException("Email already registered: test@example.com"));

        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("fullName", "Test User")
                .param("email", "test@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Email already registered: test@example.com"));
    }
}