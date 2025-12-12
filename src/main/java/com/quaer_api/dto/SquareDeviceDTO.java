package com.quaer_api.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Square设备DTO
 */
@Data
public class SquareDeviceDTO {

    /**
     * 设备ID
     */
    private String id;

    /**
     * 设备名称
     */
    private String name;

    /**
     * 位置ID
     */
    private String locationId;

    /**
     * 设备代码（配对代码）
     */
    private String deviceCode;

    /**
     * 设备状态
     */
    private String status;

    /**
     * 产品类型
     */
    private String productType;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 配对代码状态
     */
    private String pairStatus;

    /**
     * 配对码
     */
    private String code;

    /**
     * 配对码ID
     */
    private String deviceCodeId;
}
