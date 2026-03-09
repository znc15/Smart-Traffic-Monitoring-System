-- Add new columns to api_clients table
ALTER TABLE api_clients
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS allowed_endpoints TEXT,
    ADD COLUMN IF NOT EXISTS rate_limit INT NOT NULL DEFAULT 1000,
    ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMP;

-- Create api_usage_logs table
CREATE TABLE IF NOT EXISTS api_usage_logs (
    id              BIGSERIAL PRIMARY KEY,
    api_client_id   BIGINT REFERENCES api_clients(id),
    endpoint        VARCHAR(255) NOT NULL,
    method          VARCHAR(10)  NOT NULL,
    status_code     INT          NOT NULL,
    response_time_ms BIGINT,
    request_ip      VARCHAR(45),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_api_usage_logs_client_time
    ON api_usage_logs (api_client_id, created_at);

CREATE INDEX IF NOT EXISTS idx_api_usage_logs_created_at
    ON api_usage_logs (created_at);

CREATE INDEX IF NOT EXISTS idx_api_usage_logs_endpoint
    ON api_usage_logs (endpoint);
