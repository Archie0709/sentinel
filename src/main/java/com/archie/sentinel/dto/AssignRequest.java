package com.archie.sentinel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignRequest(@NotBlank @Size(max = 50) String assignee) {}
