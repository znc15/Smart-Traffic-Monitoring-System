CREATE OR REPLACE VIEW traffic_flow_hourly AS
SELECT
    road_name,
    STR_TO_DATE(DATE_FORMAT(sample_time, '%Y-%m-%d %H:00:00'), '%Y-%m-%d %H:%i:%s') AS bucket_at,
    SUM(count_car + count_motor + count_person) AS total_flow,
    AVG(congestion_index) AS avg_congestion_index
FROM traffic_samples
GROUP BY road_name, STR_TO_DATE(DATE_FORMAT(sample_time, '%Y-%m-%d %H:00:00'), '%Y-%m-%d %H:%i:%s');

CREATE OR REPLACE VIEW traffic_flow_daily AS
SELECT
    road_name,
    CAST(DATE(sample_time) AS DATETIME) AS bucket_at,
    SUM(count_car + count_motor + count_person) AS total_flow,
    AVG(congestion_index) AS avg_congestion_index
FROM traffic_samples
GROUP BY road_name, DATE(sample_time);

CREATE OR REPLACE VIEW traffic_flow_weekly AS
SELECT
    road_name,
    CAST(DATE(DATE_SUB(sample_time, INTERVAL (DAYOFWEEK(sample_time)-1) DAY)) AS DATETIME) AS bucket_at,
    SUM(count_car + count_motor + count_person) AS total_flow,
    AVG(congestion_index) AS avg_congestion_index
FROM traffic_samples
GROUP BY road_name, DATE(DATE_SUB(sample_time, INTERVAL (DAYOFWEEK(sample_time)-1) DAY));

CREATE OR REPLACE VIEW traffic_flow_monthly AS
SELECT
    road_name,
    STR_TO_DATE(DATE_FORMAT(sample_time, '%Y-%m-01'), '%Y-%m-%d') AS bucket_at,
    SUM(count_car + count_motor + count_person) AS total_flow,
    AVG(congestion_index) AS avg_congestion_index
FROM traffic_samples
GROUP BY road_name, STR_TO_DATE(DATE_FORMAT(sample_time, '%Y-%m-01'), '%Y-%m-%d');
