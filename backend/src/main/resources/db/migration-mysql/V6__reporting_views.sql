CREATE OR REPLACE VIEW traffic_flow_hourly AS
SELECT
    road_name,
    CAST(DATE_FORMAT(sample_time, '%Y-%m-%d %H:00:00') AS DATETIME) AS bucket_at,
    SUM(count_car + count_motor + count_person) AS total_flow,
    AVG(congestion_index) AS avg_congestion_index
FROM traffic_samples
GROUP BY road_name, CAST(DATE_FORMAT(sample_time, '%Y-%m-%d %H:00:00') AS DATETIME);

CREATE OR REPLACE VIEW traffic_flow_daily AS
SELECT
    road_name,
    CAST(DATE(sample_time) AS DATETIME) AS bucket_at,
    SUM(count_car + count_motor + count_person) AS total_flow,
    AVG(congestion_index) AS avg_congestion_index
FROM traffic_samples
GROUP BY road_name, CAST(DATE(sample_time) AS DATETIME);

CREATE OR REPLACE VIEW traffic_flow_weekly AS
SELECT
    road_name,
    STR_TO_DATE(CONCAT(YEARWEEK(sample_time, 1), ' Monday'), '%X%V %W') AS bucket_at,
    SUM(count_car + count_motor + count_person) AS total_flow,
    AVG(congestion_index) AS avg_congestion_index
FROM traffic_samples
GROUP BY road_name, STR_TO_DATE(CONCAT(YEARWEEK(sample_time, 1), ' Monday'), '%X%V %W');

CREATE OR REPLACE VIEW traffic_flow_monthly AS
SELECT
    road_name,
    CAST(DATE_FORMAT(sample_time, '%Y-%m-01 00:00:00') AS DATETIME) AS bucket_at,
    SUM(count_car + count_motor + count_person) AS total_flow,
    AVG(congestion_index) AS avg_congestion_index
FROM traffic_samples
GROUP BY road_name, CAST(DATE_FORMAT(sample_time, '%Y-%m-01 00:00:00') AS DATETIME);
