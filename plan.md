# WasteFlow Full-Stack Implementation Plan

## 1. Project Goal

WasteFlow is an academic, algorithm-driven waste collection platform. It records bins, locations, roads, trucks, collection requests, routes, and completed trips. It simulates changing fill levels, prioritizes urgent bins, calculates routes on a weighted graph, animates a truck on a map, and produces operational and algorithm-performance reports.

The system should demonstrate both a useful application and the required academic concepts:

- Java object-oriented design and clean package boundaries.
- Custom data structures and searching/sorting implementations.
- Weighted graphs, Dijkstra shortest paths, and greedy route construction.
- Relational database design and transaction handling.
- Computer graphics primitives, transformations, clipping, and animation.
- A small COA ALU/register/instruction simulator.
- Complexity analysis and measurable algorithm comparisons.

The core product must work without IoT hardware or real GPS data. Sensor readings, travel times, and truck movement are simulated using deterministic data so the demonstration is repeatable.

## 2. Recommended Architecture

Use a modular monolith first. It is easier to demonstrate, test, deploy, and explain than microservices, while still having clear boundaries that could be separated later.

```text
React + TypeScript web client
        |
        | HTTPS/JSON REST API
        v
Spring Boot Java application
  - authentication and authorization
  - bin, truck, request, route, report services
  - custom data structures and algorithms
  - simulation and animation event APIs
        |
        | JDBC / Flyway migrations
        v
MySQL database
```

### 2.1 Frontend

- React with TypeScript and Vite.
- React Router for application screens.
- TanStack Query for server state, caching, loading, and mutation handling.
- A chart library such as Apache ECharts or Recharts for reports.
- HTML Canvas for the 2D map, route lines, bins, buildings, and truck animation.
- WebSocket or Server-Sent Events for live simulation updates; polling is an acceptable first milestone.
- A small design system of reusable tables, forms, status badges, dialogs, and chart panels.

The frontend should display the results of the Java algorithms, not reimplement the route optimizer. It may perform presentation-only calculations such as map scaling and animation interpolation.

### 2.2 Backend

- Java 21 LTS.
- Spring Boot 3.x.
- Spring Web for REST endpoints.
- Spring Security for login and role-based access.
- Spring JDBC or jOOQ for explicit SQL and predictable database behavior. Use JDBC directly for the academic implementation if the course requires JDBC evidence.
- Flyway for versioned database migrations.
- Bean Validation for request validation.
- Jackson for JSON serialization.
- SLF4J/Logback for structured application logs.

Keep the algorithm package independent of Spring and the database. This lets the algorithms be tested with in-memory objects and demonstrated separately in the viva.

### 2.3 Persistence

Use MySQL in development and production-like demonstrations. Store all operational state needed to reproduce a route and all algorithm metrics needed for reports.

CSV import/export is an auxiliary feature, not the primary source of truth. Imported data must pass the same validation rules as API-created data.

### 2.4 Desktop Compatibility Option

If the department explicitly requires Java Swing, retain the backend and algorithm modules and add a Swing client that calls the same REST API. Do not duplicate domain or route logic in Swing. The web client is the recommended primary UI because it provides a clearer full-stack demonstration.

## 3. Repository Structure

