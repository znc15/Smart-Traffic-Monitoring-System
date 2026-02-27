CREATE TABLE IF NOT EXISTS traffic_events (
    id BIGSERIAL PRIMARY KEY,
    node_id VARCHAR(128),
    road_name VARCHAR(255) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    level VARCHAR(32),
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    payload_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_traffic_events_road_created
    ON traffic_events (road_name, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_traffic_events_type_created
    ON traffic_events (event_type, created_at DESC);
