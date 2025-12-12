# 停车场管理系统 (Parking Management System)

基于 Spring Boot 的智能停车场管理系统，集成 MQTT 通信和 Square 支付功能。

## 项目简介

本系统是一个完整的停车场管理解决方案，支持：
- 车辆进出记录管理
- MQTT 实时通信
- Square 终端支付集成
- 车牌识别截图上传
- Webhook 支付回调处理

## 运行环境要求

### 1. Java 环境
- **JDK 版本**: JDK 8 或以上（推荐 JDK 11/17）
- **下载地址**: https://www.oracle.com/java/technologies/downloads/
- **验证安装**:
  ```bash
  java -version
  ```

### 2. MySQL 数据库
- **版本**: MySQL 8.0 或以上
- **下载地址**: https://dev.mysql.com/downloads/mysql/
- **验证安装**:
  ```bash
  mysql --version
  ```

### 3. Maven 构建工具
- **版本**: Maven 3.6+
- **下载地址**: https://maven.apache.org/download.cgi
- **验证安装**:
  ```bash
  mvn -version
  ```

## 数据库配置

### 1. 创建数据库用户和数据库

```sql
-- 创建数据库
CREATE DATABASE parking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户（如果不存在）
CREATE USER 'Parking'@'localhost' IDENTIFIED BY 'aA135135@@@';

-- 授予权限
GRANT ALL PRIVILEGES ON parking.* TO 'Parking'@'localhost';
FLUSH PRIVILEGES;
```

### 2. 导入数据库

项目包含数据库导出文件 `parking_database.sql`，导入方式：

**方式一：命令行导入**
```bash
mysql -u Parking -p parking < parking_database.sql
```

**方式二：MySQL Workbench 导入**
1. 打开 MySQL Workbench
2. 连接到数据库
3. Server -> Data Import
4. 选择 `parking_database.sql` 文件
5. 执行导入

## 项目配置

### 1. 修改配置文件

编辑 `src/main/resources/application.yml`，根据实际情况修改以下配置：

#### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/parking?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: Parking
    password: aA135135@@@  # 修改为你的数据库密码
```

#### MQTT 配置（可选）
```yaml
mqtt:
  broker-url: tcp://49.234.8.138:1883  # MQTT 服务器地址
  client-id: parking_server
  username: parking_device
  password: Device@2025
```

#### Square 支付配置（可选）
```yaml
square:
  access-token: YOUR_ACCESS_TOKEN  # 替换为你的 Square Access Token
  location-id: YOUR_LOCATION_ID    # 替换为你的 Location ID
  device-id: YOUR_DEVICE_ID        # 替换为你的设备 ID
```

#### 日志和截图路径
```yaml
logging:
  file:
    path: D:/停车场/quare_api/logs  # 修改为实际路径

# 截图存储路径（Windows 默认 D:/停车场/snapshots，Linux 默认 /opt/quaer_api/snapshots）
```

## 编译和运行

### 1. 编译项目

```bash
# 进入项目目录
cd D:\停车场\quare_api\quaer_api

# 编译打包（跳过测试）
mvn clean package -DskipTests
```

### 2. 运行项目

**方式一：使用 Maven 运行**
```bash
mvn spring-boot:run
```

**方式二：运行打包后的 JAR**
```bash
java -jar target/quaer_api-0.0.1-SNAPSHOT.jar
```

### 3. 访问系统

- **默认端口**: 8086
- **访问地址**: http://localhost:8086
- **测试页面**: http://localhost:8086/index.html

## API 接口说明

### 车辆记录
- `GET /api/vehicles` - 获取所有车辆记录
- `GET /api/vehicles/{id}` - 获取指定车辆记录
- `GET /api/vehicles/license/{licensePlate}` - 根据车牌查询

### MQTT 测试
- `POST /api/mqtt/send` - 发送 MQTT 消息

### Square 支付
- `POST /api/square/terminal/checkout` - 创建终端支付
- `POST /api/payments/webhook` - Square Webhook 回调

## 常见问题

### 1. 数据库连接失败
- 检查 MySQL 服务是否启动
- 验证数据库用户名和密码
- 确认数据库 `parking` 已创建

### 2. 端口被占用
修改 `application.yml` 中的端口：
```yaml
server:
  port: 8086  # 改为其他可用端口
```

### 3. Maven 依赖下载慢
配置国内镜像源，编辑 `~/.m2/settings.xml`：
```xml
<mirror>
  <id>aliyun</id>
  <mirrorOf>central</mirrorOf>
  <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

## 目录结构

```
quaer_api/
├── src/main/java/com/quaer_api/
│   ├── controller/       # 控制器层
│   ├── service/          # 业务逻辑层
│   ├── repository/       # 数据访问层
│   ├── entity/           # 实体类
│   ├── dto/              # 数据传输对象
│   ├── config/           # 配置类
│   └── util/             # 工具类
├── src/main/resources/
│   ├── application.yml   # 应用配置
│   └── static/           # 静态资源
├── parking_database.sql  # 数据库导出文件
├── schema.sql            # 数据库表结构
└── pom.xml               # Maven 配置
```

## 技术栈

- **框架**: Spring Boot 3.x
- **数据库**: MySQL 8.0 + Spring Data JPA
- **MQTT**: Eclipse Paho
- **支付**: Square API
- **构建工具**: Maven
- **日志**: Logback

## 开发者

- 用户: susu
- 邮箱: 276746009@qq.com

## 许可

版权所有 © 2025
