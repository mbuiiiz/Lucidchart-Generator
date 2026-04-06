package com.group16.aetherxmlbridge.controller;

import com.group16.aetherxmlbridge.model.AppUser;
import com.group16.aetherxmlbridge.model.ZohoProject;
import com.group16.aetherxmlbridge.repository.AppUserRepository;
import com.group16.aetherxmlbridge.service.PhoneMaskingService;
import com.group16.aetherxmlbridge.service.ZohoApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.mockito.ArgumentMatchers.any;

class PageControllerTest {

    private MockMvc mockMvc;
    private AppUserRepository appUserRepository;
    private ZohoApiService zohoApiService;
    private PhoneMaskingService phoneMaskingService;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        zohoApiService = mock(ZohoApiService.class);
        phoneMaskingService = mock(PhoneMaskingService.class);

        PageController controller = new PageController(
                appUserRepository,
                zohoApiService,
                phoneMaskingService
            );

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void dashboard_userWithZohoToken_loadsProjectsAndFlagsConnected() throws Exception {
        AppUser user = AppUser.builder()
                .email("test@example.com")
                .fullName("Test User")
                .passwordHash("hashed")
                .role("ROLE_USER")
                .zohoAccessToken("token123")
                .build();

        ZohoProject project = new ZohoProject(
                "1", "Deal A", "Open", "example.com",
                "Context", "Purpose", "Notes", "Concerns"
        );

        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(zohoApiService.fetchProjects(user)).thenReturn(List.of(project));

        mockMvc.perform(get("/dashboard").principal(new UsernamePasswordAuthenticationToken("test@example.com", "N/A")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("currentUser", user))
                .andExpect(model().attribute("zohoConnected", true))
                .andExpect(model().attribute("projects", List.of(project)));
    }

    @Test
    void dashboard_userWithoutZohoToken_setsZohoConnectedFalse() throws Exception {
        AppUser user = AppUser.builder()
                .email("test@example.com")
                .fullName("Test User")
                .passwordHash("hashed")
                .role("ROLE_USER")
                .zohoAccessToken(null)
                .build();

        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(zohoApiService.fetchProjects(user)).thenReturn(List.of());

        mockMvc.perform(get("/dashboard").principal(new UsernamePasswordAuthenticationToken("test@example.com", "N/A")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("currentUser", user))
                .andExpect(model().attribute("zohoConnected", false))
                .andExpect(model().attribute("projects", List.of()));
    }

    @Test
    void profile_loadsCurrentUser() throws Exception {
        AppUser user = AppUser.builder()
            .email("test@example.com")
            .fullName("Test User")
            .passwordHash("hashed")
            .role("ROLE_USER")
            .phoneNumber("+14343143242")
            .build();

    when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(phoneMaskingService.mask(any())).thenReturn("*** *** 1234");
    when(phoneMaskingService.format(any())).thenReturn("+1 (434) 314-3242");

    mockMvc.perform(get("/profile").principal(new UsernamePasswordAuthenticationToken("test@example.com", "N/A")))
            .andExpect(status().isOk())
            .andExpect(view().name("profile"))
            .andExpect(model().attribute("currentUser", user))
            .andExpect(model().attribute("maskedPhoneNumber", "*** *** 1234"))
            .andExpect(model().attribute("formattedPhoneNumber", "+1 (434) 314-3242"));
}

    @Test
    void profile_passwordSuccessParam_setsSuccessMessage() throws Exception {
        AppUser user = AppUser.builder()
            .email("test@example.com")
            .fullName("Test User")
            .passwordHash("hashed")
            .role("ROLE_USER")
            .phoneNumber("+14343143242")
            .build();

    when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(phoneMaskingService.mask(any())).thenReturn("*** *** 1234");
    when(phoneMaskingService.format(any())).thenReturn("+1 (434) 314-3242");

    mockMvc.perform(get("/profile")
            .param("passwordSuccess", "true")
            .principal(new UsernamePasswordAuthenticationToken("test@example.com", "N/A")))
            .andExpect(status().isOk())
            .andExpect(view().name("profile"))
            .andExpect(model().attribute("currentUser", user))
            .andExpect(model().attribute("maskedPhoneNumber", "*** *** 1234"))
            .andExpect(model().attribute("formattedPhoneNumber", "+1 (434) 314-3242"))
            .andExpect(model().attribute("passwordSuccess", "Password updated successfully"));
}

    @Test
    void projects_userWithZohoToken_loadsProjectsAndFlagsConnected() throws Exception {
        AppUser user = AppUser.builder()
                .email("test@example.com")
                .fullName("Test User")
                .passwordHash("hashed")
                .role("ROLE_USER")
                .zohoAccessToken("token123")
                .build();

        ZohoProject project = new ZohoProject(
                "1", "Deal A", "Open", "example.com",
                "Context", "Purpose", "Notes", "Concerns"
        );

        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(zohoApiService.fetchProjects(user)).thenReturn(List.of(project));

        mockMvc.perform(get("/projects").principal(new UsernamePasswordAuthenticationToken("test@example.com", "N/A")))
                .andExpect(status().isOk())
                .andExpect(view().name("projects"))
                .andExpect(model().attribute("currentUser", user))
                .andExpect(model().attribute("zohoConnected", true))
                .andExpect(model().attribute("projects", List.of(project)));
    }
}
