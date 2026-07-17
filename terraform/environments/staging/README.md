# Môi trường Staging

Không tạo tài nguyên `staging` cho đến khi `dev` đạt cổng nghiệm thu.

Khi bắt đầu `staging`, phải dùng lại các module đã kiểm chứng ở `dev`.

Khác biệt giữa `dev` và `staging` nằm ở:

- Backend key.
- File `.tfvars`.
- CIDR.
- Instance size.
- Backup retention.
- Số replica.
- Domain.
- Mức High Availability.

Không copy module `dev` thành một module `staging` riêng.
