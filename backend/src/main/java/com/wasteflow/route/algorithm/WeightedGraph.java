package com.wasteflow.route.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeightedGraph {

    private final Map<Long, List<Edge>> adjacency = new HashMap<>();

    public void addEdge(Long from, Long to, double distance, double time) {
        adjacency.computeIfAbsent(from, ignored -> new ArrayList<>()).add(new Edge(to, distance, time));
    }

    public List<Edge> neighbors(Long locationId) {
        return adjacency.getOrDefault(locationId, List.of());
    }

    public record Edge(Long destination, double distance, double time) {
    }
}