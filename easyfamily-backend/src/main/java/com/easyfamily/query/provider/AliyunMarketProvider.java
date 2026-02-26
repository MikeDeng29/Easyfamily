package com.easyfamily.query.provider;

import com.easyfamily.common.exception.BusinessException;
import com.easyfamily.query.config.AliyunMarketProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class AliyunMarketProvider implements BindingQueryProvider {

    private final AliyunMarketProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public AliyunMarketProvider(AliyunMarketProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String key() {
        return "aliyun-market";
    }

    @Override
    public ProviderResult queryBinding(String phone, String queryType) {
        ensureConfigured();
        URI uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path(properties.path())
                .queryParam("phone", phone)
                .queryParam("queryType", queryType)
                .build(true)
                .toUri();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "APPCODE " + properties.appCode())
                .header("Accept", "application/json")
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("ALIYUN_MARKET_HTTP_ERROR", "aliyun market status=" + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            boolean bankBound = parseBoolean(root, "bankBound");
            boolean socialBound = parseBoolean(root, "socialBound");
            return new ProviderResult(bankBound, socialBound, "provider-aliyun-market");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("ALIYUN_MARKET_CALL_ERROR", ex.getMessage());
        }
    }

    private void ensureConfigured() {
        if (isBlank(properties.baseUrl()) || isBlank(properties.path()) || isBlank(properties.appCode())) {
            throw new BusinessException("ALIYUN_MARKET_CONFIG_MISSING", "aliyun market configuration is incomplete");
        }
    }

    private boolean parseBoolean(JsonNode node, String field) {
        JsonNode value = node.path("data").path(field);
        if (value.isMissingNode() || value.isNull()) {
            return false;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        return "1".equals(value.asText()) || "true".equalsIgnoreCase(value.asText());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
