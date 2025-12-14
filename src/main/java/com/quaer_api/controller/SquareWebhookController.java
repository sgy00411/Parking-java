package com.quaer_api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quaer_api.entity.PaymentOrder;
import com.quaer_api.service.SquareWebhookService;
import com.quaer_api.util.SquareSignatureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Square Webhook 事件接收控制器
 * 接收Square支付平台的Webhook通知并记录到日志和数据库
 */
@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class SquareWebhookController {

    private final SquareSignatureValidator signatureValidator;
    private final ObjectMapper objectMapper;
    private final SquareWebhookService webhookService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${square.webhook.url:https://test001.cn/api/webhook}")
    private String webhookUrl;

    /**
     * 接收 Square webhook 事件
     */
    @PostMapping
    public ResponseEntity<String> receiveWebhook(
            @RequestHeader(value = "x-square-signature", required = false) String signature,
            HttpServletRequest request) {

        String payload = null;

        try {
            payload = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

            log.info("=".repeat(80));
            log.info("收到 Square Webhook 事件 - 时间: {}", getCurrentTime());
            log.info("=".repeat(80));

            // 🔥 动态构建 Webhook URL
            String scheme = request.getScheme(); // http or https
            String serverName = request.getServerName(); // 域名
            int serverPort = request.getServerPort(); // 端口
            String contextPath = request.getContextPath(); // 上下文路径
            String servletPath = request.getServletPath(); // Servlet路径

            String dynamicWebhookUrl;
            // 如果是标准端口(80/443),不包含端口号
            if ((scheme.equals("http") && serverPort == 80) ||
                    (scheme.equals("https") && serverPort == 443)) {
                dynamicWebhookUrl = scheme + "://" + serverName + contextPath + servletPath;
            } else {
                dynamicWebhookUrl = scheme + "://" + serverName + ":" + serverPort + contextPath + servletPath;
            }

            log.info("📝 动态构建的 Webhook URL: {}", dynamicWebhookUrl);
            log.info("📝 配置文件中的 URL: {}", webhookUrl);

            if (signature == null || signature.isBlank()) {
                log.error("❌ 缺少签名头 x-square-signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing signature");
            }

            log.info("📝 收到签名: {}", signature);

            // 🔥 使用动态 URL 进行验证
            boolean isValid = signatureValidator.isValidSignature(payload, signature, dynamicWebhookUrl);

            if (!isValid) {
                log.error("❌ 签名验证失败,拒绝请求");
                log.error("尝试使用配置文件 URL 再次验证...");

                // 尝试使用配置文件中的 URL
                isValid = signatureValidator.isValidSignature(payload, signature, webhookUrl);

                if (!isValid) {
                    log.error("❌ 两种 URL 都验证失败");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
                }
            }

            log.info("✅ 签名验证成功");

            // 解析 JSON
            JsonNode jsonPayload = objectMapper.readTree(payload);

            // 提取基本信息
            String merchantId = jsonPayload.path("merchant_id").asText();
            String type = jsonPayload.path("type").asText();
            String eventId = jsonPayload.path("event_id").asText();
            String createdAt = jsonPayload.path("created_at").asText();

            log.info("=".repeat(80));
            log.info("Webhook 事件详情:");
            log.info("  商户 ID: {}", merchantId);
            log.info("  事件类型: {}", type);
            log.info("  事件 ID: {}", eventId);
            log.info("  创建时间: {}", createdAt);
            log.info("=".repeat(80));

            // 根据事件类型处理并保存到数据库
            PaymentOrder savedOrder = null;
            switch (type) {
                case "payment.created":
                    handlePaymentCreated(jsonPayload);
                    savedOrder = webhookService.handlePaymentCreated(jsonPayload);
                    break;

                case "payment.updated":
                    handlePaymentUpdated(jsonPayload);
                    savedOrder = webhookService.handlePaymentUpdated(jsonPayload);
                    break;

                default:
                    log.warn("⚠️ 未处理的事件类型: {}", type);
            }

            // 显示完整的 JSON 内容
            log.info("-".repeat(80));
            log.info("完整 Webhook 数据:");
            log.info(jsonPayload.toPrettyString());
            log.info("=".repeat(80));

            if (savedOrder != null) {
                log.info("💾 数据库记录 ID: {}", savedOrder.getId());
            }

            return ResponseEntity.ok("Webhook received successfully");

        } catch (IOException e) {
            log.error("❌ 读取请求体失败", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request body");

        } catch (Exception e) {
            log.error("❌ 处理 webhook 时发生错误", e);
            log.error("Payload: {}", payload);
            // 即使处理失败，也返回 200，避免 Square 无限重试
            return ResponseEntity.ok("Webhook received but processing failed: " + e.getMessage());
        }
    }

    /**
     * 处理支付创建事件
     */
    private void handlePaymentCreated(JsonNode payload) {
        log.info("📝 处理支付创建事件");

        try {
            // 提取支付数据
            JsonNode data = payload.path("data");
            JsonNode object = data.path("object");
            JsonNode payment = object.path("payment");

            String paymentId = payment.path("id").asText();
            String orderId = payment.path("order_id").asText(null);
            String status = payment.path("status").asText();
            String sourceType = payment.path("source_type").asText(null);

            // 金额信息
            JsonNode amountMoney = payment.path("amount_money");
            long amount = amountMoney.path("amount").asLong(0L);
            String currency = amountMoney.path("currency").asText("CAD");

            // 收据信息
            String receiptNumber = payment.path("receipt_number").asText(null);
            String receiptUrl = payment.path("receipt_url").asText(null);

            log.info("=".repeat(80));
            log.info("💰 支付创建信息:");
            log.info("  支付 ID: {}", paymentId);
            log.info("  订单 ID: {}", orderId);
            log.info("  支付状态: {}", status);
            log.info("  支付来源: {}", sourceType);
            log.info("  支付金额: {} {}", formatAmount(amount), currency);
            log.info("  收据编号: {}", receiptNumber);
            log.info("  收据链接: {}", receiptUrl);

            // 卡片信息
            if ("CARD".equals(sourceType)) {
                JsonNode cardDetails = payment.path("card_details");
                if (!cardDetails.isMissingNode()) {
                    String cardBrand = cardDetails.path("card").path("card_brand").asText(null);
                    String last4 = cardDetails.path("card").path("last_4").asText(null);
                    String cardType = cardDetails.path("card").path("card_type").asText(null);
                    String entryMethod = cardDetails.path("entry_method").asText(null);

                    log.info("  卡片品牌: {}", cardBrand);
                    log.info("  卡号后4位: {}", last4);
                    log.info("  卡片类型: {}", cardType);
                    log.info("  刷卡方式: {}", entryMethod);

                    // 判断支付来源
                    String paymentSource = "ONLINE";
                    if ("EMV".equals(entryMethod) || "CONTACTLESS".equals(entryMethod)) {
                        paymentSource = "TERMINAL";
                    }
                    log.info("  支付渠道: {}", paymentSource);
                }
            }

            log.info("=".repeat(80));
            log.info("✅ 支付创建事件处理完成");

        } catch (Exception e) {
            log.error("❌ 处理支付创建事件失败", e);
        }
    }

    /**
     * 处理支付更新事件
     */
    private void handlePaymentUpdated(JsonNode payload) {
        log.info("📄 处理支付更新事件");

        try {
            JsonNode data = payload.path("data");
            JsonNode object = data.path("object");
            JsonNode payment = object.path("payment");

            String paymentId = payment.path("id").asText();
            String status = payment.path("status").asText();
            String updatedAt = payment.path("updated_at").asText();

            // 金额信息
            JsonNode amountMoney = payment.path("amount_money");
            long amount = amountMoney.path("amount").asLong(0L);
            String currency = amountMoney.path("currency").asText("CAD");

            log.info("=".repeat(80));
            log.info("🔄 支付更新信息:");
            log.info("  支付 ID: {}", paymentId);
            log.info("  新状态: {}", status);
            log.info("  支付金额: {} {}", formatAmount(amount), currency);
            log.info("  更新时间: {}", updatedAt);
            log.info("=".repeat(80));
            log.info("✅ 支付更新事件处理完成");

        } catch (Exception e) {
            log.error("❌ 处理支付更新事件失败", e);
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
     * 健康检查端点
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        log.info("收到健康检查请求 - 时间: {}", getCurrentTime());
        return ResponseEntity.ok("Webhook endpoint is running");
    }
}
