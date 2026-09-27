package com.wasteflow.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LocationRequest(
        @NotBlank String name,
        @NotNull Double xCoordinate,
        @NotNull Double yCoordinate
) {
}