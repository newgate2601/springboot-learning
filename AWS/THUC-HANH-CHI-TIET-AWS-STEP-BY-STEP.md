# Thực hành triển khai hệ thống chi tiết từng bước

> Tài liệu này ghi lại quá trình thực hành triển khai hệ thống theo kiểu làm thật: cần bấm gì trên giao diện, tạo file nào, cấu hình gì, chạy lệnh gì, kiểm tra ra sao và vì sao phải làm như vậy.  
> Phạm vi không chỉ có AWS, mà còn gồm GitLab, Terraform, Docker, Amazon ECR, Kubernetes/EKS, Argo CD, GitOps, application config, secret, database, Kafka, Redis, observability, CI/CD và các bước kiểm thử vận hành.  
> Mỗi bước lớn sẽ có mục tiêu, ý nghĩa, thao tác chi tiết trên giao diện liên quan, file/config cần tạo hoặc sửa, lệnh cần chạy, cách kiểm tra hoàn thành và lỗi thường gặp.  
> Các bước sau nên viết tiếp theo đúng format này để toàn bộ quá trình thực hành nhất quán, dễ đọc lại và dễ làm lại.

---

## Format chuẩn cho mỗi bước thực hành

Khi thêm một bước mới, dùng cấu trúc sau:

```text
## Bước X - Tên bước lớn

### 1. Mục tiêu của bước này
Giải thích sau khi xong bước này ta đạt được điều gì.
Nêu rõ bước này phục vụ phần nào của hệ thống: AWS, GitLab, Terraform, Docker, Kubernetes, Argo CD, ứng dụng, database, CI/CD hoặc vận hành.

### 2. Vì sao cần làm bước này
Giải thích ý nghĩa kỹ thuật và ý nghĩa vận hành.
Nêu rõ nếu bỏ qua bước này thì hệ thống sẽ thiếu gì, rủi ro gì hoặc không chạy được ở đâu.

### 3. Trước khi bắt đầu cần có gì
Liệt kê tài khoản, quyền, region, công cụ, thông tin cần chuẩn bị.
Ví dụ: AWS account, GitLab project, AWS CLI, Terraform, Docker, kubectl, helm, domain, repository URL, biến môi trường, secret.

### 4. Thao tác chi tiết
Ghi từng hành động nhỏ theo đúng thứ tự thực hiện.
Nếu thao tác trên giao diện thì ghi: bấm đâu, chọn gì, nhập gì, xác nhận gì.
Nếu thao tác trong code/repo thì ghi: tạo file nào, sửa file nào, thêm nội dung gì.
Nếu thao tác bằng CLI thì ghi: chạy lệnh nào, chạy ở thư mục nào, output mong đợi là gì.

### 5. File/config/lệnh liên quan
Ghi rõ:
- Đường dẫn file cần tạo hoặc sửa.
- Nội dung cấu hình quan trọng.
- Biến môi trường hoặc secret cần có.
- Command cần chạy.
- Ý nghĩa của từng file/config/lệnh.

### 6. Giải thích từng phần quan trọng
Giải thích các dòng trên màn hình, cảnh báo, nút bấm, lựa chọn, file config, tham số Terraform, Kubernetes manifest, CI job, Dockerfile hoặc application property quan trọng.

### 7. Kiểm tra hoàn thành
Ghi dấu hiệu nào chứng minh bước này đã làm xong.
Nên có kiểm tra bằng giao diện, CLI hoặc log nếu phù hợp.

### 8. Lỗi thường gặp và cách xử lý
Ghi lại vấn đề có thể gặp trong lúc thao tác.
Ưu tiên ghi lỗi thật dễ gặp: sai region, thiếu quyền, sai secret, sai path, sai branch, pipeline fail, pod CrashLoopBackOff, image pull lỗi.

### 9. Kết quả sau bước này
Tóm tắt trạng thái hệ thống sau khi hoàn thành.
Nêu rõ bước tiếp theo nên làm là gì.
```

---

## Bước 2.1 - Bảo vệ AWS root account bằng MFA

### 1. Mục tiêu của bước này

Mục tiêu của bước này là bật **Multi-Factor Authentication (MFA)** cho AWS root account.

