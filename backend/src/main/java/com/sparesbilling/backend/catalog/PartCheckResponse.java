package com.sparesbilling.backend.catalog;

/** duplicate = same part number and same brand exists (blocked); otherBrand = same number under another brand (warning). */
public record PartCheckResponse(boolean duplicate, boolean otherBrand, String otherBrandName) {}
