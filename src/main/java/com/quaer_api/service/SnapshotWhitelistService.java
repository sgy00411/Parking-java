package com.quaer_api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 快照文件白名单管理服务
 * 用于验证上传的文件是否是MQTT消息中声明的文件
 */
@Slf4j
@Service
public class SnapshotWhitelistService {

    // 文件名白名单：key=filename, value=添加时间
    private final Map<String, LocalDateTime> whitelist = new ConcurrentHashMap<>();

    // 白名单有效期（秒）- 5分钟内有效
    private static final long WHITELIST_EXPIRE_SECONDS = 300;

    /**
     * 添加文件名到白名单
     * @param filename 文件名
     */
    public void addToWhitelist(String filename) {
        if (filename != null && !filename.trim().isEmpty()) {
            whitelist.put(filename, LocalDateTime.now());
            log.info("✅ 文件名已加入白名单: {}", filename);

            // 清理过期的白名单条目
            cleanExpiredEntries();
        }
    }

    /**
     * 检查文件名是否在白名单中
     * @param filename 文件名
     * @return 是否在白名单中
     */
    public boolean isInWhitelist(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }

        LocalDateTime addTime = whitelist.get(filename);
        if (addTime == null) {
            log.warn("⚠️ 文件不在白名单中: {}", filename);
            return false;
        }

        // 检查是否过期
        long seconds = java.time.Duration.between(addTime, LocalDateTime.now()).getSeconds();
        if (seconds > WHITELIST_EXPIRE_SECONDS) {
            log.warn("⚠️ 文件白名单已过期: {} ({}秒前添加)", filename, seconds);
            whitelist.remove(filename);
            return false;
        }

        return true;
    }

    /**
     * 从白名单中移除文件名
     * @param filename 文件名
     */
    public void removeFromWhitelist(String filename) {
        if (filename != null) {
            whitelist.remove(filename);
            log.info("🗑️ 文件名已从白名单移除: {}", filename);
        }
    }

    /**
     * 清理过期的白名单条目
     */
    private void cleanExpiredEntries() {
        LocalDateTime now = LocalDateTime.now();
        whitelist.entrySet().removeIf(entry -> {
            long seconds = java.time.Duration.between(entry.getValue(), now).getSeconds();
            if (seconds > WHITELIST_EXPIRE_SECONDS) {
                log.debug("🧹 清理过期白名单条目: {} ({}秒前添加)", entry.getKey(), seconds);
                return true;
            }
            return false;
        });
    }

    /**
     * 获取白名单大小
     * @return 白名单中的文件数量
     */
    public int getWhitelistSize() {
        cleanExpiredEntries();
        return whitelist.size();
    }
}
