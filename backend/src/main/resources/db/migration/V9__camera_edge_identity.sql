ALTER TABLE cameras
    ADD COLUMN edge_node_id VARCHAR(128),
    ADD COLUMN node_api_key VARCHAR(255);

CREATE INDEX idx_cameras_edge_node_id
    ON cameras (edge_node_id);
