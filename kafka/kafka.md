# Apache Kafka

## Event-Driven Architecture

🙂 **Event-Driven Architecture (EDA)** là kiến trúc trong đó các thành phần phối hợp chủ yếu bằng cách **phát sinh, truyền và phản ứng với event**. Event là bản ghi bất biến mô tả sự thật đã xảy ra như `OrderCreated`, `PaymentSucceeded`, `InventoryReserved`.

```text
Order Service ──publish──> OrderCreated
                              ├──> Payment Service
                              ├──> Inventory Service
                              ├──> Notification Service
                              └──> Analytics Service
```

Producer biết điều gì đã xảy ra nhưng không cần biết tất cả ai sẽ phản ứng. Consumer tự đăng ký, deploy và scale độc lập.

> EDA không đồng nghĩa với Kafka. EDA là cách thiết kế; Kafka, RabbitMQ hoặc cloud pub/sub là hạ tầng có thể dùng để hiện thực kiến trúc đó.

### Tại sao cần Event-Driven Architecture?

Synchronous flow `Order -> Payment -> Inventory -> Shipping -> Notification` dễ hiểu khi hệ thống nhỏ, nhưng khi số service và lưu lượng tăng lên sẽ xuất hiện các vấn đề sau.

#### 1. Temporal coupling

**Temporal coupling** nghĩa là các service phải cùng hoạt động tại đúng thời điểm request được xử lý.

```text
Order Service       UP
Payment Service     UP
Inventory Service   DOWN  -> cả synchronous flow bị chặn
Shipping Service    UP
```

Chỉ cần Inventory đang restart, deploy hoặc mất kết nối thì toàn bộ request có thể thất bại dù các service khác vẫn hoạt động. Producer và downstream bị ràng buộc về **thời gian tồn tại**: caller không thể hoàn tất nếu callee chưa sẵn sàng ngay lúc đó.

Với event-driven flow, Order Service có thể publish `OrderCreated` vào hạ tầng lưu event. Inventory Service tạm dừng rồi đọc event sau khi hoạt động trở lại, miễn event vẫn còn được lưu. Producer và consumer không bắt buộc online cùng lúc.

> Không phải bước nào cũng có thể xử lý sau. Nếu business bắt buộc xác nhận còn hàng trước khi trả kết quả checkout, bước đó vẫn cần synchronous call hoặc một cơ chế reservation phù hợp.

#### 2. Availability coupling

**Availability coupling** nghĩa là độ sẵn sàng của toàn bộ flow phụ thuộc vào độ sẵn sàng của mọi service trên critical path.

Giả sử mỗi service có availability `99.9%` và request phải đi qua năm service độc lập:

```text
Availability xấp xỉ
= 99.9% × 99.9% × 99.9% × 99.9% × 99.9%
≈ 99.5%
```

Con số chỉ mang tính minh họa vì failure thực tế không hoàn toàn độc lập. Ý chính là thêm dependency bắt buộc có thể làm availability end-to-end thấp hơn availability của từng service.

```text
Order đã lưu -> Payment thành công -> Inventory đã giữ
                                     -> Notification timeout
                                     -> API trả lỗi 500
```

User nhận lỗi dù nghiệp vụ quan trọng đã chạy gần hoàn tất. Nếu Notification chỉ là side effect, đặt nó trên critical path là coupling không cần thiết. Publish `OrderConfirmed` để Notification xử lý bất đồng bộ giúp lỗi gửi email không làm lỗi việc xác nhận order.

EDA không làm downstream tự nhiên có availability cao hơn. Nó giúp **cô lập lỗi**, cho phép consumer phục hồi và xử lý backlog; broker/event platform lúc này lại là dependency hạ tầng cần được thiết kế chịu lỗi.

#### 3. Latency cộng dồn

Trong chuỗi gọi tuần tự, caller phải chờ từng network round-trip và thời gian xử lý của từng downstream:

```text
Order DB                 40 ms
Payment Service         250 ms
Inventory Service       120 ms
Shipping Service        180 ms
Notification Service    300 ms
--------------------------------
Tổng gần đúng            890 ms
```

Ngoài business logic còn có DNS, connection pool, TLS, serialization, network congestion, queue trong server và Garbage Collection. Chỉ một service có tail latency cao cũng kéo dài toàn bộ request:

```text
Payment bình thường:  250 ms
Payment tại p99:     2.000 ms
                     -> latency end-to-end tăng mạnh
```

Gọi song song các downstream độc lập có thể giảm tổng thời gian xuống gần latency của nhánh chậm nhất, nhưng caller vẫn phải chờ và xử lý partial failure.

Với EDA, API chỉ hoàn tất phần bắt buộc rồi publish event; Notification, Analytics hoặc Loyalty chạy sau. Điều này giảm **response latency mà client phải chờ**, không có nghĩa toàn bộ business process hoàn thành ngay. End-to-end processing latency có thể dài hơn và cần SLA riêng.

#### 4. Retry mơ hồ

Timeout chỉ nói rằng caller **không nhận được response đúng hạn**, không khẳng định downstream chưa thực hiện nghiệp vụ.

```text
Order Service -> Payment Service: charge 1.250.000 VND
Payment Service -> ngân hàng: thanh toán thành công
Payment Service -> lưu transaction thành công
Response -> bị mất hoặc tới sau timeout
Order Service -> chỉ nhìn thấy timeout
```

Nếu Order Service retry ngây thơ, khách hàng có thể bị charge hai lần. Nếu không retry, order có thể bị đánh dấu thất bại dù tiền đã trừ.

Các câu hỏi bắt buộc xuất hiện:

- Retry lỗi nào: timeout, `503`, connection reset hay tất cả?
- Retry bao nhiêu lần và backoff thế nào?
- Request đầu tiên đã tạo side effect chưa?
- Downstream có hỗ trợ idempotency key không?
- Caller crash sau khi downstream thành công thì phục hồi state thế nào?

EDA cũng có duplicate vì producer hoặc consumer có thể retry. Event flow phải thiết kế delivery semantics, mang `eventId`/business key và yêu cầu consumer idempotent. Broker không tự giải quyết idempotency của Payment hoặc external database.

#### 5. Khó thêm downstream mới

Trong synchronous orchestration, Order Service thường biết trực tiếp mọi downstream:

```java
paymentClient.charge(order);
inventoryClient.reserve(order);
notificationClient.send(order);
analyticsClient.track(order);
loyaltyClient.addPoints(order);
```

Thêm Fraud Detection hoặc Recommendation đòi hỏi sửa code, config, authentication, timeout, circuit breaker, test và deployment của Order Service. Service gốc ngày càng biết nhiều capability không thuộc trách nhiệm cốt lõi:

```text
Thêm Loyalty Service
  -> sửa Order Service
  -> thêm client/config/auth
  -> thay đổi error handling
  -> regression test checkout
  -> deploy lại Order Service
```

Với event `OrderConfirmed`, consumer mới chỉ cần subscribe contract phù hợp; producer không cần biết Analytics, Loyalty hay Recommendation tồn tại.

Việc thêm consumer vẫn có chi phí: schema compatibility, ACL, tải bổ sung lên broker, replay policy và tác động nếu consumer gọi ngược hệ thống nguồn.

#### 6. Khó hấp thụ traffic spike

Giả sử flash sale tạo `10.000 order/s`, nhưng Inventory chỉ xử lý được `3.000 request/s`:

```text
Order traffic:       10.000 request/s
Inventory capacity:  3.000 request/s
Chênh lệch:           7.000 request/s
```

Trong synchronous flow, request dồn vào connection pool, thread pool và queue trong memory:

```text
Traffic spike
  -> connection pool đầy
  -> request chờ lâu
  -> timeout
  -> caller retry
  -> traffic tăng thêm
  -> cascading failure
```

Retry đồng loạt có thể tạo **retry storm**: downstream vừa phục hồi đã bị request cũ và mới đánh cùng lúc.

Event broker có thể đóng vai trò buffer:

```text
Producer: 10.000 event/s -> durable event log -> Consumer: 3.000 event/s
                                      |
                                      -> backlog/consumer lag tăng
```

Producer tiếp tục ghi trong giới hạn throughput và storage; consumer xử lý theo capacity rồi bắt kịp sau khi spike kết thúc. Nhưng buffer không tạo thêm năng lực xử lý và không vô hạn:

- Backlog tăng thêm `7.000 event/s`.
- Consumer lag làm business result tới chậm hơn.
- Spike kéo dài có thể làm cạn storage hoặc vượt retention.
- Vẫn cần autoscaling, backpressure, rate limit, quota và capacity planning.

Kafka giúp lưu backlog bền vững và quan sát consumer lag, nhưng hệ thống vẫn phải xác định độ trễ tối đa chấp nhận được và cách bảo vệ downstream.

### Thành phần và cấu trúc event

```text
Producer -> topic/queue/stream -> broker/platform -> consumer/processor
```

- **Producer** phát hiện thay đổi và publish event.
- **Channel** là luồng logic như `order-events`.
- **Broker/platform** nhận, lưu hoặc route event.
- **Consumer/processor** filter, enrich, join, aggregate hoặc tạo event mới.
- **Contract/schema** quy định field, type, semantic và compatibility.

```json
{
  "eventId": "01J6A8Y7M4N9K2P3Q5R6S7T8V9",
  "eventType": "OrderCreated",
  "eventVersion": 2,
  "occurredAt": "2026-08-28T09:15:30Z",
  "producer": "order-service",
  "correlationId": "checkout-7f3c",
  "data": {"orderId":"9001","customerId":"C101","totalAmount":1250000}
}
```

`eventId` dùng deduplicate; `eventVersion` xác định contract; `occurredAt` là thời điểm business xảy ra; `correlationId` nối event cùng flow.

### Choreography

#### Cách các service phối hợp với nhau

Trong Choreography không có một service trung tâm đứng ra điều khiển toàn bộ quy trình. Mỗi service chỉ cần biết:

- Nó phải nhận thông báo từ service nào.
- Khi nhận thông báo thì thực hiện phần nghiệp vụ nào.
- Xử lý xong thì thông báo kết quả cho các service khác.
- Nếu thất bại thì phải thông báo lỗi để service đã thực hiện bước trước biết mà hoàn tác.

Xét quy trình đặt hàng gồm ba service chính:

- **Order Service** quản lý trạng thái đơn hàng.
- **Inventory Service** quản lý số lượng hàng và việc giữ hàng.
- **Payment Service** quản lý thanh toán và hoàn tiền.

Order Service không gọi lần lượt Inventory rồi Payment. Thay vào đó, mỗi service phát thông báo về kết quả công việc của mình; service quan tâm sẽ nhận thông báo đó và tiếp tục xử lý.

#### Luồng xử lý bình thường

Khi khách hàng tạo đơn, Order Service lưu đơn ở trạng thái đang xử lý rồi phát một thông báo cho biết có đơn hàng mới.

Inventory Service là service đăng ký nhận thông báo này. Sau khi nhận được, Inventory kiểm tra số lượng hàng và giữ hàng cho đơn.

Nếu giữ hàng thành công, Inventory phát thông báo cho biết phần hàng của đơn đã được giữ. Payment Service đăng ký nhận loại thông báo này, vì Payment chỉ được phép thanh toán sau khi Inventory đã giữ được hàng.

Payment thực hiện thanh toán. Khi thanh toán thành công, Payment phát thông báo kết quả. Order Service nhận thông báo đó và chuyển đơn sang trạng thái đã xác nhận.

Có thể mô tả bằng trách nhiệm của từng service:

```text
Order Service:
Phát thông báo có đơn mới
        |
        v
Inventory Service:
Nhận thông báo -> giữ hàng -> phát thông báo giữ hàng thành công
        |
        v
Payment Service:
Nhận thông báo -> thanh toán -> phát thông báo thanh toán thành công
        |
        v
Order Service:
Nhận thông báo -> xác nhận đơn
```

Điểm quan trọng là service phía trước không gọi trực tiếp service phía sau:

- Order không gọi Payment.
- Inventory không gọi Payment.
- Inventory chỉ thông báo kết quả giữ hàng.
- Payment tự đăng ký nhận kết quả đó và quyết định bắt đầu thanh toán.

Luồng được hình thành từ việc service nào đăng ký nhận thông báo của service nào.

#### Compensation

Compensation là một **nghiệp vụ mới để bù tác động của bước đã thành công**, không phải rollback hay xóa lịch sử. Ví dụ: Payment thất bại sau khi Inventory đã giữ hàng thì Inventory phải release reservation; đã thu tiền thì Payment hoàn tiền; đã tạo vận đơn thì Shipping hủy vận đơn.

Nguyên tắc ownership: service nào sở hữu resource thì service đó bù resource của mình. Order chỉ theo dõi trạng thái chung, không tự cộng stock hay hoàn tiền thay service khác.

| Resource cần bù | Service thực hiện | Hành động |
| --- | --- | --- |
| Reservation | Inventory | Trả hàng |
| Payment | Payment | Hoàn tiền |
| Shipment | Shipping | Hủy vận đơn |

Trong Choreography, service gặp lỗi phát event; các service liên quan tự subscribe và phản ứng:

```text
PaymentFailed
  -> Inventory: release hàng
  -> Order: CANCELLING

InventoryReleased
  -> Order: CANCELLED
```

Order chỉ chuyển sang `CANCELLED` sau khi mọi compensation bắt buộc hoàn tất. Trong lúc chờ, giữ trạng thái `CANCELLING`.

Nếu nhiều bước cần bù, có thể:

- Chạy **tuần tự** khi business yêu cầu thứ tự, thường theo chiều ngược với flow đã chạy.
- Chạy **song song** khi các hành động độc lập; Order phải lưu và chờ đủ kết quả.

Có thể để từng service nghe trực tiếp failure event, hoặc để Order phát một event chung như `OrderCancellationRequested`. Cách thứ hai giảm số loại lỗi downstream mà mỗi service phải biết; nhưng nếu Order điều khiển chi tiết thứ tự và chờ từng bước, nó đã trở thành orchestrator.

Các yêu cầu bắt buộc:

- **Idempotent:** duplicate event không được hoàn tiền hay cộng stock hai lần; chỉ cho phép transition hợp lệ như `RESERVED -> RELEASED`.
- **Retry và phục hồi:** compensation lỗi thì retry với backoff, sau đó DLT/alert/replay hoặc `MANUAL_REVIEW`; không đánh dấu hoàn tất chỉ vì đã gửi message.
- **Xử lý timeout thận trọng:** timeout là kết quả chưa rõ, đặc biệt với Payment; cần reconcile bằng business key/idempotency key trước khi retry hoặc hoàn tiền.
- **Chặn vòng lặp:** event có ý nghĩa rõ, không phát lại do duplicate, từ chối transition sai và dùng `correlationId` để trace.

Khi số compensation, deadline và ràng buộc thứ tự tăng cao, nên chuyển critical flow sang Orchestration.

#### Quan sát luồng Choreography

Do không có coordinator, cần biết rõ:

- Service nào phát từng loại thông báo.
- Service nào consume.
- Service nào compensation resource nào.
- Order đang chờ compensation nào.
- Message nào đang retry hoặc nằm trong Dead-Letter Topic.

Một bảng ownership có thể dùng khi thiết kế:

| Tình huống | Service phát | Service consume | Hành động |
| --- | --- | --- | --- |
| Có đơn mới | Order | Inventory | Giữ hàng |
| Giữ hàng thành công | Inventory | Payment | Thanh toán |
| Thanh toán thành công | Payment | Order | Xác nhận đơn |
| Thanh toán thất bại | Payment | Inventory, Order | Trả hàng, bắt đầu hủy |
| Shipping thất bại | Shipping | Payment, Inventory, Order | Hoàn tiền, trả hàng, theo dõi hủy |
| Hoàn tiền xong | Payment | Order | Đánh dấu phần payment đã bù |
| Trả hàng xong | Inventory | Order | Đánh dấu phần inventory đã bù |

Bảng này quan trọng hơn việc chỉ vẽ một chuỗi tên event, vì nó thể hiện rõ ownership và trách nhiệm.

#### Khi nào Choreography bắt đầu quá phức tạp?

Choreography phù hợp khi:

- Flow ngắn.
- Service phản ứng tương đối độc lập.
- Compensation ít và không cần thứ tự phức tạp.
- Không cần một nơi trung tâm quản lý workflow.

Nên cân nhắc Orchestration khi:

- Có nhiều bước bắt buộc.
- Compensation phải chạy theo thứ tự.
- Order phải chờ nhiều kết quả bù.
- Có nhiều timeout và retry policy.
- Có manual review.
- Không còn rõ service nào chịu trách nhiệm kết thúc flow.
- Muốn biết chính xác workflow đang ở bước nào từ một nơi duy nhất.

Quy tắc thực tế:

```text
Flow ngắn, phản ứng độc lập
  -> Choreography

Flow dài, nhiều nhánh, timeout và compensation
  -> Orchestration

Critical path phức tạp nhưng có nhiều side effect độc lập
  -> Orchestration cho critical path
  -> Choreography cho side effect
```

### Orchestration

Orchestration dùng một **orchestrator** để quyết định bước tiếp theo của workflow. Participant vẫn sở hữu nghiệp vụ và dữ liệu của mình; orchestrator chỉ gửi command, nhận kết quả và lưu tiến độ.

```text
OrderCreated
  -> Orchestrator gửi ReserveInventory
  <- InventoryReserved
  -> Orchestrator gửi ChargePayment
  <- PaymentSucceeded
  -> Orchestrator gửi CreateShipment
  <- ShipmentCreated
  -> COMPLETED
```

Khác với Choreography, Inventory không cần biết Payment hay Shipping tồn tại. Nó chỉ xử lý command thuộc domain của mình và trả kết quả.

Orchestrator cần lưu bền vững một lượng state tối thiểu cho mỗi Saga:

- Workflow đang ở bước nào và đang chờ kết quả gì.
- Những bước nào đã thành công để biết cần compensation gì.
- Deadline, số lần retry và version để chống cập nhật đồng thời.
- Trạng thái kết thúc như `COMPLETED`, `COMPENSATED` hoặc `MANUAL_REVIEW`.

Khi một bước lỗi, orchestrator chỉ gửi compensation command cho những bước thực sự đã thành công:

```text
InventoryReserved
PaymentSucceeded
ShipmentFailed
  -> RefundPayment
  -> ReleaseInventory
  -> OrderCancelled
```

Thứ tự compensation do business quyết định, thường ngược với flow đã chạy. Orchestrator phải chờ kết quả thực tế; gửi `RefundPayment` chưa có nghĩa tiền đã được hoàn.

Các yêu cầu quan trọng:

- **Durable state:** restart vẫn tiếp tục được workflow đang chạy.
- **Idempotency:** retry command không được charge, refund hay release hai lần.
- **Timeout và reconciliation:** timeout là trạng thái chưa rõ; phải kiểm tra kết quả bằng business key trước khi retry hoặc compensation.
- **Concurrency:** dùng partition theo Saga ID, optimistic locking hoặc cơ chế tương đương để tránh hai nhánh cùng cập nhật sai state.
- **Failure recovery:** retry với backoff; quá SLA thì alert, DLT hoặc chuyển `MANUAL_REVIEW`.
- **Atomicity:** cập nhật Saga state và phát command cần Outbox hoặc cơ chế transaction phù hợp.

Orchestrator không nên chứa domain logic, đọc database của participant hoặc tự thực hiện payment/inventory. Nếu nó làm mọi việc, hệ thống sẽ thành một God Service. Orchestration cũng không bắt buộc gọi HTTP đồng bộ; command và result có thể truyền bất đồng bộ qua broker.

Nên dùng Orchestration khi flow có nhiều bước bắt buộc, nhiều nhánh, timeout, compensation theo thứ tự hoặc cần biết chính xác workflow đang ở đâu. Với reaction độc lập như Notification, Analytics hay Loyalty, Choreography thường gọn hơn.

### So sánh tổng hợp

| Tiêu chí | Choreography | Orchestration |
| --- | --- | --- |
| Ai quyết định bước tiếp? | Consumer phản ứng với event | Orchestrator |
| Flow nằm ở đâu? | Phân tán qua subscription | State machine/workflow tập trung |
| Message thường dùng | Event | Command + result/event |
| Fan-out | Rất tự nhiên | Thường phát event ra ngoài flow |
| Timeout | Cần owner/timer riêng | Orchestrator quản lý |
| Compensation | Phân tán | Điều phối tập trung |
| Quan sát tiến độ | Tổng hợp event/trace | Đọc Saga state |
| Flow ngắn | Phù hợp | Có thể dư thừa |
| Flow dài, nhiều nhánh | Dễ rối | Phù hợp hơn |
| Nguy cơ | Dependency ẩn, event spaghetti | God Orchestrator |
| Thêm reaction độc lập | Ít sửa producer | Không cần đưa vào orchestrator |
| Thay đổi trình tự bắt buộc | Sửa nhiều consumer | Chủ yếu sửa workflow |

### Có thể kết hợp cả hai không?

Có, và đây thường là cách thực tế nhất.

Dùng orchestration cho **critical path**:

```text
Order Saga
  -> ReserveInventory
  -> ChargePayment
  -> CreateShipment
  -> OrderConfirmed
```

Dùng choreography cho **side effect**:

```text
OrderConfirmed
  ├-> Notification
  ├-> Analytics
  ├-> Loyalty
  ├-> Recommendation
  └-> Data Warehouse
```

Lợi ích:

- Critical path có state, timeout và compensation rõ.
- Side effect vẫn loose coupling và dễ mở rộng.
- Orchestrator không cần biết mọi consumer.
- Analytics lỗi không làm Saga thất bại.

### Quan hệ với Saga

**Saga** là pattern quản lý một business transaction trải qua nhiều local transaction.

Saga có thể được triển khai bằng:

```text
Choreography-based Saga
  -> participant phản ứng với event

Orchestration-based Saga
  -> orchestrator gửi command và theo dõi result
```

Saga không đồng nghĩa với orchestration. Điểm chung là không dùng một ACID transaction bao trùm mọi service; failure được xử lý bằng compensation.

### Event và Command trong hai mô hình

Choreography thiên về business event:

```text
OrderCreated
InventoryReserved
PaymentSucceeded
```

Orchestration thường kết hợp command và result:

```text
Command: ReserveInventory
Result:  InventoryReserved / InventoryRejected
```

Khác biệt semantic:

- Event nói: “Việc này đã xảy ra”.
- Command nói: “Hãy thử thực hiện việc này”.
- Command có target/capability cụ thể.
- Event có thể có nhiều consumer không biết trước.

### Observability cho cả hai mô hình

Mọi message trong cùng flow nên có:

```json
{
  "eventId": "E110",
  "correlationId": "order-9001",
  "causationId": "E100",
  "eventType": "InventoryReserved",
  "occurredAt": "2026-08-28T10:00:10Z"
}
```

- `eventId`: nhận diện message và deduplicate.
- `correlationId`: nối toàn bộ flow của order.
- `causationId`: message nào tạo ra message hiện tại.
- `occurredAt`: thời điểm business event xảy ra.

Cần theo dõi:

- Workflow/Saga đang ở state nào.
- Event cuối cùng của mỗi order.
- Processing latency từng bước.
- Timeout và retry count.
- Consumer lag.
- Compensation đang pending.
- Event nằm trong DLT.
- Workflow quá SLA.

Choreography cần event map và trace tốt hơn vì không có state trung tâm. Orchestration cần dashboard Saga/workflow và alert cho state bị kẹt.

### Cách lựa chọn từng bước

Đặt các câu hỏi sau:

1. Flow có bao nhiêu bước bắt buộc?
2. Có nhiều nhánh hoặc điều kiện business không?
3. Có deadline dài hạn không?
4. Một bước lỗi có cần bù các bước trước không?
5. Có cần biết chính xác flow đang ở đâu không?
6. Có manual review không?
7. Reaction có độc lập với kết quả chính không?
8. Thêm consumer mới có thường xuyên không?

Gợi ý:

```text
Reaction độc lập, fan-out
  -> ưu tiên Choreography

Flow bắt buộc, dài, nhiều nhánh/timeout/compensation
  -> cân nhắc Orchestration

Critical path phức tạp + nhiều side effect độc lập
  -> kết hợp cả hai
```

### Checklist thiết kế Choreography

- Event có business meaning rõ không?
- Event owner là team/domain nào?
- Consumer nào nghe từng event?
- Có event cycle không?
- Ai sở hữu timeout?
- Failure event là gì?
- Compensation do service nào kích hoạt?
- Làm sao biết flow bị kẹt?
- Có `correlationId` và tracing không?
- Consumer có idempotent không?
- Schema thay đổi được kiểm soát không?
- Replay có chạy lại side effect không?

### Checklist thiết kế Orchestration

- State machine có được vẽ rõ không?
- Mỗi state chờ message nào?
- Deadline của từng bước là gì?
- Command có idempotency key không?
- State và command publication có atomic không?
- Duplicate/out-of-order được xử lý thế nào?
- Compensation theo thứ tự nào?
- Compensation thất bại thì retry/manual review ra sao?
- Orchestrator restart có resume được không?
- Nhiều instance update cùng Saga được bảo vệ thế nào?
- Workflow version mới xử lý instance cũ ra sao?
- Orchestrator có lấn sang domain logic không?
- Side effect độc lập có thể chuyển sang choreography không?

### Kết luận ngắn

```text
Choreography:
“Có việc X đã xảy ra; service nào quan tâm thì tự phản ứng.”

Orchestration:
“Workflow đang ở bước X; service Y hãy làm việc Z,
sau đó trả kết quả để tôi quyết định bước tiếp theo.”
```

Choreography tối ưu cho autonomy, fan-out và reaction độc lập. Orchestration tối ưu cho visibility, trình tự bắt buộc, timeout và compensation. Một hệ thống tốt không cố dùng duy nhất một mô hình; nó chọn mô hình theo tính chất của từng business flow.

### Các kiểu sử dụng event

#### 1. Event Notification

Event chỉ báo rằng việc gì đó đã xảy ra:

```json
{"eventType":"OrderCreated","orderId":"9001"}
```

Consumer phải gọi lại producer để lấy chi tiết:

```text
OrderCreated(9001)
  -> Notification
      -> GET /orders/9001
      -> gửi email
```

Ưu điểm: payload nhỏ, ít phát tán dữ liệu nhạy cảm, producer vẫn là nguồn chính.

Nhược điểm: consumer lại phụ thuộc runtime vào producer; nhiều consumer có thể làm API nguồn quá tải; dữ liệu đọc được có thể khác thời điểm event; replay event cũ có thể không lấy được state lịch sử.

#### 2. Event-Carried State Transfer

Event mang đủ dữ liệu để consumer xử lý:

```json
{
  "eventType":"OrderCreated",
  "orderId":"9001",
  "customerId":"C101",
  "items":[{"productId":"P10","quantity":2}],
  "totalAmount":1250000
}
```

Consumer không phải gọi lại Order Service, có thể xử lý khi producer downtime và replay đúng state lúc event xảy ra.

Đổi lại payload lớn hơn, dữ liệu bị sao chép, có thể stale, schema phức tạp và phải quản lý PII. Không nên nhét toàn bộ database row vào event “cho chắc”; chỉ mang dữ liệu thực sự cần.

#### 3. Event Sourcing

CRUD thường chỉ giữ state cuối:

```text
account.balance = 800.000
```

Event Sourcing giữ chuỗi event tạo nên state:

```text
AccountOpened(0)
MoneyDeposited(+1.000.000)
MoneyWithdrawn(-200.000)
--------------------------
Balance = 800.000
```

Ưu điểm: audit trail, biết nguyên nhân thay đổi, dựng projection mới, xem state quá khứ.

Chi phí: event cũ phải đọc được lâu dài; replay có thể chậm nên cần snapshot; side effect không được chạy lại; sửa event sai và xóa PII khó.

> Kafka có log và replay nhưng không tự biến application thành Event Sourcing. Đây là quyết định thiết kế domain và cách dựng state.

#### 4. CQRS và Materialized View

CQRS tách write model và read model:

```text
Order Service ghi order
  -> OrderCreated / OrderUpdated
      ├-> Elasticsearch: Order Search
      ├-> Customer Order History
      └-> Sales Dashboard
```

Consumer tạo **materialized view** đã join/tính sẵn để query nhanh. Đánh đổi là eventual consistency:

```text
10:00:00.000 Order cập nhật SHIPPED
10:00:00.050 publish OrderShipped
10:00:00.200 consumer cập nhật Search
10:00:00.250 UI mới thấy SHIPPED
```

Trong 250 ms, write model và read model khác nhau. Hệ thống phải xác định độ trễ chấp nhận được.

### Use case thực tế: quy trình đặt hàng

#### Bước 1. Tạo order

```text
Client -> POST /orders
Order Service:
  1. validate
  2. tính tổng tiền
  3. lưu order=PENDING
  4. publish OrderCreated
  5. trả orderId=9001
```

#### Bước 2. Giữ hàng

```text
Đủ hàng:
InventoryReserved(orderId=9001, reservationId=R500)

Thiếu hàng:
InventoryRejected(orderId=9001, reason=OUT_OF_STOCK)
```

Reservation cần deadline, ví dụ 15 phút, vì không thể giữ hàng mãi khi khách chưa thanh toán.

#### Bước 3. Thanh toán

```text
InventoryReserved
  -> ChargePayment(9001, 1.250.000)
      -> PaymentSucceeded
      hoặc PaymentFailed
```

Payment phải idempotent để retry không charge hai lần.

#### Bước 4. Hoàn tất hoặc compensation

Thành công:

```text
InventoryReserved + PaymentSucceeded
  -> OrderConfirmed
  -> CreateShipment
  -> ShipmentCreated
```

Thanh toán thất bại:

```text
InventoryReserved + PaymentFailed
  -> ReleaseInventory
  -> InventoryReleased
  -> OrderCancelled
```

Đã thanh toán nhưng không giữ được hàng:

```text
PaymentSucceeded + InventoryRejected
  -> RefundPayment
  -> PaymentRefunded
  -> OrderCancelled
```

Đây là **Saga**: business transaction lớn được chia thành local transaction. Khi bước sau thất bại, hệ thống chạy **compensation**.

Compensation không phải rollback tuyệt đối:

- Refund là transaction mới, không xóa payment cũ.
- Release inventory là state change mới.
- Email đã gửi không thể thực sự thu hồi.
- Compensation cũng có thể thất bại và cần retry/manual review.

#### Bước 5. Fan-out

```text
OrderConfirmed
  ├-> Notification gửi email/push
  ├-> Loyalty cộng điểm
  ├-> Analytics cập nhật doanh thu
  ├-> Recommendation học sở thích
  └-> Data Platform ghi Warehouse
```

Mỗi consumer độc lập. Notification lỗi không làm Analytics dừng.

#### Trạng thái user nhìn thấy

| Trạng thái | Ý nghĩa |
| --- | --- |
| `PENDING` | Đã nhận đơn, đang xử lý |
| `RESERVING_INVENTORY` | Đang giữ hàng |
| `PAYMENT_PROCESSING` | Đang thanh toán |
| `CONFIRMED` | Đơn được chấp nhận |
| `CANCELLED` | Đơn thất bại/đã hoàn tác |
| `MANUAL_REVIEW` | Cần nhân viên xử lý |

Không nên trả “thành công” khi mới nhận event. Có thể trả `202 Accepted` hoặc order `PENDING`, rồi client polling/WebSocket/SSE để nhận kết quả cuối.

### Những use case phù hợp với EDA

#### Notification và side effect

```text
PasswordChanged
  ├-> gửi email cảnh báo
  ├-> revoke session
  └-> ghi audit log
```

#### Realtime analytics

```text
ProductViewed / AddedToCart / OrderConfirmed
  -> aggregate theo phút
  -> dashboard conversion
```

#### Change Data Capture

```text
PostgreSQL/MySQL -> CDC -> customer-changes
                           ├-> Elasticsearch
                           ├-> Cache
                           └-> Data Warehouse
```

CDC đồng bộ tốt nhưng database row change không phải lúc nào cũng có semantic rõ như domain event.

#### Fraud detection

```text
LoginAttempt + DeviceChanged + PaymentRequested
  -> join theo account
  -> risk score
  -> FraudSuspected
```

#### IoT và telemetry

Hàng nghìn thiết bị gửi nhiệt độ, vị trí, metric; consumer lưu lịch sử, phát hiện bất thường hoặc cập nhật dashboard.

#### Streaming ETL

```text
Raw event -> validate -> loại lỗi -> enrich -> chuẩn hóa -> Data Lake
```

### Ưu điểm của EDA

- **Loose coupling**: producer phụ thuộc event contract, không biết implementation của mọi consumer.
- **Scale độc lập**: Payment, Notification và Analytics scale theo capacity riêng.
- **Cô lập lỗi**: consumer lỗi không nhất thiết làm producer lỗi; có thể đọc backlog sau.
- **Buffer traffic spike**: broker giữ backlog trong giới hạn storage/retention.
- **Fan-out**: nhiều hệ thống dùng cùng business fact.
- **Replay/backfill**: rebuild index, sửa projection, tạo pipeline mới.

Replay phải chặn external side effect; không được gửi lại email hoặc charge lại tiền.

### Nhược điểm và chi phí

- **Eventual consistency**: service cập nhật khác thời điểm.
- **Debug khó**: flow không nằm trong một call stack.
- **Duplicate/out-of-order**: consumer phải idempotent và xử lý ordering.
- **Schema evolution**: producer/consumer deploy độc lập.
- **Distributed transaction**: cần Saga, compensation, Outbox.
- **Vận hành**: broker, disk, replication, lag, retry, DLT, ACL.
- **Kiểm thử**: phải test duplicate, event muộn, crash, replay, compensation lỗi.