Sau khi hoàn thành, mỗi lần đăng nhập AWS bằng root account, ngoài email và mật khẩu, AWS sẽ yêu cầu thêm một mã xác thực 6 số từ ứng dụng Authenticator.

Ví dụ luồng đăng nhập sau khi bật MFA:

```text
Email root account
  -> Mật khẩu AWS
  -> Mã MFA 6 số từ app Authenticator
  -> Vào AWS Console
```

### 2. Vì sao cần làm bước này

Root account là tài khoản mạnh nhất trong một AWS account.

Root account có thể:

- Truy cập toàn bộ tài nguyên trong AWS account.
- Thay đổi thông tin thanh toán.
- Tạo hoặc xóa IAM user, role, policy.
- Tạo access key rất mạnh.
- Đóng AWS account.

Nếu root account chỉ có email và mật khẩu, khi mật khẩu bị lộ thì người khác có thể chiếm toàn bộ AWS account. MFA thêm một lớp bảo vệ thứ hai.

MFA có nghĩa là:

```text
Something you know     = mật khẩu
Something you have     = điện thoại/app Authenticator
```

Kẻ xấu muốn đăng nhập thì phải có cả mật khẩu lẫn thiết bị MFA.

### 3. Trước khi bắt đầu cần có gì

Cần chuẩn bị:

- Đăng nhập được AWS Console bằng root account.
- Có điện thoại hoặc thiết bị cài ứng dụng Authenticator.
- Một app tạo mã MFA, ví dụ:
  - Google Authenticator
  - Microsoft Authenticator
  - Authy
  - 1Password
  - Bitwarden Authenticator

Trong bài thực hành này, chưa dùng AWS Organizations và IAM Identity Center để tránh ảnh hưởng tới free credit. Vì vậy ta bảo vệ root account trước, sau đó mới tạo IAM user hoặc role đơn giản cho lab.

### 4. Thao tác chi tiết trên AWS Console

#### 4.1. Vào trang Security credentials

1. Đăng nhập AWS Console.
2. Nhìn góc phải trên cùng.
3. Bấm vào tên account hoặc account menu.
4. Chọn:

```text
Security credentials
```

Trang này dùng để quản lý các thông tin bảo mật của tài khoản đang đăng nhập, gồm MFA, access key, password và một số credential khác.

#### 4.2. Tìm phần MFA

1. Trong trang **My security credentials**, tìm mục:

```text
Multi-factor authentication (MFA)
```

2. Bấm nút:

```text
Assign MFA device
```

hoặc:

```text
Add MFA
```

Tên nút có thể hơi khác tùy giao diện AWS, nhưng ý nghĩa là gắn một thiết bị MFA mới vào account.

#### 4.3. Chọn loại MFA device

AWS có thể cho chọn nhiều loại MFA. Với bài lab, chọn:

```text
Authenticator app
```

Ý nghĩa:

- Đây là loại MFA dùng app trên điện thoại để tạo mã OTP 6 số.
- Mã thường đổi sau mỗi 30 giây.
- Không cần mua thiết bị vật lý riêng.

Sau đó nhập tên thiết bị MFA, ví dụ:

```text
newgate2601-root-mfa
```

Tên này chỉ để nhận diện trong AWS Console. Nên đặt tên rõ để biết MFA này dùng cho root account.

#### 4.4. Quét QR code

AWS sẽ hiện màn hình **Set up device** với QR code.

Trên điện thoại:

1. Mở app Authenticator.
2. Bấm dấu `+` hoặc nút thêm tài khoản.
3. Chọn:

```text
Scan QR code
```

4. Quét QR code trên màn hình AWS.

Sau khi quét, app sẽ tạo một mục mới cho AWS và hiển thị mã 6 số.

Lưu ý:

- QR code là bí mật.
- Không chia sẻ ảnh QR code công khai.
- Không gửi QR code cho người khác.
- QR code chỉ dùng lúc thiết lập MFA, không dùng mỗi lần đăng nhập.

#### 4.5. Nhập hai mã MFA liên tiếp

AWS yêu cầu nhập hai mã liên tiếp để chắc chắn app đã đồng bộ đúng.

