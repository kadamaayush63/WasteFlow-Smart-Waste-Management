# WasteFlow Project Explanation

## 1. Project Overview

WasteFlow is an intelligent waste collection management and route optimization system. It solves the problem of sending collection trucks to the bins that need attention instead of following a fixed route regardless of bin condition.

The application combines:

- A Java Spring Boot backend.
- A MySQL relational database managed through Flyway migrations.
- A React and TypeScript frontend built with Vite.
- Waste-bin fill simulation.
- Collection-request prioritization.
- A weighted road graph and Dijkstra shortest-path algorithm.
- Truck capacity and status management.
- A 2D Canvas map with animated truck movement.
- A COA teaching simulator that traces 64-bit register and ALU operations for bin and route calculations.

The current implementation is the functional MVP described in `mvp.md`. `doc.md` describes a larger academic system, so this file clearly separates implemented features from future extensions.

## 2. Problem Being Solved

Traditional waste collection often uses fixed routes. That can cause:

- Trucks to visit bins that are not yet full.
- Critical bins to wait too long.
- Unnecessary travel distance and time.
- Poor use of truck capacity.

WasteFlow improves this process by monitoring bin fill levels, generating collection requests for urgent bins, checking truck availability and capacity, and calculating a route through a connected road network.

## 3. System Architecture

```text
React + TypeScript + HTML Canvas frontend
                    |
                    | JSON REST API
                    v
Spring Boot Java backend
  - domain entities
  - REST controllers
  - services and validation
  - simulation
  - route optimization
  - Dijkstra algorithm
                    |
                    | JPA / Hibernate + Flyway
                    v
MySQL database
```

The project uses a modular monolith. The backend is one Spring Boot application, but the code is separated into domain packages such as `bin`, `truck`, `collection`, `location`, `road`, `route`, and `simulation`.

## 4. Technology Used

### Backend

- Java 21.
- Spring Boot.
- Spring Web for REST APIs.
- Spring Data JPA for entity persistence and repositories.
- MySQL driver for the production-like database connection.
- H2 for the test database.
- Flyway for version-controlled database migrations.
- Jakarta Bean Validation for request validation.
- Spring Boot Actuator for health and application information.
- Maven for build and test execution.

### Frontend

- React.
- TypeScript.
- Vite.
- `lucide-react` for interface icons.
- HTML Canvas for the road map, route lines, bin markers, and truck animation.
- Plain CSS for the dashboard layout and visual styling.

## 5. Main Domain Model

### Location

A location represents a point in the collection area. The demo uses Cartesian coordinates (`x_coordinate` and `y_coordinate`) so the map can be drawn without GPS data.

Examples include:

- Depot.
- North Gate.
- Library.
- Student Center.

### Road

A road connects two locations and stores:

- Source location.
- Destination location.
- Distance.
- Travel time.
- Whether the road is bidirectional.

Roads become the edges of the weighted graph used by the route algorithm.

### WasteBin

A waste bin stores:

- Bin code.
- Location.
- Capacity in kilograms.
- Current fill in kilograms.
- Waste type.
- Current status.
- Collection timestamps.

The `WasteBin` class classifies its own status using the fill ratio:

| Fill ratio | Status |
|---|---|
| Less than 50% | `NORMAL` |
| 50% to less than 75% | `MEDIUM` |
| 75% to less than 95% | `HIGH` |
| 95% or more | `CRITICAL` |

The class also controls fill increases and collection resets. This keeps state changes inside the domain object instead of spreading them across controllers.

### CollectionRequest

A collection request is created for a bin that requires service. It stores:

- The related bin.
- A priority score based on the bin fill percentage.
- Urgency.
- Status.
- Creation and resolution timestamps.

Request states include `PENDING`, `ASSIGNED`, `COMPLETED`, and `CANCELLED`.

### Truck

A truck stores:

- Truck code.
- Capacity.
- Current load.
- Current status.
- Current location.

