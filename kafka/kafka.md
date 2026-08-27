# Apache Kafka

## Tài liệu tham khảo

- <https://kafka.apache.org/documentation/>
- <https://developer.confluent.io/courses/kafka-connect/intro/>
- <https://developer.confluent.io/learn/kafka-transactions-and-guarantees/>
- <https://piotrminkowski.com/2022/01/24/distributed-transactions-in-microservices-with-kafka-streams-and-spring-boot/>
- <https://romanglushach.medium.com/the-evolution-of-kafka-architecture-from-zookeeper-to-kraft-f42d511ba242>

## Kafka vs RabbitMQ Overview

- Message: Kafka persistence >< RabbitMQ xóa ngay sau khi nhận ACK từ consumer.
- Kafka dùng batching nên thông lượng cao hơn >< RabbitMQ từng message.
- Performance: Kafka performance tốt hơn, hàng triệu messages 1s, thiết kế để hoạt động trong môi trường distributed để xử lý song song >< RabbitMQ cũng tốt nhưng không bằng Kafka, khoảng 10000 message 1s.
- Độ tin cậy, durability: Kafka độ tin cậy cao, persist message trên disk, hỗ trợ replica để đảm bảo dữ liệu không bị mất nếu node bị lỗi, ACK >< RabbitMQ cũng dùng ACK, replica.
- Consume: Kafka dùng pull >< RabbitMQ mặc định dùng push, có thể chuyển sang pull
- Usecase: Kafka dùng trong xử lý big data, real time, monitoring >< RabbitMQ hạn chế hơn, phù hợp với truyền thông giữa các service, tác vụ không yêu cầu thông lượng cao.
- Scale: Kafka dễ scale hơn RabbitMQ.

## Pipeline

![Kafka image 1](images/kafka-image-01.png)

![Kafka image 2](images/kafka-image-02.png)

**Data pipeline** là một luồng xử lý dữ liệu tự động hoặc bán tự động. Nó lấy dữ liệu từ một hoặc nhiều nguồn, xử lý/biến đổi dữ liệu đó, rồi đưa kết quả tới một hoặc nhiều nơi nhận.

Trong context Kafka, pipeline thường là **event streaming pipeline**: dữ liệu được ghi nhận dưới dạng các event, lưu vào Kafka topic, sau đó nhiều consumer hoặc connector có thể đọc cùng dòng event đó để xử lý theo mục đích riêng.

Nói đơn giản:

- **Data source**: nơi phát sinh hoặc lưu dữ liệu gốc, ví dụ MySQL, PostgreSQL, MongoDB, log file, API, app backend.
- **Kafka topic**: nơi Kafka lưu dòng event. Topic giống một dòng dữ liệu có tên, ví dụ `order-created`, `payment-succeeded`, `user-clicked-product`.
- **Pipeline processor**: phần xử lý ở giữa, có thể dùng Kafka Streams, Flink, consumer service tự viết, hoặc job xử lý dữ liệu khác.
- **Data sink**: nơi nhận dữ liệu sau xử lý, ví dụ Elasticsearch, Redis, Data Warehouse, S3, dashboard realtime, service khác.
- **Source connector**: connector đọc dữ liệu từ hệ thống ngoài rồi ghi vào Kafka, ví dụ đọc thay đổi từ PostgreSQL vào topic.
- **Sink connector**: connector đọc dữ liệu từ Kafka rồi ghi ra hệ thống ngoài, ví dụ đẩy event từ Kafka sang Elasticsearch hoặc S3.

Ví dụ thực tế trong hệ thống bán hàng:

```text
Order Service
  -> Kafka topic: order-created
  -> Fraud Check Service
  -> Payment Service
  -> Data Warehouse
  -> Realtime Dashboard
```

Khi user đặt hàng, `Order Service` không cần gọi trực tiếp tất cả service còn lại. Nó chỉ publish event `order-created` vào Kafka. Các service khác subscribe event đó và xử lý theo nhu cầu riêng. Cách này giúp hệ thống dễ mở rộng hơn, vì thêm service mới chỉ cần subscribe topic, không phải sửa lại flow chính của `Order Service`.

