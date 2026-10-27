package com.wasteflow.coa;

import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class X64InstructionSimulator {

    private static final BigInteger TWO_TO_64 = BigInteger.ONE.shiftLeft(Long.SIZE);
    private static final BigInteger UNSIGNED_LONG_MASK = TWO_TO_64.subtract(BigInteger.ONE);
    private static final int MAX_EXECUTION_STEPS = 100_000;

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
        registers.put("RSI", 0L);
        registers.put("RDI", 0L);
        registers.put("AL", 0L);
    }

    Result execute(List<String> source) {
        return execute(source, new byte[0], 0);
    }

    Result execute(List<String> source, byte[] inputMemory, int outputCapacity) {
        if (outputCapacity < 0) {
            throw new IllegalArgumentException("Output memory capacity cannot be negative");
        }
        byte[] outputMemory = new byte[outputCapacity];
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
        while (programCounter < instructions.size() && trace.size() < MAX_EXECUTION_STEPS) {
            Instruction instruction = instructions.get(programCounter);
            int nextProgramCounter = programCounter + 1;
            boolean halted = false;
            switch (instruction.opcode()) {
                case "MOV" -> writeRegister(instruction.register(), instruction.value());
                case "ADD" -> add(instruction.register(), operandValue(instruction, instruction.secondRegister()));
                case "IMUL" -> multiply(instruction.register(), instruction.value());
                case "CQO" -> registers.put("RDX", registers.get("RAX") < 0 ? -1L : 0L);
                case "IDIV" -> divide(instruction.register());
                case "CMP" -> compare(registers.get(instruction.register()),
                        operandValue(instruction, instruction.secondRegister()));
                case "TEST" -> test(instruction.register(), instruction.secondRegister());
                case "DEC" -> decrement(instruction.register());
                case "LODSB" -> loadStringByte(inputMemory);
                case "STOSB" -> storeStringByte(outputMemory, outputCapacity);
                case "JGE" -> {
                    if (signEqualsOverflow()) nextProgramCounter = label(labels, instruction.label());
                }
                case "JL" -> {
                    if (!signEqualsOverflow()) nextProgramCounter = label(labels, instruction.label());
                }
                case "JE", "JZ" -> {
                    if (zero) nextProgramCounter = label(labels, instruction.label());
                }
                case "JNZ" -> {
                    if (!zero) nextProgramCounter = label(labels, instruction.label());
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
        String output = outputString(outputMemory);
        return new Result(Map.copyOf(registers), flags(), List.copyOf(trace), output);
    }

    private long operandValue(Instruction instruction, String register) {
        return register == null ? instruction.value() : registers.get(register);
    }

    private void writeRegister(String register, long value) {
        if (register.equals("AL")) {
            if (value < 0 || value > 0xff) {
                throw new IllegalArgumentException("AL value must fit in one byte");
            }
            registers.put(register, value);
            registers.put("RAX", (registers.get("RAX") & ~0xffL) | value);
            return;
        }
        registers.put(register, value);
        if (register.equals("RAX")) {
            registers.put("AL", value & 0xffL);
        }
    }

    private void loadStringByte(byte[] inputMemory) {
        long sourceIndex = registers.get("RSI");
        if (sourceIndex < 0 || sourceIndex >= inputMemory.length) {
            throw new IllegalArgumentException("LODSB source pointer is outside input memory");
        }
        writeRegister("AL", Byte.toUnsignedInt(inputMemory[Math.toIntExact(sourceIndex)]));
        registers.put("RSI", sourceIndex + 1);
    }

    private void storeStringByte(byte[] outputMemory, int outputCapacity) {
        long destinationIndex = registers.get("RDI");
        if (destinationIndex < 0 || destinationIndex >= outputCapacity) {
            throw new IllegalArgumentException("STOSB destination pointer is outside output memory");
        }
        outputMemory[Math.toIntExact(destinationIndex)] = (byte) registers.get("AL").longValue();
        registers.put("RDI", destinationIndex + 1);
    }

    private String outputString(byte[] outputMemory) {
        if (outputMemory.length == 0) return "";
        int outputLength = Math.toIntExact(registers.get("RDI"));
        if (outputLength == 0 || outputLength > outputMemory.length) {
            throw new IllegalStateException("Simulated string program did not produce a valid output");
        }
        int terminator = outputLength - 1;
        if (outputMemory[terminator] != 0) {
            throw new IllegalStateException("Simulated string output is missing its terminator");
        }
        return new String(outputMemory, 0, terminator, StandardCharsets.US_ASCII);
    }

    private void add(String register, long value) {
        long left = registers.get(register);
        long result = left + value;
        carry = Long.compareUnsigned(result, left) < 0;
        overflow = ((left ^ result) & (value ^ result)) < 0;
        writeRegister(register, result);
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
        writeRegister(register, result);
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
        writeRegister("RAX", quotient);
        registers.put("RDX", remainder);
    }

    private void compare(long left, long right) {
        long result = left - right;
        zero = left == right;
        sign = result < 0;
        carry = Long.compareUnsigned(left, right) < 0;
        overflow = ((left ^ right) & (left ^ result)) < 0;
    }

    private void test(String firstRegister, String secondRegister) {
        long result = registers.get(firstRegister) & registers.get(secondRegister);
        zero = result == 0;
        sign = result < 0;
        carry = false;
        overflow = false;
    }

    private void decrement(String register) {
        long left = registers.get(register);
        long result = left - 1;
        registers.put(register, result);
        zero = result == 0;
        sign = result < 0;
        overflow = left == Long.MIN_VALUE;
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
            case "MOV", "IMUL" -> {
                String[] operands = operandText.split("\\s*,\\s*", 2);
                if (operands.length != 2) throw new IllegalArgumentException("Invalid instruction: " + line);
                yield new Instruction(opcode, register(operands[0]), number(operands[1]), null, null);
            }
            case "ADD", "CMP" -> {
                String[] operands = operandText.split("\\s*,\\s*", 2);
                if (operands.length != 2) throw new IllegalArgumentException("Invalid instruction: " + line);
                String secondRegister = registerOrNull(operands[1]);
                long immediate = secondRegister == null ? number(operands[1]) : 0;
                yield new Instruction(opcode, register(operands[0]), immediate, null, secondRegister);
            }
            case "TEST" -> {
                String[] operands = operandText.split("\\s*,\\s*", 2);
                if (operands.length != 2) throw new IllegalArgumentException("Invalid instruction: " + line);
                yield new Instruction(opcode, register(operands[0]), 0, null, register(operands[1]));
            }
            case "DEC", "IDIV" -> new Instruction(opcode, register(operandText), 0, null, null);
            case "JGE", "JL", "JE", "JZ", "JNZ", "JMP" ->
                    new Instruction(opcode, null, 0, operandText.toUpperCase(Locale.ROOT), null);
            case "CQO", "HLT", "LODSB", "STOSB" -> new Instruction(opcode, null, 0, null, null);
            default -> throw new IllegalArgumentException("Unsupported simulated instruction: " + opcode);
        };
    }

    private String registerOrNull(String value) {
        String candidate = value.toUpperCase(Locale.ROOT);
        return registers.containsKey(candidate) ? candidate : null;
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

    private record Instruction(String opcode, String register, long value, String label, String secondRegister) {
        String display() {
            if (register == null) return label == null ? opcode : opcode + " " + label;
            if (opcode.equals("IDIV") || opcode.equals("DEC")) return opcode + " " + register;
            if (secondRegister != null) return opcode + " " + register + ", " + secondRegister;
            return opcode + " " + register + ", " + value;
        }
    }

    record Result(
            Map<String, Long> registers,
            CoaSimulationResponse.CpuFlags flags,
            List<CoaSimulationResponse.TraceStep> trace,
            String output
    ) {
    }
}
