ALTER TABLE cameras
    ADD COLUMN IF NOT EXISTS edge_node_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS node_api_key VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_cameras_edge_node_id
    ON cameras (edge_node_id);
