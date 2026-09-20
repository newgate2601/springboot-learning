# lending-event-backbone ↔ Camunda: cách hoạt động

Nguồn: `C:\code\lending\lending\lending-event-backbone` (Spring Boot, Java 8, Camunda 7.18.0 nhúng qua `camunda-bpm-spring-boot-starter`). Phân tích dựa trên BPMN mẫu `CAKE_CASHLOAN` (tìm thấy bản gần giống tại `C:\code\camuda\springboot-learning\src\main\resources\processes\cake-cashloan.bpmn`).

## 1. Camunda engine chạy nhúng, không phải external-task tách rời

`pom.xml` khai báo `camunda-bpm-spring-boot-starter` + `camunda-bpm-spring-boot-starter-rest` 7.18.0, nghĩa là process engine chạy **in-process** ngay trong service này (cùng datasource, cùng JVM). Các `camunda:class="...Delegate"` trong BPMN là `JavaDelegate` Spring bean thật, được engine gọi trực tiếp trong thread xử lý — không phải REST/gRPC ra ngoài.

`lending-event-backbone` tự nó **không chứa file `.bpmn`** trong `src/main/resources` — quy trình được thiết kế/lưu ở nơi khác (project `camuda` bạn đang có sẵn là ví dụ) rồi mới deploy vào engine của service này (qua auto-deployment classpath `processes/**`, hoặc qua Camunda REST admin deploy — chưa xác nhận được bước deploy thực tế trong repo).

## 2. Mẫu "gửi song song + chờ song song" lặp lại trong mọi sub-process

Mọi sub-process trong BPMN (`underwriting`, `decision_making`, `precheck_1`, `precheck_2_*`, `offer_package`, `get_repeat_package`, `evaluate_customer_segment`, `check_standardized_profile`, `scoring`) đều theo đúng 1 khuôn:

```
startEvent → parallelGateway (fork)
   ├─ sendTask (camunda:asyncBefore="true", camunda:class=XxxDelegate)  → publish Kafka request
   └─ receiveTask (messageRef=..._RESULT)                              → park chờ message
   → parallelGateway (join) → endEvent
```

Ví dụ cụ thể sub-process `underwriting`:

- **sendTask "Thẩm định hồ sơ"** chạy `UnderwritingApplicationDelegate.execute()`. Delegate build `Event<UnderwritingApplicationRequest>` rồi gọi `messagingPort.pushAsync(event, messageTopic.getLendingApplicationService())` — publish lên Kafka topic `lending-application-service` (`kafka.lending.topic.lending-application-service`). Nhờ `camunda:asyncBefore="true"`, bước này chạy qua Job Executor của Camunda (async continuation), tách khỏi transaction gốc — publish lỗi sẽ được Camunda tự retry qua job retry, không rollback cả process.
- **receiveTask "Thẩm định hồ sơ"** (messageRef `UNDERWRITING_RESULT`) không chạy code gì cả — nó chỉ khiến process instance **park**, subscribe một Camunda message event tên `UNDERWRITING_RESULT` trên chính process instance đó. Token chỉ đi tiếp khi có ai đó gọi `correlate()` đúng message này.
- Vì cổng join (parallel gateway thứ 2) cần cả 2 nhánh, sub-process **thực chất bị chặn tại receiveTask** cho tới khi có response — nhánh sendTask hoàn tất gần như ngay lập tức.

Service nghiệp vụ (underwriting/scoring/precheck/...) xử lý bất đồng bộ ở phía nó, xong thì publish kết quả ngược lại lên Kafka topic riêng của service này: `lending-event-backbone-service` (`kafka.lending.topic.lending-event-backbone-service`).

## 3. Chiều nhận — "correlate" message để đánh thức process

`EventConsumerHandler` (`@KafkaListener` trên topic `lending-event-backbone-service`):

1. Deserialize payload → `BaseEvent`.
2. Ghi audit log vào bảng `EventBackboneStore` (đúng như tên service — mọi event in/out đều được lưu vết).
3. Tra `CommandType` → tìm `EventHandler` tương ứng qua `EventHandlerFactory` (`DefaultEventHandlerFactory` auto-đăng ký tất cả bean `EventHandler` vào map ở `@PostConstruct`).
4. Gọi `eventHandler.handleEvent(...)` — chạy **bất đồng bộ** (`@Async("customThreadPoolExecutor")`).