Pipeline quan trọng vì:

- Giảm thao tác thủ công khi di chuyển/xử lý dữ liệu.
- Tách rời source và sink, giúp hệ thống bớt phụ thuộc trực tiếp vào nhau.
- Kafka có thể đóng vai trò buffer ở giữa, giúp hệ thống chịu tải tốt hơn khi consumer xử lý chậm.
- Dễ scale từng phần: producer, Kafka broker, partition, consumer, connector, storage.
- Có thể replay dữ liệu trong thời gian Kafka còn giữ log, hữu ích khi downstream service lỗi hoặc cần xử lý lại.
- Phù hợp cho logging, analytics, monitoring, CDC, notification, recommendation, fraud detection.

![Kafka image 3](images/kafka-image-03.png)

### ETL và ELT

ETL và ELT đều là cách đưa dữ liệu từ source tới hệ thống đích. Điểm khác nhau quan trọng nhất là **transform xảy ra trước hay sau khi dữ liệu được load vào nơi lưu trữ đích**.

| Mô hình | Luồng xử lý | Khi nào hợp | Điểm cần chú ý |
| --- | --- | --- | --- |
| **ETL** | `Extract -> Transform -> Load` | Sink chỉ nên nhận dữ liệu sạch; cần mask dữ liệu nhạy cảm trước khi lưu; cần chuẩn hóa realtime để nhiều downstream dùng chung | Phải định nghĩa transform sớm. Nếu logic sai, cần sửa logic rồi replay/backfill dữ liệu |
| **ELT** | `Extract -> Load -> Transform` | Data Lake/Lakehouse/Warehouse có storage và compute mạnh; muốn giữ raw data để audit, thử nhiều model hoặc transform lại | Raw zone cần governance, phân quyền, schema, retention và kiểm soát PII chặt chẽ |

#### ETL là gì?

**ETL** là viết tắt của `Extract -> Transform -> Load`:

- **Extract**: lấy dữ liệu từ database, API, log, file, application event,...
- **Transform**: validate schema, lọc record lỗi, đổi format/timezone/currency, mask PII, deduplicate, join, enrich hoặc aggregate.
- **Load**: ghi dữ liệu đã được xử lý vào Data Warehouse, Elasticsearch, database phục vụ báo cáo hoặc một Kafka topic khác.

Ví dụ thực tế: hệ thống bán hàng cần tạo dữ liệu order chuẩn hóa cho dashboard realtime.

```text
MySQL binlog
  -> CDC source connector
  -> Kafka topic: mysql.shop.orders.raw
  -> Kafka Streams/Flink
       - chỉ giữ order hợp lệ
       - đổi USD sang VND
       - chuẩn hóa created_at về UTC
       - loại bỏ/mask thông tin nhạy cảm
  -> Kafka topic: analytics.orders.cleaned
  -> Warehouse sink connector
  -> bảng fact_orders
```

Đây là **streaming ETL** vì dữ liệu được transform trước khi load vào bảng đích `fact_orders`. Kafka topic `mysql.shop.orders.raw` vẫn có thể giữ event gốc trong một khoảng retention để debug hoặc replay, nhưng xét theo đích phân tích thì bước transform vẫn nằm trước bước load.

#### ELT là gì?

**ELT** là viết tắt của `Extract -> Load -> Transform`. Dữ liệu được load gần như nguyên bản vào Data Lake, Lakehouse hoặc staging/raw table trước; các SQL job, dbt, Spark hoặc engine của Warehouse mới tạo các bảng đã chuẩn hóa sau đó.

```text
MySQL/PostgreSQL/SaaS API
  -> source connector hoặc CDC
  -> Kafka raw topics
  -> S3/GCS/ADLS/BigQuery/Snowflake raw zone
  -> SQL/dbt/Spark transform
  -> staging_orders
  -> fact_orders
  -> sales_daily
```

Đây là **streaming ingestion + ELT**: Kafka và sink connector đưa dữ liệu mới vào raw zone liên tục, nhưng transform business chính diễn ra sau khi load. ELT không đồng nghĩa với batch; bước transform có thể chạy theo lịch, micro-batch hoặc continuous tùy nền tảng đích.

