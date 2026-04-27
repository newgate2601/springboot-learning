# Đề bài backend Spring Boot: Hệ thống E-commerce Order Fulfillment

## 1. Mục tiêu học tập

Tài liệu này mô tả một app backend Spring Boot có nghiệp vụ đủ phức tạp để luyện các kỹ năng backend thực chiến:

- Thiết kế domain có nhiều trạng thái và nhiều luồng xử lý.
- Xử lý transaction phức tạp giữa đơn hàng, tồn kho, thanh toán và vận chuyển.
- Xử lý concurrency khi nhiều người cùng mua một sản phẩm có số lượng tồn kho giới hạn.
- Thiết kế query báo cáo phức tạp trên dữ liệu lớn.
- Tối ưu database bằng index, pagination, locking, batch processing và explain query plan.
- Thiết kế API idempotent, retry-safe, audit-friendly.
- Tách trách nhiệm rõ ràng giữa controller, service, repository, domain logic và background job.

App được đề xuất là **E-commerce Order Fulfillment System**, tập trung vào luồng từ lúc khách hàng xem sản phẩm, thêm vào giỏ hàng, checkout, giữ hàng, thanh toán, trừ tồn kho, đóng gói, giao hàng, hủy đơn, hoàn tiền và báo cáo.

## 2. Bối cảnh nghiệp vụ tổng quát

Hệ thống phục vụ một sàn bán hàng online hoặc một cửa hàng thương mại điện tử có nhiều kho hàng. Khách hàng có thể mua nhiều sản phẩm trong một đơn hàng. Mỗi sản phẩm có thể có nhiều SKU khác nhau, ví dụ cùng một mẫu áo nhưng khác size, màu sắc. Hàng tồn kho được quản lý theo từng kho, không chỉ theo sản phẩm tổng.

Một đơn hàng đi qua nhiều trạng thái:

```text
CREATED
PENDING_PAYMENT
PAID
PACKING
READY_TO_SHIP
SHIPPED
DELIVERED
COMPLETED
```

Ngoài luồng thành công, hệ thống phải xử lý nhiều luồng bất thường:

```text
PAYMENT_FAILED
CANCELLED
EXPIRED
REFUND_PENDING
REFUNDED
RETURN_REQUESTED
RETURN_APPROVED
RETURN_REJECTED
RETURNED
```

Điểm cốt lõi của hệ thống không phải là CRUD sản phẩm, mà là đảm bảo tính đúng đắn khi nhiều user cùng thao tác, nhiều callback thanh toán được gửi trùng, nhiều job chạy nền xử lý đơn hết hạn, nhiều trạng thái đơn hàng thay đổi theo thời gian.

## 3. Các module chính

### 3.1. Catalog

Quản lý thông tin sản phẩm hiển thị cho khách hàng:

- Product: tên sản phẩm, mô tả, brand, category, trạng thái bán.
- SKU: mã hàng cụ thể, thuộc tính như màu, size, barcode, giá, trọng lượng.
- Category: danh mục cha con.
- Product image: ảnh đại diện và ảnh chi tiết.
- Price: giá hiện tại, giá khuyến mãi, thời gian áp dụng.

Catalog phục vụ read-heavy nên có thể cache bằng Redis ở giai đoạn nâng cao.

### 3.2. Customer

Quản lý thông tin khách hàng:

- Hồ sơ khách hàng.
- Địa chỉ giao hàng.
- Lịch sử đơn hàng.
- Trạng thái khách hàng: active, blocked, deleted.

### 3.3. Cart

Giỏ hàng là nơi khách hàng chọn sản phẩm trước khi checkout.

Giỏ hàng không nên trừ tồn kho thật. Giỏ hàng chỉ phản ánh ý định mua tạm thời. Hệ thống chỉ giữ hàng khi khách bắt đầu checkout.

### 3.4. Inventory

Quản lý tồn kho theo SKU và warehouse:

- On hand: tổng số lượng vật lý trong kho.
- Reserved: số lượng đang bị giữ cho các đơn chưa thanh toán hoặc chưa hoàn tất.
- Available: số lượng có thể bán, thường là `on_hand - reserved`.
- Sold: số lượng đã bán.
- Damaged: số lượng hỏng.
- Returned: số lượng hàng trả về.

Không nên chỉ lưu một cột `quantity`. Cần có transaction ledger để audit mọi thay đổi tồn kho.

### 3.5. Order

Quản lý đơn hàng:

- Order header: thông tin chung, customer, tổng tiền, trạng thái.
- Order item: từng SKU, số lượng, đơn giá, giảm giá.
- Order status history: lịch sử đổi trạng thái.
- Order note: ghi chú nội bộ.

Order là aggregate trung tâm của hệ thống.

### 3.6. Payment

Thanh toán được mock bằng một payment gateway giả lập:

- Tạo payment intent.
- Nhận callback thành công hoặc thất bại.
- Xử lý callback trùng.
- Xử lý thanh toán timeout.
- Xử lý refund.

### 3.7. Shipment

Vận chuyển:

- Tạo shipment cho đơn đã thanh toán.
- Đóng gói.
- Bàn giao đơn vị vận chuyển.
- Theo dõi trạng thái giao hàng.
- Xử lý giao thất bại, giao lại, trả hàng.

### 3.8. Promotion

Khuyến mãi:

- Coupon theo mã.
- Giảm theo phần trăm.
- Giảm theo số tiền cố định.
- Free shipping.
- Điều kiện tối thiểu đơn hàng.
- Giới hạn lượt dùng theo user và toàn hệ thống.

Module này có nhiều race condition nếu nhiều user cùng dùng coupon có quota giới hạn.

### 3.9. Reporting

Báo cáo cho admin:

- Doanh thu theo ngày, tháng, quý.
- Sản phẩm bán chạy.
- Tỷ lệ hủy đơn.
- Tỷ lệ thanh toán thất bại.
- Tồn kho thấp.
- Hàng tồn lâu.
- Khách hàng mua nhiều nhất.
- Giá trị đơn hàng trung bình.

Reporting là nơi luyện query phức tạp, index, materialized view và dữ liệu lớn.