> EDA không tự động làm hệ thống nhanh hơn. Nó giảm thời gian caller chờ và tăng khả năng scale, nhưng toàn bộ business flow có thể hoàn tất chậm hơn.

### Những bài toán bắt buộc phải thiết kế

#### 1. Dual-write problem

```java
orderRepository.save(order);    // database
eventPublisher.publish(event);  // broker
```

Hai lỗi nguy hiểm:

```text
DB commit + publish lỗi -> có order, không có OrderCreated
Publish xong + DB rollback -> consumer thấy order không tồn tại
```

`try/catch` không thể tạo atomicity giữa hai hệ thống.

#### 2. Transactional Outbox

```sql
BEGIN;
INSERT INTO orders(id, status) VALUES ('9001', 'PENDING');
INSERT INTO outbox_events(event_id, event_type, payload)
VALUES ('E100', 'OrderCreated', '{...}');
COMMIT;
```

Relay/CDC đọc Outbox rồi publish:

```text
orders + outbox_events -> Relay/CDC -> Kafka
```

Rollback thì cả order và outbox mất; commit thì event còn trong outbox để retry. Relay có thể publish lặp nếu crash sau publish, nên Outbox không loại bỏ duplicate.

#### 3. Idempotency

Idempotent nghĩa là xử lý cùng input nhiều lần vẫn có kết quả business tương đương một lần.

```text
PaymentSucceeded(E200)
PaymentSucceeded(E200)  // duplicate
```

Consumer lưu `eventId` với unique constraint và cập nhật state trong cùng transaction. Có thể dùng business key như `orderId + paymentAttempt` khi hai event khác ID nhưng cùng một hành động.

#### 4. Ordering

```text
Mong muốn: OrderCreated(v1) -> OrderPaid(v2) -> OrderShipped(v3)
Có thể gặp: OrderPaid(v2) -> OrderCreated(v1) -> OrderShipped(v3)
```

Cách xử lý:

- Route event cùng entity theo `orderId`.
- Mang `aggregateVersion`.
- Chỉ áp dụng version hợp lệ.
- Buffer/retry event đến sớm.
- Dùng state machine từ chối transition sai.

Không yêu cầu total order nếu chỉ cần order theo từng order/account vì sẽ giảm parallelism.

#### 5. Retry và Dead-Letter Topic

| Lỗi | Ví dụ | Xử lý |
| --- | --- | --- |
| Tạm thời | timeout, `503` | Retry với backoff |
| Quá tải | `429` | Backoff dài, rate limit |
| Dữ liệu lỗi | thiếu field | Không retry vô hạn |
| Business reject | hết hàng | Phát failure event |
| Bug | exception | Alert, sửa, replay |

```text
order-events -> retry-1m -> retry-10m -> order-events-dlt
```

DLT cần owner, alert, retention, lý do lỗi, metadata gốc và công cụ replay; nó không phải thùng rác.

#### 6. Schema evolution

Đổi `amount` từ number thành object có thể phá consumer cũ. Quy tắc thường dùng:

- Thêm field optional.
- Không đổi type/semantic field cũ.
- Không xóa field khi consumer còn dùng.
- Breaking change cần version/topic mới.
- Schema Registry và CI kiểm tra compatibility.

#### 7. Observability

```text
correlationId=checkout-7f3c
  -> OrderCreated E100
  -> InventoryReserved E110
  -> PaymentFailed E120
  -> InventoryReleased E130
  -> OrderCancelled E140
```

Cần `eventId`, `correlationId`, `causationId`, event type/version/producer/time; theo dõi produce rate, consume rate, latency, error, retry, consumer lag, DLT và event quá SLA.

#### 8. Ownership, Security và Governance

Domain tạo business fact nên sở hữu semantic và compatibility của event.

- Không publish password, token, secret.
- Chỉ gửi PII consumer thực sự cần.
- Topic có ACL.
- Mã hóa khi truyền/lưu.
- Retention phù hợp mục đích và quy định.
- Có chiến lược xóa/ẩn danh dữ liệu cá nhân.

Immutable về kỹ thuật không có nghĩa được phép giữ PII mãi mãi.

### Khi nào không nên dùng EDA?

- Caller cần kết quả ngay: HTTP/gRPC tự nhiên hơn.
- CRUD nhỏ, ít dependency: local transaction/background job có thể đủ.
- Hai thay đổi trong cùng database cần atomic: dùng local transaction.
- Team chưa có idempotency, schema governance, tracing và runbook.
- Priority, per-message delay hoặc routing phức tạp là yêu cầu chính: task queue/message broker khác có thể phù hợp hơn.

Kiến trúc thực tế thường hybrid:

```text
Synchronous:
  - query
  - validation tức thời
  - command cần kết quả ngay

Asynchronous:
  - side effect
  - fan-out
  - workflow dài
  - data integration
  - streaming analytics
```

### Checklist trước khi chọn EDA

1. Business fact nào thực sự là event?
2. Domain nào sở hữu event?
3. Consumer cần notification hay state đầy đủ?
4. Eventual consistency tối đa bao lâu?
5. Ordering cần theo order/customer/account hay toàn cục?
6. Xử lý duplicate thế nào?
7. DB change và publish đồng bộ bằng cách nào?
8. Lỗi nào retry, lỗi nào vào DLT?
9. Schema version và compatibility ra sao?
10. Có cần replay, giữ event bao lâu?
11. Replay có chạy lại email/payment không?
12. Metric, trace, alert và runbook là gì?
13. Event chứa PII nào và ai được đọc?
14. Broker/consumer downtime nhiều giờ thì phục hồi thế nào?

### Từ Event-Driven Architecture đến Kafka

Sau khi hiểu EDA, câu hỏi là hạ tầng nào có thể vận chuyển và lưu event khi quy mô tăng:

```text
- Nhiều producer ghi liên tục
- Nhiều ứng dụng đọc cùng event độc lập
- Throughput cao
- Lưu event bền vững
- Consumer downtime rồi đọc tiếp
- Replay dữ liệu cũ
- Scale ngang và chịu lỗi
- Giữ ordering theo entity
```

Kafka được thiết kế cho nhóm nhu cầu này.

#### Ánh xạ EDA sang Kafka

| Nhu cầu EDA | Kafka | Giải thích |
| --- | --- | --- |
| Application phát event | **Producer** | Ghi record vào Kafka |
| Luồng event có tên | **Topic** | Nơi logic chứa dòng event |
| Parallelism/ordering theo key | **Partition** | Topic chia thành ordered log |
| Vị trí record | **Offset** | Số thứ tự trong partition |
| Application đọc | **Consumer** | Fetch record |
| Instance chia việc | **Consumer Group** | Partition được chia trong group |
| Nhiều hệ thống đọc độc lập | **Nhiều group** | Mỗi group có offset riêng |
| Lưu/replay | **Retention** | Giữ record theo time/size |
| Chịu lỗi | **Replication** | Có bản sao trên broker khác |

#### Topic là event stream có tên

```text
Topic: order-events
OrderCreated -> InventoryReserved -> PaymentSucceeded -> OrderConfirmed -> ...
```

Topic là log nhận record liên tục, không phải queue xóa record ngay khi một consumer đọc.

#### Partition tạo parallelism và ordering theo key

```text
order-events
  Partition 0: order 100, 103, 106
  Partition 1: order 101, 104, 107
  Partition 2: order 102, 105, 108
```

Dùng `orderId` làm key giúp event cùng order vào cùng partition:

```text
key=9001: OrderCreated -> PaymentSucceeded -> OrderShipped
```

Kafka giữ order trong partition, không có total order giữa mọi partition. Đây là đánh đổi để scale.

#### Consumer Group phân phối event

Trong cùng group, instance chia partition:

```text
payment-service:
P1 <- Partition 0
P2 <- Partition 1
P3 <- Partition 2
```

Ba partition và năm consumer nghĩa là hai consumer idle.

Các group khác nhau đọc độc lập:

```text
order-events
  ├-> payment-service group
  ├-> inventory-service group
  ├-> notification-service group
  └-> analytics-service group
```

Payment đọc không làm Analytics mất event.

#### Offset là bookmark

```text
offset    0    1    2    3
record   [A]  [B]  [C]  [D]
                    ^
              group đã commit
```

Consumer restart đọc tiếp từ committed offset; reset offset cho phép replay nếu record còn retention.

Offset không chứng minh external side effect đã hoàn tất. Consumer charge tiền rồi crash trước commit có thể đọc lại record; Payment vẫn phải idempotent.

#### Retention tạo buffer và replay

Kafka không xóa record chỉ vì đã consume:

```text
retention = 7 ngày
Payment đã đọc     -> record vẫn còn
Analytics đọc chậm -> đọc sau
Consumer mới       -> replay lịch sử
```

Consumer lag quá retention có thể mất phần record cũ. Retention phải dựa trên downtime, replay, storage và compliance.

#### Replication tạo khả năng chịu lỗi

```text
Partition 0:
Leader   -> Broker 1
Follower -> Broker 2
Follower -> Broker 3
```

Leader lỗi thì replica đủ điều kiện có thể lên thay. Durability còn phụ thuộc replication factor, ISR, `acks`, `min.insync.replicas`.

#### Order flow trên Kafka

```text
Order Service -> OrderCreated(key=9001) -> order-events
                    ├-> inventory-service -> InventoryReserved
                    └-> analytics-service -> dashboard

InventoryReserved -> inventory-events
                    -> payment-service -> PaymentSucceeded

PaymentSucceeded -> payment-events
                    ├-> order-service -> CONFIRMED
                    └-> notification-service -> gửi thông báo
```

Thiết kế topic phải cân nhắc domain ownership, throughput, ordering, retention và ACL; không tạo tùy tiện một topic cho mọi event.

#### Kafka giải quyết gì?

- Lưu/phân phối event throughput cao.
- Nhiều consumer group đọc độc lập.
- Buffer backlog và theo dõi lag.
- Scale bằng partition.
- Replay bằng offset.
- Chịu lỗi bằng replication.
- CDC và stream processing.

Kafka không tự giải quyết:

- Event nào nên tồn tại và semantic.
- Dual-write database–Kafka.
- Consumer idempotency.
- Saga compensation.
- Schema governance.
- Exactly-once với mọi external API/database.
- Tracing, alert, security, business correctness.

```text
EDA trả lời:
Hệ thống phối hợp bằng business event như thế nào?

Kafka trả lời:
Event được ghi, lưu, chia partition, phân phối,
theo dõi offset và replay ở quy mô lớn thế nào?
```

Kafka là công cụ hiện thực một phần EDA, không phải bản thân kiến trúc. Phần `Kafka Concept` tiếp theo đi sâu vào producer, topic, partition, offset, consumer group, broker, replication và retention.

## Kafka Concept

🙂 Apache Kafka là một **distributed event streaming platform**. Nói ngắn gọn, Kafka cho phép nhiều hệ thống **publish, lưu trữ, đọc và xử lý một dòng event liên tục** với throughput cao, có thể scale ngang và chịu lỗi.

Kafka kết hợp ba khả năng chính:

1. **Publish/subscribe event**: producer ghi event, consumer đăng ký đọc event.
2. **Lưu event bền vững**: event được giữ trong topic theo retention policy, không bị xóa chỉ vì một consumer đã đọc xong.
3. **Xử lý event stream**: application có thể filter, join, aggregate, enrich hoặc phản ứng với event khi nó xuất hiện.

```text
Event sources              Kafka                         Event consumers
Order Service  ─┐      ┌─ orders ────────────────┐   ┌─ Payment Service
Payment DB/CDC ─┼────> │  partitioned event log  │ ─>├─ Fraud Detection
Mobile App     ─┘      └─────────────────────────┘   ├─ Data Warehouse
                                                    └─ Realtime Dashboard
```

Điểm cốt lõi là producer và consumer **không cần biết trực tiếp về nhau**. `Order Service` chỉ publish `OrderCreated`; Payment, Fraud, Analytics hoặc một consumer được thêm sau này có thể đọc event theo nhu cầu riêng.

### “Stream” có nghĩa là gì?

**Stream** là một dòng event xuất hiện nối tiếp theo thời gian. Khác với một dataset hữu hạn đã có sẵn, stream thường không có điểm kết thúc rõ ràng: khi hệ thống còn hoạt động thì event mới vẫn tiếp tục được sinh ra.

```text
thời gian ---------------------------------------------------------->

OrderCreated -> PaymentSucceeded -> OrderPacked -> OrderShipped -> ...
```

Ví dụ:

- Mỗi lần user click sản phẩm tạo một event trong click stream.
- Mỗi lần tài xế gửi vị trí tạo một event trong location stream.
- Mỗi thay đổi `INSERT/UPDATE/DELETE` trong database có thể trở thành một event trong CDC stream.
- Mỗi order mới hoặc lần đổi trạng thái order tạo event trong order stream.

Trong Kafka, stream không phải một object đơn lẻ nằm trọn trong memory. Nó là cách nhìn logic về chuỗi record được ghi liên tục vào topic. Topic có thể có nhiều partition nên Kafka chỉ đảm bảo thứ tự record trong từng partition, không có một thứ tự toàn cục cho toàn bộ stream.

Cần phân biệt:

- **Event stream**: dòng event liên tục, ví dụ các event trong topic `order-events`.
- **Event streaming**: toàn bộ việc capture, lưu trữ, vận chuyển và cung cấp dòng event cho các hệ thống khác.
- **Stream processing**: đọc dòng event và xử lý liên tục như filter, enrich, join, aggregate hoặc phát sinh event mới.

```text
Event stream đầu vào
  -> Stream processing
  -> Event stream đầu ra

orders.raw
  -> validate + enrich + aggregate
  -> orders.cleaned / sales-per-minute
```

Kafka được gọi là **event streaming platform** vì nó không chỉ chuyển message: Kafka còn lưu dòng event để consumer có thể xử lý ngay khi event xuất hiện hoặc đọc lại dữ liệu cũ còn trong retention. Chi tiết cách xử lý stream được trình bày tại mục `Stream Processing - Event Driven Architecture`; phần này chỉ giải thích ý nghĩa của chữ “stream”.

### Mental model tổng quan

Có thể hình dung Kafka như một **distributed append-only log**:

```text
Producer -> Topic -> Partitioned log -> Consumer group
```

- **Event/record**: ghi lại sự thật rằng một việc đã xảy ra, ví dụ `OrderCreated`, `PaymentSucceeded`, `ProductStockChanged`.
- **Producer**: application ghi event vào Kafka.
- **Topic**: dòng event có tên, ví dụ `order-events`.
- **Partition**: chia topic thành nhiều ordered log để lưu và xử lý song song.
- **Offset**: vị trí của record trong một partition.
- **Consumer/consumer group**: application đọc event; nhiều instance trong cùng group chia nhau các partition để scale.
- **Broker/replica**: broker lưu partition; replica cung cấp khả năng chịu lỗi khi broker gặp sự cố.

Mục này chỉ cung cấp mental model. Các cơ chế được giải thích chi tiết ở phần sau:

| Muốn tìm hiểu | Đọc mục |
| --- | --- |
| Broker, topic, partition, segment, offset, ordering và event key | `Broker + Topic + Partitions + Segment + Offset` |
| Cấu trúc event/message/record/data | `Event / Message / Record / Data` |
| Producer, serializer, partition strategy, ACK, retry, idempotence, compression | `Producer` |
| Consumer group, commit offset, rebalance, offset reset và consumer thread | `Consumer` |
| At-most-once, at-least-once và exactly-once | `Kafka Delivery Semantics` |
| Leader, follower, ISR và replication | `Kafka Replica` |
| Retention, segment deletion và log compaction | `Log Retention + Cleanup Policy` |
| Vì sao Kafka có throughput cao | `Why Kafka Fast?` |
| So sánh với message broker truyền thống | `RabbitMQ vs Kafka` |

> Lưu ý: partition là **ordered log ở góc nhìn logic**, không nên hiểu đơn giản là đúng một file vật lý. Trên disk, một partition gồm nhiều segment; xem chi tiết tại `Broker + Topic + Partitions + Segment + Offset`.

### Log-based khác queue truyền thống ở điểm nào?

Với queue truyền thống, message thường được broker theo dõi theo vòng đời giao nhận và được loại khỏi queue sau khi consumer xử lý/ACK theo cơ chế của broker. Với Kafka, record được giữ độc lập với việc đã có consumer đọc hay chưa; mỗi consumer group theo dõi vị trí đọc của riêng mình.

Điều này đem lại bốn đặc tính quan trọng:

