package com.quaer_api.controller;

import com.quaer_api.service.MqttClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * MQTT测试控制器
 * 提供HTTP接口测试MQTT发布功能
 */
@Slf4j
@RestController
@RequestMapping("/mqtt")
public class MqttTestController {

    @Autowired
    private MqttClientService mqttClientService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 测试发布消息到默认主题
     * @param message 消息内容
     * @return 响应结果
     */
    @PostMapping("/publish")
    public Map<String, Object> publishMessage(@RequestParam String message) {
        log.info(">>> 收到发布消息请求: {}", message);

        Map<String, Object> result = new HashMap<>();

        try {
            mqttClientService.publishToDefaultTopic(message);
            result.put("success", true);
            result.put("message", "消息发送成功");
            result.put("content", message);
            result.put("timestamp", getCurrentTime());

            log.info(">>> HTTP接口发布MQTT消息成功");

        } catch (Exception e) {
            log.error("!!! HTTP接口发布MQTT消息失败", e);
            result.put("success", false);
            result.put("message", "消息发送失败: " + e.getMessage());
            result.put("timestamp", getCurrentTime());
        }

        return result;
    }

    /**
     * 发布消息到指定主题
     * @param topic 主题
     * @param message 消息内容
     * @return 响应结果
     */
    @PostMapping("/publish/{topic}")
    public Map<String, Object> publishToTopic(
            @PathVariable String topic,
            @RequestParam String message) {

        log.info(">>> 收到发布消息请求 - 主题: {}, 内容: {}", topic, message);

        Map<String, Object> result = new HashMap<>();

        try {
            mqttClientService.publish(topic, message);
            result.put("success", true);
            result.put("message", "消息发送成功");
            result.put("topic", topic);
            result.put("content", message);
            result.put("timestamp", getCurrentTime());

            log.info(">>> HTTP接口发布MQTT消息到主题 {} 成功", topic);

        } catch (Exception e) {
            log.error("!!! HTTP接口发布MQTT消息失败", e);
            result.put("success", false);
            result.put("message", "消息发送失败: " + e.getMessage());
            result.put("timestamp", getCurrentTime());
        }

        return result;
    }

    /**
     * 检查MQTT连接状态
     * @return 连接状态
     */
    @GetMapping("/status")
    public Map<String, Object> checkStatus() {
        log.info(">>> 检查MQTT连接状态");

        Map<String, Object> result = new HashMap<>();
        boolean connected = mqttClientService.isConnected();

        result.put("connected", connected);
        result.put("status", connected ? "已连接" : "未连接");
        result.put("timestamp", getCurrentTime());

        return result;
    }

    /**
     * 发送测试消息
     * @return 响应结果
     */
    @GetMapping("/test")
    public Map<String, Object> sendTestMessage() {
        log.info(">>> 发送MQTT测试消息");

        String testMessage = String.format("测试消息 - 时间: %s", getCurrentTime());

        Map<String, Object> result = new HashMap<>();

        try {
            mqttClientService.publishToDefaultTopic(testMessage);
            result.put("success", true);
            result.put("message", "测试消息发送成功");
            result.put("content", testMessage);
            result.put("timestamp", getCurrentTime());

        } catch (Exception e) {
            log.error("!!! 发送测试消息失败", e);
            result.put("success", false);
            result.put("message", "测试消息发送失败: " + e.getMessage());
            result.put("timestamp", getCurrentTime());
        }

        return result;
    }

    /**
     * 获取当前时间字符串
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}
