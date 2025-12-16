package com.quaer_api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQTT消息去重器
 * 对于同一个车牌号+同一个停车场在3分钟内收到的重复消息进行过滤
 * 间隔时间基于消息本身的timestamp字段，而非服务器接收时间
 */
@Slf4j
@Component
public class MqttMessageDeduplicator {

    /**
     * 消息去重缓存
     * Key: parkingLotCode + "_" + plateNumber + "_" + direction
     * Value: 消息的timestamp时间
     */
    private final Map<String, LocalDateTime> messageCache = new ConcurrentHashMap<>();

    /**
     * 去重时间窗口（分钟）
     */
    private static final int DEDUP_WINDOW_MINUTES = 3;

    /**
     * 日期时间格式化器（与消息中的timestamp格式匹配）
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 检查消息是否为重复消息
     *
     * @param parkingLotCode 停车场编号
     * @param plateNumber 车牌号
     * @param direction 方向（"entry"或"exit"）
     * @param messageTimestamp 消息本身的时间戳（来自消息的timestamp字段）
     * @return true=重复消息（应该忽略），false=新消息（应该处理）
     */
    public boolean isDuplicate(String parkingLotCode, String plateNumber, String direction, String messageTimestamp) {
        if (parkingLotCode == null || plateNumber == null || direction == null || messageTimestamp == null) {
            return false;
        }

        // 标准化车牌号（去除连字符）
        String normalizedPlate = plateNumber.replace("-", "");

        // 生成缓存Key（包含方向）
        String cacheKey = parkingLotCode + "_" + normalizedPlate + "_" + direction;

        // 解析消息时间戳
        LocalDateTime currentMessageTime;
        try {
            currentMessageTime = LocalDateTime.parse(messageTimestamp, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("⚠️ 无法解析消息时间戳: {} | 停车场: {} | 车牌: {}",
                messageTimestamp, parkingLotCode, normalizedPlate);
            return false;  // 如果无法解析时间戳，则不去重
        }

        // 从缓存中获取上次该车牌+方向的消息时间戳
        LocalDateTime lastMessageTime = messageCache.get(cacheKey);

        if (lastMessageTime == null) {
            // 第一次收到该消息，记录时间戳并返回false（不是重复）
            messageCache.put(cacheKey, currentMessageTime);
            log.debug("首次收到消息 | 停车场: {} | 车牌: {} | 方向: {} | 消息时间: {}",
                parkingLotCode, normalizedPlate, direction, messageTimestamp);
            return false;
        }

        // 计算消息时间戳之间的间隔（分钟）
        long minutesDiff = java.time.Duration.between(lastMessageTime, currentMessageTime).toMinutes();

        if (minutesDiff < DEDUP_WINDOW_MINUTES) {
            // 在3分钟内的重复消息
            log.warn("⚠️ 检测到重复消息，已忽略 | 停车场: {} | 车牌: {} | 方向: {} | 上次消息时间: {} | 本次消息时间: {} | 间隔: {}分钟",
                parkingLotCode, normalizedPlate, direction, lastMessageTime, messageTimestamp, minutesDiff);
            return true;
        } else {
            // 超过3分钟，更新时间戳并返回false（不是重复）
            messageCache.put(cacheKey, currentMessageTime);
            log.debug("消息间隔超过3分钟，允许处理 | 停车场: {} | 车牌: {} | 方向: {} | 上次消息时间: {} | 本次消息时间: {} | 间隔: {}分钟",
                parkingLotCode, normalizedPlate, direction, lastMessageTime, messageTimestamp, minutesDiff);
            return false;
        }
    }

    /**
     * 清理过期的缓存记录（超过3分钟的记录）
     * 建议定期调用此方法以释放内存
     */
    public void cleanupExpiredCache() {
        LocalDateTime now = LocalDateTime.now();
        int sizeBefore = messageCache.size();

        messageCache.entrySet().removeIf(entry -> {
            long minutesDiff = java.time.Duration.between(entry.getValue(), now).toMinutes();
            return minutesDiff >= DEDUP_WINDOW_MINUTES;
        });

        int removedCount = sizeBefore - messageCache.size();
        if (removedCount > 0) {
            log.info("清理过期缓存 | 删除记录数: {} | 剩余记录数: {}", removedCount, messageCache.size());
        }
    }

    /**
     * 获取当前缓存大小（用于监控）
     */
    public int getCacheSize() {
        return messageCache.size();
    }

    /**
     * 清空所有缓存（用于测试或重置）
     */
    public void clearAll() {
        int size = messageCache.size();
        messageCache.clear();
        log.info("已清空所有缓存 | 清除记录数: {}", size);
    }
}
