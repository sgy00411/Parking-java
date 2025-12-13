package com.quaer_api.dto;

import java.util.List;

/**
 * LED 广告屏文字颜色配置
 */
public class LedTextColor {
    private Integer a; // 透明度 0-255
    private Integer r; // 红色分量 0-255
    private Integer g; // 绿色分量 0-255
    private Integer b; // 蓝色分量 0-255

    public LedTextColor() {
    }

    public LedTextColor(Integer a, Integer r, Integer g, Integer b) {
        this.a = a;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    // 预设颜色
    public static LedTextColor white() {
        return new LedTextColor(255, 255, 255, 255);
    }

    public static LedTextColor red() {
        return new LedTextColor(255, 255, 0, 0);
    }

    public static LedTextColor green() {
        return new LedTextColor(255, 0, 255, 0);
    }

    public static LedTextColor blue() {
        return new LedTextColor(255, 0, 0, 255);
    }

    public static LedTextColor yellow() {
        return new LedTextColor(255, 255, 255, 0);
    }

    public Integer getA() {
        return a;
    }

    public void setA(Integer a) {
        this.a = a;
    }

    public Integer getR() {
        return r;
    }

    public void setR(Integer r) {
        this.r = r;
    }

    public Integer getG() {
        return g;
    }

    public void setG(Integer g) {
        this.g = g;
    }

    public Integer getB() {
        return b;
    }

    public void setB(Integer b) {
        this.b = b;
    }
}