- **Replay**: có thể đọc lại event còn trong retention để sửa bug, rebuild projection/search index hoặc backfill pipeline.
- **Fan-out**: nhiều consumer group đọc cùng topic độc lập; Payment đọc không làm Analytics mất event.
- **Decoupling**: producer không phải gọi trực tiếp mọi downstream; từng consumer có thể deploy và scale riêng.
- **Buffering**: consumer có thể xử lý chậm hơn producer trong một khoảng thời gian vì event đã được lưu trong Kafka.

Kafka không làm slow consumer biến mất. Nếu consumer lag quá lâu và record đã hết retention, phần dữ liệu đó có thể bị xóa trước khi consumer đọc tới. Cơ chế offset và retention được giải thích ở các mục `Consumer` và `Log Retention + Cleanup Policy`.

### Ví dụ thực tế: order flow

```text
Order Service
  -> publish OrderCreated(key=orderId:9001)
  -> Kafka topic: order-events
       -> Payment Service
       -> Fraud Service
       -> Notification Service
       -> Analytics Service
```

Nếu Analytics ngừng 30 phút, Payment vẫn có thể tiếp tục hoạt động. Khi Analytics chạy lại, nó đọc tiếp từ vị trí đã commit nếu dữ liệu vẫn còn trong retention. Nếu thêm Recommendation Service sau này, service mới chỉ cần subscribe topic; không phải sửa flow chính của Order Service.

Dùng `orderId` làm key thường giúp các event của cùng order đi vào cùng partition:

```text
OrderCreated -> PaymentSucceeded -> OrderPacked -> OrderShipped
```

Nhờ vậy có thể giữ thứ tự theo từng order. Kafka không đảm bảo total order giữa mọi partition. Cách chọn key và ảnh hưởng tới ordering được trình bày tại `Producer Message Key`, `Producer Partition Strategy` và `Event Key`.

### Kafka phù hợp với use case nào?

- Event-driven microservices và tích hợp bất đồng bộ giữa nhiều hệ thống.
- CDC: đưa thay đổi từ database thành event stream.
- Realtime analytics, monitoring và dashboard.
- Log, metric và user activity tracking tập trung.
- Fraud detection, recommendation, notification và IoT telemetry.
- Streaming ETL/ELT và đồng bộ dữ liệu sang Data Lake, Warehouse hoặc Elasticsearch.
- Event Sourcing khi application được thiết kế theo mô hình đó ngay từ đầu.

Hai ví dụ quen thuộc:

- Hệ thống xem phim có thể publish `MovieWatched`, `MoviePaused`, `SearchPerformed` để pipeline recommendation cập nhật gợi ý gần realtime.
- Hệ thống gọi xe có thể stream vị trí tài xế, trạng thái chuyến đi và payment event cho tracking, pricing, fraud và analytics.

Các ví dụ này mô tả kiểu bài toán Kafka phù hợp, không có nghĩa Kafka tự thực hiện thuật toán recommendation, tìm đường hoặc tính giá. Business logic vẫn nằm ở stream processor hoặc service phía consumer.

### Kafka không tự giải quyết điều gì?

- Kafka không thay thế relational database cho CRUD, truy vấn ad-hoc, join tùy ý hoặc transaction business thông thường.
- Kafka không tự đảm bảo end-to-end exactly-once với mọi database/API bên ngoài; xem `Kafka Delivery Semantics`.
- Kafka không tự tạo schema/data contract đúng hoặc ngăn producer phát event sai business.
- Kafka không phải lựa chọn tự nhiên nhất cho mọi task queue cần priority, per-message delay, complex routing hoặc request/reply đơn giản.
- Kafka không tự đem lại Event Sourcing. Event Sourcing là cách thiết kế domain; Kafka chỉ có thể là một thành phần của kiến trúc đó.
- Kafka không nên được coi là backup duy nhất của database nếu retention/compaction không giữ đủ dữ liệu để phục hồi.

### Đánh đổi cần nhớ

- Lưu record sau khi consume giúp replay và fan-out nhưng tốn storage, replication traffic và chi phí vận hành.
- Partition giúp scale nhưng đổi lại chỉ có ordering trong partition; chọn sai key có thể gây hot partition hoặc sai thứ tự theo entity.
- Consumer linh hoạt về vị trí đọc nhưng application phải xử lý duplicate, retry, rebalance, poison message và consumer lag.
- Decoupling giúp hệ thống dễ mở rộng nhưng tăng eventual consistency và làm tracing/debug phức tạp hơn synchronous call.
- Production cần theo dõi broker health, disk, under-replicated partition, throughput, consumer lag và schema compatibility.

Chi tiết của từng đánh đổi được khai thác tại các mục kỹ thuật tương ứng ở phía dưới; `Kafka Concept` chỉ đóng vai trò bản đồ tổng quan.

### Tư duy chọn nhanh

- Cần một event được nhiều hệ thống độc lập đọc: Kafka phù hợp.
- Cần throughput cao, lưu event và replay: Kafka phù hợp.
- Chỉ cần giao một job đơn giản cho một worker, trong khi routing/delay/priority quan trọng hơn replay: nên đánh giá message broker/task queue khác.
- Cần request-response tức thời và caller phải nhận kết quả ngay: HTTP/gRPC thường tự nhiên hơn; Kafka có thể xử lý các side effect bất đồng bộ.
- Chưa có retention, data contract và idempotency rõ ràng: chưa nên đưa pipeline lên production chỉ vì đã có Kafka.

### Đánh giá lại ghi chú cũ

- Đúng: Kafka là log-based event streaming platform; partition là ordered append-only log ở góc nhìn logic; offset cho phép consumer theo dõi vị trí và replay; nhiều consumer group có thể đọc độc lập.
- Cần sửa: partition không chỉ là một file vật lý mà gồm nhiều segment; Kafka chỉ đảm bảo ordering trong partition; slow consumer vẫn có nguy cơ mất dữ liệu đã hết retention.
- Cần bỏ cách nói tuyệt đối: sequential disk I/O có lợi cho throughput nhưng không nên khẳng định chung rằng nó “nhanh hơn random write trong RAM hàng nghìn lần”. Các yếu tố hiệu năng được phân tích riêng tại `Why Kafka Fast?`.
- Cần nói cẩn trọng hơn: replay có thể rebuild projection/search index hoặc state nếu giữ đủ event và transform deterministic; nó không mặc nhiên phục hồi được database hay biến hệ thống thành Event Sourcing.

## Why is Kafka fast?

Kafka nhanh nhờ nhiều cơ chế phối hợp với nhau, không phải chỉ vì “ghi tuần tự xuống disk” hoặc “dùng zero-copy”. Nói chính xác hơn: Kafka được thiết kế để đạt **throughput cao** bằng cách biến nhiều record nhỏ thành các luồng dữ liệu lớn, liên tục và có thể xử lý song song.

Các yếu tố chính:

1. Append-only log và sequential I/O.
2. Tận dụng OS page cache.
3. Batching ở nhiều tầng.
4. Compression theo record batch.
5. Zero-copy trên đường đọc phù hợp.
6. Partitioning để xử lý song song trên nhiều broker/consumer.
7. Pull model và fetch theo batch.
8. Ít trạng thái giao nhận riêng cho từng consumer trên broker.

> “Fast” trong Kafka chủ yếu nói về **tổng lượng dữ liệu xử lý trong một đơn vị thời gian**. Một cấu hình tối ưu throughput có thể cố ý chờ thêm vài mili giây để gom batch, nên không đồng nghĩa mọi record luôn có latency thấp nhất.

### 1. Append-only log và sequential I/O

Record mới được append vào cuối active segment của partition. Kafka không phải tìm một vị trí ngẫu nhiên trên disk cho từng record giống workload random update.

```text
Partition log

[record 0][record 1][record 2][record 3] ---> append record mới
```

Sequential access có lợi vì:

- Ít disk seek hơn, đặc biệt rõ trên HDD.
- Các write nhỏ có thể được gom thành write lớn và liên tục.
- Read thường đi tuần tự từ offset hiện tại của consumer.
- OS có thể read-ahead và cache các page được truy cập gần đây.

SSD làm chênh lệch sequential/random I/O nhỏ hơn HDD, nhưng contiguous I/O, batching và ít system call vẫn có lợi. Không nên hiểu rằng disk luôn nhanh hơn RAM; ý đúng là **sequential disk access có thể rất hiệu quả**, còn random memory access có thể gặp cache miss và pattern truy cập kém.

Chi tiết cách partition được chia thành segment nằm tại `Broker + Topic + Partitions + Segment + Offset`.

### 2. Kafka tận dụng OS page cache

Kafka chủ yếu dựa vào **page cache của operating system** thay vì tự xây một object cache lớn trong JVM.

Khi broker ghi vào file:

```text
Kafka broker
  -> write system call
  -> OS page cache
  -> kernel flush xuống disk theo cơ chế của OS/filesystem
```

Khi consumer đọc dữ liệu vừa được ghi, các page tương ứng **thường** vẫn còn trong page cache:

```text
Consumer fetch
  <- dữ liệu từ page cache
  <- không nhất thiết phải đọc physical disk ở lần fetch đó
```

Tác dụng:

- Tránh giữ thêm một bản cache lớn dưới dạng Java object/byte array trong heap.
- Giảm áp lực Garbage Collection.
- OS tự dùng phần RAM còn trống làm cache và thu hồi khi hệ thống cần memory.
- Dữ liệu cache có thể tiếp tục tồn tại sau khi Kafka process restart, miễn OS chưa reboot hoặc reclaim page.

> Không nên viết “consumer đọc dữ liệu mới thì chắc chắn lấy từ RAM”. Page cache phụ thuộc memory pressure, working set và trạng thái hệ thống; dữ liệu có thể đã bị evict và phải đọc lại từ storage.

Page cache cũng không đồng nghĩa record đã an toàn tuyệt đối trước power loss. Durability còn phụ thuộc replication, `acks`, ISR và cấu hình filesystem/broker; xem `Producer ACK` và `Kafka Replica`.

### 3. Batching giảm system call và network round-trip

Nếu gửi 1.000 record bằng 1.000 request riêng:

```text
1 record -> 1 request -> 1 network round-trip -> 1 lần xử lý nhỏ
```

thì phần overhead của request header, syscall, network packet và broker processing có thể lớn hơn chính payload.

Kafka cố gắng xử lý theo batch:

```text
1.000 records
  -> gom thành các record batch theo partition
  -> ít produce request hơn
  -> broker append các block lớn hơn
  -> consumer fetch nhiều record trong một response
```

Batching xuất hiện ở nhiều tầng:

- Producer gom record theo partition trước khi gửi.
- Một produce request có thể chứa nhiều batch.
- Broker ghi và replicate dữ liệu theo block/batch hiệu quả hơn.
- Consumer fetch một vùng dữ liệu thay vì yêu cầu từng record.

Tác dụng:

- Ít request và system call hơn.
- Network packet lớn và hiệu quả hơn.
- Sequential disk operation lớn hơn.
- Compression tốt hơn vì có nhiều dữ liệu giống nhau trong cùng batch.

Đánh đổi là **latency vs throughput**. Chờ lâu hơn giúp batch đầy hơn nhưng một record có thể phải đợi trước khi được gửi. Các cấu hình liên quan như `batch.size`, `linger.ms` và `buffer.memory` được khai thác tại `High Load Producer`.

### 4. Compression theo batch

Các event cùng loại thường lặp lại field name và nhiều giá trị:

```json
{"eventType":"UserClicked","userId":1001,"productId":501}
{"eventType":"UserClicked","userId":1001,"productId":502}
{"eventType":"UserClicked","userId":1002,"productId":501}
```

Nén từng record riêng lẻ không tận dụng tốt phần dữ liệu lặp. Kafka nén **record batch**, giúp compression ratio tốt hơn:

```text
Record batch
  -> compress một lần
  -> gửi qua network
  -> lưu trong Kafka log ở dạng compressed
  -> truyền batch compressed cho consumer
  -> consumer decompress
```

Lợi ích:

- Giảm network bandwidth producer -> broker.
- Giảm dung lượng storage.
- Giảm bandwidth khi replicate giữa broker.
- Giảm bandwidth broker -> consumer.

Đánh đổi:

- Producer và consumer tốn CPU để compress/decompress.
- Batch nhỏ thường nén kém hiệu quả.
- Chọn thuật toán phụ thuộc ưu tiên CPU, ratio, throughput và compatibility.

Kafka hỗ trợ các codec như `gzip`, `snappy`, `lz4`, `zstd`. Cấu hình và cách chọn được trình bày chi tiết tại `Producer Compression`.

### 5. Zero-copy giảm việc copy dữ liệu qua user space

Nội dung trong ảnh cũ mô tả đường truyền file thông thường:

```text
Không dùng zero-copy

1. Disk -> kernel read buffer/page cache
2. Kernel space -> user-space application buffer
3. User space -> kernel socket buffer
4. Kernel socket buffer -> NIC
```

Ở đường này, Kafka application không biến đổi payload nhưng dữ liệu vẫn bị copy vào user space rồi copy ngược về kernel để gửi qua network. Việc đó tốn CPU, memory bandwidth và system call.

Với `sendfile()`/zero-copy, OS có thể chuyển dữ liệu từ page cache tới network mà không cần copy payload qua Kafka user-space buffer:

```text
Có zero-copy

Disk -> OS page cache -> socket/NIC -> network
             ^
        Kafka yêu cầu kernel gửi vùng file này
```

Tác dụng:

- Giảm số lần copy dữ liệu.
- Giảm context switch giữa user space và kernel space.
- Giảm CPU và memory bandwidth của broker.
- Broker có thể phục vụ consumer với tốc độ gần giới hạn network hơn khi dữ liệu đã nằm trong page cache.

Zero-copy chủ yếu hữu ích trên đường broker gửi log data cho consumer/follower khi Kafka không cần deserialize và transform từng record.

> Giới hạn quan trọng: tài liệu Kafka ghi rõ `sendfile` không được dùng khi bật SSL/TLS vì thư viện TLS hoạt động trong user space và Kafka hiện không dùng in-kernel `SSL_sendfile`. Khi đó vẫn có lợi từ batching, compression, page cache và partitioning, nhưng không nên khẳng định đường truyền có đầy đủ zero-copy như plaintext.

### 6. Partitioning tạo parallelism

Một topic có thể chia thành nhiều partition và phân bố trên nhiều broker:

```text
Topic: order-events

Partition 0 -> Broker A
Partition 1 -> Broker B
Partition 2 -> Broker C
```

Nhờ đó:

- Nhiều producer có thể ghi vào các partition khác nhau.
- Nhiều broker phục vụ I/O song song.
- Nhiều consumer trong cùng group xử lý các partition khác nhau.
- Có thể scale ngang bằng cách thêm broker, partition và consumer phù hợp.

Partitioning không làm một partition đơn lẻ nhanh vô hạn. Một hot key có thể dồn phần lớn traffic vào một partition và một broker, trong khi các partition khác nhàn rỗi.

```text
Sai distribution:
Partition 0: ████████████████████
Partition 1: ██
Partition 2: █
```

Cách chọn key, số partition và ảnh hưởng tới ordering được trình bày tại `Event Key`, `Producer Message Key` và `Producer Partition Strategy`.

### 7. Consumer pull và fetch theo batch

Kafka consumer chủ động gửi fetch request và chỉ rõ offset muốn đọc. Broker trả về một vùng record liên tiếp, thay vì phải push và theo dõi trạng thái giao nhận của từng record cho từng consumer.

```text
Consumer: cho tôi dữ liệu từ offset 500, tối đa theo fetch config
Broker:   trả về [500...N] trong một response
```

Pull model giúp consumer:

- Tự điều chỉnh tốc độ lấy dữ liệu theo khả năng xử lý.
- Fetch nhiều record trong một lần.
- Tạm dừng rồi tiếp tục từ offset.
- Replay bằng cách đổi vị trí đọc.

Nếu fetch quá nhỏ, consumer tạo nhiều request và giảm throughput. Nếu fetch/batch quá lớn, memory usage và thời gian xử lý một poll có thể tăng. Các cơ chế commit, rebalance và consumer thread được trình bày tại `Consumer`.

### 8. Kafka giữ ít trạng thái giao nhận per-message trên broker

Kafka không xóa record chỉ vì một consumer đã đọc xong. Record được cleanup theo retention/compaction của topic; tiến độ consumer group được biểu diễn chủ yếu bằng committed offset.

```text
Topic data:       giữ theo retention/compaction
Consumer group A: offset 1.000
Consumer group B: offset   750
Consumer group C: offset 1.200
```

Broker không cần đánh dấu riêng từng record là “đã ACK bởi consumer A/B/C rồi xóa record đó”. Mô hình log + offset giúp thêm consumer group mới tương đối rẻ và cho phép nhiều group đọc độc lập.

Điều này không có nghĩa consumer logic đơn giản: application vẫn phải thiết kế commit offset, idempotency, retry và duplicate handling. Xem `Consumer Offset Commit Strategy` và `Kafka Delivery Semantics`.