#### Kafka nằm ở đâu trong ETL/ELT?

Kafka là **event streaming platform và lớp trung chuyển/lưu event**, không phải cứ đưa dữ liệu qua Kafka thì pipeline tự động trở thành ETL hoặc ELT.

- **Kafka Connect** chủ yếu di chuyển dữ liệu vào/ra Kafka. `Source Connector` đọc từ hệ thống ngoài và ghi vào topic; `Sink Connector` đọc topic rồi ghi sang hệ thống đích.
- **Single Message Transform (SMT)** của Kafka Connect hợp với thay đổi đơn giản trên từng record như rename field, thêm timestamp, route topic hoặc mask field. Không nên dùng SMT cho join, aggregate hoặc business logic phức tạp.
- **Kafka Streams/Flink/ksqlDB** phù hợp với transform phức tạp như filter, branch, join, enrich, aggregate, windowing và xử lý theo event time.
- **Data Warehouse/Lakehouse engine** phù hợp khi chọn ELT và muốn transform bằng SQL/dbt/Spark sau khi raw data đã được load.

Hai kiến trúc thường gặp:

```text
Streaming ETL
Source -> Kafka raw topic -> Stream processor -> Kafka cleaned topic -> Sink

Streaming ingestion + ELT
Source -> Kafka raw topic -> Raw storage/table -> Transform trong Lakehouse/Warehouse
```

Kafka có thể xuất hiện ở cả ETL lẫn ELT. Thậm chí một hệ thống thường dùng **hybrid**: mask PII và validate schema trước khi load, sau đó mới join/aggregate trong Warehouse.

#### Ví dụ hybrid thực tế

Giả sử công ty cần phân tích payment nhưng không được đưa số thẻ thô vào Data Lake:

```text
Payment DB
  -> CDC
  -> payments.raw (quyền truy cập rất hạn chế)
  -> stream processor tokenization/masking
  -> payments.sanitized
  -> Data Lake raw zone
  -> dbt/Spark join với orders, customers
  -> payment_daily_report
```

- Mask/tokenize số thẻ **trước khi load** là ETL vì đây là yêu cầu bảo mật.
- Join và aggregate **sau khi load** là ELT vì Warehouse/Lakehouse làm phần transform analytics.
- Không nhất thiết ép toàn bộ pipeline vào đúng một nhãn; điều quan trọng là đặt transform ở nơi phù hợp với latency, bảo mật, chi phí và khả năng replay.

#### Những vấn đề production phải thiết kế

**1. Schema và data contract**

Producer và consumer cần thống nhất schema. Nên dùng Avro, Protobuf hoặc JSON Schema cùng Schema Registry/data contract để quản lý compatibility. Nếu producer tự ý đổi tên hoặc xóa field, connector hay stream processor có thể lỗi hoặc tạo dữ liệu sai âm thầm.

**2. Duplicate và delivery semantics**

Kafka/connector thường có thể xử lý theo `at-least-once`, nên record có thể được gửi hoặc xử lý lại khi retry/restart. Sink nên idempotent, chẳng hạn upsert theo `order_id`, hoặc processor cần deduplicate theo business key/event id.

> Không nên hiểu `exactly-once` là tự động đúng một lần từ database nguồn đến mọi hệ thống đích. Kafka Streams có thể đảm bảo atomic giữa input offset, state store và output Kafka topic khi cấu hình phù hợp; Kafka Connect còn phụ thuộc vào khả năng của từng connector và sink bên ngoài.

**3. Ordering và partition key**

Kafka chỉ đảm bảo thứ tự trong một partition. Nếu cần giữ đúng thứ tự trạng thái của cùng một order, các event nên dùng `order_id` làm key để đi vào cùng partition.

```text
order_id=9001: CREATED -> PAID -> SHIPPED
```

Nếu key không ổn định, `SHIPPED` có thể được xử lý ở partition khác và xuất hiện trước `PAID` ở downstream.

**4. Event đến trễ và dữ liệu thay đổi**

