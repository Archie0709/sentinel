package com.archie.sentinel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeSeverityRequest(@NotBlank @Size(max = 20) String severity) {}
