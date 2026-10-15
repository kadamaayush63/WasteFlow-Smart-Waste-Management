package com.wasteflow.route;

import com.wasteflow.bin.WasteBin;
import com.wasteflow.collection.CollectionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RouteFillEligibilityTest {

    @Test
    void requiresAtLeastTenPercentFillForRouteEligibility() {
        assertFalse(RouteService.isRouteEligible(requestWithFill(9.99)));
        assertTrue(RouteService.isRouteEligible(requestWithFill(10)));
        assertTrue(RouteService.isRouteEligible(requestWithFill(50)));
    }

    private CollectionRequest requestWithFill(double fillPercentage) {
        CollectionRequest request = mock(CollectionRequest.class);
        WasteBin bin = mock(WasteBin.class);
        when(request.getBin()).thenReturn(bin);
        when(bin.getCapacityKg()).thenReturn(200.0);
        when(bin.getCurrentFillKg()).thenReturn(200 * fillPercentage / 100);
        return request;
    }
}
