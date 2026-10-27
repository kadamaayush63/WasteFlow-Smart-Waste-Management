# Computer Organization and Architecture Tasks 1, 2, and 3

## Project context

WasteFlow includes a COA Lab that demonstrates processor-level execution using real application data. It can trace a bin fill-status calculation and route distance/time totals.

> **Implementation note:** The COA Lab is an educational Java simulator for a limited x86-64-style instruction set. It models 64-bit registers and instruction behavior, but does not assemble or execute native x86-64 machine code.

## Task 1: Processor-level model of an application

| Concept | Status | Implementation | Source location |
|---|---|---|---|
| Identify application data and operations | Implemented | Bin fill/capacity and route distance/time are used as simulator inputs. | [CoaSimulationService.java](backend/src/main/java/com/wasteflow/coa/CoaSimulationService.java#L20), [CoaSimulationService.java](backend/src/main/java/com/wasteflow/coa/CoaSimulationService.java#L71) |
| Map application operations to processor components | Implemented in simulator | Bin status uses multiply, divide, comparisons, and conditional branches; route totals use addition. | [CoaSimulationService.java](backend/src/main/java/com/wasteflow/coa/CoaSimulationService.java#L29), [CoaSimulationService.java](backend/src/main/java/com/wasteflow/coa/CoaSimulationService.java#L75) |
| Register model | Implemented in simulator | The simulated CPU has 64-bit `RAX`, `RBX`, `RCX`, and `RDX` registers. | [X64InstructionSimulator.java](backend/src/main/java/com/wasteflow/coa/X64InstructionSimulator.java#L23) |
| ALU operations | Implemented in simulator | `ADD`, `IMUL`, `IDIV`, and `CMP` are interpreted by the CPU simulator. | [X64InstructionSimulator.java](backend/src/main/java/com/wasteflow/coa/X64InstructionSimulator.java#L49), [X64InstructionSimulator.java](backend/src/main/java/com/wasteflow/coa/X64InstructionSimulator.java#L80) |
| Status flags | Implemented in simulator | Zero, sign, carry, and overflow flags are tracked and returned. | [X64InstructionSimulator.java](backend/src/main/java/com/wasteflow/coa/X64InstructionSimulator.java#L80), [CoaSimulationResponse.java](backend/src/main/java/com/wasteflow/coa/CoaSimulationResponse.java#L14) |
| Control unit / instruction flow | Implemented in simulator | Program counter steps through instructions, resolves labels/branches, and stops on `HLT`. | [X64InstructionSimulator.java](backend/src/main/java/com/wasteflow/coa/X64InstructionSimulator.java#L42), [X64InstructionSimulator.java](backend/src/main/java/com/wasteflow/coa/X64InstructionSimulator.java#L55) |
| CPU execution trace | Implemented in simulator | Each executed instruction records register and flag state. | [X64InstructionSimulator.java](backend/src/main/java/com/wasteflow/coa/X64InstructionSimulator.java#L65) |
| Simulator interface | Implemented | COA Lab UI runs simulations and displays the result, registers, flags, instruction listing, and trace. | [CoaLab.tsx](frontend/src/CoaLab.tsx#L50), [CoaLab.tsx](frontend/src/CoaLab.tsx#L90) |

### Task 1 workflow

1. Select a WasteFlow bin or use a loaded route.
2. Submit its fill/capacity or distance/time values to the COA API.
3. Convert measurements to integer milli-units for the simulated registers.
4. Interpret the instruction program and update registers/flags.
5. Display the result and per-instruction state in the COA Lab.

## Task 2: 64-bit assembly-based application processing

| Concept | Status | Implementation | Source location |
|---|---|---|---|
| Identify numerical application operations | Implemented | Bin fill-status and route distance/time calculations are selected. | [CoaSimulationService.java](backend/src/main/java/com/wasteflow/coa/CoaSimulationService.java#L20), [CoaSimulationService.java](backend/src/main/java/com/wasteflow/coa/CoaSimulationService.java#L71) |
| First numerical operation: bin status | Implemented in simulator | `MOV`, `IMUL`, `CQO`, `IDIV`, `CMP`, and conditional branches classify the selected bin. | [CoaSimulationService.java](backend/src/main/java/com/wasteflow/coa/CoaSimulationService.java#L29) |
| Second numerical operation: route totals | Implemented in simulator | `ADD` instructions accumulate route distance and travel time in `RAX` and `RBX`. | [CoaSimulationService.java](backend/src/main/java/com/wasteflow/coa/CoaSimulationService.java#L75), [CoaSimulationService.java](backend/src/main/java/com/wasteflow/coa/CoaSimulationService.java#L81) |
| Connect operations to application data | Implemented | The frontend sends a selected bin's live values or the loaded route totals to the backend. | [CoaLab.tsx](frontend/src/CoaLab.tsx#L25), [CoaLab.tsx](frontend/src/CoaLab.tsx#L38) |
| REST endpoints | Implemented | Bin-status and route-total simulation endpoints accept validated input. | [CoaSimulationController.java](backend/src/main/java/com/wasteflow/coa/CoaSimulationController.java#L21), [CoaSimulationController.java](backend/src/main/java/com/wasteflow/coa/CoaSimulationController.java#L26) |
| Native 64-bit assembly source assembled/executed | Not implemented | The instruction text is interpreted by Java; it is not assembled into native machine code. | [X64InstructionSimulator.java](backend/src/main/java/com/wasteflow/coa/X64InstructionSimulator.java#L10) |

### Supported simulated instruction subset

The teaching simulator interprets `MOV`, `ADD`, `IMUL`, `CQO`, `IDIV`, `CMP`, `JGE`, `JL`, `JMP`, and `HLT`. The instruction dispatch and control flow are in [X64InstructionSimulator.java](backend/src/main/java/com/wasteflow/coa/X64InstructionSimulator.java#L42).

## Task 3 (CO3): Arithmetic algorithm and string/data processing

### Concepts and locations

| COA Task 3 concept | How it is used | Source location |
|---|---|---|
| Successive addition | The simulator adds the loaded route distance once per run using a 64-bit `ADD`/`DEC`/`CMP`/`JNZ` loop to estimate total mileage. | [CoaSimulationService.java](backend/src/main/java/com/wasteflow/coa/CoaSimulationService.java), [X64InstructionSimulator.java](backend/src/main/java/com/wasteflow/coa/X64InstructionSimulator.java) |
| Route workload input | The operator selects a repetition count from 1 to 100; the loaded route distance and count are sent to the backend. | [CoaLab.tsx](frontend/src/CoaLab.tsx), [CoaSimulationController.java](backend/src/main/java/com/wasteflow/coa/CoaSimulationController.java) |
| Route bin-code string processing | Bin codes from route stops are processed in planned order as null-terminated input data; simulated `LODSB`/`STOSB` instructions copy codes and add separators to build the route summary. | [CoaSimulationService.java](backend/src/main/java/com/wasteflow/coa/CoaSimulationService.java), [X64InstructionSimulator.java](backend/src/main/java/com/wasteflow/coa/X64InstructionSimulator.java) |
| CO3 controls and execution trace | The COA Lab runs both operations and displays the instruction listing, register values, flags, and instruction-by-instruction trace. | [CoaLab.tsx](frontend/src/CoaLab.tsx) |
| Native 64-bit assembly execution | CO3 instructions are interpreted by the Java x86-64-style teaching simulator; they are not assembled into or executed as native machine code. | [X64InstructionSimulator.java](backend/src/main/java/com/wasteflow/coa/X64InstructionSimulator.java) |

CO3 API endpoints:

- `POST /api/coa/route-workload` accepts `{"distanceKm": 3.25, "repetitionCount": 4}` and returns the repeated-route mileage calculation and CPU trace.
- `POST /api/coa/route-summary` accepts `{"binCodes": ["BIN-01", "BIN-02"]}` and returns the route summary assembled by the simulated string instructions.

The supported simulated instruction subset now also includes `DEC`, `JNZ`, `JE`/`JZ`, `TEST`, `LODSB`, and `STOSB` for loops and byte-oriented string processing.

### API examples

Bin-status simulation:

```http
POST /api/coa/bin-status
Content-Type: application/json

{"currentFillKg": 95, "capacityKg": 100}
```

Route totals simulation:

```http
POST /api/coa/route-totals
Content-Type: application/json

{"legs":[{"distanceKm":3.25,"travelTimeMinutes":4.5},{"distanceKm":2.75,"travelTimeMinutes":5.5}]}
```

## Tests

The COA simulation tests cover bin thresholds, invalid fill, route distance/time accumulation, repeated-route mileage using successive addition, route-summary string processing, and invalid string/workload inputs. See [CoaSimulationServiceTest.java](backend/src/test/java/com/wasteflow/coa/CoaSimulationServiceTest.java#L20).

## Summary

Task 1 processor-model concepts—registers, ALU operations, flags, control flow, and execution trace—are implemented in an educational simulator and exposed in the application UI. Task 2's two WasteFlow calculations and CO3's repeated-route arithmetic and bin-code string processing are connected to simulated x86-64-style programs. **Native 64-bit Assembly compilation/execution is not implemented**, so any assignment requirement specifically requiring actual assembly source or machine-code execution remains outstanding.
