package com.wasteflow.route.dto;

import com.wasteflow.route.Route;
import com.wasteflow.route.RouteStop;
import com.wasteflow.route.RouteStopStatus;

import java.util.List;

public record RouteResponse(
        Long id,
        Long truckId,
        String algorithm,
        String status,
        double totalDistance,
        double totalTime,
        List<StopResponse> stops
) {
    public static RouteResponse from(Route route, List<RouteStop> stops) {
        return new RouteResponse(
                route.getId(), route.getTruck().getId(), route.getAlgorithm(), route.getStatus().name(),
                route.getTotalDistance(), route.getTotalTime(), stops.stream().map(StopResponse::from).toList()
        );
    }

    public record StopResponse(Long id, int sequenceNumber, Long binId, Long locationId, RouteStopStatus status, Double collectedKg) {
        public static StopResponse from(RouteStop stop) {
            return new StopResponse(stop.getId(), stop.getSequenceNumber(), stop.getBin().getId(), stop.getBin().getLocation().getId(), stop.getStatus(), stop.getCollectedKg());
        }
    }
}