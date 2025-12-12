package com.quaer_api.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "payment_orders") // 数据库表名
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 本地数据库自增ID

    // === 基本支付信息 ===
    @Column(name = "square_payment_id", unique = true, nullable = true)
    private String squarePaymentId; // Square的payment id

    @Column(name = "order_id")
    private String orderId; // Square的order id

    @Column(name = "location_id")
    private String locationId; // Square的location id

    @Column(name = "receipt_number")
    private String receiptNumber; // 收据编号 (短编号)

    @Column(name = "receipt_url", columnDefinition = "TEXT")
    private String receiptUrl; // 收据链接

    @Column(name = "note", columnDefinition = "TEXT")
    private String note; // 备注

    // === 金额信息 ===
    @Column(name = "amount")
    private Long amount; // 支付金额(分)

    @Column(name = "total_amount")
    private Long totalAmount; // 总金额(分)

    @Column(name = "approved_amount")
    private Long approvedAmount; // 批准金额(分)

    @Column(name = "currency")
    private String currency; // 货币（CAD, USD等）

    // === 状态信息 ===
    @Column(name = "status")
    private String status; // 支付状态（COMPLETED 等）

    @Column(name = "source_type")
    private String sourceType; // 支付来源类型 (CARD, CASH等)

    // === 时间信息 ===
    @Column(name = "created_at")
    private OffsetDateTime createdAt; // 创建时间

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt; // 更新时间

    @Column(name = "authorized_at")
    private OffsetDateTime authorizedAt; // 授权时间

    @Column(name = "captured_at")
    private OffsetDateTime capturedAt; // 资金划转时间

    @Column(name = "delayed_until")
    private OffsetDateTime delayedUntil; // 延迟到期时间

    @Column(name = "delay_duration")
    private String delayDuration; // 延迟持续时间 (ISO 8601格式)

    @Column(name = "delay_action")
    private String delayAction; // 延迟动作 (CANCEL等)

    // === 卡片信息 ===
    @Column(name = "card_brand")
    private String cardBrand; // 卡品牌 (VISA, MASTERCARD等)

    @Column(name = "last4")
    private String last4; // 卡号后4位

    @Column(name = "card_type")
    private String cardType; // 卡类型 (CREDIT, DEBIT等)

    @Column(name = "prepaid_type")
    private String prepaidType; // 预付卡类型 (NOT_PREPAID等)

    @Column(name = "card_bin")
    private String cardBin; // 卡片BIN (前6位)

    @Column(name = "card_exp_month")
    private Integer cardExpMonth; // 卡片过期月份

    @Column(name = "card_exp_year")
    private Integer cardExpYear; // 卡片过期年份

    @Column(name = "card_fingerprint")
    private String cardFingerprint; // 卡片指纹

    // === 终端支付相关字段 ===
    @Column(name = "payment_source")
    private String paymentSource; // 支付来源: ONLINE(在线支付) 或 TERMINAL(终端支付)

    @Column(name = "reference_id")
    private String referenceId; // 终端支付的引用ID

    @Column(name = "device_id")
    private String deviceId; // 终端设备ID

    @Column(name = "checkout_id", nullable = true)
    private String checkoutId; // 终端 Checkout ID (可能与 squarePaymentId 不同)

    // === 卡片详情状态 ===
    @Column(name = "card_status")
    private String cardStatus; // 卡片状态 (CAPTURED等)

    @Column(name = "entry_method")
    private String entryMethod; // 输入方式 (KEYED, SWIPED等)

    @Column(name = "cvv_status")
    private String cvvStatus; // CVV验证状态

    @Column(name = "avs_status")
    private String avsStatus; // AVS验证状态

    @Column(name = "statement_description")
    private String statementDescription; // 账单描述

    // === 风险评估 ===
    @Column(name = "risk_level")
    private String riskLevel; // 风险等级 (NORMAL, HIGH等)

    @Column(name = "risk_evaluation_created_at")
    private OffsetDateTime riskEvaluationCreatedAt; // 风险评估创建时间

    // === 应用信息 ===
    @Column(name = "square_product")
    private String squareProduct; // Square产品 (ECOMMERCE_API等)

    @Column(name = "application_id")
    private String applicationId; // 应用ID

    @Column(name = "version_token")
    private String versionToken; // 版本令牌

    // === 关联字段 ===
    @Column(name = "vehicle_record_id")
    private Long vehicleRecordId; // 关联的出场记录ID

    // === 系统字段 ===
    @Column(name = "created_by")
    private String createdBy; // 创建者

    @Column(name = "local_created_at", nullable = true)
    private OffsetDateTime localCreatedAt = OffsetDateTime.now(); // 本地创建时间

    @Column(name = "local_updated_at")
    private OffsetDateTime localUpdatedAt; // 本地更新时间

    // === 生命周期回调 ===
    @PreUpdate
    protected void onUpdate() {
        localUpdatedAt = OffsetDateTime.now();
    }
}
