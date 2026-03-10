SET @has_edge_node_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cameras'
      AND column_name = 'edge_node_id'
);
SET @sql_add_edge_node_id := IF(
    @has_edge_node_id = 0,
    'ALTER TABLE cameras ADD COLUMN edge_node_id VARCHAR(128)',
    'SELECT 1'
);
PREPARE stmt_add_edge_node_id FROM @sql_add_edge_node_id;
EXECUTE stmt_add_edge_node_id;
DEALLOCATE PREPARE stmt_add_edge_node_id;

SET @has_node_api_key := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cameras'
      AND column_name = 'node_api_key'
);
SET @sql_add_node_api_key := IF(
    @has_node_api_key = 0,
    'ALTER TABLE cameras ADD COLUMN node_api_key VARCHAR(255)',
    'SELECT 1'
);
PREPARE stmt_add_node_api_key FROM @sql_add_node_api_key;
EXECUTE stmt_add_node_api_key;
DEALLOCATE PREPARE stmt_add_node_api_key;

SET @has_idx_edge_node_id := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cameras'
      AND index_name = 'idx_cameras_edge_node_id'
);
SET @sql_idx_edge_node_id := IF(
    @has_idx_edge_node_id = 0,
    'CREATE INDEX idx_cameras_edge_node_id ON cameras (edge_node_id)',
    'SELECT 1'
);
PREPARE stmt_idx_edge_node_id FROM @sql_idx_edge_node_id;
EXECUTE stmt_idx_edge_node_id;
DEALLOCATE PREPARE stmt_idx_edge_node_id;
