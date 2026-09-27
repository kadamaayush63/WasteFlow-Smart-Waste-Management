INSERT INTO waste_bins (id, bin_code, location_id, capacity_kg, current_fill_kg, waste_type, status)
VALUES
    (9, 'BIN-009', 3, 180, 162, 'ORGANIC', 'HIGH'),
    (10, 'BIN-010', 4, 120, 114, 'GENERAL', 'CRITICAL'),
    (11, 'BIN-011', 5, 200, 130, 'RECYCLABLE', 'MEDIUM'),
    (12, 'BIN-012', 2, 150, 120, 'ORGANIC', 'HIGH'),
    (13, 'BIN-013', 3, 100, 55, 'GENERAL', 'MEDIUM');

INSERT INTO collection_requests (bin_id, priority_score, urgency, status)
VALUES
    (9, 90, 'HIGH', 'PENDING'),
    (10, 95, 'CRITICAL', 'PENDING'),
    (11, 65, 'MEDIUM', 'PENDING'),
    (12, 80, 'HIGH', 'PENDING'),
    (13, 55, 'MEDIUM', 'PENDING');
