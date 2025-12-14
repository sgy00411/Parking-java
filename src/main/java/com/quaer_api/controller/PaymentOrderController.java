package com.quaer_api.controller;

import com.quaer_api.entity.PaymentOrder;
import com.quaer_api.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 支付订单Controller
 * 提供支付订单查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/payment-orders")
@RequiredArgsConstructor
public class PaymentOrderController {

    private final PaymentOrderRepository paymentOrderRepository;

    /**
     * 查询支付订单列表（分页）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPaymentOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentSource
    ) {
        try {
            log.info("查询支付订单列表: page={}, pageSize={}, status={}, paymentSource={}",
                page, pageSize, status, paymentSource);

            Pageable pageable = PageRequest.of(
                page - 1,
                pageSize,
                Sort.by(Sort.Direction.DESC, "id")
            );

            Page<PaymentOrder> paymentOrderPage;

            // 根据条件查询
            if (status != null && !status.isEmpty()) {
                paymentOrderPage = paymentOrderRepository.findByStatus(status, pageable);
            } else if (paymentSource != null && !paymentSource.isEmpty()) {
                paymentOrderPage = paymentOrderRepository.findByPaymentSource(paymentSource, pageable);
            } else {
                paymentOrderPage = paymentOrderRepository.findAll(pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", paymentOrderPage.getContent());
            response.put("total", paymentOrderPage.getTotalElements());
            response.put("currentPage", page);
            response.put("totalPages", paymentOrderPage.getTotalPages());

            log.info("查询结果: 总记录数={}, 当前页记录数={}",
                paymentOrderPage.getTotalElements(),
                paymentOrderPage.getContent().size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("查询支付订单列表失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 根据ID查询支付订单详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPaymentOrderById(@PathVariable Long id) {
        try {
            log.info("查询支付订单详情: id={}", id);

            Optional<PaymentOrder> paymentOrder = paymentOrderRepository.findById(id);

            if (paymentOrder.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", paymentOrder.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "支付订单不存在");
                return ResponseEntity.status(404).body(errorResponse);
            }

        } catch (Exception e) {
            log.error("查询支付订单详情失败: id={}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 根据Square Payment ID查询支付订单
     */
    @GetMapping("/by-payment-id/{paymentId}")
    public ResponseEntity<Map<String, Object>> getPaymentOrderByPaymentId(@PathVariable String paymentId) {
        try {
            log.info("根据PaymentID查询支付订单: paymentId={}", paymentId);

            Optional<PaymentOrder> paymentOrder = paymentOrderRepository.findBySquarePaymentId(paymentId);

            if (paymentOrder.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", paymentOrder.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "支付订单不存在");
                return ResponseEntity.status(404).body(errorResponse);
            }

        } catch (Exception e) {
            log.error("根据PaymentID查询支付订单失败: paymentId={}", paymentId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 根据Square Order ID查询支付订单
     */
    @GetMapping("/by-order-id/{orderId}")
    public ResponseEntity<Map<String, Object>> getPaymentOrderByOrderId(@PathVariable String orderId) {
        try {
            log.info("根据OrderID查询支付订单: orderId={}", orderId);

            Optional<PaymentOrder> paymentOrder = paymentOrderRepository.findByOrderId(orderId);

            if (paymentOrder.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", paymentOrder.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "支付订单不存在");
                return ResponseEntity.status(404).body(errorResponse);
            }

        } catch (Exception e) {
            log.error("根据OrderID查询支付订单失败: orderId={}", orderId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 根据车辆记录ID查询支付订单
     */
    @GetMapping("/by-vehicle-record/{vehicleRecordId}")
    public ResponseEntity<Map<String, Object>> getPaymentOrderByVehicleRecordId(@PathVariable Long vehicleRecordId) {
        try {
            log.info("根据车辆记录ID查询支付订单: vehicleRecordId={}", vehicleRecordId);

            Optional<PaymentOrder> paymentOrder = paymentOrderRepository.findByVehicleRecordId(vehicleRecordId);

            if (paymentOrder.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", paymentOrder.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "支付订单不存在");
                return ResponseEntity.status(404).body(errorResponse);
            }

        } catch (Exception e) {
            log.error("根据车辆记录ID查询支付订单失败: vehicleRecordId={}", vehicleRecordId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
