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

**Saga** là pattern quản lý một **business transaction dài** bằng cách chia nó thành nhiều **local transaction** thuộc các service khác nhau. Mỗi local transaction commit trên database của chính service đó, sau đó phát event hoặc trả result để bước tiếp theo chạy.

Ví dụ một checkout không thể dùng một transaction ACID duy nhất bao trùm Order, Inventory, Payment và Shipping:

```text
1. Order tạo đơn PENDING
2. Inventory giữ hàng
3. Payment thu tiền
4. Shipping tạo vận đơn
5. Order chuyển sang CONFIRMED
```

Nếu bước 3 hoặc bước 4 thất bại, hệ thống không thể rollback database của mọi service như một monolith transaction. Thay vào đó, Saga chạy **compensation**:

```text
PaymentFailed sau khi InventoryReserved
  -> Inventory release hàng
  -> Order chuyển CANCELLED

ShipmentFailed sau khi PaymentSucceeded và InventoryReserved
  -> Payment refund tiền
  -> Inventory release hàng
  -> Order chuyển CANCELLED hoặc MANUAL_REVIEW
```

Điểm quan trọng: compensation không phải rollback kỹ thuật. Nó là một nghiệp vụ mới, có audit riêng và có thể thất bại riêng. Ví dụ hoàn tiền có thể pending vì cổng thanh toán lỗi; lúc đó Order không nên giả vờ đã hủy xong mà nên ở trạng thái `CANCELLING`, `REFUND_PENDING` hoặc `MANUAL_REVIEW`.

#### Vì sao cần Saga?

Trong microservices, mỗi service thường sở hữu database riêng. Điều này giúp service độc lập deploy, scale và thay đổi schema, nhưng làm transaction xuyên service trở nên khó:

- Không có một database transaction chung cho mọi service.
- Network call có thể timeout trong khi service bên kia vẫn xử lý thành công.
- Retry có thể tạo duplicate nếu không có idempotency.
- Một bước đã commit không thể bị service khác âm thầm rollback.
- Business vẫn cần một kết quả cuối cùng dễ hiểu với user.

Saga giải quyết bằng cách chấp nhận **eventual consistency**: tại một thời điểm ngắn, các service có thể chưa đồng bộ hoàn toàn, nhưng workflow có quy tắc rõ để đi tới trạng thái cuối như `COMPLETED`, `CANCELLED`, `COMPENSATED` hoặc `MANUAL_REVIEW`.

Saga có thể được triển khai bằng:

```text
Choreography-based Saga
  -> participant phản ứng với event

Orchestration-based Saga
  -> orchestrator gửi command và theo dõi result
```

Saga không đồng nghĩa với orchestration. **Saga là pattern transaction**, còn **Choreography** và **Orchestration** là hai cách điều phối Saga. Điểm chung là không dùng một ACID transaction bao trùm mọi service; failure được xử lý bằng compensation.

#### Choreography-based Saga

Trong Choreography-based Saga, không có coordinator trung tâm. Mỗi participant nghe event, tự quyết định phản ứng và phát event tiếp theo.

```text
OrderCreated
  -> Inventory giữ hàng
  -> InventoryReserved
  -> Payment thu tiền
  -> PaymentSucceeded
  -> Order xác nhận đơn
```

Khi lỗi:

```text
OrderCreated
  -> InventoryReserved
  -> PaymentFailed
  -> Inventory nghe PaymentFailed và release hàng
  -> Order nghe PaymentFailed/InventoryReleased và hủy đơn
```

Ưu điểm:

- Service tự chủ, không cần một service trung tâm biết toàn bộ flow.
- Dễ thêm side effect mới như Notification, Analytics, Loyalty bằng cách subscribe event.
- Producer ít phải sửa khi có consumer mới.
- Phù hợp với fan-out và reaction độc lập.

Nhược điểm:

- Flow bị phân tán qua nhiều subscription nên khó đọc nếu chỉ nhìn một service.
- Dễ sinh dependency ẩn: Payment phụ thuộc event của Inventory, Order phụ thuộc event của Payment, nhưng không có nơi nào thể hiện toàn bộ state machine.
- Timeout và compensation phức tạp hơn vì phải quyết định service nào sở hữu timer và service nào kết luận flow bị kẹt.
- Dễ thành event spaghetti nếu event naming, ownership, correlation và transition không rõ.

Use case phù hợp:

- Quy trình ngắn, ít bước bắt buộc.
- Các bước phản ứng tương đối độc lập.
- Side effect không ảnh hưởng kết quả chính, ví dụ gửi email, cập nhật điểm thưởng, ghi analytics.
- Hệ thống thường xuyên thêm downstream consumer mới.

Không nên dùng mặc định khi:

- Flow có nhiều nhánh business, nhiều deadline hoặc nhiều compensation phải chạy theo thứ tự.
- Cần một màn hình vận hành đọc ngay Saga đang ở state nào.
- Có manual review, retry policy phức tạp hoặc SLA nghiêm ngặt trên từng bước.

#### Orchestration-based Saga

Trong Orchestration-based Saga, một orchestrator lưu state của Saga và quyết định bước tiếp theo. Participant chỉ xử lý command thuộc domain của mình và trả result.

```text
OrderSagaOrchestrator
  -> ReserveInventory
  <- InventoryReserved
  -> ChargePayment
  <- PaymentSucceeded
  -> CreateShipment
  <- ShipmentCreated
  -> MarkOrderConfirmed
```

Khi lỗi:

```text
OrderSagaOrchestrator
  -> ReserveInventory
  <- InventoryReserved
  -> ChargePayment
  <- PaymentSucceeded
  -> CreateShipment
  <- ShipmentFailed
  -> RefundPayment
  <- PaymentRefunded
  -> ReleaseInventory
  <- InventoryReleased
  -> MarkOrderCancelled
```

Ưu điểm:

- Dễ quan sát vì trạng thái workflow nằm ở một nơi.
- Timeout, retry, deadline và compensation theo thứ tự dễ quản lý hơn.
- Dễ thay đổi trình tự bước bắt buộc mà không bắt mọi participant biết nhau.
- Phù hợp với critical path cần trạng thái rõ ràng.

Nhược điểm:

- Orchestrator có thể trở thành God Service nếu chứa domain logic thay vì chỉ điều phối.
- Thêm một component phải vận hành, lưu state, scale và recover.
- Nếu thiết kế command/result không idempotent, retry từ orchestrator có thể gây double charge, double refund hoặc release sai.
- Side effect độc lập đưa hết vào orchestrator sẽ làm workflow phình to không cần thiết.

Use case phù hợp:

- Checkout, booking, onboarding, loan approval, KYC, payout hoặc các flow có nhiều bước bắt buộc.
- Flow cần timeout rõ, ví dụ giữ vé 10 phút, thanh toán trong 15 phút, đối soát sau 1 giờ.
- Compensation cần thứ tự, ví dụ hoàn tiền trước rồi mới hủy shipment, hoặc release inventory sau khi refund thành công.
- Cần dashboard vận hành biết từng Saga đang `WAITING_PAYMENT`, `WAITING_SHIPMENT`, `COMPENSATING` hay `MANUAL_REVIEW`.

Không nên dùng cho:

- Reaction độc lập đơn giản như gửi notification sau `OrderConfirmed`.
- Fan-out analytics hoặc data warehouse ingestion.
- Flow quá nhỏ, nơi orchestrator làm tăng độ phức tạp nhiều hơn lợi ích.

#### Saga và 2PC khác nhau thế nào?

| Tiêu chí | Saga | 2PC / distributed transaction |
| --- | --- | --- |
| Cách đảm bảo | Chuỗi local transaction + compensation | Commit/rollback đồng thời qua nhiều resource |
| Consistency | Eventual consistency | Stronger atomicity ở tầng transaction |
| Khi lỗi sau khi đã commit | Chạy nghiệp vụ bù | Rollback nếu còn trong transaction |
| Thời gian giữ lock | Ngắn, theo từng local transaction | Có thể dài và ảnh hưởng availability |
| Phù hợp | Microservices, business workflow dài | Ít resource, hạ tầng hỗ trợ tốt, transaction ngắn |
| Chi phí | Cần idempotency, state, retry, compensation | Coupling cao, khó scale, nhạy với network/participant failure |

Trong hệ thống phân tán, Saga thường thực tế hơn 2PC vì không giữ lock dài và không yêu cầu mọi service/database tham gia cùng một transaction protocol. Đổi lại, developer phải thiết kế trạng thái trung gian và compensation rất rõ.

#### Trạng thái Saga nên được mô hình hóa rõ

Không nên chỉ có `SUCCESS` và `FAILED`. Một Saga production thường cần các trạng thái trung gian:

```text
PENDING
INVENTORY_RESERVED
PAYMENT_PENDING
PAYMENT_SUCCEEDED
SHIPMENT_PENDING
COMPLETED
CANCELLING
COMPENSATING_PAYMENT
COMPENSATING_INVENTORY
COMPENSATED
MANUAL_REVIEW
```

Các transition phải có luật rõ:

```text
PENDING -> INVENTORY_RESERVED -> PAYMENT_SUCCEEDED -> COMPLETED
PENDING -> INVENTORY_REJECTED -> CANCELLED
PAYMENT_SUCCEEDED -> SHIPMENT_FAILED -> COMPENSATING_PAYMENT
COMPENSATING_PAYMENT -> PAYMENT_REFUNDED -> COMPENSATING_INVENTORY
COMPENSATING_INVENTORY -> INVENTORY_RELEASED -> COMPENSATED
```

Điều này giúp xử lý duplicate và out-of-order message. Ví dụ nếu nhận lại `PaymentSucceeded` khi Saga đã `COMPLETED`, service có thể bỏ qua. Nếu nhận `PaymentSucceeded` khi Order đã `CANCELLED`, phải kiểm tra lại bằng idempotency key và có thể chuyển sang refund/manual review.

#### Các yêu cầu bắt buộc khi thiết kế Saga

- **Local transaction rõ ràng:** mỗi bước chỉ commit dữ liệu trong boundary của service đó.
- **Idempotency:** retry command/event không làm lặp side effect như charge tiền hai lần.
- **Correlation ID:** mọi message cùng Saga phải có cùng `correlationId` hoặc `sagaId`.
- **Causation ID:** biết message hiện tại được tạo ra bởi message nào để trace và debug.
- **Durable state:** trạng thái Saga hoặc trạng thái participant phải sống sót qua restart.
- **Outbox:** cập nhật database và phát event/command phải tránh dual-write problem.
- **Retry có kiểm soát:** retry với backoff, giới hạn, DLT và alert.
- **Timeout không kết luận vội:** timeout nghĩa là chưa biết kết quả; với payment/shipping cần reconcile trước khi bù.
- **Compensation idempotent:** refund, release inventory, cancel shipment đều phải chạy lại an toàn.
- **Manual review:** luôn có đường xử lý khi tự động retry/compensation không thể kết luận.

#### Ví dụ use case thực tế

Checkout thương mại điện tử:

```text
CreateOrder
ReserveInventory
ChargePayment
CreateShipment
ConfirmOrder
```

Nếu payment thất bại thì release inventory. Nếu shipment thất bại sau khi payment thành công thì refund payment và release inventory.

Booking vé:

```text
HoldSeat
CreateBooking
PayBooking
IssueTicket
```

Ghế có deadline giữ chỗ. Nếu user không thanh toán kịp, Saga hủy booking và release seat. Nếu payment thành công nhưng issue ticket lỗi, cần retry issue ticket hoặc đưa vào manual review trước khi hoàn tiền, tùy business.

Payout/withdrawal:

```text
CreateWithdrawalRequest
ReserveBalance
SendPayoutToBank
MarkCompleted
```

Nếu bank timeout, không được retry mù quáng vì có thể chuyển tiền hai lần. Saga phải reconcile theo transaction reference trước khi quyết định retry, reverse hoặc manual review.

#### Checklist chọn cách triển khai Saga

Chọn Choreography-based Saga khi:

- Flow ngắn và dễ nhìn bằng event map.
- Mỗi service phản ứng độc lập.
- Compensation ít, không cần thứ tự phức tạp.
- Muốn thêm consumer mới mà ít sửa workflow chính.

Chọn Orchestration-based Saga khi:

- Flow dài, nhiều nhánh hoặc nhiều bước bắt buộc.
- Cần state machine rõ và dashboard vận hành.
- Có timeout, retry, compensation theo thứ tự.
- Cần kiểm soát SLA và manual review.

Kết hợp cả hai khi:

```text
Critical path đặt hàng
  -> Orchestration-based Saga

Sau khi OrderConfirmed
  -> Choreography cho Notification, Analytics, Loyalty
```

Đây thường là cách cân bằng nhất: phần quyết định tiền, hàng, vận đơn có điều phối rõ; phần side effect vẫn mở rộng tự nhiên bằng event.

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

Không nên chọn Choreography hay Orchestration chỉ vì một mô hình nghe hiện đại hơn. Cách chọn đúng là nhìn vào **tính chất của từng business flow**: flow đó có bao nhiêu bước bắt buộc, lỗi phải bù thế nào, có cần một nơi nhìn thấy tiến độ không, và các reaction phía sau có thật sự ảnh hưởng tới kết quả chính không.

#### 1. Flow có bao nhiêu bước bắt buộc?

Nếu flow chỉ có một event chính rồi nhiều service phản ứng độc lập, Choreography thường tự nhiên hơn.

```text
OrderConfirmed
  -> Notification gửi email
  -> Analytics ghi dữ liệu
  -> Loyalty cộng điểm
  -> Recommendation cập nhật gợi ý
```

Trong ví dụ này, Notification lỗi không làm đơn hàng thất bại. Analytics chậm không cần chặn user. Loyalty có thể retry sau. Các service này chỉ cần biết “đơn đã được xác nhận”, không cần điều khiển thứ tự của nhau. Dùng orchestrator để gọi từng side effect sẽ làm workflow chính phình to và tạo coupling không cần thiết.