Thao tác:

1. Nhìn mã 6 số hiện tại trong app Authenticator.
2. Nhập vào ô:

```text
MFA Code 1
```

3. Chờ khoảng 30 giây để app đổi sang mã mới.
4. Nhập mã mới vào ô:

```text
MFA Code 2
```

5. Bấm:

```text
Add MFA
```

Ví dụ:

```text
MFA Code 1: 123456
Chờ mã đổi
MFA Code 2: 789012
```

Không nhập cùng một mã vào cả hai ô. Hai ô cần hai mã khác nhau, liên tiếp nhau.

### 5. Giải thích từng phần trên màn hình `Set up device`

Bảng này dùng để đọc nhanh từng phần trên màn hình và hiểu ngay mình cần làm gì.

| Phần trên màn hình | Ý nghĩa | Bạn cần làm gì |
|---|---|---|
| `Set up device` | Màn hình thiết lập thiết bị xác thực MFA. Thiết bị ở đây là app Authenticator trên điện thoại, không nhất thiết là thiết bị vật lý riêng. | Đọc theo thứ tự từ bước 1 đến bước 3 trên màn hình. |
| `Authenticator app` | Loại MFA dùng ứng dụng tạo mã OTP. OTP là mã dùng một lần, thường gồm 6 chữ số và đổi liên tục sau khoảng 30 giây. | Dùng app như Google Authenticator, Microsoft Authenticator, Authy, 1Password hoặc Bitwarden Authenticator. |
| `Install a compatible application` | AWS nhắc bạn cài app tương thích trước khi quét QR. | Nếu điện thoại đã có app Authenticator thì bỏ qua. Nếu chưa có thì cài một app Authenticator. |
| `See a list of compatible applications` | Link xem danh sách app MFA AWS hỗ trợ. | Thường không cần bấm nếu dùng app phổ biến. |
| `QR code` | Mã QR chứa secret để app Authenticator sinh mã OTP cho account AWS này. | Mở app Authenticator, bấm thêm tài khoản, chọn quét QR và quét mã này. Không chia sẻ QR code. |
| `Show secret key` | Hiện secret dạng chữ nếu camera không quét được QR. | Chỉ dùng khi không quét được QR code. Bình thường để nguyên. |
| `MFA Code 1` | Ô nhập mã 6 số hiện tại trong app Authenticator. | Nhập mã đang hiển thị ngay sau khi quét QR. |
| `MFA Code 2` | Ô nhập mã 6 số kế tiếp sau khi app đổi mã. AWS cần hai mã liên tiếp để xác nhận app hoạt động đúng. | Chờ khoảng 30 giây cho app đổi mã, rồi nhập mã mới. |
| `Add MFA` | Nút xác nhận gắn thiết bị MFA vào root account. | Chỉ bấm sau khi nhập đủ `MFA Code 1` và `MFA Code 2`. |

Tóm tắt thao tác trên màn hình này:

```text
Cài/mở app Authenticator
+ Quét QR code
+ Nhập MFA Code 1
+ Chờ mã đổi
+ Nhập MFA Code 2
+ Bấm Add MFA
```

Ghi nhớ quan trọng:

| Câu hỏi | Trả lời |
|---|---|
| Có phải quét QR mỗi lần đăng nhập không? | Không. QR code chỉ dùng một lần khi thiết lập MFA. |
| Lần sau đăng nhập cần gì? | Email, mật khẩu AWS và mã 6 số trong app Authenticator. |
| App hỏi mật khẩu thì là mật khẩu gì? | Thường là PIN/mật khẩu của app hoặc điện thoại, không phải mật khẩu AWS. |
| Có nên chụp/gửi QR code cho người khác không? | Không. QR code là bí mật. |

### 6. Kiểm tra hoàn thành

Sau khi bấm **Add MFA**, AWS hiển thị thông báo màu xanh:

```text
MFA device assigned
```

Trong phần **Multi-factor authentication (MFA)** sẽ thấy một dòng loại:

```text
Virtual
```

và identifier có dạng:

```text
arn:aws:iam::<account-id>:mfa/<ten-mfa-device>
```

Ví dụ:

