# Hot Refresh Trong Backend Và Spring Boot/Spring Cloud

## 1. Hot refresh là gì?

Trong backend, **hot refresh** là việc làm mới một phần cấu hình hoặc bean runtime khi ứng dụng đang chạy, không cần restart toàn bộ app.

Trong Spring ecosystem, khi nói về hot refresh config, thường đang nói đến:

- `@RefreshScope` của Spring Cloud.
- Actuator endpoint `/actuator/refresh`.
- Spring Cloud Config Server.
- Spring Cloud Bus với Kafka/RabbitMQ.
- Spring Cloud Kubernetes Configuration Watcher.
- Cơ chế reload config từ external source như ConfigMap, Secret, Consul, Vault, database, feature flag service.

Cần phân biệt:

- **Restart**: dừng app/pod/container rồi start lại.
- **Hot refresh**: app vẫn chạy, chỉ làm mới config/bean liên quan.
- **Hot deploy/rolling deploy**: deploy version mới trong khi hệ thống vẫn phục vụ traffic.

## 2. `@RefreshScope` là gì?

`@RefreshScope` là annotation của **Spring Cloud**, không phải Spring Boot core.

Nó đánh dấu một Spring bean thuộc custom scope tên là `refresh`.

Về concept:

```java
@RefreshScope
@Component
public class PaymentProperties {
    @Value("${payment.timeout-ms}")
    private int timeoutMs;

    public int getTimeoutMs() {
        return timeoutMs;
    }
}
```

Khi có refresh event:

1. Spring reload lại `Environment` từ các config source có hỗ trợ.
2. Refresh scope clear cache của target bean.
3. Bean có `@RefreshScope` sẽ được tạo lại ở lần gọi tiếp theo.
4. Giá trị config mới được bind/inject vào bean mới.

Quan trọng: `@RefreshScope` **không tự sửa `application.yml`**, **không tự watch file**, và **không tự refresh toàn cluster**.

## 3. Cơ chế bên trong `@RefreshScope`

Spring không inject trực tiếp object thật vào các service khác. Nó thường inject một **scoped proxy**.

Ví dụ:

```text
OrderService
  -> inject PaymentProperties proxy
      -> delegate tới scopedTarget.paymentProperties thật
```

Khi app mới start:

```text
paymentProperties proxy
  -> scopedTarget.paymentProperties(timeoutMs = 3000)
```

Khi gọi:

```http
POST /actuator/refresh
```

Spring sẽ:

```text
clear scopedTarget.paymentProperties khỏi RefreshScope cache
```

Lần gọi tiếp theo:

```text
paymentProperties proxy
  -> tạo scopedTarget.paymentProperties mới
  -> bind timeoutMs từ Environment mới
```

Proxy vẫn sống, target bean phía sau proxy bị thay thế.

## 4. Vì sao sửa `application.yml` trong JAR không refresh được?

Nếu file config nằm trong:

```text
src/main/resources/application.yml
```

thì sau khi build, nó nằm trong:

```text
app.jar
```

hoặc Docker image.

Lúc này muốn đổi config thì thường phải:

```text
sửa source -> build lại -> tạo artifact/image mới -> deploy lại
```

`@RefreshScope` không có tác dụng magic trong trường hợp này, vì config không nằm ở nơi có thể thay đổi runtime.

Muốn hot refresh có ý nghĩa, config nên được **externalized**, tức nằm ngoài artifact deploy.

## 5. Các cách xử lý hot refresh config

### Cách 1: Gọi `/actuator/refresh` trên từng instance

Thêm Actuator và Spring Cloud Context/Config tùy nhu cầu.

Expose endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: refresh
```

Gọi:

```bash
curl -X POST http://localhost:8080/actuator/refresh
```

Flow:

```text
Config external thay đổi
  -> POST /actuator/refresh
  -> reload Environment
  -> clear RefreshScope cache
  -> @RefreshScope bean được recreate
