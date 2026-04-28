package com.smarttraffic.backend.service.analytics;

import com.smarttraffic.backend.config.DbRuntimeProperties;
import com.smarttraffic.backend.model.TrafficEventEntity;
import com.smarttraffic.backend.model.TrafficSampleEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MirrorWriteService {

    private static final Logger log = LoggerFactory.getLogger(MirrorWriteService.class);

    private final DbRuntimeProperties dbRuntimeProperties;
    private final ObjectProvider<NamedParameterJdbcTemplate> mysqlMirrorJdbcProvider;
    private final ObjectProvider<NamedParameterJdbcTemplate> postgresMirrorJdbcProvider;

    public MirrorWriteService(
            DbRuntimeProperties dbRuntimeProperties,
            @Qualifier("mysqlMirrorJdbcTemplate") ObjectProvider<NamedParameterJdbcTemplate> mysqlMirrorJdbcProvider,
            @Qualifier("postgresMirrorJdbcTemplate") ObjectProvider<NamedParameterJdbcTemplate> postgresMirrorJdbcProvider
    ) {
        this.dbRuntimeProperties = dbRuntimeProperties;
        this.mysqlMirrorJdbcProvider = mysqlMirrorJdbcProvider;
        this.postgresMirrorJdbcProvider = postgresMirrorJdbcProvider;
    }

    public void mirrorTrafficSample(TrafficSampleEntity sample) {
        resolveMirrorTarget().ifPresent(target -> {
            String sql;
            if (target.dialect.equals("mysql")) {
                sql = """
                        INSERT INTO traffic_samples (
                            node_id, road_name, sample_time, count_car, count_motor, count_person,
                            avg_speed_car, avg_speed_motor, density_status, speed_status,
                            congestion_index, lane_stats_json, source
                        ) VALUES (
                            :nodeId, :roadName, :sampleTime, :countCar, :countMotor, :countPerson,
                            :avgSpeedCar, :avgSpeedMotor, :densityStatus, :speedStatus,
                            :congestionIndex, CAST(:laneStatsJson AS JSON), :source
                        )
                        ON DUPLICATE KEY UPDATE
                            count_car = VALUES(count_car),
                            count_motor = VALUES(count_motor),
                            count_person = VALUES(count_person),
                            avg_speed_car = VALUES(avg_speed_car),
                            avg_speed_motor = VALUES(avg_speed_motor),
                            density_status = VALUES(density_status),
                            speed_status = VALUES(speed_status),
                            congestion_index = VALUES(congestion_index),
                            lane_stats_json = VALUES(lane_stats_json),
                            source = VALUES(source)
                        """;
            } else {
                sql = """
                        INSERT INTO traffic_samples (
                            node_id, road_name, sample_time, count_car, count_motor, count_person,
                            avg_speed_car, avg_speed_motor, density_status, speed_status,
                            congestion_index, lane_stats_json, source
                        ) VALUES (
                            :nodeId, :roadName, :sampleTime, :countCar, :countMotor, :countPerson,
                            :avgSpeedCar, :avgSpeedMotor, :densityStatus, :speedStatus,
                            :congestionIndex, CAST(:laneStatsJson AS JSONB), :source
                        )
                        ON CONFLICT (node_id, road_name, sample_time) DO UPDATE SET
                            count_car = EXCLUDED.count_car,
                            count_motor = EXCLUDED.count_motor,
                            count_person = EXCLUDED.count_person,
                            avg_speed_car = EXCLUDED.avg_speed_car,
                            avg_speed_motor = EXCLUDED.avg_speed_motor,
                            density_status = EXCLUDED.density_status,
                            speed_status = EXCLUDED.speed_status,
                            congestion_index = EXCLUDED.congestion_index,
                            lane_stats_json = EXCLUDED.lane_stats_json,
                            source = EXCLUDED.source
                        """;
            }

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("nodeId", sample.getNodeId())
                    .addValue("roadName", sample.getRoadName())
                    .addValue("sampleTime", sample.getSampleTime())
                    .addValue("countCar", sample.getCountCar())
                    .addValue("countMotor", sample.getCountMotor())
                    .addValue("countPerson", sample.getCountPerson())
                    .addValue("avgSpeedCar", sample.getAvgSpeedCar())
                    .addValue("avgSpeedMotor", sample.getAvgSpeedMotor())
                    .addValue("densityStatus", sample.getDensityStatus())
                    .addValue("speedStatus", sample.getSpeedStatus())
                    .addValue("congestionIndex", sample.getCongestionIndex())
                    .addValue("laneStatsJson", sample.getLaneStatsJson())
                    .addValue("source", sample.getSource());

            try {
                target.jdbc.update(sql, params);
            } catch (Exception ex) {
                log.warn("mirror sample write failed: {}", ex.getMessage());
            }
        });
    }

    public void mirrorTrafficEvent(TrafficEventEntity event) {
        resolveMirrorTarget().ifPresent(target -> {
            String sql = """
                    INSERT INTO traffic_events (
                        node_id, road_name, event_type, level, start_at, end_at, payload_json
                    ) VALUES (
                        :nodeId, :roadName, :eventType, :level, :startAt, :endAt,
                        %s
                    )
                    """.formatted(target.dialect.equals("mysql") ? "CAST(:payloadJson AS JSON)" : "CAST(:payloadJson AS JSONB)");

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("nodeId", event.getNodeId())
                    .addValue("roadName", event.getRoadName())
                    .addValue("eventType", event.getEventType())
                    .addValue("level", event.getLevel())
                    .addValue("startAt", event.getStartAt())
                    .addValue("endAt", event.getEndAt())
                    .addValue("payloadJson", event.getPayloadJson());

            try {
                target.jdbc.update(sql, params);
            } catch (Exception ex) {
                log.warn("mirror event write failed: {}", ex.getMessage());
            }
        });
    }

    private Optional<MirrorTarget> resolveMirrorTarget() {
        if (!dbRuntimeProperties.isMirrorWrite()) {
            return Optional.empty();
        }

        if (dbRuntimeProperties.isPrimaryPostgres()) {
            NamedParameterJdbcTemplate mysql = mysqlMirrorJdbcProvider.getIfAvailable();
            return mysql == null ? Optional.empty() : Optional.of(new MirrorTarget(mysql, "mysql"));
        }

        NamedParameterJdbcTemplate pg = postgresMirrorJdbcProvider.getIfAvailable();
        return pg == null ? Optional.empty() : Optional.of(new MirrorTarget(pg, "postgres"));
    }

    private record MirrorTarget(NamedParameterJdbcTemplate jdbc, String dialect) {
    }
}
