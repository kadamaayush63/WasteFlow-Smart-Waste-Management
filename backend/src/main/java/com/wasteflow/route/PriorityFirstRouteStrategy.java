package com.wasteflow.route;

import com.wasteflow.collection.CollectionRequest;
import com.wasteflow.route.algorithm.DijkstraPathFinder;
import com.wasteflow.route.algorithm.WeightedGraph;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PriorityFirstRouteStrategy extends AbstractRouteRequestOrderingStrategy {

    public PriorityFirstRouteStrategy() {
        super(new DijkstraPathFinder());
    }

    @Override
    public String algorithm() {
        return "DIJKSTRA";
    }

    @Override
    public List<CollectionRequest> orderRequests(
            WeightedGraph graph,
            Long startLocationId,
            List<CollectionRequest> requests
    ) {
        return byPriority(requests);
    }
}