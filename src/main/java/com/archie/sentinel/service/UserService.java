package com.archie.sentinel.service;

import com.archie.sentinel.dto.UserResponse;
import com.archie.sentinel.exception.DuplicateResourceException;
import com.archie.sentinel.exception.InvalidInputException;
import com.archie.sentinel.exception.ResourceNotFoundException;
import com.archie.sentinel.model.Role;
import com.archie.sentinel.model.User;
import com.archie.sentinel.repository.RoleRepository;
import com.archie.sentinel.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final String ADMIN = "ADMIN";
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_BYTES = 72;   // BCrypt ignores anything beyond this

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(String username, String rawPassword,
                                   String roleName, String actorUsername) {
        requireAdmin(actorUsername);

        String cleanUsername = validateUsername(username);
        validatePassword(rawPassword);

        if (userRepository.existsByUsername(cleanUsername)) {
            throw new DuplicateResourceException("Username already exists");
        }
        Role role = loadRole(roleName);

        User saved = userRepository.saveAndFlush(
                new User(cleanUsername, passwordEncoder.encode(rawPassword), role));
        return UserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(String actorUsername) {
        requireAdmin(actorUsername);
        return userRepository.findAllByOrderByUsernameAsc().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse changeRole(Long userId, String roleName, String actorUsername) {
        User actor = requireAdmin(actorUsername);

        if (Objects.equals(actor.getId(), userId)) {
            throw new AccessDeniedException("Administrators cannot change their own role");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        target.setRole(loadRole(roleName));
        return UserResponse.from(userRepository.saveAndFlush(target));
    }

    // Helpers

    private User requireAdmin(String username) {
        User actor = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));
        if (!ADMIN.equals(actor.getRole().getName())) {
            throw new AccessDeniedException("Administrator access required");
        }
        return actor;
    }

    private Role loadRole(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown role " + name));
    }

    private String validateUsername(String username) {
        String clean = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(clean).matches()) {
            throw new InvalidInputException(
                    "Username must be 3-50 characters: letters, digits, '.', '_' or '-'");
        }
        return clean;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new InvalidInputException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new InvalidInputException(
                    "Password must be at most " + MAX_PASSWORD_BYTES + " bytes");
        }
    }
}