```text
waste-flow/
├── README.md
├── plan.md
├── .env.example
├── docker-compose.yml
├── pom.xml                         # Maven multi-module parent
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/wasteflow/
│       │   ├── WasteFlowApplication.java
│       │   ├── config/
│       │   ├── auth/
│       │   │   ├── AuthController.java
│       │   │   ├── AuthService.java
│       │   │   └── SecurityConfig.java
│       │   ├── bin/
│       │   │   ├── BinController.java
│       │   │   ├── BinService.java
│       │   │   ├── BinRepository.java
│       │   │   ├── BinDto.java
│       │   │   └── BinMapper.java
│       │   ├── truck/
│       │   ├── location/
│       │   ├── road/
│       │   ├── collection/
│       │   ├── route/
│       │   ├── report/
│       │   ├── simulation/
│       │   ├── coa/
│       │   └── common/
│       │       ├── ApiError.java
│       │       ├── GlobalExceptionHandler.java
│       │       └── PageResponse.java
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/
│       │       ├── V1__initial_schema.sql
│       │       └── V2__seed_demo_data.sql
│       └── test/java/com/wasteflow/
│           ├── algorithm/
│           ├── route/
│           ├── collection/
│           └── api/
├── algorithms/
│   ├── pom.xml
│   └── src/main/java/com/wasteflow/algorithms/
│       ├── datastructures/
│       │   ├── CustomHashTable.java
│       │   ├── CustomPriorityQueue.java
│       │   ├── CustomQueue.java
│       │   ├── CustomDeque.java
│       │   ├── SinglyLinkedList.java
│       │   ├── DoublyLinkedList.java
│       │   ├── CircularQueue.java
│       │   └── Stack.java
│       ├── search/
│       │   ├── LinearSearch.java
│       │   └── BinarySearch.java
│       ├── sort/
│       │   ├── MergeSort.java
│       │   ├── QuickSort.java
│       │   └── SortMetrics.java
│       ├── graph/
│       │   ├── WeightedGraph.java
│       │   ├── GraphNode.java
│       │   ├── GraphEdge.java
│       │   └── Dijkstra.java
│       ├── routing/
│       │   ├── RouteOptimizer.java
│       │   ├── DijkstraRouteOptimizer.java
│       │   ├── GreedyRouteOptimizer.java
│       │   ├── RouteCandidate.java
│       │   └── RouteMetrics.java
│       └── complexity/
│           └── BenchmarkRunner.java
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── src/
│   │   ├── app/
│   │   │   ├── App.tsx
│   │   │   ├── router.tsx
│   │   │   └── queryClient.ts
│   │   ├── api/
│   │   │   ├── client.ts
│   │   │   ├── bins.ts
│   │   │   ├── routes.ts
│   │   │   └── reports.ts
│   │   ├── components/
│   │   │   ├── DataTable.tsx
│   │   │   ├── StatusBadge.tsx
│   │   │   ├── MetricCard.tsx
│   │   │   └── FormField.tsx
│   │   ├── features/
│   │   │   ├── dashboard/
│   │   │   ├── bins/
│   │   │   ├── trucks/
│   │   │   ├── requests/
│   │   │   ├── routes/
│   │   │   ├── map/
│   │   │   ├── reports/
│   │   │   └── coa/
│   │   ├── hooks/
│   │   ├── types/
│   │   ├── styles/
│   │   └── main.tsx
│   └── public/
├── docs/
│   ├── architecture.md
│   ├── api.md
│   ├── algorithms.md
│   ├── database.md
│   ├── viva-demo-script.md
│   └── screenshots/
└── scripts/
    ├── seed-demo-data.sql
    └── run-benchmark.ps1
```

## 4. Domain Model

### Core entities

- `User`: username, password hash, role, active status.
- `Location`: name, latitude/longitude or demo x/y coordinates.
- `Road`: source location, destination location, distance, travel time, direction.
- `WasteBin`: location, capacity, current fill, waste type, status, last serviced time.
- `Truck`: capacity, current load, status, current location.
- `CollectionRequest`: bin, urgency, priority score, creation time, status.
- `Route`: truck, algorithm name, ordered stops, total distance, estimated time, status.
- `RouteStop`: sequence, bin, arrival time, collected quantity, completion status.
- `CollectionEvent`: bin, truck, collected quantity, timestamp, operator.
- `AlgorithmRun`: algorithm, input size, execution time, distance, route score, operation count.

Use enums for stable states such as `BinStatus`, `TruckStatus`, `RequestStatus`, `RouteStatus`, and `UserRole`. Validate capacity and fill values with `0 <= currentFill <= capacity`.

## 5. Database Design

Tables:

- `users(id, username, password_hash, role, active, created_at)`
- `locations(id, name, x_coordinate, y_coordinate, latitude, longitude)`
- `roads(id, source_location_id, destination_location_id, distance, travel_time, bidirectional)`
- `bins(id, bin_code, location_id, capacity, current_fill, waste_type, status, last_collected_at, version)`
- `trucks(id, truck_code, capacity, current_load, status, location_id)`
- `collection_requests(id, bin_id, priority_score, urgency, status, created_at, resolved_at)`
- `routes(id, truck_id, algorithm, status, total_distance, total_time, created_at, started_at, completed_at)`
- `route_stops(id, route_id, sequence_no, bin_id, arrival_time, collected_quantity, status)`
- `collection_events(id, route_id, truck_id, bin_id, quantity, collected_at)`
- `algorithm_runs(id, algorithm, input_size, execution_time_ms, distance, operation_count, created_at)`

Important constraints and indexes:

- Unique indexes on `users.username`, `bins.bin_code`, and `trucks.truck_code`.
- Foreign keys on all entity relationships.
- Check constraints for non-negative capacity, fill, distance, and travel time.
- Indexes on bin status, request status/priority, truck status, and route creation date.
- Optimistic locking through `bins.version` to prevent simultaneous collection updates from overwriting one another.

## 6. REST API Contract

All endpoints return JSON and use consistent error responses with `code`, `message`, `fieldErrors`, and `timestamp`.

