package com.quaer_api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT配置类
 * 启用MQTT配置属性
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MqttProperties.class)
public class MqttConfig {

    public MqttConfig(MqttProperties mqttProperties) {
        log.info("=== 加载MQTT配置 ===");
        log.info("MQTT Broker: {}", mqttProperties.getBrokerUrl());
        log.info("客户端ID: {}", mqttProperties.getClientId());
        log.info("===================");
    }
}
