package com.quaer_api.dto;

import java.util.List;

/**
 * LED 广告屏过车场景请求
 */
public class LedPassingSceneRequest {
    private Integer showTime;          // 页面显示时间(秒)
    private String voice;              // 语音播报内容
    private List<LedTextItem> textList; // 文字列表

    public LedPassingSceneRequest() {
    }

    public LedPassingSceneRequest(Integer showTime, String voice, List<LedTextItem> textList) {
        this.showTime = showTime;
        this.voice = voice;
        this.textList = textList;
    }

    public Integer getShowTime() {
        return showTime;
    }

    public void setShowTime(Integer showTime) {
        this.showTime = showTime;
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
