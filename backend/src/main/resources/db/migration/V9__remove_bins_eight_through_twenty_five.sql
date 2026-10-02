CREATE TEMPORARY TABLE wasteflow_routes_to_remove_v9 (
    route_id BIGINT PRIMARY KEY
);

INSERT INTO wasteflow_routes_to_remove_v9 (route_id)
SELECT DISTINCT route_id
FROM route_stops
WHERE bin_id BETWEEN 8 AND 25;

DELETE route_stops
FROM route_stops
JOIN wasteflow_routes_to_remove_v9 ON wasteflow_routes_to_remove_v9.route_id = route_stops.route_id;

DELETE routes
FROM routes
JOIN wasteflow_routes_to_remove_v9 ON wasteflow_routes_to_remove_v9.route_id = routes.id;

DROP TEMPORARY TABLE wasteflow_routes_to_remove_v9;

DELETE FROM collection_requests
WHERE bin_id BETWEEN 8 AND 25;

DELETE FROM waste_bins
WHERE id BETWEEN 8 AND 25;