package com.wasteflow.route;

import com.wasteflow.bin.WasteBin;
import com.wasteflow.location.Location;
import com.wasteflow.route.dto.RouteResponse;
import com.wasteflow.truck.Truck;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RouteResponseTest {

    @Test
    void buildsReadableRouteSummary() {
        Route route = mock(Route.class);
        Truck truck = mock(Truck.class);
        RouteStop stop = mock(RouteStop.class);
        WasteBin bin = mock(WasteBin.class);
        Location location = mock(Location.class);
        when(route.getId()).thenReturn(9L);
        when(route.getTruck()).thenReturn(truck);
        when(truck.getId()).thenReturn(2L);
        when(truck.getTruckCode()).thenReturn("TRUCK-002");
        when(route.getAlgorithm()).thenReturn("DIJKSTRA");
        when(route.getStatus()).thenReturn(RouteStatus.ASSIGNED);
        when(route.getTotalDistance()).thenReturn(12.5);
        when(route.getTotalTime()).thenReturn(8.0);
        when(stop.getId()).thenReturn(1L);
        when(stop.getSequenceNumber()).thenReturn(1);
        when(stop.getBin()).thenReturn(bin);
        when(bin.getId()).thenReturn(3L);
        when(bin.getBinCode()).thenReturn("BIN-003");
        when(bin.getLocation()).thenReturn(location);
        when(location.getId()).thenReturn(4L);
        when(stop.getStatus()).thenReturn(RouteStopStatus.PENDING);

        RouteResponse response = RouteResponse.from(route, List.of(stop));

        assertEquals("Route #9 for TRUCK-002: BIN-003 | 12.5 km, 8.0 min", response.summary());
    }

    @Test
    void summarizesRoutesWithoutStops() {
        Route route = mock(Route.class);
        Truck truck = mock(Truck.class);
        when(route.getId()).thenReturn(10L);
        when(route.getTruck()).thenReturn(truck);
        when(truck.getId()).thenReturn(2L);
        when(truck.getTruckCode()).thenReturn("TRUCK-002");
        when(route.getAlgorithm()).thenReturn("DIJKSTRA");
        when(route.getStatus()).thenReturn(RouteStatus.ASSIGNED);
        when(route.getTotalDistance()).thenReturn(0.0);
        when(route.getTotalTime()).thenReturn(0.0);

        RouteResponse response = RouteResponse.from(route, List.of());

        assertEquals("Route #10 for TRUCK-002: no collection stops | 0.0 km, 0.0 min", response.summary());
    }
}