Handler cụ thể, ví dụ `UnderwritingResponseEventHandler.doHandle()`:

- Build map biến BPM (`STATUS`, `SUB_STATUS`, `ERROR_CODE`, `IS_UNDERWRITING_PASSED`, ...) từ payload.
- Guard: `bpmTemplate.existsWaitingMessage(applicationId, Message.UNDERWRITING_RESULT)` — kiểm tra process có đang thực sự chờ message này không (tránh corelate vào process đã đi tiếp/đã huỷ).
- `bpmTemplate.correlateMessageBusinessKey("UNDERWRITING_RESULT", applicationId, variables)` → dưới nền là `runtimeService.createMessageCorrelation(...).processInstanceBusinessKey(...).setVariables(...).correlate()` trong `CamundaAdapter`.

Correlate xong → receiveTask nhận được, set biến (trong đó có `underwritingPassed`), process đi tiếp qua join → ra khỏi sub-process → `gateway.check_underwriting_passed` đọc đúng biến `underwritingPassed` để rẽ nhánh ký hợp đồng hay từ chối.

`BaseEventHandler` có `@Retryable` cho `OptimisticLockingException` (đụng độ ghi cùng process instance) và `MismatchingMessageCorrelationException` (message đến trễ/trùng sau khi process đã đi tiếp), kèm `@Recover` hạ xuống log WARN thay vì fail cứng — thiết kế chịu được message trễ/duplicate từ Kafka.

## 4. `submit_application` — throw message event, không phải request/response

Node `submit_application` là `intermediateThrowEvent` với `messageEventDefinition camunda:class=SubmitApplicationDelegate`. Khác sendTask/receiveTask: đây là **throw** — chạy delegate đồng bộ ngay khi token đi qua (không có receive counterpart trong BPMN này). Delegate chỉ publish một event thông báo `BPM_SUBMIT_APPLICATION` lên topic `bpm-process-instance-event`, rồi flow đi tiếp luôn vào sub-process `underwriting`. Đây là notification ra ngoài, không chặn process.

## 5. `CamundaEventListener` — kênh quan sát tiến trình song song, không thuộc luồng nghiệp vụ

Đây là 1 Spring `@EventListener` bắt **mọi** sự kiện START/END của mọi activity trong process (Camunda Spring Boot Starter publish execution event thành Spring ApplicationEvent). Với mỗi service task / receive task / user task / end event, nó build `BpmStatusResponse` (activity hiện tại, activity trước, status/subStatus, thời gian mỗi bước qua biến `send_time_*` / `receive_time_*`) và publish qua `bpmService.sendBpmStatusEvent(...)` — đây chính là nguồn dữ liệu cho các màn hình theo dõi tiến trình (kiểu "Lượt chạy" / Process Execution mà bạn đang làm ở AMIS) mà không cần poll trực tiếp bảng history của Camunda.

## 6. 3 topic Kafka tách biệt = "event backbone" thật sự

| Chiều | Topic (property) | Mục đích |
|---|---|---|
| Request ra service nghiệp vụ | `kafka.lending.topic.lending-application-service` | precheck, underwriting, scoring, decision-making, ekyc, offer-package... |
| Response về engine | `kafka.lending.topic.lending-event-backbone-service` | engine consume, correlate message vào process đang chờ |
| Trạng thái/telemetry | `kafka.lending.topic.bpm-process-instance-event` (`lending.bpm.process_instance_event`) | trạng thái tiến trình cho hệ thống khác theo dõi |

Nói cách khác: Camunda **không gọi REST trực tiếp** tới các service nghiệp vụ để chờ đồng bộ. Toàn bộ request/response đều đi qua Kafka theo mẫu send/receive message — `lending-event-backbone` đóng vai trò hub (orchestrator) giữa engine Camunda và các service nghiệp vụ, decouple hoàn toàn qua message queue.

## Ghi chú

Người dùng ban đầu có nhắc tới một truy vấn "sorting lines" đang chậm cần tối ưu, nhưng chưa gửi kèm câu query/code cụ thể trong hội thoại này — chưa có gì để sửa. Nếu vẫn cần, gửi lại đoạn code/câu query đó là mình xem tiếp.