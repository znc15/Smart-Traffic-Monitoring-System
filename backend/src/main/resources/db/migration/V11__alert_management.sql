ALTER TABLE site_settings ADD COLUMN congestion_threshold DOUBLE DEFAULT 0.8;

CREATE TABLE alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(64) NOT NULL,
    level VARCHAR(32) NOT NULL,
    road_name VARCHAR(255) NOT NULL,
    node_id VARCHAR(128),
    message TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'UNCONFIRMED',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL
);

CREATE INDEX idx_alerts_status ON alerts(status);
CREATE INDEX idx_alerts_created_at ON alerts(created_at);
