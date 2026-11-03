package com.sparesbilling.backend.auth;

import jakarta.validation.constraints.NotBlank;

/** shopCode is empty only for the platform Super Admin login. */
public record LoginRequest(String shopCode, @NotBlank String username, @NotBlank String password) {}
