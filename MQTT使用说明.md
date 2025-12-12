# MQTT客户端使用说明


## 项目概述

本项目实现了一个完整的MQTT客户端,用于连接停车场MQTT服务器,实现消息的订阅和发布功能。

## 功能特性

- ✅ 从YAML配置文件读取MQTT配置
- ✅ 自动连接MQTT服务器
- ✅ 自动订阅配置的主题
- ✅ 支持发布消息到指定主题
- ✅ 详细的日志记录(连接状态、消息发送、消息接收)
- ✅ 自动重连机制
- ✅ 提供HTTP接口测试MQTT功能

## 项目结构

```
src/main/
├── java/com/quaer_api/
│   ├── config/
│   │   ├── MqttConfig.java          # MQTT配置类
│   │   └── MqttProperties.java      # MQTT配置属性
│   ├── service/
│   │   ├── MqttClientService.java   # MQTT客户端服务
│   │   └── MqttMessageHandler.java  # MQTT消息处理器
│   ├── controller/
│   │   └── MqttTestController.java  # MQTT测试控制器
│   └── QuaerApiApplication.java     # 应用程序入口
└── resources/
    └── application.yml               # 应用配置文件
```

## 配置说明

### application.yml 配置文件

```yaml
mqtt:
  broker-url: tcp://49.234.8.138:1883  # MQTT服务器地址
  client-id: parking_java_client        # 客户端ID
  username: parking_device              # 用户名
  password: Device@2025                 # 密码
  subscribe-topics:                     # 订阅的主题列表
    - parking/camera
  publish-topic: parking/camera         # 默认发布主题
  qos: 1                                # QoS级别 (0,1,2)
  retained: false                       # 是否保留消息
  connection:                           # 连接配置
    clean-session: true                 # 清除会话
    connection-timeout: 30              # 连接超时(秒)
    keep-alive-interval: 60             # 心跳间隔(秒)
    automatic-reconnect: true           # 自动重连
    max-reconnect-delay: 128000         # 最大重连延迟(毫秒)
```

### MQTT服务器信息

- **服务器地址**: 49.234.8.138
- **MQTT端口**: 1883
- **WebSocket端口**: 8083
- **Dashboard地址**: http://49.234.8.138:18083
- **Dashboard账号**: admin / Parking@2025
- **设备账号**: parking_device / Device@2025

## 使用方法

### 1. 启动应用

```bash
# 方式1: 使用Maven运行
mvn spring-boot:run

# 方式2: 编译后运行
mvn clean package
java -jar target/quaer_api-0.0.1-SNAPSHOT.jar
```

### 2. 查看日志

启动后,程序会自动连接MQTT服务器并订阅主题,控制台会输出详细日志:

```
=== 开始初始化MQTT客户端 ===
MQTT配置信息:
  Broker URL: tcp://49.234.8.138:1883
  Client ID: parking_java_client
  Username: parking_device
  订阅主题: [parking/camera]
  发布主题: parking/camera
  QoS级别: 1
--- 正在连接MQTT服务器...
>>> MQTT连接成功! 时间: 2025-11-30 20:00:00
>>> 订阅主题成功: parking/camera QoS: 1 时间: 2025-11-30 20:00:00
=== MQTT客户端初始化完成 ===
```

### 3. 使用HTTP接口测试

#### 3.1 检查连接状态

```bash
# 方式1: curl
curl http://localhost:8080/mqtt/status

# 方式2: 浏览器访问
http://localhost:8080/mqtt/status
```

响应示例:
```json
{
  "connected": true,
  "status": "已连接",
  "timestamp": "2025-11-30 20:00:00"
}
```

#### 3.2 发送测试消息

```bash
curl http://localhost:8080/mqtt/test
```

响应示例:
```json
{
  "success": true,
  "message": "测试消息发送成功",
  "content": "测试消息 - 时间: 2025-11-30 20:00:00",
  "timestamp": "2025-11-30 20:00:00"
}
```

#### 3.3 发布自定义消息到默认主题

```bash
curl -X POST "http://localhost:8080/mqtt/publish?message=你的消息内容"
```

