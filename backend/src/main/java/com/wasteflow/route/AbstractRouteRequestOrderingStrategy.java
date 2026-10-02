package com.wasteflow.route;

import com.wasteflow.collection.CollectionRequest;
import com.wasteflow.route.algorithm.DijkstraPathFinder;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public abstract class AbstractRouteRequestOrderingStrategy implements RouteRequestOrderingStrategy {

    protected final DijkstraPathFinder pathFinder;

    protected AbstractRouteRequestOrderingStrategy(DijkstraPathFinder pathFinder) {
        this.pathFinder = Objects.requireNonNull(pathFinder);
    }

    protected List<CollectionRequest> byPriority(List<CollectionRequest> requests) {
        return requests.stream()
                .sorted(Comparator.comparingDouble(CollectionRequest::getPriorityScore).reversed()
                        .thenComparing(CollectionRequest::getCreatedAt))
                .toList();
    }
}