## 4. Workflow tổng quát

### 4.1. Luồng mua hàng thành công

```text
Khách xem sản phẩm
-> Thêm SKU vào giỏ hàng
-> Checkout
-> Hệ thống validate cart
-> Hệ thống giữ hàng trong inventory reservation
-> Tạo order ở trạng thái PENDING_PAYMENT
-> Tạo payment intent
-> Khách thanh toán
-> Payment gateway gửi callback success
-> Hệ thống xác nhận payment
-> Hệ thống chuyển order sang PAID
-> Hệ thống chuyển reservation thành SOLD
-> Hệ thống tạo shipment
-> Kho đóng gói
-> Đơn vị vận chuyển nhận hàng
-> Giao thành công
-> Order chuyển DELIVERED
-> Sau thời gian khiếu nại, order chuyển COMPLETED
```

### 4.2. Luồng thanh toán thất bại

```text
Checkout
-> Giữ hàng
-> Tạo order PENDING_PAYMENT
-> Payment failed
-> Order chuyển PAYMENT_FAILED
-> Release inventory reservation
-> Ghi nhận payment failure reason
```

### 4.3. Luồng khách không thanh toán

```text
Checkout
-> Giữ hàng trong 15 phút
-> Không có payment callback thành công
-> Background job quét reservation hết hạn
-> Order chuyển EXPIRED
-> Release reservation
-> Available stock tăng lại
```

### 4.4. Luồng hủy đơn trước khi thanh toán

```text
Order PENDING_PAYMENT
-> Customer cancel
-> Validate order có thể hủy
-> Order chuyển CANCELLED
-> Release reservation
-> Cancel payment intent nếu có
```

### 4.5. Luồng hủy đơn sau khi thanh toán nhưng chưa giao

```text
Order PAID hoặc PACKING
-> Customer/Admin request cancel
-> Validate shipment chưa SHIPPED
-> Order chuyển REFUND_PENDING
-> Tạo refund request
-> Payment gateway callback refund success
-> Order chuyển REFUNDED hoặc CANCELLED
-> Trả tồn kho nếu hàng chưa xuất kho
```

### 4.6. Luồng trả hàng sau khi giao

```text
Order DELIVERED
-> Customer tạo return request
-> Admin duyệt hoặc từ chối
-> Nếu duyệt, customer gửi hàng về kho
-> Kho kiểm tra hàng
-> Nếu hàng đạt, tạo refund
-> Cập nhật inventory RETURNED
-> Order chuyển RETURNED hoặc PARTIALLY_RETURNED
```

## 5. Các nghiệp vụ cần xử lý

### 5.1. Product và SKU

Nghiệp vụ chính:

- Tạo sản phẩm mới.
- Tạo nhiều SKU cho một sản phẩm.
- Bật hoặc tắt bán sản phẩm.
- Cập nhật giá.
- Cập nhật ảnh.
- Gắn category.
- Search sản phẩm theo keyword, category, price range, brand, trạng thái tồn kho.

Case cần cẩn thận:

- Không cho xóa product đã từng có order. Chỉ soft delete hoặc inactive.
- Không cho sửa mã SKU nếu SKU đã phát sinh giao dịch tồn kho.
- Giá order item phải snapshot tại thời điểm đặt hàng, không phụ thuộc giá hiện tại của SKU.
- Product inactive thì không được thêm vào cart hoặc checkout, nhưng order cũ vẫn phải hiển thị đúng.
- Search cần phân biệt sản phẩm hết hàng và sản phẩm ngừng bán.

### 5.2. Cart

Nghiệp vụ chính:

- Thêm SKU vào giỏ.
- Tăng giảm số lượng.
- Xóa item.
- Xem giỏ hàng.
- Validate giỏ hàng trước checkout.

Case cần cẩn thận:

- SKU bị inactive sau khi đã nằm trong cart.
- Giá thay đổi sau khi item đã nằm trong cart.
- Tồn kho không đủ tại thời điểm checkout.
- Cart item quantity vượt quá giới hạn mỗi khách.
- Một user checkout nhiều tab cùng lúc.

Cách xử lý đề xuất:

- Cart chỉ là dữ liệu tạm, không đảm bảo giữ hàng.
- Khi view cart, luôn tính lại giá và trạng thái SKU mới nhất.
- Khi checkout, validate lại toàn bộ cart trong transaction.
- Không tin dữ liệu giá hoặc tổng tiền từ frontend gửi lên.

### 5.3. Inventory stock

Nghiệp vụ chính:

- Nhập kho.
- Điều chỉnh tồn kho.
- Giữ hàng khi checkout.
- Release hàng khi order hết hạn hoặc bị hủy.
- Chuyển reservation thành sold khi payment thành công.
- Nhận hàng trả về.
- Ghi ledger cho mọi biến động.

Các bảng gợi ý:

```text
inventory_stock
- id
- sku_id
- warehouse_id
- on_hand_quantity
- reserved_quantity
- sold_quantity
- version
- created_at
- updated_at

inventory_transaction
- id
- sku_id
- warehouse_id
- order_id
- order_item_id
- type
- quantity
- before_on_hand
- after_on_hand
- before_reserved
- after_reserved
- reason
- created_at
```

Case cần cẩn thận:

- Hai user cùng mua SKU cuối cùng.
- Job release reservation chạy cùng lúc với payment success callback.
- Admin điều chỉnh tồn kho khi đang có checkout.
- Payment callback success đến sau khi order đã expired.
- Một order có nhiều SKU, một SKU đủ hàng nhưng SKU khác thiếu hàng.

Cách xử lý đề xuất:

- Dùng transaction database bao quanh bước reserve stock.
- Dùng pessimistic lock hoặc optimistic lock cho dòng `inventory_stock`.
- Available stock phải được tính từ `on_hand_quantity - reserved_quantity`.
- Reserve chỉ thành công nếu available đủ.
- Mọi thay đổi phải ghi `inventory_transaction`.
- Payment success chỉ được convert reservation sang sold nếu order còn hợp lệ.
- Job release expired reservation phải kiểm tra trạng thái order trước khi release.

