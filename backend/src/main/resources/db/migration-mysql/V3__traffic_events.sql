CREATE TABLE IF NOT EXISTS traffic_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    node_id VARCHAR(128),
    road_name VARCHAR(255) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    level VARCHAR(32),
    start_at DATETIME,
    end_at DATETIME,
    payload_json JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_traffic_events_road_created
    ON traffic_events (road_name, created_at DESC);

CREATE INDEX idx_traffic_events_type_created
    ON traffic_events (event_type, created_at DESC);