Truck states include `AVAILABLE`, `ASSIGNED`, `COLLECTING`, and `UNAVAILABLE`. The truck rejects loads larger than its capacity, changes to `ASSIGNED` when a route is created, changes to `COLLECTING` when the route starts, and becomes available again after route completion.

### Route and RouteStop

A route stores:

- Assigned truck.
- Algorithm name.
- Total distance.
- Total estimated time.
- Route status.
- Creation and completion timestamps.

A route contains ordered route stops. Each stop links a route to a bin and records its sequence number, completion status, and collected quantity.

## 6. Complete Application Workflow

The implemented workflow is:

1. Flyway creates the database schema and inserts demo data.
2. The frontend loads bins, requests, locations, roads, trucks, and the latest active route.
3. The dashboard shows urgent bins, pending requests, available trucks, and total waste.
4. The user selects collection requests.
5. The user selects an available truck.
6. The backend checks that the truck exists, is available, and has enough capacity.
7. The route service orders the selected requests by priority score.
8. Dijkstra calculates the shortest path from the truck location to each requested bin in sequence.
9. The backend stores the route and route stops and assigns the requests to the truck.
10. The frontend displays the route on the Canvas map.
11. The user can start the route, complete stops, and complete the route.
12. Completing a stop records collected waste, completes the related request, and reduces the truck load.
13. Completing all stops makes the truck available again.
14. An active route is restored after a browser refresh through `GET /api/routes/active`.

## Programming Lab Concepts Implemented

- **Arrays and CRUD:** The bin API supports create, read/display, update, and delete operations. Bin responses are materialized into a Java array and traversed for display and search; database storage keeps changes persistent.
- **Searching:** `GET /api/bins?search=...` uses linear search over bin code and location name.
- **Singly linked list:** A project-owned list provides insert, find, update, delete, size, and traversal operations. Route planning stores requests in the existing priority order, then uses the linked list to build its ordered stop list.

Bin display keeps the application's original urgency order. The existing Dijkstra priority queue remains part of route calculation; it is required by the route algorithm, not added as an unrelated PL task.

## 7. Data Structures and Algorithms Used

### 7.1 Relational collections and lists

Spring Data repositories return Java `List` collections for bins, requests, roads, and route stops. Streams are used to filter, map, sort, and aggregate operational data.

Examples:

- Filtering bins that are not collected during a simulation tick.
- Ordering collection requests by priority and creation time.
- Calculating total selected waste.
- Mapping entities to API response records.

### 7.2 Weighted graph

The road network is represented by `WeightedGraph` using an adjacency-list structure:

```text
Map<LocationId, List<Edge>>
```

Each edge stores:

- Destination location.
- Distance.
- Travel time.

Bidirectional database roads are inserted in both directions before route calculation.

### 7.3 Dijkstra shortest path

`DijkstraPathFinder` uses:

- A map of the shortest known distance to each location.
- A map of travel time.
- A previous-location map for path reconstruction.
- A previous-location map for rebuilding the route in forward order.
- A visited set.
- Java's `PriorityQueue` to process the closest unvisited location first.

The algorithm returns:

- Ordered location IDs.
- Total distance.
- Total travel time.

For a graph with $V$ vertices and $E$ edges, this implementation uses an adjacency list and a priority queue, giving the typical complexity of approximately $O((V + E) \log V)$.

### 7.4 Priority-based request handling

The request endpoint returns pending requests ordered by:

1. Highest `priorityScore` first.
2. Earliest creation time as a tie-breaker.

The route service also sorts selected requests by descending priority before building the route sequence. This provides the MVP's priority-queue behavior through database ordering and Java sorting, although a separate custom `PriorityQueue` class is not currently implemented.

### 7.5 Route construction strategies

The route planner supports two selectable request-ordering strategies. Both use Dijkstra to calculate the shortest road path between each consecutive stop:

