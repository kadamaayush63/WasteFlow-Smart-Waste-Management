package com.wasteflow.route;

import com.wasteflow.collection.CollectionRequest;
import com.wasteflow.collection.CollectionRequestRepository;
import com.wasteflow.collection.RequestStatus;
import com.wasteflow.road.Road;
import com.wasteflow.road.RoadRepository;
import com.wasteflow.route.algorithm.DijkstraPathFinder;
import com.wasteflow.route.algorithm.WeightedGraph;
import com.wasteflow.route.dto.OptimizeRouteRequest;
import com.wasteflow.route.dto.RouteResponse;
import com.wasteflow.truck.Truck;
import com.wasteflow.truck.TruckRepository;
import com.wasteflow.truck.TruckStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final RouteStopRepository stopRepository;
    private final TruckRepository truckRepository;
    private final CollectionRequestRepository requestRepository;
    private final RoadRepository roadRepository;
    private final DijkstraPathFinder pathFinder = new DijkstraPathFinder();

    public RouteService(RouteRepository routeRepository, RouteStopRepository stopRepository, TruckRepository truckRepository,
                        CollectionRequestRepository requestRepository, RoadRepository roadRepository) {
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.truckRepository = truckRepository;
        this.requestRepository = requestRepository;
        this.roadRepository = roadRepository;
    }

    @Transactional
    public RouteResponse optimize(OptimizeRouteRequest input) {
        if (!"DIJKSTRA".equalsIgnoreCase(input.algorithm())) {
            throw new IllegalArgumentException("MVP supports only the DIJKSTRA algorithm");
        }
        Truck truck = truckRepository.findById(input.truckId())
                .orElseThrow(() -> new IllegalArgumentException("Truck not found: " + input.truckId()));
        if (truck.getStatus() != TruckStatus.AVAILABLE) {
            throw new IllegalStateException("Truck is not available: " + truck.getTruckCode());
        }
        List<CollectionRequest> requests = input.requestIds().stream()
                .map(id -> requestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Request not found: " + id)))
                .sorted(Comparator.comparingDouble(CollectionRequest::getPriorityScore).reversed()
                        .thenComparing(CollectionRequest::getCreatedAt))
                .toList();
        double load = requests.stream().mapToDouble(request -> request.getBin().getCurrentFillKg()).sum();
        if (load > truck.getCapacityKg()) {
            throw new IllegalStateException("Selected requests exceed truck capacity");
        }

        WeightedGraph graph = buildGraph();
        Long currentLocation = truck.getLocation().getId();
        double totalDistance = 0;
        double totalTime = 0;
        for (CollectionRequest request : requests) {
            var path = pathFinder.findPath(graph, currentLocation, request.getBin().getLocation().getId());
            totalDistance += path.distance();
            totalTime += path.time();
            currentLocation = request.getBin().getLocation().getId();
        }

        truck.assign(load);
        Route route = routeRepository.save(new Route(truck, "DIJKSTRA", totalDistance, totalTime));
        List<RouteStop> stops = new java.util.ArrayList<>();
        int sequence = 1;
        for (CollectionRequest request : requests) {
            request.assign();
            stops.add(new RouteStop(route, request.getBin(), sequence++));
        }
        requestRepository.saveAll(requests);
        stopRepository.saveAll(stops);
        return RouteResponse.from(route, stops);
    }

    @Transactional(readOnly = true)
    public RouteResponse findById(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Route not found: " + id));
        return RouteResponse.from(route, stopRepository.findByRouteOrderBySequenceNumber(route));
    }

    @Transactional(readOnly = true)
    public Optional<RouteResponse> findActive() {
        return routeRepository.findFirstByStatusInOrderByCreatedAtDesc(List.of(RouteStatus.ASSIGNED, RouteStatus.COLLECTING))
                .map(route -> RouteResponse.from(route, stopRepository.findByRouteOrderBySequenceNumber(route)));
    }

    @Transactional
    public RouteResponse start(Long id) {
        Route route = getRoute(id);
        if (route.getStatus() != RouteStatus.ASSIGNED) {
            throw new IllegalStateException("Route cannot be started from status " + route.getStatus());
        }
        route.start();
        route.getTruck().startCollecting();
        return currentResponse(route);
    }

    @Transactional
    public RouteResponse completeStop(Long routeId, Long stopId) {
        Route route = getRoute(routeId);
        if (route.getStatus() != RouteStatus.COLLECTING) {
            throw new IllegalStateException("Start the route before completing stops");
        }
        RouteStop stop = stopRepository.findById(stopId)
                .orElseThrow(() -> new IllegalArgumentException("Route stop not found: " + stopId));
        if (!stop.getRoute().getId().equals(routeId)) {
            throw new IllegalArgumentException("Route stop does not belong to route " + routeId);
        }
        if (stop.getStatus() != RouteStopStatus.PENDING) {
            throw new IllegalStateException("Route stop is already completed");
        }
        stop.complete();
        requestRepository.findFirstByBinAndStatusInOrderByCreatedAtDesc(
                        stop.getBin(), List.of(RequestStatus.ASSIGNED, RequestStatus.PENDING))
                .ifPresent(CollectionRequest::complete);
        route.getTruck().reduceLoad(stop.getCollectedKg());
        return currentResponse(route);
    }

    @Transactional
    public RouteResponse complete(Long id) {
        Route route = getRoute(id);
        List<RouteStop> stops = stopRepository.findByRouteOrderBySequenceNumber(route);
        if (stops.stream().anyMatch(stop -> stop.getStatus() == RouteStopStatus.PENDING)) {
            throw new IllegalStateException("Complete all route stops before completing the route");
        }
        route.complete();
        route.getTruck().makeAvailable();
        return currentResponse(route);
    }

    private Route getRoute(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Route not found: " + id));
    }

    private RouteResponse currentResponse(Route route) {
        return RouteResponse.from(route, stopRepository.findByRouteOrderBySequenceNumber(route));
    }

    private WeightedGraph buildGraph() {
        WeightedGraph graph = new WeightedGraph();
        for (Road road : roadRepository.findAll()) {
            graph.addEdge(road.getSource().getId(), road.getDestination().getId(), road.getDistance(), road.getTravelTimeMinutes());
            if (road.isBidirectional()) {
                graph.addEdge(road.getDestination().getId(), road.getSource().getId(), road.getDistance(), road.getTravelTimeMinutes());
            }
        }
        return graph;
    }
}