### Ví dụ thực tế: vì sao gửi từng message lại chậm?

Giả sử application phát 50.000 click event mỗi giây, mỗi event khoảng 300 bytes.

Cách kém hiệu quả:

```text
50.000 event
  -> 50.000 request nhỏ
  -> nhiều header, packet, syscall và broker request
```

Cách Kafka tối ưu:

```text
50.000 event
  -> batch theo partition
  -> compress batch
  -> ít request lớn hơn
  -> append tuần tự
  -> consumer fetch theo block
```

Payload gốc chỉ khoảng 15 MB/s, nhưng gửi từng record có thể tạo overhead rất lớn. Batching và compression giúp hệ thống tiến gần hơn tới việc truyền chính payload thay vì dành phần lớn tài nguyên cho thao tác bao quanh từng message.

### Khi nào Kafka vẫn chậm?

Kafka có thể có throughput thấp hoặc latency cao nếu:

- Producer gửi từng record với batch rất nhỏ hoặc `linger.ms` không phù hợp workload.
- Payload quá lớn, serialization hoặc compression tiêu tốn nhiều CPU.
- Chọn key tạo hot partition.
- Số partition/broker không đủ cho mức parallelism cần thiết.
- Disk chậm, gần đầy hoặc page cache bị memory pressure.
- Network giữa producer, broker, replica và consumer bị nghẽn.
- Replication factor, `acks=all` và ISR làm tăng chi phí để đổi lấy durability cao hơn.
- Consumer xử lý business logic chậm hoặc gọi database/API đồng bộ cho từng record.
- Consumer poll/fetch không hợp lý hoặc xảy ra rebalance thường xuyên.
- TLS, authorization, quotas và cross-region traffic thêm CPU/network overhead.
- Broker vừa phục vụ traffic realtime vừa chịu replay/backfill lớn.

Kafka nhanh ở data plane không có nghĩa toàn bộ pipeline nhanh. Nếu consumer nhận một batch trong 10 ms nhưng mất 5 giây để insert từng record vào database, bottleneck nằm ở downstream chứ không phải Kafka.

### Tư duy tối ưu nhanh

- Muốn tăng throughput producer: ưu tiên batching, compression và phân phối key đều; xem `High Load Producer`, `Producer Compression`.
- Muốn tăng throughput consumer: fetch/process theo batch, scale theo partition và tránh gọi downstream từng record nếu có thể.
- Muốn giảm latency: giảm thời gian chờ batch nhưng chấp nhận throughput/CPU/network có thể kém hơn.
- Muốn durability cao: cấu hình ACK, replica và ISR đúng; không đánh đổi an toàn dữ liệu chỉ để benchmark đẹp.
- Muốn tìm bottleneck: đo producer request latency, broker disk/network, under-replicated partition và consumer lag thay vì chỉ nhìn message/second.

### Đánh giá lại ghi chú cũ

- Đúng: Kafka hưởng lợi từ partition parallelism, batching, compression, sequential I/O, OS page cache và zero-copy.
- Cần sửa thuật ngữ: `Partition detach` nên viết là **partitioning tạo parallelism và phân phối tải**.
- Cần sửa: broker không đơn giản gom message trong một “in-memory buffer riêng” rồi mới flush. Producer có buffer/batch; broker ghi vào log thông qua filesystem và tận dụng OS page cache.
- Cần nói cẩn trọng hơn: dữ liệu mới ghi **thường** còn trong page cache, không phải “chắc chắn”.
- Cần bỏ cách nói tuyệt đối: sequential I/O có thể rất hiệu quả nhưng không phải luôn nhanh hơn mọi random access trong RAM.
- Cần bổ sung: zero-copy có điều kiện và không dùng theo đường `sendfile` khi Kafka bật SSL/TLS.
- Cần nhớ: throughput cao là kết quả end-to-end của batch lớn, I/O liên tục và parallelism; cấu hình tối ưu throughput thường đánh đổi latency, CPU, memory hoặc durability.

## RabbitMQ vs Kafka

- Model: Push Model (RabbitMQ - Broker đẩy tin): Ngay khi có tin nhắn mới, Broker sẽ chủ động "nhồi" tin nhắn đó xuống Consumer, **Độ trễ cực thấp:** Tin nhắn được chuyển đi ngay lập tức khi vừa đến Broker + **Tiết kiệm tài nguyên Consumer:** Consumer không phải tốn công "hỏi" xem có tin mới hay chưa + **Dễ gây "ngợp" (Overwhelmed):** Nếu Consumer xử lý chậm mà Broker cứ đẩy dồn dập, Consumer có thể bị treo hoặc tràn bộ nhớ. (Để khắc phục, RabbitMQ dùng cơ chế `Quality of Service - QoS` để giới hạn số tin nhắn chưa Ack) >< Pull Model (Kafka - Consumer kéo tin): Consumer tự quyết định khi nào nó sẵn sàng để lấy dữ liệu từ Broker, **Tự chủ về tốc độ (Flow Control):** Consumer xử lý theo khả năng của mình. Nếu hệ thống đang quá tải, nó có thể chậm lại mà không sợ bị Broker ép chết + **Batching (Gộp tin):** Consumer có thể kéo một lúc hàng ngàn tin nhắn để xử lý đồng loạt, giúp tối ưu hóa hiệu suất mạng và I/O + **Độ trễ nhất định:** Nếu Consumer không kiểm tra thường xuyên, tin nhắn sẽ nằm chờ ở Broker lâu hơn một chút.
- Throughput: **RabbitMQ:** Coi Queue là một cấu trúc dữ liệu phức tạp trong bộ nhớ (In-memory). Khi tin nhắn được gửi đi và Ack, nó phải cập nhật trạng thái liên tục. Việc quản lý trạng thái của từng tin nhắn riêng lẻ tốn rất nhiều CPU >< **Kafka:** Sử dụng cơ chế **Sequential Log (Ghi nhật ký tuần tự)**. Dữ liệu chỉ được ghi nối đuôi vào cuối file trên đĩa cứng. Việc ghi tuần tự nhanh hơn rất nhiều so với ghi ngẫu nhiên. Ngoài ra, Kafka dùng kỹ thuật **Zero-copy**, cho phép dữ liệu đi thẳng từ đĩa cứng ra card mạng mà không cần đi qua CPU của ứng dụng + vẫn có độ trễ do phải ghi disk.
- Scalability: RabbitMQ (Dọc - Vertical): Một Queue trong RabbitMQ về cơ bản là đơn luồng (Single-threaded) trên một Node để đảm bảo thứ tự tin nhắn. Nếu bạn muốn xử lý nhiều tin hơn, bạn cần CPU mạnh hơn trên chính Node đó. Việc chia nhỏ một Queue ra nhiều Server rất phức tạp >< Kafka (Ngang - Horizontal) nhờ Partition: Một Topic trong Kafka được chia thành nhiều Partition + Mỗi Partition có thể nằm trên một Server (Broker) khác nhau.
- Persistence: RabbitMQ được thiết kế để chuyển tiếp tin nhắn, không phải để lưu trữ lâu dài, khi tin nhắn đến, nó được đưa vào một cấu trúc hàng đợi (Queue) trong bộ nhớ (RAM). Nếu cấu hình "persistent", nó sẽ được ghi xuống đĩa cứng + Broker phải theo dõi sát sao trạng thái của từng tin nhắn (Đang chờ, Đã gửi, Đã nhận Ack) + Ngay khi Consumer xác nhận đã xử lý xong (`Acknowledgment`), tin nhắn đó sẽ bị xóa hoàn toàn khỏi hàng đợi để giải phóng tài nguyên >< Kafka coi dữ liệu là một chuỗi các sự kiện (Events) không thể thay đổi, được ghi tuần tự vào các file log trên đĩa, tin nhắn (Message) được ghi nối đuôi vào cuối một Partition. Nó không bị xóa đi sau khi Consumer đọc xong + Thay vì Broker phải nhớ Consumer đã đọc đến đâu, chính Consumer sẽ giữ một "dấu trang" gọi là Offset + Dữ liệu được giữ lại dựa trên cấu hình (ví dụ: giữ trong 7 ngày hoặc giữ đến khi đạt 100GB). Sau thời gian đó, Kafka mới tự động xóa các đoạn log cũ → Bạn có thể yêu cầu Consumer đọc lại dữ liệu từ 3 ngày trước nếu gặp sự cố. Điều này là không thể với RabbitMQ + Hiệu suất của Kafka không phụ thuộc vào việc bạn lưu 1GB hay 1TB dữ liệu, vì việc ghi chỉ là ghi nối đuôi (Sequential I/O).
- Routing: là điểm mà RabbitMQ tỏa sáng rực rỡ hơn hẳn so với Kafka.
- RabbitMQ: "Bộ định tuyến" thông minh, Producer không gửi tin nhắn trực tiếp vào Queue. Thay vào đó, nó gửi vào một **Exchange**. Exchange này đóng vai trò như một cảnh sát giao thông, nhìn vào "nhãn" (Routing Key) của tin nhắn để quyết định đẩy nó vào đâu.

![Kafka image 5](images/kafka-image-05.png)

- Apache Kafka: Định tuyến đơn giản (Key-based): Kafka không có khái niệm Exchange. Khả năng định tuyến của nó thô sơ hơn và tập trung vào việc phân chia dữ liệu (Partitioning).

![Kafka image 6](images/kafka-image-06.png)

## Broker + Topic + Partitions + Segment + Offset

- 1 Cluster = n Brokers (node)
- 1 Broker = n Partitions
- 1 Topic = n Partitions.
- 1 Partition = n Segments
- 1 Segments = n Offsets

![Kafka image 7](images/kafka-image-07.png)

![Kafka image 8](images/kafka-image-08.png)

Broker thực chất là một tiến trình (process) chạy trên một máy chủ vật lý hoặc máy chủ ảo, một hệ thống Kafka thường bao gồm nhiều Broker kết hợp lại với nhau để tạo thành một Kafka Cluster + các messages sẽ được lưu trữ trong các Partitions của Broker dưới dạng bytes trong disk của Broker.

- Mỗi Broker trong Cluster sẽ có 1 id giá trị số khác nhau để phân biệt các Broker.
- Mỗi Broker có khả năng lưu trữ nhiều Partition.
- Mặc dù Broker lưu trữ dữ liệu, nhưng nó không theo dõi việc Consumer đã đọc đến đâu (việc này do Consumer hoặc Metadata quản lý), giúp nó giữ được hiệu năng cực cao.
- Chịu trách nhiệm nhận tin nhắn từ Producer, lưu trữ chúng xuống đĩa cứng và trả lại cho Consumer khi được yêu cầu + Quản lý partition, mỗi Topic trong Kafka được chia nhỏ thành các Partition, các Partition này sẽ được phân tán (distributed) đều trên các Broker trong Cluster.
- Trong một cụm (Cluster), sẽ có một Broker được bầu làm Controller. Broker này có quyền lực cao nhất: nó theo dõi trạng thái của các Broker khác, quản lý việc bầu chọn Leader cho các Partition và đảm bảo sự cân bằng trong hệ thống.

![Kafka image 9](images/kafka-image-09.png)

- Kafka có tính năng tốt là khi bất kỳ Producer, Consumer nào mà connect được với 1 Broker trong Cluster thì sẽ biết cách connect tới toàn bộ Cluster này → chỉ cần connect với 1 Broker thì Client sẽ tự động biết cách connect với các Broker khác

![Kafka image 10](images/kafka-image-10.png)

Topic sử dụng để lưu trữ, tổ chức các events, là 1 datastream (đại diện cho luồng dữ liệu giúp duy trì thứ tự message).

- Topic khá giống với database table, các events như là các row trong table này vậy nhưng không có query mà sử dụng thông qua Producer, Consumer + cũng không có constraint.
- Topic có thể handle nhiều loại message type như là: JSON, Avro, text files, binary,...
- Các events có chung mục đích sẽ được đặt trong cùng 1 Topic: giả sử Topic “user” sẽ chỉ có thông tin events của user; Topic “payment” sẽ chỉ có thông tin events của payment thôi
- 1 Topic chứa 1 tập các Partitions (tuy nhiên các Partition này phải chung mục đích của Topic đã phân tách ra chúng) + mỗi Partitions chứa các events và mỗi khi events tới nó sẽ được add vào cuối danh sách của Partition (tức là các events sẽ order theo time)
- Các events sau khi add vào Topic sẽ không thể thay đổi được
- 1 Topic có thể có 0,1,n Consumers read events/ Publishers write events từ Topic đó

=> Scalable vì 1 application server có thể read/ write data từ nhiều Topics khác nhau tại 1 thời điểm + việc ta thêm Topic mới không ảnh hưởng tới các Topic cũ

Partition tức là 1 phần của 1 Topic sau khi nó được chia nhỏ ra + partitions này có thể đặt trên các Brokers riêng biệt trong hệ thống Kafka cluster nhưng nó vẫn chung mục đích với Topic gốc, chỉ là nhân bản ra để xử lý đồng thời → sẽ giảm thời gian handle đi thay vì xử lý các events trên 1 node, việc handle events sẽ diễn ra trên nhiều Brokers/ nodes, làm hệ thống dễ scale hơn, performance cũng sẽ tốt hơn.

- Mặc định thì 1 Topic được tạo mà không chỉ định rõ số lượng Partition thì Topic đó chỉ gồm 1 Partition.
- Partition ra đời để giải quyết hạn chế tính chất của Topic là: 1 Topic chỉ nằm trên 1 Broker/ node nên sẽ làm giảm khả năng scale của cả hệ thống, việc process trên 1 Topic lúc này chỉ xảy ra trên 1 node nên sẽ gây quá tải => nhờ đó mà ta có thể write, read, process trên nhiều node, giúp các process này xảy ra nhanh hơn.
- Thông thường nếu message không có key, các events sẽ được phân phối đều giữa tất cả các Partitions (tức mỗi Partitions sẽ nhận được 1 phần data đồng đều)
- Tuy nhiên giữa nhiều Partition như thế này, chắc chắn không đảm bảo được thứ tự read của Consumer tới toàn bộ Partition + tuy nhiên với 1 Partition thì vẫn luôn đảm bảo thứ tự read.
- Việc xóa Partition là không thể, không ổn do khi được add thì các events đã tới và việc xóa sẽ làm mất events.

Leader Partition tức là trong hệ thống Partition cần được replica, sẽ có 1 Partition làm Leader, nhận mọi request từ Producer và Consumer, sau đó đồng bộ tới các Slave Partition khác.

![Kafka image 11](images/kafka-image-11.png)

- Tuy nhiên từ Kafka >= 2, có 1 chức năng là Kafka Consumer Replica Fetching cho phép Consumer read được bất kỳ Partition nào, miễn là tốc độ xử lý cao (do là vấn đề địa lý, network).

### Event Key

- Khi có 1 event được send tới Topic, bản thân là nó được append vào một trong các partition của Topic thay vì toàn bộ partition + nếu events same key thì chúng sẽ được bắn tới cùng 1 partition + Kafka cũng đảm bảo rằng Consumer đã subscribe partition nào thì sẽ chỉ đọc được những events ở partition ấy và đọc các events theo đúng thứ tự chúng được write
- Để đảm bảo thêm tính fault-tolerant, mỗi Topic cần replicated để khi có hỏng 1 Topic, vẫn còn những nơi khác xử lý.

### Segment

- 1 Partition có nhiều Segment (được biểu diễn bằng 1 file) + Mặc dù về mặt logic, một Partition là một file log dài vô tận, nhưng thực tế hệ điều hành không thể quản lý một file lớn đến hàng Terabyte một cách hiệu quả. Do đó, Kafka chia nhỏ một Partition thành các Segment.

![Kafka image 12](images/kafka-image-12.png)

- Segment cuối cùng là Segment đang được hoạt động, hiện đang được ghi vào nên Offset cuối cùng của nó vẫn chưa được xác định
- Ta có thể config size tối đa cho 1 Segment thông qua config log.segment.bytes, và mặc định của nó là 1 Gb + nếu vượt quá 1Gb thì Segment sẽ bị đóng và 1 Segment mới được tạo ra
- Ta có thể config thời gian tạo 1 Segment mới ngay cả khi chưa vượt quá size tối đa của nó thông qua config log.segment.ms + mặc định sẽ là 1 tuần + khi apply thì nó sẽ tính mốc thời gian từ thời gian của offset cuối cùng thuộc Segment để quyết định thời gian clean Segment
- Dựa vào Retention Policy (chính sách lưu trữ), Segment cũ sẽ bị xóa nếu vượt quá thời gian lưu trữ (log retention period) hoặc kích thước lưu trữ tối đa (log retention size)
- Dọn dẹp dữ liệu (Data Retention): Đây là lý do chính. Kafka không xóa từng tin nhắn riêng lẻ vì quá tốn kém hiệu năng. Thay vào đó, nó xóa nguyên một Segment cũ khi Segment đó vượt quá thời gian lưu trữ hoặc kích thước cho phép + Hiệu năng: Làm việc với các file có kích thước vừa phải giúp hệ điều hành quản lý bộ nhớ đệm (Page Cache) tốt hơn và việc tìm kiếm dữ liệu qua Index nhanh hơn + An toàn: Nếu một file log bị hỏng, nó chỉ ảnh hưởng đến một Segment nhỏ thay vì làm hỏng toàn bộ dữ liệu của cả một Partition.