### 5.4. Inventory reservation

Reservation là nghiệp vụ quan trọng nhất để luyện concurrency.

Khi checkout, hệ thống tạo reservation:

```text
reservation_id
order_id
sku_id
warehouse_id
quantity
status: ACTIVE, RELEASED, CONSUMED, EXPIRED
expires_at
```

State hợp lệ:

```text
ACTIVE -> CONSUMED
ACTIVE -> RELEASED
ACTIVE -> EXPIRED
```

Không được:

```text
CONSUMED -> RELEASED
RELEASED -> CONSUMED
EXPIRED -> CONSUMED
```

Case cần handle cực kỳ cẩn thận:

- Payment success đến đúng lúc reservation expiry job đang release.
- User bấm thanh toán nhiều lần.
- Callback payment success bị gửi lại nhiều lần.
- Một order có nhiều reservation ở nhiều warehouse.
- Một vài reservation tạo thành công nhưng reservation tiếp theo fail.

Cách xử lý:

- Tạo order và reserve toàn bộ item trong cùng transaction.
- Nếu bất kỳ SKU nào không đủ hàng, rollback toàn bộ checkout.
- Khi consume reservation, lock order và reservation.
- Payment success phải kiểm tra idempotency key.
- Expiry job phải lock từng reservation hoặc order trước khi xử lý.
- Mọi transition reservation phải idempotent.

### 5.5. Order

Nghiệp vụ chính:

- Tạo order từ cart.
- Tính tổng tiền.
- Áp coupon.
- Tính phí vận chuyển.
- Cập nhật trạng thái.
- Hủy đơn.
- Xem lịch sử đơn hàng.
- Admin tra cứu đơn hàng.

Các trạng thái chính:

```text
PENDING_PAYMENT
PAID
PACKING
READY_TO_SHIP
SHIPPED
DELIVERED
COMPLETED
PAYMENT_FAILED
EXPIRED
CANCELLED
REFUND_PENDING
REFUNDED
```

Transition hợp lệ:

```text
PENDING_PAYMENT -> PAID
PENDING_PAYMENT -> PAYMENT_FAILED
PENDING_PAYMENT -> EXPIRED
PENDING_PAYMENT -> CANCELLED
PAID -> PACKING
PAID -> REFUND_PENDING
PACKING -> READY_TO_SHIP
PACKING -> REFUND_PENDING
READY_TO_SHIP -> SHIPPED
SHIPPED -> DELIVERED
DELIVERED -> COMPLETED
REFUND_PENDING -> REFUNDED
```

Case cần cẩn thận:

- Không cho admin nhảy trạng thái tùy tiện.
- Không cho cancel order đã SHIPPED.
- Không cho payment success cập nhật order đã CANCELLED nếu cancellation thắng trước.
- Không cho order expired rồi lại paid nếu payment đến quá muộn, trừ khi có chính sách manual review.
- Không cho tạo shipment trước khi payment confirmed.

Cách xử lý:

- Tạo service riêng để validate state transition.
- Mỗi lần đổi trạng thái phải ghi `order_status_history`.
- Dùng optimistic lock ở order để tránh lost update.
- Các action quan trọng phải idempotent.

### 5.6. Payment

Nghiệp vụ chính:

- Tạo payment intent cho order.
- Nhận callback success.
- Nhận callback failed.
- Retry callback.
- Refund.
- Lưu payment transaction.

Bảng gợi ý:

```text
payment_transaction
- id
- order_id
- provider
- provider_transaction_id
- amount
- currency
- status
- idempotency_key
- raw_payload
- created_at
- updated_at

payment_event_log
- id
- provider
- event_id
- event_type
- order_id
- processed
- processed_at
- raw_payload
- created_at
```

Case cần handle cực kỳ cẩn thận:

- Payment gateway gửi cùng một callback 2 lần.
- Payment success đến sau payment failed.
- Payment success đến sau order expired.
- Amount trong callback không khớp order amount.
- Callback không xác thực chữ ký.
- Refund callback đến trước khi refund request được lưu.

Cách xử lý:

- Dùng `event_id` hoặc `provider_transaction_id` làm idempotency key.
- Tạo unique constraint cho idempotency key.
- Verify signature trước khi xử lý.
- So sánh amount, currency, order id.
- Lock order khi xử lý payment callback.
- Nếu callback trùng, trả success nhưng không xử lý lại.
- Nếu callback hợp lệ nhưng order không còn ở trạng thái cho phép, ghi log và đưa vào manual review.

### 5.7. Promotion và coupon

Nghiệp vụ chính:

- Tạo coupon.
- Coupon có thời gian hiệu lực.
- Coupon có số lượt dùng tối đa.
- Coupon có số lượt dùng tối đa mỗi user.
- Coupon có điều kiện min order amount.
- Coupon chỉ áp dụng cho một số category hoặc SKU.

Case cần cẩn thận:

- Coupon còn đúng 1 lượt nhưng nhiều user cùng checkout.
- User dùng lại coupon sau khi hủy order.
- Coupon áp dụng được lúc cart nhưng hết hạn lúc checkout.
- Coupon bị admin disable giữa quá trình checkout.

Cách xử lý:

- Chỉ confirm usage coupon trong transaction checkout.
- Lock coupon row khi tăng usage.
- Có bảng `coupon_usage` theo order và user.
- Nếu order expired hoặc cancelled trước payment, có thể release coupon usage tùy chính sách.
- Validate lại coupon ở thời điểm checkout, không tin frontend.

### 5.8. Shipment

Nghiệp vụ chính:

- Tạo shipment sau khi order paid.
- Gán warehouse xử lý.
- Đóng gói.
- Tạo tracking number.
- Cập nhật trạng thái vận chuyển.
- Xử lý giao thất bại.

Trạng thái shipment:

```text
CREATED
PACKING
READY_TO_PICKUP
PICKED_UP
IN_TRANSIT
DELIVERED
FAILED_DELIVERY
RETURNING
RETURNED_TO_WAREHOUSE
```

Case cần cẩn thận:

