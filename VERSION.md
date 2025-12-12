# 停车场支付系统 - 版本记录

---

## 版本 251201.02 (2025-12-01)

### 📋 版本概述
实现MQTT入场消息接收和数据库自动写入功能，完整实现车辆入场记录管理。

### ✨ 新增功能

#### 1. 车辆记录数据库集成
- ✅ 创建 `VehicleRecord` 实体类，映射 `vehicle_records` 数据库表
- ✅ 支持入场和出场完整字段记录
- ✅ 自动管理 `created_at` 和 `updated_at` 时间戳

**实现文件**:
- `entity/VehicleRecord.java` - 车辆记录实体类

#### 2. 车辆入场记录处理服务
- ✅ 实现与Python脚本完全一致的入场逻辑
- ✅ **场景A（新车入场）**: 车牌无未出场记录时，插入新记录
- ✅ **场景B（重复入场）**: 车牌已有未出场记录时，更新入场信息
- ✅ 车牌标准化比较（去除连字符，`ABE-6234` = `ABE6234`）
- ✅ 详细的日志记录，包含车牌号、权重、摄像头信息等

**实现文件**:
- `repository/VehicleRecordRepository.java` - 数据访问层
- `service/VehicleRecordService.java` - 车辆记录业务服务
- `dto/MqttEntryMessage.java` - MQTT入场消息DTO

#### 3. MQTT消息处理增强
- ✅ 更新消息主题匹配逻辑，支持 `parking/0001/camera` 格式
- ✅ 自动解析JSON格式的入场消息
- ✅ 根据 `event_type` 和 `action` 分发消息处理
- ✅ 支持 `entry_new` (新车入场) 和 `entry_update` (重复入场) 两种动作

**实现文件**:
- `service/MqttMessageHandler.java` - MQTT消息处理器（已更新）

#### 4. 日志系统配置
- ✅ 创建完整的 Logback 日志配置
- ✅ 日志文件自动滚动（按天，最大100MB）
- ✅ 分离的日志文件：主日志、MQTT专用日志、错误日志
- ✅ 保留30天历史日志

**日志文件位置**:
- 主日志: `D:/停车场/quare_api/logs/quaer_api.log`
- MQTT日志: `D:/停车场/quare_api/logs/mqtt.log`
- 错误日志: `D:/停车场/quare_api/logs/error.log`

**配置文件**:
- `logback-spring.xml` - Logback配置文件
- `application.yml` - 添加日志路径配置

#### 5. 测试工具和接口
- ✅ 创建 MQTT 消息测试工具，可生成模拟入场消息
- ✅ 提供测试 REST 接口，方便手动测试
- ✅ 支持测试新车入场和重复入场两种场景

**实现文件**:
- `util/MqttMessageTestUtil.java` - MQTT消息测试工具
- `controller/TestController.java` - 测试控制器

### 🔄 业务逻辑说明

#### 入场记录处理流程
1. Python摄像头脚本检测到车辆入场
2. 发送MQTT消息到 `parking/0001/camera` 主题
3. Java应用订阅 `parking/#` 接收消息
4. 解析JSON消息，提取车辆信息
5. 查询数据库是否有该车牌的未出场记录
6. **如果没有** → 插入新记录 (status='entered')
7. **如果有** → 更新入场信息，保持 status='entered'

#### 数据库字段说明
```sql
-- 状态字段
status: 'entered' (已入场未出场) | 'exited' (已出场) | 'exit_only' (异常-仅出场)

-- 入场字段
entry_plate_number    -- 入场车牌号
entry_time           -- 入场时间
entry_camera_ip      -- 入场摄像头IP
entry_camera_id      -- 入场摄像头ID
entry_camera_name    -- 入场摄像头名称
entry_event_id       -- 入场事件ID
entry_detection_count -- 识别次数
entry_weight         -- 识别权重
entry_snapshot       -- 入场截图文件名

-- 出场字段 (当前版本暂不使用)
exit_* ...

-- 时间戳
created_at, updated_at
```

### 🎯 测试方法

#### 方式1：使用测试接口
```bash
# 测试新车入场
curl "http://localhost:8080/api/test/entry/new?plate=ABC-1234"

# 测试重复入场
curl "http://localhost:8080/api/test/entry/update?plate=ABC-1234&recordId=1"

# 健康检查
curl "http://localhost:8080/api/test/health"
```

#### 方式2：等待真实MQTT消息
- Python摄像头脚本自动检测车辆并发送消息
- Java应用自动接收并处理
- 查看日志文件确认处理结果

### 📝 MQTT消息格式

