# Square Terminal 支付命令参数配置说明

## 原始 curl 命令分析

```bash
curl https://connect.squareup.com/v2/terminals/checkouts \
  -X POST \
  -H 'Square-Version: 2025-10-16' \
  -H 'Authorization: Bearer EAAAl9Se8DP-KKycVFi0HiFU_9-bHh0F9m9c3XvGaH6S7Oeuvp-VfiQWALTE0FYY' \
  -H 'Content-Type: application/json' \
  -d '{
    "checkout": {
      "amount_money": {
        "currency": "CAD",
        "amount": 2
      },
      "device_options": {
        "device_id": "533CS145C3000603"
      }
    },
    "idempotency_key": "91ab9d34-f327-404d-95cf-f30fc029a25d"
  }'
```

---

## 参数分类

### ✅ 应该从配置文件读取的参数

| 参数 | 说明 | 配置项 | 当前值 | 备注 |
|------|------|--------|--------|------|
| **API Base URL** | Square API 基础地址 | `square.base-url` | `https://connect.squareup.com` | 固定不变 |
| **API Version** | Square API 版本 | `square.api-version` | `2025-10-16` | 定期更新 |
| **Access Token** | 访问令牌 | `square.access-token` | `EAAAl9Se8DP...` | 敏感信息 |
| **Device ID** | 终端设备ID | `square.device-id` | `533CS145C3000095` | **⚠️ 与命令不一致!** |
| **Currency** | 默认货币 | `square.currency` | `CAD` | 业务默认值 |

### ⚠️ **重要警告: Device ID 不一致**

- **配置文件中**: `533CS145C3000095`
- **curl 命令中**: `533CS145C3000603`

**这是两个不同的设备！请确认：**
1. 你有多台 POS 机？
2. 哪个设备ID是正确的？
3. 是否需要支持多设备？

---

### 🔄 动态参数（每次请求不同，不应配置）

| 参数 | 说明 | 如何生成 |
|------|------|----------|
| **amount** | 支付金额（分） | 由业务逻辑决定 |
| **idempotency_key** | 幂等性密钥 | 使用 UUID 生成 |

---

## 完整配置文件 (application.yml)

```yaml
# Square支付配置
square:
  # API配置
  api-version: 2025-10-16
  access-token: EAAAl9Se8DP-KKycVFi0HiFU_9-bHh0F9m9c3XvGaH6S7Oeuvp-VfiQWALTE0FYY
  base-url: https://connect.squareup.com

  # 商户配置
  location-id: LYZ4X83G13CQK
  environment: production
  application-id: sq0idp-uSq1U9r96qjboOEDiqFD1A

  # 终端设备配置
  device-id: 533CS145C3000095  # ⚠️ curl命令中使用的是 533CS145C3000603

  # 如果有多台设备，可以这样配置:
  # devices:
  #   - id: 533CS145C3000095
  #     name: 前台POS-1
  #   - id: 533CS145C3000603
  #     name: 前台POS-2

  # 默认货币
  currency: CAD

  # Webhook配置
  webhook:
    url: https://car.test001.cn:8083/api/payments/webhook
    signature-key: y834xn53DXGniXAGr7NtCA
```

---

## Java 代码示例：如何使用这些配置

### 1. 创建配置属性类

```java
@Data
@Component
@ConfigurationProperties(prefix = "square")
public class SquareProperties {

    // API配置
    private String apiVersion;
    private String accessToken;
    private String baseUrl;

    // 商户配置
    private String locationId;
    private String environment;
    private String applicationId;

    // 设备配置
    private String deviceId;

    // 货币配置
    private String currency;

    // Webhook配置
    private WebhookConfig webhook;

    @Data
    public static class WebhookConfig {
        private String url;
        private String signatureKey;
    }
}
```

### 2. 创建终端支付服务