- Không tạo shipment cho order chưa paid.
- Không cancel order khi shipment đã picked up.
- Shipment callback từ carrier gửi trùng.
- Carrier báo delivered cho order đã refund.
- Một order có thể split thành nhiều shipment nếu lấy hàng từ nhiều warehouse.

Cách xử lý:

- Shipment status cũng cần state transition validator.
- Ghi shipment event history.
- Carrier webhook phải idempotent.
- Khi delivered, cập nhật order nếu toàn bộ shipment đều delivered.

### 5.9. Return và refund

Nghiệp vụ chính:

- Customer tạo yêu cầu trả hàng.
- Admin duyệt hoặc từ chối.
- Warehouse nhận hàng trả.
- Kiểm tra tình trạng hàng.
- Hoàn tiền toàn phần hoặc một phần.
- Nhập lại tồn kho nếu hàng còn bán được.

Case cần cẩn thận:

- Không cho return quá thời hạn.
- Không cho return sản phẩm không thuộc order.
- Không cho return số lượng lớn hơn đã mua.
- Không refund hai lần cho cùng item.
- Một order có thể trả một phần.
- Hàng trả về có thể hỏng, không nhập lại available stock.

Cách xử lý:

- Có bảng `return_request` và `return_item`.
- Có bảng `refund_transaction`.
- Refund cũng phải idempotent như payment.
- Inventory transaction type có `RETURNED_SELLABLE` và `RETURNED_DAMAGED`.

## 6. Use case cần handle cực kỳ cẩn thận

### 6.1. Overselling SKU cuối cùng

Tình huống:

```text
SKU A còn available = 1
User 1 checkout SKU A quantity 1
User 2 checkout SKU A quantity 1
Hai request đến gần như cùng lúc
```

Sai lầm thường gặp:

- Query available stock trước, thấy còn hàng, sau đó update sau.
- Không lock dòng tồn kho.
- Không kiểm tra affected row khi update.

Cách xử lý 1: pessimistic locking

```sql
SELECT *
FROM inventory_stock
WHERE sku_id = :skuId AND warehouse_id = :warehouseId
FOR UPDATE;
```

Sau khi lock:

```text
available = on_hand - reserved
if available >= requested_quantity:
    reserved += requested_quantity
else:
    throw out_of_stock
```

Cách xử lý 2: atomic conditional update

```sql
UPDATE inventory_stock
SET reserved_quantity = reserved_quantity + :qty
WHERE sku_id = :skuId
  AND warehouse_id = :warehouseId
  AND on_hand_quantity - reserved_quantity >= :qty;
```

Sau đó kiểm tra số row affected. Nếu bằng 0 thì không đủ hàng.

### 6.2. Payment callback trùng

Tình huống:

```text
Gateway gửi event payment_success event_id = abc
Backend xử lý thành công
Gateway retry lại event_id = abc
```

Cách xử lý:

- Tạo unique constraint trên `payment_event_log.event_id`.
- Khi nhận event, insert event log trước.
- Nếu insert fail do duplicate key, trả HTTP 200 và không xử lý lại.
- Không được throw lỗi làm gateway retry vô hạn.

### 6.3. Payment success và order expiry chạy đồng thời

Tình huống:

```text
Order PENDING_PAYMENT, reservation expires_at = 10:00
10:00:00 payment success callback đến
10:00:01 expiry job cũng xử lý order này
```

Cách xử lý:

- Cả payment callback và expiry job đều phải lock order.
- Payment callback chỉ xử lý nếu order đang PENDING_PAYMENT.
- Expiry job chỉ expire nếu order vẫn PENDING_PAYMENT và chưa có payment success hợp lệ.
- Sau khi một luồng đổi trạng thái, luồng còn lại phải thấy trạng thái mới và dừng idempotently.

### 6.4. Checkout một order nhiều SKU

Tình huống:

```text
Order gồm SKU A, B, C
A đủ hàng
B đủ hàng
C thiếu hàng
```

Cách xử lý:

- Reserve toàn bộ item trong cùng transaction.
- Nếu bất kỳ SKU nào fail, rollback toàn bộ.
- Không tạo order nửa vời.
- Trả response chi tiết SKU nào thiếu hàng.

### 6.5. Coupon quota race condition

Tình huống:

```text
Coupon FLASHSALE còn 1 lượt
100 user checkout cùng lúc
```

Cách xử lý:

- Lock coupon row hoặc dùng conditional update.
- Tạo `coupon_usage` unique theo `coupon_id + order_id`.
- Với giới hạn mỗi user, unique theo `coupon_id + user_id` nếu chỉ cho dùng một lần.
- Chỉ user thắng transaction mới được áp coupon.

### 6.6. Admin chỉnh tồn kho trong lúc đang checkout

Tình huống:

```text
Admin giảm on_hand từ 10 xuống 2
Trong lúc đó 5 user đang checkout
```

Cách xử lý:

- Điều chỉnh tồn kho cũng phải lock inventory row.
- Không cho set `on_hand_quantity < reserved_quantity` nếu không có workflow đặc biệt.
- Ghi ledger type `ADJUSTMENT`.
- Nếu cần giảm do mất hàng, có thể tạo trạng thái thiếu hàng để admin xử lý order đang reserved.

### 6.7. Lost update khi đổi trạng thái order

Tình huống:

```text
Admin chuyển PAID -> PACKING
Payment callback retry cũng update PAID
Hai request ghi đè dữ liệu của nhau
```

Cách xử lý:

- Dùng `@Version` optimistic locking cho order.
- Hoặc lock order row khi update trạng thái quan trọng.
- Mỗi transition phải kiểm tra trạng thái hiện tại.
- Không update order bằng object cũ không kiểm tra version.

### 6.8. Report trên dữ liệu lớn bị chậm

Tình huống:

```text
orders 10 triệu rows
order_items 50 triệu rows
Admin mở dashboard doanh thu theo ngày
Query group by trực tiếp trên bảng lớn gây timeout
```

Cách xử lý:

- Tạo index theo `created_at`, `status`, `sku_id`, `customer_id`.
- Dùng bảng summary theo ngày.
- Dùng materialized view cho report nặng.
- Dùng batch job aggregate dữ liệu.
- Tách read model cho reporting nếu cần.
- Luôn dùng pagination cho danh sách lớn.

