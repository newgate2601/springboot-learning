# WS-AtomicTransaction

## 1. WS-AtomicTransaction là gì?

WS-AtomicTransaction, thường viết là WS-AT, là một chuẩn trong nhóm WS-* dùng để điều phối transaction phân tán giữa nhiều SOAP Web Service.

Mục tiêu chính của WS-AT là đảm bảo nhiều service cùng tham gia một transaction chung theo nguyên tắc:

```text
Hoặc tất cả cùng commit, hoặc tất cả cùng rollback.
```

Ví dụ:

1. `OrderService` tạo đơn hàng.
2. `InventoryService` giữ hàng trong kho.
3. `PaymentService` trừ tiền.
4. Nếu tất cả thành công, toàn bộ transaction commit.
5. Nếu một service thất bại, toàn bộ transaction rollback.

Đây là bài toán distributed transaction.

## 2. Vấn đề WS-AT giải quyết

Nếu tất cả thao tác nằm trong cùng một database, transaction local có thể xử lý:

```sql
BEGIN;

INSERT INTO orders (...);
UPDATE inventory SET reserved = reserved + 1 WHERE product_id = ?;
INSERT INTO payment (...);

COMMIT;
```

Nhưng trong hệ thống phân tán, mỗi service thường có database riêng:

```text
OrderService      -> order_db
InventoryService  -> inventory_db
PaymentService    -> payment_db
```

Khi đó, transaction local không thể tự đảm bảo atomicity trên cả ba database.

Ví dụ lỗi:

```text
OrderService tạo order thành công
InventoryService giữ hàng thành công
PaymentService trừ tiền thất bại
```

Nếu không có cơ chế điều phối, hệ thống bị lệch trạng thái:

```text
Order đã tạo
Hàng đã bị giữ
Tiền chưa trừ
```

WS-AT cung cấp protocol để điều phối commit hoặc rollback giữa các service tham gia.

## 3. Atomic nghĩa là gì?

Atomic nghĩa là một transaction được xem như một đơn vị không thể chia nhỏ.

Với WS-AT:

- Nếu tất cả participant đồng ý commit, transaction commit.
- Nếu một participant từ chối hoặc lỗi, transaction rollback.
- Participant không được tự quyết định commit cuối cùng nếu transaction chung chưa có quyết định.

Mục tiêu là tránh trạng thái chỉ một phần service commit còn phần khác rollback.

## 4. Các chuẩn liên quan

WS-AtomicTransaction thường đi cùng các chuẩn sau:

| Chuẩn | Vai trò |
| --- | --- |
| WS-Coordination | Tạo coordination context và điều phối participant |
| WS-AtomicTransaction | Định nghĩa protocol atomic transaction |
| WS-Addressing | Định tuyến SOAP message giữa coordinator và participant |
| WS-Security | Bảo vệ message transaction |
| WS-Policy | Khai báo service có hỗ trợ transaction hay không |

Có thể hiểu:

- WS-Coordination là nền tảng điều phối chung.
- WS-AtomicTransaction là protocol transaction cụ thể chạy trên nền WS-Coordination.

## 5. Các vai trò chính

### 5.1. Initiator

Initiator là bên bắt đầu transaction.

Ví dụ:

- Client gọi luồng đặt hàng.
- `OrderService` bắt đầu orchestration.
- Một workflow service điều phối nhiều SOAP service.

Initiator yêu cầu coordinator tạo transaction context, sau đó truyền context này trong SOAP Header khi gọi các service khác.

### 5.2. Coordinator

Coordinator là thành phần điều phối transaction.

Coordinator chịu trách nhiệm:

- Tạo transaction context.
- Cung cấp registration service.
- Cho participant đăng ký tham gia transaction.
- Gửi lệnh `Prepare`.
- Thu thập vote từ participant.
- Quyết định `Commit` hoặc `Rollback`.
- Gửi quyết định cuối cùng cho participant.
- Hỗ trợ recovery khi có lỗi.

