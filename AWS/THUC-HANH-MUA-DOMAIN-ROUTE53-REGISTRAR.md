# Thực hành mua domain qua AWS Route 53 Registrar

> Tài liệu này hướng dẫn thao tác mua domain trực tiếp trên AWS bằng Route 53 Registrar để phục vụ các bài lab dùng Route 53, ACM, ALB, EKS Ingress, Argo CD hoặc API endpoint.  
> Đây là bước có thể phát sinh chi phí thật. Đọc kỹ giá, thời hạn đăng ký, auto-renew và thông tin liên hệ trước khi xác nhận mua.

---

## 1. Mục tiêu của bước này

Mục tiêu là sở hữu một domain thật, ví dụ:

```text
newgate2601.com
```

Sau khi mua xong, ta có thể tạo các subdomain cho endpoint lab:

```text
api.newgate2601.com
argocd.newgate2601.com
app.newgate2601.com
```

và để Terraform tạo:

```text
ACM certificate
Route 53 DNS validation record
Route 53 alias record trỏ về ALB
HTTPS listener cho ALB
```

Lưu ý: mua domain không giống tạo EC2 hay ALB. Đây là đăng ký quyền sở hữu tên miền và có chi phí theo năm.

---

## 2. Vì sao không để Terraform tự mua domain?

Không nên để Terraform tự mua domain vì:

- Mua domain phát sinh tiền thật.
- Domain registration cần thông tin liên hệ cá nhân/công ty.
- Có thể có email xác minh sau khi mua.
- Có auto-renew, privacy protection và điều khoản đăng ký riêng.
- Nếu ai đó chạy nhầm `terraform apply`, có thể mua domain ngoài ý muốn.

Vì vậy bài lab tách rõ:

```text
Mua domain
  -> thao tác có kiểm soát trên AWS Console

Tạo DNS record, ACM, ALB
  -> quản lý bằng Terraform
```

---

## 3. Trước khi bắt đầu cần có gì

Cần chuẩn bị:

- Đăng nhập được AWS Console bằng IAM user/admin lab.
- Đã bật MFA cho account.
- Biết region lab đang dùng, ví dụ:

```text
ap-southeast-1
```

- Có quyền dùng Route 53 và Route 53 Domains.
- Có thẻ/thanh toán hợp lệ trong AWS account.
- Đã chọn tên domain muốn mua.

Ví dụ:

```text
newgate2601.com
newgate2601.net
newgate2601.dev
```

Lưu ý về giá:

```text
Mỗi đuôi domain có giá khác nhau.
.com, .net, .org, .io, .dev có thể khác giá rất nhiều.
Một số domain premium có giá cao hơn domain bình thường.
```

---

## 4. Thao tác chi tiết trên AWS Console

### 4.1. Mở AWS Console

1. Mở trình duyệt.
2. Vào:

```text
https://console.aws.amazon.com/
```

3. Đăng nhập bằng IAM user lab, không dùng root account cho công việc hằng ngày.
4. Kiểm tra góc phải trên cùng xem đang ở đúng account.

Route 53 là dịch vụ global, không phụ thuộc chặt vào region như EC2 hay ALB. Tuy nhiên ACM certificate cho ALB sau này vẫn phải tạo ở cùng region với ALB.

### 4.2. Vào dịch vụ Route 53

Trên AWS Console:

1. Bấm ô tìm kiếm ở thanh trên cùng.
2. Gõ:

```text
Route 53
```

3. Chọn dịch vụ:

```text
Route 53
```

Bạn sẽ vào Route 53 Console.

### 4.3. Vào trang đăng ký domain

Trong menu bên trái của Route 53:

1. Tìm nhóm:

```text
Domains
```

2. Chọn:

```text
Registered domains
```

3. Bấm nút:

```text
Register domains
```

hoặc:

```text
Register domain
```

Tên nút có thể thay đổi nhẹ theo giao diện AWS, nhưng ý nghĩa là bắt đầu đăng ký domain mới.

### 4.4. Tìm domain muốn mua

Ở màn hình tìm domain:

1. Nhập domain mong muốn.

Ví dụ:

```text
newgate2601.com
```

2. Bấm:

```text
Search
```

AWS sẽ kiểm tra domain còn khả dụng hay không.

Các trạng thái thường gặp:

| Trạng thái | Ý nghĩa |
|---|---|
| Available | Domain còn có thể mua. |
| Unavailable | Domain đã có người sở hữu. |
| Premium | Domain có giá đặc biệt, thường rất cao. |
| Suggested alternatives | AWS gợi ý tên/đuôi khác. |

