package com.wasteflow.bin;

import com.wasteflow.location.Location;
import com.wasteflow.location.LocationRepository;
import com.wasteflow.collection.CollectionRequest;
import com.wasteflow.collection.CollectionRequestRepository;
import com.wasteflow.collection.RequestStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WasteBinServiceTest {

    @Test
    void filtersBinsByCodeOrLocationUsingLinearSearch() {
        WasteBinRepository binRepository = mock(WasteBinRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        CollectionRequestRepository requestRepository = mock(CollectionRequestRepository.class);
        List<WasteBin> fixture = List.of(
                bin(1L, "BIN-NORMAL", BinStatus.NORMAL, 90),
                bin(2L, "BIN-CRITICAL-LOW", BinStatus.CRITICAL, 70),
                bin(3L, "BIN-CRITICAL-HIGH", BinStatus.CRITICAL, 95),
                bin(4L, "BIN-COLLECTED", BinStatus.COLLECTED, 100)
        );
        when(binRepository.findAllByOrderByStatusDescCurrentFillKgDesc()).thenReturn(fixture);

        List<WasteBinResponse> bins = new WasteBinService(binRepository, locationRepository, requestRepository)
            .findAll("critical");

        assertEquals(List.of("BIN-CRITICAL-LOW", "BIN-CRITICAL-HIGH"),
                bins.stream().map(WasteBinResponse::binCode).toList());
    }

    @Test
    void updatesEachSensorBinUsingItsReportedFillPercentage() {
        WasteBinRepository binRepository = mock(WasteBinRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        CollectionRequestRepository requestRepository = mock(CollectionRequestRepository.class);
        WasteBin firstBin = mock(WasteBin.class);
        WasteBin secondBin = mock(WasteBin.class);
        WasteBin thirdBin = mock(WasteBin.class);

        when(firstBin.getId()).thenReturn(1L);
        when(firstBin.getCapacityKg()).thenReturn(200.0);
        when(firstBin.getStatus()).thenReturn(BinStatus.HIGH);
        when(secondBin.getId()).thenReturn(2L);
        when(secondBin.getCapacityKg()).thenReturn(200.0);
        when(secondBin.getStatus()).thenReturn(BinStatus.MEDIUM);
        when(thirdBin.getId()).thenReturn(3L);
        when(thirdBin.getCapacityKg()).thenReturn(200.0);
        when(thirdBin.getStatus()).thenReturn(BinStatus.NORMAL);
        when(binRepository.findAllById(Set.of(1L, 2L, 3L)))
                .thenReturn(List.of(firstBin, secondBin, thirdBin));
        when(requestRepository.findFirstByBinAndStatusInOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList()
        )).thenReturn(Optional.empty());

        new WasteBinService(binRepository, locationRepository, requestRepository).updateFillPercentages(
                Map.of(1L, 100.0, 2L, 50.0, 3L, 0.0),
                Instant.parse("2026-10-07T08:00:00Z"),
                Instant.parse("2026-10-07T08:00:01Z")
        );

        verify(firstBin).setCurrentFillKg(200.0);
        verify(secondBin).setCurrentFillKg(100.0);
        verify(thirdBin).setCurrentFillKg(0.0);
    }

    @Test
    void ignoresCollectedBinReadingsDuringCooldownThenAcceptsFreshReading() {
        WasteBinRepository binRepository = mock(WasteBinRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        CollectionRequestRepository requestRepository = mock(CollectionRequestRepository.class);
        WasteBin bin = mock(WasteBin.class);
        Instant collectedAt = Instant.parse("2026-10-07T08:00:00Z");
        when(bin.getId()).thenReturn(1L);
        when(bin.getCapacityKg()).thenReturn(200.0);
        when(bin.getStatus()).thenReturn(BinStatus.NORMAL);
        when(bin.getLastCollectedAt()).thenReturn(collectedAt);
        when(binRepository.findAllById(Set.of(1L))).thenReturn(List.of(bin));

        WasteBinService service = new WasteBinService(binRepository, locationRepository, requestRepository);
        service.updateFillPercentages(
                Map.of(1L, 80.0),
                collectedAt.minusSeconds(1),
                collectedAt.plusSeconds(21)
        );
        service.updateFillPercentages(
                Map.of(1L, 80.0),
                collectedAt.plusSeconds(1),
                collectedAt.plusSeconds(19)
        );
        service.updateFillPercentages(
                Map.of(1L, 80.0),
                collectedAt.plusSeconds(15),
                collectedAt.plusSeconds(20)
        );

        verify(bin).setCurrentFillKg(160.0);
        verify(requestRepository).save(org.mockito.ArgumentMatchers.any(CollectionRequest.class));
    }

    @Test
    void createsTruckRequestWhenFreshReadingIsAtLeastTenPercent() {
        WasteBinRepository binRepository = mock(WasteBinRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        CollectionRequestRepository requestRepository = mock(CollectionRequestRepository.class);
        WasteBin bin = mock(WasteBin.class);
        Instant readingAt = Instant.parse("2026-10-07T08:00:00Z");
        when(bin.getId()).thenReturn(1L);
        when(bin.getCapacityKg()).thenReturn(200.0);
        when(bin.getStatus()).thenReturn(BinStatus.MEDIUM);
        when(binRepository.findAllById(Set.of(1L))).thenReturn(List.of(bin));
        when(requestRepository.findFirstByBinAndStatusInOrderByCreatedAtDesc(
                bin, List.of(RequestStatus.PENDING, RequestStatus.ASSIGNED)))
                .thenReturn(Optional.empty());

        new WasteBinService(binRepository, locationRepository, requestRepository).updateFillPercentages(
                Map.of(1L, 66.5),
                readingAt,
                readingAt.plusSeconds(1)
        );

        verify(bin).setCurrentFillKg(133.0);
        verify(requestRepository).save(org.mockito.ArgumentMatchers.any(CollectionRequest.class));
    }

    @Test
    void refreshesExistingPendingRequestWhenSensorFillChanges() {
        WasteBinRepository binRepository = mock(WasteBinRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        CollectionRequestRepository requestRepository = mock(CollectionRequestRepository.class);
        WasteBin bin = mock(WasteBin.class);
        CollectionRequest request = mock(CollectionRequest.class);
        when(bin.getId()).thenReturn(1L);
        when(bin.getCapacityKg()).thenReturn(200.0);
        when(bin.getStatus()).thenReturn(BinStatus.HIGH);
        when(binRepository.findAllById(Set.of(1L))).thenReturn(List.of(bin));
        when(request.getStatus()).thenReturn(RequestStatus.PENDING);
        when(requestRepository.findFirstByBinAndStatusInOrderByCreatedAtDesc(
                bin, List.of(RequestStatus.PENDING, RequestStatus.ASSIGNED)))
                .thenReturn(Optional.of(request));

        new WasteBinService(binRepository, locationRepository, requestRepository).updateFillPercentages(
                Map.of(1L, 80.0),
                Instant.parse("2026-10-07T08:00:00Z"),
                Instant.parse("2026-10-07T08:00:01Z")
        );

        verify(request).refreshFromBin();
        verify(request, never()).cancel();
        verify(requestRepository, never()).save(org.mockito.ArgumentMatchers.any(CollectionRequest.class));
    }

    @Test
    void keepsPendingRequestAndUpdatesItWhenFreshReadingIsAboveZero() {
        WasteBinRepository binRepository = mock(WasteBinRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        CollectionRequestRepository requestRepository = mock(CollectionRequestRepository.class);
        WasteBin bin = mock(WasteBin.class);
        CollectionRequest request = mock(CollectionRequest.class);
        when(bin.getId()).thenReturn(1L);
        when(bin.getCapacityKg()).thenReturn(200.0);
        when(bin.getStatus()).thenReturn(BinStatus.NORMAL);
        when(binRepository.findAllById(Set.of(1L))).thenReturn(List.of(bin));
        when(request.getStatus()).thenReturn(RequestStatus.PENDING);
        when(requestRepository.findFirstByBinAndStatusInOrderByCreatedAtDesc(
                bin, List.of(RequestStatus.PENDING, RequestStatus.ASSIGNED)))
                .thenReturn(Optional.of(request));

        new WasteBinService(binRepository, locationRepository, requestRepository).updateFillPercentages(
                Map.of(1L, 5.0),
                Instant.parse("2026-10-07T08:00:00Z"),
                Instant.parse("2026-10-07T08:00:01Z")
        );

        verify(request).refreshFromBin();
        verify(request, never()).cancel();
    }

    @Test
    void cancelsPendingRequestWhenFillIsExactlyZero() {
        WasteBinRepository binRepository = mock(WasteBinRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        CollectionRequestRepository requestRepository = mock(CollectionRequestRepository.class);
        WasteBin bin = mock(WasteBin.class);
        CollectionRequest request = mock(CollectionRequest.class);
        when(bin.getId()).thenReturn(1L);
        when(bin.getCapacityKg()).thenReturn(200.0);
        when(bin.getStatus()).thenReturn(BinStatus.NORMAL);
        when(binRepository.findAllById(Set.of(1L))).thenReturn(List.of(bin));
        when(request.getStatus()).thenReturn(RequestStatus.PENDING);
        when(requestRepository.findFirstByBinAndStatusInOrderByCreatedAtDesc(
                bin, List.of(RequestStatus.PENDING, RequestStatus.ASSIGNED)))
                .thenReturn(Optional.of(request));

        new WasteBinService(binRepository, locationRepository, requestRepository).updateFillPercentages(
                Map.of(1L, 0.0),
                Instant.parse("2026-10-07T08:00:00Z"),
                Instant.parse("2026-10-07T08:00:01Z")
        );

        verify(request).cancel();
        verify(request, never()).refreshFromBin();
    }

    private WasteBin bin(Long id, String code, BinStatus status, double fill) {
        WasteBin bin = mock(WasteBin.class);
        Location location = mock(Location.class);
        when(bin.getId()).thenReturn(id);
        when(bin.getBinCode()).thenReturn(code);
        when(bin.getLocation()).thenReturn(location);
        when(location.getId()).thenReturn(1L);
        when(location.getName()).thenReturn("Test location");
        when(bin.getCapacityKg()).thenReturn(100.0);
        when(bin.getCurrentFillKg()).thenReturn(fill);
        when(bin.getWasteType()).thenReturn(WasteType.GENERAL);
        when(bin.getStatus()).thenReturn(status);
        return bin;
    }
}