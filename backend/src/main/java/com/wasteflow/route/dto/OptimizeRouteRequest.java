package com.wasteflow.route.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OptimizeRouteRequest(
        @NotNull Long truckId,
        @NotEmpty List<Long> requestIds,
        @NotNull String algorithm
) {
}