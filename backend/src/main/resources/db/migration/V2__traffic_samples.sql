CREATE TABLE IF NOT EXISTS traffic_samples (
    id BIGSERIAL PRIMARY KEY,
    node_id VARCHAR(128),
    road_name VARCHAR(255) NOT NULL,
    sample_time TIMESTAMP NOT NULL,
    count_car INTEGER NOT NULL DEFAULT 0,
    count_motor INTEGER NOT NULL DEFAULT 0,
    count_person INTEGER NOT NULL DEFAULT 0,
    avg_speed_car DOUBLE PRECISION NOT NULL DEFAULT 0,
    avg_speed_motor DOUBLE PRECISION NOT NULL DEFAULT 0,
    density_status VARCHAR(32),
    speed_status VARCHAR(32),
    congestion_index DOUBLE PRECISION NOT NULL DEFAULT 0,
    lane_stats_json JSONB,
    source VARCHAR(32) NOT NULL DEFAULT 'edge',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_traffic_samples_road_time
    ON traffic_samples (road_name, sample_time DESC);

CREATE INDEX IF NOT EXISTS idx_traffic_samples_sample_time
    ON traffic_samples (sample_time DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_traffic_samples_node_road_time
    ON traffic_samples (node_id, road_name, sample_time);