```text
arn:aws:iam::150914615641:mfa/newgate2601-root-mfa
```

Như vậy là MFA cho root account đã hoàn thành.

### 7. Lỗi thường gặp và cách xử lý

#### Nhập MFA Code 1 và MFA Code 2 bị báo sai

Nguyên nhân thường gặp:

- Nhập cùng một mã vào cả hai ô.
- Mã đã hết hạn vì chờ quá lâu.
- Đồng hồ trên điện thoại bị lệch giờ.

Cách xử lý:

- Xóa hai ô.
- Chờ app đổi sang mã mới.
- Nhập mã mới vào `MFA Code 1`.
- Chờ mã đổi lần nữa.
- Nhập mã kế tiếp vào `MFA Code 2`.

#### App hỏi mật khẩu sau khi quét

Đây thường là mật khẩu/PIN của chính app hoặc điện thoại, không phải mật khẩu AWS.

AWS chỉ cần mã MFA 6 số do app sinh ra.

#### Có cần quét QR mỗi lần đăng nhập không?

Không.

QR code chỉ dùng một lần lúc thiết lập MFA. Những lần đăng nhập sau chỉ cần mở app và lấy mã 6 số.

#### Có còn đăng nhập bằng email được không?

Có.

Sau khi bật MFA, vẫn đăng nhập bằng email root account và mật khẩu như cũ. Chỉ khác là AWS hỏi thêm mã MFA.

Luồng đăng nhập mới:

```text
Email
  -> Password
  -> MFA code
  -> AWS Console
```

### 8. Kết quả sau bước này

Trạng thái sau khi hoàn thành:

```text
[x] Root account đã có MFA
[x] Đăng nhập root cần thêm mã Authenticator
[x] QR code không cần dùng lại
[x] App Authenticator trở thành thiết bị xác thực cho root account
```

---

## Bước 2.2 - Vô hiệu hóa root access key

### 1. Mục tiêu của bước này

Mục tiêu là kiểm tra root account có access key hay không. Nếu có access key đang `Active`, cần chuyển nó sang `Inactive`.

Sau bước này, root account vẫn dùng được để đăng nhập Console khi thật sự cần, nhưng không còn access key active để gọi AWS API bằng quyền root.

### 2. Vì sao cần làm bước này

Access key là cặp thông tin dùng cho truy cập AWS bằng chương trình:

```text
AWS CLI
Terraform
SDK
Script tự động
```

Root access key rất nguy hiểm vì nó có quyền cao nhất trong AWS account. Nếu key bị lộ, người khác có thể thao tác tài nguyên, tạo chi phí hoặc thay đổi cấu hình bảo mật.

Nguyên tắc cho bài lab:

```text
Không dùng root access key
Không tạo access key cho root
Nếu root access key đang active thì deactivate trước
```

Sau này nếu cần chạy AWS CLI hoặc Terraform, ta sẽ tạo IAM user hoặc IAM role riêng cho lab. Không dùng root access key cho công việc hằng ngày.

Nếu bỏ qua bước này:

- Root account vẫn có credential mạnh để gọi AWS API.
- Nếu access key bị lộ, MFA không bảo vệ được các lệnh API dùng access key.
- Việc học Terraform/AWS CLI dễ đi sai hướng vì dùng nhầm quyền root.

### 3. Trước khi bắt đầu cần có gì

- Đã bật MFA cho root account ở bước 2.1.
- Đang đăng nhập AWS Console bằng root account.
- Đang ở trang **IAM -> Security credentials** hoặc có thể vào được trang này.

### 4. Thao tác chi tiết trên AWS Console

Làm lần lượt:

| Bước nhỏ | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Vào **IAM**. | Mở được trang Identity and Access Management. |
| 2 | Vào **Security credentials**. | Thấy trang **My security credentials** của root user. |
| 3 | Kéo xuống phần **Access keys**. | Thấy danh sách access key nếu root từng tạo key. |
| 4 | Nếu thấy `Access keys (1)` và dòng key có `Status: Active`, bấm vòng tròn chọn dòng key đó. | Dòng access key được chọn. |
| 5 | Bấm nút **Actions** ở bên phải khu vực **Access keys**. | Menu thao tác mở ra. |
| 6 | Chọn **Deactivate**. | AWS hỏi xác nhận vô hiệu hóa key. |
| 7 | Xác nhận thao tác. | Trạng thái key chuyển từ `Active` sang `Inactive`. |

