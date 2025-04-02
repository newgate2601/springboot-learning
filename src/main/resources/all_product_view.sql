-- https://chat.deepseek.com/a/chat/s/fde15086-cc41-4aea-9425-fcc404042bf3

CREATE
OR REPLACE VIEW all_product_view AS
SELECT tbl_product_mapping.id,
       tbl_product_mapping.product_code,
       tbl_product_mapping.part_code,
       tbl_product_mapping.po,
       tbl_product_mapping.pc_order,
       'mapping' AS source_table
FROM tbl_product_mapping

UNION ALL

SELECT tbl_product_part.id,
       tbl_product_part.product_code,
       tbl_product_part.part_code,
       tbl_product_part.po,
       tbl_product_part.pc_order,
       'part' AS source_table
FROM tbl_product_part;

-- Cả tbl_product_mapping và tbl_product_part đều có cột id
-- Khi UNION ALL, các ID từ 2 bảng có thể trùng nhau nhưng thực tế là 2 bản ghi khác nhau
-- Điều này gây ra hiện tượng trùng lặp khi Spring Boot xử lý kết quả
-- Giải pháp tạo ID duy nhất View mới giải quyết bằng cách:   Tạo unique_id bằng cách ghép tiền tố vào ID gốc:

-- của cty
CREATE
OR REPLACE VIEW public.all_mapping_view AS
SELECT ROW_NUMBER() OVER (ORDER BY product_code) AS id,  -- Tạo ID mới product_code, po_number, -- Tương ứng với cột `po` trong ảnh
       part_number,                                                                             -- Tương ứng với cột `part_code` trong ảnh
       number_sub_code,-- Cột mới (không có trong ảnh)
       quantity,                                                                                -- Cột mới (không có trong ảnh)
       source                                                                                   -- 'mapping' hoặc 'part'
FROM (SELECT pm.product_code,
             pm.po_number   AS po_number,  -- Ở đây là `po` trong ảnh
             pm.part_number AS part_number,-- Ở đây là `part_code` trong ảnh
             pm.number_sub_code,           -- Không có trong ảnh
             pm.quantity,                  -- Không có trong ảnh
             'mapping'      AS source
      FROM product_mapping pm -- Bảng tbl_product_mapping trong ảnh

      UNION ALL

      SELECT pp.product_code,
             pp.po_number   AS po_number,       -- Ở đây là `po` trong ảnh
             pp.part_number AS part_number,-- Ở đây là `part_code` trong ảnh
             NULL           AS number_sub_code, -- Giá trị NULL vì bảng part không có cột này
             pp.quantity,                       -- Không có trong ảnh
             'part'         AS source
      FROM product_part pp -- Bảng tbl_product_part trong ảnh
     ) AS combined_data;

-- của mk

CREATE
OR REPLACE VIEW all_product_view AS
SELECT ('mapping_' || tbl_product_mapping.id) as id,
       tbl_product_mapping.product_code,
       tbl_product_mapping.part_code,
       tbl_product_mapping.po,
       tbl_product_mapping.pc_order,
       'mapping'::text AS source_table
FROM tbl_product_mapping
UNION ALL
SELECT ('part_' || tbl_product_part.id) as id,
       tbl_product_part.product_code,
       tbl_product_part.part_code,
       tbl_product_part.po,
       tbl_product_part.pc_order,
       'part'::text AS source_table
FROM tbl_product_part;

