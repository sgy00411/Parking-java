package com.quaer_api.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quaer_api.dto.MqttEntryMessage;
import com.quaer_api.dto.MqttExitMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * MQTT消息测试工具
 * 用于生成测试用的MQTT消息
 */
@Slf4j
@Component
public class MqttMessageTestUtil {

    @Autowired
    private com.quaer_api.service.MqttClientService mqttClientService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 发送测试用的入场消息（新车入场）
     */
    public void sendTestEntryNewMessage(String plateNumber) {
        try {
            MqttEntryMessage message = new MqttEntryMessage();
            message.setMessageId(UUID.randomUUID().toString());
            message.setRecordId(null);  // 新记录时为null
            message.setEventType("entry");
            message.setAction("entry_new");
            message.setStatus("entered");
            message.setEntryPlateNumber(plateNumber);
            message.setEntryTime(LocalDateTime.now().format(DATE_TIME_FORMATTER));
            message.setEntryCameraIp("192.168.1.100");
            message.setEntryCameraId(1);
            message.setEntryCameraName("测试入口摄像头");
            message.setEntryEventId(1001);
            message.setEntryDetectionCount(5);
            message.setEntryWeight(new BigDecimal("25.50"));
            message.setEntrySnapshot("test_snapshot_" + System.currentTimeMillis() + ".jpg");
            message.setTimestamp(LocalDateTime.now().format(DATE_TIME_FORMATTER));
            message.setUploadPhoto(1);

            String jsonMessage = objectMapper.writeValueAsString(message);

            log.info("=== 发送测试入场消息 ===");
            log.info("车牌: {}", plateNumber);
            log.info("JSON: {}", jsonMessage);

            // 发布到 parking/0001/camera 主题（模拟真实环境）
            mqttClientService.publish("parking/0001/camera", jsonMessage);

            log.info("✅ 测试消息已发送到主题: parking/0001/camera");

        } catch (Exception e) {
            log.error("❌ 发送测试消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送测试用的入场消息（重复入场更新）
     */
    public void sendTestEntryUpdateMessage(String plateNumber, Long recordId) {
        try {
            MqttEntryMessage message = new MqttEntryMessage();
            message.setMessageId(UUID.randomUUID().toString());
            message.setRecordId(recordId);
            message.setEventType("entry");
            message.setAction("entry_update");
            message.setStatus("entered");
            message.setEntryPlateNumber(plateNumber);
            message.setEntryTime(LocalDateTime.now().format(DATE_TIME_FORMATTER));
            message.setEntryCameraIp("192.168.1.100");
            message.setEntryCameraId(1);
            message.setEntryCameraName("测试入口摄像头");
            message.setEntryEventId(1002);
            message.setEntryDetectionCount(8);
            message.setEntryWeight(new BigDecimal("30.20"));
            message.setEntrySnapshot("test_snapshot_update_" + System.currentTimeMillis() + ".jpg");
            message.setTimestamp(LocalDateTime.now().format(DATE_TIME_FORMATTER));
            message.setUploadPhoto(1);

            String jsonMessage = objectMapper.writeValueAsString(message);

            log.info("=== 发送测试入场更新消息 ===");
            log.info("车牌: {}", plateNumber);
            log.info("记录ID: {}", recordId);
            log.info("JSON: {}", jsonMessage);

            // 发布到 parking/0001/camera 主题（模拟真实环境）
            mqttClientService.publish("parking/0001/camera", jsonMessage);

            log.info("✅ 测试更新消息已发送到主题: parking/0001/camera");

        } catch (Exception e) {
            log.error("❌ 发送测试更新消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送测试用的出口消息（正常出场）
     */
    public void sendTestExitNormalMessage(String plateNumber) {
        try {
            MqttExitMessage message = new MqttExitMessage();
            message.setMessageId(UUID.randomUUID().toString());
            message.setRecordId(null);
            message.setEventType("exit");
            message.setAction("exit_normal");
            message.setStatus("exited");
            message.setExitPlateNumber(plateNumber);
            message.setExitTime(LocalDateTime.now().format(DATE_TIME_FORMATTER));
            message.setExitCameraIp("192.168.1.101");
            message.setExitCameraId(2);
            message.setExitCameraName("测试出口摄像头");
            message.setExitEventId(2001);
            message.setExitDetectionCount(6);
            message.setExitWeight(new BigDecimal("28.30"));
            message.setExitSnapshot("test_snapshot_exit_" + System.currentTimeMillis() + ".jpg");
            message.setTimestamp(LocalDateTime.now().format(DATE_TIME_FORMATTER));

            String jsonMessage = objectMapper.writeValueAsString(message);

            log.info("=== 发送测试出口消息（正常出场） ===");
            log.info("车牌: {}", plateNumber);
            log.info("JSON: {}", jsonMessage);

            // 发布到 parking/0001/camera 主题（模拟真实环境）
            mqttClientService.publish("parking/0001/camera", jsonMessage);

            log.info("✅ 测试出口消息已发送到主题: parking/0001/camera");

        } catch (Exception e) {
            log.error("❌ 发送测试出口消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送测试用的出口消息（异常出口 - 无入场记录）
     */
    public void sendTestExitOnlyMessage(String plateNumber) {
        try {
            MqttExitMessage message = new MqttExitMessage();
            message.setMessageId(UUID.randomUUID().toString());
            message.setRecordId(null);
            message.setEventType("exit");
            message.setAction("exit_only_new");
            message.setStatus("exit_only");
            message.setExitPlateNumber(plateNumber);
            message.setExitTime(LocalDateTime.now().format(DATE_TIME_FORMATTER));
            message.setExitCameraIp("192.168.1.101");
            message.setExitCameraId(2);
            message.setExitCameraName("测试出口摄像头");
            message.setExitEventId(2002);
            message.setExitDetectionCount(7);
            message.setExitWeight(new BigDecimal("26.80"));
            message.setExitSnapshot("test_snapshot_exit_only_" + System.currentTimeMillis() + ".jpg");
            message.setTimestamp(LocalDateTime.now().format(DATE_TIME_FORMATTER));

            String jsonMessage = objectMapper.writeValueAsString(message);

            log.info("=== 发送测试出口消息（异常出口-无入场） ===");
            log.info("车牌: {}", plateNumber);
            log.info("JSON: {}", jsonMessage);

            // 发布到 parking/0001/camera 主题（模拟真实环境）
            mqttClientService.publish("parking/0001/camera", jsonMessage);

            log.info("✅ 测试异常出口消息已发送到主题: parking/0001/camera");

        } catch (Exception e) {
            log.error("❌ 发送测试异常出口消息失败: {}", e.getMessage(), e);
        }
    }
}
