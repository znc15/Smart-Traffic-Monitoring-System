CREATE TABLE IF NOT EXISTS traffic_predictions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    road_name VARCHAR(255) NOT NULL,
    generated_at DATETIME NOT NULL,
    predict_time DATETIME NOT NULL,
    predicted_flow DOUBLE NOT NULL,
    confidence_low DOUBLE NOT NULL,
    confidence_high DOUBLE NOT NULL,
    algorithm VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_traffic_predictions_unique (road_name, generated_at, predict_time)
);

CREATE INDEX idx_traffic_predictions_road_predict_time
    ON traffic_predictions (road_name, predict_time DESC);

CREATE INDEX idx_traffic_predictions_generated_at
    ON traffic_predictions (generated_at DESC);
