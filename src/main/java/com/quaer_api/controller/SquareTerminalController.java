package com.quaer_api.controller;

import com.quaer_api.service.SquareTerminalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Square终端支付测试控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/square/terminal")
@RequiredArgsConstructor
public class SquareTerminalController {

    private final SquareTerminalService terminalService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 测试发起2分钱支付
     */
    @PostMapping("/test-payment")
    public ResponseEntity<Map<String, Object>> testPayment() {
        log.info(">>> 收到测试支付请求 - 金额: 2分 - 时间: {}", getCurrentTime());

        Map<String, Object> result = new HashMap<>();

        try {
            String response = terminalService.createTerminalCheckout(2);

            result.put("success", true);
            result.put("message", "终端支付请求已发送");
            result.put("amount", "2分 ($0.02)");
            result.put("response", response);
            result.put("timestamp", getCurrentTime());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("!!! 测试支付失败", e);
            result.put("success", false);
            result.put("message", "终端支付请求失败: " + e.getMessage());
            result.put("timestamp", getCurrentTime());

            return ResponseEntity.ok(result);
        }
    }

    /**
     * 发起指定金额支付
     */
    @PostMapping("/payment")
    public ResponseEntity<Map<String, Object>> createPayment(@RequestParam long amountInCents) {
        log.info(">>> 收到支付请求 - 金额: {}分 (${}) - 时间: {}",
                amountInCents, formatAmount(amountInCents), getCurrentTime());

        Map<String, Object> result = new HashMap<>();

        try {
            String response = terminalService.createTerminalCheckout(amountInCents);

            result.put("success", true);
            result.put("message", "终端支付请求已发送");
            result.put("amountInCents", amountInCents);
            result.put("amountFormatted", formatAmount(amountInCents) + " CAD");
            result.put("response", response);
            result.put("timestamp", getCurrentTime());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("!!! 支付请求失败", e);
            result.put("success", false);
            result.put("message", "终端支付请求失败: " + e.getMessage());
            result.put("timestamp", getCurrentTime());

            return ResponseEntity.ok(result);
        }
    }

    /**
     * 获取当前时间字符串
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    /**
     * 格式化金额（分转元）
     */
    private String formatAmount(long amountInCents) {
        return String.format("$%.2f", amountInCents / 100.0);
    }
}
