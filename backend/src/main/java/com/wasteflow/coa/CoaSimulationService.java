package com.wasteflow.coa;

import com.wasteflow.coa.CoaSimulationRequest.BinStatus;
import com.wasteflow.coa.CoaSimulationRequest.Leg;
import com.wasteflow.coa.CoaSimulationRequest.RouteSummary;
import com.wasteflow.coa.CoaSimulationRequest.RouteTotals;
import com.wasteflow.coa.CoaSimulationRequest.RouteWorkload;
import com.wasteflow.coa.CoaSimulationResponse.CpuFlags;
import com.wasteflow.coa.CoaSimulationResponse.TraceStep;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    public CoaSimulationResponse simulateRouteWorkload(RouteWorkload input) {
        validateFiniteRange(input.distanceKm(), "Distance");
        if (input.repetitionCount() < 1 || input.repetitionCount() > 100) {
            throw new IllegalArgumentException("Route repetition count must be between 1 and 100");
        }

        List<String> program = List.of(
                "MOV RAX, 0",
                "MOV RBX, " + toMilliUnits(input.distanceKm()),
                "MOV RCX, " + input.repetitionCount(),
                "REPEAT_ROUTE:",
                "ADD RAX, RBX",
                "DEC RCX",
                "CMP RCX, 0",
                "JNZ REPEAT_ROUTE",
                "HLT"
        );
        X64InstructionSimulator.Result execution = new X64InstructionSimulator().execute(program);
        double repeatedDistanceKm = execution.registers().get("RAX") / 1000.0;
        return response(
                "ROUTE_WORKLOAD",
                String.format(Locale.ROOT, "%d runs x %.3f km = %.3f km total",
                        input.repetitionCount(), input.distanceKm(), repeatedDistanceKm),
                program,
                execution
        );
    }

    public CoaSimulationResponse simulateRouteSummary(RouteSummary input) {
        if (input.binCodes().isEmpty() || input.binCodes().size() > 10) {
            throw new IllegalArgumentException("A route summary must contain between 1 and 10 bin codes");
        }

        ByteArrayOutputStream inputMemory = new ByteArrayOutputStream();
        for (String binCode : input.binCodes()) {
            if (binCode.isBlank() || binCode.length() > 32
                    || binCode.chars().anyMatch(character -> character < 0x20 || character > 0x7e)) {
                throw new IllegalArgumentException("Bin codes must contain 1 to 32 printable ASCII characters");
            }
            inputMemory.writeBytes(binCode.getBytes(StandardCharsets.US_ASCII));
            inputMemory.write(0);
        }

        List<String> program = List.of(
                "MOV RSI, 0",
                "MOV RDI, 0",
                "MOV RCX, " + input.binCodes().size(),
                "NEXT_CHARACTER:",
                "LODSB",
                "TEST AL, AL",
                "JZ END_BIN_CODE",
                "STOSB",
                "JMP NEXT_CHARACTER",
                "END_BIN_CODE:",
                "DEC RCX",
                "CMP RCX, 0",
                "JE FINISH_SUMMARY",
                "MOV AL, 32",
                "STOSB",
                "MOV AL, 45",
                "STOSB",
                "MOV AL, 62",
                "STOSB",
                "MOV AL, 32",
                "STOSB",
                "JMP NEXT_CHARACTER",
                "FINISH_SUMMARY:",
                "MOV AL, 0",
                "STOSB",
                "HLT"
        );
        byte[] sourceBytes = inputMemory.toByteArray();
        int outputCapacity = sourceBytes.length + (input.binCodes().size() - 1) * 4 + 1;
        X64InstructionSimulator.Result execution = new X64InstructionSimulator()
                .execute(program, sourceBytes, outputCapacity);
        return response("ROUTE_SUMMARY", execution.output(), program, execution);
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
