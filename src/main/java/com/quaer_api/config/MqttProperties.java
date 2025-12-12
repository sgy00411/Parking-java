package com.quaer_api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MQTT配置属性类
 * 从application.yml中读取mqtt配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

    /**
     * MQTT服务器地址 tcp://ip:port
     */
    private String brokerUrl;

    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 订阅的主题列表
     */
    private List<String> subscribeTopics;

    /**
     * 发布的主题
     */
    private String publishTopic;

    /**
     * QoS级别
     */
    private int qos = 1;

    /**
     * 是否保留消息
     */
    private boolean retained = false;

    /**
     * 连接配置
     */
    private ConnectionConfig connection = new ConnectionConfig();

    @Data
    public static class ConnectionConfig {
        /**
         * 清除会话
         */
        private boolean cleanSession = true;

        /**
         * 连接超时时间(秒)
         */
        private int connectionTimeout = 30;

        /**
         * 心跳间隔(秒)
         */
        private int keepAliveInterval = 60;

        /**
         * 自动重连
         */
        private boolean automaticReconnect = true;

        /**
         * 最大重连延迟(毫秒)
         */
        private int maxReconnectDelay = 128000;
    }
}
