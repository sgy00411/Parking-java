package com.quaer_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quaer_api.config.SquareProperties;
import com.quaer_api.dto.MqttEntryMessage;
import com.quaer_api.dto.MqttExitMessage;
import com.quaer_api.entity.PaymentOrder;
import com.quaer_api.entity.VehicleRecord;
import com.quaer_api.repository.PaymentOrderRepository;
import com.quaer_api.repository.VehicleRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * 车辆记录服务
 */
@Slf4j
@Service
public class VehicleRecordService {

    @Autowired
    private VehicleRecordRepository vehicleRecordRepository;

    @Autowired
    private SquareTerminalService squareTerminalService;

    @Autowired
    private SquareOnlinePaymentService squareOnlinePaymentService;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private SquareProperties squareProperties;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private MqttClientService mqttClientService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 处理入场消息并保存到数据库
     *
     * 逻辑：
     * 1. 如果数据库中没有该车牌的未出场记录 → 插入新记录
     * 2. 如果数据库中已有该车牌的未出场记录 → 更新入场信息
     *
     * @param message MQTT入场消息
     * @param parkingLotCode 停车场编号（从MQTT主题提取）
     * @return 是否成功处理
     */
    @Transactional
    public boolean handleEntryMessage(MqttEntryMessage message, String parkingLotCode) {
        try {
            log.info("========================================");
            log.info("处理入场消息");
            log.info("  停车场编号: {}", parkingLotCode);
            log.info("  车牌号: {}", message.getEntryPlateNumber());
            log.info("  摄像头: {}({})", message.getEntryCameraName(), message.getEntryCameraIp());
            log.info("  识别权重: {}", message.getEntryWeight());
            log.info("  动作类型: {}", message.getAction());
            log.info("========================================");

            // 标准化车牌号（去除连字符）
            String normalizedPlate = message.getEntryPlateNumber().replace("-", "");

            // 查询该停车场中是否已有该车牌的未出场记录（停车场编号+车牌号）
            Optional<VehicleRecord> existingRecord =
                vehicleRecordRepository.findUnexitedRecordByParkingLotAndPlate(parkingLotCode, normalizedPlate);

            if (existingRecord.isPresent()) {
                // 场景B：重复入场 - 更新已有记录
                return updateExistingEntry(existingRecord.get(), message, parkingLotCode);
            } else {
                // 场景A：新车入场 - 插入新记录
                return insertNewEntry(message, parkingLotCode);
            }

        } catch (Exception e) {
            log.error("!!! 处理入场消息失败! 车牌: {}, 错误: {}",
                message.getEntryPlateNumber(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 插入新的入场记录
     */
    private boolean insertNewEntry(MqttEntryMessage message, String parkingLotCode) {
        try {
            VehicleRecord record = new VehicleRecord();

            // 设置停车场编号
            record.setParkingLotCode(parkingLotCode);

            // 设置状态
            record.setStatus("entered");

            // 设置入场信息（使用服务器当前时间）
            record.setEntryPlateNumber(message.getEntryPlateNumber());
            record.setEntryTime(LocalDateTime.now());  // 使用服务器当前时间
            record.setEntryCameraIp(message.getEntryCameraIp());
            record.setEntryCameraId(message.getEntryCameraId());
            record.setEntryCameraName(message.getEntryCameraName());
            record.setEntryEventId(message.getEntryEventId());
            record.setEntryDetectionCount(message.getEntryDetectionCount());
            record.setEntryWeight(message.getEntryWeight());
            record.setEntrySnapshot(message.getEntrySnapshot());

            // 设置设备配置
            record.setPaymentDeviceId(message.getPaymentDeviceId());
            record.setLedScreenConfig(message.getLedScreenConfig());
            record.setBarrierGateId(message.getBarrierGateId());
            record.setBackupChannelId(message.getBackupChannelId());

            // 保存到数据库
            VehicleRecord saved = vehicleRecordRepository.save(record);

            log.info("✅ 入场记录已保存 | ID: {} | 车牌: {} | 权重: {} | 摄像头: {}({})",
                saved.getId(),
                message.getEntryPlateNumber(),
                message.getEntryWeight(),
                message.getEntryCameraName(),
                message.getEntryCameraIp());

            // 🚀 入场成功后自动开闸
            autoOpenGateForEntry(saved);

            return true;

        } catch (Exception e) {
            log.error("❌ 插入新入场记录失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 更新已有的入场记录（重复入场）
     */
    private boolean updateExistingEntry(VehicleRecord record, MqttEntryMessage message, String parkingLotCode) {
        try {
            String oldPlate = record.getEntryPlateNumber();

            // 更新停车场编号
            record.setParkingLotCode(parkingLotCode);

            // 更新入场信息为最新值（使用服务器当前时间）
            record.setEntryPlateNumber(message.getEntryPlateNumber());
            record.setEntryTime(LocalDateTime.now());  // 使用服务器当前时间
            record.setEntryCameraIp(message.getEntryCameraIp());
            record.setEntryCameraId(message.getEntryCameraId());
            record.setEntryCameraName(message.getEntryCameraName());
            record.setEntryEventId(message.getEntryEventId());
            record.setEntryDetectionCount(message.getEntryDetectionCount());
            record.setEntryWeight(message.getEntryWeight());
            record.setEntrySnapshot(message.getEntrySnapshot());

            // 更新设备配置
            record.setPaymentDeviceId(message.getPaymentDeviceId());
            record.setLedScreenConfig(message.getLedScreenConfig());
            record.setBarrierGateId(message.getBarrierGateId());
            record.setBackupChannelId(message.getBackupChannelId());

            // 保持状态为 entered
            record.setStatus("entered");

            // 保存更新
            VehicleRecord updated = vehicleRecordRepository.save(record);

            log.info("🔄 入场记录已更新 | ID: {} | 停车场: {} | 车牌: {} → {} | 权重: {} | 摄像头: {}({}) | 原因: 重复入场",
                updated.getId(),
                parkingLotCode,
                oldPlate,
                message.getEntryPlateNumber(),
                message.getEntryWeight(),
                message.getEntryCameraName(),
                message.getEntryCameraIp());

            // 🚀 入场更新成功后自动开闸
            autoOpenGateForEntry(updated);

            return true;

        } catch (Exception e) {
            log.error("❌ 更新入场记录失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 处理出口消息并保存到数据库
     *
     * 三种场景：
     * 1. 正常出场 (exit_normal): 找到入场记录，更新为已出场
     * 2. 异常出口-新建 (exit_only_new): 无入场记录，无异常出口记录，插入新记录
     * 3. 异常出口-更新 (exit_only_update): 无入场记录，但有异常出口记录，更新记录
     *
     * @param message MQTT出口消息
     * @param parkingLotCode 停车场编号（从MQTT主题提取）
     * @return 更新后的车辆记录，失败返回null
     */
    @Transactional
    public VehicleRecord handleExitMessage(MqttExitMessage message, String parkingLotCode) {
        try {
            log.info("========================================");
            log.info("处理出口消息");
            log.info("  停车场编号: {}", parkingLotCode);
            log.info("  车牌号: {}", message.getExitPlateNumber());
            log.info("  摄像头: {}({})", message.getExitCameraName(), message.getExitCameraIp());
            log.info("  识别权重: {}", message.getExitWeight());
            log.info("  动作类型: {}", message.getAction());
            log.info("========================================");

            // 标准化车牌号（去除连字符）
            String normalizedPlate = message.getExitPlateNumber().replace("-", "");

            // 查询该停车场中是否有该车牌的未出场记录（停车场编号+车牌号）
            Optional<VehicleRecord> entryRecord =
                vehicleRecordRepository.findUnexitedRecordByParkingLotAndPlate(parkingLotCode, normalizedPlate);

            if (entryRecord.isPresent()) {
                // 场景1：正常出场 - 更新入场记录
                return handleNormalExit(entryRecord.get(), message, parkingLotCode);
            } else {
                // 未找到入场记录，查询是否有异常出口记录
                Optional<VehicleRecord> exitOnlyRecord =
                    vehicleRecordRepository.findExitOnlyRecordByParkingLotAndPlate(parkingLotCode, normalizedPlate);

                if (exitOnlyRecord.isPresent()) {
                    // 场景3：异常出口-更新
                    return handleExitOnlyUpdate(exitOnlyRecord.get(), message, parkingLotCode);
                } else {
                    // 场景2：异常出口-新建
                    return handleExitOnlyNew(message, parkingLotCode);
                }
            }

        } catch (Exception e) {
            log.error("!!! 处理出口消息失败! 车牌: {}, 错误: {}",
                message.getExitPlateNumber(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 场景1：正常出场 - 更新入场记录
     */
    private VehicleRecord handleNormalExit(VehicleRecord record, MqttExitMessage message, String parkingLotCode) {
        try {
            String entryPlate = record.getEntryPlateNumber();
            LocalDateTime entryTime = record.getEntryTime();
            LocalDateTime exitTime = LocalDateTime.now();  // 使用服务器当前时间

            // 计算停留时长（秒）
            Integer durationSeconds = null;
            String durationStr = "未知";
            if (entryTime != null) {
                durationSeconds = (int) ChronoUnit.SECONDS.between(entryTime, exitTime);

                // 格式化停留时长
                int hours = durationSeconds / 3600;
                int minutes = (durationSeconds % 3600) / 60;
                int seconds = durationSeconds % 60;
                if (hours > 0) {
                    durationStr = String.format("%d小时%d分%d秒", hours, minutes, seconds);
                } else {
                    durationStr = String.format("%d分%d秒", minutes, seconds);
                }
            }

            // 更新记录
            record.setStatus("exited");
            record.setExitPlateNumber(message.getExitPlateNumber());
            record.setExitTime(exitTime);  // 使用服务器当前时间
            record.setExitCameraIp(message.getExitCameraIp());
            record.setExitPaymentDeviceId(message.getExitPaymentDeviceId());
            record.setExitLedScreenConfig(message.getExitLedScreenConfig());
            record.setExitBarrierGateId(message.getExitBarrierGateId());
            record.setExitCameraId(message.getExitCameraId());
            record.setExitCameraName(message.getExitCameraName());
            record.setExitEventId(message.getExitEventId());
            record.setExitDetectionCount(message.getExitDetectionCount());
            record.setExitWeight(message.getExitWeight());
            record.setExitSnapshot(message.getExitSnapshot());
            record.setDurationSeconds(durationSeconds);

            // 用出场消息的设备配置覆盖更新原有的设备配置
            record.setPaymentDeviceId(message.getPaymentDeviceId());
            record.setLedScreenConfig(message.getLedScreenConfig());
            record.setBarrierGateId(message.getBarrierGateId());
            record.setBackupChannelId(message.getBackupChannelId());

            // 针对所有正常出场记录，计算停车时长和费用
            if (entryTime != null) {
                // 计算停车时长（分钟），不足1分钟按1分钟计算
                long durationMinutes = ChronoUnit.MINUTES.between(entryTime, exitTime);
                // 如果有余数秒，向上取整
                if (durationSeconds != null && durationSeconds % 60 > 0) {
                    durationMinutes++;
                }
                // 确保至少1分钟
                if (durationMinutes < 1) {
                    durationMinutes = 1;
                }

                // 计算停车费用：1分钟 = 1美分，最高9美分
                int parkingFeeCents = (int) Math.min(durationMinutes, 9);

                record.setParkingDurationMinutes((int) durationMinutes);
                record.setParkingFeeCents(parkingFeeCents);

                log.info("💰 停车费用计算 | 记录ID: {} | 时长: {}分钟 | 费用: {}美分",
                    record.getId(), durationMinutes, parkingFeeCents);
            }

            // 保存更新
            VehicleRecord updated = vehicleRecordRepository.save(record);

            log.info("✅ 正常出场记录已更新 | ID: {} | 停车场: {} | 入场车牌: {} | 出场车牌: {} | 停留时长: {} | 权重: {} | 摄像头: {}({})",
                updated.getId(),
                parkingLotCode,
                entryPlate,
                message.getExitPlateNumber(),
                durationStr,
                message.getExitWeight(),
                message.getExitCameraName(),
                message.getExitCameraIp());

            // 针对所有有金额的记录，保存成功后发起支付
            if (updated.getParkingFeeCents() != null && updated.getParkingFeeCents() > 0) {
                try {
                    log.info("=".repeat(80));
                    log.info("🔔 触发支付流程 | 记录ID: {} | 金额: {}美分", updated.getId(), updated.getParkingFeeCents());

                    // 获取支付设备ID（优先使用出场消息中的设备ID，如果为空则使用配置文件中的默认值）
                    String paymentDeviceId = updated.getPaymentDeviceId();
                    if (paymentDeviceId != null && !paymentDeviceId.trim().isEmpty()) {
                        log.info("📟 使用出场消息中的支付设备ID: {}", paymentDeviceId);
                    } else {
                        log.info("📟 使用默认支付设备ID（配置文件）");
                    }
                    log.info("=".repeat(80));

                    // 🔥 同时发起两种支付方式
                    // 1️⃣ 终端支付（POS机）
                    String paymentResponse;
                    if (paymentDeviceId != null && !paymentDeviceId.trim().isEmpty()) {
                        paymentResponse = squareTerminalService.createTerminalCheckout(updated.getParkingFeeCents(), paymentDeviceId);
                    } else {
                        paymentResponse = squareTerminalService.createTerminalCheckout(updated.getParkingFeeCents());
                    }

                    log.info("📱 终端支付请求已发送到设备");
                    log.info("终端支付响应: {}", paymentResponse);

                    // 2️⃣ 在线支付（二维码）
                    String paymentDescription = "停车费 - " + updated.getEntryPlateNumber();
                    String locationId = updated.getParkingLotCode();
                    log.info("📍 使用记录中的停车场编号作为Location ID: {}", locationId);

                    SquareOnlinePaymentService.SquareOnlinePaymentResponse onlinePaymentResponse =
                        squareOnlinePaymentService.createPaymentLink(updated.getParkingFeeCents(), paymentDescription, locationId);

                    if (onlinePaymentResponse.isSuccess()) {
                        log.info("💳 在线支付链接创建成功");
                        log.info("支付URL: {}", onlinePaymentResponse.getPaymentUrl());

                        // 将支付URL保存到车辆记录中，供前端显示二维码
                        updated.setOnlinePaymentUrl(onlinePaymentResponse.getPaymentUrl());
                        updated.setOnlinePaymentLinkId(onlinePaymentResponse.getPaymentLinkId());
                        vehicleRecordRepository.save(updated);

                        // 立即创建PaymentOrder记录，关联到车辆记录
                        // 这样webhook收到支付完成通知时，能通过order_id找到这个PaymentOrder
                        // 并自动更新车辆记录的支付状态
                        try {
                            PaymentOrder onlinePaymentOrder = new PaymentOrder();

                            // 关联车辆记录ID - 关键！
                            onlinePaymentOrder.setVehicleRecordId(updated.getId());

                            // 设置Order ID（Square自动生成的）
                            onlinePaymentOrder.setOrderId(onlinePaymentResponse.getOrderId());

                            // 设置金额信息
                            onlinePaymentOrder.setAmount((long) updated.getParkingFeeCents());
                            onlinePaymentOrder.setCurrency(squareProperties.getCurrency());

                            // 设置状态和来源
                            onlinePaymentOrder.setStatus("PENDING");
                            onlinePaymentOrder.setPaymentSource("ONLINE");

                            // 设置备注
                            onlinePaymentOrder.setNote("在线支付 - 车牌: " + updated.getEntryPlateNumber());

                            // 保存支付记录
                            PaymentOrder savedOnlineOrder = paymentOrderRepository.save(onlinePaymentOrder);

                            log.info("💾 在线支付记录已创建 | 支付记录ID: {} | Order ID: {} | 车辆记录ID: {}",
                                savedOnlineOrder.getId(), onlinePaymentResponse.getOrderId(), updated.getId());

                        } catch (Exception e) {
                            log.error("❌ 创建在线支付记录失败 | 车辆记录ID: {} | 错误: {}",
                                updated.getId(), e.getMessage(), e);
                        }
                    } else {
                        log.warn("⚠️ 在线支付链接创建失败: {}", onlinePaymentResponse.getErrorMessage());
                    }

                    // 解析响应并创建支付记录
                    if (paymentResponse != null && !paymentResponse.startsWith("Error:") && !paymentResponse.startsWith("Exception:")) {
                        try {
                            JsonNode responseJson = objectMapper.readTree(paymentResponse);
                            JsonNode checkoutNode = responseJson.path("checkout");

                            if (!checkoutNode.isMissingNode()) {
                                // 创建支付记录
                                PaymentOrder paymentOrder = new PaymentOrder();

                                // 关联出场记录ID
                                paymentOrder.setVehicleRecordId(updated.getId());

                                // 从响应中提取信息
                                String checkoutId = checkoutNode.path("id").asText(null);
                                String orderId = checkoutNode.path("order_id").asText(null);
                                String referenceId = checkoutNode.path("reference_id").asText(null);
                                String deviceId = checkoutNode.path("device_options").path("device_id").asText(null);

                                paymentOrder.setCheckoutId(checkoutId);
                                paymentOrder.setOrderId(orderId);
                                paymentOrder.setReferenceId(referenceId);
                                paymentOrder.setDeviceId(deviceId);

                                // 设置金额信息
                                paymentOrder.setAmount((long) updated.getParkingFeeCents());
                                paymentOrder.setCurrency("USD");

                                // 设置状态和来源
                                paymentOrder.setStatus("PENDING"); // 初始状态为待处理
                                paymentOrder.setPaymentSource("TERMINAL");

                                // 设置备注
                                paymentOrder.setNote("停车费支付 - 车牌: " + updated.getEntryPlateNumber());

                                // 保存支付记录
                                PaymentOrder savedOrder = paymentOrderRepository.save(paymentOrder);

                                log.info("💾 支付记录已创建 | 支付记录ID: {} | Checkout ID: {} | 出场记录ID: {}",
                                    savedOrder.getId(), checkoutId, updated.getId());

                                // 更新车辆记录的支付状态
                                updated.setPaymentStatus("pending");
                                vehicleRecordRepository.save(updated);

                                log.info("✅ 出场记录支付状态已更新为: pending");
                            }

                        } catch (Exception e) {
                            log.error("❌ 创建支付记录失败 | 出场记录ID: {} | 错误: {}",
                                updated.getId(), e.getMessage(), e);
                        }
                    }

                } catch (Exception e) {
                    log.error("❌ 发起支付失败，但出场记录已保存 | 记录ID: {} | 错误: {}",
                        updated.getId(), e.getMessage(), e);
                    // 注意：支付失败不影响出场记录的保存，继续返回 true
                }
            }

            return updated;

        } catch (Exception e) {
            log.error("❌ 处理正常出场失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 场景2：异常出口-新建记录
     */
    private VehicleRecord handleExitOnlyNew(MqttExitMessage message, String parkingLotCode) {
        try {
            VehicleRecord record = new VehicleRecord();

            // 设置停车场编号
            record.setParkingLotCode(parkingLotCode);

            // 设置状态
            record.setStatus("exit_only");

            // 设置出场信息（使用服务器当前时间）
            record.setExitPlateNumber(message.getExitPlateNumber());
            record.setExitTime(LocalDateTime.now());  // 使用服务器当前时间
            record.setExitCameraIp(message.getExitCameraIp());
            record.setExitPaymentDeviceId(message.getExitPaymentDeviceId());
            record.setExitLedScreenConfig(message.getExitLedScreenConfig());
            record.setExitBarrierGateId(message.getExitBarrierGateId());
            record.setExitCameraId(message.getExitCameraId());
            record.setExitCameraName(message.getExitCameraName());
            record.setExitEventId(message.getExitEventId());
            record.setExitDetectionCount(message.getExitDetectionCount());
            record.setExitWeight(message.getExitWeight());
            record.setExitSnapshot(message.getExitSnapshot());

            // 保存到数据库
            VehicleRecord saved = vehicleRecordRepository.save(record);

            log.warn("⚠️ 异常出口记录已保存 | ID: {} | 停车场: {} | 车牌: {} | 权重: {} | 摄像头: {}({}) | 原因: 无入场记录",
                saved.getId(),
                parkingLotCode,
                message.getExitPlateNumber(),
                message.getExitWeight(),
                message.getExitCameraName(),
                message.getExitCameraIp());

            return saved;

        } catch (Exception e) {
            log.error("❌ 新建异常出口记录失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 场景3：异常出口-更新记录
     */
    private VehicleRecord handleExitOnlyUpdate(VehicleRecord record, MqttExitMessage message, String parkingLotCode) {
        try {
            String oldPlate = record.getExitPlateNumber();

            // 更新出场信息（使用服务器当前时间）
            record.setExitPlateNumber(message.getExitPlateNumber());
            record.setExitTime(LocalDateTime.now());  // 使用服务器当前时间
            record.setExitCameraIp(message.getExitCameraIp());
            record.setExitPaymentDeviceId(message.getExitPaymentDeviceId());
            record.setExitLedScreenConfig(message.getExitLedScreenConfig());
            record.setExitBarrierGateId(message.getExitBarrierGateId());
            record.setExitCameraId(message.getExitCameraId());
            record.setExitCameraName(message.getExitCameraName());
            record.setExitEventId(message.getExitEventId());
            record.setExitDetectionCount(message.getExitDetectionCount());
            record.setExitWeight(message.getExitWeight());
            record.setExitSnapshot(message.getExitSnapshot());

            // 保持状态为 exit_only
            record.setStatus("exit_only");

            // 保存更新
            VehicleRecord updated = vehicleRecordRepository.save(record);

            log.warn("🔄 异常出口记录已更新 | ID: {} | 停车场: {} | 车牌: {} → {} | 权重: {} | 摄像头: {}({}) | 原因: 重复异常出口",
                updated.getId(),
                parkingLotCode,
                oldPlate,
                message.getExitPlateNumber(),
                message.getExitWeight(),
                message.getExitCameraName(),
                message.getExitCameraIp());

            return updated;

        } catch (Exception e) {
            log.error("❌ 更新异常出口记录失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析日期时间字符串
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("日期时间解析失败: {}, 使用当前时间", dateTimeStr);
            return LocalDateTime.now();
        }
    }

    /**
     * 入场成功后自动开闸
     * @param vehicleRecord 车辆记录
     */
    private void autoOpenGateForEntry(VehicleRecord vehicleRecord) {
        try {
            log.info("========================================");
            log.info("🚀 入场成功，开始自动开闸 | 记录ID: {} | 车牌: {}",
                vehicleRecord.getId(), vehicleRecord.getEntryPlateNumber());

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
                    "{\"id\":\"%s\",\"type\":\"modbus\",\"msg\":{\"cmd\":\"opentime\",\"addr\":255,\"channel\":%d,\"time\":10}}",
                    messageId,
                    channel
            );

            log.info("📨 MQTT消息: {}", mqttMessage);
            log.info("  命令: opentime (常开端口打开1秒)");
            log.info("  端口: {}", channel);
            log.info("  时长: 10 (1秒)");

            // 发送MQTT消息
            mqttClientService.publish(topic, mqttMessage);

            log.info("✅ 入场自动开闸指令已发送到MQTT");
            log.info("========================================");

        } catch (Exception e) {
            log.error("❌ 入场自动开闸失败 | 记录ID: {} | 错误: {}", vehicleRecord.getId(), e.getMessage(), e);
        }
    }
}

