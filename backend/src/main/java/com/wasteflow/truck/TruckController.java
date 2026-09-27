package com.wasteflow.truck;

import com.wasteflow.location.LocationRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trucks")
public class TruckController {

    private final TruckRepository truckRepository;
    private final LocationRepository locationRepository;

    public TruckController(TruckRepository truckRepository, LocationRepository locationRepository) {
        this.truckRepository = truckRepository;
        this.locationRepository = locationRepository;
    }

    @GetMapping
    public List<TruckResponse> findAll() {
        return truckRepository.findAll().stream().map(TruckResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TruckResponse create(@Valid @RequestBody CreateTruckRequest request) {
        var location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + request.locationId()));
        return TruckResponse.from(truckRepository.save(new Truck(request.truckCode(), request.capacityKg(), location)));
    }

    public record CreateTruckRequest(
            @NotBlank String truckCode,
            @NotNull @DecimalMin("0.01") Double capacityKg,
            @NotNull Long locationId
    ) {
    }
}