package com.quaer_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
 * Square在线支付服务
 * 用于生成在线支付链接和二维码
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SquareOnlinePaymentService {

    private final SquareProperties squareProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 创建在线支付链接
     *
     * @param amountInCents 支付金额（分）
     * @param description 支付描述（例如：车牌号）
     * @param locationId 商户位置ID（如果为null则使用配置文件默认值）
     * @return 包含支付链接和订单ID的响应
     */
    public SquareOnlinePaymentResponse createPaymentLink(long amountInCents, String description, String locationId) {
        try {
            log.info("=".repeat(80));
            log.info("准备创建 Square 在线支付链接 - 时间: {}", getCurrentTime());
            log.info("=".repeat(80));

            // 构建请求URL
            String url = squareProperties.getBaseUrl() + "/v2/online-checkout/payment-links";

            // 生成幂等性密钥
            String idempotencyKey = UUID.randomUUID().toString();

            // 生成订单ID
            String orderId = "ORDER-" + System.currentTimeMillis();

            // 构建请求体
            ObjectNode request = objectMapper.createObjectNode();
            ObjectNode quickPay = objectMapper.createObjectNode();
            ObjectNode priceMoney = objectMapper.createObjectNode();

            // 设置金额
            priceMoney.put("currency", squareProperties.getCurrency());
            priceMoney.put("amount", amountInCents);

            // 确定使用的location_id：优先使用传入的值，否则使用配置文件默认值
            String useLocationId = (locationId != null && !locationId.trim().isEmpty())
                ? locationId
                : squareProperties.getLocationId();

            // 设置quick_pay
            quickPay.put("name", description != null ? description : "停车费");
            quickPay.put("location_id", useLocationId);
            quickPay.set("price_money", priceMoney);

            // 组装请求
            request.set("quick_pay", quickPay);
            request.put("idempotency_key", idempotencyKey);

            log.info("请求信息:");
            log.info("  API URL: {}", url);
            log.info("  API Version: {}", squareProperties.getApiVersion());
            log.info("  Location ID: {}", useLocationId);
            log.info("  支付金额: {} {}", formatAmount(amountInCents), squareProperties.getCurrency());
            log.info("  支付描述: {}", description);
            log.info("  订单ID: {}", orderId);
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
                    JsonNode paymentLinkNode = jsonResponse.path("payment_link");
                    if (!paymentLinkNode.isMissingNode()) {
                        String paymentLinkId = paymentLinkNode.path("id").asText();
                        String paymentUrl = paymentLinkNode.path("url").asText();
                        String longUrl = paymentLinkNode.path("long_url").asText();

                        // 提取Square自动生成的order_id（如果有）
                        String squareOrderId = paymentLinkNode.path("order_id").asText(null);

                        log.info("-".repeat(80));
                        log.info("✅ 在线支付链接创建成功!");
                        log.info("  Payment Link ID: {}", paymentLinkId);
                        log.info("  Square Order ID: {}", squareOrderId);
                        log.info("  支付URL (短): {}", paymentUrl);
                        log.info("  支付URL (长): {}", longUrl);
                        log.info("-".repeat(80));

                        // 返回响应对象
                        SquareOnlinePaymentResponse result = new SquareOnlinePaymentResponse();
                        result.setSuccess(true);
                        result.setPaymentLinkId(paymentLinkId);
                        result.setPaymentUrl(paymentUrl);
                        result.setLongUrl(longUrl);
                        result.setOrderId(squareOrderId != null ? squareOrderId : orderId);
                        result.setAmountInCents(amountInCents);
                        result.setDescription(description);

                        log.info("=".repeat(80));
                        return result;
                    }
                } else {
                    log.info("响应体为空");
                }
            } else {
                log.error("❌ Square API 返回错误状态: {}", response.getStatusCode());
                log.error("响应内容: {}", response.getBody());
            }

            log.info("=".repeat(80));

            // 返回失败响应
            SquareOnlinePaymentResponse result = new SquareOnlinePaymentResponse();
            result.setSuccess(false);
            return result;

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("❌ 创建在线支付链接失败!", e);
            log.error("错误信息: {}", e.getMessage());
            log.error("=".repeat(80));

            // 返回失败响应
            SquareOnlinePaymentResponse result = new SquareOnlinePaymentResponse();
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
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

    /**
     * Square在线支付响应类
     */
    public static class SquareOnlinePaymentResponse {
        private boolean success;
        private String paymentLinkId;
        private String paymentUrl;
        private String longUrl;
        private String orderId;
        private long amountInCents;
        private String description;
        private String errorMessage;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getPaymentLinkId() {
            return paymentLinkId;
        }

        public void setPaymentLinkId(String paymentLinkId) {
            this.paymentLinkId = paymentLinkId;
        }

        public String getPaymentUrl() {
            return paymentUrl;
        }

        public void setPaymentUrl(String paymentUrl) {
            this.paymentUrl = paymentUrl;
        }

        public String getLongUrl() {
            return longUrl;
        }

        public void setLongUrl(String longUrl) {
            this.longUrl = longUrl;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public long getAmountInCents() {
            return amountInCents;
        }

        public void setAmountInCents(long amountInCents) {
            this.amountInCents = amountInCents;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