Nếu flow có nhiều bước bắt buộc, bước sau phụ thuộc kết quả bước trước, Orchestration thường rõ hơn.

```text
Checkout:
1. ReserveInventory
2. ChargePayment
3. CreateShipment
4. ConfirmOrder
```

Ở đây Payment chỉ được charge sau khi giữ hàng thành công. Shipping chỉ tạo vận đơn sau khi thanh toán thành công. Order chỉ được confirmed khi các bước chính đã xong. Đây không còn là nhiều reaction độc lập; nó là một chuỗi quyết định có thứ tự.

#### 2. Có nhiều nhánh hoặc điều kiện business không?

Flow càng nhiều nhánh, Choreography càng khó đọc vì logic nằm rải rác ở nhiều consumer.

Ví dụ checkout có các luật:

```text
Nếu hàng có sẵn
  -> reserve inventory

Nếu hàng preorder
  -> tạo preorder, chưa charge full amount

Nếu payment bằng COD
  -> bỏ qua online charge, tạo shipment

Nếu payment bằng credit card
  -> authorize trước, capture sau

Nếu khách VIP
  -> cho phép backorder

Nếu đơn giá trị cao
  -> fraud check trước shipment
```

Với Choreography, mỗi service nghe event và tự suy luận bước tiếp theo. Sau một thời gian, muốn trả lời “đơn này vì sao đi nhánh COD mà không đi nhánh credit card?” phải đọc nhiều service và nhiều subscription. Với Orchestration, các nhánh này nằm trong state machine/workflow nên dễ review hơn.

Choreography vẫn dùng được nếu nhánh ít và event có meaning rõ:

```text
PaymentSucceeded
  -> Order confirm
  -> Notification gửi receipt

PaymentFailed
  -> Order cancel
  -> Inventory release hàng
```

Nhưng khi rule bắt đầu giống một cây quyết định dài, orchestrator giúp tránh việc business flow bị “ẩn” trong nhiều consumer.

#### 3. Có deadline dài hạn không?

Deadline là dấu hiệu mạnh nên cân nhắc Orchestration, vì cần một nơi lưu “đang chờ gì” và “quá hạn thì làm gì”.

Ví dụ booking vé:

```text
SeatHeld lúc 10:00
Payment phải hoàn tất trước 10:15

Nếu 10:15 chưa có PaymentSucceeded:
  -> CancelBooking
  -> ReleaseSeat
```

Nếu dùng Choreography, phải quyết định service nào giữ timer. Order giữ? Booking giữ? Payment giữ? Một scheduler riêng nghe event? Cách nào cũng được, nhưng phải rất rõ ownership. Nếu không rõ, hệ thống dễ gặp lỗi kiểu ghế bị giữ mãi vì không service nào chịu trách nhiệm timeout.

Với Orchestration, Saga state có thể ghi:

```text
sagaId: booking-9001
state: WAITING_PAYMENT
deadline: 2026-08-29T10:15:00Z
onTimeout: CancelBooking + ReleaseSeat
```

Khi orchestrator restart, nó đọc lại state và tiếp tục xử lý các Saga quá hạn. Đây là lý do các flow có timeout dài, giữ resource hoặc cần SLA thường hợp với Orchestration.

#### 4. Một bước lỗi có cần bù các bước trước không?

Nếu lỗi chỉ làm dừng bước hiện tại, Choreography có thể đủ. Nhưng nếu lỗi bước sau buộc phải bù nhiều bước trước, cần nhìn kỹ.

Ví dụ:

```text
InventoryReserved
PaymentSucceeded
ShipmentFailed
```

Sau `ShipmentFailed`, hệ thống có thể cần:

```text
1. RefundPayment
2. ReleaseInventory
3. MarkOrderCancelled
4. NotifyCustomer
```

Nếu thứ tự bù không quan trọng, Choreography ổn:

```text
ShipmentFailed
  -> Payment refund
  -> Inventory release
  -> Order chờ đủ PaymentRefunded và InventoryReleased
```

Nếu thứ tự bù quan trọng, Orchestration rõ hơn:

```text
ShipmentFailed
  -> RefundPayment
  <- PaymentRefunded
  -> ReleaseInventory
  <- InventoryReleased
  -> CancelOrder
```

Lý do là orchestrator biết chính xác bước nào đã thành công và chỉ bù những bước đó. Nếu Payment chưa thành công thì không refund. Nếu Inventory chưa reserve thì không release. Điều này giảm lỗi compensation chạy sai trạng thái.

#### 5. Có cần biết chính xác flow đang ở đâu không?

Nếu đội vận hành cần mở dashboard và thấy ngay từng đơn đang kẹt ở đâu, Orchestration có lợi thế lớn.

Ví dụ trạng thái Saga:

```text
order-1001: WAITING_PAYMENT, retry=1, deadline=10:15
order-1002: COMPENSATING_PAYMENT, refundRequestId=R88
order-1003: MANUAL_REVIEW, reason=BANK_TIMEOUT_UNKNOWN
```

Với Choreography, vẫn quan sát được nhưng thường phải dựng từ event log, trace và state của nhiều service:

```text
OrderCreated -> InventoryReserved -> PaymentRequested
Không thấy PaymentSucceeded hoặc PaymentFailed sau 15 phút
```

Điều này không sai, nhưng chi phí observability cao hơn. Choreography cần event map, correlation ID, trace, consumer lag, DLT và dashboard tổng hợp tốt. Orchestration cần dashboard Saga state và alert cho state bị kẹt. Nói ngắn: Choreography phân tán trách nhiệm nên phải đầu tư trace; Orchestration tập trung state nên dễ hỏi “đang ở đâu?” hơn.

#### 6. Có manual review không?

Manual review là dấu hiệu flow đã vượt khỏi happy path đơn giản.

Ví dụ payout:

```text
ReserveBalance
SendPayoutToBank
Bank timeout
```

Timeout với ngân hàng không có nghĩa là chuyển tiền thất bại. Có thể ngân hàng đã nhận request, xử lý thành công, nhưng response bị mất. Nếu retry ngay, user có thể nhận tiền hai lần. Nếu reverse ngay, có thể reverse nhầm một giao dịch đang thành công.

Flow an toàn hơn:

```text
BankTimeout
  -> ReconcileByTransactionReference
  -> nếu bank confirmed: MarkCompleted
  -> nếu bank rejected: ReleaseBalance
  -> nếu bank unknown quá lâu: MANUAL_REVIEW
```

Orchestration phù hợp vì có nơi lưu trạng thái `WAITING_RECONCILIATION` hoặc `MANUAL_REVIEW`. Choreography vẫn làm được, nhưng cần một service sở hữu rõ việc reconcile và kết luận cuối cùng.

#### 7. Reaction có độc lập với kết quả chính không?

Đây là câu hỏi quan trọng để tránh đưa quá nhiều thứ vào orchestrator.

Ví dụ sau khi đơn hàng confirmed:

```text
OrderConfirmed
  -> gửi email
  -> gửi push notification
  -> ghi analytics
  -> cập nhật search index
  -> cộng loyalty point
```

Nếu email lỗi, đơn hàng vẫn confirmed. Nếu analytics delay, user không cần chờ. Những việc này nên là Choreography vì chúng là side effect độc lập. Producer chỉ phát `OrderConfirmed`; service nào quan tâm thì tự xử lý.

Ngược lại, nếu một reaction quyết định trạng thái chính, nó không còn là side effect:

```text
FraudCheckPassed
  -> mới được capture payment

FraudCheckRejected
  -> hủy order và release inventory
```

Fraud check ảnh hưởng trực tiếp tới flow checkout, nên thường thuộc critical path. Critical path dài và có điều kiện như vậy nên được orchestration hoặc ít nhất phải có owner state rất rõ.

#### 8. Thêm consumer mới có thường xuyên không?

Nếu yêu cầu thường xuyên là “khi X xảy ra, thêm một service nữa xử lý”, Choreography giúp mở rộng tốt.

Ví dụ ban đầu:

```text
OrderConfirmed
  -> Notification
```

Sau đó thêm:

```text
OrderConfirmed
  -> Notification
  -> Analytics
  -> Loyalty
  -> CRM
  -> Data Warehouse
```

Order Service không cần biết tất cả consumer này. Nó chỉ phát event có contract ổn định. Đây là lợi thế lớn của event-driven architecture.

Nhưng nếu “thêm consumer” thực chất là thêm một bước bắt buộc vào flow, cần cẩn thận.

```text
Trước đây:
ReserveInventory -> ChargePayment -> ConfirmOrder

Sau này:
ReserveInventory -> FraudCheck -> ChargePayment -> ConfirmOrder
```

`FraudCheck` không phải consumer phụ; nó thay đổi thứ tự nghiệp vụ. Nếu dùng Choreography, phải sửa nhiều subscription và đảm bảo Payment không chạy trước khi Fraud passed. Với Orchestration, thay đổi này thường là sửa state machine rõ ràng hơn.

#### Gợi ý quyết định nhanh

```text
Reaction độc lập, fan-out, thêm consumer thường xuyên
  -> ưu tiên Choreography

Flow bắt buộc, dài, nhiều nhánh, có timeout hoặc compensation
  -> cân nhắc Orchestration

Critical path phức tạp nhưng có nhiều side effect độc lập
  -> Orchestration cho critical path
  -> Choreography cho side effect
```

Ví dụ kết hợp:

```text
Checkout critical path:
OrderSagaOrchestrator
  -> ReserveInventory
  -> FraudCheck
  -> ChargePayment
  -> CreateShipment
  -> ConfirmOrder

Sau khi OrderConfirmed:
OrderConfirmed
  -> Notification
  -> Analytics
  -> Loyalty
  -> Data Warehouse
```

#### Cách hiểu ngắn nhưng đầy đủ

Choreography giống câu:

```text
“Có việc X đã xảy ra; service nào quan tâm thì tự phản ứng.”
```

Ví dụ `OrderConfirmed` là một sự thật nghiệp vụ đã xảy ra. Notification nghe để gửi email. Analytics nghe để ghi dữ liệu. Loyalty nghe để cộng điểm. Order Service không ra lệnh trực tiếp cho từng service, cũng không cần biết sau này sẽ có thêm CRM hay Data Warehouse. Vì vậy Choreography tối ưu cho autonomy, fan-out và reaction độc lập.

Orchestration giống câu:

```text
“Workflow đang ở bước X; service Y hãy làm việc Z,
sau đó trả kết quả để tôi quyết định bước tiếp theo.”
```

Ví dụ Order Saga đang ở `WAITING_PAYMENT`, orchestrator gửi `ChargePayment` cho Payment Service. Nếu nhận `PaymentSucceeded`, nó gửi tiếp `CreateShipment`. Nếu nhận `PaymentFailed`, nó gửi `ReleaseInventory` rồi hủy đơn. Participant không cần biết toàn bộ flow; orchestrator giữ state và quyết định bước kế tiếp. Vì vậy Orchestration tối ưu cho visibility, trình tự bắt buộc, timeout và compensation.

Một hệ thống tốt không cố dùng duy nhất một mô hình cho mọi thứ. Nó chọn theo từng phần của business flow: phần quyết định tiền, hàng, booking, payout thường cần state rõ và bù lỗi cẩn thận; phần notification, analytics, cache invalidation, search indexing thường nên phản ứng tự do qua event.

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

CRUD bình thường lưu **kết quả cuối cùng**.

```text
account.balance = 800.000
```

Nhìn vào đây chỉ biết tài khoản còn 800.000, nhưng không biết vì sao. Có thể trước đó user nạp tiền, rút tiền, được hoàn tiền hoặc bị trừ phí.

Event Sourcing lưu **những việc đã xảy ra**, rồi tính state hiện tại từ các event đó.

```text
AccountOpened(0)
MoneyDeposited(+1.000.000)
MoneyWithdrawn(-200.000)
--------------------------
Balance = 800.000
```

Ý tưởng rất đơn giản:

```text
State hiện tại = kết quả cộng dồn các event đã xảy ra
```

Ví dụ khi user muốn rút 200.000:

```text
1. Load các event của account
2. Tính ra balance hiện tại = 800.000
3. Kiểm tra 800.000 có đủ rút 200.000 không
4. Nếu đủ, append event MoneyWithdrawn(-200.000)
5. Balance mới = 600.000
```

Điểm khác biệt là app không ghi kiểu:

```text
UPDATE account SET balance = 600.000
```

Mà ghi:

```text
MoneyWithdrawn(-200.000)
```

Rồi từ lịch sử đó suy ra balance mới.

##### Vì sao cách này hữu ích?

Vì event cho biết **lý do state thay đổi**.

```text
CRUD:
balance = 600.000

Event Sourcing:
MoneyDeposited(+1.000.000)
MoneyWithdrawn(-200.000)
MoneyWithdrawn(-200.000)
=> balance = 600.000
```

Với CRUD, chỉ nhìn state cuối. Với Event Sourcing, đọc được cả câu chuyện: tài khoản từng nạp bao nhiêu, rút mấy lần, lúc nào thay đổi.

Ví dụ order:

```text
OrderCreated
InventoryReserved
PaymentSucceeded
OrderConfirmed
```

Nếu order bị hủy:

```text
OrderCreated
InventoryReserved
PaymentFailed
InventoryReleased
OrderCancelled
```

Nhìn chuỗi event là biết order hủy vì payment fail, và inventory đã được release. Nếu chỉ nhìn `order.status = CANCELLED`, ta mất nhiều ngữ cảnh hơn.

##### Projection là gì?

Nếu mỗi lần mở màn hình số dư đều replay toàn bộ event từ đầu thì chậm. Vì vậy thường tạo thêm **projection** hoặc **read model**: một bản dữ liệu đã được tính sẵn để phục vụ query nhanh.

