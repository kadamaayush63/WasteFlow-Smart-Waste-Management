CREATE TABLE locations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    x_coordinate DOUBLE NOT NULL,
    y_coordinate DOUBLE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE roads (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_location_id BIGINT NOT NULL,
    destination_location_id BIGINT NOT NULL,
    distance DOUBLE NOT NULL,
    travel_time_minutes DOUBLE NOT NULL,
    bidirectional BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_roads_source FOREIGN KEY (source_location_id) REFERENCES locations (id),
    CONSTRAINT fk_roads_destination FOREIGN KEY (destination_location_id) REFERENCES locations (id),
    CONSTRAINT chk_roads_distance CHECK (distance > 0),
    CONSTRAINT chk_roads_time CHECK (travel_time_minutes > 0)
);

CREATE TABLE waste_bins (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bin_code VARCHAR(40) NOT NULL,
    location_id BIGINT NOT NULL,
    capacity_kg DOUBLE NOT NULL,
    current_fill_kg DOUBLE NOT NULL DEFAULT 0,
    waste_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    last_collected_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_waste_bins_code UNIQUE (bin_code),
    CONSTRAINT fk_waste_bins_location FOREIGN KEY (location_id) REFERENCES locations (id),
    CONSTRAINT chk_waste_bins_capacity CHECK (capacity_kg > 0),
    CONSTRAINT chk_waste_bins_fill CHECK (current_fill_kg >= 0 AND current_fill_kg <= capacity_kg)
);

CREATE TABLE trucks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    truck_code VARCHAR(40) NOT NULL,
    capacity_kg DOUBLE NOT NULL,
    current_load_kg DOUBLE NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    location_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_trucks_code UNIQUE (truck_code),
    CONSTRAINT fk_trucks_location FOREIGN KEY (location_id) REFERENCES locations (id),
    CONSTRAINT chk_trucks_capacity CHECK (capacity_kg > 0),
    CONSTRAINT chk_trucks_load CHECK (current_load_kg >= 0 AND current_load_kg <= capacity_kg)
);

CREATE TABLE collection_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bin_id BIGINT NOT NULL,
    priority_score DOUBLE NOT NULL,
    urgency VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_collection_requests_bin FOREIGN KEY (bin_id) REFERENCES waste_bins (id),
    INDEX idx_collection_requests_status_priority (status, priority_score, created_at)
);

CREATE TABLE routes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    truck_id BIGINT NOT NULL,
    algorithm VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_distance DOUBLE NOT NULL DEFAULT 0,
    total_time DOUBLE NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_routes_truck FOREIGN KEY (truck_id) REFERENCES trucks (id)
);

CREATE TABLE route_stops (
    id BIGINT NOT NULL AUTO_INCREMENT,
    route_id BIGINT NOT NULL,
    bin_id BIGINT NOT NULL,
    sequence_number INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    collected_kg DOUBLE NULL,
    collected_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_route_stops_route FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT fk_route_stops_bin FOREIGN KEY (bin_id) REFERENCES waste_bins (id),
    CONSTRAINT uk_route_stops_sequence UNIQUE (route_id, sequence_number)
);