## 7. Nghiệp vụ chi tiết từng màn hình

Phần này mô tả theo góc nhìn sản phẩm. Khi implement backend, mỗi màn hình tương ứng với một nhóm API.

## 7.1. Màn Product Listing của khách hàng

Mục tiêu:

- Hiển thị danh sách sản phẩm có thể mua.
- Cho phép search, filter, sort, pagination.

Thông tin hiển thị:

- Tên sản phẩm.
- Ảnh đại diện.
- Giá thấp nhất hoặc giá theo SKU mặc định.
- Giá khuyến mãi nếu có.
- Rating giả lập nếu muốn mở rộng.
- Trạng thái còn hàng, sắp hết hàng, hết hàng.
- Brand.
- Category.

Filter cần có:

- Keyword.
- Category.
- Brand.
- Price range.
- Còn hàng hoặc tất cả.
- Sort theo giá tăng, giá giảm, mới nhất, bán chạy.

Case cần handle:

- Keyword rỗng.
- Category không tồn tại.
- Min price lớn hơn max price.
- Page size quá lớn.
- Product inactive không được hiển thị.
- Product còn active nhưng tất cả SKU hết hàng thì hiển thị hết hàng hoặc ẩn tùy policy.

API gợi ý:

```http
GET /api/products?keyword=&categoryId=&brand=&minPrice=&maxPrice=&inStock=true&page=0&size=20&sort=price_asc
```

Query cần luyện:

- Join product, sku, inventory_stock.
- Aggregate available stock theo product.
- Filter theo category tree.
- Sort theo computed field.

## 7.2. Màn Product Detail

Mục tiêu:

- Hiển thị chi tiết sản phẩm và các SKU có thể chọn.

Thông tin hiển thị:

- Tên, mô tả, ảnh.
- Danh sách variant như màu, size.
- Giá từng SKU.
- Available stock từng SKU hoặc trạng thái còn hàng.
- Chính sách đổi trả.
- Sản phẩm liên quan.

Case cần handle:

- Product không tồn tại.
- Product inactive.
- SKU inactive.
- SKU hết hàng.
- Giá SKU thay đổi sau khi user mở màn hình.
- User chọn variant không hợp lệ.

API gợi ý:

```http
GET /api/products/{productId}
GET /api/products/{productId}/skus
```

Lưu ý:

- Không trả toàn bộ số lượng tồn kho nếu không muốn lộ dữ liệu.
- Có thể trả dạng `IN_STOCK`, `LOW_STOCK`, `OUT_OF_STOCK`.

## 7.3. Màn Cart

Mục tiêu:

- Cho user quản lý sản phẩm muốn mua.

Action:

- Add item.
- Update quantity.
- Remove item.
- Clear cart.
- View cart.

Thông tin hiển thị:

- SKU name.
- Product name.
- Variant.
- Unit price hiện tại.
- Quantity.
- Subtotal.
- Warning nếu SKU hết hàng, inactive hoặc giá đã đổi.

Case cần handle:

- Add SKU không tồn tại.
- Add SKU inactive.
- Add quantity <= 0.
- Quantity vượt giới hạn.
- Update quantity về 0 thì xóa item.
- SKU hết hàng vẫn có thể nằm trong cart cũ nhưng không checkout được.
- User chưa đăng nhập thì dùng session cart hoặc yêu cầu login, tùy scope.

API gợi ý:

```http
GET /api/cart
POST /api/cart/items
PATCH /api/cart/items/{itemId}
DELETE /api/cart/items/{itemId}
DELETE /api/cart
```

Validation quan trọng:

- Không tính tổng tiền từ frontend.
- Khi view cart, backend tính lại giá và trạng thái.
- Khi checkout, validate lại một lần nữa trong transaction.

## 7.4. Màn Checkout

Mục tiêu:

- User xác nhận địa chỉ, coupon, phương thức thanh toán và tạo order.

Thông tin cần nhập:

- Shipping address.
- Billing information nếu cần.
- Coupon code.
- Payment method.
- Ghi chú đơn hàng.

Backend xử lý:

```text
1. Validate customer active
2. Load cart
3. Validate cart không rỗng
4. Validate từng SKU còn active
5. Validate giá và promotion
6. Validate coupon
7. Tính subtotal
8. Tính discount
9. Tính shipping fee
10. Tính final amount
11. Reserve inventory
12. Confirm coupon usage
13. Tạo order PENDING_PAYMENT
14. Tạo payment intent
15. Trả payment URL hoặc payment token
```

Case cần handle cực kỳ cẩn thận:

- Cart rỗng.
- SKU hết hàng.
- Một phần SKU thiếu hàng.
- Coupon hết hạn.
- Coupon hết lượt.
- User bấm checkout 2 lần liên tục.
- Network timeout sau khi order đã tạo nhưng frontend chưa nhận response.
- Payment intent tạo thất bại sau khi đã reserve hàng.

Cách xử lý:

- Dùng idempotency key từ client cho checkout request.
- Nếu cùng idempotency key gửi lại, trả về order/payment cũ.
- Checkout phải chạy trong transaction cho phần order, reservation, coupon usage.
- Nếu payment intent gọi external sau transaction, cần có trạng thái rõ ràng để retry.
- Nếu payment intent fail, có thể cancel order và release reservation.

API gợi ý:

```http
POST /api/checkout
Idempotency-Key: checkout-user-123-uuid
```

Response gợi ý:

```json
{
  "orderId": 1001,
  "orderCode": "ORD-20260423-000001",
  "status": "PENDING_PAYMENT",
  "amount": 1250000,
  "paymentUrl": "https://mock-payment.local/pay/abc"
}
```

## 7.5. Màn Payment Result

Mục tiêu:

- Hiển thị kết quả thanh toán cho user sau khi quay lại từ payment gateway.

Case cần handle:

- Frontend redirect về trước khi webhook backend xử lý xong.
- Payment đang pending.
- Payment success.
- Payment failed.
- Order expired.
- User refresh trang nhiều lần.

