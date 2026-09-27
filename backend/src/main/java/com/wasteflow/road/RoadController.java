package com.wasteflow.road;

import com.wasteflow.location.Location;
import com.wasteflow.location.LocationRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roads")
public class RoadController {

    private final RoadRepository roadRepository;
    private final LocationRepository locationRepository;

    public RoadController(RoadRepository roadRepository, LocationRepository locationRepository) {
        this.roadRepository = roadRepository;
        this.locationRepository = locationRepository;
    }

    @GetMapping
    public List<RoadResponse> findAll() {
        return roadRepository.findAll().stream().map(RoadResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoadResponse create(@Valid @RequestBody RoadRequest request) {
        Location source = locationRepository.findById(request.sourceLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Source location not found: " + request.sourceLocationId()));
        Location destination = locationRepository.findById(request.destinationLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Destination location not found: " + request.destinationLocationId()));
        return RoadResponse.from(roadRepository.save(new Road(
                source, destination, request.distance(), request.travelTimeMinutes(), request.bidirectional()
        )));
    }
}