### Offset

![Kafka image 13](images/kafka-image-13.png)

- Mỗi Partition có 1 tập offset riêng + offset không thể thay đổi, luôn giữ giá trị cố định.
- Offset bắt đầu từ 0 với mỗi Partition và tăng dần lên theo số lượng message nạp vào, đại diện cho vị trí của 1 message trong 1 Partition.
- Sử dụng Offset với mục đích chính là giúp Consumer theo dõi vị trí đọc bằng cách đánh dấu Offset đã đọc (vị trí đã đọc rồi) để đảm bảo không tiêu thụ 1 message 2 lần.

![Kafka image 14](images/kafka-image-14.png)

![Kafka image 15](images/kafka-image-15.png)

### Event / Message / Record / Data

- Với context của Apache Kafka hay là Event-Driven Architecture thì event nghĩa là có 1 sự thay đổi liên quan tới data và nó sẽ cần các bên khác xử lý khi data này có sự thay đổi = something happened

![Kafka image 16](images/kafka-image-16.png)

- Event có key, value, timestamp, optional metadata headers
- Các Events trong Kafka không bị xóa sau khi sử dụng + ta có thể config thời gian sống của các events một cách độc lập với từng Topics/ sẽ xóa nếu vượt quá size của Partition + performance của Kafka không ảnh hưởng bởi số lượng events đang có trong Topic nên việc lưu data trong thời gian dài là ổn.

## Producer

![Kafka image 17](images/kafka-image-17.png)

Producer sử dụng để create và publish message tới Topics.

- Message gửi tới Topic được tuần tự hóa + message được gắn với Offset.
- Topic, Partition không quyết định việc nhận messages nào, nó được quyết định bởi Producer (dựa trên các strategy bên dưới).
- Ta có thể dễ dàng thêm/ bỏ 1 Producer mà không ảnh hưởng tới các services khác.

### Producer Message Key

Nói về message trong Kafka, mỗi message sẽ có 3 thành phần chính gồm:

- key: là key của message, thường biểu diễn dạng String
- value: giá trị của message
- partition: partition để message gửi tới, thường biểu diễn dạng số thứ tự của partition.

![Kafka image 18](images/kafka-image-18.png)

- Key của message có thể null được, nếu null thì sẽ sử dụng Round robin.
- Các message mà same key sẽ được đặt chung vào 1 Partition dựa vào thuật toán hashing đơn giản murmur 2 algorithm: partition = hash(key) % số lượng partition => nếu hashing mà dữ liệu cùng key sẽ luôn nằm trên cùng 1 partition trừ khi số lượng partition thay đổi.

### Kafka Message Serializer

- Tuy nhiên ở Producer, Consumer ta không viết message dạng byte -> ta cần quá trình serialization để giúp convert object sang byte trước khi send tới Kafka + convert sang object khi Consumer nhận message

Ví dụ: Tôi khi send message từ Producer có key-value là 123-helloworld nhưng tất nhiên nó không ở dạng byte trong ngôn ngữ lập trình -> chỉ định Integer Serializer cho key, giúp chuyển 123 sang dạng chuỗi byte + đối với value cũng tương tự vậy

![Kafka image 19](images/kafka-image-19.png)

### Producer Partition Strategy

#### Round-Robin Partition Strategy

![Kafka image 20](images/kafka-image-20.png)

- Round-robin apply với message không có key/ dev lựa chọn rõ ràng strategy này với project + mỗi message sẽ được nạp vào 1 request và send tới 1 Partition cụ thể (1 message/ 1 request)
- Round-robin bỏ qua Hash function bất kể có giá trị key message hay không -> Strategy này phù hợp khi workload hầu hết tập trung vào 1 hash key value của message -> dẫn tới Kafka chỉ work trong 1 Partition duy nhất -> dẫn tới chỉ work trong 1 Consumer duy nhất (trong cùng 1 Consumer Group) + nếu khác group thì lại chỉ work trong các Consumers được assign với Partition đó

#### Default Partition Strategy

- Nếu key null, messages được gửi theo thuật toán Round Robin/ Sticky tùy theo version. (Mặc định với Kafka, từ version <= 2.3, sử dụng Round-robin, và từ version >= 2.4 sử dụng Sticky partition)
- Nếu có key, Kafka sẽ hash key này và dựa vào hash key value này.

![Kafka image 21](images/kafka-image-21.png)

=> Tức là nếu cùng 1 message key, message sẽ nằm trên cùng 1 partition >< nếu số lượng partition thay đổi thì nếu trùng message key với các message mà trước khi bị thay đổi thì có thể bị nằm trên 1 partition khác.

#### Why Sticky Partition Strategy?

- Nếu mà send nhiều messages tới cùng 1 Partitions thì ta có thể sử dụng Batching để send các messages này tới Topic cùng 1 thời điểm + rõ ràng là các Batch mà gửi 1 số lượng messages nhỏ (tức sẽ cần nhiều requests, queue hơn để handle -> sẽ gây latency cao hơn) thì nó sẽ không tối ưu bằng việc Batch gửi 1 số lượng lớn messages.

batch.size tức là số lượng messages/ batch, khi đạt tới giới hạn này, ngay lập tức batching với số lượng messages này tới Topic
linger.ms tức là thời gian batching diễn ra mà không cần lấp đầy batch.size
=> tức là ngay sau khi đạt tới batch.size / linger.ms thời gian đã trôi qua thì sẽ ngay lập tức batching

- Việc sử dụng linger.ms chấp nhận 1 lượng delay khá nhỏ, nhưng lại giúp giảm đáng kể latency của toàn bộ quá trình, tăng thông lượng do vì số lượng request ít hơn
- Mặc định với Kafka, batch.size = 16.384 bytes ; linger.ms = 0ms
- Việc linger.ms = 0 thì không phải là cứ lần nào generate ra message bởi Producer thì sẽ send 1 message đơn lẻ đó ngay lập tức đến Topic mà Producer cần 1 khoảng thời gian n (rất nhỏ) để xử lý và send các messages + trong n thời gian này thì nếu các messages được tạo ra để gửi đến cùng Partition thì chúng sẽ được nhóm vào chung 1 Batch và gửi đi trên 1 request + nhưng việc đặt như thế này thì thông thường Batch sẽ có ít messages do => linger.ms sẽ không ngăn chặn Batching

Sticky partition strategy sẽ giải quyết vấn đề bằng cách đặt ra 1 Batch + sau khi Batch đó lấp đầy bằng các messages thì sẽ Batching nó tới 1 Partition

- Điều này giúp hạn chế tình trạng 1 request 1 message mà sử dụng Batching với số lượng nhỏ messages.
- Trong nhiều lần như vậy thì các Partitions sẽ không đồng đều về số lượng.

![Kafka image 22](images/kafka-image-22.png)

- Sticky partition strategy apply khi key message = null/ dev lựa chọn rõ ràng strategy này với project.
- Nếu message có key, thì ta vẫn phải đảm bảo nó gửi tới đúng Partition, còn không có key thì nó sẽ chọn 1 Sticky Partition + việc pick Partition thường là round-robin giữa các Partition để đảm bảo cân bằng giữa các Partitions
- Sticky khá giống với Round-robin, nhưng ưu điểm của nó là Batching 1 số lượng lớn cho 1 Partition + sau khi Batching xong nó sẽ chọn 1 Sticky Partition khác để Batching turn2 => nó sẽ giảm số lượng request + giảm switch Partition so với Round-robin

### Producer ACK

Producer Acknowledgement tức là 1 sự confirm từ phía Broker là messages đã được send từ Producer tới Broker thành công/ thất bại.

acks = 0 thì ở mode này, thì Producer không chờ Broker gửi ack về, chỉ cần biết là messages đã gửi đi rồi nhưng không biết nó đã thành công/ thất bại + mode này giúp tăng throughput đáng kể nhưng không đảm bảo rằng messages gửi đi có thành công/ mất messages

- 1 case khá phổ biến là Broker offline trước khi nhận được message/ exception sẽ gây mất message

![Kafka image 23](images/kafka-image-23.png)

acks = 1 là mode default của Kafka từ v1.0 tới v2.8 + Producer sẽ chờ cho tới khi Broker phản hồi acks + nó sẽ sent luôn ack ngay sau khi write thành công vào Leader Broker, không đợi quá trình replica có thành công hay thất bại -> vẫn có khả năng mất message nhưng có giới hạn.

- Nếu như việc sent message không thành công bị phản hồi thông qua ack, Producer sẽ retry request
- 1 case khá phổ biến là Replica Broker offline/ gây exception thì sẽ gây mất message ở các Replica nhưng vẫn thông báo thành công tới Producer

![Kafka image 24](images/kafka-image-24.png)

- TH1 Message đã persist xuống Leader Broker + Leader Broker chết trước khi gửi ACK về Producer
- TH1.1 Message đã persist xuống Leader Broker mới + lúc này Producer retry send message cũ này tới Leader Broker mới -> consume 2 lần message này

  (at least once)

- TH1.2 Message chưa persist xuống Leader Broker mới + Producer retry thì message chỉ bị consume 1 lần -> hợp lý
- TH2 ACK trả về Producer thành công trước khi Leader Broker chết + chưa persist xuống các ISR Broker -> Leader Broker mới sẽ không có message này và gây mất message (at most once)

acks = all, ack = -1 là mode default từ v3.0 đổ lên + có level reliability cao nhất + Producer chỉ nhận được ack sau quá trình all in-sync replica (ISR - quá trình đồng bộ message tới toàn bộ replica) + tức là nếu nó ghi vào Leader Broker thành công nhưng đồng bộ xuống các Replica thất bại thì cả quá trình sẽ thất bại + tất nhiên là nó sẽ tăng đáng kể latency do phải network call

- Trước khi gửi về Producer, Broker Leader sẽ xem các ack của các Replica xem có thành công/ thất bại -> send ack về cho Producer
- Ta phải config thêm min.insync.replicas = value (value tức là số lượng Broker đã nhận message thành công, tính cả Leader thì sẽ send ack thành công cho Producer)
- Khá giống với các TH trong acks = 1, nhưng khác chỗ là TH2 luôn success là at least once

![Kafka image 25](images/kafka-image-25.png)

- TH1 Message đã persist xuống Leader Broker cùng các ISR Broker tuy nhiên Leader Broker bỗng nhiên sập -> Producer chưa nhận được ACK -> send lại message dẫn tới duplicate message.

### Idempotent Producer

Idempotent Producer sinh ra để giải quyết vấn đề duplicate message ở phía trên

![Kafka image 26](images/kafka-image-26.png)

- Từ Kafka 3.0 đổ lên thì mode này đặt là mặc định.
- Mỗi lần send message, nó sẽ gửi kèm message sequence + producer id -> Partition sẽ lưu thông tin này lại -> giả sử truyền ACK lại nhưng Producer không nhận được thì nó send lại message cùng với message sequence + producer id cũ -> Partition nhận ra và không persist lại message này nhưng vẫn gửi ACK trở lại cho Producer để nó ngừng send lại (message sequence này bắt đầu từ 0, và message mới sẽ thêm 1 đơn vị).
- Mỗi lần đồng bộ dữ liệu xuống Slave Partition, cũng sẽ copy giá trị message sequence + producer id để lỡ mà Lead Partition die thì khi nó lên làm Lead vẫn có dấu message này nên cũng không thể duplicate message được.
- Cũng có support transaction trong Kafka Producer, đặt transaction lên 1 method khiến: 1 là message có thể truyền hết đến các Broker hoặc không message nào được truyền tới.

### Producer Retry

![Kafka image 27](images/kafka-image-27.png)

- Dùng retry.backoff.ms (default 100ms) để chỉ định thời lượng tạm dừng trước khi retry
- request.timeout.ms (default 30s) để chỉ định thời gian chờ tối đa phản hồi từ Broker sau khi Producer request message.
- delivery.timeout.ms (default 2p) để chỉ định thời gian chờ tối đa cho 1 message được gửi thành công (bao gồm thời gian từ khi gửi request đầu tiên, tính cả time retry)

### High Load Producer

Bandwidth tức băng thông - là số lượng data có thể truyền tải trong 1s (Mbps/s) - là tốc độ tối đa mà data có thể truyền qua mạng
ThroughPut tức thông lượng - là lượng data thực tế được truyền qua hệ thống trong 1 khoảng thời gian nhất định + Bandwidth > ThroughPut do ThroughPut là thực tế, chịu ảnh hưởng bởi các yếu tố: nhiễu, tắc mạng,...

High Load Producer tức tại 1 thời điểm, quá nhiều events phải truyền qua mạng để gửi tới Broker + Broker/ Producer không thể handle được nhiều events như vậy tại 1 thời điểm (là do config batch nên có nhiều message chưa được gửi đi)

- Ta có thể lưu các events này trên RAM của Producer + ta có thể cài đặt thông qua buffer.memory (default 32Mb) để config kích thước mặc định RAM dùng để lưu tạm thời các events trước khi send chúng tới Broker giúp hệ thống mượt hơn ngay cả tại thời điểm nhu cầu cao
- Tuy nhiên khi vượt quá dung lượng cho phép thì dẫn tới tình trạng tắc nghẽn + ta sẽ thiết lập max.block.ms, khi mà quá dung lượng RAM config kia mà send thêm messages, thay vì trả về exception luôn thì quá trình send messages này phải đợi 1 khoảng thời gian max.block.ms + nếu RAM config vẫn đầy sau khoảng thời gian này thì sẽ trả về exception

### Producer Compression

Producer Message Compression tức trước khi send message tới Broker, Producer sẽ compress message để giảm size cho message, đặc biệt là khi Batching 1 tập messages lớn làm cho tốc độ truyền message lên Broker sẽ nhanh hơn + cần ít dung lượng để lưu message trên Broker (giảm tới 4 lần)

![Kafka image 28](images/kafka-image-28.png)

- compression.type sử dụng để config type compress cho Producer với các định dạng như none (default), lz4, gzip, snappy, zstd
- Tuy nhiên nó lại yêu cầu nhiều hơn CPU cho các phép tính toán compress trước khi send message.

## Consumer

Consumer sử dụng để consume message từ Topic

![Kafka image 29](images/kafka-image-29.png)

- Consumer sử dụng pull model, tức là Consumer chủ động request message từ Kafka Broker và nhận messages từ response thay vì Kafka Broker send message tới các Broker + nếu có message thì trả về luôn + nếu không có message thì chờ trong 1 khoảng thời gian cấu hình rồi mới phản hồi.
- Việc read data từ Partition theo thứ tự Offset tăng dần

Consumer Deserializer cũng tương tự như Producer Serializer nhưng khác ở chỗ là lúc nhận byte thì chuyển sang dạng object để Consumer có thể work được

![Kafka image 30](images/kafka-image-30.png)

Consumer Group tức là 1 tập các Consumer đều read events từ cùng 1 Topic có nhiều partition tuy nhiên 1 message chỉ được tiêu thụ bởi 1 Consumer.

- Kafka sử dụng property group.id để đặt tên cho Consumer Group

![Kafka image 31](images/kafka-image-31.png)

- Nếu >= 2 Consumers cùng subscribe same Topic và cùng same Consumer Group thì chúng sẽ chia ra để xử lí các events của Topic đó mà không consumer trùng events với nhau -> ngoài partition ra thì Consumer Group là một cách để scale, performance cũng sẽ tốt hơn
- Nếu ta không sử dụng Consumer Group, thì với mỗi Consumer đều phải consume mọi message từ Topic -> các Consumer này sẽ consume message giống nhau.
- Consumer Group sẽ chia các Consumer tới để tiêu thụ với các Partitions riêng biệt (đây chính là lý do mà các events không thể trùng trong 1 Consumer group)

![Kafka image 32](images/kafka-image-32.png)

- Nếu mà số lượng Consumer trong cùng 1 Consumer Group > số lượng Partitions của 1 Topic thì sẽ có Consumer rảnh

![Kafka image 33](images/kafka-image-33.png)

- Cho phép nhiều Consumer Groups focus vào chung Partition (bởi các Consumer Groups khác nhau có thể subscribe cùng 1 Topic)

Consumer Offset sử dụng để tracking messages nào được xử lý thành công gần nhất trong 1 Consumer Group dựa trên \_\_consumer\_offsets Topic

- Hầu hết các Consumer đều tự động commit offset theo định kỳ thay vì mỗi message được consume thành công để đảm bảo performance + Kafka Broker chịu trách nhiệm đảm bảo ghi vào \_\_consumer\_offsets Topic

