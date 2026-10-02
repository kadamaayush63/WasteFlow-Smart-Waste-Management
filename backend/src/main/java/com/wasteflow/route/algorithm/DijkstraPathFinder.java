package com.wasteflow.route.algorithm;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class DijkstraPathFinder {

    public Path findPath(WeightedGraph graph, Long source, Long target) {
        Map<Long, Double> distances = new HashMap<>();
        Map<Long, Double> times = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingDouble(Node::distance));

        distances.put(source, 0.0);
        times.put(source, 0.0);
        queue.add(new Node(source, 0.0));

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (!visited.add(current.locationId())) continue;
            if (current.locationId().equals(target)) break;

            for (WeightedGraph.Edge edge : graph.neighbors(current.locationId())) {
                double candidate = current.distance() + edge.distance();
                if (candidate < distances.getOrDefault(edge.destination(), Double.POSITIVE_INFINITY)) {
                    distances.put(edge.destination(), candidate);
                    times.put(edge.destination(), times.get(current.locationId()) + edge.time());
                    previous.put(edge.destination(), current.locationId());
                    queue.add(new Node(edge.destination(), candidate));
                }
            }
        }

        if (!distances.containsKey(target)) {
            throw new IllegalArgumentException("No route exists from location " + source + " to " + target);
        }

        List<Long> path = new ArrayList<>();
        for (Long current = target; current != null; current = previous.get(current)) {
            path.add(0, current);
        }
        return new Path(path, distances.get(target), times.get(target));
    }

    private record Node(Long locationId, double distance) {
    }

    public record Path(List<Long> locationIds, double distance, double time) {
    }
}