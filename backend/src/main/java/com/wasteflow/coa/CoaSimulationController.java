package com.wasteflow.coa;

import com.wasteflow.coa.CoaSimulationRequest.BinStatus;
import com.wasteflow.coa.CoaSimulationRequest.RouteSummary;
import com.wasteflow.coa.CoaSimulationRequest.RouteTotals;
import com.wasteflow.coa.CoaSimulationRequest.RouteWorkload;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coa")
public class CoaSimulationController {

    private final CoaSimulationService coaSimulationService;

    public CoaSimulationController(CoaSimulationService coaSimulationService) {
        this.coaSimulationService = coaSimulationService;
    }

    @PostMapping("/bin-status")
    public CoaSimulationResponse simulateBinStatus(@Valid @RequestBody BinStatus request) {
        return coaSimulationService.simulateBinStatus(request);
    }

    @PostMapping("/route-totals")
    public CoaSimulationResponse simulateRouteTotals(@Valid @RequestBody RouteTotals request) {
        return coaSimulationService.simulateRouteTotals(request);
    }

    @PostMapping("/route-workload")
    public CoaSimulationResponse simulateRouteWorkload(@Valid @RequestBody RouteWorkload request) {
        return coaSimulationService.simulateRouteWorkload(request);
    }

    @PostMapping("/route-summary")
    public CoaSimulationResponse simulateRouteSummary(@Valid @RequestBody RouteSummary request) {
        return coaSimulationService.simulateRouteSummary(request);
    }
}
