INSERT INTO trucks (id, truck_code, capacity_kg, current_load_kg, status, location_id)
VALUES
    (2, 'TRUCK-002', 250, 0, 'AVAILABLE', 1);

INSERT INTO collection_requests (bin_id, priority_score, urgency, status)
VALUES
    (3, 40, 'NORMAL', 'PENDING');
