Đề bài: https://github.com/copilot/c/9f93f3cb-3ebd-4592-86b0-772e1521df93
https://github.com/copilot/share/ca664302-0864-8407-8802-3646645240c6

https://github.com/copilot/c/d917b13a-e158-49e9-a463-12b5b8105b61
https://github.com/copilot/c/83659041-feb1-4aaf-9eae-71e5de6ea683

+ Low cardinality convention: https://github.com/copilot/c/b0b4d123-7236-4fee-b515-e682abaa34f5

+ DISCARD PLANS;
+ DISCARD ALL;
===================================Câu 2: Top 20 ngành hàng (category) theo doanh thu (90 ngày)==============================
SET search_path = bd, public;

EXPLAIN ANALYZE
WITH max_ts AS (
SELECT max(order_ts) AS max_order_ts
FROM bd.fact_orders
) -- index trên order_ts

SELECT
p.category_id,
SUM(o.qty * o.unit_price_cents - o.discount_cents + o.shipping_cents + o.tax_cents) AS net_revenue_cents,
COUNT(*) AS orders_count
FROM bd.fact_orders o
JOIN bd.dim_product p
ON p.product_id = o.product_id
WHERE o.order_ts >= (SELECT date_trunc('day', max_order_ts) - interval '89 days' FROM max_ts)
AND o.order_ts <  (SELECT date_trunc('day', max_order_ts) + interval '1 day'  FROM max_ts)
AND o.status IN (1, 2)  -- paid/shipped
GROUP BY p.category_id
ORDER BY net_revenue_cents DESC
LIMIT 20;

+ Data nhiều nên lấy ra thì optimize hết cỡ cũng tới 12s
+ Không phải lúc nào có index mới mạnh hơn thì câu query cũng sẽ nhanh hơn (pg không thể đánh giá được hết tổng quát 
performance của 1 câu query, thường chỉ là optimize từng query nhỏ lẻ rồi gộp lại → có thể câu query tổng thể sẽ không thực sự optimize)
+ 1 câu query, nhưng khi thêm index vào lại gây chậm (Đây là hiện tượng kinh điển: random I/O amplification trên bảng
dimension do mất đi locality của Bitmap Heap Scan.) https://github.com/copilot/c/267a9e74-3801-4e7d-a591-083a4afdf6f5

===================================Câu 3: Tỉ lệ hủy và hoàn tiền theo kênh bán (channel)==============================
SET search_path = bd, public;

EXPLAIN ANALYZE
SELECT channel,
COUNT(*) FILTER (WHERE status = 3),
COUNT(*) FILTER (WHERE status = 4)
-- ,ROUND(100.0 * COUNT(*) FILTER (WHERE fact_orders.status = 4) / NULLIF(COUNT(*), 0), 4),
-- ROUND(100.0 * COUNT(*) FILTER (WHERE fact_orders.status = 3) / NULLIF(COUNT(*), 0), 4)
FROM fact_orders
WHERE status IN (3, 4)
GROUP BY fact_orders.channel

+ Tổng rows trong bảng:    ~10,000,000 >< Rows match (status 3,4): ~2,500,000  → chiếm 25% 
--> pg sẽ seq scan thay vì index scan (vì index scan sẽ phải random I/O, còn seq scan sẽ đọc liên tục), tổng thể sẽ nhanh hơn


                    Có 2 cách để group by:
                            CÁCH 1                                    CÁCH 2
                    (Plan hiện tại)                           (Plan thay thế)
                    
                    SeqScan + Filter                          SeqScan + Filter
                    W0: 3 rows, Leader: 3 rows               W0: 3 rows, Leader: 3 rows
                    │                                         │
                    ▼                                         ▼
                    Partial HashAgg                           Partial HashAgg
                    W0: offline=1,online=2                    W0: offline=1,online=2
                    Ld: offline=2,online=1                    Ld: offline=2,online=1
                    │                                         │
                    ▼                                         │
                    Sort (mỗi process)                              │ (không cần sort)
                    W0: offline→online                              │
                    Ld: offline→online                              │
                    │                                         │
                    ▼                                         ▼
                    Gather Merge                              Gather
                    → offline,1                               → online,2  (lộn xộn)
                    → offline,2  (cùng group                  → offline,1
                    → online,2    liền nhau)                  → offline,2
                    → online,1                                → online,1
                    │                                         │
                    ▼                                         ▼
                    Finalize GroupAggregate                   Finalize HashAggregate
                    Đọc stream:                               Dùng hash table:
                    offline: 1+2 = 3 → emit                  online:  2+1 = 3
                    online:  2+1 = 3 → emit                  offline: 1+2 = 3
                    │                                         │
                    ▼                                         ▼
                    offline=3, online=3 ✅                    offline=3, online=3 ✅
                    
                    GIỐNG NHAU                                GIỐNG NHAU


