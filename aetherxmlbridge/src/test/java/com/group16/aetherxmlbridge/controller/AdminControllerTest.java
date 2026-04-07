package com.group16.aetherxmlbridge.controller;

import com.group16.aetherxmlbridge.model.AppUser;
import com.group16.aetherxmlbridge.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminControllerTest {

    private MockMvc mockMvc;
    private AppUserRepository appUserRepository;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);

        AdminController controller = new AdminController(appUserRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllUsers_returnsAdminUsersView() throws Exception {
        when(appUserRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-users"));
    }

    @Test
    void getAllUsers_addsUsersToModel() throws Exception {
        AppUser user1 = new AppUser();
        AppUser user2 = new AppUser();

        when(appUserRepository.findAll()).thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", hasSize(2)));
    }

    @Test
    void getAllUsers_withPrincipal_addsCurrentUserToModel() throws Exception {
        AppUser currentUser = new AppUser();
        currentUser.setEmail("admin@test.com");

        when(appUserRepository.findAll()).thenReturn(List.of());
        when(appUserRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(currentUser));

        mockMvc.perform(get("/admin/users").principal(new Principal() {
            @Override
            public String getName() {
                return "admin@test.com";
            }
        }))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attribute("currentUser", currentUser));
    }

    @Test
    void getAllUsers_withPrincipalUserNotFound_doesNotAddResolvedCurrentUserObject() throws Exception {
        when(appUserRepository.findAll()).thenReturn(List.of());
        when(appUserRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/users").principal(new Principal() {
            @Override
            public String getName() {
                return "missing@test.com";
            }
        }))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-users"))
                .andExpect(model().attribute("activePage", "admin-users"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    void getAllUsers_withoutPrincipal_doesNotAddCurrentUserToModel() throws Exception {
        when(appUserRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("currentUser"));
    }

    @Test
    void getAllUsers_setsActivePageToAdminUsers() throws Exception {
        when(appUserRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("activePage", "admin-users"));
    }
}