# Query Lab Data Model

## 1) Mục tiêu business

Bộ schema này được thiết kế để học các truy vấn sau:
- Join nhiều cấp giữa mua hàng, tồn kho, bán hàng, giao hàng.
- Truy vết nguồn gốc hàng hóa từ `PO -> lô tồn -> shipment`.
- Phân tích biên lợi nhuận theo sản phẩm, nhà cung cấp, khách hàng, khu vực.
- Luyện tối ưu query trên tập dữ liệu lớn với index và điều kiện lọc.

## 2) Business logic tổng quan

### Luồng Mua hàng (Procurement)
1. User thuộc một business unit tạo `ql_purchase_order` cho một `ql_supplier`.
2. Từng dòng PO lưu tại `ql_purchase_order_line` theo sản phẩm.
3. Mỗi sản phẩm có thể map với nhiều nhà cung cấp qua `ql_product_supplier_map`.

### Luồng Tồn kho (Inventory)
1. Khi nhận hàng từ PO line, hệ thống tạo `ql_inventory_lot`.
2. Tồn được quản lý theo kho (`ql_warehouse`) + sản phẩm + lot code.
3. Lô có thông tin hạn dùng để query FEFO/FIFO.

### Luồng Bán hàng (Sales)
1. Khách hàng tạo `ql_sales_order`, mỗi dòng lưu tại `ql_sales_order_line`.
2. Đơn bán liên kết business unit và sales owner để truy báo cáo doanh thu theo nhóm.

### Luồng Giao hàng (Fulfillment)
1. `ql_shipment` được tạo để giao một sales order từ một kho.
2. `ql_shipment_item` map dòng SO với lô tồn đã xuất.
3. Từ đây có thể truy vết ngược shipment item về PO line gốc.

## 3) Danh sách table và ý nghĩa

| Table | Vai trò business | Khóa chính | FK chính |
|---|---|---|---|
| `ql_business_unit` | Đơn vị vận hành để tách dữ liệu | `id` | - |
| `ql_user_account` | Người dùng thao tác nghiệp vụ | `id` | `business_unit_id` |
| `ql_supplier` | Nhà cung cấp | `id` | - |
| `ql_product_category` | Nhóm sản phẩm (có parent) | `id` | `parent_id` |
| `ql_product` | Master sản phẩm | `id` | `category_id` |
| `ql_product_supplier_map` | Mapping nhiều-nhiều product <> supplier | `id` | `product_id`, `supplier_id` |
| `ql_warehouse` | Kho vận hành | `id` | `business_unit_id` |
| `ql_purchase_order` | Header PO | `id` | `supplier_id`, `business_unit_id`, `ordered_by_user_id` |
| `ql_purchase_order_line` | Dòng chi tiết PO | `id` | `purchase_order_id`, `product_id` |
| `ql_customer` | Master khách hàng | `id` | - |
| `ql_sales_order` | Header SO | `id` | `customer_id`, `business_unit_id`, `sales_owner_user_id` |
| `ql_sales_order_line` | Dòng chi tiết SO | `id` | `sales_order_id`, `product_id` |
| `ql_inventory_lot` | Tồn theo lô trong kho | `id` | `warehouse_id`, `product_id`, `source_po_line_id` |
| `ql_shipment` | Header giao hàng | `id` | `sales_order_id`, `warehouse_id` |
| `ql_shipment_item` | Mapping dòng giao hàng với SO line và lô tồn | `id` | `shipment_id`, `sales_order_line_id`, `inventory_lot_id` |

## 4) Mapping table cần chú ý

| Mapping table | Kiểu mapping | Mục đích |
|---|---|---|
| `ql_product_supplier_map` | Product N-N Supplier | Chọn NCC tốt nhất theo giá, lead time, min order qty |
| `ql_shipment_item` | Shipment N-N SalesOrderLine + InventoryLot | Truy vết xuất kho đến cấp lô cho từng dòng bán |

## 5) Join path để luyện query sâu

### Path A: Profitability theo nhà cung cấp
`ql_supplier -> ql_purchase_order -> ql_purchase_order_line -> ql_inventory_lot -> ql_shipment_item -> ql_sales_order_line -> ql_sales_order`

### Path B: Fill rate theo kho và lô
`ql_warehouse -> ql_inventory_lot -> ql_shipment_item -> ql_sales_order_line`

### Path C: Product performance theo category
`ql_product_category -> ql_product -> ql_sales_order_line -> ql_sales_order`

## 6) Ghi chú implementation

- Toàn bộ table và FK được map bằng JPA Entity trong package `com.example.learning.entity.querylab`.
- Repository được tạo đầy đủ trong package `com.example.learning.repository.querylab`.
- Mỗi field trong entity đã được gắn comment business ngay cạnh để học schema nhanh.