Cách triển khai ở mức concept:

1. **Event store** lưu lịch sử event gốc, ví dụ tài khoản đã nạp tiền, rút tiền, hoàn tiền.
2. Một thành phần gọi là **projector** hoặc **projection consumer** đọc các event này theo thứ tự.
3. Projector cập nhật một bảng/view đọc nhanh, ví dụ bảng số dư hiện tại của từng tài khoản.
4. Màn hình/API đọc từ projection thay vì replay event từ đầu.
5. Projection lưu lại đã xử lý tới event nào để khi restart có thể chạy tiếp, không xử lý lại sai.

Ví dụ dễ hiểu: event log lưu toàn bộ lịch sử giao dịch như `MoneyDeposited`, `MoneyWithdrawn`, `MoneyRefunded`. Projection lưu sẵn số dư hiện tại đã tính từ các event đó. Khi user mở app, hệ thống đọc ngay số dư từ projection. Khi có giao dịch mới, projector đọc event mới và cập nhật lại số dư trong projection.

Event log vẫn là nguồn chính. Projection chỉ là bản phụ để đọc nhanh. Nếu projection bị sai hoặc mất, có thể xóa projection rồi replay event từ đầu để dựng lại.

Một event log có thể tạo nhiều projection khác nhau. Cùng lịch sử giao dịch tài khoản có thể tạo bảng số dư hiện tại, sao kê giao dịch, báo cáo dòng tiền hoặc view phục vụ fraud/risk. Mỗi projection phục vụ một kiểu đọc khác nhau, nhưng đều lấy dữ liệu từ cùng một lịch sử event.

Điểm cần cẩn thận: projector phải xử lý event theo cách idempotent. Nếu cùng một event bị đọc lại sau retry hoặc restart, projection không được cộng tiền hai lần. Thường projection sẽ lưu `eventId`, `sequence` hoặc vị trí đã xử lý để biết event nào đã apply rồi.

##### Snapshot là gì?

Nếu một tài khoản có quá nhiều event, replay từ đầu sẽ chậm. Snapshot là bản chụp state tại một thời điểm để replay nhanh hơn.

```text
Snapshot ở event số 10.000:
balance = 20.000.000

Sau đó chỉ cần replay event 10.001 trở đi
```

Snapshot giống cache/checkpoint. Nó không thay thế event gốc.

##### Ưu điểm

- Có lịch sử đầy đủ, rất tốt cho audit.
- Biết vì sao state hiện tại có giá trị như vậy.
- Có thể dựng lại state ở quá khứ.
- Có thể replay event để tạo projection/báo cáo mới.
- Phù hợp với nghiệp vụ tiền, order, booking, workflow nhiều trạng thái.

##### Nhược điểm và chi phí

- Phức tạp hơn CRUD nhiều.
- Event đã lưu rồi thì khó sửa, vì nó là lịch sử.
- Replay nhiều event có thể chậm, nên cần snapshot/projection.
- Schema event phải giữ tương thích lâu dài.
- Không được replay side effect bừa bãi. Replay để dựng view thì được, nhưng không được gửi email hoặc charge tiền lại.
- Dữ liệu nhạy cảm/PII phải thiết kế cẩn thận vì event log thường sống rất lâu.

##### Khi nào nên dùng?

Nên dùng khi lịch sử là phần quan trọng của nghiệp vụ:

- Ví, tài khoản, ledger, giao dịch tiền.
- Order/booking có nhiều bước và compensation.
- Hệ thống cần audit mạnh.
- Cần biết state tại một thời điểm trong quá khứ.
- Cần tạo nhiều read model từ cùng một lịch sử event.

Không nên dùng mặc định cho CRUD đơn giản:

```text
Product name/description
User profile
CMS article
Setting/config đơn giản
```

Các dữ liệu này thường chỉ cần state hiện tại, audit log đơn giản hoặc version history là đủ.

##### Kafka có phải Event Sourcing không?

Không. Kafka có log và replay, nhưng chỉ dùng Kafka không có nghĩa là Event Sourcing.

Không phải Event Sourcing:

```text
Update database trước
Sau đó publish event lên Kafka
```

Event Sourcing đúng nghĩa:

```text
Append event trước
State/read model được dựng từ event
```

Tóm lại: Event Sourcing là cách lưu dữ liệu bằng lịch sử event. Nó rất mạnh khi cần audit và replay, nhưng không nên dùng cho mọi thứ vì làm hệ thống phức tạp hơn CRUD.

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

## Kafka Concept

🙂 Apache Kafka là một **distributed event streaming platform**. Nói dễ hiểu: Kafka là hạ tầng trung gian để nhiều hệ thống có thể **ghi event**, **lưu event**, rồi **đọc event** theo nhu cầu riêng.

Kafka không chỉ chuyển message từ A sang B. Kafka giữ event trong một khoảng thời gian theo cấu hình, nên consumer có thể đọc ngay, đọc chậm hơn producer, hoặc đọc lại dữ liệu cũ nếu record vẫn còn trong retention.

Ví dụ: Order Service ghi event `OrderCreated` vào Kafka. Payment Service đọc để xử lý thanh toán. Analytics Service đọc để làm báo cáo. Notification Service đọc để gửi thông báo. Các consumer này không lấy mất event của nhau, vì mỗi nhóm consumer có vị trí đọc riêng.

### “Stream” có nghĩa là gì?

**Stream** là dòng event xuất hiện liên tục theo thời gian. Khi hệ thống còn chạy thì event mới vẫn tiếp tục sinh ra.

Ví dụ: user click sản phẩm, tạo order, thanh toán thành công, tài xế gửi vị trí, database có thay đổi mới. Mỗi việc như vậy có thể trở thành một event trong stream.

Kafka gọi là event streaming platform vì nó không chỉ nhận event tại một thời điểm, mà còn lưu và phân phối cả dòng event liên tục đó cho nhiều hệ thống đọc.

### Mental model tổng quan

Các khái niệm chính:

- **Producer**: ứng dụng ghi event vào Kafka.
- **Record/event**: dữ liệu được ghi vào Kafka, thường mô tả một việc đã xảy ra.
- **Topic**: luồng event có tên, ví dụ `order-events` hoặc `payment-events`.
- **Partition**: phần chia nhỏ của topic để Kafka xử lý song song và giữ thứ tự theo một key.
- **Offset**: vị trí của một record trong partition.
- **Consumer**: ứng dụng đọc event từ Kafka.
- **Consumer group**: nhiều instance cùng chia nhau việc đọc topic.
- **Broker**: server Kafka lưu dữ liệu.
- **Replication**: tạo bản sao dữ liệu trên nhiều broker để chịu lỗi.
- **Retention**: chính sách giữ record trong bao lâu hoặc giữ tối đa bao nhiêu dung lượng.

Ví dụ ngắn: topic `order-events` có các event về order. Payment Service đọc để thanh toán. Analytics Service đọc để thống kê. Nếu Analytics dừng một lúc, Payment vẫn chạy bình thường. Khi Analytics chạy lại, nó đọc tiếp từ vị trí cũ nếu dữ liệu còn được Kafka giữ.

### Log-based khác queue truyền thống ở điểm nào?

Với nhiều queue truyền thống, message thường biến mất khỏi queue sau khi được xử lý. Với Kafka, record không biến mất chỉ vì một consumer đã đọc. Record được giữ theo retention.

Điều này tạo ra khác biệt lớn:

- Nhiều consumer group có thể đọc cùng một topic độc lập.
- Consumer đọc chậm có thể xử lý backlog sau.
- Có thể replay event cũ để dựng lại projection, search index hoặc báo cáo.
- Producer không cần biết có bao nhiêu hệ thống phía sau đang đọc event.

Nhưng Kafka không giữ dữ liệu mãi mãi nếu retention không cấu hình như vậy. Nếu consumer dừng quá lâu và dữ liệu cũ đã bị xóa, consumer có thể không đọc lại được phần đã mất.

### Kafka phù hợp khi nào?

- Một event cần nhiều hệ thống độc lập cùng đọc.
- Lượng event lớn và cần throughput cao.
- Consumer có thể chậm hoặc downtime tạm thời rồi đọc tiếp.
- Cần replay dữ liệu cũ để rebuild view, backfill hoặc phân tích.
- Cần xử lý stream gần realtime như tracking, analytics, CDC, monitoring, fraud detection.

Ví dụ: hệ thống thương mại điện tử phát event `OrderConfirmed`. Payment, Notification, Analytics, Warehouse và Loyalty có thể đọc cùng event đó cho mục đích riêng.

### Kafka không phù hợp khi nào?

- Chỉ cần gọi request-response và cần kết quả ngay.
- Chỉ cần chạy job đơn giản, cần priority, delay từng message hoặc routing phức tạp.
- Dữ liệu chủ yếu là CRUD/query linh hoạt như database quan hệ.
- Hệ thống chưa có idempotency, schema contract, monitoring và retry rõ ràng.
- Team nghĩ Kafka sẽ tự giải quyết distributed transaction, Saga hoặc exactly-once với mọi hệ thống bên ngoài.

### Đánh đổi cần nhớ

- Lưu event để replay được thì tốn storage và chi phí vận hành.
- Partition giúp scale nhưng ordering chỉ chắc trong từng partition.
- Consumer có thể đọc lại nên phải xử lý duplicate/idempotency.
- Bất đồng bộ giúp decouple nhưng làm hệ thống eventual consistency và khó trace hơn.
- Kafka mạnh về vận chuyển/lưu event; business correctness vẫn do application thiết kế.

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

Kafka ghi record mới bằng cách **thêm vào cuối log** của partition. Nó không update record cũ tại nhiều vị trí khác nhau như một database thường làm.

Ví dụ topic order có nhiều event mới: `OrderCreated`, `PaymentSucceeded`, `OrderShipped`. Kafka chỉ cần ghi tiếp các event mới vào cuối partition. Cách ghi này đơn giản và đều đặn hơn so với việc phải tìm đúng row cũ rồi update tại nhiều vị trí trên disk.

Vì ghi và đọc chủ yếu đi theo thứ tự, Kafka tận dụng tốt sequential I/O. Trên disk, đọc/ghi tuần tự thường hiệu quả hơn đọc/ghi rải rác vì hệ thống phải di chuyển và tìm vị trí ít hơn. Ngay cả với SSD, ghi theo luồng lớn và đều vẫn có lợi vì giảm overhead và dễ batch hơn.

Lợi ích chính:

- Ghi nhanh hơn vì chủ yếu append vào cuối.
- Dễ gom nhiều record nhỏ thành một lần ghi lớn.
- Consumer thường đọc tiếp từ vị trí đang đọc, nên pattern đọc cũng tuần tự.
- Hệ điều hành dễ cache và đọc trước dữ liệu gần vị trí hiện tại.

Điểm cần nhớ: ý này không có nghĩa disk nhanh hơn RAM. Ý đúng là Kafka tránh kiểu ghi rải rác, tận dụng pattern đọc/ghi tuần tự để đạt throughput cao.

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

Kafka nén dữ liệu theo **record batch**. Producer thường gom nhiều record lại trước khi gửi, sau đó nén cả batch bằng codec như `gzip`, `snappy`, `lz4` hoặc `zstd`.

Nén theo batch thường hiệu quả hơn nén từng record riêng lẻ, vì codec có nhiều dữ liệu hơn để tìm phần lặp. Ví dụ nhiều event click, order hoặc log thường có format gần giống nhau; khi gom lại thành batch, dữ liệu có nhiều pattern lặp nên nén tốt hơn.

Kafka không cần hiểu nội dung event là JSON, Avro hay Protobuf. Với Kafka, record là dữ liệu dạng bytes; phần nén do codec xử lý trên bytes của cả batch.

Record batch được gửi qua network, lưu trên broker và replicate ở dạng compressed khi cấu hình phù hợp. Consumer decompress khi đọc để application xử lý payload.

Lợi ích:

- Giảm network bandwidth producer -> broker.
- Giảm dung lượng storage.
- Giảm bandwidth khi replicate giữa broker.
- Giảm bandwidth broker -> consumer.

Đánh đổi:

- Producer và consumer tốn CPU để compress/decompress.
- Batch nhỏ thường nén kém hiệu quả.
- Chọn thuật toán phụ thuộc ưu tiên CPU, ratio, throughput và compatibility.

Kafka hỗ trợ các codec như `gzip`, `snappy`, `lz4`, `zstd`. Cách chọn thường gặp:

- **`lz4`**: lựa chọn phổ biến khi cần throughput cao và latency thấp. Nén/giải nén nhanh, ratio khá tốt, hợp với event realtime, log, tracking, order event.
- **`zstd`**: hợp khi muốn nén tốt hơn để giảm network/storage, nhưng vẫn giữ hiệu năng tốt. Phù hợp với payload lớn, traffic nhiều, hoặc chi phí băng thông/storage quan trọng.
- **`snappy`**: ưu tiên nhẹ CPU và tốc độ, nhưng ratio thường không tốt bằng `zstd`/`gzip`. Dùng khi hệ thống đã dùng sẵn Snappy hoặc workload cần nén nhanh hơn là nén sâu.
- **`gzip`**: ratio tốt nhưng tốn CPU và chậm hơn, thường không phải lựa chọn mặc định cho pipeline realtime throughput cao. Hợp hơn với dữ liệu cần nén mạnh và latency không quá nhạy.
- **`none`**: dùng khi payload rất nhỏ, dữ liệu đã được nén sẵn, CPU đang là bottleneck, hoặc môi trường dev/test chưa cần tối ưu network/storage.

Quy tắc nhanh: bắt đầu với `lz4` nếu ưu tiên tốc độ, thử `zstd` nếu muốn giảm dung lượng nhiều hơn, tránh chọn `gzip` cho realtime path nếu chưa benchmark.

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

