package com.quaer_api.controller;

import com.quaer_api.entity.PaymentOrder;
import com.quaer_api.entity.VehicleRecord;
import com.quaer_api.repository.PaymentOrderRepository;
import com.quaer_api.repository.VehicleRecordRepository;
import com.quaer_api.service.LedDisplayService;
import com.quaer_api.service.SquareOnlinePaymentService;
import com.quaer_api.service.SquareTerminalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Predicate;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车辆出入口记录API控制器 - 只读
 */
@Slf4j
@RestController
@RequestMapping("/api/vehicle-records")
@CrossOrigin(origins = "*")
public class VehicleRecordController {

    @Autowired
    private VehicleRecordRepository vehicleRecordRepository;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private SquareOnlinePaymentService squareOnlinePaymentService;

    @Autowired
    private SquareTerminalService squareTerminalService;

    @Autowired
    private LedDisplayService ledDisplayService;

    @Value("${snapshot.base-dir:}")
    private String snapshotBaseDir;

    /**
     * 获取快照存储根目录
     */
    private String getSnapshotBaseDir() {
        // 1. 优先使用配置文件中的路径
        if (snapshotBaseDir != null && !snapshotBaseDir.trim().isEmpty()) {
            return snapshotBaseDir;
        }

        // 2. 根据操作系统自动选择默认路径
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "D:/停车场/snapshots";
        } else {
            return "/opt/quaer_api/snapshots";
        }
    }

    /**
     * 获取车辆记录列表（分页+筛选）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getVehicleRecords(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "plateNumber", required = false) String plateNumber,
            @RequestParam(value = "parkingLotCode", required = false) String parkingLotCode,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        try {
            log.info("获取车辆记录列表: page={}, pageSize={}, status={}, plateNumber={}, parkingLotCode={}, startDate={}, endDate={}",
                    page, pageSize, status, plateNumber, parkingLotCode, startDate, endDate);

            // 创建分页对象（页码从0开始）
            Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));

            // 构建动态查询条件
            Specification<VehicleRecord> spec = (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();

                // 状态筛选
                if (status != null && !status.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), status));
                }

                // 车牌号筛选（模糊搜索入口和出口车牌）
                if (plateNumber != null && !plateNumber.trim().isEmpty()) {
                    String pattern = "%" + plateNumber.trim() + "%";
                    Predicate entryPlate = criteriaBuilder.like(root.get("entryPlateNumber"), pattern);
                    Predicate exitPlate = criteriaBuilder.like(root.get("exitPlateNumber"), pattern);
                    predicates.add(criteriaBuilder.or(entryPlate, exitPlate));
                }

                // 停车场编号筛选
                if (parkingLotCode != null && !parkingLotCode.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("parkingLotCode"), parkingLotCode));
                }

                // 日期范围筛选（基于入场时间或出场时间）
                if (startDate != null && !startDate.trim().isEmpty()) {
                    try {
                        LocalDateTime startDateTime = LocalDate.parse(startDate).atStartOfDay();
                        Predicate entryAfter = criteriaBuilder.greaterThanOrEqualTo(root.get("entryTime"), startDateTime);
                        Predicate exitAfter = criteriaBuilder.greaterThanOrEqualTo(root.get("exitTime"), startDateTime);
                        predicates.add(criteriaBuilder.or(entryAfter, exitAfter));
                    } catch (Exception e) {
                        log.warn("无效的开始日期格式: {}", startDate);
                    }
                }

                if (endDate != null && !endDate.trim().isEmpty()) {
                    try {
                        LocalDateTime endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);
                        Predicate entryBefore = criteriaBuilder.lessThanOrEqualTo(root.get("entryTime"), endDateTime);
                        Predicate exitBefore = criteriaBuilder.lessThanOrEqualTo(root.get("exitTime"), endDateTime);
                        predicates.add(criteriaBuilder.or(entryBefore, exitBefore));
                    } catch (Exception e) {
                        log.warn("无效的结束日期格式: {}", endDate);
                    }
                }

                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };

            // 执行分页查询
            Page<VehicleRecord> recordPage = vehicleRecordRepository.findAll(spec, pageable);

            log.info("查询结果: 总记录数={}, 当前页记录数={}", recordPage.getTotalElements(), recordPage.getContent().size());

            // 构建返回数据
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("records", recordPage.getContent());
            response.put("total", recordPage.getTotalElements());
            response.put("page", page);
            response.put("pageSize", pageSize);
            response.put("totalPages", recordPage.getTotalPages());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取车辆记录列表失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取车辆记录失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取单条车辆记录详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getVehicleRecordDetail(@PathVariable Long id) {
        try {
            log.info("获取车辆记录详情: id={}", id);

            return vehicleRecordRepository.findById(id)
                    .map(record -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("record", record);
                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("success", false);
                        errorResponse.put("message", "记录不存在");
                        return ResponseEntity.status(404).body(errorResponse);
                    });

        } catch (Exception e) {
            log.error("获取车辆记录详情失败: id={}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取记录详情失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取快照图片
     */
    @GetMapping("/snapshot/{parkingLotCode}/{filename}")
    public ResponseEntity<Resource> getSnapshot(
            @PathVariable String parkingLotCode,
            @PathVariable String filename
    ) {
        try {
            log.info("获取快照图片: parkingLotCode={}, filename={}", parkingLotCode, filename);

            Path filePath = Paths.get(getSnapshotBaseDir(), parkingLotCode, filename);
            File file = filePath.toFile();

            if (!file.exists()) {
                log.warn("快照文件不存在: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "image/jpeg";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("获取快照图片失败: parkingLotCode={}, filename={}", parkingLotCode, filename, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 获取统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam(value = "parkingLotCode", required = false) String parkingLotCode
    ) {
        try {
            log.info("获取统计信息: parkingLotCode={}", parkingLotCode);

            List<VehicleRecord> allRecords;

            if (parkingLotCode != null && !parkingLotCode.trim().isEmpty()) {
                Specification<VehicleRecord> spec = (root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("parkingLotCode"), parkingLotCode);
                allRecords = vehicleRecordRepository.findAll(spec);
            } else {
                allRecords = vehicleRecordRepository.findAll();
            }

            long totalRecords = allRecords.size();
            long enteredCount = allRecords.stream().filter(r -> "entered".equals(r.getStatus())).count();
            long exitedCount = allRecords.stream().filter(r -> "exited".equals(r.getStatus())).count();
            long exitOnlyCount = allRecords.stream().filter(r -> "exit_only".equals(r.getStatus())).count();

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("total", totalRecords);
            statistics.put("entered", enteredCount);
            statistics.put("exited", exitedCount);
            statistics.put("exitOnly", exitOnlyCount);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", statistics);

            log.info("统计结果: total={}, entered={}, exited={}, exitOnly={}",
                    totalRecords, enteredCount, exitedCount, exitOnlyCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 手动发起双通道支付(终端 + 在线)
     *
     * 访问示例：
     * POST http://localhost:8086/api/vehicle-records/{id}/initiate-payment
     *
     * @param id 车辆记录ID
     * @param paymentDeviceId 终端设备ID(可选)
     * @return 支付结果
     */
    @PostMapping("/{id}/initiate-payment")
    public ResponseEntity<Map<String, Object>> initiatePayment(
            @PathVariable Long id,
            @RequestParam(value = "paymentDeviceId", required = false) String paymentDeviceId
    ) {
        try {
            log.info("========================================");
            log.info("手动发起双通道支付: 记录ID={}, 设备ID={}", id, paymentDeviceId);
            log.info("========================================");

            // 查找记录
            VehicleRecord record = vehicleRecordRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("记录不存在: " + id));

            // 检查是否有停车费
            if (record.getParkingFeeCents() == null || record.getParkingFeeCents() <= 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "该记录没有停车费,无法发起支付");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 检查是否已支付
            if ("paid".equals(record.getPaymentStatus())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "该记录已支付,无需重复支付");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 1️⃣ 发起终端支付
            String terminalPaymentResponse = null;
            boolean terminalSuccess = false;
            try {
                // 优先级: 1) 数据库记录中的设备ID 2) 前端传来的设备ID 3) 配置文件默认值
                if (record.getPaymentDeviceId() != null && !record.getPaymentDeviceId().trim().isEmpty()) {
                    log.info("📟 使用记录中的支付设备ID: {}", record.getPaymentDeviceId());
                    terminalPaymentResponse = squareTerminalService.createTerminalCheckout(
                            record.getParkingFeeCents(), record.getPaymentDeviceId());
                } else if (paymentDeviceId != null && !paymentDeviceId.trim().isEmpty()) {
                    log.info("📟 使用前端传入的支付设备ID: {}", paymentDeviceId);
                    terminalPaymentResponse = squareTerminalService.createTerminalCheckout(
                            record.getParkingFeeCents(), paymentDeviceId);
                } else {
                    log.info("📟 使用默认支付设备ID（配置文件）");
                    terminalPaymentResponse = squareTerminalService.createTerminalCheckout(
                            record.getParkingFeeCents());
                }
                terminalSuccess = true;
                log.info("✅ 终端支付请求已发送");
            } catch (Exception e) {
                log.error("❌ 终端支付失败: {}", e.getMessage());
                terminalPaymentResponse = "终端支付失败: " + e.getMessage();
            }

            // 2️⃣ 发起在线支付
            String onlinePaymentUrl = null;
            String onlinePaymentLinkId = null;
            boolean onlineSuccess = false;
            try {
                String description = "停车费 - " +
                    (record.getEntryPlateNumber() != null ? record.getEntryPlateNumber() : record.getExitPlateNumber());

                // 使用记录中的parking_lot_code作为location_id
                String locationId = record.getParkingLotCode();
                log.info("📍 使用记录中的停车场编号作为Location ID: {}", locationId);

                SquareOnlinePaymentService.SquareOnlinePaymentResponse onlineResponse =
                        squareOnlinePaymentService.createPaymentLink(record.getParkingFeeCents(), description, locationId);

                if (onlineResponse.isSuccess()) {
                    onlinePaymentUrl = onlineResponse.getPaymentUrl();
                    onlinePaymentLinkId = onlineResponse.getPaymentLinkId();
                    String squareOrderId = onlineResponse.getOrderId();

                    // 保存在线支付链接到车辆记录
                    record.setOnlinePaymentUrl(onlinePaymentUrl);
                    record.setOnlinePaymentLinkId(onlinePaymentLinkId);
                    record.setPaymentStatus("pending");
                    vehicleRecordRepository.save(record);

                    // 🔥 关键修复：同时创建payment_orders记录，保存vehicle_record_id和order_id
                    // 这样webhook回来时就能找到记录并更新车辆记录的支付状态
                    PaymentOrder paymentOrder = new PaymentOrder();
                    paymentOrder.setOrderId(squareOrderId);
                    paymentOrder.setVehicleRecordId(id);  // 保存车辆记录ID
                    paymentOrder.setAmount(record.getParkingFeeCents().longValue());  // Integer转Long
                    paymentOrder.setCurrency("CAD");
                    paymentOrder.setStatus("PENDING");
                    paymentOrder.setPaymentSource("ONLINE");
                    paymentOrder.setNote("在线支付 - " + description);
                    paymentOrderRepository.save(paymentOrder);

                    log.info("✅ PaymentOrder记录已创建: OrderID={}, VehicleRecordID={}, Amount={}",
                            squareOrderId, id, record.getParkingFeeCents());

                    onlineSuccess = true;
                    log.info("✅ 在线支付链接已生成: {}", onlinePaymentUrl);
                } else {
                    log.error("❌ 在线支付失败: {}", onlineResponse.getErrorMessage());
                }
            } catch (Exception e) {
                log.error("❌ 在线支付失败: {}", e.getMessage());
            }

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", terminalSuccess || onlineSuccess);
            response.put("recordId", id);
            response.put("amount", record.getParkingFeeCents());

            Map<String, Object> terminalResult = new HashMap<>();
            terminalResult.put("success", terminalSuccess);
            terminalResult.put("response", terminalPaymentResponse);
            response.put("terminal", terminalResult);

            Map<String, Object> onlineResult = new HashMap<>();
            onlineResult.put("success", onlineSuccess);
            onlineResult.put("paymentUrl", onlinePaymentUrl);
            onlineResult.put("paymentLinkId", onlinePaymentLinkId);
            response.put("online", onlineResult);

            if (terminalSuccess && onlineSuccess) {
                response.put("message", "终端支付和在线支付已同时发起");
            } else if (terminalSuccess) {
                response.put("message", "仅终端支付成功,在线支付失败");
            } else if (onlineSuccess) {
                response.put("message", "仅在线支付成功,终端支付失败");
            } else {
                response.put("message", "终端支付和在线支付均失败");
            }

            log.info("========================================");
            log.info("双通道支付发起完成: 终端={}, 在线={}", terminalSuccess, onlineSuccess);
            log.info("========================================");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("发起支付失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "发起支付失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 批量为已有费用的记录生成支付链接
     *
     * 访问示例：
     * http://localhost:8086/api/vehicle-records/generate-payment-links
     */
    @PostMapping("/generate-payment-links")
    public ResponseEntity<Map<String, Object>> generatePaymentLinks() {
        try {
            log.info("========================================");
            log.info("开始批量生成支付链接");
            log.info("========================================");

            // 查找所有有停车费但没有支付链接的记录（状态=exited，有费用，无支付URL）
            Specification<VehicleRecord> spec = (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(criteriaBuilder.equal(root.get("status"), "exited"));
                predicates.add(criteriaBuilder.isNotNull(root.get("parkingFeeCents")));
                predicates.add(criteriaBuilder.greaterThan(root.get("parkingFeeCents"), 0));
                predicates.add(criteriaBuilder.isNull(root.get("onlinePaymentUrl")));
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };

            List<VehicleRecord> records = vehicleRecordRepository.findAll(spec);

            log.info("找到 {} 条需要生成支付链接的记录", records.size());

            int successCount = 0;
            int failureCount = 0;

            for (VehicleRecord record : records) {
                try {
                    String description = "停车费 - " + (record.getEntryPlateNumber() != null ? record.getEntryPlateNumber() : record.getExitPlateNumber());

                    // 使用记录中的parking_lot_code作为location_id
                    String locationId = record.getParkingLotCode();

                    // 生成支付链接
                    SquareOnlinePaymentService.SquareOnlinePaymentResponse response =
                            squareOnlinePaymentService.createPaymentLink(record.getParkingFeeCents(), description, locationId);

                    if (response.isSuccess()) {
                        // 创建PaymentOrder记录并关联vehicle_record_id
                        PaymentOrder paymentOrder = new PaymentOrder();
                        paymentOrder.setOrderId(response.getOrderId());
                        paymentOrder.setVehicleRecordId(record.getId().longValue());
                        paymentOrder.setAmount(record.getParkingFeeCents().longValue());
                        paymentOrder.setCurrency("CAD");
                        paymentOrder.setStatus("PENDING");
                        paymentOrder.setPaymentSource("ONLINE");
                        paymentOrder.setNote(description);
                        paymentOrderRepository.save(paymentOrder);

                        log.info("✅ 创建PaymentOrder记录: OrderID={}, VehicleRecordID={}", response.getOrderId(), record.getId());

                        // 保存支付链接到记录
                        record.setOnlinePaymentUrl(response.getPaymentUrl());
                        record.setOnlinePaymentLinkId(response.getPaymentLinkId());
                        vehicleRecordRepository.save(record);

                        log.info("✅ 记录ID: {} 支付链接生成成功", record.getId());
                        successCount++;
                    } else {
                        log.error("❌ 记录ID: {} 支付链接生成失败: {}", record.getId(), response.getErrorMessage());
                        failureCount++;
                    }

                    // 避免请求过快,暂停100ms
                    Thread.sleep(100);

                } catch (Exception e) {
                    log.error("❌ 记录ID: {} 处理失败: {}", record.getId(), e.getMessage());
                    failureCount++;
                }
            }

            log.info("========================================");
            log.info("批量生成完成");
            log.info("  成功: {} 条", successCount);
            log.info("  失败: {} 条", failureCount);
            log.info("========================================");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalRecords", records.size());
            response.put("successCount", successCount);
            response.put("failureCount", failureCount);
            response.put("message", String.format("批量生成完成: 成功 %d 条, 失败 %d 条", successCount, failureCount));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("批量生成支付链接失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "批量生成失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 发起LCD支付 - 在LCD屏幕显示支付二维码
     *
     * 访问示例：
     * POST http://localhost:8086/api/vehicle-records/{id}/lcd-payment
     *
     * @param id 车辆记录ID
     * @return 发送结果
     */
    @PostMapping("/{id}/lcd-payment")
    public ResponseEntity<Map<String, Object>> sendLcdPayment(@PathVariable Long id) {
        try {
            log.info("========================================");
            log.info("发起LCD支付: 记录ID={}", id);
            log.info("========================================");

            // 查找记录
            VehicleRecord record = vehicleRecordRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("记录不存在: " + id));

            // 检查是否有在线支付链接
            if (record.getOnlinePaymentUrl() == null || record.getOnlinePaymentUrl().trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "该记录没有在线支付链接,请先生成支付链接");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 检查是否有LED屏幕配置
            if (record.getLedScreenConfig() == null || record.getLedScreenConfig().trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "该记录没有LED屏幕配置信息");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 获取LCD设备CID
            String ledDeviceCid = record.getLedScreenConfig();
            log.info("📺 LCD设备CID: {}", ledDeviceCid);

            // 准备显示的文本内容
            List<com.quaer_api.dto.LedTextItem> textList = new ArrayList<>();

            // 第一行：车牌号（绿色）
            String plateNumber = record.getEntryPlateNumber() != null ?
                record.getEntryPlateNumber() : record.getExitPlateNumber();
            com.quaer_api.dto.LedTextItem line1 = new com.quaer_api.dto.LedTextItem();
            line1.setLid(0);
            line1.setText("车牌: " + plateNumber);
            line1.setColor(com.quaer_api.dto.LedTextColor.green());
            textList.add(line1);

            // 第二行：停车时长（黄色）
            String durationText = "时长: ";
            if (record.getParkingDurationMinutes() != null) {
                int minutes = record.getParkingDurationMinutes();
                int hours = minutes / 60;
                int mins = minutes % 60;
                if (hours > 0) {
                    durationText += hours + "小时" + mins + "分钟";
                } else {
                    durationText += mins + "分钟";
                }
            } else {
                durationText += "未知";
            }
            com.quaer_api.dto.LedTextItem line2 = new com.quaer_api.dto.LedTextItem();
            line2.setLid(1);
            line2.setText(durationText);
            line2.setColor(com.quaer_api.dto.LedTextColor.yellow());
            textList.add(line2);

            // 第三行：停车费用（红色）
            String amountText = "金额: ";
            if (record.getParkingFeeCents() != null) {
                amountText += "$" + String.format("%.2f", record.getParkingFeeCents() / 100.0);
            } else {
                amountText += "$0.00";
            }
            com.quaer_api.dto.LedTextItem line3 = new com.quaer_api.dto.LedTextItem();
            line3.setLid(2);
            line3.setText(amountText);
            line3.setColor(com.quaer_api.dto.LedTextColor.red());
            textList.add(line3);

            // 第四行：请付款（白色）
            com.quaer_api.dto.LedTextItem line4 = new com.quaer_api.dto.LedTextItem();
            line4.setLid(3);
            line4.setText("请扫码付款");
            line4.setColor(com.quaer_api.dto.LedTextColor.white());
            textList.add(line4);

            // 创建支付场景请求
            com.quaer_api.dto.LedPaySceneRequest paySceneRequest =
                new com.quaer_api.dto.LedPaySceneRequest();
            paySceneRequest.setShowTime(120);  // 显示120秒（2分钟）
            paySceneRequest.setQrcode(record.getOnlinePaymentUrl());
            paySceneRequest.setVoice("");  // 不使用语音（空字符串）
            paySceneRequest.setTextList(textList);

            // 发送到指定的LCD屏幕设备
            ledDisplayService.showPaySceneToDevice(ledDeviceCid, paySceneRequest);

            log.info("✅ LCD支付界面已发送到设备: {}", ledDeviceCid);
            log.info("========================================");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "LCD支付界面已发送");
            response.put("ledDeviceCid", ledDeviceCid);
            response.put("paymentUrl", record.getOnlinePaymentUrl());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("发起LCD支付失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "发起LCD支付失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
