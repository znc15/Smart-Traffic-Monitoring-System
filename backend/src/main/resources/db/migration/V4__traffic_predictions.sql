CREATE TABLE IF NOT EXISTS traffic_predictions (
    id BIGSERIAL PRIMARY KEY,
    road_name VARCHAR(255) NOT NULL,
    generated_at TIMESTAMP NOT NULL,
    predict_time TIMESTAMP NOT NULL,
    predicted_flow DOUBLE PRECISION NOT NULL,
    confidence_low DOUBLE PRECISION NOT NULL,
    confidence_high DOUBLE PRECISION NOT NULL,
    algorithm VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_traffic_predictions_road_predict_time
    ON traffic_predictions (road_name, predict_time DESC);

CREATE INDEX IF NOT EXISTS idx_traffic_predictions_generated_at
    ON traffic_predictions (generated_at DESC);