Kafka consumer chủ động gửi fetch request và chỉ rõ offset muốn đọc. Broker trả về một vùng record liên tiếp. Broker vẫn lưu offset commit của Consumer Group trong `__consumer_offsets`, nhưng không phải lưu trạng thái giao nhận riêng cho từng record của từng consumer.

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

RabbitMQ và Kafka đều giúp các hệ thống giao tiếp bất đồng bộ, nhưng chúng tối ưu cho hai kiểu bài toán khác nhau.

- **RabbitMQ** giống message broker truyền thống hơn: nhận message, route message vào queue, đẩy message cho consumer xử lý, consumer xử lý xong thì ack.
- **Kafka** giống event log hơn: producer ghi record vào topic, Kafka giữ record theo retention, consumer group tự đọc theo offset của mình.

| Tiêu chí | RabbitMQ | Kafka |
| --- | --- | --- |
| Mục tiêu chính | Giao message/job tới consumer | Lưu và phân phối dòng event |
| Cách consumer nhận dữ liệu | Broker thường đẩy message tới consumer | Consumer chủ động kéo dữ liệu |
| Sau khi consume | Message thường được xóa sau ack | Record vẫn còn đến khi hết retention |
| Nhiều hệ thống cùng đọc | Cần thiết kế exchange/queue phù hợp | Nhiều consumer group đọc độc lập tự nhiên |
| Replay dữ liệu cũ | Không phải thế mạnh chính | Là use case rất mạnh nếu còn retention |
| Routing | Rất mạnh với exchange/routing key | Đơn giản hơn, chủ yếu theo topic/key/partition |
| Scale throughput | Scale theo queue/consumer, có giới hạn theo kiểu queue | Scale mạnh bằng partition và consumer group |
| Use case hợp | Task queue, routing phức tạp, command/job | Event stream, analytics, CDC, log, fan-out, replay |

### 1. Push và pull model

RabbitMQ thường theo hướng **push**: broker gửi message xuống consumer khi có message mới. Consumer ack sau khi xử lý xong. Nếu consumer xử lý chậm, RabbitMQ có cơ chế QoS/prefetch để giới hạn số message đang giao nhưng chưa ack.

Kafka theo hướng **pull**: consumer chủ động hỏi broker để lấy thêm record. Consumer có thể lấy theo batch và tự điều chỉnh tốc độ đọc theo khả năng xử lý.

Ví dụ: service gửi email xử lý từng job nhỏ có thể hợp với RabbitMQ. Pipeline analytics đọc hàng nghìn event mỗi lần để aggregate thường hợp với Kafka hơn.

### 2. Throughput

RabbitMQ phải quản lý trạng thái message theo vòng đời queue: message đang chờ, đã giao cho consumer, đã ack hay cần redeliver. Cách này rất hợp với job/message cần xử lý riêng lẻ, nhưng broker phải theo dõi nhiều trạng thái hơn.

Kafka tối ưu cho throughput lớn bằng cách ghi record nối tiếp vào log, đọc theo offset, batch dữ liệu, nén theo batch và chia topic thành nhiều partition. Kafka ít quản lý trạng thái giao nhận riêng cho từng consumer trên broker hơn; consumer group theo dõi vị trí đọc của mình bằng offset.

Ví dụ: xử lý vài nghìn background job có retry/routing rõ ràng có thể dùng RabbitMQ tốt. Lưu hàng trăm nghìn click event mỗi giây cho nhiều pipeline đọc song song thường hợp Kafka hơn.

### 3. Scalability

RabbitMQ scale tốt bằng nhiều queue, nhiều consumer, cluster và cơ chế routing. Tuy nhiên một queue cụ thể vẫn có giới hạn riêng, nhất là khi cần giữ ordering hoặc có nhiều message chưa ack.

Kafka scale bằng **partition**. Một topic có thể chia thành nhiều partition, các partition nằm trên nhiều broker, và nhiều consumer trong cùng group chia nhau đọc partition. Càng nhiều partition hợp lý, Kafka càng có nhiều đơn vị để xử lý song song.

Ví dụ: nếu cần giữ thứ tự theo từng order, Kafka có thể dùng `orderId` làm key để event của cùng order đi cùng partition. Các order khác nhau vẫn có thể xử lý song song trên partition khác.

### 4. Persistence và replay

RabbitMQ chủ yếu tối ưu cho việc giao message tới consumer. Khi consumer ack, message thường được loại khỏi queue. RabbitMQ có persistent message, nhưng mục tiêu chính vẫn không phải giữ lịch sử event lâu dài để nhiều hệ thống replay.

Kafka giữ record theo retention, độc lập với việc consumer đã đọc hay chưa. Payment Service đọc xong không làm Analytics mất record. Nếu Analytics dừng một lúc, nó có thể đọc tiếp từ offset cũ khi chạy lại, miễn record vẫn còn trong retention.

Ví dụ: nếu cần rebuild search index từ event `ProductUpdated` trong 3 ngày gần nhất, Kafka phù hợp hơn. Nếu chỉ cần giao job “resize ảnh này” cho một worker xử lý xong rồi bỏ, RabbitMQ thường đơn giản hơn.

### 5. Routing

RabbitMQ mạnh về routing. Producer gửi message vào exchange, exchange dựa trên routing key, pattern hoặc rule để đưa message vào queue phù hợp. Điều này hợp với hệ thống cần định tuyến message linh hoạt.

Kafka routing đơn giản hơn. Producer ghi vào topic; nếu topic có nhiều partition thì key quyết định record vào partition nào. Kafka không có exchange routing phong phú như RabbitMQ. Thế mạnh của Kafka nằm ở event stream, lưu trữ, replay và consumer group độc lập.

Ví dụ: nếu message `payment.failed.vip.eu` cần route tới nhiều queue theo rule khác nhau, RabbitMQ rất hợp. Nếu mọi service quan tâm tới `payment-events` tự đọc và xử lý theo nhu cầu riêng, Kafka hợp hơn.

### Chọn nhanh

Chọn RabbitMQ khi:

- Cần task queue/job queue đơn giản.
- Cần routing linh hoạt, priority, delay, retry theo message.
- Message xử lý xong thì không cần giữ lại lâu.
- Mỗi message thường chỉ cần một nhóm worker xử lý.

Chọn Kafka khi:

- Cần nhiều hệ thống đọc cùng một event độc lập.
- Cần throughput cao và xử lý theo batch.
- Cần lưu event trong một khoảng thời gian để replay.
- Cần stream processing, CDC, analytics, audit pipeline hoặc event-driven fan-out.

Tóm lại: RabbitMQ mạnh ở **giao và route message để xử lý công việc**. Kafka mạnh ở **lưu và phân phối dòng event cho nhiều hệ thống đọc độc lập**.

## Broker + Topic + Partitions + Segment + Offset

- 1 Cluster = n Brokers (node)
- 1 Broker = n Partitions
- 1 Topic = n Partitions.
- 1 Partition = n Segments
- 1 Segments = n Offsets

![Kafka image 7](images/kafka-image-07.png)

![Kafka image 8](images/kafka-image-08.png)

### Broker

Broker là một tiến trình Kafka chạy trên máy chủ vật lý, máy ảo hoặc container. Một hệ thống Kafka production thường có nhiều Broker kết hợp lại thành một **Kafka Cluster**.

Broker chịu trách nhiệm chính cho các việc sau:

- Nhận message từ Producer.
- Lưu message xuống disk theo từng Partition.
- Trả dữ liệu cho Consumer khi Consumer gửi request đọc.
- Quản lý các Partition đang nằm trên Broker đó.
- Phối hợp với Broker khác để replication, leader election và cân bằng dữ liệu trong Cluster.

Mỗi Broker trong Cluster có một **broker id** riêng để phân biệt với các Broker khác.

Ví dụ:

```text
Kafka Cluster
- Broker 1
- Broker 2
- Broker 3
```

Nếu Topic `order-events` có 3 Partition, Kafka có thể phân tán các Partition lên nhiều Broker:

```text
order-events / Partition 0 -> Broker 1
order-events / Partition 1 -> Broker 2
order-events / Partition 2 -> Broker 3
```

Cách phân tán này giúp dữ liệu không bị dồn vào một máy duy nhất. Khi tải tăng, hệ thống có thể scale bằng cách thêm Broker và phân bổ thêm Partition.

Broker lưu message và Kafka cũng lưu offset của Consumer Group trong Topic nội bộ `__consumer_offsets`. Tuy nhiên, Kafka không lưu trạng thái theo kiểu "message này đã được Consumer A xử lý chưa" cho từng message riêng lẻ. Thay vào đó, Kafka chỉ lưu vị trí đã đọc tới đâu theo **Consumer Group + Topic + Partition**.

```text
Topic: order-events
Partition 0 có các message:
offset 0, 1, 2, 3, 4, 5, 6, 7

Consumer Group: order-service
Đã xử lý xong tới offset 4

Kafka lưu offset đã commit:
group = order-service
topic = order-events
partition = 0
committed_offset = 5
```

Nghĩa là lần sau `order-service` đọc tiếp từ offset `5`. Kafka không cần lưu từng dòng kiểu `message 0 đã ack chưa`, `message 1 đã ack chưa`, `message 2 đã ack chưa` cho từng Consumer.

Điểm này giúp Kafka có hiệu năng tốt hơn vì metadata cần quản lý nhỏ hơn nhiều. Broker vẫn lưu offset, nhưng offset là một con số đại diện cho vị trí đọc của một Consumer Group trên một Partition, không phải trạng thái giao nhận riêng của từng message.

Trong Kafka dùng **KRaft**, metadata của Cluster được quản lý bởi một nhóm controller gọi là **controller quorum**. Tại một thời điểm sẽ có một controller đang active để điều phối metadata.

Controller chịu trách nhiệm:

- Theo dõi Broker nào còn hoạt động, Broker nào bị lỗi.
- Điều phối leader election cho Partition.
- Cập nhật metadata của Cluster.
- Quản lý thay đổi như tạo Topic, xóa Topic, thay đổi Partition hoặc Replica assignment.

Nếu Broker đang giữ Leader của một Partition bị lỗi, active controller sẽ chọn một Replica phù hợp để làm Leader mới.

Nếu chính active controller bị lỗi, controller quorum sẽ bầu một active controller mới từ các controller node còn sống. Trong thời gian chuyển active controller, các thao tác liên quan tới metadata như leader election, tạo/xóa Topic hoặc cập nhật metadata cho client có thể chậm lại trong thời gian ngắn. Tuy nhiên, Kafka không phụ thuộc vĩnh viễn vào một controller cố định.

Tóm gọn:

```text
Partition Leader lỗi
  -> active controller chọn Leader mới cho Partition

Active controller lỗi
  -> controller quorum bầu active controller mới
```

![Kafka image 9](images/kafka-image-09.png)

Producer hoặc Consumer không cần biết toàn bộ chi tiết Cluster ngay từ đầu. Chỉ cần kết nối được tới một Broker trong danh sách bootstrap server, client có thể lấy metadata để biết Topic có những Partition nào, Partition nào đang do Broker nào làm Leader, sau đó kết nối tới Broker cần thiết.

Ví dụ:

```text
Client connect Broker 1
  -> lấy metadata Cluster
  -> biết Partition 2 của order-events đang có Leader ở Broker 3
  -> gửi request đọc/ghi tới Broker 3 khi cần
```

![Kafka image 10](images/kafka-image-10.png)

### Topic

Topic là nơi Kafka dùng để phân loại và lưu trữ message/event theo mục đích sử dụng. Producer ghi message vào Topic, Consumer đọc message từ Topic.

Ví dụ thực tế:

```text
Topic: user-events
- user_registered
- user_logged_in
- user_updated_profile

Topic: payment-events
- payment_created
- payment_success
- payment_failed
```

Không nên trộn nhiều loại dữ liệu không liên quan vào cùng một Topic. Nếu `user-events`, `payment-events`, `order-events` bị gom chung vào một Topic, Consumer sẽ phải tự lọc nhiều dữ liệu không cần thiết và contract của Topic khó quản lý hơn.

Topic có một số đặc điểm quan trọng:

- Topic chứa một hoặc nhiều Partition.
- Message mới được ghi thêm vào log, không update trực tiếp message cũ.
- Message trong Kafka là bytes, payload có thể là JSON, Avro, Protobuf, text hoặc binary.
- Một Topic có thể có nhiều Producer ghi vào và nhiều Consumer Group đọc ra.
- Consumer đọc xong không làm message biến mất ngay; message được giữ theo retention/cleanup policy.

Topic có thể hơi giống database table ở chỗ đều chứa nhiều record, nhưng không nên hiểu Topic là database table.

| Điểm so sánh | Database table | Kafka Topic |
| --- | --- | --- |
| Mục tiêu chính | Lưu trạng thái hiện tại và hỗ trợ query | Lưu dòng event/message |
| Cách đọc | Query linh hoạt bằng SQL hoặc API | Consumer đọc theo offset |
| Constraint | Có thể có primary key, foreign key, unique | Không có constraint kiểu database |
| Update dữ liệu | Có thể update/delete row | Chủ yếu append message mới |
| Use case | Tra cứu trạng thái hiện tại | Truyền event, stream processing, replay |

Ví dụ database phù hợp cho câu hỏi:

```sql
SELECT * FROM users WHERE id = 10;
```

Kafka Topic phù hợp cho luồng xử lý:

```text
User đăng ký thành công
  -> Producer gửi user_registered vào user-events
  -> Email Service đọc để gửi email
  -> Analytics Service đọc để ghi nhận signup
  -> Recommendation Service đọc để khởi tạo profile
```

### Partition

Partition là phần nhỏ hơn của một Topic. Một Topic có thể có một hoặc nhiều Partition. Mỗi Partition là một log có thứ tự riêng; message mới được append vào cuối Partition.

Nếu Topic chỉ có một Partition:

```text
Topic: order-events
Partition 0
```