Nếu không thấy access key nào trong phần **Access keys**, nghĩa là root account hiện không có access key. Khi đó bước này coi như đã đạt.

```text
IAM
  -> Security credentials
  -> Access keys
  -> Chọn access key đang Active
  -> Actions
  -> Deactivate
  -> Confirm
```

### 5. File/config/lệnh liên quan

Không cần tạo file, sửa config hoặc chạy lệnh ở bước này.

Đây là bước bảo mật thao tác trực tiếp trên AWS Console.

Sau này khi cần AWS CLI/Terraform, không dùng root access key. Ta sẽ tạo credential riêng cho IAM user lab.

### 6. Giải thích từng phần quan trọng

| Phần trên màn hình | Ý nghĩa | Bạn cần làm gì |
|---|---|---|
| `Access keys` | Khu vực quản lý access key dùng cho AWS CLI, SDK, Terraform hoặc script. | Kiểm tra có key nào đang active không. |
| `Access key ID` | Mã định danh của access key. Đây chưa phải secret, nhưng vẫn là thông tin nhạy cảm. | Không cần copy. Chỉ dùng để nhận diện dòng key. |
| `Status: Active` | Key đang hoạt động và có thể gọi AWS API. | Cần chuyển sang `Inactive` nếu đây là root access key. |
| `Status: Inactive` | Key đã bị vô hiệu hóa. Các chương trình dùng key này sẽ không gọi AWS API được nữa. | Đây là trạng thái mong muốn cho root access key. |
| Vòng tròn chọn dòng | Dùng để chọn access key trước khi thao tác. | Bấm vào vòng tròn để nút **Actions** sáng lên. |
| `Actions` | Menu thao tác với access key đã chọn. | Bấm sau khi đã chọn dòng key. |
| `Deactivate` | Tạm vô hiệu hóa access key. Có thể bật lại nếu thật sự cần. | Chọn mục này trước, an toàn hơn `Delete` khi chưa chắc key có đang được dùng ở đâu không. |
| `Delete` | Xóa hẳn access key, không khôi phục được. | Chỉ dùng sau khi chắc chắn không còn hệ thống nào cần key này. |

So sánh nhanh:

| Lựa chọn | Khi nào dùng | Có khôi phục được không? |
|---|---|---|
| `Deactivate` | Dùng ngay bây giờ để chặn root access key. | Có thể bật lại nếu cần. |
| `Delete` | Dùng sau khi chắc chắn key không còn dùng ở đâu. | Không khôi phục được. |

### 7. Kiểm tra hoàn thành

Bước này hoàn thành khi phần **Access keys** không còn key root nào ở trạng thái `Active`.

Trạng thái mong muốn:

```text
Access keys (0)
```

hoặc:

```text
Access keys (1)
Status: Inactive
```

Nếu vẫn thấy:

```text
Status: Active
```

thì bước này chưa xong.

### 8. Lỗi thường gặp và cách xử lý

| Lỗi | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| Nút **Actions** bị mờ | Chưa chọn dòng access key. | Bấm vòng tròn nhỏ bên trái dòng access key. |
| Không thấy nút **Deactivate** | Chưa chọn đúng dòng, hoặc menu đang bị che do giao diện. | Chọn lại dòng key, thử thu nhỏ zoom trình duyệt xuống 80-90%. |
| Không chắc key đang dùng ở đâu | Key có thể từng dùng cho AWS CLI, Terraform hoặc script cũ. | Chỉ `Deactivate` trước, chưa `Delete`. Nếu vài ngày không có gì lỗi thì quay lại xóa sau. |
| Sau khi deactivate thì script/AWS CLI lỗi | Script đó đang dùng root access key. | Không bật lại root key vội. Tạo IAM user lab riêng rồi cấu hình lại script/AWS CLI bằng user đó. |

### 9. Kết quả sau bước này

Trạng thái hệ thống sau khi hoàn thành:

