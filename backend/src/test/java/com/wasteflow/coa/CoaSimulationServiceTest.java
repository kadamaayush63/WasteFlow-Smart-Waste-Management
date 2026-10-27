package com.wasteflow.coa;

import com.wasteflow.coa.CoaSimulationRequest.BinStatus;
import com.wasteflow.coa.CoaSimulationRequest.Leg;
import com.wasteflow.coa.CoaSimulationRequest.RouteSummary;
import com.wasteflow.coa.CoaSimulationRequest.RouteTotals;
import com.wasteflow.coa.CoaSimulationRequest.RouteWorkload;
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

    @Test
    void estimatesRepeatedRouteDistanceUsingSuccessiveAddition() {
        CoaSimulationResponse result = service.simulateRouteWorkload(new RouteWorkload(3.25, 4));

        assertEquals("4 runs x 3.250 km = 13.000 km total", result.result());
        assertEquals(13000, result.registers().get("RAX"));
        assertEquals(4, result.trace().stream().filter(step -> step.instruction().equals("ADD RAX, RBX")).count());
        assertEquals("JNZ REPEAT_ROUTE", result.trace().get(result.trace().size() - 2).instruction());
        assertEquals("HLT", result.trace().getLast().instruction());
    }

    @Test
    void buildsRouteSummaryUsingSimulatedX64StringInstructions() {
        CoaSimulationResponse result = service.simulateRouteSummary(
                new RouteSummary(List.of("BIN-01", "BIN-02", "BIN-03")));

        assertEquals("BIN-01 -> BIN-02 -> BIN-03", result.result());
        assertTrue(result.assemblyProgram().contains("LODSB"));
        assertTrue(result.assemblyProgram().contains("STOSB"));
        assertTrue(result.trace().stream().anyMatch(step -> step.instruction().equals("JZ END_BIN_CODE")));
        assertTrue(result.trace().stream().anyMatch(step -> step.instruction().equals("LODSB")
                && step.registers().get("RAX") == (long) 'B'));
        assertEquals("HLT", result.trace().getLast().instruction());
    }

    @Test
    void rejectsNonAsciiRouteBinCodesForByteStringSimulation() {
        assertThrows(IllegalArgumentException.class,
                () -> service.simulateRouteSummary(new RouteSummary(List.of("BIN-É"))));
    }

    @Test
    void rejectsRouteWorkloadsOutsideSupportedRepetitionRange() {
        assertThrows(IllegalArgumentException.class,
                () -> service.simulateRouteWorkload(new RouteWorkload(3.25, 101)));
    }
}