Toàn bộ message của Topic nằm trong một Partition. Cách này đơn giản, giữ thứ tự dễ hơn, nhưng khả năng xử lý song song bị giới hạn.

Nếu Topic có nhiều Partition:

```text
Topic: order-events
Partition 0
Partition 1
Partition 2
```

Kafka có thể đặt các Partition này trên nhiều Broker khác nhau:

```text
Partition 0 -> Broker 1
Partition 1 -> Broker 2
Partition 2 -> Broker 3
```

Partition giúp Kafka:

- Tăng throughput đọc/ghi.
- Cho phép nhiều Consumer trong cùng Consumer Group xử lý song song.
- Phân tán dữ liệu trên nhiều Broker.
- Giảm tải cho từng Broker riêng lẻ.
- Scale tốt hơn khi lượng event tăng.

Ví dụ hệ thống thương mại điện tử có Topic `order-events`. Nếu mỗi ngày chỉ có vài nghìn đơn hàng, 1 Partition có thể đủ. Nếu mỗi phút có hàng chục nghìn đơn hàng, Topic nên có nhiều Partition để nhiều Consumer xử lý song song:

```text
Consumer Group: order-service

Consumer 1 đọc Partition 0
Consumer 2 đọc Partition 1
Consumer 3 đọc Partition 2
```

Kafka chỉ đảm bảo thứ tự message **bên trong cùng một Partition**. Kafka không đảm bảo thứ tự tuyệt đối trên toàn Topic nếu Topic có nhiều Partition.

```text
Partition 0:
event A -> event B -> event C

Partition 1:
event X -> event Y -> event Z
```

Trong từng Partition, thứ tự được giữ. Nhưng giữa Partition 0 và Partition 1, Kafka không đảm bảo event nào được xử lý trước trên toàn hệ thống.

Vì vậy, nếu cần giữ đúng thứ tự cho một đối tượng cụ thể, nên dùng message key phù hợp. Ví dụ với payment, dùng `paymentId` làm key để các event của cùng một payment đi vào cùng một Partition:

```text
payment_created
payment_processing
payment_success
```

Nếu cả ba event đều có key `paymentId = 1001`, cùng serializer, cùng partitioner và số lượng Partition không đổi, Kafka sẽ đưa chúng vào cùng một Partition, nên Consumer có thể đọc theo đúng thứ tự của payment đó.

Lưu ý: có thể tăng số lượng Partition của Topic, nhưng không thể giảm số lượng Partition theo cách đơn giản. Lý do là message cũ đã nằm trong các Partition hiện tại; nếu giảm Partition thì Kafka phải quyết định di chuyển hoặc gộp dữ liệu cũ sang Partition khác, việc này có thể làm phức tạp offset, ordering, replica và dữ liệu đã lưu trên disk. Thực tế nếu muốn giảm Partition, thường phải tạo Topic mới với số Partition ít hơn rồi migrate dữ liệu/application sang Topic mới.

Việc tăng Partition cũng cần cẩn thận vì nó có thể làm thay đổi cách key được phân bổ cho **message mới**. Với Producer mặc định, nếu message có key, Kafka thường chọn Partition theo công thức gần như sau:

```text
partition = hash(serialized_key) % number_of_partitions
```

Trong Java Kafka Producer mặc định, thuật toán hash thường dùng là **Murmur2** trên key sau khi serialize sang bytes. Sau đó kết quả hash được đưa về số dương và chia lấy dư theo số lượng Partition.

Ví dụ cùng một key `orderId = 1001` có hash giả sử là `7`:

```text
Khi Topic có 3 Partition:
partition = 7 % 3 = 1
-> orderId = 1001 đi vào Partition 1

Sau khi tăng Topic lên 5 Partition:
partition = 7 % 5 = 2
-> message mới của orderId = 1001 có thể đi vào Partition 2
```

Message cũ không tự động chuyển Partition. Nếu trước khi tăng Partition, các event của `orderId = 1001` đang nằm ở Partition 1, thì chúng vẫn nằm ở Partition 1. Nhưng message mới có cùng key sau khi tăng Partition có thể đi sang Partition khác vì `number_of_partitions` đã đổi.

Điều này có nghĩa là Kafka không đảm bảo cùng một key sẽ mãi mãi vào đúng Partition cũ nếu số lượng Partition thay đổi. Kafka chỉ đảm bảo các message có cùng key được tính vào cùng một Partition khi số lượng Partition và partitioner không đổi.

Nếu hệ thống bắt buộc giữ ordering tuyệt đối theo key trong thời gian dài, cần thiết kế số Partition cẩn thận trước production. Một số cách xử lý thường gặp:

- Chọn số Partition đủ lớn ngay từ đầu để ít phải tăng sau này.
- Tránh tăng Partition cho Topic đang cần strict ordering theo key.
- Nếu cần thay đổi lớn, tạo Topic mới với số Partition mới rồi migrate có kiểm soát.
- Dùng custom partitioner nếu cần quy tắc phân bổ key đặc biệt, nhưng phải tự chịu trách nhiệm về phân phối tải và compatibility.

### Leader Partition

Khi một Partition có nhiều Replica, Kafka sẽ chọn một Replica làm **Leader**. Producer và Consumer mặc định làm việc với Leader của Partition đó. Các Replica còn lại là **Follower**, nhận dữ liệu replicate từ Leader.

![Kafka image 11](images/kafka-image-11.png)

Ví dụ:

```text
Topic: order-events
Partition 0

Leader   -> Broker 1
Follower -> Broker 2
Follower -> Broker 3
```

Producer ghi message vào Leader. Leader sau đó replicate dữ liệu sang Follower. Nếu Broker 1 bị lỗi, Kafka có thể chọn một Follower đủ điều kiện làm Leader mới.

#### Leader chết trước khi replicate thì có mất message không?

Có thể mất message nếu message mới chỉ được ghi ở Leader nhưng chưa được replicate sang Replica đủ điều kiện, sau đó Leader chết.

Ví dụ:

```text
replication-factor = 3

Broker 1: Leader
Broker 2: Follower
Broker 3: Follower

Producer gửi message M1
Broker 1 ghi M1
Broker 1 chết trước khi Broker 2/Broker 3 kịp nhận M1
Broker 2 được bầu làm Leader mới
-> M1 không có trên Leader mới
-> M1 bị mất
```

Khả năng này phụ thuộc nhiều vào cấu hình Producer và Broker.

Với `acks=0`, Producer không chờ Broker xác nhận. Nếu request thất bại hoặc Leader chết trước khi ghi/replicate, Producer có thể không biết message đã mất.

Với `acks=1`, Producer chỉ cần Leader ghi xong là nhận ACK thành công. Nếu Leader chết sau khi ACK nhưng trước khi Follower replicate, message vẫn có thể mất khi Leader mới không có message đó.

Với `acks=all`, Producer chỉ nhận ACK khi Leader và đủ số Replica trong ISR đã ghi message theo cấu hình `min.insync.replicas`. Cách này giảm rủi ro mất message nhiều hơn, nhưng latency ghi sẽ cao hơn.

Ví dụ cấu hình thường dùng cho dữ liệu quan trọng:

```properties
replication.factor=3
min.insync.replicas=2
acks=all
enable.idempotence=true
```

Ý nghĩa:

- `replication.factor=3`: mỗi Partition có 3 Replica.
- `min.insync.replicas=2`: ít nhất 2 Replica trong ISR phải ghi được message.
- `acks=all`: Producer chỉ nhận thành công khi điều kiện ISR được đáp ứng.
- `enable.idempotence=true`: Producer retry an toàn hơn, giảm duplicate do retry.

Nếu chỉ còn 1 Replica trong ISR mà `min.insync.replicas=2`, Kafka sẽ từ chối ghi thay vì nhận message trong trạng thái không đủ an toàn. Đây là đánh đổi: hệ thống có thể báo lỗi ghi tạm thời, nhưng tránh nhận thành công một message có rủi ro mất cao.

Cần tránh bật **unclean leader election** cho Topic quan trọng. Nếu Kafka cho phép bầu một Replica không nằm trong ISR làm Leader, hệ thống có thể phục hồi availability nhanh hơn nhưng có nguy cơ mất các message mà Replica đó chưa kịp đồng bộ.

Tóm gọn:

```text
acks=0
  -> nhanh nhất, rủi ro mất message cao nhất

acks=1
  -> Leader ghi xong là thành công, vẫn có thể mất nếu Leader chết trước khi replicate

acks=all + min.insync.replicas phù hợp
  -> an toàn hơn, đổi lại ghi chậm hơn và có thể từ chối ghi khi ISR không đủ
```

#### Nếu Consumer đọc từ Follower thì offset quản lý thế nào?

Từ Kafka 2.4, tính năng **fetch from follower** cho phép Consumer đọc từ Replica gần hơn về mặt địa lý trong một số mô hình triển khai multi-region. Tuy nhiên, offset vẫn là offset logic của **Partition**, không phải offset riêng của từng Replica.

Các Replica của cùng một Partition lưu cùng một log và cùng hệ offset cho các record đã replicate.

```text
Partition 0

Leader log:
offset 0, 1, 2, 3, 4

Follower log:
offset 0, 1, 2, 3, 4
```

Consumer đọc từ Leader hay Follower thì vẫn đọc theo offset của `Partition 0`. Điểm dễ nhầm là: Consumer không phải cứ đọc một batch data là lại đi hỏi Broker chứa `__consumer_offsets`.

Luồng đọc data và luồng commit offset là hai việc khác nhau:

```text
Fetch data:
Consumer -> Leader hoặc Follower gần hơn
          -> nhận nhiều record theo batch

Commit offset:
Consumer -> Group Coordinator
          -> Kafka ghi offset vào topic nội bộ __consumer_offsets
```

Offset commit vẫn được lưu theo:

```text
Consumer Group + Topic + Partition
```

Ví dụ:

```text
group = order-service
topic = order-events
partition = 0
committed_offset = 5
```

Kafka không lưu kiểu:

```text
Consumer này đọc tới offset 5 trên Leader
Consumer này đọc tới offset 5 trên Follower
```

Nó chỉ lưu rằng Consumer Group `order-service` đã xử lý tới vị trí nào của `order-events / Partition 0`. Việc record được fetch từ Replica nào là chi tiết phục vụ tối ưu network/latency, không làm thay đổi ý nghĩa offset.

Vì vậy, đọc từ Follower vẫn có lợi trong multi-region. Data fetch thường là phần nặng vì có thể trả về rất nhiều record và payload lớn. Offset commit chỉ là metadata nhỏ, thường được commit theo batch hoặc theo interval, không phải commit sau từng record trong hầu hết ứng dụng.

Ví dụ:

```text
Consumer ở Singapore
Leader ở US
Follower ở Singapore

Fetch data lớn:
Consumer -> Follower Singapore

Commit offset nhỏ:
Consumer -> Group Coordinator
```

Nếu mỗi batch fetch có vài MB data, còn offset commit chỉ là một bản ghi metadata nhỏ, thì việc fetch từ Follower gần hơn vẫn giảm đáng kể network latency và cross-region bandwidth cho đường đọc data.

Follower cũng không nên trả dữ liệu vượt quá phần đã được xem là an toàn của Partition. Nếu Follower đang lag, Consumer có thể đọc chậm hơn hoặc phải fetch từ Replica khác, nhưng offset commit của Consumer Group vẫn không đổi mô hình.

### Event Key

Event key là giá trị Producer gửi kèm message để Kafka quyết định message nên đi vào Partition nào. Nếu các message có cùng key, cùng partitioner và số lượng Partition không đổi, chúng thường được đưa vào cùng một Partition.

Ví dụ:

```text
key = orderId

orderId = 1001 -> Partition 1
orderId = 1001 -> Partition 1
orderId = 2002 -> Partition 2
```

Cách chọn key ảnh hưởng trực tiếp tới ordering và phân phối tải.

Kafka chọn Partition theo các trường hợp chính:

- Nếu Producer chỉ định trực tiếp `partition`, Kafka dùng Partition đó.
- Nếu không chỉ định `partition` nhưng có key, Producer hash key rồi chia theo số lượng Partition.
- Nếu key là `null`, Producer dùng chiến lược mặc định cho message không key, thường là sticky partitioner ở các version Kafka mới để gom batch tốt hơn.

Vì key được hash sau khi serialize, cùng một giá trị logic nhưng serializer khác nhau có thể tạo bytes khác nhau và dẫn tới Partition khác nhau. Ví dụ key `123` serialize bằng Integer Serializer sẽ khác key `"123"` serialize bằng String Serializer.

Key tốt khi:

- Có ý nghĩa business rõ ràng.
- Cần giữ thứ tự theo entity, ví dụ `orderId`, `paymentId`, `userId`.
- Có độ phân tán đủ tốt để tránh một Partition nhận quá nhiều message.

Key không tốt khi:

- Quá ít giá trị khác nhau, ví dụ chỉ dùng `country = VN` trong hệ thống mà gần như toàn bộ traffic ở Việt Nam.
- Không liên quan tới yêu cầu ordering.
- Là giá trị dễ bị lệch tải, khiến một Partition quá nóng còn các Partition khác ít dữ liệu.

Ví dụ lỗi thiết kế:

```text
Topic: order-events
Key: country

90% message có country = VN
  -> nhiều message dồn vào cùng một Partition
```

Tốt hơn có thể dùng:

```text
Key: orderId
```

Khi đó các event của cùng một order vẫn giữ được thứ tự, đồng thời nhiều order khác nhau có thể được phân tán tốt hơn.

### Segment

Segment là phần nhỏ hơn của một Partition log. Về mặt logic, một Partition là một log dài, message mới cứ được append vào cuối. Nhưng nếu Kafka lưu cả Partition vào một file duy nhất rất lớn thì sẽ khó quản lý, khó xóa dữ liệu cũ và khó tìm vị trí cần đọc. Vì vậy Kafka chia log của mỗi Partition thành nhiều **Segment**.