Coordinator tương đương transaction manager trong distributed transaction.

### 5.3. Participant

Participant là service tham gia transaction.

Ví dụ:

- `OrderService`.
- `InventoryService`.
- `PaymentService`.

Participant chịu trách nhiệm:

- Nhận transaction context từ SOAP Header.
- Đăng ký với coordinator.
- Thực hiện nghiệp vụ trong local transaction.
- Trả lời `Prepared` hoặc `Aborted` khi được hỏi.
- Commit hoặc rollback khi coordinator ra quyết định cuối cùng.

### 5.4. Registration Service

Registration Service là endpoint để participant đăng ký vào transaction.

Khi một service nhận SOAP request có transaction context, service đó có thể gọi Registration Service để báo:

```text
Tôi muốn tham gia transaction này.
```

Sau khi đăng ký, coordinator biết participant này cần được đưa vào quá trình two-phase commit.

### 5.5. Transaction Context

Transaction Context là metadata được truyền trong SOAP Header.

Nó thường chứa:

- Transaction identifier.
- Coordination type.
- Registration service address.
- Thông tin định tuyến bằng WS-Addressing.

Ví dụ ý tưởng:

```xml
<wscoor:CoordinationContext>
    <wscoor:Identifier>uuid:tx-001</wscoor:Identifier>
    <wscoor:CoordinationType>
        http://docs.oasis-open.org/ws-tx/wsat/2006/06
    </wscoor:CoordinationType>
    <wscoor:RegistrationService>
        <wsa:Address>https://tx.example.com/register</wsa:Address>
    </wscoor:RegistrationService>
</wscoor:CoordinationContext>
```

Service nhận context này biết request đang thuộc transaction `uuid:tx-001`.

## 6. Two-Phase Commit trong WS-AT

WS-AT chủ yếu dựa trên Two-Phase Commit, thường viết là 2PC.

2PC gồm hai phase:

1. Prepare phase.
2. Commit hoặc rollback phase.

### 6.1. Phase 1: Prepare

Coordinator hỏi tất cả participant:

```text
Bạn có sẵn sàng commit không?
```

Participant cần:

- Kiểm tra dữ liệu hợp lệ.
- Ghi transaction log nếu là durable participant.
- Giữ lock cần thiết.
- Đảm bảo nếu sau đó nhận lệnh commit thì có thể commit được.
- Trả lời `Prepared` hoặc `Aborted`.

Nếu participant trả lời `Prepared`, nó đã cam kết rằng có thể commit khi coordinator yêu cầu.

### 6.2. Phase 2: Commit hoặc Rollback

Nếu tất cả participant trả lời `Prepared`:

```text
Coordinator -> Commit
```

Nếu có một participant trả lời `Aborted`, lỗi hoặc timeout:

```text
Coordinator -> Rollback
```

Sau khi nhận quyết định cuối cùng:

- Participant commit local transaction nếu nhận `Commit`.
- Participant rollback local transaction nếu nhận `Rollback`.

## 7. Luồng hoạt động cơ bản

```text
1. Initiator yêu cầu coordinator tạo transaction.
2. Coordinator trả về transaction context.
3. Initiator gọi Service A, B, C và gắn transaction context vào SOAP Header.
4. Mỗi service nhận context và đăng ký participant với coordinator.
5. Mỗi service thực hiện nghiệp vụ nhưng chưa commit cuối cùng.
6. Initiator yêu cầu hoàn tất transaction.
7. Coordinator gửi Prepare tới tất cả participant.
8. Participant trả Prepared nếu sẵn sàng commit, hoặc Aborted nếu không thể.
9. Nếu tất cả Prepared, coordinator gửi Commit.
10. Nếu có lỗi hoặc Aborted, coordinator gửi Rollback.
11. Participant commit hoặc rollback local transaction.
12. Coordinator kết thúc transaction.
```

