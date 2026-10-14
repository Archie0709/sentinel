package com.archie.sentinel.security;

import com.archie.sentinel.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static com.archie.sentinel.support.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CurrentUserAccessTest {

    @Mock UserRepository userRepository;
    private CurrentUserAccess access;

    @BeforeEach
    void setUp() {
        access = new CurrentUserAccess(userRepository);
        lenient().when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(user(1, "admin", "ADMIN")));
        lenient().when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(user(2, "alice", "ANALYST")));
    }

    @Test
    void requireAdmin_returnsAdminUser() {
        assertThat(access.requireAdmin("admin").getUsername()).isEqualTo("admin");
    }

    @Test
    void requireAdmin_deniesAnalyst() {
        assertThatThrownBy(() -> access.requireAdmin("alice"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireUser_deniesUnknownUser() {
        assertThatThrownBy(() -> access.requireUser("ghost"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void isAdmin_distinguishesRoles() {
        assertThat(access.isAdmin(user(1, "a", "ADMIN"))).isTrue();
        assertThat(access.isAdmin(user(2, "b", "ANALYST"))).isFalse();
    }
}