- **Priority first (`DIJKSTRA`):** sort selected requests by priority score, then creation time; visit them in that order.
- **Nearest stop (`NEAREST_STOP`):** repeatedly choose the remaining request with the shortest Dijkstra distance from the truck's current location.

The nearest-stop strategy is greedy and does not guarantee a globally optimal vehicle route. The strategies demonstrate different ordering tradeoffs; route totals are calculated using the chosen stop order.

### 7.6 Complexity and scope

The project currently demonstrates Dijkstra complexity and compares priority-first and nearest-stop ordering in the route workflow. Formal runtime benchmarking and custom implementations of every structure listed in `doc.md` are future extensions.

## 8. Object-Oriented Programming Concepts

### Encapsulation

Entity fields are private. State changes are controlled through methods such as:

- `WasteBin.increaseFill()`.
- `WasteBin.collect()`.
- `Truck.assign()`.
- `Truck.startCollecting()`.
- `Truck.reduceLoad()`.
- `Truck.makeAvailable()`.
- `Route.start()`.
- `Route.complete()`.

### Classes and objects

The real-world nouns from the problem become Java classes, including `WasteBin`, `Truck`, `Location`, `Road`, `CollectionRequest`, `Route`, and `RouteStop`.

### Constructors and validation

Constructors initialize valid domain state. REST request records use Jakarta Validation annotations to reject invalid bin data such as non-positive capacity or fill values outside the capacity.

### Strategy interface, inheritance, and polymorphism

`RouteRequestOrderingStrategy` defines the route-ordering contract. `AbstractRouteRequestOrderingStrategy` provides shared Dijkstra support and priority ordering. `PriorityFirstRouteStrategy` and `NearestStopRouteStrategy` extend the abstract class and initialize it using `super(...)`. `RouteService` selects an implementation from a map by the requested algorithm name and invokes the interface method polymorphically. This keeps route-order selection out of a growing algorithm-specific conditional.

### StringBuilder route summary

`RouteResponse.from()` uses `StringBuilder` to create a readable summary containing the route number, truck, ordered bin stops, distance, and time. The response is also displayed in the route planner. `StringBuffer` is not used because summary generation is local to one request and does not need synchronized string mutation.

### Composition and relationships

JPA relationships model the system:

- A bin belongs to a location.
- A road connects two locations.
- A request belongs to a bin.
- A route belongs to a truck.
- A route contains route stops.
- A route stop refers to a bin.

### Enums

Enums make state values explicit and safer than arbitrary strings:

- `BinStatus`.
- `WasteType`.
- `RequestStatus`.
- `TruckStatus`.
- `RouteStatus`.
- `RouteStopStatus`.

### Layered design

The backend separates responsibilities into:

- Entities: persistent domain state.
- Repositories: database access.
- Services: business rules and transactions.
- Controllers: HTTP endpoints.
- DTOs/responses: API representation.
- Algorithm classes: graph and path-finding logic independent of Spring.

### Error handling

`GlobalExceptionHandler` converts common failures into JSON responses, including:

- Invalid arguments as HTTP 400.
- Invalid state transitions as HTTP 409.
- Validation failures as HTTP 400 with field errors.

## 9. Simulation and Monitoring

The simulation endpoint represents changing sensor readings without requiring physical IoT hardware.

Each simulation tick:

1. Finds bins that have not been collected.
2. Increases their fill by the configured amount, currently 5 kg per tick by default.
3. Recalculates each bin status.
4. Creates a request when a bin becomes `HIGH` or `CRITICAL` and has no pending or assigned request.
5. Saves the changed bins and newly created requests.

This demonstrates the monitoring and alert-generation part of `doc.md` using deterministic data.

