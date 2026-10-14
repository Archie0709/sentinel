package com.archie.sentinel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 200) String password,
        @NotBlank @Size(max = 20) String role) {

    // Records print all fields by default so overwrites password so they cannot be logged.
    @Override
    public String toString() {
        return "CreateUserRequest[username=" + username + ", role=" + role + ", password=****]";
    }
}
