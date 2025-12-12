package com.quaer_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quaer_api.config.SquareProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Square终端支付服务
 * 用于发起POS机终端支付
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SquareTerminalService {

    private final SquareProperties squareProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 发起终端支付
     *
     * @param amountInCents 支付金额（分）
     * @return 支付响应
     */
    public String createTerminalCheckout(long amountInCents) {
        // 使用配置文件中的默认设备ID
        return createTerminalCheckout(amountInCents, squareProperties.getDeviceId());
    }

    /**
     * 发起终端支付（指定设备ID）
     *
     * @param amountInCents 支付金额（分）
     * @param deviceId 支付设备ID
     * @return 支付响应
     */
    public String createTerminalCheckout(long amountInCents, String deviceId) {
        try {
            log.info("=".repeat(80));
            log.info("准备发起 Square 终端支付 - 时间: {}", getCurrentTime());
            log.info("=".repeat(80));

            // 构建请求URL
            String url = squareProperties.getBaseUrl() + "/v2/terminals/checkouts";

            // 生成幂等性密钥
            String idempotencyKey = UUID.randomUUID().toString();

            // 构建请求体
            ObjectNode request = objectMapper.createObjectNode();
            ObjectNode checkout = objectMapper.createObjectNode();
            ObjectNode amountMoney = objectMapper.createObjectNode();
            ObjectNode deviceOptions = objectMapper.createObjectNode();

            // 设置金额
            amountMoney.put("currency", squareProperties.getCurrency());
            amountMoney.put("amount", amountInCents);

            // 设置设备ID
            deviceOptions.put("device_id", deviceId);

            // 组装checkout
            checkout.set("amount_money", amountMoney);
            checkout.set("device_options", deviceOptions);

            // 组装请求
            request.set("checkout", checkout);
            request.put("idempotency_key", idempotencyKey);

            log.info("请求信息:");
            log.info("  API URL: {}", url);
            log.info("  API Version: {}", squareProperties.getApiVersion());
            log.info("  设备ID: {}", deviceId);
            log.info("  支付金额: {} {}", formatAmount(amountInCents), squareProperties.getCurrency());
            log.info("  幂等性密钥: {}", idempotencyKey);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Square-Version", squareProperties.getApiVersion());
            headers.set("Authorization", "Bearer " + squareProperties.getAccessToken());

            // 创建HTTP请求实体
            HttpEntity<String> requestEntity = new HttpEntity<>(
                    objectMapper.writeValueAsString(request),
                    headers
            );

            log.info("-".repeat(80));
            log.info("发送请求到 Square API...");
            log.info("请求体:");
            log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));
            log.info("-".repeat(80));

            // 发送POST请求
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            log.info("=".repeat(80));
            log.info("Square API 响应:");
            log.info("  HTTP Status: {}", response.getStatusCode());
            log.info("  响应时间: {}", getCurrentTime());
            log.info("-".repeat(80));

            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                String responseBody = response.getBody();
                log.info("响应内容:");

                if (responseBody != null) {
                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    log.info(jsonResponse.toPrettyString());

                    // 提取关键信息
                    JsonNode checkoutNode = jsonResponse.path("checkout");
                    if (!checkoutNode.isMissingNode()) {
                        String checkoutId = checkoutNode.path("id").asText();
                        String status = checkoutNode.path("status").asText();

                        log.info("-".repeat(80));
                        log.info("✅ 终端支付请求成功!");
                        log.info("  Checkout ID: {}", checkoutId);
                        log.info("  状态: {}", status);
                        log.info("  设备将显示支付界面,等待顾客操作...");
                    }
                } else {
                    log.info("响应体为空");
                }

                log.info("=".repeat(80));
                return responseBody;

            } else {
                log.error("❌ Square API 返回错误状态: {}", response.getStatusCode());
                log.error("响应内容: {}", response.getBody());
                log.info("=".repeat(80));
                return "Error: " + response.getStatusCode();
            }

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("❌ 发起终端支付失败!", e);
            log.error("错误信息: {}", e.getMessage());
            log.error("=".repeat(80));
            return "Exception: " + e.getMessage();
        }
    }

    /**
     * 格式化金额（分转元）
     */
    private String formatAmount(long amountInCents) {
        return String.format("$%.2f", amountInCents / 100.0);
    }

    /**
     * 获取当前时间字符串
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}
