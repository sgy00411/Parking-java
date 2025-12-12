package com.quaer_api.controller;

import com.quaer_api.dto.SquareDeviceDTO;
import com.quaer_api.service.SquareDeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Square设备管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/square")
@CrossOrigin(origins = "*")
public class SquareDeviceController {

    @Autowired
    private SquareDeviceService squareDeviceService;

    /**
     * 创建设备配对代码
     */
    @PostMapping("/devices/codes")
    public ResponseEntity<Map<String, Object>> createDeviceCode(
            @RequestBody Map<String, String> request) {
        try {
            String locationId = request.get("locationId");
            String deviceName = request.get("deviceName");

            log.info("创建设备配对代码 - locationId: {}, deviceName: {}", locationId, deviceName);

            if (locationId == null || locationId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Location ID is required");
                return ResponseEntity.status(400).body(errorResponse);
            }

            if (deviceName == null || deviceName.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Device name is required");
                return ResponseEntity.status(400).body(errorResponse);
            }

            SquareDeviceDTO device = squareDeviceService.createDeviceCode(locationId, deviceName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("device", device);
            response.put("message", "设备配对代码创建成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("创建设备配对代码失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create device code");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取指定位置的设备列表
     */
    @GetMapping("/devices")
    public ResponseEntity<Map<String, Object>> getDevices(
            @RequestParam(required = false) String locationId) {
        try {
            log.info("获取设备列表 - locationId: {}", locationId);

            List<SquareDeviceDTO> devices;
            if (locationId != null && !locationId.trim().isEmpty()) {
                devices = squareDeviceService.getDevicesByLocation(locationId);
            } else {
                devices = squareDeviceService.getAllDevices();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("devices", devices);
            response.put("total", devices.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取设备列表失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch devices");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