API gợi ý:

```http
GET /api/orders/{orderId}/payment-status
```

Cách xử lý:

- Không tin query param redirect là bằng chứng thanh toán.
- Trạng thái thanh toán chính xác phải dựa vào callback verified từ gateway.
- Nếu payment pending, frontend polling trong giới hạn.

## 7.6. Payment Webhook

Mục tiêu:

- Nhận callback từ payment gateway.

Endpoint:

```http
POST /api/payment/webhooks/mock-provider
```

Backend xử lý:

```text
1. Verify signature
2. Parse event
3. Insert payment_event_log với unique event_id
4. Nếu duplicate thì trả 200
5. Load payment transaction
6. Validate amount, currency, order
7. Lock order
8. Nếu event success và order PENDING_PAYMENT:
   - mark payment success
   - order -> PAID
   - reservation -> CONSUMED
   - inventory reserved giảm, sold tăng
   - tạo shipment
9. Nếu event failed và order PENDING_PAYMENT:
   - mark payment failed
   - order -> PAYMENT_FAILED
   - release reservation
10. Commit transaction
```

Case cần handle:

- Signature sai.
- Event duplicate.
- Event không map được order.
- Amount mismatch.
- Order đã expired.
- Order đã cancelled.
- Payment success sau failed.
- Payment failed sau success.

Policy đề xuất:

- Success sau failed nhưng order còn PENDING_PAYMENT thì có thể cho success thắng.
- Success sau expired thì đưa vào manual review hoặc refund tự động.
- Failed sau success thì bỏ qua và log.
- Duplicate luôn trả 200.

## 7.7. Màn Order List của khách hàng

Mục tiêu:

- User xem lịch sử đơn hàng.

Filter:

- Trạng thái.
- Thời gian tạo.
- Keyword order code.

Thông tin hiển thị:

- Order code.
- Ngày tạo.
- Tổng tiền.
- Trạng thái.
- Một vài item đại diện.
- Payment status.
- Shipment status.

Case cần handle:

- User chỉ xem được order của chính mình.
- Pagination bắt buộc.
- Order cũ vẫn hiển thị đúng giá snapshot.
- Filter status không hợp lệ.

API gợi ý:

```http
GET /api/me/orders?status=&from=&to=&page=0&size=20
```

## 7.8. Màn Order Detail của khách hàng

Mục tiêu:

- User xem chi tiết đơn hàng và thực hiện action hợp lệ.

Thông tin hiển thị:

- Order code.
- Trạng thái hiện tại.
- Timeline trạng thái.
- Danh sách item.
- Giá từng item tại thời điểm mua.
- Discount.
- Shipping fee.
- Final amount.
- Payment information.
- Shipment tracking.
- Địa chỉ giao hàng.

Action theo trạng thái:

- PENDING_PAYMENT: pay again, cancel.
- PAID: cancel nếu chưa packing hoặc policy cho phép.
- PACKING: request cancel nếu kho chưa đóng gói xong.
- SHIPPED: track shipment.
- DELIVERED: request return.
- COMPLETED: xem lại hoặc mua lại.

Case cần handle:

- User cancel khi payment callback đang xử lý.
- User request return quá hạn.
- User pay again cho order expired.
- Tracking chưa có dữ liệu.

API gợi ý:

```http
GET /api/me/orders/{orderId}
POST /api/me/orders/{orderId}/cancel
POST /api/me/orders/{orderId}/pay-again
POST /api/me/orders/{orderId}/return-requests
```

## 7.9. Màn Admin Order Management

Mục tiêu:

- Admin tra cứu, kiểm tra, xử lý đơn hàng.

Filter:

- Order code.
- Customer email hoặc phone.
- Status.
- Payment status.
- Shipment status.
- Date range.
- Min amount, max amount.
- Warehouse.

Action:

- View detail.
- Update status theo action hợp lệ.
- Cancel order.
- Approve refund.
- Add internal note.
- Export CSV.

Case cần handle:

- Không cho admin đổi trạng thái trái state machine.
- Không cho cancel order đã shipped.
- Không cho refund order chưa paid.
- Admin thao tác cùng lúc với payment/shipment webhook.
- Export dữ liệu lớn phải chạy async, không block request.

API gợi ý:

```http
GET /api/admin/orders
GET /api/admin/orders/{orderId}
POST /api/admin/orders/{orderId}/cancel
POST /api/admin/orders/{orderId}/notes
POST /api/admin/orders/export
```

Query cần luyện:

- Search nhiều điều kiện.
- Join customer, payment, shipment.
- Sort theo created_at, amount.
- Pagination ổn định bằng created_at + id.

## 7.10. Màn Warehouse Picking/Packing

Mục tiêu:

- Nhân viên kho xử lý đơn đã thanh toán.

Luồng:

```text
Order PAID
-> Warehouse nhận danh sách cần pick
-> Nhân viên pick item
-> Xác nhận đủ hàng
-> Đóng gói
-> Tạo shipment label
-> Chuyển READY_TO_SHIP
```

Action:

- List orders cần đóng gói.
- View pick list.
- Confirm picked.
- Report thiếu hàng.
- Confirm packed.
- Print label.

Case cần handle:

- Order đã PAID nhưng hàng vật lý bị mất.
- Nhân viên mở cùng một order ở hai máy.
- Order bị refund pending trong lúc kho đang packing.
- Split shipment nếu nhiều warehouse.

Cách xử lý:

- Lock hoặc assign order cho một warehouse staff khi bắt đầu picking.
- Có trạng thái `PACKING`.
- Nếu thiếu hàng, chuyển order sang exception queue.
- Không cho ship order đang refund pending.

API gợi ý:

```http
GET /api/admin/warehouse/pick-tasks
POST /api/admin/warehouse/orders/{orderId}/start-packing
POST /api/admin/warehouse/orders/{orderId}/confirm-packed
POST /api/admin/warehouse/orders/{orderId}/report-shortage
```

## 7.11. Màn Inventory Management

Mục tiêu:

- Admin quản lý tồn kho.

Chức năng:

