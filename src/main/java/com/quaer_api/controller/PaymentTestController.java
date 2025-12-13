package com.quaer_api.controller;

import com.quaer_api.service.SquareOnlinePaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 支付测试控制器
 * 用于测试Square在线支付功能
 */
@Slf4j
@RestController
@RequestMapping("/api/test/payment")
@RequiredArgsConstructor
public class PaymentTestController {

    private final SquareOnlinePaymentService squareOnlinePaymentService;

    /**
     * 测试创建在线支付链接
     *
     * 访问示例：
     * http://localhost:8086/api/test/payment/create?amount=100&description=测试停车费
     *
     * @param amount 金额（美分），默认100（即$1.00）
     * @param description 支付描述，默认"测试停车费"
     * @return 支付链接信息
     */
    @GetMapping("/create")
    public SquareOnlinePaymentService.SquareOnlinePaymentResponse createTestPayment(
            @RequestParam(defaultValue = "100") long amount,
            @RequestParam(defaultValue = "测试停车费 - AB1234") String description) {

        log.info("========================================");
        log.info("收到测试支付请求");
        log.info("  金额: {} 美分 (${:.2f})", amount, amount / 100.0);
        log.info("  描述: {}", description);
        log.info("========================================");

        // 调用服务创建支付链接
        SquareOnlinePaymentService.SquareOnlinePaymentResponse response =
            squareOnlinePaymentService.createPaymentLink(amount, description);

        if (response.isSuccess()) {
            log.info("测试支付链接创建成功!");
            log.info("  支付URL: {}", response.getPaymentUrl());
            log.info("  Payment Link ID: {}", response.getPaymentLinkId());
        } else {
            log.error("测试支付链接创建失败: {}", response.getErrorMessage());
        }

        return response;
    }

    /**
     * 快速测试 - 固定金额1美元
     *
     * 访问示例：
     * http://localhost:8086/api/test/payment/quick
     *
     * @return 支付链接信息
     */
    @GetMapping("/quick")
    public SquareOnlinePaymentService.SquareOnlinePaymentResponse createQuickPayment() {
        log.info("快速测试支付 - 固定金额 $1.00");
        return squareOnlinePaymentService.createPaymentLink(100, "测试停车费 - 快速测试");
    }
}