![Kafka image 34](images/kafka-image-34.png)

- Consumer Offset là quan trọng vì giả sử các Consumer bị crash và restart/ 1 Consumer mới được thêm vào Group thì chúng sẽ consume từ latest commit offset thay vì consume toàn bộ message trong Partition từ offset 0
- Theo mặc định, Java sẽ tự động commit các Consumer Offset được kiểm soát bởi property enable.auto.commit=true sau mỗi auto.commit.interval.ms (mặc định là 5s) khi poll() được gọi

### Consumer Offset Commit Strategy

Commit Offset có nghĩa là quá trình ghi lại offset (= message id) cuối cùng đã đọc để giúp Consumer biết vị trí đọc message tiếp theo + khi Consumer mới đọc Partition này thì nó sẽ biết vị trí đọc của message thay vì đọc lại từ đầu.

- nên nhớ rằng poll() được message thì phải xử lý xong trước đã, rồi mới poll() lần tiếp theo.

Auto Offset Commit tức cơ chế tự động commit offset sau khi poll message về.

![Kafka image 35](images/kafka-image-35.png)

- Sử dụng enable.auto.commit=true thì sau mỗi khi poll được message, sau khoảng thời gian auto.commit.interval.ms thì Consumer sẽ gửi request tới Partition để tự động commit offset đã consume với offset có giá trị cao nhất. (Commit không tự thực hiện bởi dev, mà thực hiện tự động bởi Consumer ở chế độ background -> low latency)
- Nhưng trường hợp mà Consumer commit nhưng thực sự chưa xử lý xong message đó, Consumer offline + Consumer chưa commit và message đã xử lý xong, Consumer offline -> sẽ gây mất message/ duplicate message.
- Phù hợp với các dữ liệu ít quan trọng như log, monitoring hoặc giảm thời gian autocommit đi.

Sync Commit tức manual commit bởi dev, sử dụng blocking

- Do sử dụng Blocking nên thread executive message sẽ bị Block tới khi nhận được ACK từ Broker để báo commit thành công
- Blocking sẽ tốn performance
- Tuy nhiên đây là phương pháp đáng tin cậy nhất, phù hợp khi sử dụng với financial transactions/ các processing quan trọng

Async Commit cũng là manual commit bởi dev, nhưng sử dụng non-blocking

![Kafka image 36](images/kafka-image-36.png)

- Do sử dụng Non-blocking nên thread executive message sẽ không bị Block do không đợi ACK từ Broker để báo commit thành công
- Non-blocking nhanh do không phải đợi ACK, performance ổn hơn với Sync
- Tuy nhiên nó cũng gặp nhiều rủi ro như khi exe thành công ở Consumer nhưng Broker offline, do Non-blocking nên nó cũng sẽ không biết ACK thành công/ thất bại -> messages có thể bị duplicate

### Partition Rebalancing

Partition Rebalancing là quá trình phân phối lại quyền sở hữu các Partitions giữa các Consumer trong cùng một Consumer Group.

- Rebalancing được kích hoạt bởi Group Coordinator (một broker đóng vai trò quản lý group) khi có sự thay đổi về trạng thái của Group: Một Consumer mới startup và tham gia vào Group + Một Consumer bị crash, shutdown, hoặc không gửi heartbeat đúng hạn (session timeout) + Thêm Partition mới vào Topic hoặc Consumer thay đổi danh sách Topic đăng ký (regex subscription) + Consumer mất quá nhiều thời gian để xử lý bản ghi giữa các lần gọi `.poll()`, dẫn đến `max.poll.interval.ms` bị quá hạn.
- Sử dụng partition.assignment.strategy để cấu hình chiến lược phân vùng lại.

![Kafka image 37](images/kafka-image-37.png)

Eager Rebalance tức là nếu mà Consumers được add vào Consumer Group thì tất cả các Consumers trong Consumer Group cần assigned lại với Partitions nào

![Kafka image 38](images/kafka-image-38.png)

- Stop the world là drawback lớn nhất của nó là trong lúc nó assigned lại như vậy, Kafka sẽ ngừng hoạt động trong lúc assigned này + quá trình assigned diễn ra thường xuyên sẽ có tác động tiêu cực tới hệ thống + nếu như việc assigned với số lượng Partitions quá lớn sẽ gây dừng lại Server trong 1 thời gian khá dài để assigned.
- Sử dụng chiến lược tái cân bằng: Ranger Assignor (default), Round Robin, Sticky Assignor, Cooperative Sticky Assignor (tối ưu nhất).

Cooperative Rebalance (Incremental Rebalance, Kafka 2.4+) không giống như strategy trước mà chỉ assigned lại các Partitions thuộc 1 Consumer cũ sang Consumer mới này.

- Ưu điểm của strategy này là tránh được Stop the world tuy nhiên vẫn sẽ làm dừng lại Partition được assigned này.

![Kafka image 39](images/kafka-image-39.png)

Trong Kafka truyền thống (kafka 2.3-), mỗi khi một Consumer khởi động lại, nó được coi là một "thành viên mới" hoàn toàn. Điều này kích hoạt một đợt Rebalancing ngay lập tức >< Static Group Membership sinh ra để hạn chế tối đa sự rebalance do Consumer chỉ bị lỗi tạm thời trong khoảng thời gian ngắn và có thể restart để sử dụng lại ngay.

- Thông thường nếu 1 Consumer không phải là 1 Static Group Membership, khi tham gia 1 Consumer Group nó sẽ nhận được 1 member id do Kafka cấp phát + nếu rời Consumer Group nhưng tham gia lại thì nó sẽ nhận được 1 member id mới và yêu cầu rebalancing lại.
- Hiểu đơn giản: Đây là tính năng cho phép một Consumer giữ nguyên "danh tính" của mình kể cả sau khi shutdown và restart, giúp Group Coordinator nhận diện được "người cũ" và **không kích hoạt Rebalance**.
- Static Group Membership giúp duy trì member id, giúp Kafka nhận diện Consumer ngay cả khi khởi động lại và tiếp tục consume partitions cũ, tránh tình trạng rebalancing không cần thiết + member id này phải là duy nhất trong 1 Group nếu không sẽ báo lỗi + thấy có thể tự định nghĩa member id qua group.instance.id

![Kafka image 40](images/kafka-image-40.png)

![Kafka image 41](images/kafka-image-41.png)

- Nếu không sử dụng Static Group Membership thì khi offline xong tái online thì phải trải qua 2 lần Partition Rebalancing + việc này cũng làm cho hệ thống dừng hoạt động một phần trong 1 khoảng thời gian + việc restart lại Consumer này khiến nó lấy 1 Member Id mới trong Group Consumer + new Partitions assigned.
- Mỗi Consumer ta sẽ gán 1 group.instance.id + nếu Consumer bị offline và restart trước 1 khoảng thời gian session.timeout.ms thì Consumer sẽ không được coi là offline -> rebalancing sẽ không xảy ra.
- Tuy nhiên ta phải chấp nhận rằng việc các Partitions đang được gán cho Consumer vừa bị offline đó không được consume do Consumer Group không nhận ra rằng 1 Consumer vừa offline và phải gán các Partitions của nó cho Consumer mới -> các events sẽ cứ nằm ở đó không được consume cho tới khi vượt quá session.timeout.ms để rebalancing hoặc đợi Consumer này restart được.

### Consumer Offset Reset Config

- Bằng cách lưu trữ last offset success commit thì mỗi khi first read/ create/ restart/ rebalance/ offline 1 khoảng thời gian dài, message bị clean mất do retention policy,... với Consumer thì sẽ giúp Consumer biết để read message tiếp theo từ vị trí nào
- Mỗi Partitions sẽ có 1 offset riêng để dễ dàng tracking

![Kafka image 42](images/kafka-image-42.png)

auto.offset.reset.earliest tức nếu không tìm thấy last commit offset, sẽ read từ message đầu tiên của Partition đó

![Kafka image 43](images/kafka-image-43.png)

- Config này phù hợp với việc muốn lấy data từ đầu của Partition + vừa tạo
- Sử dụng config này -> cần retention lâu hơn để đảm bảo data có sẵn từ lúc đầu khi cần -> đòi hỏi dung lượng disk cao hơn
- Việc read từ đầu của 1 Partitions có quá nhiều messages sẽ gây ra quá tải cho Consumer -> giảm hiệu suất
- Ngoài ra còn có thể khiến các messages xử lý nhiều lần -> cần consider khi dùng

auto.offset.reset.latest tức nếu không tìm thấy last commit offset, sẽ read từ các message sau message cuối cùng hiện có trong Partition (default) - tức thay vì read từ các message đã có, ta sẽ read các message từ thời điểm Consumer này bắt đầu subscribe với Partitions này

![Kafka image 44](images/kafka-image-44.png)

- Config này tốt khi chỉ quan tâm tới các messages mới được tạo, không cần xử lí lịch sử
- Config này cần thêm thời gian config cho retention policy để cấp thêm thời gian, tránh xóa \_\_consumer\_offsets trước khi Consumer restart (bản thân consumer\_offsets cũng nằm trong phạm vi quản lý của retention policy)

auto.offset.reset.none tức nếu không tìm thấy last commit offset, sẽ throw exception

### Consumer Internal Thread

Consumer Group Coordinator là 1 thành phần nằm trong Group Consumer (1 Group Consumer sẽ có 1 Coordinator) với chức năng chính là kiểm tra trạng thái của các Consumer xem chúng có đang hoạt động hay không.

![Kafka image 45](images/kafka-image-45.png)

Heartbeat mechanism tức Consumer sẽ định kỳ gửi 1 signal tới để xác nhận rằng nó đang còn sống.

- heartbeat.interval.ms (default 3s) tức khoảng thời gian định kỳ gửi heartbeat. (truyền thống thì đặt = ⅓ session.timeout.ms)
- session.timeout.ms (default 45s cho Kafka >= 3.0, trước là 10s) tức khoảng thời gian mà không nhận được heartbeat sẽ coi Consumer die.

Pool mechanism tức sẽ nhận ra do Consumer poll() tới để lấy message.

- max.poll.interval.ms (default 5p) là khoảng thời gian giữa 2 lần poll() lớn hơn giá trị này thì sẽ coi như Consumer die. (quan trọng trong khuôn khổ dữ liệu lớn như Spark, nơi xử lý dữ liệu có thể tốn thời gian >< nếu ứng dụng nhanh thì có thể cài thời gian thấp đi)
- max.poll.records (default 500) xác định số lượng message tối đa được lấy trong 1 poll()