- Xem tồn kho theo SKU và warehouse.
- Nhập kho.
- Điều chỉnh tồn kho.
- Xem lịch sử biến động tồn kho.
- Xem reservation đang active.
- Xem hàng sắp hết.

Thông tin hiển thị:

- SKU.
- Warehouse.
- On hand.
- Reserved.
- Available.
- Sold.
- Updated at.

Action:

- Stock inbound.
- Stock adjustment.
- Transfer giữa warehouse nếu muốn mở rộng.
- Export inventory.

Case cần handle:

- Không cho adjustment làm on hand nhỏ hơn reserved.
- Không cho nhập quantity âm.
- Mọi adjustment cần reason.
- Mọi thay đổi phải ghi ledger.
- Admin xem available phải gần real-time.

API gợi ý:

```http
GET /api/admin/inventory
POST /api/admin/inventory/inbound
POST /api/admin/inventory/adjust
GET /api/admin/inventory/transactions
GET /api/admin/inventory/reservations
```

Query cần luyện:

- Filter theo SKU, warehouse, low stock.
- Transaction history theo date range.
- Aggregate tồn kho theo product.

## 7.12. Màn Coupon Management

Mục tiêu:

- Admin tạo và quản lý coupon.

Thông tin coupon:

- Code.
- Discount type: percentage, fixed amount, free shipping.
- Discount value.
- Max discount amount.
- Min order amount.
- Start time, end time.
- Total usage limit.
- Per user usage limit.
- Applicable category/SKU.
- Status active/inactive.

Case cần handle:

- Coupon code duplicate.
- End time nhỏ hơn start time.
- Discount value không hợp lệ.
- Percentage lớn hơn 100.
- Coupon đang được dùng thì không xóa cứng.
- Coupon hết lượt nhưng vẫn active về mặt cấu hình.

API gợi ý:

```http
GET /api/admin/coupons
POST /api/admin/coupons
PATCH /api/admin/coupons/{couponId}
POST /api/admin/coupons/{couponId}/disable
GET /api/admin/coupons/{couponId}/usages
```

## 7.13. Màn Return Request của khách hàng

Mục tiêu:

- User yêu cầu trả hàng hoặc hoàn tiền.

Input:

- Order item.
- Quantity muốn trả.
- Reason.
- Ảnh minh chứng nếu muốn mở rộng.

Case cần handle:

- Order chưa delivered.
- Order đã quá hạn return.
- Item không thuộc order.
- Quantity vượt quá quantity đã mua trừ quantity đã return trước đó.
- Sản phẩm không cho return theo policy.

API gợi ý:

```http
POST /api/me/orders/{orderId}/return-requests
GET /api/me/return-requests
```

## 7.14. Màn Admin Return Management

Mục tiêu:

- Admin duyệt, từ chối, xử lý hàng trả.

Action:

- Approve return.
- Reject return.
- Confirm received.
- Mark sellable.
- Mark damaged.
- Trigger refund.

Case cần handle:

- Admin duyệt hai lần.
- Kho nhận ít hơn số lượng customer khai báo.
- Hàng trả bị hỏng.
- Refund failed.
- Refund callback duplicate.

API gợi ý:

```http
GET /api/admin/return-requests
POST /api/admin/return-requests/{id}/approve
POST /api/admin/return-requests/{id}/reject
POST /api/admin/return-requests/{id}/confirm-received
POST /api/admin/return-requests/{id}/refund
```

## 7.15. Màn Reporting Dashboard

Mục tiêu:

- Admin xem sức khỏe kinh doanh và vận hành.

Metric nên có:

- Gross revenue.
- Net revenue.
- Number of orders.
- Paid orders.
- Cancelled orders.
- Refund amount.
- Average order value.
- Top selling SKU.
- Low stock SKU.
- Payment failure rate.
- Fulfillment time average.

Filter:

- Date range.
- Warehouse.
- Category.
- Brand.

Case cần handle:

- Date range quá lớn.
- Timezone.
- Dữ liệu hôm nay chưa aggregate xong.
- Refund làm giảm net revenue.
- Cancelled order không được tính doanh thu.
- Partial refund.

API gợi ý:

```http
GET /api/admin/reports/revenue?from=&to=&groupBy=day
GET /api/admin/reports/top-products?from=&to=&limit=10
GET /api/admin/reports/low-stock
GET /api/admin/reports/payment-failure-rate
```

Query cần luyện:

```sql
SELECT
  date_trunc('day', o.paid_at) AS day,
  COUNT(*) AS paid_orders,
  SUM(o.final_amount) AS gross_revenue,
  SUM(o.refunded_amount) AS refunded_amount,
  SUM(o.final_amount - o.refunded_amount) AS net_revenue
FROM orders o
WHERE o.status IN ('PAID', 'PACKING', 'SHIPPED', 'DELIVERED', 'COMPLETED')
  AND o.paid_at >= :from
  AND o.paid_at < :to
GROUP BY date_trunc('day', o.paid_at)
ORDER BY day;
```

## 8. Background jobs cần có

### 8.1. Job expire pending orders

Chạy mỗi phút hoặc mỗi vài phút.

Nhiệm vụ:

- Tìm order `PENDING_PAYMENT` quá hạn thanh toán.
- Lock order.
- Chuyển order sang `EXPIRED`.
- Release reservation.
- Release coupon usage nếu policy cho phép.

Case cần handle:

- Payment success đang xử lý cùng lúc.
- Job chạy nhiều instance.
- Một batch quá lớn.

Cách xử lý:

- Process theo batch nhỏ.
- Dùng `FOR UPDATE SKIP LOCKED` nếu dùng PostgreSQL.
- Mỗi order xử lý trong transaction riêng.

### 8.2. Job release expired reservation

Nếu reservation tách riêng khỏi order expiry, job này xử lý reservation hết hạn.

Lưu ý:

- Không release reservation của order đã paid.
- Không release reservation đã consumed.
- Không release hai lần.

### 8.3. Job aggregate report

Chạy định kỳ để tổng hợp dữ liệu:

- Revenue daily summary.
- SKU sales summary.
- Inventory daily snapshot.

Mục tiêu:

