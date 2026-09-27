package com.wasteflow.route;

import com.wasteflow.route.algorithm.DijkstraPathFinder;
import com.wasteflow.route.algorithm.WeightedGraph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DijkstraPathFinderTest {

    @Test
    void findsTheShortestWeightedPath() {
        WeightedGraph graph = new WeightedGraph();
        graph.addEdge(1L, 2L, 4, 4);
        graph.addEdge(1L, 3L, 1, 1);
        graph.addEdge(3L, 2L, 1, 1);

        DijkstraPathFinder.Path path = new DijkstraPathFinder().findPath(graph, 1L, 2L);

        assertEquals(java.util.List.of(1L, 3L, 2L), path.locationIds());
        assertEquals(2, path.distance());
    }
}