package com.teamflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,64}") String username,
    @NotBlank @Size(min = 8, max = 128) String password,
    @NotBlank @Size(max = 100) String displayName,
    @Email @Size(max = 254) String email) {}
