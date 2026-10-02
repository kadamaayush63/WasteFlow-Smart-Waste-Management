package com.wasteflow.coa;

import com.wasteflow.coa.CoaSimulationRequest.BinStatus;
import com.wasteflow.coa.CoaSimulationRequest.Leg;
import com.wasteflow.coa.CoaSimulationRequest.RouteTotals;
import com.wasteflow.coa.CoaSimulationResponse.CpuFlags;
import com.wasteflow.coa.CoaSimulationResponse.TraceStep;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
public class CoaSimulationService {

    private static final double MAX_INPUT_KG_OR_DISTANCE = 1_000_000_000;

    public CoaSimulationResponse simulateBinStatus(BinStatus input) {
        validateFiniteRange(input.currentFillKg(), "Current fill");
        validateFiniteRange(input.capacityKg(), "Capacity");
        if (input.capacityKg() <= 0 || input.currentFillKg() > input.capacityKg()) {
            throw new IllegalArgumentException("Bin fill must be between zero and its positive capacity");
        }

        long currentFillMilliKg = toMilliUnits(input.currentFillKg());
        long capacityMilliKg = toMilliUnits(input.capacityKg());
        List<String> program = List.of(
                "MOV RAX, " + currentFillMilliKg,
                "IMUL RAX, 100",
                "MOV RBX, " + capacityMilliKg,
                "CQO",
                "IDIV RBX",
                "CMP RAX, 95",
                "JGE CRITICAL",
                "CMP RAX, 75",
                "JGE HIGH",
                "CMP RAX, 50",
                "JGE MEDIUM",
                "MOV RCX, 0",
                "JMP DONE",
                "CRITICAL:",
                "MOV RCX, 3",
                "JMP DONE",
                "HIGH:",
                "MOV RCX, 2",
                "JMP DONE",
                "MEDIUM:",
                "MOV RCX, 1",
                "DONE:",
                "HLT"
        );
        X64InstructionSimulator.Result execution = new X64InstructionSimulator().execute(program);
        long percentage = execution.registers().get("RAX");
        String status = switch (Math.toIntExact(execution.registers().get("RCX"))) {
            case 0 -> "NORMAL";
            case 1 -> "MEDIUM";
            case 2 -> "HIGH";
            case 3 -> "CRITICAL";
            default -> throw new IllegalStateException("CPU simulator produced an unknown bin status");
        };
        return response(
                "BIN_STATUS",
                String.format(Locale.ROOT, "%d%% fill -> %s", percentage, status),
                program,
                execution
        );
    }

    public CoaSimulationResponse simulateRouteTotals(RouteTotals input) {
        if (input.legs().size() > 100) {
            throw new IllegalArgumentException("A COA demonstration route can contain at most 100 legs");
        }
        List<String> program = new ArrayList<>();
        program.add("MOV RAX, 0");
        program.add("MOV RBX, 0");
        for (Leg leg : input.legs()) {
            validateFiniteRange(leg.distanceKm(), "Distance");
            validateFiniteRange(leg.travelTimeMinutes(), "Travel time");
            program.add("ADD RAX, " + toMilliUnits(leg.distanceKm()));
            program.add("ADD RBX, " + toMilliUnits(leg.travelTimeMinutes()));
        }
        program.add("HLT");

        X64InstructionSimulator.Result execution = new X64InstructionSimulator().execute(program);
        double distanceKm = execution.registers().get("RAX") / 1000.0;
        double travelTimeMinutes = execution.registers().get("RBX") / 1000.0;
        return response(
                "ROUTE_TOTALS",
                String.format(Locale.ROOT, "%.3f km; %.3f minutes", distanceKm, travelTimeMinutes),
                program,
                execution
        );
    }

    private void validateFiniteRange(double value, String label) {
        if (!Double.isFinite(value) || value < 0 || value > MAX_INPUT_KG_OR_DISTANCE) {
            throw new IllegalArgumentException(label + " must be finite and between 0 and 1,000,000,000");
        }
    }

    private long toMilliUnits(double value) {
        return Math.round(value * 1000);
    }

    private CoaSimulationResponse response(
            String operation,
            String result,
            List<String> source,
            X64InstructionSimulator.Result execution
    ) {
        List<String> listing = List.copyOf(source);
        Map<String, Long> registers = execution.registers();
        CpuFlags flags = execution.flags();
        List<TraceStep> trace = execution.trace();
        return new CoaSimulationResponse(operation, result, listing, registers, flags, trace);
    }
}
