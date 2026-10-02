# PSOOP Java Tasks 1 and 2 Implementation

## Project context

WasteFlow is a Java Spring Boot and React application for monitoring waste-bin fill levels, managing collection requests, and planning truck routes. This document maps the PSOOP Java Task 1 and Task 2 concepts shown in the assignment sheet to the current implementation.

## Task 1: Java fundamentals

| Concept | Status | How it is used | Source location |
|---|---|---|---|
| Real-world case study | Used | Waste bins, collection requests, trucks, and route planning form the case study. | [WasteBin.java](backend/src/main/java/com/wasteflow/bin/WasteBin.java#L17), [RouteService.java](backend/src/main/java/com/wasteflow/route/RouteService.java#L47) |
| Control statements | Used | Conditions validate bin fill, truck availability/capacity, route state, and search input. | [WasteBin.java](backend/src/main/java/com/wasteflow/bin/WasteBin.java#L75), [RouteService.java](backend/src/main/java/com/wasteflow/route/RouteService.java#L50) |
| Loops | Used | Loops search bins and process selected requests, roads, and route stops. | [WasteBinService.java](backend/src/main/java/com/wasteflow/bin/WasteBinService.java#L25), [RouteService.java](backend/src/main/java/com/wasteflow/route/RouteService.java#L64) |
| Array | Used | Repository results are converted into a `WasteBinResponse[]`; the array is traversed for linear search and returned for listing. | [WasteBinService.java](backend/src/main/java/com/wasteflow/bin/WasteBinService.java#L26) |
| `StringBuilder` | Used | Builds a readable route report containing truck, ordered bin stops, distance, and time. | [RouteResponse.java](backend/src/main/java/com/wasteflow/route/dto/RouteResponse.java#L19) |
| `StringBuffer` | Not used | No `StringBuffer` use exists in the application Java source. The route summary is local to a request, so a synchronized shared buffer is not needed. | — |
| Objects and classes | Used | Domain classes represent bins, trucks, routes, and other application objects. | [WasteBin.java](backend/src/main/java/com/wasteflow/bin/WasteBin.java#L17), [Truck.java](backend/src/main/java/com/wasteflow/truck/Truck.java#L16) |
| Methods | Used | Domain methods perform operations such as updating/classifying a bin and assigning a truck. | [WasteBin.java](backend/src/main/java/com/wasteflow/bin/WasteBin.java#L67), [Truck.java](backend/src/main/java/com/wasteflow/truck/Truck.java#L49) |

### Array and CRUD distinction

Bin listing and linear search use an array in memory. Create, update, and delete are application features, but their persistent storage is the database through Spring Data JPA rather than the array.

- Search/list: [WasteBinService.java](backend/src/main/java/com/wasteflow/bin/WasteBinService.java#L26)
- Create: [WasteBinService.java](backend/src/main/java/com/wasteflow/bin/WasteBinService.java#L51)
- Update: [WasteBinService.java](backend/src/main/java/com/wasteflow/bin/WasteBinService.java#L63)
- Delete: [WasteBinService.java](backend/src/main/java/com/wasteflow/bin/WasteBinService.java#L72)

## Task 2: OOP, Java features, and programming pattern

| Concept | Status | How it is used | Source location |
|---|---|---|---|
| Lambda expressions | Used | Lambdas transform/select route strategies and process requests; the nearest-stop strategy compares candidate distances. | [RouteService.java](backend/src/main/java/com/wasteflow/route/RouteService.java#L44), [NearestStopRouteStrategy.java](backend/src/main/java/com/wasteflow/route/NearestStopRouteStrategy.java#L36) |
| Interface | Used | Defines the contract shared by route-ordering algorithms. | [RouteRequestOrderingStrategy.java](backend/src/main/java/com/wasteflow/route/RouteRequestOrderingStrategy.java#L8) |
| Abstract class | Used | Provides shared path-finder state and priority-ordering behavior for route strategies. | [AbstractRouteRequestOrderingStrategy.java](backend/src/main/java/com/wasteflow/route/AbstractRouteRequestOrderingStrategy.java#L10) |
| Inheritance | Used | Priority-first and nearest-stop strategy classes extend the shared abstract strategy. | [PriorityFirstRouteStrategy.java](backend/src/main/java/com/wasteflow/route/PriorityFirstRouteStrategy.java#L10), [NearestStopRouteStrategy.java](backend/src/main/java/com/wasteflow/route/NearestStopRouteStrategy.java#L11) |
| Polymorphism | Used | `RouteService` looks up a strategy by algorithm name and calls its interface method; the selected implementation determines request order. | [RouteService.java](backend/src/main/java/com/wasteflow/route/RouteService.java#L50), [RouteService.java](backend/src/main/java/com/wasteflow/route/RouteService.java#L77) |
| `static` keyword | Used | The application entry point and response factory methods are static. | [BackendApplication.java](backend/src/main/java/com/wasteflow/BackendApplication.java#L9), [RouteResponse.java](backend/src/main/java/com/wasteflow/route/dto/RouteResponse.java#L19) |
| `final` keyword | Used | Service dependencies are final fields; the custom linked-list class is final. | [RouteService.java](backend/src/main/java/com/wasteflow/route/RouteService.java#L28), [SinglyLinkedList.java](backend/src/main/java/com/wasteflow/route/SinglyLinkedList.java#L10) |
| `super` keyword | Used | Each concrete route-strategy constructor initializes the shared superclass with `super(...)`. | [PriorityFirstRouteStrategy.java](backend/src/main/java/com/wasteflow/route/PriorityFirstRouteStrategy.java#L14), [NearestStopRouteStrategy.java](backend/src/main/java/com/wasteflow/route/NearestStopRouteStrategy.java#L15) |
| Exception handling | Used | Business rules throw argument/state exceptions; a global handler converts them into HTTP error responses. | [RouteService.java](backend/src/main/java/com/wasteflow/route/RouteService.java#L51), [GlobalExceptionHandler.java](backend/src/main/java/com/wasteflow/common/GlobalExceptionHandler.java#L13) |
| Java packages | Used | Code is organized into packages such as `bin`, `route`, `truck`, and `simulation`. | [WasteBin.java](backend/src/main/java/com/wasteflow/bin/WasteBin.java#L1), [RouteService.java](backend/src/main/java/com/wasteflow/route/RouteService.java#L1) |
| Java collections | Used | `List`, `Map`, `Optional`, and collection operations manage requests, graph data, and strategies. | [RouteService.java](backend/src/main/java/com/wasteflow/route/RouteService.java#L33), [WeightedGraph.java](backend/src/main/java/com/wasteflow/route/algorithm/WeightedGraph.java#L10) |
| Java generics | Used | The custom linked list is generic (`SinglyLinkedList<T>`) and is used for collection requests and route stops. | [SinglyLinkedList.java](backend/src/main/java/com/wasteflow/route/SinglyLinkedList.java#L10), [RouteService.java](backend/src/main/java/com/wasteflow/route/RouteService.java#L59) |
| Java Swing | Not used | The UI is implemented in React and TypeScript, not Java Swing. | [App.tsx](frontend/src/App.tsx) |
| File handling | Not used in the application workflow | No Java file read/write feature is implemented. Flyway migration resources are database setup files, not application file-handling functionality. | — |
| Programming pattern | Used | The Strategy Pattern provides selectable priority-first and nearest-stop route ordering. | [RouteRequestOrderingStrategy.java](backend/src/main/java/com/wasteflow/route/RouteRequestOrderingStrategy.java#L8), [RouteService.java](backend/src/main/java/com/wasteflow/route/RouteService.java#L50) |

## Summary

Task 1 uses control statements, loops, arrays, `StringBuilder`, objects, and methods. `StringBuffer` is not used. Task 2 uses lambdas, an interface, an abstract class, inheritance, polymorphism, `static`, `final`, `super`, exception handling, packages, collections, generics, and the Strategy Pattern. Java Swing and application-level file handling are not used.
