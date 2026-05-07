ALTER TABLE cameras
    ADD COLUMN latitude DOUBLE,
    ADD COLUMN longitude DOUBLE;

CREATE TABLE IF NOT EXISTS api_clients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO api_clients (name, api_key, enabled, created_at)
VALUES ('default-dev-client', 'dev-maas-key-change-me', TRUE, NOW());
