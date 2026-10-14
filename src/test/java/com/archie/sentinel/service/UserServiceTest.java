package com.archie.sentinel.service;

import com.archie.sentinel.exception.DuplicateResourceException;
import com.archie.sentinel.exception.InvalidInputException;
import com.archie.sentinel.model.User;
import com.archie.sentinel.repository.RoleRepository;
import com.archie.sentinel.repository.UserRepository;
import com.archie.sentinel.security.CurrentUserAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static com.archie.sentinel.support.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String GOOD_PASSWORD = "correct-horse-battery";

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;

    private UserService service;

    private User admin;
    private User alice;   // (analyst)

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, roleRepository, passwordEncoder,
                new CurrentUserAccess(userRepository));

        admin = user(1, "admin", "ADMIN");
        alice = user(2, "alice", "ANALYST");
        lenient().when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        lenient().when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
    }

    @Test
    void createUser_byAnalyst_isDenied_andSavesNothing() {
        assertThatThrownBy(() -> service.createUser("newuser", GOOD_PASSWORD, "ANALYST", "alice"))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void createUser_byUnknownActor_isDenied() {
        assertThatThrownBy(() -> service.createUser("newuser", GOOD_PASSWORD, "ANALYST", "ghost"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createUser_storesHashNotRawPassword() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(roleRepository.findByName("ANALYST")).thenReturn(Optional.of(role("ANALYST")));
        when(passwordEncoder.encode(GOOD_PASSWORD)).thenReturn("$2a$hashed");
        when(userRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.createUser("  newuser ", GOOD_PASSWORD, "ANALYST", "admin");

        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("newuser");
        assertThat(captor.getValue().getPasswordHash())
                .isEqualTo("$2a$hashed")
                .isNotEqualTo(GOOD_PASSWORD);
        assertThat(result.role()).isEqualTo("ANALYST");
    }

    @Test
    void createUser_rejectsShortPassword() {
        assertThatThrownBy(() -> service.createUser("newuser", "short", "ANALYST", "admin"))
                .isInstanceOf(InvalidInputException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void createUser_rejectsPasswordOver72Bytes() {
        String tooLong = "a".repeat(73);

        assertThatThrownBy(() -> service.createUser("newuser", tooLong, "ANALYST", "admin"))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void createUser_rejectsInvalidUsername() {
        assertThatThrownBy(() -> service.createUser("bad name!", GOOD_PASSWORD, "ANALYST", "admin"))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void createUser_rejectsDuplicateUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser("alice", GOOD_PASSWORD, "ANALYST", "admin"))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void listUsers_byAnalyst_isDenied() {
        assertThatThrownBy(() -> service.listUsers("alice"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void changeRole_byAnalyst_isDenied() {
        assertThatThrownBy(() -> service.changeRole(2L, "ADMIN", "alice"))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(alice.getRole().getName()).isEqualTo("ANALYST");
    }

    @Test
    void changeRole_adminCannotChangeOwnRole() {
        assertThatThrownBy(() -> service.changeRole(1L, "ANALYST", "admin"))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(admin.getRole().getName()).isEqualTo("ADMIN");
    }

    @Test
    void changeRole_adminCanPromoteAnalyst() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(alice));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role("ADMIN")));
        when(userRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.changeRole(2L, "ADMIN", "admin");

        assertThat(result.role()).isEqualTo("ADMIN");
    }
}