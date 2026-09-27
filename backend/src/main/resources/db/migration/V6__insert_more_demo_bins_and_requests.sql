INSERT INTO waste_bins (id, bin_code, location_id, capacity_kg, current_fill_kg, waste_type, status)
VALUES
    (14, 'BIN-014', 4, 170, 153, 'GENERAL', 'HIGH'),
    (15, 'BIN-015', 5, 220, 198, 'ORGANIC', 'CRITICAL'),
    (16, 'BIN-016', 2, 130, 104, 'RECYCLABLE', 'MEDIUM'),
    (17, 'BIN-017', 3, 190, 171, 'GENERAL', 'HIGH'),
    (18, 'BIN-018', 4, 150, 123, 'ORGANIC', 'CRITICAL');

INSERT INTO collection_requests (bin_id, priority_score, urgency, status)
VALUES
    (14, 88, 'HIGH', 'PENDING'),
    (15, 97, 'CRITICAL', 'PENDING'),
    (16, 62, 'MEDIUM', 'PENDING'),
    (17, 86, 'HIGH', 'PENDING'),
    (18, 94, 'CRITICAL', 'PENDING');