Aggregate theo phút/ngày cần phân biệt **event time** với thời điểm processor nhận event. Với event đến trễ, cần thiết kế window/grace period và cách cập nhật lại kết quả đã xuất. CDC cũng có `INSERT`, `UPDATE`, `DELETE`; bỏ qua delete/tombstone có thể làm Warehouse giữ record đã bị xóa ở source.

**5. Record lỗi và poison message**

Không nên để một record sai schema làm đứng toàn bộ connector/pipeline. Cần có retry có giới hạn, Dead Letter Queue (DLQ), alert và quy trình sửa rồi replay record lỗi. DLQ chỉ cô lập lỗi, không thay thế việc theo dõi và xử lý nguyên nhân.

**6. Replay và backfill**

Kafka cho phép consumer đọc lại khi event còn trong retention. Tuy nhiên replay an toàn cần:

- Giữ raw topic đủ lâu hoặc archive sang object storage.
- Transform có tính deterministic hoặc version rõ ràng.
- Sink chịu được upsert/deduplicate.
- Tách luồng backfill khỏi realtime nếu replay tạo tải lớn.
- Biết output cũ cần ghi đè, hiệu chỉnh hay tạo dataset version mới.

#### Khi nào chọn ETL, ELT hoặc hybrid?

| Nhu cầu | Lựa chọn thường phù hợp |
| --- | --- |
| Fraud detection, alert, search index cần dữ liệu sạch trong vài giây | Streaming ETL |
| Mask PII trước khi dữ liệu rời vùng bảo mật | ETL hoặc bước ETL đầu của hybrid |
| Giữ raw data để audit, data science, transform lại nhiều lần | ELT |
| Báo cáo phức tạp, nhiều join lớn, logic thay đổi thường xuyên | ELT trong Warehouse/Lakehouse |
| Vừa cần realtime serving vừa cần analytics linh hoạt | Hybrid: ETL realtime + ELT analytics |

Tư duy chọn nhanh:

- Cần phản ứng ngay hoặc sink chỉ được nhận dữ liệu đã làm sạch: transform trước khi load.
- Cần giữ dữ liệu gốc và thường xuyên thay đổi logic analytics: load raw trước rồi transform.
- Chỉ cần đổi tên/mask/route từng record: cân nhắc Kafka Connect SMT.
- Cần join, aggregate, window hoặc stateful processing realtime: dùng stream processor.
- Cần join rất lớn và truy vấn ad-hoc trên lịch sử dài: thường để Warehouse/Lakehouse xử lý.

#### Đánh giá lại ghi chú cũ

- Đúng: ETL là `Extract -> Transform -> Load`, ELT là `Extract -> Load -> Transform`; Kafka có thể tham gia cả hai mô hình.
- Cần làm rõ: Kafka không tự thực hiện toàn bộ ETL/ELT. Kafka Connect đảm nhiệm data movement; transform phức tạp cần stream processor hoặc compute engine ở hệ thống đích.
- Cần bổ sung: ELT không nhất thiết là batch, còn streaming ETL không loại trừ việc giữ raw topic để replay.
- Cần cẩn trọng: delivery guarantee phải xét **end-to-end**. Không nên tuyên bố exactly-once chỉ vì một đoạn Kafka Streams hoặc connector đã bật exactly-once.

### Batch Processing

**Batch Processing** là cách xử lý dữ liệu theo từng lô. Hệ thống gom dữ liệu trong một khoảng thời gian rồi mới chạy job xử lý.

Ví dụ:

- Mỗi đêm tính doanh thu trong ngày.
- Mỗi 30 phút đồng bộ tồn kho từ database sang Elasticsearch.
- Mỗi giờ tổng hợp log để tạo report.

Ưu điểm:

- Dễ triển khai, dễ debug.
- Phù hợp với report, thống kê, backup, data warehouse.
- Có thể xử lý dữ liệu lớn nhưng không yêu cầu realtime.

Nhược điểm:

- Dữ liệu không phải mới nhất tại thời điểm user xem.
- Nếu job chạy lâu hoặc lỗi, dữ liệu bị trễ.
- Có thể phải scan cả dataset, kể cả phần không thay đổi.
- Không hợp với use case cần phản ứng ngay như fraud detection, notification realtime, tracking tài xế.

