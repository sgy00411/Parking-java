package com.quaer_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.quaer_api.entity.PaymentOrder;
import com.quaer_api.entity.VehicleRecord;
import com.quaer_api.repository.PaymentOrderRepository;
import com.quaer_api.repository.VehicleRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquareWebhookService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final VehicleRecordRepository vehicleRecordRepository;
    private final LedDisplayService ledDisplayService;
    private final MqttClientService mqttClientService;

    /**
     * 处理 payment.created 事件
     */
    @Transactional
    public PaymentOrder handlePaymentCreated(JsonNode payload) {
        try {
            // 提取支付数据
            JsonNode data = payload.path("data");
            JsonNode object = data.path("object");
            JsonNode payment = object.path("payment");

            String paymentId = payment.path("id").asText();
            String orderId = payment.path("order_id").asText(null);
            String checkoutId = payment.path("terminal_checkout_id").asText(null);

            log.info("处理 payment.created 事件 | PaymentID: {} | OrderID: {} | CheckoutID: {}", paymentId, orderId, checkoutId);

            PaymentOrder order = null;

            // 1. 首先尝试通过 checkout_id 查找已存在的记录（终端支付的主要关联字段）
            if (checkoutId != null && !checkoutId.isEmpty()) {
                Optional<PaymentOrder> existingByCheckoutId = paymentOrderRepository.findByCheckoutId(checkoutId);
                if (existingByCheckoutId.isPresent()) {
                    order = existingByCheckoutId.get();
                    log.info("✅ 找到已存在的支付记录（通过CheckoutID）| 记录ID: {} | VehicleRecordID: {}",
                        order.getId(), order.getVehicleRecordId());
                }
            }

            // 2. 如果没找到，尝试通过 order_id 查找
            if (order == null && orderId != null && !orderId.isEmpty()) {
                Optional<PaymentOrder> existingByOrderId = paymentOrderRepository.findByOrderId(orderId);
                if (existingByOrderId.isPresent()) {
                    order = existingByOrderId.get();
                    log.info("✅ 找到已存在的支付记录（通过OrderID）| 记录ID: {} | VehicleRecordID: {}",
                        order.getId(), order.getVehicleRecordId());
                }
            }

            // 3. 如果没找到，尝试通过 payment_id 查找
            if (order == null) {
                Optional<PaymentOrder> existingByPaymentId = paymentOrderRepository.findBySquarePaymentId(paymentId);
                if (existingByPaymentId.isPresent()) {
                    order = existingByPaymentId.get();
                    log.info("✅ 找到已存在的支付记录（通过PaymentID）| 记录ID: {} | VehicleRecordID: {}",
                        order.getId(), order.getVehicleRecordId());
                }
            }

            // 4. 如果还是没找到，创建新记录（兼容旧流程，但记录警告）
            if (order == null) {
                order = new PaymentOrder();
                log.warn("⚠️ payment.created 未找到已存在订单，创建新记录 | PaymentID: {} | OrderID: {} | CheckoutID: {}",
                    paymentId, orderId, checkoutId);
            }

            // 更新支付信息
            updatePaymentInfo(order, payment);

            PaymentOrder saved = paymentOrderRepository.save(order);
            log.info("✅ 支付记录已保存: ID={}, PaymentID={}, VehicleRecordID={}, Amount={} {}",
                    saved.getId(),
                    saved.getSquarePaymentId(),
                    saved.getVehicleRecordId(),
                    saved.getAmount(),
                    saved.getCurrency());

            return saved;

        } catch (Exception e) {
            log.error("❌ 处理支付创建事件失败", e);
            throw new RuntimeException("保存支付数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理 payment.updated 事件
     */
    @Transactional
    public PaymentOrder handlePaymentUpdated(JsonNode payload) {
        try {
            JsonNode data = payload.path("data");
            JsonNode object = data.path("object");
            JsonNode payment = object.path("payment");

            String paymentId = payment.path("id").asText();
            String orderId = payment.path("order_id").asText(null);
            String checkoutId = payment.path("terminal_checkout_id").asText(null);
            String paymentStatus = payment.path("status").asText("UNKNOWN");

            log.info("处理 payment.updated 事件 | PaymentID: {} | OrderID: {} | CheckoutID: {} | Status: {}",
                paymentId, orderId, checkoutId, paymentStatus);

            PaymentOrder order = null;

            // 1. 首先尝试通过 payment_id 查找
            order = paymentOrderRepository.findBySquarePaymentId(paymentId).orElse(null);

            // 2. 如果没找到，尝试通过 checkout_id 查找（终端支付的主要关联字段）
            if (order == null && checkoutId != null && !checkoutId.isEmpty()) {
                order = paymentOrderRepository.findByCheckoutId(checkoutId).orElse(null);
                if (order != null) {
                    log.info("✅ 通过CheckoutID找到支付记录 | 记录ID: {} | VehicleRecordID: {}",
                        order.getId(), order.getVehicleRecordId());
                }
            }

            // 3. 如果没找到，尝试通过 order_id 查找
            if (order == null && orderId != null && !orderId.isEmpty()) {
                order = paymentOrderRepository.findByOrderId(orderId).orElse(null);
                if (order != null) {
                    log.info("✅ 通过OrderID找到支付记录 | 记录ID: {} | VehicleRecordID: {}",
                        order.getId(), order.getVehicleRecordId());
                }
            }

            // 4. 如果还是没找到，创建新记录（兼容旧流程，但记录警告）
            if (order == null) {
                order = new PaymentOrder();
                log.warn("⚠️ payment.updated 未找到已存在订单，创建新记录 | PaymentID: {} | OrderID: {} | CheckoutID: {}",
                    paymentId, orderId, checkoutId);
            }

            // 更新支付信息
            updatePaymentInfo(order, payment);

            // 更新时间戳
            order.setUpdatedAt(parseDateTime(payment.path("updated_at").asText(null)));
            order.setAuthorizedAt(parseDateTime(payment.path("authorized_at").asText(null)));
            order.setCapturedAt(parseDateTime(payment.path("captured_at").asText(null)));

            PaymentOrder saved = paymentOrderRepository.save(order);
            log.info("✅ 支付记录已更新: ID={}, PaymentID={}, VehicleRecordID={}, Status={}",
                    saved.getId(),
                    saved.getSquarePaymentId(),
                    saved.getVehicleRecordId(),
                    saved.getStatus());

            // 如果支付成功，更新关联的车辆记录状态
            if ("COMPLETED".equals(paymentStatus)) {
                if (saved.getVehicleRecordId() != null) {
                    try {
                        Optional<VehicleRecord> vehicleRecordOpt = vehicleRecordRepository.findById(saved.getVehicleRecordId());
                        if (vehicleRecordOpt.isPresent()) {
                            VehicleRecord vehicleRecord = vehicleRecordOpt.get();
                            vehicleRecord.setPaymentStatus("paid");
                            vehicleRecord.setSquarePaymentId(paymentId);
                            vehicleRecord.setPaymentTime(LocalDateTime.now());
                            vehicleRecordRepository.save(vehicleRecord);

                            log.info("✅ 车辆记录支付状态已更新 | 记录ID: {} | 支付ID: {} | 状态: paid",
                                saved.getVehicleRecordId(), paymentId);

                            // 发送支付成功LED显示
                            String ledDeviceCid = vehicleRecord.getLedScreenConfig();
                            String licensePlate = vehicleRecord.getExitPlateNumber() != null ?
                                vehicleRecord.getExitPlateNumber() : vehicleRecord.getEntryPlateNumber();

                            if (ledDeviceCid != null && !ledDeviceCid.trim().isEmpty() &&
                                licensePlate != null && !licensePlate.trim().isEmpty()) {
                                try {
                                    log.info(">>> 发送支付成功LED显示 | LED设备: {} | 车牌: {}", ledDeviceCid, licensePlate);
                                    ledDisplayService.sendVehiclePaymentSuccessToLed(ledDeviceCid, licensePlate);
                                    log.info("✅ 支付成功LED显示发送成功");
                                } catch (Exception ledEx) {
                                    log.error("❌ 发送支付成功LED显示失败 | LED设备: {} | 车牌: {} | 错误: {}",
                                        ledDeviceCid, licensePlate, ledEx.getMessage(), ledEx);
                                }
                            } else {
                                log.warn("⚠️ LED设备配置或车牌号为空，跳过LED显示 | LED设备: {} | 车牌: {}",
                                    ledDeviceCid, licensePlate);
                            }

                            // 🚀 支付成功后自动开闸
                            autoOpenGate(vehicleRecord);

                        } else {
                            log.error("❌ 未找到关联的车辆记录 | 车辆记录ID: {}", saved.getVehicleRecordId());
                        }
                    } catch (Exception e) {
                        log.error("❌ 更新车辆记录支付状态失败 | 车辆记录ID: {} | 错误: {}",
                            saved.getVehicleRecordId(), e.getMessage(), e);
                    }
                } else {
                    log.warn("⚠️ 支付已完成，但未关联到车辆记录 | PaymentOrderID: {} | PaymentID: {}",
                        saved.getId(), paymentId);
                }
            }

            return saved;

        } catch (Exception e) {
            log.error("❌ 处理支付更新事件失败", e);
            throw new RuntimeException("更新支付数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 统一的支付信息更新方法
     * Square webhook返回的数据优先，直接覆盖
     */
    private void updatePaymentInfo(PaymentOrder order, JsonNode payment) {
        // === 基本信息 ===
        order.setSquarePaymentId(payment.path("id").asText());
        order.setOrderId(payment.path("order_id").asText(null));
        order.setLocationId(payment.path("location_id").asText(null));
        order.setReceiptNumber(payment.path("receipt_number").asText(null));
        order.setReceiptUrl(payment.path("receipt_url").asText(null));

        // note：只有Square没有返回时，才保留我们设置的值
        String noteFromSquare = payment.path("note").asText(null);
        if (noteFromSquare != null && !noteFromSquare.isEmpty()) {
            order.setNote(noteFromSquare);
        } else if (order.getNote() == null || order.getNote().isEmpty()) {
            // 如果Square和我们都没有设置，保持为null
        }

        // === 金额信息 ===
        JsonNode amountMoney = payment.path("amount_money");
        if (!amountMoney.isMissingNode()) {
            order.setAmount(amountMoney.path("amount").asLong(0L));
            order.setCurrency(amountMoney.path("currency").asText("USD"));
        }

        JsonNode totalMoney = payment.path("total_money");
        if (!totalMoney.isMissingNode()) {
            order.setTotalAmount(totalMoney.path("amount").asLong(0L));
        }

        JsonNode approvedMoney = payment.path("approved_money");
        if (!approvedMoney.isMissingNode()) {
            order.setApprovedAmount(approvedMoney.path("amount").asLong(0L));
        }

        // === 状态信息 ===
        // 状态更新保护：只允许状态向更高优先级转换
        String newStatus = payment.path("status").asText("UNKNOWN");
        String currentStatus = order.getStatus();

        if (shouldUpdateStatus(currentStatus, newStatus)) {
            order.setStatus(newStatus);
            log.debug("✅ 状态更新: {} -> {}", currentStatus, newStatus);
        } else {
            log.warn("⚠️ 拒绝状态降级: {} -> {} (保持当前状态)", currentStatus, newStatus);
        }

        order.setSourceType(payment.path("source_type").asText(null));

        // === 时间信息 ===
        order.setCreatedAt(parseDateTime(payment.path("created_at").asText(null)));
        order.setUpdatedAt(parseDateTime(payment.path("updated_at").asText(null)));

        // === 终端支付信息 ===
        order.setReferenceId(payment.path("reference_id").asText(null));
        order.setDeviceId(payment.path("device_details").path("device_id").asText(null));

        // 判断支付来源
        String sourceType = payment.path("source_type").asText("");
        if ("CARD".equals(sourceType)) {
            JsonNode cardDetails = payment.path("card_details");
            if (!cardDetails.isMissingNode()) {
                processCardDetails(order, cardDetails);

                // 判断是否为终端支付
                String entryMethod = cardDetails.path("entry_method").asText("");
                if ("EMV".equals(entryMethod) || "CONTACTLESS".equals(entryMethod)) {
                    order.setPaymentSource("TERMINAL");
                } else {
                    order.setPaymentSource("ONLINE");
                }
            }
        } else {
            order.setPaymentSource("OTHER");
        }

        // === 应用信息 ===
        order.setSquareProduct(payment.path("square_product").asText(null));
        order.setApplicationId(payment.path("application_details").path("application_id").asText(null));
        order.setVersionToken(payment.path("version_token").asText(null));

        // === 风险评估 ===
        JsonNode riskEvaluation = payment.path("risk_evaluation");
        if (!riskEvaluation.isMissingNode()) {
            order.setRiskLevel(riskEvaluation.path("risk_level").asText(null));
            order.setRiskEvaluationCreatedAt(parseDateTime(riskEvaluation.path("created_at").asText(null)));
        }

        // === 延迟信息 ===
        order.setDelayedUntil(parseDateTime(payment.path("delayed_until").asText(null)));
        order.setDelayDuration(payment.path("delay_duration").asText(null));
        order.setDelayAction(payment.path("delay_action").asText(null));
    }

    /**
     * 处理卡片详情
     */
    private void processCardDetails(PaymentOrder order, JsonNode cardDetails) {
        // 卡片状态
        order.setCardStatus(cardDetails.path("status").asText(null));
        order.setEntryMethod(cardDetails.path("entry_method").asText(null));
        order.setCvvStatus(cardDetails.path("cvv_status").asText(null));
        order.setAvsStatus(cardDetails.path("avs_status").asText(null));
        order.setStatementDescription(cardDetails.path("statement_description").asText(null));

        // 卡片信息
        JsonNode card = cardDetails.path("card");
        if (!card.isMissingNode()) {
            order.setCardBrand(card.path("card_brand").asText(null));
            order.setLast4(card.path("last_4").asText(null));
            order.setCardType(card.path("card_type").asText(null));
            order.setPrepaidType(card.path("prepaid_type").asText(null));
            order.setCardBin(card.path("bin").asText(null));
            order.setCardExpMonth(card.path("exp_month").asInt(0));
            order.setCardExpYear(card.path("exp_year").asInt(0));
            order.setCardFingerprint(card.path("fingerprint").asText(null));
        }
    }

    /**
     * 判断是否应该更新状态（状态优先级保护）
     * 状态优先级：COMPLETED > FAILED > CANCELED > APPROVED > PENDING > UNKNOWN
     */
    private boolean shouldUpdateStatus(String currentStatus, String newStatus) {
        if (currentStatus == null || currentStatus.isEmpty()) {
            return true; // 没有当前状态，允许设置任何新状态
        }

        if (currentStatus.equals(newStatus)) {
            return true; // 相同状态，允许更新（刷新其他字段）
        }

        int currentPriority = getStatusPriority(currentStatus);
        int newPriority = getStatusPriority(newStatus);

        // 只允许状态向更高优先级转换
        return newPriority >= currentPriority;
    }

    /**
     * 获取支付状态的优先级
     */
    private int getStatusPriority(String status) {
        if (status == null) return 0;

        return switch (status) {
            case "COMPLETED" -> 100;  // 最终完成状态，最高优先级
            case "FAILED" -> 90;      // 失败状态
            case "CANCELED" -> 85;    // 取消状态
            case "APPROVED" -> 50;    // 已批准但未完成
            case "PENDING" -> 30;     // 待处理
            case "AUTHORIZED" -> 40;  // 已授权
            default -> 10;            // 未知状态，最低优先级
        };
    }

    /**
     * 解析 Square 的 RFC 3339 时间格式
     */
    private OffsetDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            log.warn("解析时间失败: {}", dateTimeStr);
            return null;
        }
    }

    /**
     * 支付成功后自动开闸
     * @param vehicleRecord 车辆记录
     */
    private void autoOpenGate(VehicleRecord vehicleRecord) {
        try {
            log.info("========================================");
            log.info("🚀 支付成功，开始自动开闸 | 记录ID: {}", vehicleRecord.getId());

            // 验证必需字段
            if (vehicleRecord.getParkingLotCode() == null || vehicleRecord.getParkingLotCode().trim().isEmpty()) {
                log.warn("⚠️ 自动开闸失败：该记录没有停车场编号(parking_lot_code) | 记录ID: {}", vehicleRecord.getId());
                return;
            }

            if (vehicleRecord.getBarrierGateId() == null || vehicleRecord.getBarrierGateId().trim().isEmpty()) {
                log.warn("⚠️ 自动开闸失败：该记录没有闸机ID(barrier_gate_id) | 记录ID: {}", vehicleRecord.getId());
                return;
            }

            // 转换端口号，失败时使用默认值1
            int channel = 1;  // 默认值
            if (vehicleRecord.getBackupChannelId() != null && !vehicleRecord.getBackupChannelId().trim().isEmpty()) {
                try {
                    channel = Integer.parseInt(vehicleRecord.getBackupChannelId().trim());
                    log.info("📟 使用记录中的端口号: {}", channel);
                } catch (NumberFormatException e) {
                    log.warn("⚠️ 端口号转换失败，使用默认值1: {}", vehicleRecord.getBackupChannelId());
                    channel = 1;
                }
            } else {
                log.warn("⚠️ 记录中没有端口号，使用默认值1");
            }

            // 构建MQTT主题: /gate/{parking_lot_code}/{barrier_gate_id}/get
            String topic = String.format("/gate/%s/%s/get",
                    vehicleRecord.getParkingLotCode(),
                    vehicleRecord.getBarrierGateId());

            log.info("📡 MQTT主题: {}", topic);

            // 生成唯一ID
            String messageId = java.util.UUID.randomUUID().toString();

            // 构建MQTT消息：常开端口，闭合2秒后自动断开
            // closetime: 关闭继电器，2秒后自动打开（常开端口闭合2秒）
            String mqttMessage = String.format(
                    "{\"id\":\"%s\",\"type\":\"modbus\",\"msg\":{\"cmd\":\"closetime\",\"addr\":255,\"channel\":%d,\"time\":20}}",
                    messageId,
                    channel
            );

            log.info("📨 MQTT消息: {}", mqttMessage);
            log.info("  命令: closetime (常开端口闭合2秒)");
            log.info("  端口: {}", channel);
            log.info("  时长: 20 (2秒)");

            // 发送MQTT消息
            mqttClientService.publish(topic, mqttMessage);

            log.info("✅ 自动开闸指令已发送到MQTT");
            log.info("========================================");

        } catch (Exception e) {
            log.error("❌ 自动开闸失败 | 记录ID: {} | 错误: {}", vehicleRecord.getId(), e.getMessage(), e);
        }
    }
}

