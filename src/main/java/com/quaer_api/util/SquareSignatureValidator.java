package com.quaer_api.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Square Webhook 签名验证工具
 * 用于验证来自Square的Webhook请求的真实性
 */
@Slf4j
@Component
public class SquareSignatureValidator {

    @Value("${square.webhook.signature-key}")
    private String signatureKey;

    /**
     * 验证 Square webhook 签名
     *
     * @param payload 原始请求体（JSON字符串）
     * @param signatureHeader Square 发送的签名（x-square-signature）
     * @param notificationUrl webhook URL（用于组合验证字符串）
     * @return true 如果签名有效
     */
    public boolean isValidSignature(String payload, String signatureHeader, String notificationUrl) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("❌ 缺少签名头 x-square-signature");
            return false;
        }

        try {
            // Square 使用 HMAC-SHA1 算法进行签名验证
            // stringToSign = notificationUrl + payload
            String stringToSign = notificationUrl + payload;

            log.debug("签名验证字符串长度: {}", stringToSign.length());
            log.debug("使用的签名密钥长度: {}", signatureKey.length());

            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    signatureKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA1"
            );
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String computedSignature = Base64.getEncoder().encodeToString(hash);

            log.debug("计算的签名: {}", computedSignature);
            log.debug("接收的签名: {}", signatureHeader);

            boolean isValid = computedSignature.equals(signatureHeader);

            if (isValid) {
                log.info("✅ Webhook 签名验证成功");
            } else {
                log.error("❌ Webhook 签名验证失败");
                log.error("期望签名: {}", computedSignature);
                log.error("实际签名: {}", signatureHeader);
            }

            return isValid;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("❌ 签名验证过程中发生错误", e);
            return false;
        }
    }
}
