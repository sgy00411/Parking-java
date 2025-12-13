package com.quaer_api.controller;

import com.quaer_api.dto.*;
import com.quaer_api.service.LedDisplayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * LED 广告屏显示控制器
 * 实现广告屏通信协议 MQTT v2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/led")
public class LedDisplayController {

    @Autowired
    private LedDisplayService ledDisplayService;

    /**
     * 搜索 LED 设备
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, String>> searchDevice() {
        log.info("搜索 LED 设备");
        ledDisplayService.searchDevice();

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "已发送搜索命令");
        return ResponseEntity.ok(response);
    }

    /**
     * 发送简单文字
     * POST /api/led/text
     * Body: { "text": "欢迎光临" }
     */
    @PostMapping("/text")
    public ResponseEntity<Map<String, String>> sendText(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        log.info("发送文字到 LED: {}", text);

        ledDisplayService.sendWelcomeText(text);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "文字已发送到 LED 屏幕");
        response.put("text", text);
        return ResponseEntity.ok(response);
    }

    /**
     * 发送车辆欢迎信息
     * POST /api/led/vehicle-welcome
     * Body: { "licensePlate": "粤B12345", "vehicleType": "月租车" }
     */
    @PostMapping("/vehicle-welcome")
    public ResponseEntity<Map<String, String>> sendVehicleWelcome(@RequestBody Map<String, String> request) {
        String licensePlate = request.get("licensePlate");
        String vehicleType = request.get("vehicleType");

        log.info("发送车辆欢迎信息: {} - {}", vehicleType, licensePlate);

        ledDisplayService.sendVehicleWelcome(licensePlate, vehicleType);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "车辆欢迎信息已发送");
        return ResponseEntity.ok(response);
    }

    /**
     * 配置文字广告
     * POST /api/led/advert/text
     */
    @PostMapping("/advert/text")
    public ResponseEntity<Map<String, String>> setTextAdvert(@RequestBody List<LedTextItem> textList) {
        log.info("配置 LED 文字广告，共 {} 行", textList.size());

        ledDisplayService.setTextAdvert(textList);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "文字广告配置已发送");
        return ResponseEntity.ok(response);
    }

    /**
     * 设置余位
     * POST /api/led/park-number
     * Body: { "number": 100, "enabled": true }
     */
    @PostMapping("/park-number")
    public ResponseEntity<Map<String, String>> setParkNumber(@RequestBody Map<String, Object> request) {
        Integer number = (Integer) request.get("number");
        Boolean enabled = (Boolean) request.get("enabled");

        log.info("设置 LED 余位: {} (启用: {})", number, enabled);

        ledDisplayService.setParkNumber(number, enabled);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "余位设置已发送");
        return ResponseEntity.ok(response);
    }

    /**
     * 显示过车界面
     * POST /api/led/scene/passing
     */
    @PostMapping("/scene/passing")
    public ResponseEntity<Map<String, String>> showPassingScene(@RequestBody LedPassingSceneRequest request) {
        log.info("显示过车界面");

        ledDisplayService.showPassingScene(request);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "过车界面已显示");
        return ResponseEntity.ok(response);
    }

    /**
     * 显示支付界面
     * POST /api/led/scene/pay
     */
    @PostMapping("/scene/pay")
    public ResponseEntity<Map<String, String>> showPayScene(@RequestBody LedPaySceneRequest request) {
        log.info("显示支付界面");

        ledDisplayService.showPayScene(request);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "支付界面已显示");
        return ResponseEntity.ok(response);
    }

    /**
     * 显示无牌车扫码界面
     * POST /api/led/scene/unlicensed
     * Body: { "qrcode": "http://...", "voice": "无牌车请扫码", "textList": [...] }
     */
    @PostMapping("/scene/unlicensed")
    public ResponseEntity<Map<String, String>> showUnlicensedScene(@RequestBody Map<String, Object> request) {
        String qrcode = (String) request.get("qrcode");
        String voice = (String) request.get("voice");
        @SuppressWarnings("unchecked")
        List<LedTextItem> textList = (List<LedTextItem>) request.get("textList");

        log.info("显示无牌车扫码界面");

        ledDisplayService.showUnlicensedScene(qrcode, voice, textList);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "无牌车扫码界面已显示");
        return ResponseEntity.ok(response);
    }

    /**
     * 切换回广告界面
     * POST /api/led/scene/advert
     */
    @PostMapping("/scene/advert")
    public ResponseEntity<Map<String, String>> showAdvertScene() {
        log.info("切换回广告界面");

        ledDisplayService.showAdvertScene();

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "已切换回广告界面");
        return ResponseEntity.ok(response);
    }

    /**
     * 测试接口 - 发送"欢迎光临"
     * GET /api/led/test/welcome
     */
    @GetMapping("/test/welcome")
    public ResponseEntity<Map<String, String>> testWelcome() {
        log.info("测试发送：欢迎光临");

        ledDisplayService.sendWelcomeText("欢迎光临");

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "测试消息已发送");
        response.put("text", "欢迎光临");
        return ResponseEntity.ok(response);
    }
}
