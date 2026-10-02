package com.wasteflow.route;

import com.wasteflow.bin.WasteBin;
import com.wasteflow.bin.WasteType;
import com.wasteflow.collection.CollectionRequest;
import com.wasteflow.location.Location;
import com.wasteflow.route.algorithm.WeightedGraph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RouteOrderingStrategyTest {

    @Test
    void priorityStrategyKeepsTheUrgentRequestFirst() {
        CollectionRequest normal = request("BIN-NORMAL", 2L, 40);
        CollectionRequest critical = request("BIN-CRITICAL", 3L, 95);

        List<CollectionRequest> ordered = new PriorityFirstRouteStrategy()
                .orderRequests(new WeightedGraph(), 1L, List.of(normal, critical));

        assertEquals(List.of(critical, normal), ordered);
    }

    @Test
    void nearestStopStrategyChoosesTheClosestRequestBeforeTheMoreUrgentOne() {
        CollectionRequest urgent = request("BIN-URGENT", 2L, 95);
        CollectionRequest nearby = request("BIN-NEARBY", 3L, 40);
        WeightedGraph graph = new WeightedGraph();
        graph.addEdge(1L, 2L, 8, 8);
        graph.addEdge(1L, 3L, 2, 2);
        graph.addEdge(3L, 2L, 2, 2);

        List<CollectionRequest> ordered = new NearestStopRouteStrategy()
                .orderRequests(graph, 1L, List.of(urgent, nearby));

        assertEquals(List.of(nearby, urgent), ordered);
    }

    private CollectionRequest request(String code, Long locationId, double fillKg) {
        Location location = mock(Location.class);
        when(location.getId()).thenReturn(locationId);
        WasteBin bin = new WasteBin(code, location, 100, fillKg, WasteType.GENERAL);
        return new CollectionRequest(bin);
    }
}