Chưa đọc: [https://medium.com/apache-kafka-from-zero-to-hero/apache-kafka-guide-39-consumer-replica-fetch-and-rack-awareness-setup-c86004d4ab80](https://medium.com/apache-kafka-from-zero-to-hero/apache-kafka-guide-39-consumer-replica-fetch-and-rack-awareness-setup-c86004d4ab80)

## Kafka Delivery Semantics

### Producer Delivery

At most once là semantic mà message chỉ được async send đi tối đa 1 lần bởi Producer, không bao giờ có chuyện lặp lại message.

![Kafka image 46](images/kafka-image-46.png)

- Kafka sử dụng At most once làm default + config acks = 0
- Fire and forget không cần kiểm tra xem có nhận được Ack từ Broker hay không/ nếu nhận được cũng sẽ không kiểm tra xem Ack là thành công hay không
- Sử dụng khi chấp nhận việc mất 1 message sẽ không ảnh hưởng tới hệ thống làm việc (như metric collections lock, logging,...)
- Việc sử dụng gửi đi không check Ack sẽ làm tăng throughput, giảm latency

At least once là semantic mà message được gửi đi >= 1 lần và message sẽ không bao giờ bị mất + có thể bị lặp lại message.

![Kafka image 47](images/kafka-image-47.png)

- Producer gửi tin nhắn và chờ xác nhận (ACK). Nếu không nhận được ACK, nó sẽ gửi lại cho đến khi thành công. Nếu lỗi mạng xảy ra sau khi Broker đã lưu tin nhưng chưa kịp gửi ACK, tin nhắn sẽ bị trùng.
- Chế độ phổ biến nhất, config acks = all

Exactly once là semantic mà message được sử dụng 1 lần mặc dù nó được gửi nhiều lần

![Kafka image 48](images/kafka-image-48.png)

- enable.idempotent = true thì tự động attach producer\_id vào mỗi message từ Producer để tránh duplicate message
- config acks = all

### Consumer Delivery

At most once: trong chế độ này, Consumer có thể làm mất dữ liệu nhưng không bao giờ xử lý trùng.

- Consumer nhận tin nhắn → Commit offset ngay lập tức → Sau đó mới bắt đầu xử lý logic (save DB, tính toán...).
- Nếu sau khi commit offset mà ứng dụng bị crash trước khi kịp xử lý xong dữ liệu, thì khi khởi động lại, Consumer sẽ đọc từ vị trí offset mới và bỏ qua tin nhắn cũ chưa xử lý xong.
- Config enable.auto.commit = true (auto.commit.interval đặt ở giá trị rất thấp để commit liên tục).

At least once Đây là chế độ mặc định và an toàn nhất cho hầu hết ứng dụng. Dữ liệu có thể bị xử lý lại nhưng không bị mất.

- Consumer nhận tin nhắn → Xử lý logic xong xuôi → Tiến hành commit offset.
- Tắt tự động commit, finally sẽ commit.

## Kafka Replica

Kafka Replica chính là "xương sống" giúp Kafka trở thành hệ thống chịu lỗi (fault-tolerant) cực kỳ mạnh mẽ mà các ông lớn công nghệ tin dùng.

- Hãy tưởng tượng Kafka Replica giống như việc bạn có nhiều bản sao của một cuốn sổ ghi chép quan trọng đặt ở các tòa nhà khác nhau; nếu một tòa nhà cháy, bạn vẫn còn bản sao ở chỗ khác để tiếp tục công việc.
- Trong Kafka, dữ liệu được chia thành các **Partition**. Mỗi Partition sẽ có nhiều bản sao (Replica) nằm trên các Broker khác nhau.
- Mất dữ liệu (Data Loss): Nếu một Broker hỏng ổ cứng, dữ liệu vẫn còn ở các Broker khác + Ngừng hoạt động (Downtime): Nếu Broker chứa Leader bị sập, Kafka sẽ tự động bầu một Follower trong ISR lên làm Leader mới ngay lập tức. Hệ thống gần như không bị gián đoạn.
- Replication là sự đánh đổi giữa Hiệu suất và Sự an toàn. Nếu bạn cần tốc độ bàn thờ và dữ liệu có mất một chút cũng không sao, bạn có thể giảm số lượng Replica. Nhưng với đa số hệ thống sản xuất (Production), con số `replication-factor = 3` là "tỉ lệ vàng".

Replica Factor là hệ số replica, cho biết số lượng bản sao (tính cả bản gốc) của Partition

![Kafka image 49](images/kafka-image-49.png)

- Ở các example local, replica factor là 1 do chỉ có 1 triển khai duy nhất của Partition; tuy nhiên trên thực tế thì Kafka work trên 1 Cluster và replica factor sẽ >= 1, thường là 2, 3 và often sẽ là 3
- Việc sử dụng Replica trong Kafka để đảm bảo rằng khi 1 Broker bị offline thì các Broker khác vẫn còn tồn tại để operation hệ thống

![Kafka image 50](images/kafka-image-50.png)

- Ở example bên trên có replica factor = 2 tức là sẽ có 1 bản sao cho tất cả các Partitions trong hệ thống, giả sử như Broker2 bị offline thì ta vẫn có thể operation vì vẫn đủ số lượng Partitions (do replica factor = 2, việc mất đi 1 Broker: 2-1 = 1 > 0 thì hệ thống vẫn work bình thường)
- Lúc này thì TopicA Partition1 sẽ trở thành Partition Leader mới do Leader cũ đã bị offline

Partition Leader là bản sao "đội trưởng". Mọi thao tác đọc (Read) và ghi (Write) từ phía Client mặc định đều đi qua Leader.

![Kafka image 51](images/kafka-image-51.png)

![Kafka image 52](images/kafka-image-52.png)

- 1 Partition cụ thể chỉ có 1 Partition Leader

Partition Follower là các bản sao "thực tập sinh". Chúng không phục vụ Client mà chỉ có nhiệm vụ duy nhất: Copy dữ liệu từ Leader để giữ mình luôn cập nhật.

- In-sync replica (ISR) là nhóm các Follower đang đuổi kịp Leader một cách sát sao. Nếu một Follower bị chậm hoặc chết, nó sẽ bị đá ra khỏi ISR.

## Log Retention + Cleanup Policy

Log còn gọi là nơi lưu trữ message trong các Partition, mỗi khi message tới thì sẽ được ghi vào cuối file mà không cần sửa đổi nội dung trước đó (append-log only).

- Kafka lưu trữ các message/ log này trên disk nên sẽ tăng đáng kể lưu lượng nên cần phải dọn dẹp.

Kafka Retention Policy  là 1 chính sách sử dụng để quyết định thời gian, kích thước tối đa cho phép để message tồn tại trong 1 Partition.

- Sử dụng khi muốn tối ưu hóa bộ nhớ Kafka để tránh chiếm nhiều dung lượng.
- Nhược điểm là nếu trong từng đó thời gian/ dung lượng mà message chưa được tiêu thụ thì vẫn sẽ mất message.
- Time based Retention tức khi đạt tới thời gian nhất định (default 7 days) thì những Segments được lưu trữ vượt quá thời gian này sẽ bị Delete/ Compact

![Kafka image 53](images/kafka-image-53.png)

- Size based Retention tức khi đạt tới kích thước nhất định của toàn bộ log của 1 Partition thì sẽ loại bỏ bớt Segments theo Delete/ Compact

![Kafka image 54](images/kafka-image-54.png)

Kafka Cleanup Policy tức chính sách mà Kafka sử dụng để xử lý message cũ trong 1 Partition, nó ảnh hưởng trực tiếp tới Kafka Retention Policy giúp kiểm soát việc xóa, giữ lại dữ liệu.

- Delete Policy (default) tức các message cũ sẽ bị xóa đi hoàn toàn khi đạt tới ngưỡng được cấu hình trong Kafka Retention Policy (thường phù hợp khi sử dụng Logging)
- Compact Policy tức sẽ xóa các message cũ đi nếu các message mới trùng với key của logs cũ + đẩy thứ tự message xuống cuối cùng của list message (tức coi như nó sẽ được tiêu thụ cuối cùng)   (thường phù hợp với trạng thái mới nhất của message mà không quan tâm tới lịch sử: Cache, CDC)

![Kafka image 55](images/kafka-image-55.png)

![Kafka image 56](images/kafka-image-56.png)

## Kafka Resolve Problem

Kafka xử lý 1 triệu message/ s: cần có chiến lược tối ưu toàn diện từ hạ tầng, producer, broker cho tới consumer:

### Tối ưu Kiến Trúc

- Mở rộng ngang (Horizontal Scaling): Để xử lý 1 triệu messages/ s, bạn sẽ cần một cụm (cluster) nhiều broker vì không một máy chủ đơn lẻ nào có thể gánh nổi khối lượng công việc này + số lượng broker cụ thể phụ thuộc vào nhiều yếu tố (kích thước message, nhân bản, ...), nhưng việc thiết kế hệ thống để có thể thêm broker một cách dễ dàng là yêu cầu tiên quyết.
- Partitions - Đơn vị của song song (Parallelism): Số lượng partition trong topic là yếu tố quyết định mức độ xử lý song song. Bạn cần đủ partitions để phân tán dữ liệu đều trên tất cả các broker, và cũng đủ để nhiều consumer có thể đọc cùng lúc. Một rule of thumb cho 1 triệu messages/s là bắt đầu với 50-100 partitions cho topic của bạn . Hãy nhớ rằng, đây là con số khởi điểm và cần được điều chỉnh dựa trên thực tế.

### Tối ưu Hạ Tầng

- Lưu trữ: Sử dụng SSD hoặc NVMe là bắt buộc để đảm bảo tốc độ đọc/ghi I/O, giảm độ trễ và tăng thông lượng. Hệ thống tệp XFS được khuyến nghị cho Kafka
- Kafka được quảng cáo là sử dụng đĩa cứng (HDD) rất hiệu quả nhờ cơ chế đọc/ghi tuần tự (sequential I/O). Tuy nhiên, khi bước vào mức hiệu năng cực cao như 1 triệu message/giây, những hạn chế cố hữu của HDD bộc lộ rõ.
- HDD: Cần thời gian để quay đĩa và di chuyển đầu đọc (seek time) ~ 5-10ms. Dù Kafka chủ yếu ghi tuần tự (append), giảm thiểu seek, nhưng các hoạt động nền như làm sạch log (log compaction), đồng bộ bộ nhớ đệm xuống đĩa (flush), hay đọc dữ liệu từ các phân đoạn cũ (nếu consumer bị tụt hậu) vẫn có thể gây ra các thao tác tìm kiếm ngẫu nhiên. Ở tần suất 1 triệu messages/s, chỉ cần một vài thao tác tìm kiếm chậm cũng đủ tạo ra độ trễ lớn và làm giảm thông lượng tổng thể.
- HDD: IOPS rất thấp, thường chỉ vài trăm. Khi có nhiều producer ghi vào các partition khác nhau, hoặc nhiều consumer đọc từ nhiều partition, hệ thống phải xử lý song song nhiều luồng I/O. HDD sẽ nhanh chóng bị quá tải.
- HDD: Băng thông tuần tự có thể đạt 150-200 MB/s.
- SSD/NVMe: Không có bộ phận chuyển động cơ học. Thời gian truy cập dữ liệu gần như tức thời và nhất quán cho cả đọc/ghi tuần tự lẫn ngẫu nhiên (thường dưới 0.1ms). Điều này đảm bảo độ trễ cực thấp và ổn định, một yếu tố sống còn cho hệ thống thời gian thực.
- SSD/NVMe: IOPS cực kỳ cao. Một ổ NVMe hiện đại có thể đạt hàng trăm ngàn, thậm chí hàng triệu IOPS. Điều này cho phép Kafka xử lý một cách thoải mái hàng ngàn kết nối producer/consumer đồng thời, mỗi kết nối thực hiện các thao tác đọc/ghi nhỏ lẻ.
- NVMe: Băng thông có thể lên tới 3000-7000 MB/s hoặc hơn. Với kích thước message trung bình 1KB, 1 triệu message tương đương với ~1GB dữ liệu. Bạn cần băng thông ghi lớn để producer ghi nhanh, và cũng cần băng thông đọc lớn để consumer đọc kịp (đặc biệt trong các tình huống bù dữ liệu hoặc nhiều consumer group đọc cùng lúc)
- Với 1 triệu messages/s, luồng dữ liệu đổ vào và đọc ra là cực kỳ lớn. HDD không thể đáp ứng được cả về tốc độ (IOPS) lẫn băng thông cần thiết, gây ra tắc nghẽn I/O, làm chậm toàn bộ hệ thống. SSD, đặc biệt là NVMe, là lựa chọn duy nhất để có đủ hiệu năng I/O cho khối lượng công việc này.
- Mạng: Băng thông mạng thường là nút thắt cổ chai đầu tiên. Hãy trang bị card mạng tốc độ cao (10GbE hoặc cao hơn). Việc Kafka 0.10 thêm 8 byte timestamp cho mỗi message đã từng làm bão hòa mạng và giảm 33% thông lượng trong một bài kiểm tra, cho thấy tầm quan trọng của việc dự phòng băng thông .
- Chúng ta đang nói về việc xử lý 1 triệu message/giây, thì Card mạng (Network Interface Card - NIC) chính là "cánh cửa" duy nhất để dữ liệu đi vào và đi ra khỏi máy chủ Kafka. Nếu "cánh cửa" này quá nhỏ, dù bên trong máy có CPU mạnh, SSD nhanh đến đâu, dữ liệu cũng không thể chui qua kịp, gây ra tắc nghẽn.
- Nói một cách đơn giản, Card mạng là phần cứng kết nối máy tính của bạn với mạng. Nó là cầu nối giao tiếp, chuyển đổi dữ liệu từ dạng byte trong máy tính thành tín hiệu để truyền qua cáp mạng (hoặc ngược lại) + Băng thông của card mạng (tính bằng Gbps - Gigabit trên giây) quyết định lượng dữ liệu tối đa có thể truyền qua mỗi giây + giảm CPU.
- CPU & RAM: CPU đa nhân giúp xử lý các tác vụ đồng thời . RAM đủ lớn giúp giảm tải I/O đĩa nhờ cơ chế page cache của Kafka.

### Tinh Chỉnh Producer

- Batching và Nén (Batching & Compression): Đây là hai kỹ thuật quan trọng nhất.
- Tăng batch.size (mặc định 16KB) lên 128KB hoặc lớn hơn để cho phép gộp nhiều message hơn trong một lần gửi, giảm số lượng request mạng .
- Tăng linger.ms (mặc định 0) lên một giá trị nhỏ như 10ms để producer chờ thêm một chút, giúp lấp đầy batch dù không vội, cân bằng giữa thông lượng và độ trễ.
- Bật nén với compression.type là lz4 hoặc zstd. Việc này giảm kích thước dữ liệu truyền tải, tiết kiệm băng thông, tiết kiệm không gian lưu trữ và tăng thông lượng hiệu dụng.
- Xử lý 1m message/ s thì tài nguyên mạng và đĩa thường là nút thắt cổ chai đầu tiên và dễ bão hòa nhất, chứ không phải CPU → đánh đổi việc nén tốn CPU nhưng lại khiến băng thông tổng thể của mạng tốt hơn.
- Tốc độ CPU vs. Tốc độ mạng/đĩa: CPU có thể nén và giải nén dữ liệu với tốc độ cực kỳ nhanh (hàng GB/s). Trong khi đó, băng thông mạng (dù là 10GbE) và tốc độ ghi đĩa có giới hạn cứng. Việc hy sinh một chút CPU để giảm tải cho mạng và đĩa là một sự đánh đổi có lợi.
- Nó giúp giảm tải cho broker: Broker nhận dữ liệu nhanh hơn, ghi đĩa nhanh hơn. Với 1 triệu message/s, việc giảm 75% dung lượng ghi xuống đĩa có thể là yếu tố sống còn để SSD không bị quá tải.
- Nó giúp toàn bộ hệ thống mạng (từ producer đến broker, giữa các broker với nhau, từ broker đến consumer) đỡ tắc nghẽn.
- Nó giúp consumer nhanh hơn: Consumer tải dữ liệu về nhanh hơn do dung lượng nhỏ hơn, và chỉ tốn thêm một chút CPU để giải nén.
- Gửi Bất đồng bộ (Asynchronous Send): Luôn sử dụng cơ chế gửi bất đồng bộ với callback để xử lý lỗi, thay vì gọi producer.send(...).get() đồng bộ làm chậm tiến trình .
- acks=1: Cung cấp sự cân bằng tốt giữa hiệu năng và độ bền dữ liệu. Chỉ cần leader ghi nhận là producer sẽ tiếp tục gửi message tiếp theo .
- max.in.flight.requests.per.connection=5: Cho phép gửi nhiều request cùng lúc trên một kết nối, tăng tốc độ .
- Mở rộng ngang (Horizontal Scaling): Đối với các hệ thống yêu cầu cực kỳ cao, bạn có thể chạy nhiều instance producer trong cùng một ứng dụng để tăng thêm mức độ song song.

### Tinh Chỉnh Broker

- Luồng Xử lý: Tăng num.network.threads và num.io.threads để broker có thể xử lý nhiều request hơn từ producer và consumer . Giá trị này phụ thuộc vào số nhân CPU và tải hệ thống, hãy bắt đầu với 8 cho network threads và 16 cho io threads .
- Cấu hình Log và Đĩa: Tăng log.segment.bytes lên 1GB để giảm số lượng tệp tin cần quản lý và tần suất thực hiện các thao tác đóng/mở file + config log.flush.interval.ms và log.flush.interval.messages ở giá trị cao để hệ điều hành tự quản lý việc ghi đệm (flush), tối ưu hiệu năng đĩa.
- Khi Kafka đóng một file segment cũ và mở một file segment mới, nó không đơn giản chỉ là "đóng" và "mở". Nó kéo theo một loạt các thao tác nặng nề, có thể gây ra hiện tượng "khựng" (hiccup) hoặc tụt hiệu năng tạm thời trong hệ thống:		 fsync() (Đồng bộ xuống đĩa): Kafka yêu cầu hệ điều hành đảm bảo toàn bộ dữ liệu trong bộ nhớ đệm (page cache) của segment đó đã được ghi thực sự xuống ổ đĩa. Đây là một thao tác I/O đồng bộ và có thể rất chậm (hàng chục tới hàng trăm mili giây) + Đóng file descriptor: Thông báo với hệ điều hành rằng không còn thao tác ghi nào nữa trên file này.
- Khi Kafka Mở một file segment mới thì mọi chuyện nặng nề hơn: Cấp phát không gian đĩa: Kafka (thông qua hệ điều hành) phải tìm một vùng không gian trống trên đĩa đủ lớn (ví dụ 1GB) để chứa file mới + Cập nhật metadata của hệ thống file: Hệ điều hành phải ghi lại thông tin về file mới này (tên file, kích thước, vị trí trên đĩa) vào cấu trúc dữ liệu của hệ thống file (inode, directory entry...). Đây là các thao tác ghi metadata + Cấp phát các block (phân mảnh): Nếu dùng ext4, việc cấp phát nhiều block nhỏ lẻ cho file lớn có thể gây phân mảnh và chậm. XFS làm tốt hơn nhưng vẫn có chi phí.
- Khi Kafka thực hiện fsync() để đóng segment cũ, nó đang yêu cầu một thao tác ghi đồng bộ xuống đĩa. Trong lúc đó, các thao tác ghi khác (cho các partition khác, cho segment hiện tại) có thể phải xếp hàng chờ đợi.

### Tinh Chỉnh Consumer

- Tăng Kích thước Fetch (Fetch Size): Cấu hình fetch.min.bytes=1048576 (1MB) và fetch.max.wait.ms=500 để consumer chỉ tải dữ liệu về khi đã có ít nhất 1MB, thay vì tải từng message nhỏ lẻ . Điều này giảm tải cho broker và mạng.

![Kafka image 57](images/kafka-image-57.png)

- Tăng Số lượng Consumer: Quy tắc cơ bản là số lượng consumer trong một group không thể vượt quá số lượng partition của topic mà chúng subscribe. Vì vậy, với 100 partitions, bạn có thể chạy tối đa 100 consumer song song .
- Xử lý Song song trong Consumer: Nếu việc xử lý message tốn thời gian, hãy xem xét việc xử lý song song trong cùng một consumer. Bạn có thể dùng một thread pool để xử lý các record từ các partition khác nhau, nhưng cần đảm bảo commit offset một cách an toàn sau khi tất cả các thread xử lý xong.

## Zookeeper

Zookeeper sử dụng để managing, maintaining các Kafka Brokers

- Nó đóng vai trò quan trọng trong Kafka, đặc biệt là: Leader election cho Partition, thông báo khi add new Topics, delete Topics, Broker up, down,...
- Từ version 2.x, Kafka muốn run bắt buộc phải có Zookeeper ; nhưng từ version 3.x đổ lên thì Kafka work mà không cần tới Zookeeper thông qua cơ chế Kafka Raft
- Zookeeper thường tổ chức dạng Cluster với 1 số lượng instance lẻ (1, 3, 5, 7) + không được vượt quá 7 Zookeeper instance
- Zookeeper Cluster sẽ có 1 Zookeeper Leader cho writing + các Zookeeper Follower cho reading

## Kafka Without Zookeeper

Từ 2020, Kafka đã được initiated mà không phụ thuộc vào Zookeeper và sáng kiến này được gọi là KIP-500 + lí do một phần nữa là Kafka Cluster khó khăn việc scaling hệ thống tới 100.000 partitions

- Bằng cách loại bỏ Zookeeper giúp handle hàng triệu partitions, đơn giản hóa việc thiết lập, nâng cao ổn định + việc quản lí security chỉ config trên Kafka + restart Kafka sẽ nhanh hơn do chỉ cần restart Kafka

![Kafka image 58](images/kafka-image-58.png)

![Kafka image 59](images/kafka-image-59.png)

![Kafka image 60](images/kafka-image-60.png)

Zookeeper là 1 open-source sử dụng để cung cấp sự điều khiển cho sự phối hợp của Distributed System thông qua 1 kho lưu trữ các key-value phân cấp
Zookeeper cung cấp các dịch vụ:	distributed config service + synchronize service + leadership election service + naming registry

- Zookeeper trong context của Kafak thì sử dụng để managing tất cả Brokers của Kafka cluster
- Data trong Kafka được divide thành các partitions + các partitions sẽ có 1 leader để chịu trách nhiệm handle read, write request cho các partitions đó + Zookeeper sẽ giúp leader election (bầu chọn leader - Paxos Algorithm)
- Zookeeper cần cài đặt trên tất cả các Node/Host của Cluster
- Zookeeper support quản lí config cho các Topics, Partitions
- Zookeeper cung cấp Distributed lock
- Zookeeper giúp send notification tới Kafka (new topic, broker die, delete topic,...)
- Kafka 2.x không thể work nếu không có Zookeeper + Kafka 3.x có thể work mà không có Zookeeper (KIP 500) mà thay vào đó sử dụng Kafka Raft + Kafka 4.x không cần tới Zookeeper
