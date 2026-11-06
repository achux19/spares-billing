package com.sparesbilling.backend.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {
    private AdminDtos() {}

    public record ShopRow(Long id, String code, String name, String status, LocalDateTime createdAt,
                          List<String> features, long userCount, long activeUserCount,
                          long invoiceCount, LocalDate lastInvoiceDate, LocalDateTime lastLoginAt) {}

    public record CreateShopRequest(
            @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,29}$",
                    message = "Shop code: 2 to 30 characters, small letters, digits and hyphen only") String code,
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Pattern(regexp = "^\\d{2}$", message = "State code must be 2 digits, for example 33") String stateCode,
            @NotBlank @Size(max = 50) String ownerUsername,
            @NotBlank @Size(max = 100) String ownerFullName,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String ownerPassword,
            @NotNull List<String> features) {}

    public record UpdateShopRequest(
            @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,29}$",
                    message = "Shop code: 2 to 30 characters, small letters, digits and hyphen only") String code,
            @NotBlank @Size(max = 150) String name) {}

    public record FeaturesRequest(@NotNull List<String> features) {}

    public record ShopUserRow(Long id, String username, String fullName, String role, boolean active,
                              LocalDateTime lastLoginAt) {}

    public record ResetPasswordRequest(
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String newPassword) {}

    public record FeatureInfo(String key, String label) {}
}
