package com.wasteflow.route;

import com.wasteflow.collection.CollectionRequest;
import com.wasteflow.route.algorithm.DijkstraPathFinder;
import com.wasteflow.route.algorithm.WeightedGraph;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NearestStopRouteStrategy extends AbstractRouteRequestOrderingStrategy {

    public NearestStopRouteStrategy() {
        super(new DijkstraPathFinder());
    }

    @Override
    public String algorithm() {
        return "NEAREST_STOP";
    }

    @Override
    public List<CollectionRequest> orderRequests(
            WeightedGraph graph,
            Long startLocationId,
            List<CollectionRequest> requests
    ) {
        List<CollectionRequest> remaining = new ArrayList<>(byPriority(requests));
        List<CollectionRequest> ordered = new ArrayList<>(remaining.size());
        Long currentLocationId = startLocationId;

        while (!remaining.isEmpty()) {
            Long sourceLocationId = currentLocationId;
            CollectionRequest nearest = remaining.stream()
                    .min((first, second) -> Double.compare(
                            distance(graph, sourceLocationId, first),
                            distance(graph, sourceLocationId, second)))
                    .orElseThrow();
            ordered.add(nearest);
            remaining.remove(nearest);
            currentLocationId = nearest.getBin().getLocation().getId();
        }
        return ordered;
    }

    private double distance(WeightedGraph graph, Long sourceLocationId, CollectionRequest request) {
        return pathFinder.findPath(graph, sourceLocationId, request.getBin().getLocation().getId()).distance();
    }
}