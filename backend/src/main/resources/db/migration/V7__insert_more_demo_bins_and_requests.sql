INSERT INTO waste_bins (id, bin_code, location_id, capacity_kg, current_fill_kg, waste_type, status)
VALUES
    (19, 'BIN-019', 5, 110, 99, 'RECYCLABLE', 'MEDIUM'),
    (20, 'BIN-020', 2, 200, 176, 'GENERAL', 'HIGH'),
    (21, 'BIN-021', 3, 180, 162, 'ORGANIC', 'HIGH'),
    (22, 'BIN-022', 4, 210, 189, 'GENERAL', 'CRITICAL'),
    (23, 'BIN-023', 5, 140, 119, 'RECYCLABLE', 'MEDIUM');

INSERT INTO collection_requests (bin_id, priority_score, urgency, status)
VALUES
    (19, 58, 'MEDIUM', 'PENDING'),
    (20, 89, 'HIGH', 'PENDING'),
    (21, 91, 'HIGH', 'PENDING'),
    (22, 98, 'CRITICAL', 'PENDING'),
    (23, 64, 'MEDIUM', 'PENDING');
