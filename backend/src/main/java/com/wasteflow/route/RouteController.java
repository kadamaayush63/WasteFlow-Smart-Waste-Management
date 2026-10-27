package com.wasteflow.route;

import com.wasteflow.route.dto.OptimizeRouteRequest;
import com.wasteflow.route.dto.RouteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/optimize")
    @ResponseStatus(HttpStatus.CREATED)
    public RouteResponse optimize(@Valid @RequestBody OptimizeRouteRequest request) {
        return routeService.optimize(request);
    }

    @PostMapping("/undo-latest")
    public RouteResponse undoLatestPlannedRoute() {
        return routeService.undoLatestPlannedRoute();
    }

    @GetMapping("/active")
    public ResponseEntity<RouteResponse> active() {
        return routeService.findActive()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public RouteResponse findById(@PathVariable Long id) {
        return routeService.findById(id);
    }

    @PostMapping("/{id}/start")
    public RouteResponse start(@PathVariable Long id) {
        return routeService.start(id);
    }

    @PostMapping("/{routeId}/stops/{stopId}/complete")
    public RouteResponse completeStop(@PathVariable Long routeId, @PathVariable Long stopId) {
        return routeService.completeStop(routeId, stopId);
    }

    @PostMapping("/{id}/complete")
    public RouteResponse complete(@PathVariable Long id) {
        return routeService.complete(id);
    }
}