The backend also polls the latest ThingSpeak feed every 20 seconds by default.
`field1`, `field2`, and `field3` provide centimeter distances for the Library
bin (BIN-001), North Gate bin (BIN-002), and Student Center bin (BIN-003),
respectively. The configured linear calibration maps 2 cm to 100% full and
12 cm to empty, with readings outside that range clamped to the nearest endpoint
(for example, 8 cm maps to 40%; distances of 12 cm or more, including anything
over 15 cm, map to 0%). Blank sensor fields are skipped so that an available
sensor can still update its own bin. Each percentage is saved as a fill weight
using that bin's capacity. ThingSpeak creates a pending request for every bin
at 10% fill or higher, so each qualifying bin is available in the Route Planner.
A fresh browser refresh reloads bin and
request data without using cached API responses. Existing pending request
percentages and urgency follow the latest sensor fill, and the request remains
in the queue when the fill is above 0%, including values below the collection
threshold. A pending request is removed only when a fresh sensor reading is
exactly 0%.
Only bins at 10% fill or higher are selectable for route planning; the backend
also validates the threshold when a route is created.
ThingSpeak is polled every 20 seconds by default.
After a truck collects a bin, it stays at 0% for 20 seconds; readings published
before collection or received during that cooldown are ignored. The next fresh
reading updates the fill and creates a new truck request if the bin is HIGH or
CRITICAL.
Configure `THINGSPEAK_CHANNEL_ID` and `THINGSPEAK_READ_API_KEY` through the
environment or `backend/.env`.

## 10. Database Design

The first migration creates these tables:

- `locations`.
- `roads`.
- `waste_bins`.
- `trucks`.
- `collection_requests`.
- `routes`.
- `route_stops`.

The schema includes:

- Primary keys.
- Foreign keys.
- Unique bin and truck codes.
- Positive-capacity checks.
- Fill-range checks.
- Request status and priority indexes.

Flyway migrations currently include:

- `V1__create_mvp_schema.sql`: creates the tables and constraints.
- `V2__insert_demo_data.sql`: inserts the original campus data, bins, truck, and requests.
- `V3__insert_additional_demo_data.sql`: adds a second truck and another request.
- `V4__insert_more_demo_bins_and_requests.sql`: adds five more bins and five linked requests.

Migrations are append-only after being applied. New seed data belongs in a new migration instead of modifying an older migration, which prevents checksum errors in Flyway.

## 11. REST API

### Bin management

- `GET /api/bins`: list all bins.
- `GET /api/bins/{id}`: get one bin.
- `POST /api/bins`: create a bin.
- `PUT /api/bins/{id}`: update a bin.
- `DELETE /api/bins/{id}`: delete a bin.

### Supporting data

- `GET /api/locations`: list map locations.
- `GET /api/roads`: list road connections.
- `GET /api/trucks`: list trucks.
- `GET /api/collection-requests?status=PENDING`: list requests by status.

### Simulation

- `POST /api/simulation/tick`: increase fill levels and create new requests.

### Routes

- `POST /api/routes/optimize`: create a route for a truck and selected requests.
- `GET /api/routes/{id}`: retrieve a route.
- `GET /api/routes/active`: retrieve the newest assigned or collecting route.
- `POST /api/routes/{id}/start`: start a route.
- `POST /api/routes/{routeId}/stops/{stopId}/complete`: complete one stop.
- `POST /api/routes/{id}/complete`: complete the route.

## 12. Frontend and Computer Graphics Concepts

The frontend dashboard contains:

- Sidebar navigation for overview, bin network, and route planner views.
- Summary cards for urgent bins, open requests, fleet readiness, and total waste.
- Request selection controls.
- Truck selection controls.
- Bin creation form.
- Collection-area map.
- Route summary and stop controls.

The `RouteCanvas` component uses the HTML Canvas API to render:

- A background grid.
- Road line segments.
- Location markers and labels.
- Bin markers colored by status.
- A dashed planned route.
- A truck marker.

The truck animation uses presentation-side graphics concepts from `doc.md`:

- Translation to move the truck between locations.
- Rotation based on the direction of travel.
- Linear interpolation between route endpoints.
- Scaling the coordinate system to fit the map viewport.
- Cohen–Sutherland line clipping to keep grid, road, and route segments within the Canvas viewport.
- Redrawing through `ResizeObserver` when the map viewport changes size.
- Color coding for depot, urgent bins, roads, and planned routes.

The Canvas renderer uses custom computer-graphics algorithms for its map primitives:

- Bresenham's integer line algorithm draws grid lines, roads, and the dashed planned route.
- The midpoint circle algorithm draws filled location/bin markers and bin marker outlines.
- Cohen–Sutherland clips line segments before rasterization.

The project uses browser Canvas rather than native OpenGL/GLFW. It does not currently include 3D objects, textures, Bezier curves, or a Java Swing GUI.

### COA Lab: processor and instruction simulation

The dashboard's **COA lab** view connects processor concepts to current WasteFlow data. Its backend endpoints are:

- `POST /api/coa/bin-status`: simulates fill-percentage arithmetic and threshold branches using the selected bin's fill and capacity.
- `POST /api/coa/route-totals`: simulates 64-bit register accumulation for route distance and travel time using the loaded route totals.

The teaching CPU models 64-bit `RAX`, `RBX`, `RCX`, and `RDX` registers, ALU arithmetic/comparison, zero/sign/carry/overflow flags, conditional branches, and a fetch/decode/execute trace. Its supported instruction subset includes `MOV`, `ADD`, `IMUL`, `CQO`, `IDIV`, `CMP`, `JGE`, `JL`, `JMP`, and `HLT`. Measurements are represented in milli-units so the simulated arithmetic uses integer registers.

This is a Java software simulator interpreting an x86-64-style instruction subset. It does not assemble or execute native x86-64 machine code; native assembly integration requires a compatible 64-bit assembler and toolchain.

## 13. Refresh and Active Route Recovery

Route state originally existed only in React memory, which meant a browser refresh set the selected route back to `null`. The implemented recovery flow fixes this:

1. The backend searches for the newest route with status `ASSIGNED` or `COLLECTING`.
2. The frontend requests it during initial data loading.
3. The route and truck ID are restored into React state.
4. The Canvas receives the truck's current location and redraws the route.
5. If no active route exists, the endpoint returns HTTP 204 and the dashboard remains in its normal empty-route state.

This keeps the database as the source of truth rather than relying on browser-only state.

## 14. Testing and Verification

The backend test suite verifies:

- Spring application context startup.
- Flyway migration loading against the test database.
- Dijkstra shortest-path behavior.

The Dijkstra test confirms that the algorithm selects the lower-weight path rather than the direct but more expensive edge.

The frontend build runs:

```text
npm run build
```

This performs TypeScript compilation and Vite production bundling.

The backend test command is:

```text
.\mvnw.cmd test
```

## 15. Running the Project

### Start MySQL

From the backend directory, start the Docker database defined in `docker-compose.yml`:

```text
docker compose up -d
```

### Start the backend

```text
cd backend
.\mvnw.cmd spring-boot:run
```

The default backend port is `8080`.

### Start the frontend

```text
cd frontend
npm install
npm run dev
```

The default Vite development server runs on port `5173`.

### Test and build

```text
cd backend
.\mvnw.cmd test

cd ..\frontend
npm run build
```

## 16. What Is Implemented Compared With `doc.md`