```text
[x] Root account đã có MFA
[x] Root access key không còn active
[x] Root account chỉ dùng cho việc quản trị đặc biệt
[x] AWS CLI/Terraform sau này sẽ dùng IAM user hoặc IAM role riêng
```

---

## Bước 2.3 - Tạo IAM group và IAM user lab

### 1. Mục tiêu của bước này

Mục tiêu là tạo một IAM user riêng để dùng cho công việc thực hành hằng ngày, thay vì tiếp tục dùng root account.

Sau bước này, ta có:

```text
IAM group: LabAdmin
IAM user : tony-lab-admin
Quyền    : AdministratorAccess trong giai đoạn học
Mục đích : đăng nhập Console và thao tác lab hằng ngày
```

Root account từ đây chỉ dùng cho việc đặc biệt như billing, khôi phục account hoặc thao tác bảo mật cấp cao.

### 2. Vì sao cần làm bước này

Root account có quyền quá mạnh. Dùng root cho công việc hằng ngày là thói quen không tốt vì:

- Khó kiểm soát ai làm gì.
- Nếu lộ thông tin đăng nhập thì ảnh hưởng toàn bộ account.
- Dễ tạo nhầm access key root.
- Không giống cách vận hành thực tế.

IAM user giúp tách người dùng thực hành khỏi root account.

Trong bài lab một account, ta chưa dùng AWS Organizations và IAM Identity Center để giữ free credit. Vì vậy IAM user/group là cách đơn giản nhất để có tài khoản thao tác riêng.

Nếu bỏ qua bước này:

- Bạn sẽ tiếp tục dùng root account cho mọi thao tác.
- Các bước sau như AWS CLI, Terraform, ECR, EKS dễ bị cấu hình bằng quyền root.
- Khó chuyển sang mô hình quyền chuẩn hơn sau này.

### 3. Trước khi bắt đầu cần có gì

- Root account đã bật MFA.
- Root access key đã được chuyển sang `Inactive` hoặc không tồn tại.
- Đang đăng nhập AWS Console.
- Đang dùng region lab chính, ví dụ `Asia Pacific (Singapore) ap-southeast-1`.

Lưu ý: IAM là dịch vụ global, nhưng vẫn nên giữ region Console nhất quán để tránh nhầm khi chuyển sang các dịch vụ khác.

### 4. Thao tác chi tiết trên AWS Console

#### 4.1. Tạo IAM group `LabAdmin`

| Bước nhỏ | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Trên ô search của AWS Console, gõ `IAM`. | Thấy dịch vụ IAM. |
| 2 | Bấm vào **IAM**. | Mở trang Identity and Access Management. |
| 3 | Ở menu trái, bấm **User groups**. | Mở danh sách IAM user groups. |
| 4 | Bấm **Create group**. | Mở màn hình tạo group. |
| 5 | Ở ô **User group name**, nhập `LabAdmin`. | Group có tên rõ ràng cho lab admin. |
| 6 | Ở phần **Attach permissions policies**, tìm `AdministratorAccess`. | Thấy policy AWS managed `AdministratorAccess`. |
| 7 | Tick chọn `AdministratorAccess`. | Group sẽ có quyền admin trong account lab. |
| 8 | Bấm **Create group**. | Tạo xong group `LabAdmin`. |

Tóm tắt:

```text
IAM
  -> User groups
  -> Create group
  -> User group name: LabAdmin
  -> Attach permissions policies: AdministratorAccess
  -> Create group
```

#### 4.2. Tạo IAM user `tony-lab-admin`

| Bước nhỏ | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Trong IAM, ở menu trái bấm **Users**. | Mở danh sách IAM users. |
| 2 | Bấm **Create user**. | Mở màn hình tạo user. |
| 3 | Ở ô **User name**, nhập `tony-lab-admin`. | User có tên riêng cho thực hành. |
| 4 | Tick chọn **Provide user access to the AWS Management Console** nếu AWS hiển thị lựa chọn này. | User có thể đăng nhập AWS Console. |
| 5 | Nếu AWS hỏi loại user, chọn mục dành cho IAM user hoặc custom password, không chọn Identity Center. | Giữ đúng mô hình lab một account. |
| 6 | Chọn **Autogenerated password** hoặc **Custom password**. | Có mật khẩu ban đầu cho user. |
| 7 | Tick **Users must create a new password at next sign-in** nếu muốn đổi mật khẩu lần đầu. | Bảo mật hơn. |
| 8 | Bấm **Next**. | Sang bước gán quyền. |