Ví dụ một Partition được chia thành nhiều Segment:

```text
Partition 0

Segment 0: offset 0    -> 934
Segment 1: offset 935  -> 1567
Segment 2: offset 1568 -> 2895
Segment 3: offset 2896 -> đang ghi tiếp
```

Segment cuối cùng là **active segment**. Đây là segment đang nhận message mới. Các segment trước đó đã được đóng lại, thường chỉ còn phục vụ đọc, retention hoặc compaction.

Một segment trên disk thường đi kèm nhiều file liên quan, ví dụ:

```text
00000000000000000000.log        chứa record thật
00000000000000000000.index      index từ offset -> vị trí byte trong file .log
00000000000000000000.timeindex  index từ timestamp -> offset gần tương ứng
```

Tên file segment thường bắt đầu bằng **base offset**, tức offset đầu tiên trong segment đó.

Ví dụ:

```text
00000000000000000935.log
```

File này là segment bắt đầu từ offset `935`.

#### Vì sao Kafka cần Segment?

Segment giúp Kafka xử lý log lớn hiệu quả hơn:

- Dễ xóa dữ liệu cũ theo retention vì Kafka có thể xóa cả segment cũ thay vì xóa từng message.
- Dễ tìm message theo offset nhờ index.
- File nhỏ hơn giúp hệ điều hành quản lý disk/page cache tốt hơn.
- Nếu một segment bị lỗi, phạm vi ảnh hưởng nhỏ hơn so với một file log cực lớn.
- Rolling segment giúp Kafka không phải mở rộng mãi một file duy nhất.

#### Kafka tìm message theo offset như thế nào?

Giả sử Consumer yêu cầu:

```text
Cho tôi đọc từ offset 5000 của Partition 0
```

Kafka xử lý gần như sau:

```text
1. Tìm segment chứa offset 5000
   Ví dụ segment có base offset 4096.

2. Dùng file index của segment đó để tìm vị trí byte gần offset 5000 trong file .log.

3. Đọc từ vị trí byte đó trong file .log.

4. Trả record cho Consumer theo batch.
```

Kafka không cần scan từ offset `0` tới `5000`. Đây là lý do index quan trọng.

#### Khi nào Kafka tạo Segment mới?

Kafka tạo segment mới khi segment hiện tại đạt điều kiện rolling, thường do size hoặc thời gian.

Cấu hình thường gặp:

```properties
log.segment.bytes=1073741824
log.segment.ms=604800000
```

Ý nghĩa:

- `log.segment.bytes`: kích thước tối đa của một segment, mặc định thường là `1GB`.
- `log.segment.ms`: thời gian tối đa trước khi Kafka roll sang segment mới, kể cả khi chưa đủ size.

Ví dụ:

```text
Segment hiện tại đạt 1GB
-> Kafka đóng segment cũ
-> Kafka tạo active segment mới
-> message mới được ghi vào active segment mới
```

#### Segment và Retention

Kafka thường xóa dữ liệu cũ theo đơn vị Segment, không xóa từng message riêng lẻ.

Ví dụ cấu hình:

```properties
log.retention.hours=168
```

Nghĩa là dữ liệu được giữ khoảng 7 ngày. Khi một segment đủ cũ theo retention, Kafka có thể xóa segment đó.

Điểm cần lưu ý: Kafka thường xóa theo **cả Segment**, không xóa chính xác từng message riêng lẻ.

Ví dụ:

```text
Retention = 7 ngày

Segment A chứa:
- message 1: đã 8 ngày tuổi
- message 2: đã 6 ngày tuổi
```

Kafka có thể chưa xóa `message 1` ngay, vì nó đang nằm chung Segment với dữ liệu chưa đủ cũ. Khi cả Segment đủ điều kiện theo retention, Kafka mới xóa Segment đó.

Nói ngắn gọn: retention là cơ chế dọn dữ liệu theo Segment, nên thời điểm xóa thực tế có thể lệch một chút so với tuổi của từng message.

#### Ưu điểm và nhược điểm của Segment

Ưu điểm:

- Tối ưu cleanup dữ liệu cũ.
- Tìm kiếm theo offset nhanh hơn nhờ index.
- Dễ quản lý file log lớn.
- Phù hợp với append-only log và sequential I/O.

Nhược điểm:

- Segment quá nhỏ có thể tạo nhiều file, tăng overhead quản lý file/index.
- Segment quá lớn có thể làm cleanup chậm hơn vì Kafka xóa theo cả segment.
- Cấu hình segment ảnh hưởng tới retention, compaction và hiệu năng disk.

Use case cần chú ý:

- Topic log traffic lớn nên tránh segment quá nhỏ vì sẽ tạo quá nhiều file.
- Topic cần cleanup nhanh hơn có thể cần segment nhỏ hơn, nhưng phải cân bằng overhead.
- Topic dùng compact policy cần theo dõi thêm chi phí log compaction.

### Offset

Offset là số thứ tự của record bên trong một Partition. Mỗi Partition có hệ offset riêng, bắt đầu từ `0` và tăng dần khi có record mới được append.

Ví dụ:

```text
Topic: order-events

Partition 0:
offset 0, 1, 2, 3, 4, 5, 6

Partition 1:
offset 0, 1, 2, 3

Partition 2:
offset 0, 1, 2, 3, 4
```

Offset chỉ có ý nghĩa trong phạm vi một Partition. Không nên so sánh `Partition 0 / offset 5` với `Partition 1 / offset 5` để kết luận record nào mới hơn trên toàn Topic.

#### Offset của record và committed offset của Consumer

Cần phân biệt hai khái niệm:

- **Record offset:** vị trí cố định của một record trong Partition.
- **Committed offset:** vị trí Consumer Group đã xác nhận xử lý tới đâu.

Record offset không thay đổi sau khi record được ghi vào Kafka.

Ví dụ:

```text
Partition 0:
offset 0: OrderCreated
offset 1: PaymentStarted
offset 2: PaymentSucceeded
```

`PaymentSucceeded` nằm ở offset `2` thì offset đó là cố định trong Partition.

Committed offset thường được hiểu là **offset tiếp theo Consumer Group sẽ đọc**, không phải offset cuối cùng đã đọc.

Ví dụ:

```text
Consumer Group: order-service

Đã xử lý xong record ở offset 0, 1, 2, 3, 4
Committed offset = 5

Khi chạy lại:
Consumer đọc tiếp từ offset 5
```

Kafka lưu committed offset vào Topic nội bộ `__consumer_offsets`.

```text
group = order-service
topic = order-events
partition = 0
committed_offset = 5
```

#### Offset giúp Consumer đọc lại dữ liệu như thế nào?

Consumer có thể đọc từ offset đã commit, hoặc reset offset theo cấu hình/policy.

Ví dụ Consumer crash:

```text
Consumer đọc offset 5, 6, 7
Xử lý xong offset 5, 6
Crash trước khi commit offset 7

Lần chạy lại:
Consumer đọc lại từ offset đã commit gần nhất
```

Nếu commit sau khi xử lý thành công, hệ thống có thể xử lý trùng một số message khi crash xảy ra trước commit. Đây là lý do Consumer logic nên idempotent.

#### Offset có đảm bảo không xử lý trùng không?

Không hoàn toàn. Offset giúp Consumer biết nên đọc tiếp từ đâu, nhưng việc có xử lý trùng hay mất message phụ thuộc vào thời điểm commit offset so với thời điểm xử lý business logic.

Nếu commit offset **trước khi xử lý**:

```text
Consumer đọc offset 10
Commit offset 11
Crash trước khi lưu DB
-> chạy lại từ offset 11
-> offset 10 bị bỏ qua
-> có thể mất xử lý business
```

Nếu commit offset **sau khi xử lý**:

```text
Consumer đọc offset 10
Lưu DB thành công
Crash trước khi commit offset 11
-> chạy lại vẫn đọc offset 10
-> có thể xử lý lại offset 10
```

Vì vậy pattern an toàn phổ biến là:

```text
Đọc message
Xử lý business logic theo cách idempotent
Commit offset sau khi xử lý thành công
```

#### Offset và retention

Offset không có nghĩa là message tồn tại mãi. Nếu message cũ bị xóa do retention, Consumer không thể đọc lại offset đó nữa.

Ví dụ:

```text
Topic giữ dữ liệu 7 ngày
Consumer dừng 10 ngày
Các segment cũ đã bị xóa
Consumer quay lại offset cũ
-> offset đó không còn dữ liệu
```

Khi offset đã commit trỏ tới dữ liệu không còn tồn tại, Consumer sẽ cần quyết định đọc tiếp từ đâu.

#### `auto.offset.reset` dùng khi nào?

`auto.offset.reset` không chỉ dùng khi message cũ bị xóa do retention. Cấu hình này được dùng khi Consumer Group **không có committed offset hợp lệ** cho Partition cần đọc.

Các trường hợp thường gặp:

- Consumer Group mới, chưa từng commit offset.
- Offset đã commit quá cũ và dữ liệu tương ứng đã bị xóa do retention.
- Offset đã commit không còn hợp lệ, ví dụ Topic bị xóa rồi tạo lại hoặc dữ liệu bị truncate trong một số tình huống đặc biệt.

Khi đó Kafka xử lý theo `auto.offset.reset`:

- `earliest`: đọc từ offset sớm nhất còn tồn tại.
- `latest`: đọc từ cuối log, chỉ nhận message mới.
- `none`: báo lỗi nếu không tìm được offset hợp lệ.

Ví dụ Consumer Group mới:

```text
Topic đã có offset 0 -> 100
Consumer Group mới chưa có committed offset

auto.offset.reset=earliest
-> đọc từ offset 0

auto.offset.reset=latest
-> bắt đầu ở cuối log, chờ message mới sau offset 100
```

#### Offset, Consumer Group và Partition assignment

Offset được lưu theo Consumer Group, Topic và Partition. Nếu có nhiều Consumer trong cùng một Group, mỗi Partition tại một thời điểm thường được assign cho một Consumer trong Group.

Ví dụ:

```text
Topic: order-events
Partitions: 3
Consumer Group: order-service

Consumer A -> Partition 0
Consumer B -> Partition 1
Consumer C -> Partition 2
```

Nếu Consumer B bị crash, Kafka rebalance:

```text
Consumer A -> Partition 0, Partition 1
Consumer C -> Partition 2
```

Consumer A đọc `Partition 1` từ committed offset của Group `order-service`. Offset không thuộc riêng Consumer B; nó thuộc Consumer Group trên Partition đó.

#### Tóm tắt Offset

- Offset là vị trí của record trong một Partition.
- Offset chỉ có ý nghĩa trong phạm vi Partition, không phải toàn Topic.
- Record offset là cố định sau khi ghi.
- Committed offset là vị trí Consumer Group sẽ đọc tiếp.
- Commit offset quá sớm có thể mất xử lý.
- Commit offset sau xử lý có thể xử lý trùng khi crash.
- Consumer nên xử lý idempotent để an toàn với retry/replay.

### Event / Message / Record / Data

Trong Kafka, các từ **event**, **message**, **record** và **data** thường được dùng gần nhau. Phần này chỉ tập trung phân biệt thuật ngữ; các ý về Topic, Partition, Offset, retention và Consumer Group đã được giải thích ở các mục riêng phía trên.

#### Event

Event là một sự việc đã xảy ra trong hệ thống. Tên event nên mô tả kết quả đã xảy ra, thường dùng dạng quá khứ.

Ví dụ:

```text
UserRegistered
OrderCreated
PaymentSucceeded
InventoryReserved
```

Phân biệt nhanh với command:

```text
PaymentSucceeded -> event, vì thanh toán đã thành công
ChargePayment    -> command, vì đây là yêu cầu thực hiện thanh toán
```

#### Message / Record

Trong Kafka, **record** là đơn vị dữ liệu Kafka lưu trong Topic Partition. Nhiều tài liệu và developer cũng gọi record là **message**. Trong phần lớn ngữ cảnh Kafka, `message` và `record` có thể hiểu gần như cùng một thứ.

Một Kafka record thường có các phần quan trọng:

```text
key       -> key của record, có thể dùng để chọn Partition
value     -> nội dung chính
timestamp -> thời điểm record được tạo hoặc ghi vào Kafka
headers   -> metadata phụ
```

Ví dụ từ ảnh cũ chuyển sang text:

```text
Event key:       Alice
Event value:     Made a payment of $200 to Bob
Event timestamp: Jun. 25, 2020 at 2:06 p.m.
```

Ví dụ record thực tế hơn:

```text
key:       paymentId=pay-9001
value:     {"eventType":"PaymentSucceeded","paymentId":"pay-9001","amount":200}
timestamp: 2026-08-30T10:15:00Z
headers:   correlationId=checkout-abc-123, schemaVersion=1
```

#### Data / Payload

Data hoặc payload thường là phần nội dung nghiệp vụ nằm trong `value` của record. Đây là phần Consumer đọc để xử lý business.

Ví dụ:

```json
{
  "paymentId": "pay-9001",
  "orderId": "ord-7001",
  "amount": 200,
  "currency": "USD"
}
```

Kafka không hiểu sâu ý nghĩa nghiệp vụ của payload. Với Kafka, `key` và `value` là bytes sau khi serialize. Application quyết định bytes đó đại diện cho JSON, Avro, Protobuf, String hay binary.

#### Phân biệt nhanh

| Khái niệm | Nghĩa chính | Ví dụ |
| --- | --- | --- |
| Event | Sự việc đã xảy ra trong business | `PaymentSucceeded` |
| Message | Cách gọi phổ biến khi nói dữ liệu được gửi qua Kafka | message gửi vào `payment-events` |
| Record | Cách gọi chính xác hơn trong Kafka API | `ProducerRecord`, `ConsumerRecord` |
| Data/Payload | Nội dung nghiệp vụ trong record value | `paymentId`, `orderId`, `amount` |

