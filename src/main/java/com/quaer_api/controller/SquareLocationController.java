package com.quaer_api.controller;

import com.quaer_api.dto.SquareLocationDTO;
import com.quaer_api.service.SquareLocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Square位置管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/square")
@CrossOrigin(origins = "*")
public class SquareLocationController {

    @Autowired
    private SquareLocationService squareLocationService;

    /**
     * 获取位置列表（支持分页和搜索）
     */
    @GetMapping("/locations")
    public ResponseEntity<Map<String, Object>> getLocations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        try {
            log.info("获取位置列表 - page: {}, size: {}, search: {}", page, size, search);

            // 获取所有位置
            List<SquareLocationDTO> allLocations = squareLocationService.getLocations();

            // 如果有搜索条件，进行过滤
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase();
                allLocations = allLocations.stream()
                        .filter(location ->
                                (location.getName() != null && location.getName().toLowerCase().contains(searchLower)) ||
                                (location.getBusinessName() != null && location.getBusinessName().toLowerCase().contains(searchLower)) ||
                                (location.getId() != null && location.getId().toLowerCase().contains(searchLower))
                        )
                        .collect(Collectors.toList());
            }

            // 计算分页
            int total = allLocations.size();
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, total);

            List<SquareLocationDTO> pagedLocations;
            if (startIndex < total) {
                pagedLocations = allLocations.subList(startIndex, endIndex);
            } else {
                pagedLocations = List.of();
            }

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("content", pagedLocations);
            response.put("totalElements", total);
            response.put("totalPages", (int) Math.ceil((double) total / size));
            response.put("currentPage", page);
            response.put("pageSize", size);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取位置列表失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch locations");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 根据ID获取位置详情
     */
    @GetMapping("/locations/{id}")
    public ResponseEntity<SquareLocationDTO> getLocationById(@PathVariable String id) {
        try {
            log.info("获取位置详情 - id: {}", id);
            SquareLocationDTO location = squareLocationService.getLocationById(id);
            return ResponseEntity.ok(location);
        } catch (Exception e) {
            log.error("获取位置详情失败: {}", id, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 设置当前使用的位置（仅更新配置，不修改Square账户）
     */
    @PostMapping("/locations/{id}/set-current")
    public ResponseEntity<Map<String, Object>> setCurrentLocation(@PathVariable String id) {
        try {
            log.info("设置当前位置 - id: {}", id);

            // 验证位置是否存在
            SquareLocationDTO location = squareLocationService.getLocationById(id);

            if (location == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Location not found");
                return ResponseEntity.status(404).body(errorResponse);
            }

            // TODO: 这里可以更新到数据库或配置文件
            // 目前仅返回成功，实际使用时location-id在application.yml中配置

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "位置设置成功，请在配置文件中更新 square.location-id");
            response.put("locationId", id);
            response.put("locationName", location.getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("设置当前位置失败: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to set current location");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
