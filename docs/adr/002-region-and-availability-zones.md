# ADR 002: Region và Availability Zones

## Trạng thái

Đề xuất.

## Bối cảnh

Hệ thống cần chạy các thành phần quan trọng như Amazon EKS, Amazon RDS for MariaDB, Amazon MSK và Amazon ElastiCache. Các dịch vụ này cần được đặt trong region và Availability Zones phù hợp để cân bằng giữa độ trễ, độ sẵn sàng, chi phí và khả năng hỗ trợ dịch vụ.

Cần chốt region và cách dùng Availability Zones ngay từ đầu vì quyết định này ảnh hưởng đến VPC, subnet, EKS node group, database subnet group, MSK subnet và ElastiCache subnet group.

## Quyết định

Sử dụng region chính là `ap-southeast-1` cho bài thực hành.

Lý do:

- Gần Việt Nam và Đông Nam Á, độ trễ phù hợp cho người dùng trong khu vực.
- Hỗ trợ đầy đủ các dịch vụ cần dùng trong bài thực hành: EKS, RDS for MariaDB, MSK, ElastiCache, ECR, Secrets Manager, S3 và CloudWatch.
- Phù hợp để mô phỏng hệ thống production tại khu vực châu Á.

Sử dụng tối thiểu 2 Availability Zones cho mỗi môi trường:

```text
ap-southeast-1a
ap-southeast-1b
```

Nếu cần độ sẵn sàng cao hơn cho staging hoặc production, có thể mở rộng lên 3 Availability Zones sau khi dev đã hoàn thành và được nghiệm thu.

## Nguyên tắc mạng

- Mỗi môi trường có CIDR riêng, không trùng nhau.
- Public subnet dùng cho load balancer và NAT Gateway nếu cần.
- Private subnet dùng cho EKS worker node, RDS, MSK và ElastiCache.
- Không đặt database, Kafka hoặc Redis trong public subnet.

CIDR đề xuất:

| Môi trường | CIDR |
|---|---|
| `shared-services` | `10.10.0.0/16` |
| `dev` | `10.20.0.0/16` |
| `staging` | `10.30.0.0/16` |
| `production` | `10.40.0.0/16` |

## Hệ quả

Ưu điểm:

- Dễ mở rộng theo từng môi trường.
- Giảm rủi ro trùng CIDR khi cần peering, transit gateway hoặc VPN.
- Phù hợp với yêu cầu dựng nền tảng dùng chung trước, sau đó mới dựng dev, staging và production.

Đánh đổi:

- Region `ap-southeast-1` có thể có chi phí cao hơn một số region khác.
- Việc dùng nhiều Availability Zones làm tăng chi phí network và NAT Gateway.

## Việc cần xác nhận

- Region cuối cùng: `ap-southeast-1`
- Số Availability Zones cho dev: `2`
- Domain nội bộ hoặc public domain: `TBD`
- Người phê duyệt quyết định: `TBD`
