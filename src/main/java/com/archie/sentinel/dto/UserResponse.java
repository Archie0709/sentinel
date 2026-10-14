package com.archie.sentinel.dto;

import com.archie.sentinel.model.User;

import java.time.LocalDateTime;

public record UserResponse(Long id, String username, String role, LocalDateTime createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(),
                u.getRole().getName(), u.getCreatedAt());
    }

}
