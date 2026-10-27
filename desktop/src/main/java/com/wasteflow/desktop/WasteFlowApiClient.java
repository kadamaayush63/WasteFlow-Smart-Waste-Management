package com.wasteflow.desktop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

final class WasteFlowApiClient {
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final TypeReference<List<Bin>> BINS = new TypeReference<>() {};
    private static final TypeReference<List<Truck>> TRUCKS = new TypeReference<>() {};
    private static final TypeReference<List<CollectionRequest>> REQUESTS = new TypeReference<>() {};

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final URI baseUri;

    WasteFlowApiClient(String baseUrl) {
        String normalized = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.baseUri = URI.create(normalized);
    }

    List<Bin> getBins() throws IOException, InterruptedException {
        return get("api/bins", BINS);
    }

    List<Truck> getTrucks() throws IOException, InterruptedException {
        return get("api/trucks", TRUCKS);
    }

    List<CollectionRequest> getPendingRequests() throws IOException, InterruptedException {
        return get("api/collection-requests?status=PENDING", REQUESTS);
    }

    Route getActiveRoute() throws IOException, InterruptedException {
        HttpResponse<String> response = send("api/routes/active", "GET", null);
        if (response.statusCode() == 204) {
            return null;
        }
        return readResponse(response, Route.class);
    }

    SimulationTick tick() throws IOException, InterruptedException {
        return post("api/simulation/tick", Map.of(), SimulationTick.class);
    }

    Route optimize(long truckId, List<Long> requestIds, String algorithm)
            throws IOException, InterruptedException {
        return post("api/routes/optimize",
                Map.of("truckId", truckId, "requestIds", requestIds, "algorithm", algorithm),
                Route.class);
    }

    Route undoLatestPlannedRoute() throws IOException, InterruptedException {
        return post("api/routes/undo-latest", Map.of(), Route.class);
    }

    Route startRoute(long routeId) throws IOException, InterruptedException {
        return post("api/routes/" + routeId + "/start", Map.of(), Route.class);
    }

    Route completeStop(long routeId, long stopId) throws IOException, InterruptedException {
        return post("api/routes/" + routeId + "/stops/" + stopId + "/complete", Map.of(), Route.class);
    }

    Route completeRoute(long routeId) throws IOException, InterruptedException {
        return post("api/routes/" + routeId + "/complete", Map.of(), Route.class);
    }

    private <T> T get(String path, TypeReference<T> type) throws IOException, InterruptedException {
        return readResponse(send(path, "GET", null), type);
    }

    private <T> T post(String path, Object body, Class<T> type) throws IOException, InterruptedException {
        String json = JSON.writeValueAsString(body);
        return readResponse(send(path, "POST", json), type);
    }

    private HttpResponse<String> send(String path, String method, String body)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(baseUri.resolve(path))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json");
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static <T> T readResponse(HttpResponse<String> response, Class<T> type) throws IOException {
        ensureSuccess(response);
        return JSON.readValue(response.body(), type);
    }

    private static <T> T readResponse(HttpResponse<String> response, TypeReference<T> type) throws IOException {
        ensureSuccess(response);
        return JSON.readValue(response.body(), type);
    }

    private static void ensureSuccess(HttpResponse<String> response) throws IOException {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }
        String message = "API request failed with HTTP " + response.statusCode();
        try {
            JsonNode error = JSON.readTree(response.body());
            if (error != null) {
                JsonNode detail = error.hasNonNull("message") ? error.get("message") : error.get("detail");
                if (detail != null && detail.isValueNode()) {
                    message = detail.asText();
                }
            }
        } catch (IOException ignored) {
            if (!response.body().isBlank()) {
                message += ": " + response.body();
            }
        }
        throw new IOException(message);
    }

    record Bin(long id, String binCode, String locationName, double capacityKg, double currentFillKg,
               String wasteType, String status) {}

    record Truck(long id, String truckCode, double capacityKg, double currentLoadKg, String status) {
        @Override
        public String toString() {
            return truckCode + " · " + status + " · " + currentLoadKg + "/" + capacityKg + " kg";
        }
    }

    record CollectionRequest(long id, String binCode, long binId, double priorityScore, String urgency) {}

    record Route(long id, long truckId, String algorithm, String status, double totalDistance, double totalTime,
                 String summary, List<RouteStop> stops) {}

    record RouteStop(long id, int sequenceNumber, long binId, long locationId, String status, Double collectedKg) {}

    record SimulationTick(int changedBinCount, int createdRequestCount) {}
}