响应示例:
```json
{
  "success": true,
  "message": "消息发送成功",
  "content": "你的消息内容",
  "timestamp": "2025-11-30 20:00:00"
}
```

#### 3.4 发布消息到指定主题

```bash
curl -X POST "http://localhost:8080/mqtt/publish/parking/camera?message=摄像头数据"
```

响应示例:
```json
{
  "success": true,
  "message": "消息发送成功",
  "topic": "parking/camera",
  "content": "摄像头数据",
  "timestamp": "2025-11-30 20:00:00"
}
```

### 4. 接收消息

当有消息发布到订阅的主题时,程序会自动接收并在日志中显示:

```
<<< 收到MQTT消息
  主题: parking/camera
  内容: {"camera_id": "001", "event": "car_detected"}
  QoS: 1
  时间: 2025-11-30 20:00:00
========================================
处理MQTT消息
  接收时间: 2025-11-30 20:00:00
  消息主题: parking/camera
  消息内容: {"camera_id": "001", "event": "car_detected"}
  消息ID: 12345
  QoS级别: 1
  是否重复: false
  是否保留: false
========================================
>>> 处理停车场摄像头消息
  消息内容: {"camera_id": "001", "event": "car_detected"}
>>> 摄像头消息处理完成
```

## 代码使用示例

### 在代码中发布消息

```java
@Autowired
private MqttClientService mqttClientService;

// 发布到默认主题
mqttClientService.publishToDefaultTopic("消息内容");

// 发布到指定主题
mqttClientService.publish("parking/camera", "消息内容");
```

### 在代码中检查连接状态

```java
@Autowired
private MqttClientService mqttClientService;

boolean connected = mqttClientService.isConnected();
if (connected) {
    System.out.println("MQTT已连接");
} else {
    System.out.println("MQTT未连接");
}
```

### 自定义消息处理逻辑

修改 `MqttMessageHandler.java` 中的 `handleCameraMessage` 方法:

```java
private void handleCameraMessage(String payload) {
    log.info(">>> 处理停车场摄像头消息");
    log.info("  消息内容: {}", payload);

    // 添加你的业务逻辑
    // 例如: 解析JSON
    // JSONObject json = JSON.parseObject(payload);
    // String cameraId = json.getString("camera_id");

    // 保存到数据库
    // cameraDataRepository.save(...);

    // 触发其他操作
    // notificationService.sendAlert(...);

    log.info(">>> 摄像头消息处理完成");
}
```

## 日志说明

程序使用详细的日志记录所有关键操作:

- **连接日志**: 记录连接成功、失败、重连等状态
- **订阅日志**: 记录主题订阅情况
- **发送日志**: 记录消息发送的详细信息
- **接收日志**: 记录接收到的消息详情
- **错误日志**: 记录所有异常和错误信息

## 常见问题

### Q1: 连接失败怎么办?

检查以下几点:
1. 网络是否通畅: `ping 49.234.8.138`
2. 端口是否开放: `telnet 49.234.8.138 1883`
3. 用户名密码是否正确
4. 防火墙是否阻止连接

### Q2: 如何修改订阅的主题?

修改 `application.yml` 中的 `subscribe-topics` 配置:

```yaml
mqtt:
  subscribe-topics:
    - parking/camera
    - parking/gate
    - parking/payment
```

### Q3: 如何调整日志级别?

在 `application.yml` 中添加:

```yaml
logging:
  level:
    com.quaer_api: DEBUG
    org.eclipse.paho: INFO
```

### Q4: 程序重启后会丢失消息吗?

根据配置:
- `clean-session: true`: 会话不保留,重启后不会接收离线消息
- `clean-session: false`: 会话保留,重启后会接收离线消息

## 依赖说明

项目使用的主要依赖:

- **Spring Boot 2.6.13**: Web框架
- **Eclipse Paho 1.2.5**: MQTT客户端库
- **Lombok**: 简化Java代码
- **Java 8**: 运行环境

## 开发建议

1. 根据实际业务需求修改消息处理逻辑
2. 添加数据持久化功能
3. 实现更复杂的消息路由
4. 添加消息队列缓冲机制
5. 实现消息加密和验证

## 技术支持

如有问题,请检查日志文件或联系开发团队。
