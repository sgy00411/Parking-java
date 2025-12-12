package com.quaer_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * MQTT入场消息DTO
 */
@Data
public class MqttEntryMessage {

    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("record_id")
    private Long recordId;

    @JsonProperty("event_type")
    private String eventType;  // "entry"

    @JsonProperty("action")
    private String action;  // "entry_new" 或 "entry_update"

    @JsonProperty("status")
    private String status;  // "entered"

    @JsonProperty("entry_plate_number")
    private String entryPlateNumber;

    @JsonProperty("entry_time")
    private String entryTime;

    @JsonProperty("entry_camera_ip")
    private String entryCameraIp;

    @JsonProperty("entry_camera_id")
    private Integer entryCameraId;

    @JsonProperty("entry_camera_name")
    private String entryCameraName;

    @JsonProperty("entry_event_id")
    private Integer entryEventId;

    @JsonProperty("entry_detection_count")
    private Integer entryDetectionCount;

    @JsonProperty("entry_weight")
    private BigDecimal entryWeight;

    @JsonProperty("entry_snapshot")
    private String entrySnapshot;

    @JsonProperty("upload_photo")
    private Integer uploadPhoto;

    @JsonProperty("timestamp")
    private String timestamp;
}
