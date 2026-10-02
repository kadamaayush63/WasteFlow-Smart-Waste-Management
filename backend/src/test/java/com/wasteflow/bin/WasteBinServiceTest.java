package com.wasteflow.bin;

import com.wasteflow.location.Location;
import com.wasteflow.location.LocationRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WasteBinServiceTest {

    @Test
    void filtersBinsByCodeOrLocationUsingLinearSearch() {
        WasteBinRepository binRepository = mock(WasteBinRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        List<WasteBin> fixture = List.of(
                bin(1L, "BIN-NORMAL", BinStatus.NORMAL, 90),
                bin(2L, "BIN-CRITICAL-LOW", BinStatus.CRITICAL, 70),
                bin(3L, "BIN-CRITICAL-HIGH", BinStatus.CRITICAL, 95),
                bin(4L, "BIN-COLLECTED", BinStatus.COLLECTED, 100)
        );
        when(binRepository.findAllByOrderByStatusDescCurrentFillKgDesc()).thenReturn(fixture);

        List<WasteBinResponse> bins = new WasteBinService(binRepository, locationRepository)
            .findAll("critical");

        assertEquals(List.of("BIN-CRITICAL-LOW", "BIN-CRITICAL-HIGH"),
                bins.stream().map(WasteBinResponse::binCode).toList());
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