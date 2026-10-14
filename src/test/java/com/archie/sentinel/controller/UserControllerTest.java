package com.archie.sentinel.controller;

import com.archie.sentinel.dto.CreateUserRequest;
import com.archie.sentinel.dto.UserResponse;
import com.archie.sentinel.exception.DuplicateResourceException;
import com.archie.sentinel.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@WithMockUser(username = "admin")
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean UserService userService;

    private static UserResponse response() {
        return new UserResponse(5L, "newuser", "ANALYST", LocalDateTime.now());
    }

    @Test
    void create_withValidBody_returns201_andResponseHasNoPasswordFields() throws Exception {
        when(userService.createUser("newuser", "correct-horse-battery", "ANALYST", "admin"))
                .thenReturn(response());

        mockMvc.perform(post("/api/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newuser","password":"correct-horse-battery","role":"ANALYST"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/5"))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void create_withMissingPassword_returns400_andNeverCallsService() throws Exception {
        mockMvc.perform(post("/api/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newuser","role":"ANALYST"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void create_whenServiceDenies_returns403() throws Exception {
        when(userService.createUser(any(), any(), any(), any()))
                .thenThrow(new AccessDeniedException("Administrator access required"));

        mockMvc.perform(post("/api/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newuser","password":"correct-horse-battery","role":"ADMIN"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withDuplicateUsername_returns409() throws Exception {
        when(userService.createUser(any(), any(), any(), any()))
                .thenThrow(new DuplicateResourceException("Username already exists"));

        mockMvc.perform(post("/api/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"correct-horse-battery","role":"ANALYST"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void list_returnsUsers_withoutPasswordFields() throws Exception {
        when(userService.listUsers("admin")).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("newuser"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void list_whenServiceDenies_returns403() throws Exception {
        when(userService.listUsers(any())).thenThrow(new AccessDeniedException("no"));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeRole_usesAuthenticatedUserAsActor() throws Exception {
        when(userService.changeRole(5L, "ADMIN", "admin")).thenReturn(response());

        mockMvc.perform(patch("/api/users/5/role").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ADMIN"}"""))
                .andExpect(status().isOk());

        verify(userService).changeRole(5L, "ADMIN", "admin");
    }

    @Test
    void changeRole_whenServiceDenies_returns403() throws Exception {
        when(userService.changeRole(anyLong(), any(), any()))
                .thenThrow(new AccessDeniedException("Administrators cannot change their own role"));

        mockMvc.perform(patch("/api/users/1/role").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ANALYST"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUserRequest_toString_doesNotRevealPassword() {
        var request = new CreateUserRequest("newuser", "correct-horse-battery", "ANALYST");

        assertThat(request.toString()).doesNotContain("correct-horse-battery");
    }

}
