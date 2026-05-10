# WS-ReliableMessaging

## 1. WS-ReliableMessaging là gì?

WS-ReliableMessaging, thường viết là WS-RM, là một chuẩn trong hệ sinh thái SOAP Web Services dùng để đảm bảo việc truyền nhận message đáng tin cậy giữa bên gửi và bên nhận.

Trong SOAP thông thường, client gửi request rồi chờ response. Nếu mạng lỗi, timeout, service crash, proxy lỗi hoặc message đi qua queue, client có thể không chắc:

- Request đã tới service chưa.
- Service đã xử lý request chưa.
- Response bị mất hay request bị mất.
- Retry có làm xử lý trùng nghiệp vụ không.
- Các message có được xử lý đúng thứ tự không.

WS-RM giải quyết các vấn đề này bằng cách thêm cơ chế sequence, message number, acknowledgement, retry và delivery assurance vào SOAP message.

## 2. Vấn đề thực tế

Ví dụ nghiệp vụ chuyển tiền:

1. Client gửi request `TransferMoney`.
2. Service nhận request và đã trừ tiền.
3. Kết nối bị timeout trước khi client nhận response.
4. Client không biết giao dịch đã xử lý hay chưa.
5. Nếu client gửi lại request, có nguy cơ trừ tiền hai lần.
6. Nếu client không gửi lại, có nguy cơ giao dịch chưa được xử lý.

WS-RM giúp client và service trao đổi acknowledgement để biết message nào đã được nhận. Tuy nhiên, WS-RM không thay thế idempotency và transaction nghiệp vụ. Với nghiệp vụ tiền bạc, backend vẫn phải có khóa chống trùng như `transactionId`, `requestId`, unique constraint hoặc idempotency key.

## 3. Namespace thường dùng

Phiên bản WS-RM 1.1 thường dùng namespace:

```xml
xmlns:wsrm="http://docs.oasis-open.org/ws-rx/wsrm/200702"
```

WS-RM thường dùng chung với WS-Addressing:

```xml
xmlns:wsa="http://www.w3.org/2005/08/addressing"
```

## 4. Các khái niệm chính

### 4.1. Reliable Messaging Source

Reliable Messaging Source là bên gửi message đáng tin cậy.

Ví dụ:

- Client gửi request SOAP tới service.
- Service A gửi message sang Service B.
- Hệ thống payment gửi giao dịch sang core banking.

Source chịu trách nhiệm:

- Tạo sequence.
- Đánh số message.
- Theo dõi acknowledgement.
- Retry message chưa được xác nhận.
- Kết thúc sequence khi hoàn tất.

### 4.2. Reliable Messaging Destination

Reliable Messaging Destination là bên nhận message đáng tin cậy.

Destination chịu trách nhiệm:

- Nhận message trong sequence.
- Lưu trạng thái message đã nhận.
- Phát hiện message trùng.
- Phát hiện message thiếu.
- Gửi acknowledgement.
- Xử lý thứ tự nếu policy yêu cầu `InOrder`.

### 4.3. Sequence

Sequence là một chuỗi message có liên quan với nhau.

```xml
<wsrm:Sequence>
    <wsrm:Identifier>uuid:sequence-001</wsrm:Identifier>
    <wsrm:MessageNumber>1</wsrm:MessageNumber>
</wsrm:Sequence>
```

Ý nghĩa:

- `Identifier`: định danh sequence.
- `MessageNumber`: số thứ tự của message trong sequence.

Destination dựa vào sequence để biết message nào đã nhận, message nào bị thiếu, message nào bị gửi trùng.

### 4.4. MessageNumber

`MessageNumber` là số thứ tự tăng dần của message trong một sequence.

Ví dụ:

```xml
<wsrm:MessageNumber>3</wsrm:MessageNumber>
```

Ý nghĩa:

- Đây là message số 3 trong sequence.
- Nếu destination đã nhận message 1 và 3 nhưng chưa nhận message 2, nó biết sequence đang bị thiếu message.
- Nếu destination đã nhận message 3 rồi lại nhận message 3 lần nữa, nó biết đây là duplicate.

### 4.5. SequenceAcknowledgement

`SequenceAcknowledgement` là header dùng để xác nhận các message đã nhận.

```xml
<wsrm:SequenceAcknowledgement>
    <wsrm:Identifier>uuid:sequence-001</wsrm:Identifier>
    <wsrm:AcknowledgementRange Lower="1" Upper="3"/>
</wsrm:SequenceAcknowledgement>
```