Nói ngắn gọn:

```text
Business tạo ra event.
Producer đóng event thành Kafka record/message.
Kafka lưu record vào Topic Partition.
Consumer đọc record và xử lý data trong value.
```

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

- Key của message có thể `null`. Nếu key là `null`, Producer sẽ dùng strategy dành cho message không key, ví dụ sticky partitioner ở các version Kafka mới hoặc round-robin ở một số version/cấu hình cũ.
- Nếu có key, Java Kafka Producer mặc định thường dùng Murmur2 để hash key sau khi key đã được serialize sang bytes, rồi chọn Partition theo công thức `partition = positive(hash(serialized_key)) % số lượng partition`.
- Khi cùng key, cùng serializer, cùng partitioner và số lượng Partition không đổi, message sẽ đi vào cùng một Partition. Nếu số lượng Partition thay đổi, cùng key đó có thể được tính ra Partition khác cho các message mới.

### Kafka Message Serializer

- Tuy nhiên ở Producer, Consumer ta không viết message dạng byte -> ta cần quá trình serialization để giúp convert object sang byte trước khi send tới Kafka + convert sang object khi Consumer nhận message

Ví dụ: Tôi khi send message từ Producer có key-value là 123-helloworld nhưng tất nhiên nó không ở dạng byte trong ngôn ngữ lập trình -> chỉ định Integer Serializer cho key, giúp chuyển 123 sang dạng chuỗi byte + đối với value cũng tương tự vậy

![Kafka image 19](images/kafka-image-19.png)

### Producer Partition Strategy

Producer Partition Strategy là cách Producer quyết định record sẽ được gửi vào Partition nào của Topic.

Thứ tự ưu tiên cơ bản:

```text
1. Nếu record chỉ định trực tiếp partition
   -> gửi vào partition đó.

2. Nếu không chỉ định partition nhưng có key
   -> hash key để chọn partition.

3. Nếu không chỉ định partition và key = null
   -> dùng strategy mặc định cho record không key.
```

Điểm quan trọng: Partition được chọn ở phía Producer, không phải Topic tự quyết định. Broker nhận request ghi vào Partition Leader tương ứng.

#### Trường hợp chỉ định partition trực tiếp

Producer có thể chỉ định thẳng Partition khi tạo record.

Ví dụ:

```java
new ProducerRecord<>("order-events", 2, "order-1001", payload);
```

Ý nghĩa:

```text
topic = order-events
partition = 2
key = order-1001
value = payload
```

Khi đã chỉ định trực tiếp `partition = 2`, Producer sẽ gửi record vào Partition 2. Key lúc này vẫn có thể được lưu trong record, nhưng không còn được dùng để chọn Partition.

Ưu điểm:

- Kiểm soát tuyệt đối record đi vào Partition nào.
- Hữu ích cho một số case đặc biệt, ví dụ routing theo rule riêng đã tính sẵn.

Nhược điểm:

- Application phải tự biết Topic có bao nhiêu Partition.
- Dễ làm lệch tải nếu chọn Partition không đều.
- Khi tăng số Partition, logic trong application có thể phải sửa.
- Thường không nên dùng mặc định trong business code phổ thông.

#### Trường hợp có key

Nếu record không chỉ định Partition nhưng có key, Java Kafka Producer mặc định sẽ chọn Partition dựa trên hash của key.

Mô hình đơn giản:

```text
partition = positive(hash(serialized_key)) % number_of_partitions
```

Với Java Kafka Producer, thuật toán hash truyền thống cho key là Murmur2 trên key sau khi serialize thành bytes.

Ví dụ:

![Kafka image 21](images/kafka-image-21.png)

```text
Topic order-events có 3 Partition: 0, 1, 2

key = order-1001
hash(key) = 7

partition = 7 % 3 = 1
-> record đi vào Partition 1
```

Cùng key sẽ đi vào cùng Partition khi các điều kiện sau không đổi:

- Cùng Topic.
- Cùng số lượng Partition.
- Cùng serializer cho key.
- Cùng partitioner/logic chọn Partition.

Ví dụ:

```text
orderId = 1001 -> Partition 1
orderId = 1001 -> Partition 1
orderId = 2002 -> Partition 2
```

Cách này phù hợp khi cần giữ thứ tự theo một entity cụ thể:

- Event của cùng một `orderId`.
- Event của cùng một `paymentId`.
- Event của cùng một `userId`.

Vì Kafka chỉ đảm bảo ordering trong một Partition, key giúp các event liên quan tới cùng một entity nằm chung Partition.

Nhược điểm:

- Nếu key phân phối không đều, một Partition có thể bị nóng.
- Nếu tăng số Partition, message mới cùng key có thể đi sang Partition khác vì phép `% number_of_partitions` thay đổi.
- Nếu đổi serializer của key, bytes sau serialize thay đổi, hash có thể thay đổi.

Ví dụ hot partition:

```text
key = country

90% traffic có country = VN
-> nhiều record dồn vào cùng một Partition
-> một Consumer xử lý Partition đó bị quá tải
```

Trong case này, key `orderId` hoặc `userId` có thể phân phối tốt hơn, tùy yêu cầu ordering.

#### Trường hợp không có key

Nếu record không chỉ định Partition và key là `null`, Producer không có căn cứ business để giữ ordering theo entity. Khi đó mục tiêu chính thường là:

- phân phối record tương đối đều,
- tạo batch đủ lớn,
- giảm số request nhỏ,
- tránh gửi quá nhiều vào Broker đang chậm.

Ở các version Kafka hiện đại, default logic dùng cơ chế **sticky partitioning** cho record không key: Producer chọn một Partition và tiếp tục gom record vào batch của Partition đó; khi batch đạt ngưỡng hoặc được gửi, Producer mới chuyển sang Partition khác.

Ví dụ:

```text
Topic có Partition 0, 1, 2

Producer chọn sticky Partition 1
record A -> Partition 1
record B -> Partition 1
record C -> Partition 1

Batch của Partition 1 được gửi
Producer chọn sticky Partition khác
record D -> Partition 0
record E -> Partition 0
```

Lý do Kafka làm vậy: batching hiệu quả hơn khi nhiều record đi vào cùng một Partition trong một khoảng ngắn. Nếu mỗi record không key cứ đổi Partition liên tục, Producer dễ tạo nhiều batch nhỏ, tăng request và overhead.

#### Sticky partitioning khác round-robin thế nào?

Round-robin chọn Partition luân phiên theo từng record.

![Kafka image 20](images/kafka-image-20.png)

```text
record 1 -> Partition 0
record 2 -> Partition 1
record 3 -> Partition 2
record 4 -> Partition 0
record 5 -> Partition 1
record 6 -> Partition 2
```

Sticky partitioning giữ một Partition trong một khoảng ngắn để gom batch.

![Kafka image 22](images/kafka-image-22.png)

```text
record 1 -> Partition 0
record 2 -> Partition 0
record 3 -> Partition 0

batch Partition 0 được gửi

record 4 -> Partition 2
record 5 -> Partition 2
record 6 -> Partition 2
```

So sánh:

| Tiêu chí | Round-robin | Sticky partitioning |
| --- | --- | --- |
| Cách chọn | Đổi Partition liên tục | Giữ một Partition cho tới khi batch đủ điều kiện gửi |
| Batch | Dễ tạo nhiều batch nhỏ | Dễ tạo batch lớn hơn |
| Throughput | Có thể kém hơn khi record nhỏ/nhiều Partition | Thường tốt hơn cho record không key |
| Ordering theo key | Không phù hợp nếu bỏ qua key | Không áp dụng cho keyed record trong default logic |
| Use case | Khi cố ý muốn phân phối đều từng record và chấp nhận mất ordering theo key | Default tốt cho record không key |

Lưu ý: Round-robin partitioner nếu được cấu hình rõ có thể bỏ qua key và phân phối cả record có key theo vòng tròn. Cách này có thể giảm hot partition do một key quá lớn, nhưng đổi lại không còn đảm bảo event cùng key đi vào cùng Partition.

#### Batch có làm đảo thứ tự message không?

Consumer không đẩy batch vào Kafka; Producer mới là bên gửi batch. Kafka Producer gom record thành batch theo từng **Topic-Partition**.

Có thể có trường hợp message sinh sau được ghi vào Kafka trước message sinh trước, nhưng cần xem chúng có cùng Partition và cùng Producer hay không.

Nếu hai message nằm ở **hai Partition khác nhau**, Kafka không đảm bảo thứ tự giữa chúng:

```text
message A sinh trước -> Partition 0
message B sinh sau   -> Partition 1

Partition 1 ghi xong trước Partition 0
-> Consumer có thể thấy B trước A nếu đọc nhiều Partition
```

Điều này bình thường vì Kafka chỉ đảm bảo ordering trong từng Partition.

Nếu hai Producer khác nhau cùng gửi vào **một Partition**, thứ tự được tính theo thứ tự Broker append vào log, không phải theo thời điểm business event được tạo ra ở từng máy:

```text
Producer 1 tạo message A lúc 10:00:00
Producer 2 tạo message B lúc 10:00:01

Batch B tới Broker trước batch A
-> Broker append B trước A
```

Nếu cùng một Producer gửi nhiều record vào cùng một Partition, Kafka Producer cố giữ thứ tự gửi trong Partition đó. Tuy nhiên, cần cấu hình retry/idempotence đúng. Với cấu hình hiện đại `enable.idempotence=true`, Producer an toàn hơn khi retry và tránh reorder do retry. Nếu tắt idempotence, bật retry và cho phép nhiều request in-flight, batch gửi sau có thể thành công trước batch gửi trước sau khi batch trước bị retry.

Tóm gọn:

```text
Cùng Producer + cùng Partition + idempotence đúng
  -> giữ thứ tự tốt nhất.

Khác Partition
  -> không có ordering toàn cục.

Khác Producer
  -> Broker ghi theo batch nào tới và được append trước.

Retry không cấu hình cẩn thận
  -> có thể gây reorder trong cùng Partition.
```

#### Default partitioning theo version

Kafka partitioner có thay đổi qua các version, nên không nên học thuộc một câu kiểu "Kafka luôn round-robin nếu key null".

Tóm tắt thực tế:

| Version / cấu hình | Khi có key | Khi key null |
| --- | --- | --- |
| Kafka cũ trước sticky default | Hash key | Round-robin |
| Kafka 2.4+ với default partitioner cũ | Hash key | Sticky partitioning |
| Kafka 3.3+ | `DefaultPartitioner` và `UniformStickyPartitioner` bị deprecated | Nên dùng default logic thay vì set các class cũ |
| Kafka 4.x | `partitioner.class` mặc định là `null`; default logic nằm trong Producer | Sticky/adaptive logic cho record không key |

Với Kafka 4.x, docs chính thức mô tả:

- Nếu `partitioner.class` không set, Producer dùng default partitioning logic.
- Nếu có key và không chỉ định Partition, Producer chọn Partition dựa trên hash của key.
- Nếu không có key và không chỉ định Partition, Producer chọn sticky Partition và đổi khi batch đạt ít nhất `batch.size`.
- `partitioner.ignore.keys=false` theo mặc định, nghĩa là key vẫn được dùng để chọn Partition.
- `partitioner.adaptive.partitioning.enable=true` theo mặc định, Producer có thể ưu tiên Partition nằm trên Broker xử lý nhanh hơn cho record không key.

#### `partitioner.ignore.keys`

`partitioner.ignore.keys` quyết định default logic có dùng key để chọn Partition hay không.

```properties
partitioner.ignore.keys=false
```

Đây là mặc định. Nếu record có key, Producer dùng hash của key để chọn Partition.

```properties
partitioner.ignore.keys=true
```

Producer không dùng key để chọn Partition, kể cả record có key. Khi đó record sẽ đi theo logic dành cho record không key.

Use case có thể cân nhắc `partitioner.ignore.keys=true`:

- Key vẫn cần lưu trong record để Consumer đọc, nhưng không cần ordering theo key.
- Một vài key quá lớn gây hot partition.
- Muốn phân phối tải đều hơn và chấp nhận event cùng key có thể nằm ở nhiều Partition.

Không nên dùng khi:

- Cần giữ thứ tự event theo `orderId`, `paymentId`, `userId`.
- Consumer phụ thuộc vào giả định same key nằm cùng Partition.

#### Adaptive partitioning

Trong Kafka 3.3+ và 4.x, default logic có cấu hình:

```properties
partitioner.adaptive.partitioning.enable=true
```

Khi bật, Producer có thể gửi nhiều record không key hơn tới các Partition nằm trên Broker đang xử lý nhanh hơn. Mục tiêu là tránh dồn thêm tải vào Broker đang chậm.

Ví dụ:

```text
Partition 0 Leader ở Broker A, Broker A đang chậm
Partition 1 Leader ở Broker B, Broker B đang phản hồi nhanh
Partition 2 Leader ở Broker C, Broker C đang phản hồi nhanh

Producer có thể ưu tiên Partition 1 hoặc 2 cho record không key
```

Điểm cần hiểu: adaptive partitioning chủ yếu liên quan tới record không key hoặc trường hợp key bị ignore. Nếu record có key và `partitioner.ignore.keys=false`, Producer vẫn ưu tiên giữ rule hash key để đảm bảo same key vào cùng Partition.

#### Custom partitioner

Có thể tự viết custom partitioner bằng cách implement `org.apache.kafka.clients.producer.Partitioner`.

Use case:

- Muốn route một nhóm khách hàng VIP vào một nhóm Partition riêng.
- Muốn tránh một số Partition tạm thời.
- Muốn dùng thuật toán hash khác.
- Muốn mapping key ổn định hơn khi thay đổi số Partition.

Nhược điểm:

