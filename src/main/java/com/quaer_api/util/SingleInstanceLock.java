package com.quaer_api.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * 单进程锁工具类
 * 确保应用程序在同一时间只能运行一个实例
 */
@Slf4j
public class SingleInstanceLock {

    private static final String LOCK_FILE = "quaer_api.lock";
    private FileChannel fileChannel;
    private FileLock fileLock;
    private RandomAccessFile randomAccessFile;

    /**
     * 尝试获取单实例锁
     * @return true表示成功获取锁，false表示已有其他实例在运行
     */
    public boolean tryLock() {
        try {
            // 获取用户临时目录
            String tempDir = System.getProperty("java.io.tmpdir");
            File lockFile = new File(tempDir, LOCK_FILE);

            log.info("尝试获取单实例锁，锁文件路径: {}", lockFile.getAbsolutePath());

            // 创建RandomAccessFile以便可以获取FileChannel
            randomAccessFile = new RandomAccessFile(lockFile, "rw");
            fileChannel = randomAccessFile.getChannel();

            // 尝试获取文件锁（非阻塞）
            fileLock = fileChannel.tryLock();

            if (fileLock == null) {
                log.error("无法获取单实例锁：已有其他实例正在运行");
                close();
                return false;
            }

            log.info("成功获取单实例锁");

            // 添加JVM关闭钩子，在程序退出时释放锁
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("程序退出，释放单实例锁");
                close();
            }));

            return true;

        } catch (IOException e) {
            log.error("获取单实例锁时发生异常", e);
            close();
            return false;
        }
    }

    /**
     * 释放锁并关闭相关资源
     */
    public void close() {
        try {
            if (fileLock != null && fileLock.isValid()) {
                fileLock.release();
                fileLock = null;
            }
        } catch (IOException e) {
            log.error("释放文件锁时发生异常", e);
        }

        try {
            if (fileChannel != null && fileChannel.isOpen()) {
                fileChannel.close();
                fileChannel = null;
            }
        } catch (IOException e) {
            log.error("关闭文件通道时发生异常", e);
        }

        try {
            if (randomAccessFile != null) {
                randomAccessFile.close();
                randomAccessFile = null;
            }
        } catch (IOException e) {
            log.error("关闭RandomAccessFile时发生异常", e);
        }
    }
}