Tóm tắt:

```text
IAM
  -> Users
  -> Create user
  -> User name: tony-lab-admin
  -> Provide user access to the AWS Management Console
  -> Chọn password
  -> Next
```

#### 4.3. Gắn user vào group `LabAdmin`

| Bước nhỏ | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Ở bước **Set permissions**, chọn **Add user to group**. | AWS hiển thị danh sách group. |
| 2 | Tick group `LabAdmin`. | User sẽ nhận quyền từ group. |
| 3 | Bấm **Next**. | Sang trang review. |
| 4 | Kiểm tra lại user name và group. | Thấy `tony-lab-admin` thuộc `LabAdmin`. |
| 5 | Bấm **Create user**. | Tạo xong IAM user lab. |

Tóm tắt:

```text
Set permissions
  -> Add user to group
  -> Chọn LabAdmin
  -> Next
  -> Create user
```

#### 4.4. Lưu thông tin đăng nhập Console

Sau khi tạo user, AWS thường hiển thị:

- Console sign-in URL.
- User name.
- Password tạm thời nếu chọn autogenerated password.

Cần lưu lại để đăng nhập lần đầu.

Không tạo access key ở bước này. Access key chỉ tạo sau khi cần AWS CLI/Terraform, và sẽ tạo có chủ đích.

### 5. File/config/lệnh liên quan

Chưa cần tạo file hoặc chạy lệnh ở bước này.

Tài nguyên được tạo trên AWS:

| Loại | Tên | Mục đích |
|---|---|---|
| IAM group | `LabAdmin` | Gom quyền admin lab vào một group. |
| IAM policy | `AdministratorAccess` | Quyền admin tạm dùng trong giai đoạn học. |
| IAM user | `tony-lab-admin` | User dùng đăng nhập Console hằng ngày. |

Sau này khi cấu hình AWS CLI, có thể tạo access key cho user này hoặc dùng phương án an toàn hơn nếu phù hợp.

### 6. Giải thích từng phần quan trọng

| Phần trên màn hình | Ý nghĩa | Bạn cần làm gì |
|---|---|---|
| `User groups` | Nơi quản lý nhóm IAM user. User trong cùng group sẽ nhận cùng bộ quyền. | Tạo group `LabAdmin` trước để quản lý quyền gọn hơn. |
| `Create group` | Tạo IAM group mới. | Dùng để tạo group `LabAdmin`. |
| `AdministratorAccess` | AWS managed policy có toàn quyền trong account. | Dùng tạm cho lab để tránh vướng quyền khi học. Không phải cấu hình production. |
| `Users` | Nơi quản lý IAM user. | Tạo user `tony-lab-admin`. |
| `Provide user access to the AWS Management Console` | Cho phép user đăng nhập AWS Console bằng username/password. | Tick chọn để user lab đăng nhập được giao diện AWS. |
| `Autogenerated password` | AWS tự sinh mật khẩu ban đầu. | Dùng được, nhớ lưu lại sau khi tạo. |
| `Custom password` | Tự đặt mật khẩu ban đầu. | Dùng nếu muốn dễ kiểm soát trong lab. |
| `Users must create a new password at next sign-in` | Bắt user đổi mật khẩu lần đầu đăng nhập. | Nên bật nếu dùng password tạm. |
| `Add user to group` | Gắn user vào group để nhận quyền. | Chọn group `LabAdmin`. |

#### `AdministratorAccess` khác gì với root user?

Trong bài lab, user `tony-lab-admin` được gắn policy `AdministratorAccess` thông qua group `LabAdmin`. Quyền này rất mạnh, nhưng **không giống hoàn toàn root user**.

