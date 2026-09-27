INSERT INTO waste_bins (id, bin_code, location_id, capacity_kg, current_fill_kg, waste_type, status)
VALUES
    (4, 'BIN-004', 2, 200, 180, 'GENERAL', 'HIGH'),
    (5, 'BIN-005', 3, 100, 95, 'RECYCLABLE', 'CRITICAL'),
    (6, 'BIN-006', 4, 160, 110, 'ORGANIC', 'MEDIUM'),
    (7, 'BIN-007', 5, 140, 70, 'GENERAL', 'MEDIUM'),
    (8, 'BIN-008', 2, 90, 45, 'RECYCLABLE', 'MEDIUM');

INSERT INTO collection_requests (bin_id, priority_score, urgency, status)
VALUES
    (4, 90, 'HIGH', 'PENDING'),
    (5, 95, 'CRITICAL', 'PENDING'),
    (6, 68.75, 'MEDIUM', 'PENDING'),
    (7, 50, 'MEDIUM', 'PENDING'),
    (8, 50, 'MEDIUM', 'PENDING');
