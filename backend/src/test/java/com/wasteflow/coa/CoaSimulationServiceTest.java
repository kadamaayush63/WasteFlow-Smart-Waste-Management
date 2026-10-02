package com.wasteflow.coa;

import com.wasteflow.coa.CoaSimulationRequest.BinStatus;
import com.wasteflow.coa.CoaSimulationRequest.Leg;
import com.wasteflow.coa.CoaSimulationRequest.RouteTotals;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoaSimulationServiceTest {

    private final CoaSimulationService service = new CoaSimulationService();

    @Test
    void simulatesBinClassificationAtCriticalThreshold() {
        CoaSimulationResponse result = service.simulateBinStatus(new BinStatus(95, 100));

        assertEquals("95% fill -> CRITICAL", result.result());
        assertEquals(95, result.registers().get("RAX"));
        assertEquals(3, result.registers().get("RCX"));
        assertTrue(result.trace().stream().anyMatch(step -> step.instruction().startsWith("IDIV")));
        assertEquals("HLT", result.trace().getLast().instruction());
    }

    @Test
    void rejectsFillAboveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> service.simulateBinStatus(new BinStatus(101, 100)));
    }

    @Test
    void simulatesRouteDistanceAndTimeAccumulationWithAddInstructions() {
        CoaSimulationResponse result = service.simulateRouteTotals(new RouteTotals(List.of(
                new Leg(3.25, 4.5),
                new Leg(2.75, 5.5)
        )));

        assertEquals("6.000 km; 10.000 minutes", result.result());
        assertEquals(6000, result.registers().get("RAX"));
        assertEquals(10000, result.registers().get("RBX"));
        assertEquals(4, result.trace().stream().filter(step -> step.instruction().startsWith("ADD")).count());
        assertFalse(result.flags().overflow());
    }
}
