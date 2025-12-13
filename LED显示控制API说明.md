# LED 广告屏 MQTT 控制接口说明

## 概述

实现了基于 MQTT 协议的 LED 广告屏控制功能，遵循《广告屏通信协议 v2.0》规范。

## MQTT 配置

- **设备 CID**: `96:6E:6D:27:DC:9D`
- **车场编码**: `test`
- **私有主题**: `MC/test/private/96:6E:6D:27:DC:9D`
- **广播主题**: `MC/test/public/all`
- **接收主题**: `MC/receiver/parking_server`

## API 接口列表

### 1. 测试接口 - 发送"欢迎光临"

**最简单的测试接口**

```bash
GET http://localhost:8086/api/led/test/welcome
```

**响应示例**:
```json
{
  "status": "success",
  "message": "测试消息已发送",
  "text": "欢迎光临"
}
```

**MQTT 消息**:
```json
{
  "type": "template",
  "cmd": "start_passing_scene",
  "sender": "parking_server",
  "request_time": 1702468800000,
  "sn": 12345,
  "config": {
    "show_time": 10,
    "voice": "欢迎光临",
    "text_list": [
      {
        "lid": 0,
        "text": "欢迎光临",
        "color": {
          "a": 255,
          "r": 255,
          "g": 255,
          "b": 255
        }
      }
    ]
  }
}
```

---

### 2. 发送自定义文字

```bash
POST http://localhost:8086/api/led/text
Content-Type: application/json

{
  "text": "欢迎光临"
}
```

**响应示例**:
```json
{
  "status": "success",
  "message": "文字已发送到 LED 屏幕",
  "text": "欢迎光临"
}
```

---

### 3. 发送车辆欢迎信息

```bash
POST http://localhost:8086/api/led/vehicle-welcome
Content-Type: application/json

{
  "licensePlate": "粤B12345",
  "vehicleType": "月租车"
}
```

**显示效果**:
- 第1行（红色）: 月租车
- 第2行（绿色）: 粤B12345
- 第3行（黄色）: 欢迎光临
- 第4行（白色）: 请入场停车

---

### 4. 搜索 LED 设备

```bash
POST http://localhost:8086/api/led/search
```

---

### 5. 设置余位

```bash
POST http://localhost:8086/api/led/park-number
Content-Type: application/json

{
  "number": 100,
  "enabled": true
}
```

---

### 6. 配置文字广告

```bash
POST http://localhost:8086/api/led/advert/text
Content-Type: application/json

[
  {
    "lid": 0,
    "text": "天安门停车场",
    "color": {
      "a": 255,
      "r": 255,
      "g": 0,
      "b": 0
    }
  },
  {
    "lid": 1,
    "text": "欢迎光临",
    "color": {
      "a": 255,
      "r": 0,
      "g": 255,
      "b": 0
    }
  }
]
```

---

### 7. 显示过车界面

```bash
POST http://localhost:8086/api/led/scene/passing
Content-Type: application/json

{
  "showTime": 10,
  "voice": "欢迎光临",
  "textList": [
    {
      "lid": 0,
      "text": "月租车",
      "color": {
        "a": 255,
        "r": 255,
        "g": 0,
        "b": 0
      }
    },
    {
      "lid": 1,
      "text": "粤B12345",
      "color": {
        "a": 255,
        "r": 0,
        "g": 255,
        "b": 0
      }
    }
  ]
}
```

---

### 8. 显示支付界面

```bash
POST http://localhost:8086/api/led/scene/pay
Content-Type: application/json

{
  "showTime": 10,
  "qrcode": "http://pay.example.com/qr/12345",
  "voice": "粤B12345,停车1小时5分钟,请缴费10元",
  "textList": [
    {
      "lid": 0,
      "text": "粤B12345",
      "color": {
        "a": 255,
        "r": 255,
        "g": 255,
        "b": 255
      }
    },
    {
      "lid": 1,
      "text": "停车 1 小时 5 分钟",
      "color": {
        "a": 255,
        "r": 0,
        "g": 255,
        "b": 0
      }
    },
    {
      "lid": 2,
      "text": "请缴费 10 元",
      "color": {
        "a": 255,
        "r": 255,
        "g": 255,
        "b": 0
      }
    },
    {
      "lid": 3,
      "text": "请扫码支付",
      "color": {
        "a": 255,
        "r": 255,
        "g": 0,
        "b": 0
      }
    }
  ]
}
```