===================================Câu 4: 100 khách hàng đăng ký sớm nhất nhưng đang active=============================
SET search_path = bd, public;

EXPLAIN ANALYZE
SELECT * FROM dim_customer WHERE status = 1 ORDER BY signup_at ASC LIMIT 20;

+ bình thường không có index sẽ seq scan, sort rồi lấy 20 record
+ Có 2 lựa chọn đánh index, cả hai đều chỉ đọc 20 index entries + ~20 heap pages. Hiệu năng query gần như ngang nhau.
-- Composite Index: CREATE INDEX idx_composite ON bd.dim_customer (status, signup_at ASC); 
  Index Scan on idx_composite
  Index Cond: (status = 1)        ← lọc TRONG index
  → Nhảy thẳng đến vùng status=1, duyệt theo signup_at ASC
  → Đọc 20 entries → fetch 20 heap rows → DỪNG
    Usecase: Bạn query NHIỀU giá trị status khác nhau, 1 index phục vụ TẤT CẢ các query

-- Partial (Condition) Index: CREATE INDEX idx_partial ON bd.dim_customer (signup_at ASC) WHERE status = 1;
  Index Scan on idx_partial
  → Toàn bộ index CHỈ CHỨA rows status=1 sẵn rồi
  → Duyệt từ đầu theo signup_at ASC
  → Đọc 20 entries → fetch 20 heap rows → DỪNG
    Usecase: Bạn CHỈ query status = 1 (hoặc rất ít giá trị cố định)
    Partial Index phát huy mạnh nhất khi điều kiện lọc ra tỷ lệ nhỏ (ví dụ 1-10%). Ở đây status=1 chiếm 60%, lợi ích tiết kiệm không đáng kể.

                        Tiêu chí	            Composite Index	        Partial Index
                        Entries trong index	   10,000,000 (toàn bộ)	    5,959,000 (chỉ status=1)
                        Kích thước index	   ~300 MB	                ~180 MB
                        Tốc độ query status=1	⚡ Rất nhanh	            ⚡ Rất nhanh
                        Tốc độ query status=0	⚡ Rất nhanh	            ❌ Không dùng được
                        Tốc độ query status=2,3,...	⚡ Rất nhanh	        ❌ Không dùng được
                        Chi phí INSERT/UPDATE	Cao hơn (luôn cập nhật)	Thấp hơn (chỉ cập nhật khi status=1)
                        RAM (shared_buffers)	Tốn nhiều hơn	        Tiết kiệm hơn
                        VACUUM/maintenance	    Nặng hơn	            Nhẹ hơn

                            ╔═══════════════════════════════════════════════════════════════╗
                            ║                                                               ║
                            ║   → Dùng COMPOSITE INDEX (status, signup_at)                  ║
                            ║                                                               ║
                            ║   Lý do:                                                      ║
                            ║   1. status=1 chiếm 60% → partial index KHÔNG tiết kiệm       ║
                            ║      nhiều kích thước                                         ║
                            ║   2. Composite linh hoạt hơn: phục vụ mọi giá trị status      ║
                            ║   3. Sau này thêm query status=0 hay status=2                 ║
                            ║      → không cần tạo thêm index                               ║
                            ║   4. Hiệu năng query TƯƠNG ĐƯƠNG với partial index            ║
                            ║                                                               ║
                            ╚═══════════════════════════════════════════════════════════════╝