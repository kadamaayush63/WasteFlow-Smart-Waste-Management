package com.wasteflow.bin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBinRequest(
        @NotBlank String binCode,
        @NotNull Long locationId,
        @NotNull @DecimalMin("0.01") Double capacityKg,
        @NotNull @DecimalMin("0.0") Double currentFillKg,
        @NotNull WasteType wasteType
) {
}