#### 场景A：新车入场 (entry_new)
```json
{
  "message_id": "uuid",
  "record_id": null,
  "event_type": "entry",
  "action": "entry_new",
  "status": "entered",
  "entry_plate_number": "ABC-1234",
  "entry_time": "2025-12-01 11:30:45",
  "entry_camera_ip": "192.168.1.100",
  "entry_camera_id": 1,
  "entry_camera_name": "入口摄像头",
  "entry_event_id": 1001,
  "entry_detection_count": 5,
  "entry_weight": 25.5,
  "entry_snapshot": "snapshot.jpg",
  "timestamp": "2025-12-01 11:30:45"
}
```

#### 场景B：重复入场 (entry_update)
```json
{
  "message_id": "uuid",
  "record_id": 123,
  "event_type": "entry",
  "action": "entry_update",
  "status": "entered",
  "entry_plate_number": "ABC-1234",
  ...
}
```

### 🔧 配置变更

**application.yml**:
```yaml
# 日志配置 (新增)
logging:
  file:
    path: D:/停车场/quare_api/logs
    name: D:/停车场/quare_api/logs/quaer_api.log
  level:
    root: INFO
    com.quaer_api: INFO
    com.quaer_api.service.MqttClientService: INFO
    com.quaer_api.service.MqttMessageHandler: INFO
```

### 📊 数据流程图
```
Python摄像头脚本
    ↓ (检测车辆入场)
MQTT Broker (49.234.8.138:1883)
    ↓ (主题: parking/0001/camera)
Java MQTT客户端 (订阅: parking/#)
    ↓ (接收消息)
MqttMessageHandler (解析JSON)
    ↓ (识别event_type=entry)
VehicleRecordService (处理入场逻辑)
    ↓ (查询+插入/更新)
MySQL数据库 (vehicle_records表)
```

### 📦 新增依赖
```xml
<!-- JPA数据访问 (已有) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- MySQL驱动 (已有) -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>

<!-- Jackson JSON处理 (已有) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

### ✅ 已验证功能
- ✅ MQTT消息主题匹配 (`parking/+/camera`)
- ✅ JSON消息解析
- ✅ 数据库实体映射
- ✅ 新车入场记录插入
- ✅ 重复入场记录更新
- ✅ 日志文件生成和滚动
- ✅ 测试接口正常工作

### 🐛 修复问题
- ✅ 修复MQTT主题匹配问题（从 `parking/camera` 改为 `contains("/camera")`）
- ✅ 正确处理 `parking/0001/camera` 等格式的主题

### 📌 注意事项
1. 数据库表 `vehicle_records` 必须已存在（由Python脚本创建）
2. MySQL连接配置：`localhost:3306/parking`
3. 重启应用后，日志文件会在 `D:/停车场/quare_api/logs/` 目录下生成
4. 测试前请确保MQTT服务器 `49.234.8.138:1883` 可访问
5. 当前版本仅实现入场功能，出场功能待实现

### 🚀 下一步计划
- [ ] 实现出场消息处理（exit_normal, exit_only_new, exit_only_update）
- [ ] 添加停留时长计算
- [ ] 车辆记录查询接口
- [ ] 异常记录处理和告警

---

## 版本 251201.01 (2025-12-01)

### 📋 版本概述
修复MQTT订阅主题配置问题，添加版本号显示功能。

### 🐛 问题修复
- ✅ 修复MQTT订阅主题配置，使用通配符 `parking/#` 订阅所有parking相关消息
- ✅ 之前只订阅了 `parking/camera`，现在可以订阅所有parking开头的主题

### ✨ 新增功能
- ✅ 在应用启动日志中显示版本号信息
- ✅ 版本号定义为常量，便于追踪和管理
- ✅ 添加版本描述，方便了解每个版本的主要变更

**实现文件**:
- `QuaerApiApplication.java` - 添加版本号常量和启动日志

### 📝 配置变更
**application.yml (第37-38行)**:
```yaml
subscribe-topics:
  - parking/#  # 订阅所有parking开头的主题
```

### 🔧 技术改进
- 使用 `@Slf4j` 注解添加日志功能
- 版本号格式: YYMMDD.NN

---

## 版本 251130.01 (2025-11-30)

### 📋 版本概述
首个正式版本，实现MQTT通信和Square支付集成的核心功能。

### ✨ 新增功能

#### 1. MQTT客户端功能
- ✅ 实现MQTT客户端连接到EMQX服务器
- ✅ 订阅 `parking/camera` 主题，接收设备消息
- ✅ 发布消息到 `parking/camera` 主题
- ✅ 自动重连机制
- ✅ 详细的连接和消息日志记录