```

Ưu điểm:

- Đơn giản.
- Phù hợp local, dev, single instance.
- Dễ demo và dễ hiểu cơ chế.

Nhược điểm:

- Multi-instance phải gọi từng instance.
- Cần bảo mật endpoint rất kỹ.
- Không tự động nếu không có automation.
- Không phù hợp nếu cần đồng bộ cluster chính xác.

Use case phù hợp:

- Service chạy 1 instance.
- Dev/test environment.
- Admin/CI/CD có thể trigger refresh thủ công.

### Cách 2: Spring Cloud Config Server

Config nằm ở config repository, thường là Git.

Flow:

```text
Config Git Repo
  -> Spring Cloud Config Server
  -> Client App
  -> /actuator/refresh
```

Client config ví dụ:

```yaml
spring:
  config:
    import: optional:configserver:http://config-server:8888
```

Khi cần đổi config:

```text
sửa config trong Git
  -> push
  -> gọi /actuator/refresh trên client app
```

Ưu điểm:

- Config tách khỏi app artifact.
- Có version history nếu dùng Git.
- Phù hợp microservices.
- Dễ quản lý config theo app/profile/environment.

Nhược điểm:

- Thêm một thành phần infrastructure.
- Cần quản lý security, availability của Config Server.
- Vẫn cần trigger refresh.
- Multi-instance vẫn cần Bus hoặc automation nếu muốn refresh hàng loạt.

Use case phù hợp:

- Nhiều service cần quản lý config tập trung.
- Cần audit/version config.
- Muốn đổi timeout, retry, endpoint, feature flag nhẹ mà không build lại app.

### Cách 3: Spring Cloud Bus

Spring Cloud Bus dùng message broker như Kafka hoặc RabbitMQ để broadcast refresh event.

Flow:

```text
POST /actuator/busrefresh vào 1 instance
  -> publish RefreshRemoteApplicationEvent lên Kafka/RabbitMQ
  -> các instance nhận event
  -> mỗi instance tự refresh
```

Expose endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: busrefresh
```

Ưu điểm:

- Phù hợp multi-instance.
- Chỉ cần trigger một lần.
- Có thể refresh tất cả instance cùng service hoặc theo destination.

Nhược điểm:

- Cần Kafka/RabbitMQ.
- Refresh không atomic tuyệt đối.
- Có thể có delay hoặc instance fail refresh.
- Tăng độ phức tạp vận hành.

Use case phù hợp:

- Microservice có nhiều instance.
- Đã có Kafka/RabbitMQ.
- Muốn refresh config runtime trên toàn bộ fleet.

### Cách 4: Kubernetes ConfigMap/Secret + restart pod

Config nằm trong ConfigMap/Secret. Khi config đổi, rollout/restart pod để app đọc config mới khi start.

Flow:

```text
Update ConfigMap/Secret
  -> rollout restart Deployment
  -> pod mới start với config mới
```

Ví dụ:

```bash
kubectl rollout restart deployment/payment-service
```

Ưu điểm:

- Đơn giản, predictable.
- Hợp với immutable infrastructure.
- Tránh inconsistent runtime state.
- Phù hợp production hiện đại.

Nhược điểm:

- Không phải hot refresh đúng nghĩa.
- Pod bị restart, cần readiness/liveness và rolling update đúng.
- Cần đảm bảo zero-downtime deployment.

Use case phù hợp:

- Production trên Kubernetes.
- Config quan trọng, ảnh hưởng lifecycle.
- Team ưu tiên tính ổn định hơn reload runtime.

### Cách 5: Kubernetes ConfigMap/Secret + Spring Cloud Kubernetes Configuration Watcher

Spring Cloud Kubernetes có watcher để phát hiện ConfigMap/Secret thay đổi và trigger refresh.

Flow:

```text
ConfigMap/Secret changed
  -> Configuration Watcher detect
  -> gọi /actuator/refresh tới app instances
  -> @RefreshScope bean refresh
```

Có thể dùng HTTP mode hoặc messaging mode với Spring Cloud Bus.

Ưu điểm:

- Tự động hơn so với gọi refresh thủ công.
- Phù hợp Kubernetes.
- Có thể refresh nhiều instance.

