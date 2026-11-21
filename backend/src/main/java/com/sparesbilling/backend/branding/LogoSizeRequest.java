package com.sparesbilling.backend.branding;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record LogoSizeRequest(@Min(8) @Max(50) int heightMm, @Min(15) @Max(120) int widthMm) {}