- Dashboard không query trực tiếp trên bảng order lớn.
- Có thể backfill khi logic report thay đổi.

### 8.4. Job retry failed integration

Retry các tác vụ external bị lỗi:

- Tạo payment intent fail.
- Gửi email fail.
- Tạo shipment label fail.
- Publish event fail.

Cần có:

- Retry count.
- Next retry at.
- Dead-letter status.
- Error message.

## 9. API và rule kỹ thuật nên áp dụng

### 9.1. Idempotency

Các API nên hỗ trợ idempotency:

- Checkout.
- Payment webhook.
- Refund webhook.
- Cancel order.
- Create return request.

Nguyên tắc:

- Client gửi `Idempotency-Key`.
- Backend lưu key, request hash, response snapshot.
- Request retry cùng key trả cùng response.
- Cùng key nhưng payload khác thì trả lỗi.

### 9.2. Audit log

Các action cần audit:

- Admin đổi trạng thái order.
- Admin điều chỉnh tồn kho.
- Admin duyệt refund.
- Payment webhook.
- Shipment webhook.

Audit log nên có:

```text
actor_type
actor_id
action
entity_type
entity_id
before_value
after_value
reason
created_at
```

### 9.3. Transaction boundary

Các use case bắt buộc có transaction rõ:

- Checkout.
- Reserve stock.
- Payment success handling.
- Payment failed handling.
- Cancel order.
- Refund success handling.
- Inventory adjustment.
- Coupon usage confirm.

Không nên:

- Gọi external API lâu bên trong transaction nếu không cần thiết.
- Giữ lock database quá lâu.
- Gửi email trong transaction chính.

### 9.4. Validation

Không tin dữ liệu frontend:

- Giá.
- Tổng tiền.
- Discount.
- Shipping fee.
- Order status.
- Customer id.
- Available stock.

Backend phải tự load và tính lại.

## 10. Gợi ý database lớn để luyện performance

Khi đã xong CRUD và workflow chính, hãy seed dữ liệu lớn:

- 100.000 products.
- 500.000 SKUs.
- 10 warehouses.
- 5.000.000 orders.
- 20.000.000 order items.
- 20.000.000 inventory transactions.
- 5.000.000 payment events.

Các bài luyện:

- Query order list dưới 300ms với filter phổ biến.
- Query dashboard revenue dưới 1s bằng summary table.
- Checkout concurrency 500 request cùng mua 1 SKU không oversell.
- Payment webhook duplicate 10 lần vẫn chỉ xử lý 1 lần.
- Expiry job xử lý 100.000 pending orders theo batch ổn định.

## 11. Thứ tự build đề xuất

### Phase 1: Core CRUD

- Product.
- SKU.
- Warehouse.
- Inventory stock.
- Customer.

### Phase 2: Cart và checkout đơn giản

- Cart.
- Checkout.
- Create order.
- Reserve stock.
- Mock payment intent.

### Phase 3: Payment và concurrency

- Payment webhook.
- Idempotency.
- Pessimistic hoặc optimistic locking.
- Prevent overselling.
- Expire pending orders.

### Phase 4: Fulfillment

- Packing.
- Shipment.
- Delivery status.
- Cancel rules.

### Phase 5: Refund và return

- Cancel paid order.
- Refund.
- Return request.
- Inventory returned.

### Phase 6: Reporting và performance

- Admin dashboard.
- Complex query.
- Index tuning.
- Summary table.
- Seed data lớn.

### Phase 7: Architecture nâng cao

- Outbox pattern.
- Async event handling.
- Redis cache.
- Message queue.
- Retry mechanism.
- Distributed tracing/logging nếu muốn.

## 12. Checklist case bắt buộc phải test

Checkout:

- Checkout cart rỗng.
- Checkout SKU inactive.
- Checkout SKU hết hàng.
- Checkout nhiều SKU, một SKU thiếu hàng.
- Checkout dùng coupon hết hạn.
- Checkout double click cùng idempotency key.
- 100 request cùng mua SKU còn 10 cái, chỉ 10 đơn reserve thành công.

Payment:

- Payment success bình thường.
- Payment failed bình thường.
- Payment callback duplicate.
- Payment amount mismatch.
- Payment success sau order expired.
- Payment failed sau payment success.

Order:

- Cancel pending order.
- Cancel paid order trước packing.
- Cancel shipped order bị từ chối.
- Transition trạng thái không hợp lệ bị từ chối.
- Hai admin cùng update một order.

Inventory:

- Inbound stock.
- Adjustment hợp lệ.
- Adjustment làm on hand nhỏ hơn reserved bị từ chối.
- Release reservation.
- Consume reservation.
- Ledger luôn được ghi.

Coupon:

- Coupon usage limit.
- Per user usage limit.
- Coupon race condition.
- Release coupon khi order expired.

Shipment:

- Tạo shipment sau paid.
- Không tạo shipment trước paid.
- Shipment webhook duplicate.
- Delivered cập nhật order.

Return/refund:

- Return quá hạn bị từ chối.
- Return partial.
- Refund duplicate callback.
- Returned sellable nhập lại kho.
- Returned damaged không nhập lại available stock.

Reporting:

- Revenue không tính cancelled order.
- Net revenue trừ refund.
- Date range timezone đúng.
- Query có pagination.
- Query có index phù hợp.

## 13. Kết luận

Đề bài này đủ lớn để học backend Spring Boot theo hướng thực chiến. Nếu làm nghiêm túc, trọng tâm không nằm ở việc tạo thật nhiều API CRUD, mà nằm ở việc đảm bảo hệ thống đúng trong các tình huống cạnh tranh, retry, dữ liệu cũ, trạng thái bất thường và dữ liệu lớn.

Khi implement, nên ưu tiên các nguyên tắc:

- State machine rõ ràng.
- Transaction boundary rõ ràng.
- Idempotency cho mọi action có retry.
- Inventory ledger để audit.
- Locking đúng chỗ để chống overselling.
- Reporting tách khỏi transaction flow chính.
- Không tin frontend trong các nghiệp vụ tiền, tồn kho, trạng thái.
