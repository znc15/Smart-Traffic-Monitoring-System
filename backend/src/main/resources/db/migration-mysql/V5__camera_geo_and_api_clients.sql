SET @has_latitude := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cameras'
      AND column_name = 'latitude'
);
SET @sql_add_latitude := IF(
    @has_latitude = 0,
    'ALTER TABLE cameras ADD COLUMN latitude DOUBLE',
    'SELECT 1'
);
PREPARE stmt_add_latitude FROM @sql_add_latitude;
EXECUTE stmt_add_latitude;
DEALLOCATE PREPARE stmt_add_latitude;

SET @has_longitude := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cameras'
      AND column_name = 'longitude'
);
SET @sql_add_longitude := IF(
    @has_longitude = 0,
    'ALTER TABLE cameras ADD COLUMN longitude DOUBLE',
    'SELECT 1'
);
PREPARE stmt_add_longitude FROM @sql_add_longitude;
EXECUTE stmt_add_longitude;
DEALLOCATE PREPARE stmt_add_longitude;

CREATE TABLE IF NOT EXISTS api_clients (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO api_clients (name, api_key, enabled, created_at)
VALUES ('default-dev-client', 'dev-maas-key-change-me', TRUE, NOW())
ON DUPLICATE KEY UPDATE id = id;