```java
@Service
@Slf4j
public class SquareTerminalService {

    @Autowired
    private SquareProperties squareProperties;

    /**
     * 发起终端支付
     * @param amountInCents 金额（分）
     * @return 支付结果
     */
    public String createTerminalCheckout(long amountInCents) {

        // 从配置读取固定参数
        String url = squareProperties.getBaseUrl() + "/v2/terminals/checkouts";
        String apiVersion = squareProperties.getApiVersion();
        String accessToken = squareProperties.getAccessToken();
        String deviceId = squareProperties.getDeviceId();
        String currency = squareProperties.getCurrency();

        // 生成动态参数
        String idempotencyKey = UUID.randomUUID().toString();

        // 构建请求体
        JSONObject request = new JSONObject();
        JSONObject checkout = new JSONObject();
        JSONObject amountMoney = new JSONObject();
        amountMoney.put("currency", currency);  // 从配置读取
        amountMoney.put("amount", amountInCents);  // 动态参数

        JSONObject deviceOptions = new JSONObject();
        deviceOptions.put("device_id", deviceId);  // 从配置读取

        checkout.put("amount_money", amountMoney);
        checkout.put("device_options", deviceOptions);
        request.put("checkout", checkout);
        request.put("idempotency_key", idempotencyKey);  // 动态生成

        log.info("发起终端支付:");
        log.info("  设备ID: {}", deviceId);
        log.info("  金额: {} {}", formatAmount(amountInCents), currency);
        log.info("  幂等性密钥: {}", idempotencyKey);

        // 发送HTTP请求
        // ... (使用 RestTemplate 或 HttpClient)

        return "支付请求已发送";
    }

    private String formatAmount(long amountInCents) {
        return String.format("$%.2f", amountInCents / 100.0);
    }
}
```

### 3. 创建测试控制器

```java
@RestController
@RequestMapping("/api/square/terminal")
public class SquareTerminalController {

    @Autowired
    private SquareTerminalService terminalService;

    /**
     * 测试发起2分钱支付
     */
    @PostMapping("/test-payment")
    public ResponseEntity<String> testPayment() {
        String result = terminalService.createTerminalCheckout(2);
        return ResponseEntity.ok(result);
    }

    /**
     * 发起指定金额支付
     */
    @PostMapping("/payment")
    public ResponseEntity<String> createPayment(@RequestParam long amountInCents) {
        String result = terminalService.createTerminalCheckout(amountInCents);
        return ResponseEntity.ok(result);
    }
}
```

---

## 参数对照表

| curl 命令中的参数 | 配置文件路径 | 类型 |
|-------------------|--------------|------|
| `https://connect.squareup.com` | `square.base-url` | 固定 |
| `Square-Version: 2025-10-16` | `square.api-version` | 固定 |
| `Authorization: Bearer EAAAl...` | `square.access-token` | 固定 |
| `"currency": "CAD"` | `square.currency` | 固定 |
| `"device_id": "533CS145C3000603"` | `square.device-id` | 固定 |
| `"amount": 2` | 方法参数 | 动态 |
| `"idempotency_key": "..."` | UUID生成 | 动态 |

---

## 使用示例

### 发起2分钱测试支付

```bash
curl -X POST http://localhost:8080/api/square/terminal/test-payment
```

### 发起指定金额支付

```bash
# 支付 $5.00 (500分)
curl -X POST "http://localhost:8080/api/square/terminal/payment?amountInCents=500"
```

---

## 配置优先级建议

### 必须从配置文件读取：
1. ✅ `access-token` - 安全凭证
2. ✅ `api-version` - API版本
3. ✅ `base-url` - API地址
4. ✅ `device-id` - 设备标识

### 建议从配置文件读取：
1. ✅ `currency` - 默认货币
2. ✅ `location-id` - 位置ID
3. ✅ `application-id` - 应用ID

### 绝对不能写在配置文件：
1. ❌ `amount` - 每次不同
2. ❌ `idempotency_key` - 必须唯一

---

## ⚠️ 需要确认的问题

1. **Device ID 不一致**
   - 配置文件: `533CS145C3000095`
   - curl命令: `533CS145C3000603`
   - **请确认使用哪一个！**

2. **是否需要支持多设备？**
   - 如果有多台POS机，需要改为设备列表配置

3. **API Version 是否需要定期更新？**
   - 当前: `2025-10-16`
   - Square可能会发布新版本API

---

## 总结

### 从配置文件读取的参数（6个）：
1. `square.api-version` = `2025-10-16`
2. `square.access-token` = `EAAAl9Se8DP...`
3. `square.base-url` = `https://connect.squareup.com`
4. `square.device-id` = `533CS145C3000095` ⚠️
5. `square.currency` = `CAD`
6. `square.webhook.signature-key` = `y834xn53DXGniXAGr7NtCA`

### 动态生成的参数（2个）：
1. `amount` - 业务逻辑决定
2. `idempotency_key` - UUID.randomUUID()