**配置信息**:
- MQTT服务器: tcp://49.234.8.138:1883
- 用户认证: parking_device / Device@2025
- QoS级别: 1

**实现文件**:
- `MqttProperties.java` - MQTT配置属性
- `MqttClientService.java` - MQTT客户端服务
- `MqttMessageHandler.java` - MQTT消息处理器
- `MqttConfig.java` - MQTT配置类
- `MqttTestController.java` - MQTT测试接口

#### 2. Square支付Webhook功能
- ✅ 接收Square支付平台的Webhook回调通知
- ✅ 双重签名验证机制（动态URL + 配置URL）
- ✅ 处理 `payment.created` 和 `payment.updated` 事件
- ✅ 详细的支付信息日志记录（不连接数据库）

**配置信息**:
- Webhook URL: https://car.test001.cn:8083/api/payments/webhook
- 签名密钥: y834xn53DXGniXAGr7NtCA

**实现文件**:
- `SquareWebhookController.java` - Webhook接收控制器
- `SquareSignatureValidator.java` - 签名验证工具

#### 3. Square终端支付功能
- ✅ 发起POS终端支付请求
- ✅ 从配置文件读取Square API参数
- ✅ 自动生成幂等性密钥
- ✅ 详细的请求和响应日志

**配置信息**:
- API版本: 2025-10-16
- 设备ID: 533CS145C3000603
- 默认货币: CAD
- 访问令牌: EAAAl9Se8DP-KKycVFi0HiFU_9-bHh0F9m9c3XvGaH6S7Oeuvp-VfiQWALTE0FYY

**实现文件**:
- `SquareProperties.java` - Square配置属性
- `SquareTerminalService.java` - 终端支付服务
- `SquareTerminalController.java` - 终端支付接口

### 🎯 测试验证

#### MQTT测试
```bash
# 检查MQTT连接状态
curl http://localhost:8080/mqtt/status

# 发送测试消息
curl http://localhost:8080/mqtt/test

# 发送自定义消息
curl -X POST "http://localhost:8080/mqtt/publish?message=测试内容"
```

#### Square Webhook测试
```bash
# 健康检查
curl http://localhost:8080/api/payments/webhook/health
```

#### Square终端支付测试
```bash
# 发送2分钱测试支付
curl -X POST http://localhost:8080/api/square/terminal/test-payment

# 发送指定金额支付
curl -X POST "http://localhost:8080/api/square/terminal/payment?amountInCents=500"
```

### ✅ 测试结果
- MQTT连接: ✅ 成功
- MQTT消息收发: ✅ 成功
- Square Webhook接收: ✅ 成功
- Square终端支付: ✅ 成功（Checkout ID: GLeqPPjeSTiqO）

### 📦 依赖项
```xml
<!-- MQTT客户端 -->
<dependency>
    <groupId>org.eclipse.paho</groupId>
    <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
    <version>1.2.5</version>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- 配置处理器 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

### 📄 配置文件
- `application.yml` - 主配置文件
  - MQTT配置
  - Square API配置
  - Webhook配置

### 📚 文档
- `MQTT使用说明.md` - MQTT功能详细说明
- `SquareWebhookController功能说明.md` - Webhook功能说明
- `Square终端支付参数配置说明.md` - 终端支付参数说明

### 🔧 技术栈
- Java 8
- Spring Boot 2.6.13
- Eclipse Paho MQTT Client 1.2.5
- Jackson (JSON处理)
- Lombok (代码简化)

### 🚀 部署
- 服务端口: 8080
- 运行方式: `java -jar quaer_api-0.0.1-SNAPSHOT.jar`

### 📌 注意事项
1. MQTT服务器密码和Square Access Token已配置在yml文件中
2. 设备号已更新为 533CS145C3000603
3. 所有敏感配置信息都在配置文件中，便于管理
4. 日志中会记录所有重要操作，便于调试和追踪

---

## 下一版本计划

### 待实现功能
- [ ] 数据库集成（保存支付记录）
- [ ] 支付记录查询接口
- [ ] 支付状态同步
- [ ] 多设备支持
- [ ] 异常处理和重试机制
- [ ] 性能监控和统计

---

## 版本管理说明

- **版本号格式**: YYMMDD.NN
  - YYMMDD: 年月日
  - NN: 当日版本号（从01开始）

- **版本记录**: 每次更新都会在此文档中追加新版本信息

- **程序中的版本号**: 在程序启动时显示版本信息

---

*文档创建时间: 2025-11-30*
*最后更新: 2025-11-30*