- Dễ tạo lệch tải nếu thuật toán không tốt.
- Phải tự kiểm thử ordering, compatibility và behavior khi tăng Partition.
- Các cấu hình như `partitioner.ignore.keys` hoặc adaptive partitioning có thể không có tác dụng nếu dùng custom partitioner.
- Khó vận hành hơn vì behavior không còn giống default Kafka.

#### Những lỗi dễ viết sai

- Sai: "Round-robin nghĩa là 1 message = 1 request". Đúng hơn: round-robin chọn Partition theo từng record, nhưng Producer vẫn có thể batch các record theo Partition trước khi gửi request.
- Sai: "Kafka hiện đại luôn round-robin khi key null". Đúng hơn: Kafka 2.4+ dùng sticky cho record không key trong default logic; Kafka 4.x tiếp tục dùng default logic không-key theo hướng sticky/adaptive.
- Sai: "Same key luôn luôn cùng Partition". Đúng hơn: same key cùng Partition khi số Partition, serializer và partitioner không đổi.
- Sai: "Sticky luôn phân phối đều tuyệt đối". Đúng hơn: sticky tối ưu batch; phân phối dài hạn thường ổn hơn, nhưng vẫn có thể lệch trong ngắn hạn hoặc khi adaptive partitioning ưu tiên Broker nhanh hơn.
- Sai: "Dùng round-robin để xử lý hot key mà vẫn giữ ordering theo key". Nếu round-robin bỏ qua key, event cùng key có thể vào nhiều Partition, nên ordering theo key không còn được đảm bảo.

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
- Khi dữ liệu được replicate sang Follower Replica, thông tin message sequence và producer id cũng được replicate theo. Nếu Leader cũ bị lỗi và một Follower đủ điều kiện trở thành Leader mới, Leader mới vẫn có thông tin để nhận ra message retry từ Producer và tránh ghi trùng.
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

Consumer là application/client đọc record từ Kafka Topic để xử lý business logic.

![Kafka image 29](images/kafka-image-29.png)

#### Pull model có mạnh hơn push model không?

Không nên nói pull model luôn mạnh hơn push model. Pull và push giải quyết bài toán khác nhau.

Với **push model**, Broker chủ động đẩy message xuống Consumer. Cách này có thể có latency thấp vì có message là Broker gửi ngay. Nhưng Broker phải quan tâm nhiều hơn tới tốc độ từng Consumer. Nếu Consumer chậm, Broker cần cơ chế giới hạn như prefetch/QoS/backpressure để tránh đẩy quá nhiều.

Với **pull model**, Consumer chủ động gọi `poll()`/fetch request để xin dữ liệu từ Broker. Consumer tự quyết định lúc nào đọc tiếp và đọc bao nhiêu tùy khả năng xử lý của nó.

Kafka chọn pull model vì hợp với thiết kế log + offset:

- Consumer đọc theo offset của từng Partition.
- Consumer có thể fetch nhiều record theo batch.
- Consumer chậm thì đọc chậm lại, dữ liệu vẫn nằm trong Kafka cho tới khi hết retention.
- Consumer có thể đọc lại dữ liệu cũ nếu offset/retention cho phép.
- Broker không phải đẩy từng message riêng lẻ tới từng Consumer.

So sánh ngắn:

| Tiêu chí | Push model | Pull model trong Kafka |
| --- | --- | --- |
| Ai chủ động | Broker đẩy message | Consumer chủ động fetch |
| Khi Consumer chậm | Broker phải kiểm soát lượng message đang đẩy | Consumer tự giảm tốc độ poll |
| Batch | Có thể có, tùy broker/client | Rất tự nhiên vì Consumer fetch theo batch |
| Replay theo offset | Không phải thế mạnh chính của queue truyền thống | Là thế mạnh của Kafka |
| Phù hợp | Task queue, routing, giao việc nhanh | Event log, throughput cao, replay, stream processing |

Nói ngắn gọn: pull không phải lúc nào cũng tốt hơn push, nhưng pull rất hợp với Kafka vì Kafka lưu dữ liệu như log và Consumer đọc theo offset.

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

Consumer Group Coordinator là Broker chịu trách nhiệm quản lý một Consumer Group: Consumer nào đang trong group, Consumer nào được assign Partition nào, và khi nào cần rebalance.

![Kafka image 45](images/kafka-image-45.png)

Kafka dùng cả **heartbeat** và **poll** vì chúng trả lời hai câu hỏi khác nhau.

#### Heartbeat kiểm tra Consumer còn sống không

Heartbeat là tín hiệu Consumer gửi định kỳ tới Group Coordinator để báo rằng process Consumer vẫn còn sống và vẫn kết nối được tới Kafka.

Các cấu hình liên quan:

- `heartbeat.interval.ms`: khoảng thời gian giữa hai lần gửi heartbeat, thường nhỏ hơn `session.timeout.ms`.
- `session.timeout.ms`: nếu Group Coordinator không nhận được heartbeat trong khoảng thời gian này, Consumer bị coi là đã chết hoặc mất kết nối.

Ví dụ:

```text
heartbeat.interval.ms = 3s
session.timeout.ms = 45s

Consumer gửi heartbeat đều
-> Coordinator biết Consumer còn sống

Consumer crash hoặc mất network
-> không còn heartbeat
-> quá session.timeout.ms
-> Coordinator loại Consumer khỏi group và rebalance
```

#### Poll kiểm tra Consumer còn xử lý được không

`poll()` là lời gọi Consumer dùng để lấy record từ Kafka. Nhưng Kafka không chỉ quan tâm Consumer còn sống về mặt process; Kafka còn cần biết Consumer có đang xử lý dữ liệu kịp không.

Một Consumer có thể vẫn gửi heartbeat đều, nhưng application thread bị kẹt xử lý một batch quá lâu:

```text
Consumer vẫn heartbeat
-> process chưa chết

Nhưng application xử lý batch mất 30 phút
-> không gọi poll() tiếp
-> Partition đang assign cho Consumer này bị giữ quá lâu
-> các message mới ở Partition đó không được xử lý tiếp
```

Vì vậy Kafka có thêm `max.poll.interval.ms`. Nếu khoảng thời gian giữa hai lần gọi `poll()` vượt quá giá trị này, Kafka coi Consumer không còn xử lý dữ liệu đúng tiến độ và có thể loại nó khỏi group để rebalance Partition sang Consumer khác.

Các cấu hình liên quan:

- `max.poll.interval.ms`: thời gian tối đa giữa hai lần gọi `poll()` trước khi Consumer bị coi là xử lý quá chậm.
- `max.poll.records`: số record tối đa trả về trong một lần `poll()`, giúp giới hạn kích thước batch để xử lý không quá lâu.

#### Vì sao không dùng một cơ chế thôi?

Nếu chỉ dùng heartbeat:

```text
Consumer còn process và vẫn gửi heartbeat
nhưng code xử lý bị treo 30 phút
-> Coordinator vẫn tưởng Consumer ổn
-> Partition bị giữ, không Consumer khác xử lý thay
```

Nếu chỉ dùng poll:

```text
Consumer đang xử lý batch lớn hợp lệ trong vài chục giây
-> chưa gọi poll() tiếp
-> Coordinator có thể tưởng Consumer chết quá sớm
```

Vì vậy Kafka tách hai cơ chế:

```text
heartbeat
  -> kiểm tra Consumer còn sống và còn kết nối không

poll
  -> kiểm tra application có quay lại lấy dữ liệu trong thời gian hợp lý không
```

Tóm gọn:

```text
session.timeout.ms
  -> giới hạn thời gian mất heartbeat
  -> phát hiện crash/network issue

max.poll.interval.ms
  -> giới hạn thời gian không gọi poll()
  -> phát hiện xử lý quá chậm hoặc application bị kẹt
```

Nếu xử lý mỗi batch lâu, nên giảm `max.poll.records`, tối ưu business logic, hoặc đưa xử lý nặng sang worker riêng nhưng vẫn phải quản lý commit offset cẩn thận.

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

Kafka Replica là cơ chế tạo nhiều bản sao cho Partition để tăng khả năng chịu lỗi. Nếu một Broker gặp sự cố, Kafka vẫn có thể tiếp tục phục vụ dữ liệu từ Replica nằm trên Broker khác, miễn là cấu hình replication và ISR đủ an toàn.

Trong Kafka, đơn vị được replicate là **Partition**, không phải toàn bộ Topic theo một khối duy nhất. Một Topic có nhiều Partition, mỗi Partition có thể có nhiều Replica đặt trên các Broker khác nhau.

Ví dụ:

```text
Topic: payment-events
Partition 0 có replication-factor = 3

Replica 1 -> Broker 1
Replica 2 -> Broker 2
Replica 3 -> Broker 3
```

**Replica Factor** là số lượng bản sao của mỗi Partition, tính cả bản Leader.

```text
replication-factor = 1
  -> chỉ có 1 bản sao
  -> Broker chứa Partition lỗi thì Partition đó không phục vụ được

replication-factor = 3
  -> có 3 bản sao
  -> chịu lỗi tốt hơn nếu một Broker gặp sự cố
```

Trong môi trường local/dev chỉ có một Broker, `replication-factor = 1` là bình thường. Trong production, giá trị thường gặp là `3` vì cân bằng tốt giữa độ an toàn và chi phí tài nguyên.

![Kafka image 49](images/kafka-image-49.png)

Replication giúp Kafka giảm rủi ro mất dữ liệu và giảm downtime:

- Nếu Broker chứa một Follower bị lỗi, Leader vẫn phục vụ đọc/ghi bình thường.
- Nếu Broker chứa Leader bị lỗi, Kafka có thể bầu một Replica khác trong ISR làm Leader mới.
- Nếu dữ liệu đã được replicate đủ theo cấu hình `acks` và `min.insync.replicas`, rủi ro mất dữ liệu sẽ thấp hơn.

Replication cũng có chi phí:

- Tốn thêm disk vì dữ liệu được lưu nhiều bản.
- Tốn thêm network vì Leader phải gửi dữ liệu sang Follower.
- Ghi dữ liệu có thể chậm hơn nếu Producer dùng `acks=all`.
- Cần theo dõi các chỉ số như under-replicated partitions, ISR shrink/expand và broker disk usage.

![Kafka image 50](images/kafka-image-50.png)

Ví dụ `replication-factor = 2` nghĩa là mỗi Partition có 2 Replica. Nếu Broker đang chứa Leader bị lỗi, Kafka sẽ cố gắng chọn Replica còn lại làm Leader mới. Hệ thống vẫn có thể tiếp tục hoạt động nếu Replica còn lại đang đồng bộ đủ tốt và còn nằm trong ISR.

### Partition Leader

Mỗi Partition tại một thời điểm chỉ có một **Leader**. Producer ghi dữ liệu vào Leader, Consumer mặc định đọc dữ liệu từ Leader. Các Replica còn lại là Follower và sẽ fetch dữ liệu từ Leader để đồng bộ.

![Kafka image 51](images/kafka-image-51.png)

![Kafka image 52](images/kafka-image-52.png)

Ví dụ:

```text
Partition 0
Leader   -> Broker 1
Follower -> Broker 2
Follower -> Broker 3
```

Khi Producer gửi message vào Partition 0:

```text
Producer -> Broker 1 / Leader
Leader ghi message vào log
Follower fetch message từ Leader
Consumer đọc từ Leader theo offset
```

Nếu Broker 1 lỗi:

```text
Broker 1 down
Controller chọn Broker 2 hoặc Broker 3 làm Leader mới nếu đủ điều kiện
Producer/Consumer cập nhật metadata và làm việc với Leader mới
```

### Partition Follower

Follower là Replica không giữ vai trò Leader. Nhiệm vụ chính của Follower là đọc dữ liệu từ Leader và cập nhật log của mình để theo kịp Leader.

Follower bình thường không nhận request ghi trực tiếp từ Producer. Với Consumer, cách hiểu cơ bản là đọc từ Leader; một số cấu hình mới có thể cho phép fetch từ Replica gần hơn để tối ưu độ trễ mạng trong triển khai nhiều vùng địa lý.

### ISR

**ISR** là viết tắt của **In-Sync Replicas**. Đây là danh sách các Replica đang đồng bộ tốt với Leader.

Ví dụ:

```text
Partition 0
Leader: Broker 1
ISR: Broker 1, Broker 2, Broker 3
```

Nếu Broker 3 bị chậm hoặc mất kết nối, ISR có thể còn:

```text
ISR: Broker 1, Broker 2
```

Kafka ưu tiên chọn Leader mới từ ISR để giảm rủi ro mất dữ liệu. Nếu một Replica không theo kịp Leader trong thời gian cấu hình cho phép, nó sẽ bị loại khỏi ISR. Khi nó bắt kịp lại, nó có thể được đưa vào ISR trở lại.

### Ưu điểm và nhược điểm của Replica

Ưu điểm:

- Tăng khả năng chịu lỗi khi Broker bị down.
- Giảm rủi ro mất dữ liệu nếu Producer dùng cấu hình ghi an toàn.
- Cho phép Kafka bầu Leader mới khi Broker cũ gặp sự cố.
- Phù hợp với production workload cần độ bền dữ liệu cao.

Nhược điểm:

- Tốn thêm disk theo số lượng Replica.
- Tăng traffic network giữa các Broker.
- Có thể tăng latency ghi khi yêu cầu nhiều Replica xác nhận.
- Vận hành phức tạp hơn vì phải theo dõi ISR, replication lag và phân bổ dữ liệu giữa Broker.

Use case nên dùng `replication-factor = 3`:

- Payment event.
- Order event.
- Audit log.
- CDC event từ database.
- Event dùng để rebuild projection/search index.

Use case có thể dùng `replication-factor = 1`:

- Môi trường local.
- Test ngắn hạn.
- Dữ liệu demo có thể tạo lại.
- Pipeline thử nghiệm không yêu cầu độ bền dữ liệu.

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
