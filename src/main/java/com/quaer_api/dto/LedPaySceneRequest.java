package com.quaer_api.dto;

import java.util.List;

/**
 * LED 广告屏支付场景请求
 */
public class LedPaySceneRequest {
    private Integer showTime;          // 页面显示时间(秒)
    private String qrcode;             // 二维码字符串
    private String voice;              // 语音播报内容
    private List<LedTextItem> textList; // 文字列表

    public LedPaySceneRequest() {
    }

    public LedPaySceneRequest(Integer showTime, String qrcode, String voice, List<LedTextItem> textList) {
        this.showTime = showTime;
        this.qrcode = qrcode;
        this.voice = voice;
        this.textList = textList;
    }

    public Integer getShowTime() {
        return showTime;
    }

    public void setShowTime(Integer showTime) {
        this.showTime = showTime;
    }

    public String getQrcode() {
        return qrcode;
    }

    public void setQrcode(String qrcode) {
        this.qrcode = qrcode;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public List<LedTextItem> getTextList() {
        return textList;
    }

    public void setTextList(List<LedTextItem> textList) {
        this.textList = textList;
    }
}
