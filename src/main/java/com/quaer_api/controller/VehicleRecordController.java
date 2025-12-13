package com.quaer_api.controller;

import com.quaer_api.entity.VehicleRecord;
import com.quaer_api.repository.VehicleRecordRepository;
import com.quaer_api.service.SquareOnlinePaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private SquareOnlinePaymentService squareOnlinePaymentService;

    private static final String SNAPSHOT_BASE_DIR = "D:/停车场/snapshots";

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

            Path filePath = Paths.get(SNAPSHOT_BASE_DIR, parkingLotCode, filename);
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

                    // 生成支付链接
                    SquareOnlinePaymentService.SquareOnlinePaymentResponse response =
                            squareOnlinePaymentService.createPaymentLink(record.getParkingFeeCents(), description);

                    if (response.isSuccess()) {
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
}
