package com.wasteflow.route;

import com.wasteflow.collection.CollectionRequest;
import com.wasteflow.route.algorithm.WeightedGraph;

import java.util.List;

public interface RouteRequestOrderingStrategy {
    String algorithm();

    List<CollectionRequest> orderRequests(
            WeightedGraph graph,
            Long startLocationId,
            List<CollectionRequest> requests
    );
}