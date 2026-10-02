package com.wasteflow.coa;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class X64InstructionSimulator {

    private static final BigInteger TWO_TO_64 = BigInteger.ONE.shiftLeft(Long.SIZE);
    private static final BigInteger UNSIGNED_LONG_MASK = TWO_TO_64.subtract(BigInteger.ONE);

    private final Map<String, Long> registers = new LinkedHashMap<>();
    private final List<CoaSimulationResponse.TraceStep> trace = new ArrayList<>();
    private boolean zero;
    private boolean sign;
    private boolean carry;
    private boolean overflow;

    X64InstructionSimulator() {
        registers.put("RAX", 0L);
        registers.put("RBX", 0L);
        registers.put("RCX", 0L);
        registers.put("RDX", 0L);
    }

    Result execute(List<String> source) {
        List<Instruction> instructions = new ArrayList<>();
        Map<String, Integer> labels = new LinkedHashMap<>();
        for (String rawLine : source) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            if (line.endsWith(":")) {
                labels.put(line.substring(0, line.length() - 1).toUpperCase(Locale.ROOT), instructions.size());
            } else {
                instructions.add(parse(line));
            }
        }

        int programCounter = 0;
        int maxSteps = Math.max(1, instructions.size() * 2);
        while (programCounter < instructions.size() && trace.size() < maxSteps) {
            Instruction instruction = instructions.get(programCounter);
            int nextProgramCounter = programCounter + 1;
            boolean halted = false;
            switch (instruction.opcode()) {
                case "MOV" -> registers.put(instruction.register(), instruction.value());
                case "ADD" -> add(instruction.register(), instruction.value());
                case "IMUL" -> multiply(instruction.register(), instruction.value());
                case "CQO" -> registers.put("RDX", registers.get("RAX") < 0 ? -1L : 0L);
                case "IDIV" -> divide(instruction.register());
                case "CMP" -> compare(registers.get(instruction.register()), instruction.value());
                case "JGE" -> {
                    if (signEqualsOverflow()) nextProgramCounter = label(labels, instruction.label());
                }
                case "JL" -> {
                    if (!signEqualsOverflow()) nextProgramCounter = label(labels, instruction.label());
                }
                case "JMP" -> nextProgramCounter = label(labels, instruction.label());
                case "HLT" -> halted = true;
                default -> throw new IllegalArgumentException("Unsupported simulated instruction: " + instruction.opcode());
            }
            trace.add(new CoaSimulationResponse.TraceStep(
                    trace.size() + 1,
                    instruction.display(),
                    Map.copyOf(registers),
                    flags()
            ));
            if (halted) break;
            programCounter = nextProgramCounter;
        }
        if (trace.isEmpty() || !trace.get(trace.size() - 1).instruction().equals("HLT")) {
            throw new IllegalStateException("Simulated CPU program did not halt");
        }
        return new Result(Map.copyOf(registers), flags(), List.copyOf(trace));
    }

    private void add(String register, long value) {
        long left = registers.get(register);
        long result = left + value;
        carry = Long.compareUnsigned(result, left) < 0;
        overflow = ((left ^ result) & (value ^ result)) < 0;
        registers.put(register, result);
        zero = result == 0;
        sign = result < 0;
    }

    private void multiply(String register, long value) {
        long left = registers.get(register);
        long result = left * value;
        long high = Math.multiplyHigh(left, value);
        long expectedHigh = result < 0 ? -1L : 0L;
        carry = high != expectedHigh;
        overflow = carry;
        registers.put(register, result);
        zero = result == 0;
        sign = result < 0;
    }

    private void divide(String divisorRegister) {
        long divisor = registers.get(divisorRegister);
        if (divisor == 0) throw new IllegalArgumentException("IDIV divisor cannot be zero");
        BigInteger dividend = BigInteger.valueOf(registers.get("RDX")).shiftLeft(Long.SIZE)
                .add(unsigned(registers.get("RAX")));
        BigInteger[] quotientAndRemainder = dividend.divideAndRemainder(BigInteger.valueOf(divisor));
        long quotient = quotientAndRemainder[0].longValueExact();
        long remainder = quotientAndRemainder[1].longValueExact();
        registers.put("RAX", quotient);
        registers.put("RDX", remainder);
    }

    private void compare(long left, long right) {
        long result = left - right;
        zero = left == right;
        sign = result < 0;
        carry = Long.compareUnsigned(left, right) < 0;
        overflow = ((left ^ right) & (left ^ result)) < 0;
    }

    private boolean signEqualsOverflow() {
        return sign == overflow;
    }

    private int label(Map<String, Integer> labels, String name) {
        Integer target = labels.get(name);
        if (target == null) throw new IllegalArgumentException("Unknown assembly label: " + name);
        return target;
    }

    private Instruction parse(String line) {
        String[] parts = line.split("\\s+", 2);
        String opcode = parts[0].toUpperCase(Locale.ROOT);
        String operandText = parts.length == 2 ? parts[1].trim() : "";
        return switch (opcode) {
            case "MOV", "ADD", "IMUL", "CMP" -> {
                String[] operands = operandText.split("\\s*,\\s*", 2);
                if (operands.length != 2) throw new IllegalArgumentException("Invalid instruction: " + line);
                yield new Instruction(opcode, register(operands[0]), number(operands[1]), null);
            }
            case "IDIV" -> new Instruction(opcode, register(operandText), 0, null);
            case "JGE", "JL", "JMP" -> new Instruction(opcode, null, 0, operandText.toUpperCase(Locale.ROOT));
            case "CQO", "HLT" -> new Instruction(opcode, null, 0, null);
            default -> throw new IllegalArgumentException("Unsupported simulated instruction: " + opcode);
        };
    }

    private String register(String value) {
        String register = value.toUpperCase(Locale.ROOT);
        if (!registers.containsKey(register)) {
            throw new IllegalArgumentException("Unsupported simulated register: " + value);
        }
        return register;
    }

    private long number(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid 64-bit immediate value: " + value, exception);
        }
    }

    private BigInteger unsigned(long value) {
        BigInteger signedValue = BigInteger.valueOf(value);
        return value < 0 ? signedValue.add(TWO_TO_64) : signedValue.and(UNSIGNED_LONG_MASK);
    }

    private CoaSimulationResponse.CpuFlags flags() {
        return new CoaSimulationResponse.CpuFlags(zero, sign, carry, overflow);
    }

    private record Instruction(String opcode, String register, long value, String label) {
        String display() {
            if (register == null) return label == null ? opcode : opcode + " " + label;
            if (opcode.equals("IDIV")) return opcode + " " + register;
            return opcode + " " + register + ", " + value;
        }
    }

    record Result(
            Map<String, Long> registers,
            CoaSimulationResponse.CpuFlags flags,
            List<CoaSimulationResponse.TraceStep> trace
    ) {
    }
}
