package com.quaer_api.controller;

import com.quaer_api.util.MqttMessageTestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 * 用于测试MQTT消息接收和数据库写入
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private MqttMessageTestUtil mqttMessageTestUtil;

    /**
     * 发送测试入场消息（新车入场）
     * GET /api/test/entry/new?plate=ABC-1234
     */
    @GetMapping("/entry/new")
    public Map<String, Object> testEntryNew(@RequestParam String plate) {
        log.info("=== 测试接口：发送新车入场消息 ===");
        log.info("车牌号: {}", plate);

        mqttMessageTestUtil.sendTestEntryNewMessage(plate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "测试入场消息已发送");
        response.put("plateNumber", plate);
        response.put("action", "entry_new");

        return response;
    }

    /**
     * 发送测试入场消息（重复入场）
     * GET /api/test/entry/update?plate=ABC-1234&recordId=1
     */
    @GetMapping("/entry/update")
    public Map<String, Object> testEntryUpdate(
            @RequestParam String plate,
            @RequestParam Long recordId) {

        log.info("=== 测试接口：发送重复入场消息 ===");
        log.info("车牌号: {}", plate);
        log.info("记录ID: {}", recordId);

        mqttMessageTestUtil.sendTestEntryUpdateMessage(plate, recordId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "测试入场更新消息已发送");
        response.put("plateNumber", plate);
        response.put("recordId", recordId);
        response.put("action", "entry_update");

        return response;
    }

    /**
     * 发送测试出口消息（正常出场）
     * GET /api/test/exit/normal?plate=ABC-1234
     */
    @GetMapping("/exit/normal")
    public Map<String, Object> testExitNormal(@RequestParam String plate) {
        log.info("=== 测试接口：发送正常出场消息 ===");
        log.info("车牌号: {}", plate);

        mqttMessageTestUtil.sendTestExitNormalMessage(plate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "测试出口消息已发送（正常出场）");
        response.put("plateNumber", plate);
        response.put("action", "exit_normal");

        return response;
    }

    /**
     * 发送测试出口消息（异常出口-无入场记录）
     * GET /api/test/exit/only?plate=XYZ-9999
     */
    @GetMapping("/exit/only")
    public Map<String, Object> testExitOnly(@RequestParam String plate) {
        log.info("=== 测试接口：发送异常出口消息 ===");
        log.info("车牌号: {}", plate);

        mqttMessageTestUtil.sendTestExitOnlyMessage(plate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "测试异常出口消息已发送（无入场记录）");
        response.put("plateNumber", plate);
        response.put("action", "exit_only_new");

        return response;
    }

    /**
     * 健康检查
     * GET /api/test/health
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "测试接口运行正常");
        return response;
    }
}
