package com.quaer_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * MQTT出口消息DTO
 */
@Data
public class MqttExitMessage {

    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("record_id")
    private Long recordId;

    @JsonProperty("event_type")
    private String eventType;  // "exit"

    @JsonProperty("action")
    private String action;  // "exit_normal" 或 "exit_only_new" 或 "exit_only_update"

    @JsonProperty("status")
    private String status;  // "exited" 或 "exit_only"

    // 入场车牌号（仅 exit_normal 有）
    @JsonProperty("entry_plate_number")
    private String entryPlateNumber;

    @JsonProperty("exit_plate_number")
    private String exitPlateNumber;

    @JsonProperty("exit_time")
    private String exitTime;  // 不使用，服务器自己计算

    @JsonProperty("exit_camera_ip")
    private String exitCameraIp;

    @JsonProperty("exit_payment_device_id")
    private String exitPaymentDeviceId;

    @JsonProperty("exit_led_screen_config")
    private String exitLedScreenConfig;

    @JsonProperty("exit_barrier_gate_id")
    private String exitBarrierGateId;

    @JsonProperty("exit_camera_id")
    private Integer exitCameraId;

    @JsonProperty("exit_camera_name")
    private String exitCameraName;

    @JsonProperty("exit_event_id")
    private Integer exitEventId;

    @JsonProperty("exit_detection_count")
    private Integer exitDetectionCount;

    @JsonProperty("exit_weight")
    private BigDecimal exitWeight;

    @JsonProperty("exit_snapshot")
    private String exitSnapshot;

    @JsonProperty("upload_photo")
    private Integer uploadPhoto;

    // 停留时长（不使用，服务器自己计算）
    @JsonProperty("duration_seconds")
    private Integer durationSeconds;

    @JsonProperty("duration_str")
    private String durationStr;

    @JsonProperty("timestamp")
    private String timestamp;
}
