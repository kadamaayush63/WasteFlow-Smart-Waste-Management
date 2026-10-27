package com.wasteflow.route;

import com.wasteflow.bin.WasteBin;
import com.wasteflow.collection.CollectionRequest;
import com.wasteflow.collection.CollectionRequestRepository;
import com.wasteflow.collection.RequestStatus;
import com.wasteflow.location.Location;
import com.wasteflow.road.RoadRepository;
import com.wasteflow.route.dto.RouteResponse;
import com.wasteflow.truck.Truck;
import com.wasteflow.truck.TruckRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RouteServiceUndoTest {

    @Test
    void undoCancelsNewestUnstartedRouteAndReturnsItsRequestsToPending() {
        RouteRepository routeRepository = mock(RouteRepository.class);
        RouteStopRepository stopRepository = mock(RouteStopRepository.class);
        TruckRepository truckRepository = mock(TruckRepository.class);
        CollectionRequestRepository requestRepository = mock(CollectionRequestRepository.class);
        RoadRepository roadRepository = mock(RoadRepository.class);
        Route olderRoute = mock(Route.class);
        Route newestRoute = mock(Route.class);
        Truck truck = mock(Truck.class);
        RouteStop stop = mock(RouteStop.class);
        WasteBin bin = mock(WasteBin.class);
        Location location = mock(Location.class);
        CollectionRequest request = mock(CollectionRequest.class);

        when(routeRepository.findByStatusOrderByCreatedAtAscIdAsc(RouteStatus.ASSIGNED))
                .thenReturn(List.of(olderRoute, newestRoute));
        when(newestRoute.getId()).thenReturn(2L);
        when(newestRoute.getTruck()).thenReturn(truck);
        when(newestRoute.getAlgorithm()).thenReturn("DIJKSTRA");
        when(newestRoute.getStatus()).thenReturn(RouteStatus.CANCELLED);
        when(truck.getId()).thenReturn(1L);
        when(truck.getTruckCode()).thenReturn("TRUCK-1");
        when(stopRepository.findByRouteOrderBySequenceNumber(newestRoute)).thenReturn(List.of(stop));
        when(stop.getId()).thenReturn(3L);
        when(stop.getSequenceNumber()).thenReturn(1);
        when(stop.getBin()).thenReturn(bin);
        when(stop.getStatus()).thenReturn(RouteStopStatus.PENDING);
        when(bin.getId()).thenReturn(4L);
        when(bin.getLocation()).thenReturn(location);
        when(location.getId()).thenReturn(5L);
        when(requestRepository.findFirstByBinAndStatusInOrderByCreatedAtDesc(
                bin, List.of(RequestStatus.ASSIGNED))).thenReturn(Optional.of(request));

        RouteService service = new RouteService(routeRepository, stopRepository, truckRepository,
                requestRepository, roadRepository, List.of());

        RouteResponse response = service.undoLatestPlannedRoute();

        assertEquals(2L, response.id());
        assertEquals("CANCELLED", response.status());
        verify(request).unassign();
        verify(truck).makeAvailable();
        verify(newestRoute).cancel();
    }

    @Test
    void undoFailsClearlyWhenThereAreNoUnstartedRoutes() {
        RouteRepository routeRepository = mock(RouteRepository.class);
        when(routeRepository.findByStatusOrderByCreatedAtAscIdAsc(RouteStatus.ASSIGNED))
                .thenReturn(List.of());

        RouteService service = new RouteService(routeRepository, mock(RouteStopRepository.class),
                mock(TruckRepository.class), mock(CollectionRequestRepository.class),
                mock(RoadRepository.class), List.of());

        assertThrows(IllegalStateException.class, service::undoLatestPlannedRoute);
    }
}
