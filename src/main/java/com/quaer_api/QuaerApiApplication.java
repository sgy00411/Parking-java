package com.quaer_api;

import com.quaer_api.util.SingleInstanceLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class QuaerApiApplication {

    public static final String VERSION = "251205.01";
    public static final String VERSION_DATE = "2025-12-05";
    public static final String VERSION_DESC = "添加单进程运行限制和版本号管理";

    public static void main(String[] args) {
        // 单进程检查
        SingleInstanceLock singleInstanceLock = new SingleInstanceLock();
        if (!singleInstanceLock.tryLock()) {
            log.error("========================================");
            log.error("ERROR: 应用程序已经在运行中！");
            log.error("无法启动多个实例，请先关闭现有实例");
            log.error("========================================");
            System.exit(1);
        }

        log.info("========================================");
        log.info("    停车场支付系统 - Quaer API");
        log.info("========================================");
        log.info("版本号: {}", VERSION);
        log.info("版本日期: {}", VERSION_DATE);
        log.info("版本说明: {}", VERSION_DESC);
        log.info("========================================");

        SpringApplication.run(QuaerApiApplication.class, args);
    }

}