## 8. Ví dụ luồng đặt hàng

Giả sử có ba service:

```text
OrderService
InventoryService
PaymentService
```

Luồng thành công:

```text
Client -> Coordinator: Create transaction
Coordinator -> Client: CoordinationContext tx-001

Client -> OrderService: CreateOrder + tx-001
OrderService -> Coordinator: Register participant
OrderService: Insert order pending

Client -> InventoryService: ReserveStock + tx-001
InventoryService -> Coordinator: Register participant
InventoryService: Reserve stock pending

Client -> PaymentService: ChargePayment + tx-001
PaymentService -> Coordinator: Register participant
PaymentService: Charge pending

Client -> Coordinator: Complete transaction

Coordinator -> OrderService: Prepare
OrderService -> Coordinator: Prepared

Coordinator -> InventoryService: Prepare
InventoryService -> Coordinator: Prepared

Coordinator -> PaymentService: Prepare
PaymentService -> Coordinator: Prepared

Coordinator -> all participants: Commit

OrderService: Commit order
InventoryService: Commit reserved stock
PaymentService: Commit payment
```

Luồng thất bại:

```text
PaymentService -> Coordinator: Aborted

Coordinator -> OrderService: Rollback
Coordinator -> InventoryService: Rollback
Coordinator -> PaymentService: Rollback
```

Kết quả mong muốn:

```text
Không tạo order chính thức
Không giữ hàng
Không trừ tiền
```

## 9. Ví dụ SOAP Header có CoordinationContext

```xml
<soap:Envelope
    xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
    xmlns:wsa="http://www.w3.org/2005/08/addressing"
    xmlns:wscoor="http://docs.oasis-open.org/ws-tx/wscoor/2006/06"
    xmlns:wsat="http://docs.oasis-open.org/ws-tx/wsat/2006/06"
    xmlns:ord="http://example.com/order">

    <soap:Header>
        <wsa:To>https://shop.example.com/soap/order</wsa:To>
        <wsa:Action>http://example.com/order/CreateOrder</wsa:Action>
        <wsa:MessageID>uuid:msg-001</wsa:MessageID>

        <wscoor:CoordinationContext>
            <wscoor:Identifier>uuid:tx-001</wscoor:Identifier>
            <wscoor:CoordinationType>
                http://docs.oasis-open.org/ws-tx/wsat/2006/06
            </wscoor:CoordinationType>
            <wscoor:RegistrationService>
                <wsa:Address>https://tx.example.com/register</wsa:Address>
            </wscoor:RegistrationService>
        </wscoor:CoordinationContext>
    </soap:Header>

    <soap:Body>
        <ord:CreateOrderRequest>
            <ord:orderId>ORD-001</ord:orderId>
            <ord:customerId>CUS-001</ord:customerId>
            <ord:amount>1500000</ord:amount>
        </ord:CreateOrderRequest>
    </soap:Body>
</soap:Envelope>
```

Service nhận request này biết:

- Request thuộc transaction `uuid:tx-001`.
- Transaction type là WS-AtomicTransaction.
- Service có thể đăng ký participant qua registration service.
- Khi coordinator yêu cầu prepare, commit hoặc rollback, service phải phản hồi theo protocol.

## 10. Các protocol trong WS-AT

### 10.1. Completion Protocol

Completion Protocol dùng giữa initiator và coordinator.

Initiator dùng protocol này để yêu cầu:

```text
Commit transaction
```

hoặc:

```text
Rollback transaction
```

Coordinator sau đó thực hiện 2PC với các participant.

### 10.2. Volatile 2PC

Volatile 2PC dùng cho participant có trạng thái tạm thời, không cần durable log.

Ví dụ:

- Cache.
- In-memory state.
- Resource có thể tái tạo sau crash.

Volatile participant thường được xử lý trước durable participant.