| Concept from `doc.md` | Current implementation |
|---|---|
| Digital bin records | Implemented with JPA, REST CRUD, and MySQL persistence |
| Bin status classification | Implemented using fill-ratio thresholds |
| Simulated monitoring | Implemented through the simulation tick endpoint |
| Collection requests | Implemented with persistence and status transitions |
| Priority handling | Implemented through priority-score ordering and route sorting |
| Trucks and capacity | Implemented with availability, assignment, load checks, and status changes |
| Weighted roads and locations | Implemented with relational tables and a graph adjacency list |
| Dijkstra | Implemented and unit tested |
| Route-order strategies | Priority-first and nearest-stop options implemented; both use Dijkstra for shortest-path legs |
| OOP strategy concepts | Interface, abstract class, inheritance, `super(...)`, and polymorphic dispatch implemented for route ordering |
| StringBuilder | Implemented for the route summary shown in the route view; StringBuffer is unnecessary for this request-local operation |
| Route persistence | Implemented with routes and route stops |
| 2D map rendering | Implemented with HTML Canvas |
| Truck animation | Implemented with translation, rotation, and interpolation |
| REST backend | Implemented with Spring controllers and JSON responses |
| Database migrations | Implemented with Flyway versions V1 through V4 |
| Error handling | Implemented with a global REST exception handler |
| Task 1 arrays and search | Urgency-ordered bin results are materialized into an array; linear search supports bin-code and location queries |
| Task 2 linked list | Project-owned singly linked list supports insert, find, update, delete, size, and traversal; used in priority-ordered route planning |
| Task 3 stack/queue | No separate PL stack/queue feature was added; Dijkstra's priority queue remains essential to shortest-path calculation |
| Task 4 sorting/hashing | No separate sorting or hash-table feature was added |
| COA processor model | Implemented as an educational simulator with 64-bit general-purpose registers, ALU operations, flags, conditional branches, and an instruction trace |
| COA application calculations | Bin fill classification and route distance/time accumulation are traced through simulated x86-64 instruction programs |
| Native assembly execution | Not implemented; the COA lab interprets a documented x86-64 instruction subset in Java and does not execute native machine code |
| Report generation | Not currently implemented |
| Algorithm benchmarking | Not currently implemented |
| Authentication and roles | Not currently implemented |
| Java Swing GUI | Not used; React web UI is used instead |
| OpenGL or 3D graphics | Not currently implemented |
| IoT/GPS integration | ThingSpeak readings update bins 1, 2, and 3; GPS integration is not currently implemented |

## 17. Limitations of the Current MVP

The current route optimizer is designed for a clear, demonstrable MVP. It does not yet solve the full vehicle-routing problem with:

- Multiple trucks in one optimization request.
- Return-to-depot optimization.
- Time windows.
- Fuel or emissions costs.
- Traffic-aware travel times.
- Global optimization or benchmarking/comparison of all possible route orders.

The frontend also does not yet include reports, authentication, a 3D map, or custom data-structure demonstration screens.

## 18. Future Extensions

The next academic extensions can be added without replacing the current core workflow:

1. Add custom priority queue, queue, deque, and circular-queue implementations if later PL tasks require them.
2. Add a dedicated search and sorting screen with selectable algorithms and performance comparisons.
3. Add route candidate comparison and algorithm performance measurements.
4. Add multiple-truck vehicle-routing optimization.
5. Add collection history and report generation.
6. Add authentication and operator roles.
7. Add WebSocket or Server-Sent Events for live updates.
8. Add GPS coordinates and real road data.
9. Replace or complement the COA teaching interpreter with native 64-bit assembly execution when an x64 assembler/toolchain is available.
10. Add optional 3D rendering, lighting, textures, and advanced animation.

## 19. Final Summary

WasteFlow successfully implements the central problem described in `doc.md`: monitor waste bins, create prioritized collection work, assign an appropriate truck, calculate a route over a weighted graph, display the result graphically, animate the truck, and persist the operational state.

The strongest implemented academic concepts are:

- Object-oriented domain modeling in Java.
- Relational database design and migration management.
- Weighted graph representation.
- Dijkstra shortest-path calculation.
- Priority-based collection handling.
- Capacity validation and state transitions.
- 2D Canvas rendering and animation.
- REST-based full-stack integration.

The remaining concepts in `doc.md` are documented as future extensions so the project description remains technically accurate and aligned with the actual codebase.
