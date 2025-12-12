package com.quaer_api.service;

import com.quaer_api.config.SquareProperties;
import com.quaer_api.dto.SquareLocationDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Square位置服务
 */
@Slf4j
@Service
public class SquareLocationService {

    @Autowired
    private SquareProperties squareProperties;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取所有位置列表
     */
    public List<SquareLocationDTO> getLocations() {
        try {
            log.info("开始获取Square位置列表");

            // 构建请求URL
            String url = squareProperties.getBaseUrl() + "/v2/locations";

            // 构建HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Square-Version", squareProperties.getApiVersion())
                    .header("Authorization", "Bearer " + squareProperties.getAccessToken())
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            // 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Square API响应状态码: {}", response.statusCode());

            if (response.statusCode() == 200) {
                // 解析响应
                JsonNode rootNode = objectMapper.readTree(response.body());
                JsonNode locationsNode = rootNode.get("locations");

                List<SquareLocationDTO> locations = new ArrayList<>();

                if (locationsNode != null && locationsNode.isArray()) {
                    for (JsonNode locationNode : locationsNode) {
                        SquareLocationDTO location = parseLocation(locationNode);
                        locations.add(location);
                    }
                }

                log.info("成功获取{}个位置", locations.size());
                return locations;
            } else {
                log.error("获取位置列表失败, 状态码: {}, 响应: {}", response.statusCode(), response.body());
                throw new RuntimeException("Failed to fetch locations: " + response.body());
            }

        } catch (Exception e) {
            log.error("获取Square位置列表失败", e);
            throw new RuntimeException("Failed to fetch Square locations", e);
        }
    }

    /**
     * 解析位置信息
     */
    private SquareLocationDTO parseLocation(JsonNode locationNode) {
        SquareLocationDTO location = new SquareLocationDTO();

        // 基本信息
        location.setId(getTextValue(locationNode, "id"));
        location.setName(getTextValue(locationNode, "name"));
        location.setBusinessName(getTextValue(locationNode, "business_name"));
        location.setMerchantId(getTextValue(locationNode, "merchant_id"));
        location.setStatus(getTextValue(locationNode, "status"));
        location.setCurrency(getTextValue(locationNode, "currency"));
        location.setTimezone(getTextValue(locationNode, "timezone"));
        location.setMcc(getTextValue(locationNode, "mcc"));

        // 判断是否为当前配置的位置
        location.setIsCurrent(location.getId().equals(squareProperties.getLocationId()));

        // 解析地址
        JsonNode addressNode = locationNode.get("address");
        if (addressNode != null) {
            SquareLocationDTO.Address address = new SquareLocationDTO.Address();
            address.setAddressLine1(getTextValue(addressNode, "address_line_1"));
            address.setAddressLine2(getTextValue(addressNode, "address_line_2"));
            address.setLocality(getTextValue(addressNode, "locality"));
            address.setAdministrativeDistrictLevel1(getTextValue(addressNode, "administrative_district_level_1"));
            address.setPostalCode(getTextValue(addressNode, "postal_code"));
            address.setCountry(getTextValue(addressNode, "country"));
            location.setAddress(address);
        }

        // 解析坐标
        JsonNode coordinatesNode = locationNode.get("coordinates");
        if (coordinatesNode != null) {
            SquareLocationDTO.Coordinates coordinates = new SquareLocationDTO.Coordinates();
            if (coordinatesNode.has("latitude")) {
                coordinates.setLatitude(coordinatesNode.get("latitude").asDouble());
            }
            if (coordinatesNode.has("longitude")) {
                coordinates.setLongitude(coordinatesNode.get("longitude").asDouble());
            }
            location.setCoordinates(coordinates);
        }

        // 解析功能列表
        JsonNode capabilitiesNode = locationNode.get("capabilities");
        if (capabilitiesNode != null && capabilitiesNode.isArray()) {
            List<String> capabilities = new ArrayList<>();
            for (JsonNode capability : capabilitiesNode) {
                capabilities.add(capability.asText());
            }
            location.setCapabilities(capabilities);
        }

        // 解析时间
        String createdAt = getTextValue(locationNode, "created_at");
        if (createdAt != null) {
            location.setCreatedAt(parseDateTime(createdAt));
        }

        String updatedAt = getTextValue(locationNode, "updated_at");
        if (updatedAt != null) {
            location.setUpdatedAt(parseDateTime(updatedAt));
        }

        return location;
    }

    /**
     * 获取文本值
     */
    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
    }

    /**
     * 解析ISO 8601时间格式
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            // Square API返回的是ISO 8601格式，例如: "2024-01-01T12:00:00Z"
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("解析时间失败: {}", dateTimeStr, e);
            return null;
        }
    }

    /**
     * 根据ID获取位置信息
     */
    public SquareLocationDTO getLocationById(String locationId) {
        try {
            log.info("开始获取位置信息: {}", locationId);

            // 构建请求URL
            String url = squareProperties.getBaseUrl() + "/v2/locations/" + locationId;

            // 构建HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Square-Version", squareProperties.getApiVersion())
                    .header("Authorization", "Bearer " + squareProperties.getAccessToken())
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            // 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                JsonNode locationNode = rootNode.get("location");

                if (locationNode != null) {
                    return parseLocation(locationNode);
                }
            }

            throw new RuntimeException("Location not found: " + locationId);

        } catch (Exception e) {
            log.error("获取位置信息失败: {}", locationId, e);
            throw new RuntimeException("Failed to fetch location", e);
        }
    }
}