### 10.3. Durable 2PC

Durable 2PC dùng cho participant có trạng thái bền vững.

Ví dụ:

- Database.
- Payment ledger.
- Inventory reservation.
- Message queue.

Durable participant phải có khả năng recover sau crash dựa trên transaction log.

## 11. WS-AT và ACID

### 11.1. Atomicity

Đây là mục tiêu chính của WS-AT.

Tất cả participant cùng commit hoặc cùng rollback.

### 11.2. Consistency

Mỗi service vẫn phải tự đảm bảo rule nghiệp vụ local.

Ví dụ:

- Kho không được âm.
- Tài khoản không được vượt hạn mức.
- Order không được chuyển trạng thái sai.

WS-AT không tự hiểu business rule.

### 11.3. Isolation

Participant có thể phải giữ lock hoặc trạng thái pending trong quá trình transaction.

Điều này giúp tránh transaction khác nhìn thấy dữ liệu chưa commit, nhưng cũng làm tăng rủi ro lock lâu và deadlock.

### 11.4. Durability

Durable participant phải ghi log.

Nếu participant crash sau khi đã trả `Prepared`, khi recover nó vẫn phải biết transaction đang chờ quyết định cuối cùng từ coordinator.

## 12. Điểm mạnh của WS-AT

WS-AT có các điểm mạnh:

- Cung cấp atomic commit giữa nhiều SOAP service.
- Phù hợp với môi trường enterprise dùng WS-*.
- Dựa trên 2PC, một mô hình quen thuộc trong transaction manager.
- Có thể phối hợp nhiều resource như database, JMS, SOAP service.
- Có khả năng interoperability giữa các vendor nếu cùng hỗ trợ chuẩn.

## 13. Hạn chế và rủi ro

### 13.1. Coupling cao

Các service phải cùng hỗ trợ WS-AT, WS-Coordination, WS-Addressing và thường cả WS-Security.

Điều này làm các service phụ thuộc mạnh vào hạ tầng transaction chung.

### 13.2. Blocking

2PC là protocol blocking.

Nếu coordinator crash sau khi participant đã `Prepared`, participant có thể phải giữ lock và chờ coordinator recover để biết commit hay rollback.

### 13.3. Lock lâu

Distributed transaction qua network thường giữ lock lâu hơn transaction local.

Ảnh hưởng:

- Throughput giảm.
- Latency tăng.
- Deadlock risk tăng.
- Khó scale khi số participant lớn.

### 13.4. Khó vận hành

Cần quản lý:

- Transaction coordinator.
- Recovery log.
- Timeout.
- Retry.
- Monitoring.
- Manual recovery khi trạng thái không rõ ràng.

### 13.5. Không phù hợp workflow dài

Nếu workflow kéo dài nhiều giây, nhiều phút hoặc phụ thuộc hệ thống bên ngoài, WS-AT thường không phù hợp.

Ví dụ không phù hợp:

- Gọi cổng thanh toán bên ngoài.
- Chờ callback.
- Gửi email.
- Chờ người dùng xác nhận.
- Quy trình phê duyệt nhiều bước.

## 14. WS-AT so với Saga

| Tiêu chí | WS-AtomicTransaction | Saga |
| --- | --- | --- |
| Mục tiêu | Tất cả commit hoặc rollback | Hoàn tác bằng compensating action |
| Consistency | Strong consistency | Eventual consistency |
| Cơ chế | Two-phase commit | Chuỗi local transaction |
| Lock distributed | Có thể giữ lock lâu | Không giữ lock distributed lâu |
| Coupling | Cao | Thấp hơn |
| Workflow dài | Không phù hợp | Phù hợp hơn |
| Microservices | Thường không khuyến nghị | Thường phù hợp hơn |
| Khi lỗi | Coordinator quyết định rollback | Chạy hành động bù trừ |

Ví dụ Saga:

```text
1. Create order
2. Reserve stock
3. Charge payment fails
4. Compensate:
   - Release stock
   - Cancel order
```

Saga không rollback database theo nghĩa transaction global. Nó thực hiện hành động bù trừ để đưa hệ thống về trạng thái chấp nhận được.

## 15. Khi nào nên dùng WS-AT?

Nên dùng khi:

- Các service SOAP đều hỗ trợ WS-AT.
- Nghiệp vụ cần atomic commit thật sự.
- Transaction ngắn.
- Số participant ít.
- Hạ tầng enterprise đã có transaction coordinator.
- Tất cả service nằm trong môi trường mạng được kiểm soát.
- Chi phí lock và coupling chấp nhận được.
- Cần tích hợp với hệ thống legacy yêu cầu WS-* transaction.

Ví dụ phù hợp:

```text
Một hệ thống nội bộ ngân hàng:
- Các service chạy trong cùng data center.
- Đều dùng SOAP stack hỗ trợ WS-AT.
- Transaction ngắn.
- Cần commit đồng thời database và JMS.
```

## 16. Khi nào không nên dùng WS-AT?

Không nên dùng khi:

- Workflow dài.
- Service gọi qua internet hoặc mạng không ổn định.
- Có nhiều participant.
- Cần scale cao.
- Các service thuộc nhiều team hoặc domain độc lập.
- Hệ thống theo microservices hiện đại.
- Có thể dùng Saga, outbox hoặc eventual consistency.

Ví dụ không phù hợp:

```text
Workflow thương mại điện tử:
- Tạo đơn hàng.
- Giữ hàng.
- Gọi cổng thanh toán bên ngoài.
- Chờ callback.
- Gửi email.
```

Luồng này dài, có external dependency và latency không ổn định. Saga hoặc event-driven workflow thường phù hợp hơn.

## 17. Solution thực tế nếu không dùng WS-AT

Trong kiến trúc hiện đại, nhiều hệ thống tránh distributed transaction và dùng các pattern sau.

### 17.1. Saga orchestration

Một orchestrator điều phối các bước:

```text
CreateOrder -> ReserveStock -> ChargePayment
```

Nếu lỗi:

```text
RefundPayment -> ReleaseStock -> CancelOrder
```

### 17.2. Saga choreography

Các service giao tiếp qua event:

```text
OrderCreated -> StockReserved -> PaymentCharged -> OrderConfirmed
```

Nếu lỗi:

```text
PaymentFailed -> StockReleased -> OrderCancelled
```

### 17.3. Outbox pattern

Service ghi business data và event vào outbox trong cùng local transaction.

Worker sau đó publish event từ outbox.

Điều này tránh lỗi:

```text
Database commit thành công nhưng event không được gửi.
```

### 17.4. Idempotency

Mỗi request quan trọng có một business key duy nhất:

```text
transactionId
orderId
idempotencyKey
```

Nếu request bị gửi lại, service trả kết quả cũ hoặc bỏ qua xử lý trùng.

## 18. Tóm tắt

WS-AtomicTransaction là chuẩn SOAP dùng để điều phối distributed transaction theo kiểu atomic commit.

Nó dựa trên:

- WS-Coordination để tạo và truyền transaction context.
- WS-AtomicTransaction để định nghĩa protocol commit/rollback.
- WS-Addressing để định tuyến message.
- Two-phase commit để tất cả participant cùng commit hoặc rollback.
- Coordinator để điều phối.
- Participant để thực hiện local transaction và vote prepare.

WS-AT mạnh khi cần strong consistency giữa các SOAP service trong môi trường enterprise được kiểm soát. Tuy nhiên, nó phức tạp, coupling cao, blocking, khó scale và không phù hợp với workflow dài hoặc microservices hiện đại.

Với hệ thống mới, cần cân nhắc kỹ giữa WS-AT và các giải pháp như Saga, outbox, idempotency và eventual consistency.
