package com.telemetria.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemetria.web.model.HourlyAggregateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HBaseService {

    private static final Logger log = LoggerFactory.getLogger(HBaseService.class);
    private static final DateTimeFormatter SLOT_FMT = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private final RestTemplate restTemplate;
    private final String restUrl;
    private final String table;
    private final ObjectMapper objectMapper;

    public HBaseService(
            @Value("${app.hbase.rest-url:http://10.0.0.30:8085}") String restUrl,
            @Value("${app.hbase.table:webhardmon_hourly}") String table,
            ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(15_000);
        this.restTemplate = new RestTemplate(factory);
        this.restUrl = restUrl;
        this.table = table;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns hourly aggregates for the given empresa for the last {@code days} days (max 30).
     * Results are sorted by slot (yyyyMMddHH) ascending.
     * Returns an empty list if HBase is unreachable or no data exists.
     */
    public List<HourlyAggregateDTO> getRecentAggregates(Long empresaId, int days) {
        int cappedDays = Math.max(1, Math.min(days, 30));
        String fromSlot = LocalDateTime.now(ZoneOffset.UTC).minusDays(cappedDays).format(SLOT_FMT);

        // Prefix scan: all row keys starting with "{empresaId}|"
        // End key uses char(125) = '}' which is char(124)+1 = '|'+1, stopping the prefix scan cleanly
        String startRow = empresaId + "|";
        String endRow   = empresaId + "}";

        String scannerUrl = createScanner(startRow, endRow);
        if (scannerUrl == null) return List.of();

        List<TierRow> rawRows = List.of();
        try {
            rawRows = fetchRows(scannerUrl, fromSlot);
        } catch (Exception e) {
            log.warn("HBase fetch error for empresa {}: {}", empresaId, e.getMessage());
        } finally {
            deleteScanner(scannerUrl);
        }

        return aggregate(rawRows);
    }

    // ── Scanner lifecycle ────────────────────────────────────────────────────

    private String createScanner(String startRow, String endRow) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            String body = String.format(
                    "{\"startRow\":\"%s\",\"endRow\":\"%s\",\"maxVersions\":1}",
                    b64(startRow), b64(endRow));

            ResponseEntity<Void> resp = restTemplate.exchange(
                    restUrl + "/" + table + "/scanner",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Void.class);

            List<String> locations = resp.getHeaders().get(HttpHeaders.LOCATION);
            return (locations != null && !locations.isEmpty()) ? locations.get(0) : null;
        } catch (Exception e) {
            log.warn("HBase scanner creation failed: {}", e.getMessage());
            return null;
        }
    }

    private List<TierRow> fetchRows(String scannerUrl, String fromSlot) throws Exception {
        List<TierRow> result = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        while (true) {
            // n=500 requests up to 500 rows per call to minimize round-trips
            ResponseEntity<String> resp = restTemplate.exchange(
                    scannerUrl + "?n=500", HttpMethod.GET, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) break;

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode rows = root.path("Row");
            if (rows.isMissingNode() || rows.isEmpty()) break;

            for (JsonNode row : rows) {
                TierRow parsed = parseRow(row, fromSlot);
                if (parsed != null) result.add(parsed);
            }
        }

        return result;
    }

    private void deleteScanner(String scannerUrl) {
        try {
            restTemplate.delete(scannerUrl);
        } catch (Exception ignored) {
        }
    }

    // ── Row parsing ──────────────────────────────────────────────────────────

    private TierRow parseRow(JsonNode rowNode, String fromSlot) {
        try {
            String key = new String(
                    Base64.getDecoder().decode(rowNode.get("key").asText()),
                    StandardCharsets.UTF_8);

            // Expected format: {empresaId}|{ramGb}|{stoGb}|{yyyyMMddHH}
            String[] parts = key.split("\\|");
            if (parts.length != 4) return null;

            String slot = parts[3];
            if (slot.compareTo(fromSlot) < 0) return null; // outside requested range

            long   count  = 0;
            double cpuAvg = 0, cpuMin = 0, cpuMax = 0;
            double ramAvg = 0, ramMin = 0, ramMax = 0;
            double stoAvg = 0, stoMin = 0, stoMax = 0;
            double batAvg = 0, batMin = 0, batMax = 0;
            double tmpAvg = 0, tmpMin = 0, tmpMax = 0;
            double strAvg = 0, strMin = 0, strMax = 0;

            for (JsonNode cell : rowNode.path("Cell")) {
                String col = new String(
                        Base64.getDecoder().decode(cell.get("column").asText()),
                        StandardCharsets.UTF_8);
                byte[] val = Base64.getDecoder().decode(cell.get("$").asText());

                // Values are stored by HBase Bytes.toBytes(): doubles as 8-byte big-endian IEEE 754,
                // longs as 8-byte big-endian two's complement.
                switch (col) {
                    case "m:count"   -> count  = ByteBuffer.wrap(val).getLong();
                    case "m:cpu_avg" -> cpuAvg = ByteBuffer.wrap(val).getDouble();
                    case "m:cpu_min" -> cpuMin = ByteBuffer.wrap(val).getDouble();
                    case "m:cpu_max" -> cpuMax = ByteBuffer.wrap(val).getDouble();
                    case "m:ram_avg" -> ramAvg = ByteBuffer.wrap(val).getDouble();
                    case "m:ram_min" -> ramMin = ByteBuffer.wrap(val).getDouble();
                    case "m:ram_max" -> ramMax = ByteBuffer.wrap(val).getDouble();
                    case "m:sto_avg" -> stoAvg = ByteBuffer.wrap(val).getDouble();
                    case "m:sto_min" -> stoMin = ByteBuffer.wrap(val).getDouble();
                    case "m:sto_max" -> stoMax = ByteBuffer.wrap(val).getDouble();
                    case "m:bat_avg" -> batAvg = ByteBuffer.wrap(val).getDouble();
                    case "m:bat_min" -> batMin = ByteBuffer.wrap(val).getDouble();
                    case "m:bat_max" -> batMax = ByteBuffer.wrap(val).getDouble();
                    case "m:tmp_avg" -> tmpAvg = ByteBuffer.wrap(val).getDouble();
                    case "m:tmp_min" -> tmpMin = ByteBuffer.wrap(val).getDouble();
                    case "m:tmp_max" -> tmpMax = ByteBuffer.wrap(val).getDouble();
                    case "m:str_avg" -> strAvg = ByteBuffer.wrap(val).getDouble();
                    case "m:str_min" -> strMin = ByteBuffer.wrap(val).getDouble();
                    case "m:str_max" -> strMax = ByteBuffer.wrap(val).getDouble();
                }
            }

            return new TierRow(slot, count,
                    cpuAvg, cpuMin, cpuMax,
                    ramAvg, ramMin, ramMax,
                    stoAvg, stoMin, stoMax,
                    batAvg, batMin, batMax,
                    tmpAvg, tmpMin, tmpMax,
                    strAvg, strMin, strMax);
        } catch (Exception e) {
            return null;
        }
    }

    // ── Aggregation across hardware tiers ────────────────────────────────────

    private List<HourlyAggregateDTO> aggregate(List<TierRow> rows) {
        // Group by hour slot, merge all hardware tiers into one weighted aggregate
        Map<String, List<TierRow>> bySlot = rows.stream()
                .collect(Collectors.groupingBy(r -> r.slot));

        return bySlot.entrySet().stream()
                .map(e -> mergeSlot(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(HourlyAggregateDTO::slot))
                .toList();
    }

    private HourlyAggregateDTO mergeSlot(String slot, List<TierRow> tiers) {
        long totalCount = tiers.stream().mapToLong(r -> r.count).sum();
        long weight = totalCount == 0 ? 1 : totalCount;

        // Weighted average for avg metrics, global min/max for bounds
        return new HourlyAggregateDTO(
                slot,
                tiers.stream().mapToDouble(r -> r.cpuAvg * r.count).sum() / weight,
                tiers.stream().mapToDouble(r -> r.cpuMin).min().orElse(0),
                tiers.stream().mapToDouble(r -> r.cpuMax).max().orElse(0),
                tiers.stream().mapToDouble(r -> r.ramAvg * r.count).sum() / weight,
                tiers.stream().mapToDouble(r -> r.ramMin).min().orElse(0),
                tiers.stream().mapToDouble(r -> r.ramMax).max().orElse(0),
                tiers.stream().mapToDouble(r -> r.stoAvg * r.count).sum() / weight,
                tiers.stream().mapToDouble(r -> r.stoMin).min().orElse(0),
                tiers.stream().mapToDouble(r -> r.stoMax).max().orElse(0),
                tiers.stream().mapToDouble(r -> r.batAvg * r.count).sum() / weight,
                tiers.stream().mapToDouble(r -> r.batMin).min().orElse(0),
                tiers.stream().mapToDouble(r -> r.batMax).max().orElse(0),
                tiers.stream().mapToDouble(r -> r.tmpAvg * r.count).sum() / weight,
                tiers.stream().mapToDouble(r -> r.tmpMin).min().orElse(0),
                tiers.stream().mapToDouble(r -> r.tmpMax).max().orElse(0),
                tiers.stream().mapToDouble(r -> r.strAvg * r.count).sum() / weight,
                tiers.stream().mapToDouble(r -> r.strMin).min().orElse(0),
                tiers.stream().mapToDouble(r -> r.strMax).max().orElse(0),
                totalCount
        );
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private record TierRow(
            String slot, long count,
            double cpuAvg, double cpuMin, double cpuMax,
            double ramAvg, double ramMin, double ramMax,
            double stoAvg, double stoMin, double stoMax,
            double batAvg, double batMin, double batMax,
            double tmpAvg, double tmpMin, double tmpMax,
            double strAvg, double strMin, double strMax) {
    }
}
