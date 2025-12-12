package com.quaer_api.service;

import com.quaer_api.config.SquareProperties;
import com.quaer_api.dto.SquareDeviceDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.UUID;

/**
 * Square设备服务
 */
@Slf4j
@Service
public class SquareDeviceService {

    @Autowired
    private SquareProperties squareProperties;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建设备配对代码
     */
    public SquareDeviceDTO createDeviceCode(String locationId, String deviceName) {
        try {
            log.info("开始创建设备配对代码 - locationId: {}, deviceName: {}", locationId, deviceName);

            // 构建请求URL
            String url = squareProperties.getBaseUrl() + "/v2/devices/codes";

            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("idempotency_key", UUID.randomUUID().toString());

            ObjectNode deviceCode = objectMapper.createObjectNode();
            deviceCode.put("product_type", "TERMINAL_API");
            deviceCode.put("name", deviceName);
            deviceCode.put("location_id", locationId);

            requestBody.set("device_code", deviceCode);

            String requestBodyStr = objectMapper.writeValueAsString(requestBody);
            log.info("请求体: {}", requestBodyStr);

            // 构建HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Square-Version", squareProperties.getApiVersion())
                    .header("Authorization", "Bearer " + squareProperties.getAccessToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyStr))
                    .build();

            // 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Square API响应状态码: {}", response.statusCode());
            log.info("Square API响应内容: {}", response.body());

            if (response.statusCode() == 200) {
                // 解析响应
                JsonNode rootNode = objectMapper.readTree(response.body());
                JsonNode deviceCodeNode = rootNode.get("device_code");

                if (deviceCodeNode != null) {
                    SquareDeviceDTO device = new SquareDeviceDTO();
                    device.setDeviceCodeId(getTextValue(deviceCodeNode, "id"));
                    device.setCode(getTextValue(deviceCodeNode, "code"));
                    device.setName(getTextValue(deviceCodeNode, "name"));
                    device.setLocationId(getTextValue(deviceCodeNode, "location_id"));
                    device.setProductType(getTextValue(deviceCodeNode, "product_type"));
                    device.setPairStatus(getTextValue(deviceCodeNode, "status"));

                    String createdAt = getTextValue(deviceCodeNode, "created_at");
                    if (createdAt != null) {
                        device.setCreatedAt(parseDateTime(createdAt));
                    }

                    log.info("成功创建设备配对代码: {}", device.getCode());
                    return device;
                }
            }

            log.error("创建设备配对代码失败, 状态码: {}, 响应: {}", response.statusCode(), response.body());
            throw new RuntimeException("Failed to create device code: " + response.body());

        } catch (Exception e) {
            log.error("创建设备配对代码失败", e);
            throw new RuntimeException("Failed to create device code", e);
        }
    }

    /**
     * 获取指定位置的设备列表
     */
    public List<SquareDeviceDTO> getDevicesByLocation(String locationId) {
        try {
            log.info("开始获取位置设备列表 - locationId: {}", locationId);

            // 构建请求URL - 使用查询参数过滤位置
            String url = squareProperties.getBaseUrl() + "/v2/devices?location_id=" + locationId;

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
                JsonNode devicesNode = rootNode.get("devices");

                List<SquareDeviceDTO> devices = new ArrayList<>();

                if (devicesNode != null && devicesNode.isArray()) {
                    for (JsonNode deviceNode : devicesNode) {
                        SquareDeviceDTO device = parseDevice(deviceNode);
                        devices.add(device);
                    }
                }

                log.info("成功获取{}个设备", devices.size());
                return devices;
            } else {
                log.error("获取设备列表失败, 状态码: {}, 响应: {}", response.statusCode(), response.body());
                throw new RuntimeException("Failed to fetch devices: " + response.body());
            }

        } catch (Exception e) {
            log.error("获取设备列表失败", e);
            throw new RuntimeException("Failed to fetch devices", e);
        }
    }

    /**
     * 获取所有设备列表
     */
    public List<SquareDeviceDTO> getAllDevices() {
        try {
            log.info("开始获取所有设备列表");

            // 构建请求URL
            String url = squareProperties.getBaseUrl() + "/v2/devices";

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
                JsonNode devicesNode = rootNode.get("devices");

                List<SquareDeviceDTO> devices = new ArrayList<>();

                if (devicesNode != null && devicesNode.isArray()) {
                    for (JsonNode deviceNode : devicesNode) {
                        SquareDeviceDTO device = parseDevice(deviceNode);
                        devices.add(device);
                    }
                }

                log.info("成功获取{}个设备", devices.size());
                return devices;
            } else {
                log.error("获取设备列表失败, 状态码: {}, 响应: {}", response.statusCode(), response.body());
                throw new RuntimeException("Failed to fetch devices: " + response.body());
            }

        } catch (Exception e) {
            log.error("获取设备列表失败", e);
            throw new RuntimeException("Failed to fetch devices", e);
        }
    }

    /**
     * 解析设备信息
     */
    private SquareDeviceDTO parseDevice(JsonNode deviceNode) {
        SquareDeviceDTO device = new SquareDeviceDTO();

        // 基本信息
        device.setId(getTextValue(deviceNode, "id"));

        // 解析 attributes 节点
        JsonNode attributesNode = deviceNode.get("attributes");
        if (attributesNode != null) {
            device.setName(getTextValue(attributesNode, "name"));
            device.setProductType(getTextValue(attributesNode, "type"));

            String updatedAt = getTextValue(attributesNode, "updated_at");
            if (updatedAt != null) {
                device.setCreatedAt(parseDateTime(updatedAt));
            }
        }

        // 解析 status 节点
        JsonNode statusNode = deviceNode.get("status");
        if (statusNode != null) {
            String statusCategory = getTextValue(statusNode, "category");
            device.setStatus(statusCategory); // AVAILABLE, OFFLINE 等
        }

        // 解析组件获取位置信息
        JsonNode componentsNode = deviceNode.get("components");
        if (componentsNode != null && componentsNode.isArray()) {
            for (JsonNode component : componentsNode) {
                String type = getTextValue(component, "type");
                if ("APPLICATION".equals(type)) {
                    JsonNode applicationDetailsNode = component.get("application_details");
                    if (applicationDetailsNode != null) {
                        device.setLocationId(getTextValue(applicationDetailsNode, "session_location"));
                        device.setDeviceCodeId(getTextValue(applicationDetailsNode, "device_code_id"));
                    }
                }
            }
        }

        return device;
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
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("解析时间失败: {}", dateTimeStr, e);
            return null;
        }
    }
}