Nếu domain `.com` không còn, có thể thử:

```text
newgate2601.net
newgate2601.dev
newgate2601.io
newgate2601.cloud
```

Nhưng nên xem kỹ giá trước khi chọn.

### 4.5. Chọn domain và thời hạn đăng ký

Khi domain còn khả dụng:

1. Bấm chọn domain đó.
2. Thêm vào cart nếu giao diện có giỏ hàng.
3. Chọn số năm đăng ký.

Thông thường chọn:

```text
1 year
```

cho bài lab.

Không chọn nhiều năm nếu chỉ đang học hoặc chưa chắc dùng lâu dài.

### 4.6. Kiểm tra auto-renew

AWS có thể cho chọn auto-renew.

Ý nghĩa:

```text
Auto-renew bật
  -> đến hạn AWS tự gia hạn domain và tính phí

Auto-renew tắt
  -> đến hạn domain có thể hết hạn nếu không gia hạn thủ công
```

Với lab cá nhân:

- Nếu sợ quên và mất domain: bật auto-renew.
- Nếu sợ phát sinh phí năm sau: tắt auto-renew hoặc ghi reminder kiểm tra trước ngày hết hạn.

Nên ghi chú lại:

```text
Domain:
Ngày mua:
Ngày hết hạn:
Auto-renew: bật/tắt
```

### 4.7. Nhập thông tin liên hệ

AWS sẽ yêu cầu thông tin liên hệ cho domain. Thường có các nhóm:

```text
Registrant contact
Administrative contact
Technical contact
```

Nếu là lab cá nhân, có thể dùng cùng thông tin cho cả ba nhóm nếu AWS cho phép.

Thông tin thường gồm:

- Họ tên.
- Email.
- Số điện thoại.
- Địa chỉ.
- Thành phố.
- Quốc gia.
- Mã bưu chính.

Điền email thật đang dùng được vì AWS hoặc registrar có thể gửi email xác minh.

Lưu ý:

```text
Không dùng email giả.
Không dùng thông tin không truy cập được.
Nếu không xác minh email đúng hạn, domain có thể bị suspend tùy chính sách registrar/TLD.
```

### 4.8. Bật privacy protection nếu có

Một số TLD cho phép bật:

```text
Privacy protection
```

hoặc:

```text
WHOIS privacy
```

Tính năng này giúp ẩn một phần thông tin liên hệ khỏi WHOIS public.

Với lab cá nhân, nên bật nếu AWS/TLD hỗ trợ và không phát sinh yêu cầu đặc biệt.

Một số loại domain/TLD có thể không hỗ trợ privacy hoặc có quy định riêng.

### 4.9. Review đơn hàng

Ở màn hình review:

Kiểm tra thật kỹ:

```text
Domain đúng chưa?
Đuôi domain đúng chưa?
Giá/năm là bao nhiêu?
Số năm đăng ký là bao nhiêu?
Auto-renew bật hay tắt?
Email liên hệ đúng chưa?
Privacy protection bật/tắt thế nào?
```

Ví dụ cần xác nhận domain:

```text
newgate2601.com
```

Không nhầm với:

```text
newgate2601.net
newgate260l.com   # chữ l thay vì số 1
newgate2601.co
```

### 4.10. Chấp nhận điều khoản và mua domain

AWS sẽ yêu cầu xác nhận điều khoản đăng ký domain.

Thao tác:

1. Tick checkbox đồng ý điều khoản nếu có.
2. Bấm:

```text
Submit
```

hoặc:

```text
Complete purchase
```

hoặc:

```text
Register domain
```

Sau khi bấm, AWS bắt đầu xử lý đăng ký domain.

Không bấm nhiều lần nếu màn hình đang xử lý.

### 4.11. Chờ domain đăng ký xong

Sau khi mua, vào:

```text
Route 53
  -> Domains
  -> Requests
```

hoặc kiểm tra trong:

```text
Route 53
  -> Domains
  -> Registered domains
```

Trạng thái có thể là:

```text
In progress
Successful
Failed
```

Thời gian xử lý có thể vài phút hoặc lâu hơn tùy TLD/registrar.

### 4.12. Xác minh email nếu AWS gửi yêu cầu

Kiểm tra email đăng ký domain.

Nếu có email yêu cầu xác minh:

1. Mở email.
2. Bấm link xác minh.
3. Làm theo hướng dẫn.

Không bỏ qua email xác minh.

Nếu quá hạn xác minh, domain có thể bị tạm khóa/suspend theo chính sách đăng ký.

