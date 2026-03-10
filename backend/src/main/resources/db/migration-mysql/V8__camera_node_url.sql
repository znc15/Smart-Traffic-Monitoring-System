SET @has_node_url := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cameras'
      AND column_name = 'node_url'
);
SET @sql_add_node_url := IF(
    @has_node_url = 0,
    'ALTER TABLE cameras ADD COLUMN node_url VARCHAR(512)',
    'SELECT 1'
);
PREPARE stmt_add_node_url FROM @sql_add_node_url;
EXECUTE stmt_add_node_url;
DEALLOCATE PREPARE stmt_add_node_url;
