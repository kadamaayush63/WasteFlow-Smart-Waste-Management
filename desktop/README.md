# WasteFlow Swing desktop client

This standalone Java Swing application is an operator-facing desktop client for the existing WasteFlow Spring Boot API. It intentionally keeps routing, validation, persistence, and simulation in the backend rather than reimplementing that logic in the GUI.

## Run

Start the backend first, then from the workspace root build the client:

```powershell
.\backend\mvnw.cmd -f desktop\pom.xml package
java -jar desktop\target\wasteflow-desktop.jar
```

The client connects to `http://localhost:8080` by default. To use another backend URL, pass it as the first argument or set `WASTEFLOW_API_URL`:

```powershell
java -jar desktop\target\wasteflow-desktop.jar https://localhost:8443
```

## Operator workflow

1. Review bin fill levels and pending collection requests.
2. Select pending requests, an available truck, and a route ordering algorithm.
3. Plan a route; the backend checks truck capacity and request eligibility.
4. Start the route, select and complete each stop, then finish the route.
5. Advance the simulation to update bin fill and generate new collection requests.

Network calls run off the Swing event-dispatch thread so the interface remains responsive. API validation or connectivity failures are shown to the operator.
