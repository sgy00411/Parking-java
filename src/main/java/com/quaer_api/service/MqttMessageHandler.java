package com.quaer_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quaer_api.dto.MqttEntryMessage;
import com.quaer_api.dto.MqttExitMessage;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MQTT消息处理器
 * 处理接收到的MQTT消息
 */
@Slf4j
@Component
public class MqttMessageHandler {

    @Autowired
    private VehicleRecordService vehicleRecordService;

    @Autowired
    private SnapshotWhitelistService snapshotWhitelistService;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private LedDisplayService ledDisplayService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 处理接收到的消息
     * @param topic 主题
     * @param message MQTT消息
     */
    public void handleMessage(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

            log.info("========================================");
            log.info("处理MQTT消息");
            log.info("  接收时间: {}", getCurrentTime());
            log.info("  消息主题: {}", topic);
            log.info("  消息内容: {}", payload);
            log.info("  消息ID: {}", message.getId());
            log.info("  QoS级别: {}", message.getQos());
            log.info("  是否重复: {}", message.isDuplicate());
            log.info("  是否保留: {}", message.isRetained());
            log.info("========================================");

            // 根据主题处理不同的消息
            if (topic.contains("/camera")) {
                // 处理摄像头相关消息（入场/出场）
                handleCameraMessage(topic, payload);
            } else {
                handleOtherMessage(topic, payload);
            }

        } catch (Exception e) {
            log.error("!!! 处理MQTT消息失败! 主题: {} 时间: {}",
                    topic, getCurrentTime(), e);
        }
    }

    /**
     * 处理停车场摄像头消息
     * @param topic 消息主题
     * @param payload 消息内容
     */
    private void handleCameraMessage(String topic, String payload) {
        log.info(">>> 处理停车场摄像头消息");
        log.info("  消息内容: {}", payload);

        try {
            // 从主题中提取停车场编号 (如 parking/0001/camera -> 0001)
            String parkingLotCode = extractParkingLotCode(topic);
            log.info("  停车场编号: {}", parkingLotCode);

            // 先解析JSON获取event_type字段
            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(payload);
            String eventType = rootNode.get("event_type").asText();
            String action = rootNode.get("action").asText();

            if ("entry".equals(eventType)) {
                // 处理入场消息
                MqttEntryMessage entryMessage = objectMapper.readValue(payload, MqttEntryMessage.class);

                // 🔒 将快照文件名加入白名单
                if (entryMessage.getEntrySnapshot() != null && !entryMessage.getEntrySnapshot().trim().isEmpty()) {
                    snapshotWhitelistService.addToWhitelist(entryMessage.getEntrySnapshot());
                }

                if ("entry_new".equals(action) || "entry_update".equals(action)) {
                    log.info(">>> 检测到入场消息: {}", action);
                    boolean success = vehicleRecordService.handleEntryMessage(entryMessage, parkingLotCode);
                    if (success) {
                        log.info("✅ 入场消息处理成功");
                    } else {
                        log.error("❌ 入场消息处理失败");
                    }
                } else {
                    log.warn("⚠️ 未知的入场动作类型: {}", action);
                }
            } else if ("exit".equals(eventType)) {
                // 处理出场消息
                MqttExitMessage exitMessage = objectMapper.readValue(payload, MqttExitMessage.class);

                // 🔒 将快照文件名加入白名单
                if (exitMessage.getExitSnapshot() != null && !exitMessage.getExitSnapshot().trim().isEmpty()) {
                    snapshotWhitelistService.addToWhitelist(exitMessage.getExitSnapshot());
                }

                if ("exit_normal".equals(action) || "exit_only_new".equals(action) || "exit_only_update".equals(action)) {
                    log.info(">>> 检测到出场消息: {}", action);
                    boolean success = vehicleRecordService.handleExitMessage(exitMessage, parkingLotCode);
                    if (success) {
                        log.info("✅ 出场消息处理成功");
                    } else {
                        log.error("❌ 出场消息处理失败");
                    }
                } else {
                    log.warn("⚠️ 未知的出场动作类型: {}", action);
                }
            } else {
                log.warn("⚠️ 未知的事件类型: {}", eventType);
            }

        } catch (Exception e) {
            log.error("❌ 解析摄像头消息失败: {}", e.getMessage(), e);
        }

        log.info(">>> 摄像头消息处理完成");
    }

    /**
     * 从MQTT主题中提取停车场编号
     * 例如: parking/0001/camera -> 0001
     * @param topic MQTT主题
     * @return 停车场编号
     */
    private String extractParkingLotCode(String topic) {
        try {
            // 主题格式: parking/0001/camera
            String[] parts = topic.split("/");
            if (parts.length >= 2) {
                return parts[1]; // 返回第二部分，即停车场编号
            }
        } catch (Exception e) {
            log.warn("⚠️ 无法从主题提取停车场编号: {}", topic);
        }
        return "0000"; // 默认值
    }

    /**
     * 处理其他主题消息
     * @param topic 主题
     * @param payload 消息内容
     */
    private void handleOtherMessage(String topic, String payload) {
        log.info(">>> 处理其他主题消息");
        log.info("  主题: {}", topic);
        log.info("  内容: {}", payload);

        // 处理LED显示请求
        if (topic.contains("/LED")) {
            handleLedDisplayRequest(payload);
        }

        log.info(">>> 其他消息处理完成");
    }

    /**
     * 处理LED显示请求
     * @param payload 消息内容
     */
    private void handleLedDisplayRequest(String payload) {
        try {
            log.info(">>> 处理LED显示请求");

            // 解析JSON消息
            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(payload);

            // 默认LED设备编号
            String ledDeviceCid = "96:6E:6D:27:DC:9D";

            // 尝试从新格式提取LED设备编号
            if (rootNode.has("led_device_id") && !rootNode.get("led_device_id").isNull()) {
                String ledDeviceId = rootNode.get("led_device_id").asText();
                if (ledDeviceId != null && !ledDeviceId.trim().isEmpty()) {
                    ledDeviceCid = ledDeviceId;
                }
            }
            // 尝试从旧格式提取LED设备编号
            else if (rootNode.has("exit_devices")) {
                com.fasterxml.jackson.databind.JsonNode exitDevices = rootNode.get("exit_devices");
                if (exitDevices.has("led_screen_config")) {
                    String ledScreenConfig = exitDevices.get("led_screen_config").asText();
                    if (ledScreenConfig != null && !ledScreenConfig.trim().isEmpty()) {
                        ledDeviceCid = ledScreenConfig;
                    }
                }
            }

            // 提取车牌号（支持新旧格式）
            String plateNumber = "";
            if (rootNode.has("plate_number")) {
                plateNumber = rootNode.get("plate_number").asText();
            } else if (rootNode.has("plate")) {
                com.fasterxml.jackson.databind.JsonNode plate = rootNode.get("plate");
                if (plate.has("plate_number")) {
                    plateNumber = plate.get("plate_number").asText();
                }
            }

            // 提取车辆类型
            String vehicleType = "临时车";
            if (rootNode.has("vehicle_type")) {
                vehicleType = rootNode.get("vehicle_type").asText();
            }

            // 提取显示文字
            String displayText = "请稍候";  // 默认值
            if (rootNode.has("display_text")) {
                displayText = rootNode.get("display_text").asText();
            }

            // 如果车牌号为空，不处理
            if (plateNumber == null || plateNumber.trim().isEmpty()) {
                log.warn("⚠️ 车牌号为空，跳过处理");
                return;
            }

            log.info("  LED设备编号: {}", ledDeviceCid);
            log.info("  车牌号: {}", plateNumber);
            log.info("  车辆类型: {}", vehicleType);
            log.info("  显示文字: {}", displayText);

            // 发送LED显示消息
            ledDisplayService.sendVehicleWelcomeToLed(ledDeviceCid, plateNumber, vehicleType, displayText);

            log.info("✅ LED显示请求处理成功");

        } catch (Exception e) {
            log.error("❌ 处理LED显示请求失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取当前时间字符串
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}