MapReduce là một mô hình xử lý batch nổi tiếng cho dữ liệu lớn. Tuy nhiên với hệ thống hiện đại, batch job cũng có thể dùng Spark, Flink batch, SQL job trong Data Warehouse, hoặc scheduler như Airflow.

### Stream Processing - Event Driven Architecture

**Stream Processing** là cách xử lý dữ liệu ngay khi dữ liệu vừa xuất hiện dưới dạng event. Thay vì đợi đủ một lô lớn như batch, hệ thống xử lý liên tục.

Ví dụ:

```text
User click product
  -> click event vào Kafka
  -> Recommendation Service cập nhật gợi ý
  -> Analytics Service cập nhật dashboard realtime
  -> Fraud Service kiểm tra hành vi bất thường
```

Đặc điểm quan trọng:

- **Continuous processing**: pipeline chạy liên tục, có event tới là xử lý.
- **Low latency**: độ trễ thấp, thường tính bằng mili giây hoặc dưới vài giây.
- **Event-time processing**: xử lý dựa trên thời điểm event thật sự xảy ra, không chỉ dựa trên thời điểm event đến hệ thống. Điểm này quan trọng khi event đến trễ hoặc đến không đúng thứ tự.
- **Change Data Capture (CDC)**: bắt thay đổi từ database rồi đẩy thành event, ví dụ user update email, order đổi status, product đổi stock.

Use case thực tế:

- Realtime dashboard: số order, doanh thu, active user cập nhật liên tục.
- Notification: gửi email/push notification khi order được tạo hoặc payment thành công.
- Fraud detection: phát hiện giao dịch bất thường ngay lúc nó xảy ra.
- Search indexing: update Elasticsearch khi product/user/order thay đổi.
- Log monitoring: gom log từ nhiều service để alert khi error tăng đột biến.
- Recommendation: cập nhật gợi ý dựa trên hành vi vừa xảy ra của user.

Tư duy chọn nhanh:

- Dữ liệu xử lý theo lịch, không cần realtime: dùng **Batch Processing**.
- Dữ liệu cần phản ứng ngay khi phát sinh: dùng **Stream Processing**.
- Cần tách rời nhiều service và cho nhiều consumer đọc cùng dữ liệu: Kafka rất hợp làm trung tâm event pipeline.

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

Vì sao ghi tuần tự lại nhanh?

Giả sử Kafka cần ghi 1.000 record. Nếu mỗi record được ghi vào một vị trí rải rác, storage phải liên tục tìm và chuyển tới vị trí cần ghi. Đây là **random I/O**. Kafka chủ yếu nối record mới vào cuối log, nên dữ liệu được ghi thành một luồng liên tiếp:

```text
Random I/O:     ghi chỗ A -> tìm chỗ B -> tìm chỗ C -> ...
Sequential I/O: [record 1][record 2][record 3]... -> ghi tiếp ở cuối
```

Cách ghi tuần tự có lợi vì:

- **Ít disk seek**: với HDD, đầu đọc/ghi ít phải di chuyển giữa nhiều vị trí. SSD không có đầu đọc cơ học nhưng ghi/đọc theo block liên tục vẫn hiệu quả hơn nhiều thao tác nhỏ rời rạc.
- **Ghi theo batch lớn**: nhiều record nhỏ được gom lại, nên Kafka thực hiện ít lần ghi và ít `system call` hơn. `System call` là mỗi lần application phải nhờ operating system thực hiện I/O; gọi quá nhiều lần sẽ tạo thêm overhead.
- **Consumer cũng thường đọc tuần tự**: consumer đọc từ offset hiện tại rồi tiến về phía trước, nên storage không phải tìm record ở các vị trí ngẫu nhiên.
- **OS có thể read-ahead**: khi thấy application đang đọc liên tiếp, operating system có thể đoán các block tiếp theo sẽ được dùng và nạp trước chúng vào page cache.

Vì vậy, điểm chính không phải là “disk nhanh hơn RAM”, mà là Kafka dùng một **access pattern thân thiện với storage và operating system**: ghi nối đuôi, đọc liên tục và xử lý theo batch.

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