---

## 5. Kiểm tra hosted zone sau khi mua

Sau khi domain đăng ký xong, vào:

```text
Route 53
  -> Hosted zones
```

Kiểm tra có hosted zone public cho domain không.

Ví dụ:

```text
newgate2601.com
```

Nếu có, bấm vào hosted zone và kiểm tra record:

```text
NS
SOA
```

Record `NS` chứa nameserver AWS cấp cho domain.

Ví dụ:

```text
ns-123.awsdns-15.com
ns-456.awsdns-27.net
ns-789.awsdns-39.org
ns-101.awsdns-48.co.uk
```

Nếu mua domain qua Route 53 Registrar, AWS thường cấu hình domain dùng bộ nameserver này. Tuy nhiên vẫn nên kiểm tra lại trong `Registered domains`.

---

## 6. Kiểm tra bằng AWS CLI

Kiểm tra account:

```powershell
aws sts get-caller-identity
```

Kiểm tra hosted zone:

```powershell
aws route53 list-hosted-zones
```

Nếu domain là:

```text
newgate2601.com
```

kết quả cần thấy hosted zone có tên:

```text
newgate2601.com.
```

Lưu ý AWS CLI thường hiển thị domain trong DNS format có dấu chấm cuối:

```text
newgate2601.com.
```

Trong Terraform có thể dùng:

```hcl
route53_zone_name = "newgate2601.com"
```

hoặc nếu cần:

```hcl
route53_zone_name = "newgate2601.com."
```

Nếu Terraform báo không tìm thấy hosted zone, thử kiểm tra chính xác tên hosted zone bằng CLI.

---

## 7. Dùng domain cho các bài lab sau

Sau khi có domain và hosted zone, domain có thể dùng cho các endpoint thật trong các bài lab sau:

```text
api.<domain>
argocd.<domain>
app.<domain>
admin.<domain>
```

Với lab hiện tại, chưa cần tạo record ngay nếu chưa có ALB hoặc endpoint thật.

Khi một module sau này cần DNS/TLS, truyền hosted zone vào module đó bằng biến riêng, ví dụ:

```hcl
route53_zone_name = "newgate2601.com"
```

Sau đó để module tạo ACM certificate và Route 53 record cho endpoint tương ứng.

Không còn dùng domain này để dựng GitLab Self-Managed trong luồng chính của bài lab AWS credit 100$.

---

## 8. Lỗi thường gặp và cách xử lý

| Lỗi | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| Domain không available | Đã có người mua. | Chọn tên khác hoặc TLD khác. |
| Domain premium quá đắt | Domain thuộc nhóm premium. | Không dùng cho lab, chọn domain rẻ hơn. |
| Không thấy hosted zone | Domain chưa tạo hosted zone hoặc đang ở account khác. | Vào Route 53 `Hosted zones` kiểm tra, tạo public hosted zone nếu cần. |
| Terraform báo `no matching Route 53 Hosted Zone found` | `route53_zone_name` sai hoặc hosted zone chưa tồn tại. | Chạy `aws route53 list-hosted-zones`, sửa lại tên zone. |
| ACM certificate không `ISSUED` | DNS validation record chưa đúng hoặc hosted zone chưa delegate đúng. | Kiểm tra record validation và nameserver. |
| Domain mua ở nơi khác không resolve qua Route 53 | Nameserver ở registrar chưa trỏ về Route 53. | Copy 4 NS của hosted zone Route 53 sang registrar. |
| Quên xác minh email | Email registrar gửi bị bỏ qua. | Tìm email từ AWS/registrar và xác minh. |
| Bị tính phí gia hạn năm sau | Auto-renew bật. | Kiểm tra auto-renew trong Registered domains. |

---

## 9. Kết quả sau bước này

Sau khi hoàn thành:

```text
[x] Đã mua domain hoặc có domain thật
[x] Domain nằm trong Route 53 Registered domains nếu mua qua AWS
[x] Có public hosted zone trong Route 53
[x] Nameserver của domain trỏ đúng về Route 53
[x] Có thể dùng subdomain như api.<domain>, app.<domain> hoặc argocd.<domain>
[x] Terraform module sau này có thể tìm thấy hosted zone
[x] Terraform module sau này có thể tạo ACM certificate và Route 53 record cho endpoint tương ứng
```

Sau bước này, quay lại root module của endpoint cần DNS/TLS, ví dụ EKS ingress hoặc Argo CD, rồi chạy:

```powershell
terraform plan
```

để kiểm tra các resource DNS/TLS/ALB liên quan.
