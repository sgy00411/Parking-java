package com.quaer_api.service;

import com.quaer_api.config.MqttProperties;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * MQTT客户端服务
 * 负责连接MQTT服务器、订阅主题、发布消息
 */
@Slf4j
@Service
public class MqttClientService {

    @Autowired
    private MqttProperties mqttProperties;

    @Autowired
    private MqttMessageHandler messageHandler;

    private MqttClient mqttClient;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 初始化MQTT客户端并连接
     */
    @PostConstruct
    public void init() {
        try {
            // 生成唯一的Client ID: parking_server_随机UUID后8位
            String uniqueClientId = mqttProperties.getClientId() + "_" +
                                   UUID.randomUUID().toString().substring(0, 8);

            log.info("=== 开始初始化MQTT客户端 ===");
            log.info("MQTT配置信息:");
            log.info("  Broker URL: {}", mqttProperties.getBrokerUrl());
            log.info("  原始 Client ID: {}", mqttProperties.getClientId());
            log.info("  唯一 Client ID: {}", uniqueClientId);
            log.info("  Username: {}", mqttProperties.getUsername());
            log.info("  订阅主题: {}", mqttProperties.getSubscribeTopics());
            log.info("  发布主题: {}", mqttProperties.getPublishTopic());
            log.info("  QoS级别: {}", mqttProperties.getQos());

            // 创建MQTT客户端，使用唯一的Client ID
            mqttClient = new MqttClient(
                    mqttProperties.getBrokerUrl(),
                    uniqueClientId,
                    new MemoryPersistence()
            );

            // 设置回调
            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    if (reconnect) {
                        log.info(">>> MQTT重新连接成功! 服务器: {} 时间: {}",
                                serverURI, getCurrentTime());
                        // 重连后重新订阅
                        subscribeTopics();
                    } else {
                        log.info(">>> MQTT首次连接成功! 服务器: {} 时间: {}",
                                serverURI, getCurrentTime());
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    log.error("!!! MQTT连接丢失! 时间: {} 原因: {}",
                            getCurrentTime(), cause.getMessage());
                    log.error("详细错误:", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    log.info("<<< 收到MQTT消息");
                    log.info("  主题: {}", topic);
                    log.info("  内容: {}", new String(message.getPayload(), StandardCharsets.UTF_8));
                    log.info("  QoS: {}", message.getQos());
                    log.info("  时间: {}", getCurrentTime());

                    // 调用消息处理器
                    messageHandler.handleMessage(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    try {
                        log.info(">>> 消息发送完成! Token: {} 时间: {}",
                                token.getMessageId(), getCurrentTime());
                    } catch (Exception e) {
                        log.error("获取Token信息失败", e);
                    }
                }
            });

            // 连接到MQTT服务器
            connect();

            log.info("=== MQTT客户端初始化完成 ===");

        } catch (Exception e) {
            log.error("!!! MQTT客户端初始化失败!", e);
            throw new RuntimeException("MQTT客户端初始化失败", e);
        }
    }

    /**
     * 连接到MQTT服务器
     */
    private void connect() {
        try {
            log.info("--- 正在连接MQTT服务器...");

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(mqttProperties.getUsername());
            options.setPassword(mqttProperties.getPassword().toCharArray());
            options.setCleanSession(mqttProperties.getConnection().isCleanSession());
            options.setConnectionTimeout(mqttProperties.getConnection().getConnectionTimeout());
            options.setKeepAliveInterval(mqttProperties.getConnection().getKeepAliveInterval());
            options.setAutomaticReconnect(mqttProperties.getConnection().isAutomaticReconnect());
            options.setMaxReconnectDelay(mqttProperties.getConnection().getMaxReconnectDelay());

            mqttClient.connect(options);

            log.info(">>> MQTT连接成功! 时间: {}", getCurrentTime());

            // 订阅主题
            subscribeTopics();

        } catch (MqttException e) {
            log.error("!!! MQTT连接失败! 时间: {} 错误码: {}",
                    getCurrentTime(), e.getReasonCode(), e);
            throw new RuntimeException("MQTT连接失败", e);
        }
    }

    /**
     * 订阅主题
     */
    private void subscribeTopics() {
        try {
            if (mqttProperties.getSubscribeTopics() != null &&
                !mqttProperties.getSubscribeTopics().isEmpty()) {

                for (String topic : mqttProperties.getSubscribeTopics()) {
                    mqttClient.subscribe(topic, mqttProperties.getQos());
                    log.info(">>> 订阅主题成功: {} QoS: {} 时间: {}",
                            topic, mqttProperties.getQos(), getCurrentTime());
                }
            }
        } catch (MqttException e) {
            log.error("!!! 订阅主题失败! 时间: {}", getCurrentTime(), e);
        }
    }

    /**
     * 发布消息
     * @param topic 主题
     * @param message 消息内容
     */
    public void publish(String topic, String message) {
        try {
            if (!mqttClient.isConnected()) {
                log.warn("!!! MQTT未连接,无法发送消息! 时间: {}", getCurrentTime());
                return;
            }

            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(message.getBytes(StandardCharsets.UTF_8));
            mqttMessage.setQos(mqttProperties.getQos());
            mqttMessage.setRetained(mqttProperties.isRetained());

            log.info(">>> 准备发送MQTT消息");
            log.info("  主题: {}", topic);
            log.info("  内容: {}", message);
            log.info("  QoS: {}", mqttProperties.getQos());
            log.info("  时间: {}", getCurrentTime());

            mqttClient.publish(topic, mqttMessage);

            log.info(">>> 消息发送成功! 时间: {}", getCurrentTime());

        } catch (MqttException e) {
            log.error("!!! 发布消息失败! 主题: {} 时间: {}",
                    topic, getCurrentTime(), e);
        }
    }

    /**
     * 发布消息到默认主题
     * @param message 消息内容
     */
    public void publishToDefaultTopic(String message) {
        publish(mqttProperties.getPublishTopic(), message);
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        boolean connected = mqttClient != null && mqttClient.isConnected();
        log.info("MQTT连接状态: {} 时间: {}", connected ? "已连接" : "未连接", getCurrentTime());
        return connected;
    }

    /**
     * 断开连接
     */
    @PreDestroy
    public void disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                log.info("--- 正在断开MQTT连接...");
                mqttClient.disconnect();
                mqttClient.close();
                log.info(">>> MQTT连接已断开! 时间: {}", getCurrentTime());
            }
        } catch (MqttException e) {
            log.error("!!! 断开MQTT连接失败! 时间: {}", getCurrentTime(), e);
        }
    }

    /**
     * 获取当前时间字符串
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    /**
     * 获取MQTT客户端
     */
    public MqttClient getMqttClient() {
        return mqttClient;
    }
}