| Tiêu chí | Root user | IAM user có `AdministratorAccess` |
|---|---|---|
| Cách đăng nhập | Đăng nhập bằng email root của AWS account. | Đăng nhập bằng account sign-in URL, account ID hoặc alias + user name. |
| Mức quyền với tài nguyên AWS | Toàn quyền. | Gần như toàn quyền với hầu hết tài nguyên AWS trong account. |
| Quản lý billing/account gốc | Có quyền gốc với billing, account setting, đóng account, đổi thông tin account quan trọng. | Có thể bị giới hạn ở một số thao tác account/billing đặc biệt, tùy cấu hình quyền và tính năng AWS. |
| Có thể bị giới hạn bởi IAM policy không? | Không theo cách thông thường. Root là danh tính gốc của account. | Có. Quyền đến từ policy, có thể gỡ, giảm hoặc thay đổi. |
| Có nên dùng hằng ngày không? | Không. Chỉ dùng khi thật sự cần. | Có thể dùng cho lab, nhưng production nên giảm quyền theo nguyên tắc least privilege. |
| Nếu lộ thông tin đăng nhập | Rất nguy hiểm, ảnh hưởng toàn bộ account. | Nguy hiểm, nhưng có thể khóa/xóa user hoặc gỡ policy nhanh hơn. |
| Access key | Tuyệt đối không nên dùng root access key. | Có thể tạo access key cho CLI/Terraform nếu cần, nhưng phải quản lý cẩn thận. |

Hiểu ngắn gọn:

```text
Root user = chủ sở hữu gốc của AWS account
AdministratorAccess = quyền admin được cấp qua IAM policy
```

Vì vậy ta làm theo nguyên tắc:

```text
Root user:
  - Bật MFA
  - Không dùng hằng ngày
  - Không giữ access key active

IAM user LabAdmin:
  - Dùng cho thực hành hằng ngày
  - Có thể gỡ quyền hoặc xóa khi không cần
  - Sau này có thể giảm quyền khi hệ thống ổn định hơn
```

### 7. Kiểm tra hoàn thành

Bước này hoàn thành khi:

```text
[x] Có IAM group LabAdmin
[x] Group LabAdmin có policy AdministratorAccess
[x] Có IAM user tony-lab-admin
[x] User tony-lab-admin thuộc group LabAdmin
[x] Có Console sign-in URL để đăng nhập bằng IAM user
```

Kiểm tra trên Console:

```text
IAM
  -> User groups
  -> LabAdmin
  -> Permissions: AdministratorAccess
```

và:

```text
IAM
  -> Users
  -> tony-lab-admin
  -> Groups: LabAdmin
```

Sau đó mở Console sign-in URL ở tab ẩn danh và thử đăng nhập bằng user `tony-lab-admin`.

### 8. Lỗi thường gặp và cách xử lý

| Lỗi | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| Không thấy lựa chọn Console access | AWS có thể đang gợi ý dùng Identity Center. | Chọn hướng tạo IAM user có Console access, không bật Identity Center trong bài lab này. |
| Không thấy policy `AdministratorAccess` | Gõ sai tên hoặc bộ lọc policy đang giới hạn. | Tìm chính xác `AdministratorAccess`, loại policy là AWS managed. |
| Đăng nhập user mới bị bắt đổi mật khẩu | Bạn đã bật tùy chọn đổi mật khẩu lần đầu. | Đổi mật khẩu mới và lưu lại cẩn thận. |
| Đăng nhập user mới không thấy quyền tạo tài nguyên | User chưa được gắn group hoặc group chưa có policy. | Kiểm tra user thuộc `LabAdmin` và group có `AdministratorAccess`. |
| Nhầm với root login | Root dùng email. IAM user dùng account sign-in URL, account ID hoặc account alias + user name. | Dùng đúng Console sign-in URL của IAM user. |

### 9. Kết quả sau bước này

Trạng thái hệ thống sau khi hoàn thành:

```text
[x] Root account đã được bảo vệ và không dùng hằng ngày
[x] Có IAM group LabAdmin
[x] Có IAM user lab để đăng nhập Console
[x] Quyền lab được quản lý qua group
[x] Sẵn sàng bật CloudTrail và tạo budget alert ở các bước tiếp theo
```

Bước tiếp theo là **Bước 2.4 - Bật CloudTrail audit trong account lab**.