Ý nghĩa:

- Destination đã nhận các message từ số 1 đến số 3.
- Source có thể ngừng retry các message này.
- Nếu message nào chưa được ack, source có thể gửi lại.

Acknowledgement có thể được gửi riêng hoặc được gắn kèm trong SOAP response.

### 4.6. CreateSequence

Trước khi gửi message đáng tin cậy, source tạo sequence:

```xml
<wsrm:CreateSequence>
    <wsrm:AcksTo>
        <wsa:Address>http://www.w3.org/2005/08/addressing/anonymous</wsa:Address>
    </wsrm:AcksTo>
</wsrm:CreateSequence>
```

Destination trả về:

```xml
<wsrm:CreateSequenceResponse>
    <wsrm:Identifier>uuid:sequence-001</wsrm:Identifier>
</wsrm:CreateSequenceResponse>
```

Sau đó, các message nghiệp vụ sẽ dùng `uuid:sequence-001`.

### 4.7. CloseSequence và TerminateSequence

`CloseSequence` báo rằng source không gửi thêm message mới vào sequence nữa, nhưng vẫn cần acknowledgement cuối cùng.

`TerminateSequence` báo rằng sequence đã kết thúc và destination có thể dọn trạng thái.

```xml
<wsrm:TerminateSequence>
    <wsrm:Identifier>uuid:sequence-001</wsrm:Identifier>
</wsrm:TerminateSequence>
```

## 5. Ví dụ SOAP request có WS-RM

```xml
<soap:Envelope
    xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
    xmlns:wsa="http://www.w3.org/2005/08/addressing"
    xmlns:wsrm="http://docs.oasis-open.org/ws-rx/wsrm/200702"
    xmlns:pay="http://example.com/payment">

    <soap:Header>
        <wsa:To>https://bank.example.com/soap/payment</wsa:To>
        <wsa:Action>http://example.com/payment/TransferMoney</wsa:Action>
        <wsa:MessageID>uuid:msg-001</wsa:MessageID>

        <wsrm:Sequence>
            <wsrm:Identifier>uuid:sequence-001</wsrm:Identifier>
            <wsrm:MessageNumber>1</wsrm:MessageNumber>
        </wsrm:Sequence>
    </soap:Header>

    <soap:Body>
        <pay:TransferMoneyRequest>
            <pay:transactionId>TXN-20260510-0001</pay:transactionId>
            <pay:fromAccount>111111</pay:fromAccount>
            <pay:toAccount>222222</pay:toAccount>
            <pay:amount>500000</pay:amount>
            <pay:currency>VND</pay:currency>
        </pay:TransferMoneyRequest>
    </soap:Body>
</soap:Envelope>
```

Giải thích:

- `wsa:MessageID`: định danh SOAP message.
- `wsrm:Identifier`: định danh sequence.
- `wsrm:MessageNumber`: số thứ tự message trong sequence.
- `transactionId`: khóa nghiệp vụ để chống xử lý trùng ở backend.

## 6. Delivery Assurance là gì?

Delivery Assurance là mức đảm bảo truyền nhận message mà hệ thống mong muốn.

Các mức thường gặp:

- `AtMostOnce`: xử lý tối đa một lần.
- `AtLeastOnce`: xử lý ít nhất một lần.
- `ExactlyOnce`: xử lý đúng một lần.
- `InOrder`: xử lý đúng thứ tự.

Các guarantee này thường được khai báo bằng WS-Policy và được thực thi bởi SOAP stack, middleware hoặc framework WS-RM.

Điểm quan trọng: delivery assurance ở tầng messaging không tự động đảm bảo nghiệp vụ đúng tuyệt đối. Với nghiệp vụ quan trọng, cần kết hợp thêm thiết kế database, transaction và idempotency.

## 7. AtMostOnce

### 7.1. Ý nghĩa

`AtMostOnce` nghĩa là một message được xử lý tối đa một lần.

Nếu message bị gửi trùng, destination phải phát hiện duplicate và không xử lý lại.

Kết quả:

- Không có xử lý trùng.
- Có thể mất message nếu message không tới nơi và không retry thành công.

### 7.2. Khi nào dùng?

Phù hợp khi xử lý trùng nguy hiểm hơn mất message.

Ví dụ:

- Trừ tiền.
- Tạo giao dịch tài chính.
- Gửi lệnh mua bán.
- Tạo đơn hàng không có idempotency tốt.

