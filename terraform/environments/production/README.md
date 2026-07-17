# Môi trường Production

Không tạo tài nguyên `production` cho đến khi `staging` đạt cổng nghiệm thu.

Production phải dùng lại cùng module đã đi qua `dev` và `staging`.

Khác biệt của production nằm ở biến cấu hình, ví dụ:

- Multi-AZ.
- Deletion protection.
- Backup retention dài hơn.
- Instance size lớn hơn.
- Số replica cao hơn.
- Domain production.
- Alert tới kênh on-call thật.
- Quyền IAM chặt hơn.

Không copy module thành bản riêng cho production.

Nếu production cần một khả năng mới, hãy bổ sung vào module dùng chung, kiểm tra lại ở `dev`, rồi mới promote lên `staging` và `production`.
