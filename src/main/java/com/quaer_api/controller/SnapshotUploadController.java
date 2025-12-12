package com.quaer_api.controller;

import com.quaer_api.service.SnapshotWhitelistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 停车场快照上传控制器
 * 接收Python端上传的车辆快照图片
 */
@Slf4j
@RestController
@RequestMapping("/api/parking")
public class SnapshotUploadController {

    @Autowired
    private SnapshotWhitelistService whitelistService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 图片存储根目录（从配置文件读取，可选）
    @Value("${snapshot.base-dir:}")
    private String snapshotBaseDir;

    /**
     * 获取快照存���目录
     * 如果配置文件中有指定路径则使用配置的路径，否则根据操作系统自动选择
     */
    private String getSnapshotBaseDir() {
        // 如果配置文件中有指定路径，直接使用
        if (snapshotBaseDir != null && !snapshotBaseDir.trim().isEmpty()) {
            return snapshotBaseDir;
        }

        // 自动根据操作系统选择默认路径
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "D:/停车场/snapshots";
        } else {
            return "/opt/quaer_api/snapshots";
        }
    }

    /**
     * 上传停车场快照图片
     *
     * @param parkingLotCode 停车场编号（如：0001）
     * @param file 图片文件
     * @param filename 图片文件名
     * @return 上传结果
     */
    @PostMapping("/upload-snapshot")
    public ResponseEntity<Map<String, Object>> uploadSnapshot(
            @RequestParam("parking_lot_code") String parkingLotCode,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "filename", required = false) String filename) {

        log.info("=".repeat(80));
        log.info("📸 收到图片上传请求 - 时间: {}", getCurrentTime());
        log.info("=".repeat(80));

        Map<String, Object> result = new HashMap<>();

        try {
            // 参数校验
            if (parkingLotCode == null || parkingLotCode.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "停车场编号不能为空");
                result.put("timestamp", getCurrentTime());
                log.error("❌ 上传失败：停车场编号为空");
                return ResponseEntity.badRequest().body(result);
            }

            if (file == null || file.isEmpty()) {
                result.put("success", false);
                result.put("message", "图片文件不能为空");
                result.put("timestamp", getCurrentTime());
                log.error("❌ 上传失败：文件为空");
                return ResponseEntity.badRequest().body(result);
            }

            // 确定文件名
            String finalFilename = (filename != null && !filename.trim().isEmpty())
                ? filename
                : file.getOriginalFilename();

            if (finalFilename == null || finalFilename.trim().isEmpty()) {
                finalFilename = "snapshot_" + System.currentTimeMillis() + ".jpg";
            }

            log.info("请求信息:");
            log.info("  停车场编号: {}", parkingLotCode);
            log.info("  文件名: {}", finalFilename);
            log.info("  文件大小: {} KB", file.getSize() / 1024);
            log.info("  文件类型: {}", file.getContentType());
            log.info("  存储根目录: {}", getSnapshotBaseDir());

            // 🔒 白名单验证：检查文件名是否在白名单中
            if (!whitelistService.isInWhitelist(finalFilename)) {
                result.put("success", false);
                result.put("message", "文件未经授权，不在白名单中");
                result.put("filename", finalFilename);
                result.put("timestamp", getCurrentTime());
                log.error("❌ 上传失败：文件未经授权（不在白名单中）: {}", finalFilename);
                log.error("   提示：请确保先通过MQTT发送消息，将文件名加入白名单");
                return ResponseEntity.status(403).body(result);
            }

            // 创建停车场专属文件夹
            String baseDir = getSnapshotBaseDir();
            Path parkingLotDir = Paths.get(baseDir, parkingLotCode);
            File dir = parkingLotDir.toFile();
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (created) {
                    log.info("📁 创建停车场文件夹: {}", parkingLotDir);
                } else {
                    log.warn("⚠️ 停车场文件夹已存在或创建失败: {}", parkingLotDir);
                }
            }

            // 保存文件
            Path targetPath = parkingLotDir.resolve(finalFilename);
            file.transferTo(targetPath.toFile());

            // 🗑️ 上传成功后从白名单移除
            whitelistService.removeFromWhitelist(finalFilename);

            log.info("-".repeat(80));
            log.info("✅ 图片上传成功!");
            log.info("  存储路径: {}", targetPath.toAbsolutePath());
            log.info("  停车场编号: {}", parkingLotCode);
            log.info("  文件名: {}", finalFilename);
            log.info("  文件大小: {} KB ({} bytes)", file.getSize() / 1024, file.getSize());
            log.info("  白名单剩余: {} 个文件", whitelistService.getWhitelistSize());
            log.info("=".repeat(80));

            result.put("success", true);
            result.put("message", "图片上传成功");
            result.put("parking_lot_code", parkingLotCode);
            result.put("filename", finalFilename);
            result.put("file_size", file.getSize());
            result.put("file_path", targetPath.toString());
            result.put("timestamp", getCurrentTime());

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            log.error("=".repeat(80));
            log.error("❌ 图片上传失败（IO错误）!");
            log.error("错误信息: {}", e.getMessage());
            log.error("=".repeat(80));

            result.put("success", false);
            result.put("message", "图片保存失败: " + e.getMessage());
            result.put("timestamp", getCurrentTime());

            return ResponseEntity.status(500).body(result);

        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("❌ 图片上传失败!", e);
            log.error("错误信息: {}", e.getMessage());
            log.error("=".repeat(80));

            result.put("success", false);
            result.put("message", "图片上传失败: " + e.getMessage());
            result.put("timestamp", getCurrentTime());

            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 获取当前时间字符串
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}
