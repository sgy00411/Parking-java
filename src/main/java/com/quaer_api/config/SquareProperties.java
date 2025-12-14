package com.quaer_api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Square支付配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "square")
public class SquareProperties {

    /**
     * API版本
     */
    private String apiVersion;

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * API基础URL
     */
    private String baseUrl;

    /**
     * 商户位置ID
     */
    private String locationId;

    /**
     * 环境
     */
    private String environment;

    /**
     * 应用ID
     */
    private String applicationId;

    /**
     * 终端设备ID
     */
    private String deviceId;

    /**
     * 默认货币
     */
    private String currency;

    /**
     * Webhook配置
     */
    private WebhookConfig webhook;

    @Data
    public static class WebhookConfig {
        /**
         * Webhook URL
         */
        private String url;

        /**
         * 签名密钥
         */
        private String signatureKey;

        /**
         * Subscription ID
         */
        private String subscriptionId;
    }
}