### 7.3. Solution để đạt AtMostOnce

Để đạt `AtMostOnce`, destination cần có duplicate detection.

Các thành phần nên có:

- Lưu `Sequence Identifier` và `MessageNumber` đã nhận.
- Hoặc lưu `wsa:MessageID` đã nhận.
- Hoặc tốt hơn, lưu `business request id` như `transactionId`, `orderId`, `idempotencyKey`.
- Tạo unique constraint trong database cho khóa chống trùng.
- Nếu nhận lại message cũ, trả lại kết quả cũ hoặc bỏ qua.

Ví dụ bảng chống trùng:

```sql
CREATE TABLE processed_message (
    id BIGINT PRIMARY KEY,
    message_id VARCHAR(100) NOT NULL UNIQUE,
    sequence_id VARCHAR(100),
    message_number BIGINT,
    business_key VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

Với nghiệp vụ chuyển tiền, nên unique theo `transactionId`:

```sql
ALTER TABLE payment_transaction
ADD CONSTRAINT uk_payment_transaction_id UNIQUE (transaction_id);
```

Pseudo flow:

```text
Receive message
Check transactionId or messageId exists
If exists:
    Do not process business logic again
    Return previous result or acknowledgement
If not exists:
    Save transactionId/messageId as processing
    Execute business transaction
    Mark as completed
    Send acknowledgement
