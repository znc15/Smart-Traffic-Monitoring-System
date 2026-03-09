-- Add new columns to api_clients table (idempotent via information_schema checks)
SET @has_description := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'api_clients'
      AND column_name = 'description'
);
SET @sql_add_description := IF(
    @has_description = 0,
    'ALTER TABLE api_clients ADD COLUMN description TEXT',
    'SELECT 1'
);
PREPARE stmt_add_description FROM @sql_add_description;
EXECUTE stmt_add_description;
DEALLOCATE PREPARE stmt_add_description;

SET @has_allowed_endpoints := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'api_clients'
      AND column_name = 'allowed_endpoints'
);
SET @sql_add_allowed_endpoints := IF(
    @has_allowed_endpoints = 0,
    'ALTER TABLE api_clients ADD COLUMN allowed_endpoints TEXT',
    'SELECT 1'
);
PREPARE stmt_add_allowed_endpoints FROM @sql_add_allowed_endpoints;
EXECUTE stmt_add_allowed_endpoints;
DEALLOCATE PREPARE stmt_add_allowed_endpoints;

SET @has_rate_limit := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'api_clients'
      AND column_name = 'rate_limit'
);
SET @sql_add_rate_limit := IF(
    @has_rate_limit = 0,
    'ALTER TABLE api_clients ADD COLUMN rate_limit INT NOT NULL DEFAULT 1000',
    'SELECT 1'
);
PREPARE stmt_add_rate_limit FROM @sql_add_rate_limit;
EXECUTE stmt_add_rate_limit;
DEALLOCATE PREPARE stmt_add_rate_limit;

SET @has_last_used_at := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'api_clients'
      AND column_name = 'last_used_at'
);
SET @sql_add_last_used_at := IF(
    @has_last_used_at = 0,
    'ALTER TABLE api_clients ADD COLUMN last_used_at DATETIME',
    'SELECT 1'
);
PREPARE stmt_add_last_used_at FROM @sql_add_last_used_at;
EXECUTE stmt_add_last_used_at;
DEALLOCATE PREPARE stmt_add_last_used_at;

-- Create api_usage_logs table
CREATE TABLE IF NOT EXISTS api_usage_logs (
    id               BIGINT       PRIMARY KEY AUTO_INCREMENT,
    api_client_id    BIGINT,
    endpoint         VARCHAR(255) NOT NULL,
    method           VARCHAR(10)  NOT NULL,
    status_code      INT          NOT NULL,
    response_time_ms BIGINT,
    request_ip       VARCHAR(45),
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_api_usage_logs_client FOREIGN KEY (api_client_id) REFERENCES api_clients(id)
);

-- Indexes for common query patterns
CREATE INDEX idx_api_usage_logs_client_time
    ON api_usage_logs (api_client_id, created_at);

CREATE INDEX idx_api_usage_logs_created_at
    ON api_usage_logs (created_at);

CREATE INDEX idx_api_usage_logs_endpoint
    ON api_usage_logs (endpoint);
