package com.smarttraffic.backend.service.analytics;

import com.smarttraffic.backend.dto.report.TrafficReportResponse;
import com.smarttraffic.backend.dto.report.TrafficReportRowResponse;
import com.smarttraffic.backend.exception.AppException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TrafficReportExportService {

    private static final Map<String, String> GRANULARITY_VIEW_MAP = Map.of(
            "hourly", "traffic_flow_hourly",
            "daily", "traffic_flow_daily",
            "weekly", "traffic_flow_weekly",
            "monthly", "traffic_flow_monthly"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TrafficReportExportService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public TrafficReportResponse queryReport(
            String granularity,
            String roadName,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        String normalizedGranularity = normalizeGranularity(granularity);
        String viewName = GRANULARITY_VIEW_MAP.get(normalizedGranularity);
        if (viewName == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "granularity must be one of hourly|daily|weekly|monthly");
        }

        String normalizedRoadName = blankToNull(roadName);
        List<String> whereClauses = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (normalizedRoadName != null) {
            whereClauses.add("road_name = :roadName");
            params.addValue("roadName", normalizedRoadName);
        }
        if (startAt != null) {
            whereClauses.add("bucket_at >= :startAt");
            params.addValue("startAt", startAt);
        }
        if (endAt != null) {
            whereClauses.add("bucket_at <= :endAt");
            params.addValue("endAt", endAt);
        }

        StringBuilder sqlBuilder = new StringBuilder("""
                SELECT bucket_at, road_name, total_flow, avg_congestion_index
                FROM %s
                """.formatted(viewName));
        if (!whereClauses.isEmpty()) {
            sqlBuilder.append("\nWHERE ").append(String.join("\n  AND ", whereClauses));
        }
        sqlBuilder.append("\nORDER BY bucket_at ASC, road_name ASC");
        String sql = sqlBuilder.toString();

        List<TrafficReportRowResponse> rows = jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            Timestamp ts = rs.getTimestamp("bucket_at");
            return new TrafficReportRowResponse(
                    ts == null ? null : ts.toLocalDateTime(),
                    rs.getString("road_name"),
                    rs.getDouble("total_flow"),
                    rs.getDouble("avg_congestion_index")
            );
        });

        return new TrafficReportResponse(
                normalizedGranularity,
                normalizedRoadName,
                startAt,
                endAt,
                rows
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportXlsx(
            String granularity,
            String roadName,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        TrafficReportResponse report = queryReport(granularity, roadName, startAt, endAt);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("traffic_report");
            Row header = sheet.createRow(0);
            writeCell(header, 0, "bucket_at");
            writeCell(header, 1, "road_name");
            writeCell(header, 2, "total_flow");
            writeCell(header, 3, "avg_congestion_index");

            int rowIndex = 1;
            for (TrafficReportRowResponse row : report.rows()) {
                Row excelRow = sheet.createRow(rowIndex++);
                writeCell(excelRow, 0, row.bucketAt() == null ? "" : fmt.format(row.bucketAt()));
                writeCell(excelRow, 1, row.roadName() == null ? "" : row.roadName());
                writeCell(excelRow, 2, row.totalFlow() == null ? "" : String.valueOf(row.totalFlow()));
                writeCell(excelRow, 3, row.avgCongestionIndex() == null ? "" : String.valueOf(row.avgCongestionIndex()));
            }

            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to export xlsx");
        }
    }

    public static String buildExportFileName(String granularity) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ROOT));
        return "traffic_report_%s_%s.xlsx".formatted(normalizeGranularity(granularity), timestamp);
    }

    private static String normalizeGranularity(String granularity) {
        if (granularity == null || granularity.isBlank()) {
            return "hourly";
        }
        return granularity.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static void writeCell(Row row, int index, String value) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
    }
}
