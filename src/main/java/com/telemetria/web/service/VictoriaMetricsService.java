package com.telemetria.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class VictoriaMetricsService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.victoriametrics.url:http://localhost:8428}")
    private String victoriaMetricsUrl;

    public List<String> findPortatilesByEmpresa(Long empresaId) {
        if (empresaId == null) {
            return Collections.emptyList();
        }

        String baseUrl = victoriaMetricsUrl.endsWith("/")
                ? victoriaMetricsUrl.substring(0, victoriaMetricsUrl.length() - 1)
                : victoriaMetricsUrl;

        String match = "processor_info{empresa=\"" + empresaId + "\"}";
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/v1/label/portatil/values")
                .queryParam("match[]", match)
                .build()
                .encode()
                .toUri();

        try {
            JsonNode response = restTemplate.getForObject(uri, JsonNode.class);
            if (response == null || !"success".equals(response.path("status").asText())) {
                return Collections.emptyList();
            }

            JsonNode data = response.path("data");
            if (!data.isArray()) {
                return Collections.emptyList();
            }

            List<String> portatiles = new ArrayList<>();
            data.forEach(node -> {
                String portatil = node.asText(null);
                if (portatil != null && !portatil.isBlank()) {
                    portatiles.add(portatil);
                }
            });
            portatiles.sort(String::compareToIgnoreCase);
            return portatiles;
        } catch (RestClientException | IllegalArgumentException ex) {
            return Collections.emptyList();
        }
    }
}