```

### 7.4. Trade-off

`AtMostOnce` tránh duplicate tốt, nhưng nếu message thật sự bị mất trước khi destination nhận được, nó có thể không được xử lý.

Vì vậy, `AtMostOnce` phù hợp khi hệ thống chấp nhận mất message hơn là xử lý trùng, hoặc khi có cơ chế reconciliation riêng để phát hiện thiếu giao dịch.

## 8. AtLeastOnce

### 8.1. Ý nghĩa

`AtLeastOnce` nghĩa là message được xử lý ít nhất một lần.

Source sẽ retry cho tới khi nhận được acknowledgement.

Kết quả:

- Giảm nguy cơ mất message.
- Có thể phát sinh duplicate nếu destination nhận được message nhiều lần.

### 8.2. Khi nào dùng?

Phù hợp khi mất message nguy hiểm hơn xử lý trùng, hoặc operation là idempotent.

Ví dụ:

- Gửi event cập nhật cache.
- Đồng bộ dữ liệu có thể upsert.
- Gửi notification có thể chấp nhận trùng nhẹ.
- Cập nhật trạng thái theo version mới nhất.

### 8.3. Solution để đạt AtLeastOnce

Để đạt `AtLeastOnce`, source cần retry cho tới khi có acknowledgement.

Các thành phần nên có ở source:

- Outbox lưu message cần gửi.
- Trạng thái gửi: `PENDING`, `SENT`, `ACKED`, `FAILED`.
- Retry policy.
- Timeout chờ acknowledgement.
- Backoff để tránh retry quá dày.
- Dead letter hoặc manual review khi retry quá số lần.

Ví dụ bảng outbox:

```sql
CREATE TABLE soap_outbox (
    id BIGINT PRIMARY KEY,
    message_id VARCHAR(100) NOT NULL UNIQUE,
    sequence_id VARCHAR(100),
    message_number BIGINT,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    retry_count INT NOT NULL,
    next_retry_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

Pseudo flow ở source:

```text
Create business data and outbox message in same transaction
Send SOAP message
Wait for SequenceAcknowledgement
If ack received:
    Mark outbox as ACKED
If timeout or network error:
    Retry later
If retry exceeds limit:
    Move to FAILED or manual review
```

Destination vẫn nên có duplicate detection, vì retry có thể làm cùng một message tới nhiều lần.

### 8.4. Trade-off

`AtLeastOnce` bảo vệ tốt trước mất message, nhưng không tự bảo vệ khỏi xử lý trùng.

Nếu dùng `AtLeastOnce` cho nghiệp vụ không idempotent, backend có thể tạo duplicate transaction. Vì vậy destination nên thiết kế idempotent ngay cả khi guarantee chính là `AtLeastOnce`.

## 9. ExactlyOnce

### 9.1. Ý nghĩa

`ExactlyOnce` nghĩa là message được xử lý đúng một lần.

Về mặt ý tưởng, nó kết hợp:

- Retry để không mất message.
- Duplicate detection để không xử lý trùng.

Kết quả mong muốn:

- Message không bị mất.
- Message không bị xử lý trùng.

### 9.2. Lưu ý rất quan trọng

Trong hệ thống phân tán, `ExactlyOnce` tuyệt đối là rất khó.

WS-RM có thể cung cấp `ExactlyOnce` ở tầng messaging theo nghĩa:

- Destination nhận biết message nào đã đến.
- Source retry message chưa được ack.
- Destination loại bỏ duplicate.

Nhưng điều đó không tự động đảm bảo nghiệp vụ chỉ thay đổi database đúng một lần nếu code xử lý không idempotent.

Ví dụ nguy hiểm:

1. Service nhận message.
2. Service trừ tiền thành công trong database.
3. Service crash trước khi lưu trạng thái message đã xử lý hoặc trước khi gửi ack.
4. Source retry.
5. Nếu service không có `transactionId` unique, tiền có thể bị trừ lần hai.

### 9.3. Khi nào dùng?

Phù hợp với nghiệp vụ quan trọng:

- Payment.
- Core banking.
- Insurance claim.
- Order processing.
- Đồng bộ trạng thái pháp lý hoặc hồ sơ.

### 9.4. Solution để đạt ExactlyOnce

Để đạt `ExactlyOnce` thực tế, cần kết hợp nhiều lớp.

Ở tầng WS-RM:

- Dùng `Sequence Identifier`.
- Dùng `MessageNumber`.
- Dùng `SequenceAcknowledgement`.
- Retry message chưa được ack.
- Loại bỏ duplicate message.

Ở tầng application:

- Dùng idempotency key hoặc business key.
- Lưu trạng thái xử lý message.
- Ràng buộc unique trong database.
- Gói logic nghiệp vụ và lưu trạng thái message trong cùng database transaction nếu có thể.
- Nếu không cùng transaction được, dùng outbox/inbox pattern.

Ở destination, nên dùng inbox pattern:

```sql
CREATE TABLE soap_inbox (
    id BIGINT PRIMARY KEY,
    message_id VARCHAR(100) NOT NULL UNIQUE,
    sequence_id VARCHAR(100),
    message_number BIGINT,
    business_key VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    response_payload TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

Pseudo flow ở destination:

```text
Receive message
Begin transaction
Try insert message_id/business_key into inbox
If insert fails because duplicate:
    Load previous result
    Return acknowledgement and previous response
If insert succeeds:
    Execute business logic
    Save business result
    Save response payload
    Mark inbox as COMPLETED
Commit transaction
Send acknowledgement
```

Với payment:

```text
transactionId = TXN-20260510-0001
Unique constraint on payment_transaction.transaction_id
Retry with same transactionId returns existing transaction result
Retry never creates a second debit
```

### 9.5. Outbox + Inbox solution

Một solution phổ biến để đạt gần `ExactlyOnce` trong thực tế là kết hợp outbox ở source và inbox ở destination.

Source:

- Lưu nghiệp vụ và message cần gửi vào outbox trong cùng transaction.
- Worker gửi message từ outbox.
- Chỉ đánh dấu `ACKED` khi nhận acknowledgement.
- Retry nếu chưa ack.

Destination:

- Lưu message đã nhận vào inbox.
- Dùng unique key để chống trùng.
- Xử lý nghiệp vụ trong cùng transaction với inbox nếu có thể.
- Nếu duplicate, trả lại response cũ hoặc chỉ ack lại.

Luồng tổng quát:

```text
Source DB transaction:
    Save business change
    Save outbox message

Outbox worker:
    Send SOAP message with WS-RM sequence
    Wait for ack
    Retry until ack

Destination DB transaction:
    Insert inbox record with unique messageId/businessKey
    Execute business logic
    Save result
    Mark inbox completed

Destination:
    Send SequenceAcknowledgement

Source:
    Mark outbox message as ACKED
```

### 9.6. Trade-off

`ExactlyOnce` là guarantee mạnh nhưng tốn chi phí:

- Cần lưu trạng thái message.
- Cần transaction tốt.
- Cần xử lý retry phức tạp.
- Cần cleanup sequence/inbox/outbox.
- Cần thiết kế idempotency rõ ràng.

Không nên dùng quá mức cho các API read-only hoặc nghiệp vụ không quan trọng.

## 10. InOrder

### 10.1. Ý nghĩa

`InOrder` nghĩa là các message trong cùng sequence phải được xử lý đúng thứ tự `MessageNumber`.

Ví dụ:

```text
Message 1: CreateOrder
Message 2: PayOrder
Message 3: ConfirmOrder
```

Nếu message 3 tới trước message 2, destination không được xử lý message 3 ngay nếu policy yêu cầu `InOrder`.

### 10.2. Khi nào dùng?

Phù hợp khi nghiệp vụ phụ thuộc thứ tự:

- Tạo đơn hàng rồi thanh toán rồi xác nhận.
- Tạo hồ sơ rồi cập nhật rồi phê duyệt.
- Mở tài khoản rồi kích hoạt rồi ghi nhận giao dịch.
- Đồng bộ trạng thái theo chuỗi sự kiện.

Không cần thiết nếu message độc lập hoặc có version/timestamp để tự quyết định trạng thái mới nhất.

### 10.3. Solution để đạt InOrder

Để đạt `InOrder`, destination cần quản lý expected message number cho từng sequence.

Các thành phần nên có:

- Bảng lưu trạng thái sequence.
- `last_processed_message_number`.
- Buffer cho message tới sớm.
- Lock theo sequence khi xử lý.
- Timeout hoặc dead letter nếu message bị thiếu quá lâu.

Ví dụ bảng sequence state:

```sql
CREATE TABLE wsrm_sequence_state (
    sequence_id VARCHAR(100) PRIMARY KEY,
    last_processed_message_number BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

Ví dụ bảng buffer:

```sql
CREATE TABLE wsrm_message_buffer (
    id BIGINT PRIMARY KEY,
    sequence_id VARCHAR(100) NOT NULL,
    message_number BIGINT NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE (sequence_id, message_number)
);
```

Pseudo flow:

```text
Receive message with sequenceId and messageNumber
Lock sequence state
expected = lastProcessedMessageNumber + 1

If messageNumber == expected:
    Process message
    Update lastProcessedMessageNumber
    Check buffer for next messages
    Process buffered messages in order while available

If messageNumber > expected:
    Save message to buffer
    Send acknowledgement that message is received, or wait depending on policy
    Do not process business logic yet

If messageNumber <= lastProcessedMessageNumber:
    Treat as duplicate
    Do not process again
```

### 10.4. Trade-off

`InOrder` làm hệ thống an toàn hơn với nghiệp vụ phụ thuộc thứ tự, nhưng có nhược điểm:

- Tăng latency nếu message bị thiếu.
- Cần buffer message tới sớm.
- Một message lỗi có thể chặn các message sau.
- Cần cơ chế timeout, retry hoặc manual recovery.

Nếu nghiệp vụ có thể thiết kế theo idempotent event hoặc state version, đôi khi không cần `InOrder`. Ví dụ chỉ chấp nhận update có `version` lớn hơn version hiện tại.

## 11. So sánh nhanh các guarantee

| Guarantee | Mục tiêu | Cơ chế chính | Rủi ro còn lại | Solution nên có |
| --- | --- | --- | --- | --- |
| `AtMostOnce` | Không xử lý trùng | Duplicate detection | Có thể mất message | Inbox, unique key, idempotency key |
| `AtLeastOnce` | Không mất message | Retry tới khi ack | Có thể xử lý trùng | Outbox, retry, ack tracking, destination idempotent |
| `ExactlyOnce` | Không mất và không trùng | Retry + duplicate detection | Khó đảm bảo tuyệt đối nếu nghiệp vụ không idempotent | Outbox + inbox + transaction + unique business key |
| `InOrder` | Đúng thứ tự | MessageNumber + sequence state | Message thiếu có thể chặn luồng | Sequence lock, buffer, timeout, dead letter |

## 12. Quan hệ giữa các guarantee

Các guarantee có thể kết hợp.

Ví dụ:

```text
ExactlyOnce + InOrder
```

Ý nghĩa:

- Mỗi message được xử lý đúng một lần.
- Các message trong cùng sequence được xử lý đúng thứ tự.

Đây là cấu hình mạnh nhưng phức tạp, phù hợp với các luồng nghiệp vụ quan trọng có phụ thuộc thứ tự.

Ví dụ:

```text
AtLeastOnce without InOrder
```

Ý nghĩa:

- Message sẽ được retry để hạn chế mất message.
- Nhưng các message có thể tới và được xử lý không theo thứ tự.

Phù hợp với event độc lập hoặc operation idempotent.

## 13. Quan hệ với WS-Addressing

WS-RM thường dùng chung với WS-Addressing.

WS-Addressing trả lời:

- Message gửi tới đâu?
- Action là gì?
- Reply gửi về đâu?
- Message hiện tại liên quan tới message nào?

WS-RM trả lời:

- Message đã tới chưa?
- Message nào bị thiếu?
- Message nào bị trùng?
- Message có được xử lý đúng thứ tự không?

Ví dụ WS-Addressing header:

```xml
<wsa:To>https://bank.example.com/soap/payment</wsa:To>
<wsa:Action>http://example.com/payment/TransferMoney</wsa:Action>
<wsa:MessageID>uuid:msg-001</wsa:MessageID>
<wsa:ReplyTo>
    <wsa:Address>http://www.w3.org/2005/08/addressing/anonymous</wsa:Address>
</wsa:ReplyTo>
```

Ví dụ WS-RM header:

```xml
<wsrm:Sequence>
    <wsrm:Identifier>uuid:sequence-001</wsrm:Identifier>
    <wsrm:MessageNumber>1</wsrm:MessageNumber>
</wsrm:Sequence>
```

## 14. Quan hệ với WS-Security

WS-RM không mã hóa message, không ký số message và không xác thực người gửi.

Trong hệ thống thật, WS-RM thường đi cùng WS-Security để đảm bảo:

- Message không bị sửa.
- Header `Sequence`, `MessageNumber`, `MessageID` không bị giả mạo.
- Người gửi được xác thực.
- Nội dung nhạy cảm được mã hóa.

Nếu không có WS-Security, attacker có thể giả mạo message hoặc replay message, làm duplicate detection và sequence handling trở nên không đáng tin cậy.

## 15. WS-RM không thay thế transaction

WS-RM đảm bảo truyền nhận ở tầng message, nhưng không thay thế transaction nghiệp vụ.

Ví dụ:

1. Destination nhận message.
2. Destination ghi database thành công.
3. Destination crash trước khi gửi ack.
4. Source không nhận ack nên retry.
5. Destination nhận lại message.

Nếu backend không có idempotency, nghiệp vụ có thể bị xử lý hai lần.

Vì vậy, với nghiệp vụ quan trọng, nên kết hợp:

- WS-RM.
- Database transaction.
- Unique constraint.
- Idempotency key.
- Inbox/outbox pattern.
- Reconciliation job.

## 16. Khi nào nên dùng WS-RM?

Nên dùng khi:

- SOAP message không được phép mất.
- Message đi qua ESB, queue, gateway hoặc mạng không ổn định.
- Cần acknowledgement chuẩn WS-*.
- Cần phát hiện duplicate ở tầng SOAP.
- Cần đảm bảo thứ tự message.
- Hệ thống tích hợp enterprise yêu cầu WS-RM.

Không nhất thiết cần khi:

- SOAP service chỉ là HTTP request-response đơn giản.
- API chủ yếu là read-only.
- Operation có thể gọi lại an toàn.
- Hệ thống đã dùng message broker có reliability riêng và không cần interoperability WS-*.
- REST hoặc event streaming hiện đại đã đáp ứng đủ yêu cầu.

## 17. Tóm tắt

WS-ReliableMessaging là chuẩn SOAP giúp truyền nhận message đáng tin cậy thông qua sequence, message number, acknowledgement và retry.

Các thành phần quan trọng:

| Thành phần | Ý nghĩa |
| --- | --- |
| `CreateSequence` | Tạo sequence mới |
| `Sequence Identifier` | Định danh sequence |
| `MessageNumber` | Số thứ tự message trong sequence |
| `SequenceAcknowledgement` | Xác nhận message đã nhận |
| `CloseSequence` | Đóng sequence, không gửi thêm message mới |
| `TerminateSequence` | Kết thúc và dọn trạng thái sequence |

Các guarantee quan trọng:

| Guarantee | Hiểu ngắn gọn |
| --- | --- |
| `AtMostOnce` | Không xử lý quá một lần |
| `AtLeastOnce` | Cố gắng xử lý ít nhất một lần bằng retry |
| `ExactlyOnce` | Kết hợp retry và chống trùng để xử lý đúng một lần |
| `InOrder` | Xử lý message đúng thứ tự |

Trong thực tế, để đạt reliability đúng nghĩa, không nên chỉ dựa vào WS-RM header. Cần thiết kế thêm idempotency, transaction, unique constraint, outbox/inbox và reconciliation cho các nghiệp vụ quan trọng.