Nhược điểm:

- Cần setup RBAC, labels/annotations, watcher deployment.
- Kubernetes mounted ConfigMap update có tính eventually consistent.
- Nếu ConfigMap inject bằng environment variable thì container đang chạy thường không thấy giá trị mới.
- Tăng phức tạp so với rolling restart.

Use case phù hợp:

- Config nhẹ, có thể update runtime.
- Cần refresh nhanh mà không restart pod.
- Hệ thống đã chuẩn hóa Spring Cloud Kubernetes.

### Cách 6: External file mounted volume

App đọc config từ file ngoài artifact, ví dụ:

```bash
java -jar app.jar --spring.config.additional-location=file:/etc/myapp/
```

Config nằm trong:

```text
/etc/myapp/application.yml
```

Sau khi sửa file, cần có trigger:

```http
POST /actuator/refresh
```

hoặc custom watcher.

Ưu điểm:

- Đơn giản cho VM/bare-metal.
- Không cần Config Server.
- Config nằm ngoài JAR.

Nhược điểm:

- Spring Boot không mặc định watch file rồi reload.
- Cần có quy trình sửa file và trigger refresh.
- Dễ lệch config giữa các server nếu làm thủ công.
- Ít phù hợp cho cluster lớn.

Use case phù hợp:

- VM/on-prem.
- Ứng dụng nhỏ.
- Config ít thay đổi.

### Cách 7: Database/Admin config/Feature flag service

Thay vì dùng `application.yml`, business config được lưu trong DB hoặc feature flag system.

Ví dụ:

```text
config_key = checkout.new-flow.enabled
config_value = true
```

App có thể:

- đọc trực tiếp từ DB/cache,
- refresh cache qua admin endpoint,
- subscribe event thay đổi config.

Ưu điểm:

- Phù hợp business config.
- Có thể có UI/admin, audit, approval.
- Có thể rollout theo user/group/tenant.
- Không phụ thuộc `@RefreshScope`.

Nhược điểm:

- Phải tự thiết kế consistency, cache, fallback.
- Tăng complexity trong domain.
- Cần phân biệt business config và infrastructure config.

Use case phù hợp:

- Feature flags.
- A/B testing.
- Tenant-level configuration.
- Business rules thay đổi thường xuyên.

### Cách 8: `/actuator/env`

Spring Cloud có endpoint có thể update Environment runtime bằng POST tới `/actuator/env`, nếu được enable.

Ưu điểm:

- Linh hoạt.
- Có thể đổi key/value runtime nhanh.

Nhược điểm:

- Rất cần bảo mật.
- Dễ tạo trạng thái khó debug.
- Không nên mở public.
- Thường không phải lựa chọn mặc định trong production.

Use case phù hợp:

- Debug nội bộ có kiểm soát.
- Tooling/admin restricted network.

## 6. Multi-instance thì refresh toàn bộ được không?

`@RefreshScope` chỉ có tác dụng trong **một JVM/instance**.

Nếu có 3 pod:

```text
payment-service-1
payment-service-2
payment-service-3
```

Gọi:

```http
POST /actuator/refresh
```

vào `payment-service-1` thì chỉ instance đó refresh.

Muốn refresh toàn bộ:

- gọi `/actuator/refresh` tới từng instance,
- hoặc dùng Spring Cloud Bus với `/actuator/busrefresh`,
- hoặc dùng Kubernetes watcher/reloader/operator,
- hoặc rollout restart pod.

Lưu ý: Spring Cloud Bus không đảm bảo tất cả instance refresh **atomic cùng một thời điểm**. Nó là broadcast event, có thể có delay hoặc failure cục bộ.

## 7. `@RefreshScope` refresh được cái gì?

Phù hợp:

- timeout,
- retry count,
- feature toggle nhẹ,
- rate limit,
- cache TTL,
- API endpoint URL,
- batch size,
- flag bật/tắt integration,
- config đọc-only, ít state.

Cần cẩn thận:

