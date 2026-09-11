# Module EKS — bước 4.1

Module dùng chung để tạo EKS, IAM cluster/node/CNI, OIDC provider, Access Entry operator, CloudWatch logs, launch template và managed node group.

Thứ tự bootstrap: cluster → OIDC/IRSA → VPC CNI → node group → CoreDNS/kube-proxy.

## Input

- `name`, `tags`: tên và nhãn tài nguyên.
- `vpc_id`, `subnet_ids`: VPC và private app subnet từ network state; ít nhất hai AZ, tắt public IPv4 assignment.
- `kubernetes_version`, `addon_versions`, `node.ami_release`: phiên bản đã kiểm tra tương thích trên AWS.
- `operator_role_arn`: IAM role ARN tồn tại, không dùng STS session ARN.
- `admin_public_cidrs`: danh sách IPv4 /32 cho public API; mặc định rỗng.
- `management_security_group_ids`: SG được truy cập private API TCP 443.
- `node`: instance type, min/desired/max capacity, AMI release, disk size.

Module bật private API, IMDSv2, mã hóa EBS, control-plane logs và CNI NetworkPolicy engine. Quyền CNI tách khỏi node role. Operator được cấp cluster-admin bằng Access Entry.

## Output

`cluster_name`, `cluster_endpoint`, `cluster_security_group_id`, `oidc_provider_arn`, `node_role_arn`, `vpc_cni_role_arn`.

Root dev gọi module tại `../../environments/dev/eks`. Module không tạo VPC, Argo CD, EBS CSI, database hoặc ứng dụng. Bật NetworkPolicy engine chưa tạo policy workload.

Nguồn cấu hình: mục Bước 4.1 trong [tài liệu thực hành](../../../AWS/THUC-HANH-CHI-TIET-AWS-STEP-BY-STEP.md).
