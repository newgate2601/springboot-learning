# Camunda 7: các bước đầu của Cashloan

Đây là process học tập `CAKE_CASHLOAN_LEARNING`, độc lập với process `CAKE_CASHLOAN` đang có trong Camunda Run. App Spring Boot tự deploy BPMN khi khởi động và poll external tasks ở Camunda Run (`http://localhost:8080/engine-rest`). Không cần các class delegate của hệ thống cho vay gốc.

Flow: Application init → phân loại segment → Precheck → cổng cần chấm điểm. Các bước chỉ log và dùng kết quả mẫu do request truyền vào. Nếu thất bại thì kết thúc ở nhánh tương ứng; nếu thành công thì kết thúc tại `ready_for_scoring` (sẽ học tiếp chấm điểm sau) hoặc `skip_scoring` (segment ngoại mạng). Subprocess demo chỉ có một external task; không mô phỏng cặp gửi/nhận message bất đồng bộ của BPMN gốc.

## Chạy

1. Bật Camunda Run 7 tại cổng 8080 và PostgreSQL của dự án tại cổng 5432.
2. Chạy `LearningApplication` trong IDE, hoặc đặt `JAVA_HOME` tới JDK 21 và chạy `.\mvnw.cmd spring-boot:run`. App dùng cổng 8086. Nếu Camunda Run khởi động sau app, khởi động lại app để deploy process.
3. Trong Cockpit, chọn process **Cashloan - các bước đầu (demo)** để xem các instance.

Tạo instance mặc định (segment thành công, Precheck đạt, cần chấm điểm):

```powershell
$result = Invoke-RestMethod -Method Post -Uri 'http://localhost:8086/api/v1/camunda-demo/start' -ContentType 'application/json' -Body '{}'
Invoke-RestMethod -Uri "http://localhost:8086$($result.statusUrl)"
```

Poll lại URL trạng thái sau khoảng 1–2 giây nếu kết quả còn `RUNNING`. Trường `outcome` sẽ là một trong `evaluate_customer_segment_fail`, `precheck_1_vds_fail`, `ready_for_scoring`, `skip_scoring`.

Đổi dữ liệu mẫu trong body để thử nhánh:

| Body JSON | Kết quả |
| --- | --- |
| `{"segmentSuccess":false}` | Phân loại thất bại |
| `{"precheckPassed":false}` | Precheck thất bại |
| `{"segmentType":"VTP_OFF_NET"}` | Bỏ qua chấm điểm |
| `{}` | Chờ chấm điểm |

Ví dụ:

```powershell
Invoke-RestMethod -Method Post -Uri 'http://localhost:8086/api/v1/camunda-demo/start' -ContentType 'application/json' -Body '{"segmentType":"VTP_OFF_NET"}'
```

App log `Phân loại segment`, `Precheck` và ID của instance. BPMN ở `src/main/resources/processes/cashloan-first-steps.bpmn`; biến gateway tương ứng là `isSegmentEvaluatedSuccess`, `isPrecheckPassed`, `segmentType`. Cấu hình địa chỉ engine và chu kỳ poll ở `application.yaml`.

Kiểm thử bốn nhánh với Camunda Run đang bật: đặt biến môi trường `CAMUNDA_DEMO_IT=true` rồi chạy `.\mvnw.cmd test`. Test này tạo bốn instance thật trong Camunda Run.
