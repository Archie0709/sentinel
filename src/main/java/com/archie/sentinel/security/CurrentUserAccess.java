package com.archie.sentinel.security;

import com.archie.sentinel.model.User;
import com.archie.sentinel.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserAccess {

    private final UserRepository userRepository;

    public CurrentUserAccess(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** If unknown user deny access completely */
    public User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));
    }

    public User requireAdmin(String username) {
        User actor = requireUser(username);
        if (!isAdmin(actor)) {
            throw new AccessDeniedException("Administrator access required");
        }
        return actor;
    }

    public boolean isAdmin(User user) {
        return Roles.ADMIN.equals(user.getRole().getName());
    }
}