### Authentication

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`

### Dashboard

- `GET /api/dashboard/summary`
- `GET /api/dashboard/critical-bins`
- `GET /api/dashboard/active-routes`

### Bins

- `GET /api/bins?status=&wasteType=&page=&size=&sort=`
- `POST /api/bins`
- `GET /api/bins/{id}`
- `PUT /api/bins/{id}`
- `DELETE /api/bins/{id}`
- `POST /api/bins/import`
- `GET /api/bins/export`

### Trucks

- `GET /api/trucks`
- `POST /api/trucks`
- `PUT /api/trucks/{id}`
- `PATCH /api/trucks/{id}/status`

### Monitoring and requests

- `POST /api/simulation/tick`
- `POST /api/simulation/start`
- `POST /api/simulation/stop`
- `GET /api/collection-requests`
- `POST /api/collection-requests/rebuild-priority`
- `PATCH /api/collection-requests/{id}/status`

### Routes

- `POST /api/routes/optimize`
- `GET /api/routes/{id}`
- `POST /api/routes/{id}/start`
- `POST /api/routes/{id}/stops/{stopId}/collect`
- `POST /api/routes/{id}/complete`
- `GET /api/routes/{id}/events`
- `POST /api/routes/compare`

### Reports and academic demonstrations

- `GET /api/reports/collections`
- `GET /api/reports/routes`
- `GET /api/reports/algorithm-performance`
- `POST /api/coa/execute`
- `GET /api/academic/data-structure-demo`

## 7. Algorithm and Data-Structure Design

### Priority classification

Calculate a normalized fill ratio:

```text
fillRatio = currentFill / capacity
```

Suggested thresholds:

- `0.00 - 0.49`: NORMAL
- `0.50 - 0.74`: MEDIUM
- `0.75 - 0.94`: HIGH
- `0.95 - 1.00`: CRITICAL

Priority should combine urgency, fill ratio, age of request, and optional distance. Use a documented formula, for example:

```text
priorityScore = urgencyWeight + (fillRatio * 100) + waitingMinutesWeight
```

The exact weights must be configurable and recorded in the route report.

### Graph and routing

- Build an adjacency-list weighted graph from locations and roads.
- Use Dijkstra for shortest paths between the depot and each required stop.
- Use a greedy optimizer to select the next feasible stop by priority and travel cost.
- Reject routes that exceed truck capacity; select another truck or split requests.
- For small demonstrations, compare the greedy result with a brute-force permutation search.
- Return route stops, path segments, total distance, estimated time, and operation counts.
- Store the algorithm name and input snapshot with each generated route so results are reproducible.

The route service should depend on a `RouteOptimizer` interface. This demonstrates polymorphism and allows Dijkstra, greedy, and future strategies to be compared using the same input.

### Required academic demonstrations

- Custom hash table with chaining or linear probing for bin-code lookup.
- Priority queue for urgent collection requests.
- Queue for normal requests and circular queue for truck rotation.
- Deque for manual request adjustment.
- Linked list for route stops/history navigation.
- Stack for undoing recent administrative operations or path reconstruction.
- Linear and binary search over bin records.
- Merge sort and quicksort with comparison counters.
- Adjacency list and optional adjacency matrix comparison.
- Big-O notes and actual benchmark measurements for each major algorithm.

Use Java Collections in production services where appropriate, but keep custom implementations available in `algorithms` for explicit syllabus demonstrations.

## 8. Graphics and Animation Plan

The map screen should contain:

- Roads rendered as line segments.
- Locations/junctions rendered as nodes.
- Bins rendered with color by priority.
- Buildings or service areas rendered as simple polygons.
- The selected route rendered above the road network.
- A truck marker translated along route coordinates.
- A side panel showing current stop, collected quantity, distance, and progress.

Frontend Canvas responsibilities:

- Convert world coordinates to viewport coordinates.
- Apply pan, zoom, translation, rotation, and clipping.
- Draw lines using Canvas primitives; expose a separate academic demo for DDA/Bresenham output.
- Interpolate truck position between route points with `requestAnimationFrame`.
- Use a Bezier interpolation option for visual smoothing only; route distance remains based on graph edges.

The backend remains authoritative for collection events and route status. The frontend animation must resume from server state after refresh.

## 9. COA Module

Implement a self-contained simulator with:

- Registers such as `AX`, `BX`, `CX`, and `FLAGS`.
- Instructions: `MOV`, `ADD`, `SUB`, `MUL`, `CMP`, and `HALT`.
- Fetch, decode, execute steps.
- ALU operations and comparison flags.
- Decimal and hexadecimal display.
- Operation count and execution trace.

Demonstrate a real application connection by using the ALU to calculate a small route metric such as total distance or collected quantity, while keeping the simulator educational and isolated from core route correctness.

## 10. Security and Reliability

- Hash passwords with BCrypt or Argon2; never store plain text passwords.
- Protect write endpoints with roles: `ADMIN` for configuration, `OPERATOR` for collection operations, `VIEWER` for reports.
- Validate all request payloads on the server.
- Use parameterized SQL only.
- Configure CORS for the known frontend origin.
- Do not expose database credentials in source control; use environment variables.
- Wrap collection completion in a transaction that updates the bin, truck load, route stop, collection event, and request status together.
- Make simulation and route-generation operations idempotent where practical.
- Log route generation failures and invalid state transitions with correlation IDs.

## 11. Screen Plan

1. Login screen.
2. Dashboard with critical bins, truck availability, active requests, and summary charts.
3. Bin management with CRUD, filters, search, sort, import, and export.
4. Truck management with capacity, status, and location.
5. Collection queue showing priority order and request state.
6. Map and route planner with algorithm selector and route comparison.
7. Live collection animation with stop completion controls.
8. Reports with collection history, route efficiency, distance, time, and waste totals.
9. Data-structure and algorithm demonstration screen.
10. COA instruction/ALU simulator.

Every screen needs loading, empty, validation-error, server-error, and permission-denied states.

## 12. Delivery Phases

### Phase 1: Project foundation

- Create Maven modules and frontend Vite app.
- Configure MySQL through Docker Compose.
- Add Flyway migration and demo seed data.
- Add health endpoint and basic frontend shell.
- Write README setup instructions.

### Phase 2: Domain and CRUD

- Implement entities, DTOs, repositories, validation, and services.
- Complete bin, location, road, and truck CRUD.
- Add pagination, filtering, search, sorting, and CSV import/export.
- Add API integration tests for the CRUD workflow.

### Phase 3: Monitoring and prioritization

- Implement simulated fill-level ticks.
- Generate requests when thresholds are crossed.
- Implement priority scoring and queue management.
- Add dashboard summary endpoints and UI.

### Phase 4: Algorithms and route optimization

- Implement graph model and Dijkstra.
- Implement greedy route construction and truck-capacity checks.
- Add route comparison and operation metrics.
- Persist route snapshots, stops, and algorithm runs.
- Add unit tests with known graph fixtures.

### Phase 5: Map and live collection

- Implement Canvas map rendering.
- Draw roads, bins, stops, and route lines.
- Add truck animation and collection event workflow.
- Add refresh/reconnect behavior for active routes.

### Phase 6: Reports and academic demonstrations

- Add report queries and charts.
- Add custom structure demonstrations, searching/sorting comparison, and complexity metrics.
- Implement the COA simulator.
- Prepare a deterministic viva dataset and scripted demonstration.

### Phase 7: Hardening and optional extensions

- Add role-based security, audit logging, and error handling.
- Add performance benchmarks and test coverage review.
- Add optional WebSocket updates, 3D mode, real GPS coordinates, or multi-truck optimization only after the core workflow is stable.

## 13. Testing Strategy

### Backend unit tests

- Fill-level classification at every threshold and boundary.
- Priority score ordering and tie-breaking.
- Hash-table collision handling.
- Queue, deque, linked-list, stack, and circular-queue invariants.
- Dijkstra expected distances and unreachable nodes.
- Greedy route capacity handling.
- COA instruction results and flags.

### Backend integration tests

- CRUD and validation against a test database.
- Route generation with seeded bins, roads, and trucks.
- Transactional collection completion.
- Authorization for each role.
- CSV import rejection of malformed rows.

### Frontend tests

- Form validation and mutation states.
- Queue ordering display.
- Route comparison rendering.
- Map drawing with a fixed fixture.
- Animation pause, resume, completion, and refresh behavior.

### End-to-end tests

Use Playwright for the primary flow:

```text
login -> create/update bin -> simulate fill increase -> approve request
-> optimize route -> view route -> collect stop -> complete route -> view report
```

### Performance tests

Measure algorithm execution time and operation counts for increasing input sizes. Record results in `algorithm_runs` and include a report table showing expected versus observed complexity.

## 14. Definition of Done

The core project is complete when:

- A user can log in and view a dashboard.
- Bins and trucks can be created, updated, searched, sorted, and persisted.
- Simulation can create collection requests from fill-level changes.
- Critical requests are visibly ordered by the priority queue.
- A graph-backed route can be generated for an available truck.
- The UI shows the route and animates the truck.
- Completing a stop updates bin, truck, request, route, and history data consistently.
- Reports show distance, time, bins collected, waste collected, and algorithm used.
- At least one custom data structure, search, sort, Dijkstra, greedy strategy, graphics technique, and COA operation can be demonstrated live.
- Automated tests cover algorithm correctness and the main API workflow.
- The README can take a new developer from clone to demo using documented commands.

## 15. Suggested Commands

Backend:

```powershell
mvn clean test
mvn -pl backend spring-boot:run
```

Frontend:

```powershell
npm install
npm run dev
```

Database:

```powershell
docker compose up -d mysql
```

The exact commands should be finalized in `README.md` after the repository is scaffolded. Keep a single demo account and deterministic seed dataset for assessment, while documenting that credentials are development-only.

## 16. Academic Demonstration Order

1. Explain the entity model and OOP boundaries.
2. Create or inspect bins and show validation.
3. Increase a bin's simulated fill level and show automatic request creation.
4. Explain the priority queue and show the ordering.
5. Display the road graph and run Dijkstra.
6. Compare Dijkstra-based and greedy route results.
7. Show capacity constraints and truck assignment.
8. Animate the truck and complete a collection stop.
9. Show updated records and performance reports.
10. Open the custom data-structure and searching/sorting demonstrations.
11. Execute an ALU/instruction trace tied to a route metric.
12. Explain time and space complexity, limitations, and future extensions.

## 17. Scope Control

The must-have release is the web dashboard, MySQL persistence, bin/truck CRUD, simulated monitoring, priority queue, graph routing, Dijkstra, greedy optimization, Canvas map, route completion, reports, tests, and the COA mini-module.

Treat 3D rendering, texture mapping, live GPS, IoT integration, machine learning, citizen accounts, mobile clients, and full multi-vehicle optimization as optional extensions. They should not delay the demonstrable end-to-end workflow.
