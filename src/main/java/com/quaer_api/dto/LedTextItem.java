package com.quaer_api.dto;

/**
 * LED 广告屏文字项
 */
public class LedTextItem {
    private LedTextColor color; // 文字颜色
    private String text;        // 显示文本
    private Integer lid;        // 行号

    public LedTextItem() {
    }

    public LedTextItem(Integer lid, String text, LedTextColor color) {
        this.lid = lid;
        this.text = text;
        this.color = color;
    }

    public LedTextColor getColor() {
        return color;
    }

    public void setColor(LedTextColor color) {
        this.color = color;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getLid() {
        return lid;
    }

    public void setLid(Integer lid) {
        this.lid = lid;
    }
}