- `DataSource`,
- Kafka producer/consumer infrastructure,
- Hibernate `SessionFactory`,
- transaction manager,
- thread pool,
- security filter chain,
- cache lớn,
- bean giữ state nội bộ,
- bean có lifecycle phức tạp.

Lý do: refresh bean là clear target cache và tạo bean mới. Nếu bean cũ đang giữ connection, lock, thread, transaction, listener, state nội bộ thì có thể gây inconsistent state.

## 8. Nên viết config bean như thế nào?

Nên ưu tiên gom config vào class riêng:

```java
@RefreshScope
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {
    private int timeoutMs;
    private boolean enabled;

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
```

Dùng trong service:

```java
@Service
public class PaymentService {
    private final PaymentProperties properties;

    public PaymentService(PaymentProperties properties) {
        this.properties = properties;
    }

    public void pay() {
        int timeout = properties.getTimeoutMs();
        boolean enabled = properties.isEnabled();
    }
}
```

Nên hạn chế rải rác:

```java
@Value("${payment.timeout-ms}")
private int timeoutMs;
```

ở nhiều service khác nhau, vì khó track, khó test và khó refresh nhất quán.

## 9. Bảo mật endpoint refresh

Không nên expose các endpoint này public:

- `/actuator/refresh`
- `/actuator/busrefresh`
- `/actuator/env`
- `/actuator/restart`

Cần:

- đặt sau auth,
- restrict network/internal only,
- audit ai trigger,
- log config keys thay đổi,
- không log secret value,
- cân nhắc approval flow cho production.

## 10. So sánh nhanh các cách

| Cách | Hot refresh đúng nghĩa | Multi-instance | Độ phức tạp | Phù hợp production |
|---|---:|---:|---:|---|
| `/actuator/refresh` từng instance | Có | Thủ công | Thấp | Có, nếu bảo mật tốt và ít instance |
| Spring Cloud Config Server | Có | Cần Bus/automation | Trung bình | Có |
| Spring Cloud Bus | Có | Có | Trung bình/cao | Có, nếu đã có broker |
| Kubernetes rollout restart | Không | Có | Thấp/trung bình | Rất phù hợp |
| Spring Cloud Kubernetes Watcher | Có | Có | Trung bình/cao | Có, nếu team chấp nhận complexity |
| External file + refresh | Có, có điều kiện | Thủ công | Thấp/trung bình | Phù hợp VM/on-prem nhỏ |
| DB/admin config | Có | Tùy thiết kế | Trung bình/cao | Phù hợp business config |
| `/actuator/env` | Có | Cần Bus nếu nhiều instance | Cao về risk | Hạn chế |

## 11. Khuyến nghị thực tế

Nếu config nằm trong `src/main/resources/application.yml`:

```text
Đừng nghĩ @RefreshScope sẽ giúp đổi config sau deploy.
Cần build/deploy lại nếu config nằm trong artifact.
```

Nếu dùng Kubernetes production:

```text
Config quan trọng/lifecycle phức tạp -> rollout restart pod.
Config nhẹ/cần đổi nhanh -> cân nhắc Spring Cloud Kubernetes Watcher hoặc Spring Cloud Bus.
```

Nếu dùng microservices nhiều instance:

```text
Spring Cloud Config Server + Spring Cloud Bus là combo phổ biến.
```

Nếu là feature flag/business config:

```text
Cân nhắc DB/admin config hoặc feature flag platform thay vì application.yml.
```

Nếu chỉ cần dev/test:

```text
/actuator/refresh là đủ để hiểu và demo cơ chế.
```

## 12. Tóm tắt cuối

`@RefreshScope` chỉ giải quyết phần **bean có thể được recreate sau refresh event**.

Nó không giải quyết các việc sau:

- không tự sửa file config,
- không tự watch `application.yml`,
- không tự refresh tất cả instance,
- không đảm bảo consistency tuyệt đối trong cluster,
- không phù hợp cho mọi loại bean.

Công thức đúng là:

```text
External config source
  + refresh trigger
  + @RefreshScope/config rebinding
  + security/observability
  = hot refresh config có kiểm soát
```

