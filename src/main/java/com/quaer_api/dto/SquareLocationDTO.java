package com.quaer_api.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Square位置DTO
 */
@Data
public class SquareLocationDTO {

    /**
     * 位置ID
     */
    private String id;

    /**
     * 位置名称
     */
    private String name;

    /**
     * 商户名称
     */
    private String businessName;

    /**
     * 商户ID
     */
    private String merchantId;

    /**
     * 状态
     */
    private String status;

    /**
     * 货币
     */
    private String currency;

    /**
     * 时区
     */
    private String timezone;

    /**
     * MCC代码
     */
    private String mcc;

    /**
     * 地址
     */
    private Address address;

    /**
     * 坐标
     */
    private Coordinates coordinates;

    /**
     * 功能列表
     */
    private List<String> capabilities;

    /**
     * 是否为当前位置
     */
    private Boolean isCurrent;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    @Data
    public static class Address {
        private String addressLine1;
        private String addressLine2;
        private String locality;
        private String administrativeDistrictLevel1;
        private String postalCode;
        private String country;
    }

    @Data
    public static class Coordinates {
        private Double latitude;
        private Double longitude;
    }
}