---

### 9. 显示无牌车扫码界面

```bash
POST http://localhost:8086/api/led/scene/unlicensed
Content-Type: application/json

{
  "qrcode": "http://register.example.com/qr",
  "voice": "无牌车,请扫码",
  "textList": [
    {
      "lid": 0,
      "text": "欢迎光临",
      "color": {
        "a": 255,
        "r": 240,
        "g": 240,
        "b": 240
      }
    },
    {
      "lid": 1,
      "text": "无牌车请扫码",
      "color": {
        "a": 255,
        "r": 0,
        "g": 0,
        "b": 255
      }
    }
  ]
}
```

---

### 10. 切换回广告界面

```bash
POST http://localhost:8086/api/led/scene/advert
```

---

## 颜色配置

ARGB 格式，每个分量取值 0-255：

| 颜色 | A | R | G | B |
|------|---|---|---|---|
| 白色 | 255 | 255 | 255 | 255 |
| 红色 | 255 | 255 | 0 | 0 |
| 绿色 | 255 | 0 | 255 | 0 |
| 蓝色 | 255 | 0 | 0 | 255 |
| 黄色 | 255 | 255 | 255 | 0 |

---

## 快速测试

### 使用 curl 命令

```bash
# 1. 发送"欢迎光临"（最简单）
curl http://localhost:8086/api/led/test/welcome

# 2. 发送自定义文字
curl -X POST http://localhost:8086/api/led/text \
  -H "Content-Type: application/json" \
  -d '{"text":"停车场欢迎您"}'

# 3. 发送车辆欢迎
curl -X POST http://localhost:8086/api/led/vehicle-welcome \
  -H "Content-Type: application/json" \
  -d '{"licensePlate":"粤B12345","vehicleType":"月租车"}'

# 4. 设置余位
curl -X POST http://localhost:8086/api/led/park-number \
  -H "Content-Type: application/json" \
  -d '{"number":100,"enabled":true}'
```

### 使用 Postman 或 API 测试工具

1. 启动后端服务: `mvn spring-boot:run`
2. 访问: `http://localhost:8086/api/led/test/welcome`
3. 查看 MQTT 消息已发送到主题: `MC/test/private/96:6E:6D:27:DC:9D`

---

## 已实现的类文件

### DTO 类
- `LedTextColor.java` - 文字颜色配置
- `LedTextItem.java` - 文字项
- `LedPassingSceneRequest.java` - 过车场景请求
- `LedPaySceneRequest.java` - 支付场景请求

### 服务类
- `LedDisplayService.java` - LED 显示服务，封装所有 MQTT 消息发送逻辑

### 控制器
- `LedDisplayController.java` - LED 显示控制器，提供 REST API

---

## MQTT 消息主题说明

### 发送到 LED 设备
- **私有主题**: `MC/test/private/96:6E:6D:27:DC:9D`
  - 用于发送独立的业务数据到特定设备

- **广播主题**: `MC/test/public/all`
  - 用于发送公共配置和搜索命令到所有设备

### LED 设备回复
- **接收主题**: `MC/receiver/parking_server`
  - LED 设备会回复消息到这个主题
  - 通过 `sender` 字段判断是哪个设备的回复

---

## 注意事项

1. **MQTT 连接**: 确保 MQTT 服务器已启动并可连接
2. **设备 CID**: 当前硬编码为 `96:6E:6D:27:DC:9D`，可根据实际设备修改
3. **车场编码**: 当前为 `test`，可根据实际情况修改
4. **消息格式**: 严格遵循广告屏通信协议 v2.0 JSON 格式
5. **字符编码**: 所有文本使用 UTF-8 编码

---

## 调试建议

1. 使用 MQTT 客户端工具（如 MQTT.fx、MQTTX）监听主题
2. 查看后端日志输出的 JSON 消息
3. 确认 LED 设备已连接并订阅了对应主题
4. 检查防火墙是否阻止 MQTT 端口（1883）
