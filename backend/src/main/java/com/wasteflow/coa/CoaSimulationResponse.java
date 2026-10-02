package com.wasteflow.coa;

import java.util.List;
import java.util.Map;

public record CoaSimulationResponse(
        String operation,
        String result,
        List<String> assemblyProgram,
        Map<String, Long> registers,
        CpuFlags flags,
        List<TraceStep> trace
) {
    public record CpuFlags(boolean zero, boolean sign, boolean carry, boolean overflow) {
    }

    public record TraceStep(
            int step,
            String instruction,
            Map<String, Long> registers,
            CpuFlags flags
    ) {
    }
}
