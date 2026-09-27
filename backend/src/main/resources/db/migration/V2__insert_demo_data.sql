INSERT INTO locations (id, name, x_coordinate, y_coordinate)
VALUES
    (1, 'Depot', 50, 50),
    (2, 'North Gate', 50, 10),
    (3, 'Library', 20, 30),
    (4, 'Student Center', 80, 30),
    (5, 'Sports Complex', 80, 80);

INSERT INTO roads (source_location_id, destination_location_id, distance, travel_time_minutes, bidirectional)
VALUES
    (1, 2, 40, 8, TRUE),
    (1, 3, 35, 7, TRUE),
    (1, 4, 35, 7, TRUE),
    (3, 4, 60, 12, TRUE),
    (4, 5, 50, 10, TRUE),
    (2, 4, 45, 9, TRUE);

INSERT INTO waste_bins (id, bin_code, location_id, capacity_kg, current_fill_kg, waste_type, status)
VALUES
    (1, 'BIN-001', 3, 100, 80, 'GENERAL', 'HIGH'),
    (2, 'BIN-002', 4, 120, 115, 'RECYCLABLE', 'CRITICAL'),
    (3, 'BIN-003', 5, 150, 40, 'ORGANIC', 'NORMAL');

INSERT INTO trucks (id, truck_code, capacity_kg, current_load_kg, status, location_id)
VALUES
    (1, 'TRUCK-001', 300, 0, 'AVAILABLE', 1);

INSERT INTO collection_requests (bin_id, priority_score, urgency, status)
VALUES
    (1, 80, 'HIGH', 'PENDING'),
    (2, 95.8333, 'CRITICAL', 'PENDING');