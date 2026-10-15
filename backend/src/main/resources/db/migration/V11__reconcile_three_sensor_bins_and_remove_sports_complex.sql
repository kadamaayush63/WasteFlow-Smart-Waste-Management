CREATE TEMPORARY TABLE wasteflow_routes_to_remove_v11 (
    route_id BIGINT PRIMARY KEY
);

INSERT INTO wasteflow_routes_to_remove_v11 (route_id)
SELECT DISTINCT route_id
FROM route_stops
WHERE bin_id NOT IN (1, 2, 3);

DELETE route_stops
FROM route_stops
JOIN wasteflow_routes_to_remove_v11 ON wasteflow_routes_to_remove_v11.route_id = route_stops.route_id;

DELETE routes
FROM routes
JOIN wasteflow_routes_to_remove_v11 ON wasteflow_routes_to_remove_v11.route_id = routes.id;

DROP TEMPORARY TABLE wasteflow_routes_to_remove_v11;

DELETE FROM collection_requests
WHERE bin_id NOT IN (1, 2, 3);

DELETE FROM waste_bins
WHERE id NOT IN (1, 2, 3);

UPDATE waste_bins
SET location_id = 2
WHERE id = 3;

UPDATE trucks
SET location_id = 2
WHERE location_id = 5;

DELETE FROM roads
WHERE source_location_id = 5 OR destination_location_id = 5;

DELETE FROM locations
WHERE id = 5;
