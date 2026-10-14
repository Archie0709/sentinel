package com.archie.sentinel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeStatusRequest(@NotBlank @Size(max = 20) String status) {}
