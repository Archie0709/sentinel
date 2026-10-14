package com.archie.sentinel.controller;

import com.archie.sentinel.dto.*;
import com.archie.sentinel.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request,
                                               Authentication auth) {
        UserResponse created = userService.createUser(
                request.username(), request.password(), request.role(), auth.getName());
        return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
    }

    @GetMapping
    public List<UserResponse> list(Authentication auth) {
        return userService.listUsers(auth.getName());
    }

    @PatchMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable Long id,
                                   @Valid @RequestBody ChangeRoleRequest request,
                                   Authentication auth) {
        return userService.changeRole(id, request.role(), auth.getName());
    }
}
