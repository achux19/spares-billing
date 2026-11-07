package com.sparesbilling.backend.enquiry;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record EnquiryRequest(
        LocalDate enquiryDate,
        Long supplierId,
        @Size(max = 150) String supplierName,
        @Size(max = 1000) String notes,
        @NotEmpty @Size(max = 300) List<@Valid Item> items
) {
    public record Item(
            Long productId,
            @Size(max = 60) String partNumber,
            @NotBlank @Size(max = 200) String description,
            @Min(1) @Max(1000000) int quantity,
            @Pattern(regexp = "NOS|LTR") String unit
    ) {}
}
