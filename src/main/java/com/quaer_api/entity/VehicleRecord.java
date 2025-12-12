package com.quaer_api.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车辆记录实体类
 */
@Data
@Entity
@Table(name = "vehicle_records")
public class VehicleRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 停车场编号 (从MQTT主题中提取，如 parking/0001/camera 中的 0001)
     */
    @Column(name = "parking_lot_code", length = 10)
    private String parkingLotCode;

    /**
     * 状态字段
     * 'entered': 只有入场未出场
     * 'exited': 已经出场
     * 'exit_only': 异常-只有出场无入场
     */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "entered";

    // ========== 入场字段 ==========
    @Column(name = "entry_plate_number", length = 18)
    private String entryPlateNumber;

    @Column(name = "entry_time")
    private LocalDateTime entryTime;

    @Column(name = "entry_camera_ip", length = 50)
    private String entryCameraIp;

    @Column(name = "entry_camera_id")
    private Integer entryCameraId;

    @Column(name = "entry_camera_name", length = 100)
    private String entryCameraName;

    @Column(name = "entry_event_id")
    private Integer entryEventId;

    @Column(name = "entry_detection_count")
    private Integer entryDetectionCount;

    @Column(name = "entry_weight", precision = 10, scale = 2)
    private BigDecimal entryWeight;

    @Column(name = "entry_snapshot", length = 255)
    private String entrySnapshot;

    // ========== 出场字段 ==========
    @Column(name = "exit_plate_number", length = 18)
    private String exitPlateNumber;

    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    @Column(name = "exit_camera_ip", length = 50)
    private String exitCameraIp;

    @Column(name = "exit_payment_device_id", length = 50)
    private String exitPaymentDeviceId;

    @Column(name = "exit_led_screen_config", length = 255)
    private String exitLedScreenConfig;

    @Column(name = "exit_barrier_gate_id", length = 50)
    private String exitBarrierGateId;

    @Column(name = "exit_camera_id")
    private Integer exitCameraId;

    @Column(name = "exit_camera_name", length = 100)
    private String exitCameraName;

    @Column(name = "exit_event_id")
    private Integer exitEventId;

    @Column(name = "exit_detection_count")
    private Integer exitDetectionCount;

    @Column(name = "exit_weight", precision = 10, scale = 2)
    private BigDecimal exitWeight;

    @Column(name = "exit_snapshot", length = 255)
    private String exitSnapshot;

    // ========== 统计字段 ==========
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * 停车时长（分钟） - 仅针对 exit_camera_id = 4 的正常出场
     * 不足1分钟按1分钟计算
     */
    @Column(name = "parking_duration_minutes")
    private Integer parkingDurationMinutes;

    /**
     * 停车费用（美分） - 仅针对 exit_camera_id = 4 的正常出场
     * 1分钟 = 1美分，最高9美分
     */
    @Column(name = "parking_fee_cents")
    private Integer parkingFeeCents;

    // ========== 支付相关字段 ==========
    /**
     * 支付状态: null/unpaid-未支付, paid-已支付, failed-支付失败
     */
    @Column(name = "payment_status", length = 20)
    private String paymentStatus;

    /**
     * Square支付ID (关联payment_orders表)
     */
    @Column(name = "square_payment_id")
    private String squarePaymentId;

    /**
     * 支付时间
     */
    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    // ========== 时间戳 ==========
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
