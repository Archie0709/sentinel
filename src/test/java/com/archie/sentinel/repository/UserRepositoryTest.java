package com.archie.sentinel.repository;

import com.archie.sentinel.TestcontainersConfiguration;
import com.archie.sentinel.model.Role;
import com.archie.sentinel.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
public class UserRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;

    private Role analystRole;

    @BeforeEach
    public void setup() {
        analystRole = roleRepository.findByName("ANALYST").orElse(null);
    }

    @Test
    void findByUsername_returnsUser_ifExists() {
        userRepository.save(new User("bob", "hash", analystRole));

        assertThat(userRepository.findByUsername("bob")).isPresent();
    }

    @Test
    void findByUsername_returnsNull_ifNotExists() {
        assertThat(userRepository.findByUsername("nobody")).isEmpty();
    }

    @Test
    void existsByUsername_returnsAccordingly() {
        userRepository.save(new User("bob", "hash", analystRole));

        assertThat(userRepository.existsByUsername("bob")).isTrue();
        assertThat(userRepository.existsByUsername("nobody")).isFalse();
    }

    @Test
    void duplicateUsername_isRejectedOnmCreation() {
        userRepository.save(new User("bob", "hash", analystRole));

        assertThatThrownBy(() ->
                userRepository.save(new User("bob", "hash", analystRole)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

}
