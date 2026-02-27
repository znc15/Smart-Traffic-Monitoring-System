CREATE OR REPLACE VIEW traffic_flow_hourly AS
SELECT
    road_name,
    date_trunc('hour', sample_time) AS bucket_at,
    SUM(count_car + count_motor + count_person) AS total_flow,
    AVG(congestion_index) AS avg_congestion_index
FROM traffic_samples
GROUP BY road_name, date_trunc('hour', sample_time);

CREATE OR REPLACE VIEW traffic_flow_daily AS
SELECT
    road_name,
    date_trunc('day', sample_time) AS bucket_at,
    SUM(count_car + count_motor + count_person) AS total_flow,
    AVG(congestion_index) AS avg_congestion_index
FROM traffic_samples
GROUP BY road_name, date_trunc('day', sample_time);

CREATE OR REPLACE VIEW traffic_flow_weekly AS
SELECT
    road_name,
    date_trunc('week', sample_time) AS bucket_at,
    SUM(count_car + count_motor + count_person) AS total_flow,
    AVG(congestion_index) AS avg_congestion_index
FROM traffic_samples
GROUP BY road_name, date_trunc('week', sample_time);

CREATE OR REPLACE VIEW traffic_flow_monthly AS
SELECT
    road_name,
    date_trunc('month', sample_time) AS bucket_at,
    SUM(count_car + count_motor + count_person) AS total_flow,
    AVG(congestion_index) AS avg_congestion_index
FROM traffic_samples
GROUP BY road_name, date_trunc('month', sample_time);
