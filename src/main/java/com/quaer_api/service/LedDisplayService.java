package com.quaer_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quaer_api.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * LED 广告屏显示服务
 * 实现广告屏通信协议 MQTT v2.0
 */
@Slf4j
@Service
public class LedDisplayService {

    @Autowired
    private MqttClientService mqttClientService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 车场编码
    private static final String PARK_CODE = "test";

    // LED 设备 CID
    private static final String LED_DEVICE_CID = "96:6E:6D:27:DC:9D";

    // 发送者 ID (服务端)
    private static final String SENDER_ID = "parking_server";

    /**
     * 获取私有主题
     * 格式: MC/{车场编码}/private/{设备CID}
     */
    private String getPrivateTopic() {
        return "MC/" + PARK_CODE + "/private/" + LED_DEVICE_CID;
    }

    /**
     * 获取指定LED设备的私有主题
     * 格式: MC/{车场编码}/private/{设备CID}
     */
    private String getPrivateTopicForDevice(String deviceCid) {
        return "MC/" + PARK_CODE + "/private/" + deviceCid;
    }

    /**
     * 获取广播主题
     * 格式: MC/{车场编码}/public/all
     */
    private String getPublicTopic() {
        return "MC/" + PARK_CODE + "/public/all";
    }

    /**
     * 发送 MQTT 消息
     */
    private void sendMqttMessage(String topic, Map<String, Object> message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            mqttClientService.publish(topic, jsonMessage);
            log.info("发送 LED 消息到主题 [{}]: {}", topic, jsonMessage);
        } catch (Exception e) {
            log.error("发送 LED MQTT 消息失败", e);
            throw new RuntimeException("发送 LED 消息失败: " + e.getMessage());
        }
    }

    /**
     * 创建基础消息结构
     */
    private Map<String, Object> createBaseMessage(String type, String cmd) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("cmd", cmd);
        message.put("sender", SENDER_ID);
        message.put("request_time", System.currentTimeMillis());
        message.put("sn", generateSn());
        message.put("device_cid", LED_DEVICE_CID);  // LED设备编号
        return message;
    }

    /**
     * 生成消息序列号
     */
    private int generateSn() {
        return (int) (System.currentTimeMillis() % 100000);
    }

    /**
     * 搜索设备
     */
    public void searchDevice() {
        Map<String, Object> message = createBaseMessage("broadcast", "search");
        sendMqttMessage(getPublicTopic(), message);
    }

    /**
     * 配置文字广告
     */
    public void setTextAdvert(List<LedTextItem> textList) {
        Map<String, Object> message = createBaseMessage("template", "set_advert_config");

        Map<String, Object> config = new HashMap<>();
        config.put("text_list", textList);

        message.put("config", config);
        message.put("save", true);

        sendMqttMessage(getPrivateTopic(), message);
    }

    /**
     * 设置余位
     */
    public void setParkNumber(Integer parkNumber, Boolean enabled) {
        Map<String, Object> message = createBaseMessage("template", "set_advert_park_num");

        message.put("park_number", parkNumber);
        message.put("park_number_en", enabled);

        sendMqttMessage(getPrivateTopic(), message);
    }

    /**
     * 显示过车界面
     */
    public void showPassingScene(LedPassingSceneRequest request) {
        Map<String, Object> message = createBaseMessage("template", "start_passing_scene");

        Map<String, Object> config = new HashMap<>();
        config.put("show_time", request.getShowTime());
        config.put("voice", request.getVoice());
        config.put("text_list", request.getTextList());

        message.put("config", config);

        sendMqttMessage(getPrivateTopic(), message);
    }

    /**
     * 显示支付界面
     */
    public void showPayScene(LedPaySceneRequest request) {
        Map<String, Object> message = createBaseMessage("template", "start_pay_scene");

        Map<String, Object> config = new HashMap<>();
        config.put("show_time", request.getShowTime());
        config.put("qrcode", request.getQrcode());
        config.put("voice", request.getVoice());
        config.put("text_list", request.getTextList());

        message.put("config", config);

        sendMqttMessage(getPrivateTopic(), message);
    }

    /**
     * 显示支付界面到指定LED设备
     * @param ledDeviceCid LED设备编号
     * @param request 支付场景请求
     */
    public void showPaySceneToDevice(String ledDeviceCid, LedPaySceneRequest request) {
        Map<String, Object> message = createBaseMessage("template", "start_pay_scene");
        message.put("device_cid", ledDeviceCid);  // 使用指定的LED设备编号

        Map<String, Object> config = new HashMap<>();
        config.put("show_time", request.getShowTime());
        config.put("qrcode", request.getQrcode());
        config.put("voice", request.getVoice());
        config.put("text_list", request.getTextList());

        message.put("config", config);

        // 发送到指定LED设备的主题
        String topic = getPrivateTopicForDevice(ledDeviceCid);
        sendMqttMessage(topic, message);
    }

    /**
     * 显示无牌车扫码界面
     */
    public void showUnlicensedScene(String qrcode, String voice, List<LedTextItem> textList) {
        Map<String, Object> message = createBaseMessage("template", "start_ulp_scene");

        Map<String, Object> config = new HashMap<>();
        config.put("show_time", 0); // 0表示持续显示
        config.put("qrcode", qrcode);
        config.put("voice", voice);
        config.put("text_list", textList);

        message.put("config", config);

        sendMqttMessage(getPrivateTopic(), message);
    }

    /**
     * 显示广告界面（切换回空闲广告页面）
     */
    public void showAdvertScene() {
        Map<String, Object> message = createBaseMessage("template", "start_advert_scene");
        sendMqttMessage(getPrivateTopic(), message);
    }

    /**
     * 发送简单的欢迎文字
     */
    public void sendWelcomeText(String text) {
        List<LedTextItem> textList = new ArrayList<>();

        // 第一行：欢迎文字（白色）
        LedTextItem item = new LedTextItem();
        item.setLid(0);
        item.setText(text);
        item.setColor(LedTextColor.white());
        textList.add(item);

        LedPassingSceneRequest request = new LedPassingSceneRequest();
        request.setShowTime(10);  // 显示10秒
        request.setVoice(text);   // 语音播报
        request.setTextList(textList);

        showPassingScene(request);
    }

    /**
     * 发送过车欢迎信息
     */
    public void sendVehicleWelcome(String licensePlate, String vehicleType) {
        List<LedTextItem> textList = new ArrayList<>();

        // 第一行：车辆类型（红色）
        LedTextItem line1 = new LedTextItem();
        line1.setLid(0);
        line1.setText(vehicleType);
        line1.setColor(LedTextColor.red());
        textList.add(line1);

        // 第二行：车牌号（绿色）
        LedTextItem line2 = new LedTextItem();
        line2.setLid(1);
        line2.setText(licensePlate);
        line2.setColor(LedTextColor.green());
        textList.add(line2);

        // 第三行：欢迎光临（黄色）
        LedTextItem line3 = new LedTextItem();
        line3.setLid(2);
        line3.setText("欢迎光临");
        line3.setColor(LedTextColor.yellow());
        textList.add(line3);

        // 第四行：请入场停车（白色）
        LedTextItem line4 = new LedTextItem();
        line4.setLid(3);
        line4.setText("请入场停车");
        line4.setColor(LedTextColor.white());
        textList.add(line4);

        LedPassingSceneRequest request = new LedPassingSceneRequest();
        request.setShowTime(60);
        request.setVoice("欢迎光临");
        request.setTextList(textList);

        showPassingScene(request);
    }

    /**
     * 发送车辆付款信息到指定LED设备
     * @param ledDeviceCid LED设备编号
     * @param licensePlate 车牌号
     * @param durationSeconds 停车时长（秒）
     * @param parkingFeeCents 停车费用（美分，直接显示）
     */
    public void sendVehicleWelcomeToLed(String ledDeviceCid, String licensePlate, Integer durationSeconds, Integer parkingFeeCents) {
        List<LedTextItem> textList = new ArrayList<>();

        // 第一行：车牌号（绿色）
        LedTextItem line1 = new LedTextItem();
        line1.setLid(0);
        line1.setText(licensePlate);
        line1.setColor(LedTextColor.green());
        textList.add(line1);

        // 第二行：固定文本 "Time Parked:" （黄色）
        LedTextItem line2 = new LedTextItem();
        line2.setLid(1);
        line2.setText("Time Parked:");
        line2.setColor(LedTextColor.yellow());
        textList.add(line2);

        // 第三行：停车时长 "XX hrs XX mins" （白色）
        LedTextItem line3 = new LedTextItem();
        line3.setLid(2);
        if (durationSeconds != null && durationSeconds > 0) {
            int hours = durationSeconds / 3600;
            int minutes = (durationSeconds % 3600) / 60;
            line3.setText(String.format("%d hrs %d mins", hours, minutes));
        } else {
            line3.setText("0 hrs 0 mins");
        }
        line3.setColor(LedTextColor.white());
        textList.add(line3);

        // 第四行：固定文本 "Please Pay:" （黄色）
        LedTextItem line4 = new LedTextItem();
        line4.setLid(3);
        line4.setText("Please Pay:");
        line4.setColor(LedTextColor.yellow());
        textList.add(line4);

        // 第五行：费用 "$XX.XX" （红色）
        LedTextItem line5 = new LedTextItem();
        line5.setLid(4);
        if (parkingFeeCents != null && parkingFeeCents > 0) {
            // 直接显示原始数值（已经是美分）
            line5.setText(String.format("$%d", parkingFeeCents));
        } else {
            line5.setText("$0");
        }
        line5.setColor(LedTextColor.red());
        textList.add(line5);

        // 创建消息并发送到指定LED设备
        Map<String, Object> message = createBaseMessage("template", "start_passing_scene");
        message.put("device_cid", ledDeviceCid);  // 使用指定的LED设备编号

        Map<String, Object> config = new HashMap<>();
        config.put("show_time", 60);
        config.put("voice", "Please Pay");
        config.put("text_list", textList);

        message.put("config", config);

        // 发送到指定LED设备的主题
        String topic = getPrivateTopicForDevice(ledDeviceCid);
        sendMqttMessage(topic, message);
    }

    /**
     * 发送车辆入场欢迎信息到指定LED设备
     * @param ledDeviceCid LED设备编号
     * @param licensePlate 车牌号
     * @param vehicleType 车辆类型（中文）
     */
    public void sendVehicleEntryToLed(String ledDeviceCid, String licensePlate, String vehicleType) {
        List<LedTextItem> textList = new ArrayList<>();

        // 第一行：固定文本 "License Plate#" （白色）
        LedTextItem line1 = new LedTextItem();
        line1.setLid(0);
        line1.setText("License Plate#");
        line1.setColor(LedTextColor.white());
        textList.add(line1);

        // 第二行：车牌号（绿色）
        LedTextItem line2 = new LedTextItem();
        line2.setLid(1);
        line2.setText(licensePlate);
        line2.setColor(LedTextColor.green());
        textList.add(line2);

        // 第三行：车辆类型（映射为英文）（黄色）
        LedTextItem line3 = new LedTextItem();
        line3.setLid(2);
        line3.setText(mapVehicleTypeToEnglish(vehicleType));
        line3.setColor(LedTextColor.yellow());
        textList.add(line3);

        // 第四行：固定文本 "Vehicle Released" （绿色）
        LedTextItem line4 = new LedTextItem();
        line4.setLid(3);
        line4.setText("Vehicle Released");
        line4.setColor(LedTextColor.green());
        textList.add(line4);

        // 创建消息并发送到指定LED设备
        Map<String, Object> message = createBaseMessage("template", "start_passing_scene");
        message.put("device_cid", ledDeviceCid);  // 使用指定的LED设备编号

        Map<String, Object> config = new HashMap<>();
        config.put("show_time", 60);
        config.put("voice", "Welcome");
        config.put("text_list", textList);

        message.put("config", config);

        // 发送到指定LED设备的主题
        String topic = getPrivateTopicForDevice(ledDeviceCid);
        sendMqttMessage(topic, message);
    }

    /**
     * 将车辆类型从中文映射为英文
     * @param vehicleType 车辆类型（中文）
     * @return 英文车辆类型
     */
    private String mapVehicleTypeToEnglish(String vehicleType) {
        if (vehicleType == null || vehicleType.trim().isEmpty()) {
            return "Pay Parking";  // 默认值
        }

        String type = vehicleType.trim();

        // 临时车
        if (type.equals("临时车")) {
            return "Pay Parking";
        }

        // 月卡/年卡
        if (type.contains("月卡") || type.contains("年卡")) {
            return "Valid Passholder";
        }

        // 管理员车辆
        if (type.contains("管理") || type.contains("管理员")) {
            return "Management Pass";
        }

        // 默认返回临时车
        return "Pay Parking";
    }
}
