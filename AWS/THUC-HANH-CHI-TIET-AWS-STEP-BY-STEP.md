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

---

## Bước 2.4 - Bật CloudTrail audit trong account lab

### 1. Mục tiêu của bước này

Mục tiêu là bật **AWS CloudTrail** để ghi lại các hành động quan trọng xảy ra trong AWS account lab.

Sau bước này, ta có một trail ghi log quản trị như:

```text
Ai đăng nhập
Ai tạo/sửa/xóa IAM user, role, policy
Ai tạo/sửa/xóa VPC, EC2, EKS, RDS, S3, ECR
Ai gọi API quan trọng qua Console, AWS CLI, Terraform hoặc SDK
```

CloudTrail không ngăn hành động xảy ra, nhưng giúp truy vết sau khi có thay đổi hoặc sự cố.

### 2. Vì sao cần làm bước này

Ở các bước trước, ta đã:

```text
[x] Bật MFA cho root account
[x] Vô hiệu hóa root access key
[x] Tạo IAM group LabAdmin
[x] Tạo IAM user tony-lab-admin
```

Nhưng nếu chưa bật audit, khi có ai đó tạo tài nguyên sai, xóa nhầm resource hoặc dùng credential không đúng, ta rất khó trả lời các câu hỏi:

```text
Ai đã làm?
Làm lúc nào?
Làm từ IP nào?
Dùng Console hay AWS CLI?
Gọi API gì?
Thành công hay bị từ chối?
```

CloudTrail là nền tảng audit bắt buộc trước khi đi tiếp sang Terraform, GitLab CI, ECR, EKS và Argo CD.

Nếu bỏ qua bước này:

- Không có lịch sử đầy đủ để điều tra thay đổi quan trọng.
- Khó phát hiện việc dùng root account hoặc access key không đúng.
- Khi Terraform hoặc CI tạo tài nguyên, khó phân biệt thao tác của người dùng và thao tác tự động.
- Không đạt cổng nghiệm thu nền tảng dùng chung trong tài liệu `THUC-HANH-GITLAB-CI-ARGOCD-AWS-EKS.md`.

### 3. Trước khi bắt đầu cần có gì

Cần chuẩn bị:

- Đã đăng nhập AWS Console bằng IAM user lab, ví dụ `tony-lab-admin`.
- User lab thuộc group `LabAdmin` và có quyền tạo CloudTrail, S3 bucket, CloudWatch Logs nếu cần.
- Region lab chính đã được chọn, ví dụ:

```text
Asia Pacific (Singapore) ap-southeast-1
```

Lưu ý:

- CloudTrail là dịch vụ global theo account, nhưng trail có **home region**.
- Trong bài lab, chọn home region trùng với region lab chính để dễ quản lý.
- Chưa tạo staging hoặc production ở bước này.

### 4. Thao tác chi tiết trên AWS Console

#### 4.1. Mở dịch vụ CloudTrail

1. Đăng nhập AWS Console bằng IAM user lab.
2. Trên ô search ở đầu màn hình, gõ:

```text
CloudTrail
```

3. Bấm vào dịch vụ **CloudTrail**.
4. Kiểm tra region góc phải trên đang là region lab chính, ví dụ:

```text
ap-southeast-1
```

#### 4.2. Tạo trail mới

Trong CloudTrail:

1. Ở menu trái, chọn:

```text
Trails
```

2. Bấm:

```text
Create trail
```

3. Ở ô **Trail name**, nhập:

```text
lab-management-events-trail
```

Tên này có ý nghĩa:

- `lab`: trail dùng cho account lab.
- `management-events`: tập trung vào audit hành động quản trị.
- `trail`: nhận diện đây là CloudTrail trail.

#### 4.3. Chọn S3 bucket lưu log

Ở phần **Storage location**, chọn:

```text
Create new S3 bucket
```

Đặt tên bucket theo dạng duy nhất toàn cầu:

```text
newgate2601-cloudtrail-logs-<account-id>-ap-southeast-1
```

Ví dụ:

```text
newgate2601-cloudtrail-logs-150914615641-ap-southeast-1
```

Nếu tên bucket bị trùng, đổi prefix cho riêng bạn, ví dụ:

```text
tony-cloudtrail-logs-150914615641-ap-southeast-1
```

Không dùng tên quá chung như:

```text
cloudtrail-logs
aws-logs
my-bucket
```

vì S3 bucket name là duy nhất trên toàn AWS.

#### 4.4. Bật log file validation

Trong phần cấu hình trail, bật:

```text
Log file validation
```

Ý nghĩa:

- AWS tạo file digest để kiểm tra log có bị thay đổi sau khi ghi hay không.
- Đây là cấu hình tốt cho audit.
- Chi phí nhỏ, phù hợp bật ngay từ đầu.

#### 4.5. Chưa cần bật CloudWatch Logs ở bước đầu

AWS có thể hỏi có gửi log sang CloudWatch Logs hay không.

Trong bài lab giai đoạn đầu, có thể để:

```text
CloudWatch Logs: Disabled
```

Lý do:

- Mục tiêu bước này là có audit log lưu bền trong S3 trước.
- CloudWatch Logs hữu ích cho cảnh báo gần thời gian thực, nhưng có thể thêm ở bước observability/security sau.
- Bật quá nhiều ngay từ đầu dễ làm người học rối và có thể phát sinh chi phí log không cần thiết.

Nếu muốn theo dõi cảnh báo đăng nhập root hoặc API nguy hiểm ngay, có thể bật CloudWatch Logs sau bằng một bước riêng.

#### 4.6. Chọn loại event cần ghi

Ở phần **Choose log events**, chọn:

```text
Management events
```

Thiết lập:

```text
API activity: Read and Write
```

Không cần bật vội:

```text
Data events
Insights events
Network activity events
```

Giải thích nhanh:

- `Management events`: ghi các hành động quản trị tài nguyên AWS, phù hợp bật ngay.
- `Data events`: ghi chi tiết truy cập object S3 hoặc Lambda invoke, số lượng lớn hơn và có thể tăng chi phí.
- `Insights events`: phát hiện bất thường API, hữu ích sau này nhưng chưa bắt buộc cho bước đầu.
- `Network activity events`: chỉ cần khi có nhu cầu audit network activity chuyên sâu.

#### 4.7. Tạo trail

Kiểm tra lại cấu hình:

```text
Trail name        : lab-management-events-trail
Storage location  : S3 bucket riêng cho CloudTrail logs
Log file validation: Enabled
Management events : Read and Write
Multi-region trail: Enabled nếu AWS cho chọn
```

Sau đó bấm:

```text
Create trail
```

Nếu AWS có lựa chọn **Apply trail to all regions**, nên bật.

Ý nghĩa:

- Dù đang dùng region chính là Singapore, nếu sau này lỡ tạo resource ở region khác, CloudTrail vẫn ghi nhận management event.
- Giúp phát hiện thao tác nhầm region.

### 5. File/config/lệnh liên quan

Không cần tạo file trong repository ở bước này.

Tài nguyên AWS được tạo:

| Loại | Tên ví dụ | Mục đích |
|---|---|---|
| CloudTrail trail | `lab-management-events-trail` | Ghi audit management event trong account lab. |
| S3 bucket | `newgate2601-cloudtrail-logs-<account-id>-ap-southeast-1` | Lưu CloudTrail log file. |
| S3 bucket policy | Do AWS tự thêm | Cho phép CloudTrail ghi log vào bucket. |
| Log digest | Bật qua log file validation | Kiểm tra tính toàn vẹn của log file. |

Sau này khi chuyển sang Terraform, các tài nguyên này nên được đưa vào `platform-infrastructure` để quản lý bằng IaC. Ở bước hiện tại, thao tác Console giúp hiểu rõ CloudTrail trước.

### 6. Giải thích từng phần quan trọng

| Phần trên màn hình | Ý nghĩa | Bạn cần làm gì |
|---|---|---|
| `Trail name` | Tên trail dùng để nhận diện cấu hình audit. | Đặt tên rõ ràng, ví dụ `lab-management-events-trail`. |
| `Storage location` | Nơi lưu log CloudTrail. | Tạo S3 bucket riêng cho CloudTrail. |
| `Create new S3 bucket` | AWS tạo bucket mới và cấu hình policy cho CloudTrail ghi log. | Dùng lựa chọn này để giảm lỗi policy khi mới học. |
| `Log file validation` | Tạo digest để kiểm tra log có bị sửa không. | Bật. |
| `Management events` | Ghi lại thao tác quản trị trên AWS resources. | Bật `Read and Write`. |
| `Read events` | Các API đọc/xem thông tin như `DescribeInstances`, `ListBuckets`. | Bật để audit đầy đủ hơn trong lab. |
| `Write events` | Các API tạo/sửa/xóa như `CreateUser`, `RunInstances`, `DeleteBucket`. | Bắt buộc bật. |
| `Data events` | Ghi truy cập sâu vào object S3, Lambda invoke. | Chưa bật ở bước này để tránh nhiều log và chi phí. |
| `Multi-region trail` | Trail ghi management event ở tất cả region. | Nên bật nếu AWS cho chọn. |

#### Management event khác gì data event?

Hiểu đơn giản:

```text
Management event = ai quản trị tài nguyên AWS
Data event       = ai truy cập dữ liệu bên trong tài nguyên
```

Ví dụ:

| Hành động | Loại event |
|---|---|
| Tạo S3 bucket | Management event |
| Xóa IAM user | Management event |
| Tạo EKS cluster | Management event |
| Upload object vào S3 bucket | Data event |
| Download object từ S3 bucket | Data event |
| Invoke Lambda function | Data event |

Bước này chỉ cần management event vì ta đang dựng nền tảng account lab.

### 7. Kiểm tra hoàn thành

#### 7.1. Kiểm tra trail đã bật

Vào:

```text
CloudTrail
  -> Trails
  -> lab-management-events-trail
```

Kiểm tra thấy:

```text
Status: Logging
```

hoặc trạng thái tương đương cho biết trail đang ghi log.

#### 7.2. Kiểm tra Event history

Vào:

```text
CloudTrail
  -> Event history
```

Tìm thử các event gần đây, ví dụ:

```text
CreateTrail
PutBucketPolicy
CreateBucket
ConsoleLogin
```

CloudTrail Event history có thể trễ vài phút. Nếu chưa thấy ngay, chờ khoảng 5-15 phút rồi refresh.

#### 7.3. Kiểm tra log trong S3 bucket

Vào:

```text
S3
  -> Buckets
  -> <cloudtrail-log-bucket>
```

Tìm prefix có dạng:

```text
AWSLogs/<account-id>/CloudTrail/
AWSLogs/<account-id>/CloudTrail-Digest/
```

Nếu thấy file `.json.gz` trong `CloudTrail` và file digest trong `CloudTrail-Digest`, nghĩa là log đã được ghi xuống S3.

### 8. Lỗi thường gặp và cách xử lý

| Lỗi | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| Tên S3 bucket bị trùng | S3 bucket name là duy nhất toàn cầu. | Thêm account id, region hoặc prefix cá nhân vào tên bucket. |
| Không tạo được trail | IAM user thiếu quyền CloudTrail hoặc S3. | Kiểm tra user thuộc group `LabAdmin` và group có policy phù hợp. |
| Trail tạo xong nhưng chưa thấy event | CloudTrail có độ trễ ghi log. | Chờ 5-15 phút rồi refresh Event history và S3 bucket. |
| Không thấy log trong S3 | Bucket policy chưa cho CloudTrail ghi log hoặc chọn nhầm bucket. | Mở trail kiểm tra storage location, xem bucket policy do AWS tạo. |
| Tạo trail ở nhầm region | Console đang chọn region khác region lab chính. | Kiểm tra góc phải trên Console và bật multi-region trail nếu có. |
| Log quá nhiều hoặc lo chi phí | Bật nhầm data event cho S3/Lambda. | Tắt data event nếu chưa cần, chỉ giữ management event. |

### 9. Kết quả sau bước này

Trạng thái hệ thống sau khi hoàn thành:

```text
[x] Root account đã được bảo vệ bằng MFA
[x] Root access key không còn active
[x] Có IAM user lab dùng cho thao tác hằng ngày
[x] CloudTrail đã ghi management event
[x] CloudTrail log được lưu vào S3 bucket riêng
[x] Có thể truy vết hành động quan trọng trong account lab
```

Bước tiếp theo là **Bước 2.5 - Tạo budget/cost alert để bảo vệ free credit**.

---

## Bước 2.5 - Tạo budget/cost alert để bảo vệ free credit

### 1. Mục tiêu của bước này

Mục tiêu là tạo **AWS Budget** để cảnh báo khi chi phí trong account lab bắt đầu tăng.

Sau bước này, ta có một budget theo tháng, gửi email cảnh báo khi chi phí đạt các ngưỡng như:

```text
50% budget
80% budget
100% budget
```

Ví dụ nếu đặt budget là `20 USD/tháng`:

```text
10 USD  -> cảnh báo sớm
16 USD  -> cảnh báo cần kiểm tra ngay
20 USD  -> cảnh báo đã chạm ngân sách tháng
```

Budget không phải công cụ chặn chi phí tuyệt đối. Nó là hệ thống cảnh báo để ta biết sớm và dọn tài nguyên kịp thời.

### 2. Vì sao cần làm bước này

Trong bài lab AWS, nhiều dịch vụ có thể phát sinh chi phí nếu quên tắt hoặc cấu hình quá lớn:

- EC2 instance.
- NAT Gateway.
- Load Balancer.
- EKS cluster.
- RDS instance.
- MSK cluster.
- ElastiCache cluster.
- EBS volume và snapshot.
- CloudWatch log.
- Data transfer.

Free credit giúp thực hành dễ hơn, nhưng không thay thế được kiểm soát chi phí. Nếu không có budget alert, ta có thể chỉ phát hiện chi phí tăng sau nhiều ngày.

Budget/cost alert giúp:

- Biết sớm khi tài nguyên lab bắt đầu tốn tiền.
- Tránh dùng hết free credit quá nhanh.
- Phát hiện tài nguyên quên xóa.
- Tạo thói quen vận hành có kiểm soát trước khi dựng EKS, RDS, MSK và ElastiCache.

Nếu bỏ qua bước này:

- Dễ quên tài nguyên đang chạy.
- NAT Gateway, Load Balancer, EKS hoặc database có thể tạo chi phí liên tục.
- Không đạt điều kiện hoàn thành phần chuẩn bị AWS account lab trong tài liệu nền tảng.

### 3. Trước khi bắt đầu cần có gì

Cần chuẩn bị:

- Đăng nhập AWS Console bằng IAM user lab, ví dụ `tony-lab-admin`.
- User có quyền truy cập Billing/Budgets.
- Một email nhận cảnh báo chi phí.
- Đã bật CloudTrail ở bước 2.4.
- Biết mức ngân sách lab muốn đặt.

Gợi ý cho bài lab cá nhân:

| Mục đích | Budget gợi ý |
|---|---|
| Chỉ đang làm IAM, CloudTrail, S3 nhỏ | `5 USD/tháng` |
| Chuẩn bị Terraform, ECR, EC2 nhỏ | `10 USD/tháng` |
| Sắp dựng EKS/RDS/MSK lab | `20-50 USD/tháng` tùy free credit |

Nếu đang dùng free credit, vẫn nên đặt budget thấp hơn tổng free credit. Ví dụ có `100 USD` credit thì không nên đặt budget tháng đầu là `100 USD`; nên đặt thấp hơn để có cảnh báo sớm.

### 4. Thao tác chi tiết trên AWS Console

#### 4.1. Mở Billing and Cost Management

1. Đăng nhập AWS Console bằng IAM user lab.
2. Nhìn góc phải trên cùng, bấm vào tên account.
3. Chọn:

```text
Billing and Cost Management
```

hoặc trên ô search gõ:

```text
Billing
```

Sau đó mở trang Billing.

#### 4.2. Mở AWS Budgets

Ở menu trái của Billing, chọn:

```text
Budgets
```

Nếu đây là lần đầu vào Billing bằng IAM user và bị báo thiếu quyền, cần bật quyền IAM access to billing ở root account hoặc kiểm tra policy của user. Phần lỗi này có hướng dẫn ở mục 8.

#### 4.3. Tạo budget mới

Trong trang Budgets:

1. Bấm:

```text
Create budget
```

2. Nếu AWS hỏi chọn cách tạo, chọn:

```text
Customize
```

hoặc:

```text
Use a template
```

Với bài lab, nếu AWS có template **Monthly cost budget**, có thể dùng template để nhanh hơn. Nếu muốn hiểu rõ từng phần, chọn **Customize**.

#### 4.4. Chọn loại budget

Chọn:

```text
Cost budget
```

Không chọn:

```text
Usage budget
Reservation budget
Savings Plans budget
```

Lý do:

- `Cost budget` theo dõi tiền phát sinh, phù hợp bảo vệ free credit.
- `Usage budget` theo dõi số lượng sử dụng của từng dịch vụ, dùng sau khi cần kiểm soát chi tiết.
- `Reservation` và `Savings Plans` không phù hợp bài lab hiện tại.

#### 4.5. Cấu hình budget

Điền thông tin:

```text
Budget name   : lab-monthly-cost-budget
Period        : Monthly
Budget renewal: Recurring budget
Budget amount : Fixed
Amount        : 20 USD
```

Nếu muốn thận trọng hơn, đặt:

```text
Amount: 10 USD
```

Nếu chuẩn bị dựng EKS/RDS/MSK và đã có free credit đủ, có thể đặt:

```text
Amount: 50 USD
```

Không nên đặt budget quá cao chỉ để khỏi nhận email. Mục tiêu của budget là buộc mình chú ý chi phí sớm.

#### 4.6. Chọn phạm vi chi phí

Với account lab một account, giữ phạm vi mặc định:

```text
All AWS services
All linked accounts
All regions
```

Nếu có lựa chọn cost type, nên để mặc định hoặc chọn theo hướng:

```text
Include tax: Enabled nếu AWS cho chọn
Include credits: Enabled hoặc giữ mặc định
Include refunds: Enabled hoặc giữ mặc định
Include upfront reservation fees: Enabled hoặc giữ mặc định
Include recurring reservation charges: Enabled hoặc giữ mặc định
```

Trong bài lab cá nhân, điều quan trọng nhất là budget bao phủ toàn account, không lọc riêng một service.

#### 4.7. Tạo alert threshold

Tạo ít nhất ba cảnh báo:

| Ngưỡng | Loại | Ý nghĩa |
|---|---|---|
| `50%` | Actual cost | Cảnh báo sớm khi chi phí đã dùng một nửa budget. |
| `80%` | Actual cost | Cảnh báo cần kiểm tra và dọn tài nguyên ngay. |
| `100%` | Forecasted cost | Cảnh báo AWS dự đoán cuối tháng sẽ chạm hoặc vượt budget. |

Nếu AWS cho thêm nhiều alert, có thể dùng cấu hình:

```text
Alert 1: Actual cost >= 50%
Alert 2: Actual cost >= 80%
Alert 3: Actual cost >= 100%
Alert 4: Forecasted cost >= 100%
```

Giải thích:

- `Actual cost` là chi phí đã phát sinh thật tới thời điểm hiện tại.
- `Forecasted cost` là chi phí AWS dự đoán tới cuối kỳ nếu tốc độ sử dụng hiện tại tiếp tục.

#### 4.8. Nhập email nhận cảnh báo

Ở phần notification, nhập email nhận cảnh báo, ví dụ:

```text
your-email@example.com
```

Nếu có nhiều người cùng làm lab, có thể thêm nhiều email.

Lưu ý:

- Nhập đúng email đang dùng thường xuyên.
- Kiểm tra cả inbox, spam và promotions.
- Một số loại notification có thể yêu cầu xác nhận subscription nếu dùng Amazon SNS. Nếu chỉ nhập email trực tiếp trong AWS Budgets, thường không cần thao tác SNS riêng.

#### 4.9. Review và tạo budget

Kiểm tra lại:

```text
Budget name : lab-monthly-cost-budget
Period      : Monthly
Amount      : 10/20/50 USD tùy lab
Scope       : All AWS services, all regions
Alerts      : 50%, 80%, 100% hoặc forecasted 100%
Email       : email nhận cảnh báo
```

Sau đó bấm:

```text
Create budget
```

### 5. File/config/lệnh liên quan

Không cần tạo file trong repository ở bước này.

Tài nguyên/cấu hình AWS liên quan:

| Loại | Tên ví dụ | Mục đích |
|---|---|---|
| AWS Budget | `lab-monthly-cost-budget` | Theo dõi chi phí tháng của account lab. |
| Alert threshold | `50%`, `80%`, `100%` | Gửi cảnh báo theo mức chi phí. |
| Email recipient | Email cá nhân hoặc team | Nhận thông báo khi vượt ngưỡng. |
| Billing dashboard | Cost Management Console | Kiểm tra chi phí phát sinh. |

Sau này khi hệ thống trưởng thành hơn, có thể bổ sung:

- Cost Anomaly Detection.
- Budget theo từng service như EKS, RDS, MSK.
- Tag-based cost allocation.
- Budget action để tự động chặn một số quyền khi vượt ngưỡng.

Trong bước này, chỉ cần budget cảnh báo tổng chi phí toàn account.

### 6. Giải thích từng phần quan trọng

| Phần trên màn hình | Ý nghĩa | Bạn cần làm gì |
|---|---|---|
| `Cost budget` | Budget theo tiền phát sinh. | Chọn loại này để bảo vệ free credit. |
| `Budget name` | Tên budget. | Đặt `lab-monthly-cost-budget`. |
| `Period: Monthly` | Tính chi phí theo từng tháng. | Chọn monthly cho lab. |
| `Recurring budget` | Budget tự lặp lại mỗi tháng. | Bật để không phải tạo lại hằng tháng. |
| `Fixed budget` | Mức tiền cố định. | Dùng cho lab vì dễ hiểu. |
| `Actual cost` | Chi phí đã phát sinh thật. | Dùng cho alert 50% và 80%. |
| `Forecasted cost` | Chi phí AWS dự đoán tới cuối kỳ. | Dùng cho cảnh báo 100% sớm hơn. |
| `Alert threshold` | Ngưỡng kích hoạt cảnh báo. | Tạo nhiều ngưỡng để biết sớm. |
| `Email recipients` | Danh sách email nhận cảnh báo. | Nhập email thật sự đọc. |

#### Budget có tự tắt tài nguyên không?

Không.

Budget mặc định chỉ cảnh báo:

```text
Chi phí vượt ngưỡng
  -> AWS gửi email
  -> Người vận hành kiểm tra
  -> Tắt/xóa/giảm tài nguyên nếu cần
```

Vì vậy sau khi nhận email cảnh báo, cần chủ động kiểm tra:

```text
Billing
  -> Cost Explorer
  -> xem service nào đang tốn tiền
```

và:

```text
EC2
RDS
EKS
MSK
ElastiCache
Load Balancer
NAT Gateway
EBS Snapshot
CloudWatch Logs
```

### 7. Kiểm tra hoàn thành

Bước này hoàn thành khi:

```text
[x] Có budget tên lab-monthly-cost-budget
[x] Budget theo chu kỳ Monthly
[x] Budget bao phủ toàn account hoặc toàn bộ AWS services
[x] Có ít nhất 3 alert threshold
[x] Có email nhận cảnh báo
```

Kiểm tra trên Console:

```text
Billing and Cost Management
  -> Budgets
  -> lab-monthly-cost-budget
```

Trong trang chi tiết budget, kiểm tra:

```text
Budgeted amount
Current actual spend
Forecasted spend
Alert thresholds
Email recipients
```

Nếu vừa tạo budget, có thể chưa thấy số chi phí ngay. AWS cost data có thể trễ nhiều giờ.

### 8. Lỗi thường gặp và cách xử lý

| Lỗi | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| IAM user không vào được Billing | Root account chưa bật IAM access to billing hoặc user thiếu quyền. | Đăng nhập root, vào Account/Billing setting và bật IAM user access to billing, sau đó kiểm tra policy user. |
| Không thấy menu Budgets | Đang ở trang Console khác hoặc giao diện Billing thay đổi. | Search `Budgets` trên thanh tìm kiếm AWS Console. |
| Không nhận được email | Nhập sai email, email vào spam hoặc notification chưa được xác nhận. | Kiểm tra lại email, spam, promotions; nếu dùng SNS thì xác nhận subscription. |
| Budget tạo xong nhưng chi phí vẫn là 0 | Cost data chưa cập nhật hoặc chưa phát sinh chi phí. | Chờ vài giờ đến 24 giờ rồi kiểm tra lại. |
| Alert gửi trễ | AWS Budgets không phải hệ thống realtime từng giây. | Dùng alert sớm 50% và 80%; sau này thêm Cost Anomaly Detection nếu cần. |
| Vượt budget nhưng tài nguyên vẫn chạy | Budget mặc định chỉ cảnh báo, không tự tắt tài nguyên. | Vào Cost Explorer tìm service tốn tiền và tự dọn tài nguyên. |
| Free credit còn nhưng vẫn thấy cost | AWS vẫn ghi nhận usage/cost, credit có thể được áp dụng ở billing. | Kiểm tra phần Credits và Bills để hiểu credit được trừ như thế nào. |

### 9. Kết quả sau bước này

Trạng thái hệ thống sau khi hoàn thành:

```text
[x] Root account đã có MFA
[x] Root access key không còn active
[x] Có IAM user lab để thao tác hằng ngày
[x] CloudTrail audit đã bật
[x] Có budget/cost alert bảo vệ free credit
[x] Account lab sẵn sàng đi tiếp sang Terraform remote state
```

Bước tiếp theo là **Bước 3.1 - Viết Terraform bootstrap tạo remote state backend**.

---

## Bước 3.1 - Bootstrap Terraform remote state backend

### 1. Mục tiêu của bước này

Mục tiêu là tạo nơi lưu Terraform state dùng chung cho các bước hạ tầng sau.

Sau bước này ta có:

```text
S3 bucket  : lưu terraform.tfstate
DynamoDB   : lock state khi plan/apply
KMS key    : mã hóa state
Backend S3 : bootstrap state đã được migrate lên S3
```

Phần giải thích chi tiết từng file Terraform đã nằm trong `terraform/README.md`. Ở đây chỉ ghi luồng thực hành cần làm.

### 2. Trước khi bắt đầu cần có gì

Cần chuẩn bị:

- Đã đăng nhập AWS bằng IAM user lab, không dùng root access key.
- AWS CLI chạy được lệnh `aws sts get-caller-identity`.
- Terraform CLI chạy được lệnh `terraform version`.
- Đang đứng ở repository local `springboot-learning`.
- Đã có thư mục `terraform/bootstrap/backend` trong repo.

Kiểm tra nhanh:

```powershell
aws sts get-caller-identity
terraform version
```

### 3. File Terraform đã chuẩn bị

Các file chính nằm ở:

```text
terraform/bootstrap/backend
```

Gồm:

| File | Vai trò ngắn gọn |
|---|---|
| `versions.tf` | Khai báo Terraform và AWS provider version. |
| `providers.tf` | Cấu hình AWS provider và default tags. |
| `variables.tf` | Khai báo input cần truyền vào. |
| `locals.tf` | Gom tag chung thành `local.common_tags`. |
| `main.tf` | Tạo S3, DynamoDB, KMS và policy cần thiết. |
| `outputs.tf` | In ra bucket, table và KMS key ARN. |
| `terraform.tfvars.example` | File mẫu để tạo input thật. |
| `remote-state/backend.tf` | Backend S3 dùng sau khi bootstrap resource đã tồn tại. |

Không cần copy lại toàn bộ nội dung các file vào tài liệu này. Khi cần đọc chi tiết từng keyword, xem `terraform/README.md`.

### 4. Tạo file input thật

Đi tới thư mục bootstrap:

```powershell
cd C:\code\springboot-learning\terraform\bootstrap\backend
```

Copy file mẫu:

```powershell
Copy-Item terraform.tfvars.example terraform.tfvars
```

Mở `terraform.tfvars` và chỉnh theo account thật.

Ví dụ lab hiện tại:

```hcl
aws_region        = "ap-southeast-1"
project           = "newgate2601"
environment       = "bootstrap"
account_id        = "150914615641"
state_bucket_name = "newgate2601-terraform-state-150914615641-ap-southeast-1"
lock_table_name   = "terraform-state-lock"
owner             = "tony"
```

Ghi nhớ: `locals.tf` không thay thế cho `terraform.tfvars`. `locals.common_tags` chỉ dùng lại các giá trị đã được truyền qua `var.*`.

### 5. Chạy bootstrap lần đầu bằng local state

Lần đầu S3 backend chưa tồn tại, nên Terraform chưa thể lưu state lên S3 ngay. Vì vậy bootstrap chạy theo 2 pha:

```text
Pha 1: dùng local state tạm thời để tạo S3, DynamoDB, KMS
Pha 2: copy local state đó lên S3 backend vừa tạo
```

Điểm quan trọng: đây là ngoại lệ chỉ dành cho chính module bootstrap. Các root module sau như `dev/network`, `dev/eks`, `shared-services/ecr` sẽ dùng remote state ngay từ đầu.

Trạng thái thư mục đúng trước lần chạy đầu:

```text
terraform/bootstrap/backend/
  versions.tf
  providers.tf
  variables.tf
  locals.tf
  main.tf
  outputs.tf
  terraform.tfvars.example
  remote-state/backend.tf
```

Chưa nên có:

```text
backend.tf
.terraform/
terraform.tfvars
terraform.tfstate
tfplan
```

Nếu đang thực hành lại từ đầu trên cùng máy, có thể xóa các file/thư mục sinh ra từ lần chạy trước:

```powershell
Remove-Item .terraform -Recurse -Force
Remove-Item terraform.tfvars -Force
Remove-Item tfplan -Force
```

Lưu ý: các lệnh trên chỉ reset phần local. Nếu lần trước đã `terraform apply` thành công thì S3 bucket, DynamoDB table và KMS key vẫn còn trong AWS. Khi đó muốn apply lại từ số 0 thật sự, cần một trong hai cách:

```text
Cách 1: destroy tài nguyên AWS cũ trước rồi thực hành lại với cùng tên
Cách 2: đổi project/state_bucket_name để tạo bộ backend mới
```

Không nên xóa resource AWS thủ công trên Console khi state vẫn còn, vì Terraform sẽ mất dấu tài nguyên đang quản lý.

#### 5.1. `terraform init`

Chạy:

```powershell
terraform init
```

Lệnh này kết hợp các thành phần sau:

| Thành phần | Terraform dùng để làm gì |
|---|---|
| `versions.tf` | Đọc `required_version` và `required_providers`. |
| Terraform Registry | Tải AWS provider `hashicorp/aws` đúng version cho phép. |
| `.terraform/` | Lưu provider plugin đã tải về local working directory. |
| `.terraform.lock.hcl` | Ghi lại provider version và checksum thực tế đã chọn. |
| `backend.tf` nếu có | Cấu hình nơi lưu state. Lần đầu bootstrap chưa nên có file này. |

Giải thích dễ hiểu về `.terraform/`:

Khi ta viết:

```hcl
provider "aws" {
  region = var.aws_region
}
```

Terraform chưa tự biết cách gọi AWS. Nó cần một chương trình hỗ trợ tên là **AWS provider**.

`terraform init` sẽ tải chương trình AWS provider đó về máy và đặt trong thư mục:

```text
.terraform/
```

Nói đơn giản:

```text
File .tf của mình
  -> nói muốn tạo S3, DynamoDB, KMS

AWS provider trong .terraform/
  -> biết cách gọi AWS API để tạo S3, DynamoDB, KMS thật
```

Vì vậy `.terraform/` dùng để chứa những thứ Terraform cần để chạy được trong thư mục hiện tại. Nếu chưa có `.terraform/`, Terraform chưa có provider local để làm việc với AWS.

Ví dụ sau `terraform init`, bên trong có thể có dạng:

```text
.terraform/
  providers/
    registry.terraform.io/
      hashicorp/
        aws/
          5.100.0/
            windows_amd64/
              terraform-provider-aws...
```

Ý nghĩa:

```text
hashicorp/aws -> provider AWS
5.100.0       -> version provider
windows_amd64 -> bản chạy trên Windows 64-bit
```

Ghi nhớ về `.terraform/`:

- Nó nằm trên máy local.
- Nó không phải resource AWS.
- Nó không phải file state.
- Nó không cần commit lên Git.
- Nếu xóa `.terraform/`, chưa mất resource AWS nào.
- Sau khi xóa, chạy lại `terraform init` thì Terraform tải/cài lại provider.

Giải thích dễ hiểu về `.terraform.lock.hcl`:

Sau khi tải provider, Terraform tạo hoặc cập nhật file:

```text
.terraform.lock.hcl
```

File này trả lời câu hỏi:

```text
Lần này Terraform đã chọn đúng provider nào?
```

Ví dụ:

```text
hashicorp/aws version 5.100.0
```

Nếu không có file lock, máy khác hoặc CI có thể chọn provider version khác trong khoảng cho phép. Version khác có thể có thay đổi nhỏ làm output, plan hoặc behavior khác đi.

Vì vậy `.terraform.lock.hcl` giúp các lần chạy sau dùng lại cùng provider version đã chọn trước đó.

Ví dụ dễ hiểu:

```text
Lần đầu:
  versions.tf cho phép AWS provider 5.x
  terraform init chọn được 5.100.0
  .terraform.lock.hcl ghi lại: dùng 5.100.0

Lần sau:
  terraform init đọc .terraform.lock.hcl
  thấy đã khóa 5.100.0
  tiếp tục dùng 5.100.0
```

Trong `.terraform.lock.hcl` có nhiều dòng hash/checksum. Các giá trị này dùng để kiểm tra file provider tải về có đúng nội dung không.

Nói đơn giản:

```text
Version  -> đúng tên phiên bản chưa?
Hash     -> đúng nội dung file provider chưa?
```

Nếu ai đó thay đổi nội dung provider nhưng vẫn đặt tên version giống cũ, hash sẽ khác. Terraform sẽ phát hiện và không âm thầm dùng file đó.

Ghi nhớ về `.terraform.lock.hcl`:

- Nên commit file này lên Git.
- Nó giúp máy bạn, máy người khác và CI dùng cùng provider version.
- Nó không chứa password AWS.
- Nó không chứa danh sách resource AWS đã tạo.
- Nó không phải file state.

Điểm rất dễ nhầm:

```text
.terraform.lock.hcl
```

không phải là lock để chặn 2 người cùng chạy `terraform apply`.

Nó chỉ lock version provider.

Lock chống 2 người cùng sửa state là chuyện khác:

```text
DynamoDB table terraform-state-lock
```

Trong bài này, DynamoDB lock chỉ dùng sau khi đã migrate state lên S3 backend.

Nếu có 2 máy cùng chạy `terraform init`:

```text
Máy A:
  -> có thư mục .terraform/ riêng trên máy A
  -> tải/cài provider trên máy A

Máy B:
  -> có thư mục .terraform/ riêng trên máy B
  -> tải/cài provider trên máy B

Nếu .terraform.lock.hcl đã commit lên Git:
  -> cả hai máy cùng dùng provider version đã ghi trong file lock
```

Hai máy không tranh nhau ở bước `terraform init`, vì bước này chỉ chuẩn bị provider trên từng máy.

Nếu chạy lại `terraform init` khi `.terraform.lock.hcl` đã tồn tại:

```text
Nếu .terraform/ vẫn còn provider:
  -> Terraform dùng lại provider đã tải

Nếu .terraform/ đã bị xóa:
  -> Terraform đọc .terraform.lock.hcl
  -> tải lại đúng provider version đã khóa

Nếu muốn cập nhật provider lên bản mới:
  -> chạy terraform init -upgrade
  -> Terraform chọn version mới phù hợp
  -> .terraform.lock.hcl được cập nhật lại
```

Tác động của lệnh:

```text
Local machine
  -> tạo/cập nhật thư mục .terraform/
  -> tạo/cập nhật .terraform.lock.hcl
  -> chưa tạo resource AWS nào
  -> chưa gọi apply thay đổi hạ tầng
```

Giá trị tạo ra:

- Máy local biết phải dùng provider AWS nào.
- Terraform biết backend hiện tại là local hay S3.
- Provider version được khóa để lần sau chạy ổn định hơn.

Output thực tế trong lần chạy này:

```text
Initializing the backend...

Initializing provider plugins...
- Reusing previous version of hashicorp/aws from the dependency lock file
- Installing hashicorp/aws v5.100.0...
- Installed hashicorp/aws v5.100.0 (signed by HashiCorp)

Terraform has been successfully initialized!
```

Đọc output này như sau:

| Dòng output | Nó nói Terraform đang làm gì |
|---|---|
| `Initializing the backend...` | Terraform kiểm tra backend hiện tại. Lúc này chưa có `backend.tf`, nên backend là local. |
| `Reusing previous version ... from the dependency lock file` | Terraform đọc `.terraform.lock.hcl` để biết lần trước đã khóa provider AWS version nào. |
| `Installing hashicorp/aws v5.100.0...` | Terraform tải AWS provider plugin về máy. Provider này là cầu nối từ file `.tf` sang AWS API. |
| `Installed ... signed by HashiCorp` | Provider đã tải xong, và Terraform xác nhận gói này do HashiCorp phát hành. |
| `Terraform has been successfully initialized!` | Thư mục bootstrap đã sẵn sàng chạy `validate`, `plan`, `apply`. |

Giải thích dễ hiểu về `signed by HashiCorp`:

AWS provider là chương trình Terraform sẽ chạy trên máy bạn. Vì vậy Terraform cần kiểm tra gói provider tải về có đúng là gói chính thức không.

`signed by HashiCorp` nghĩa là:

```text
Terraform tải AWS provider
  -> kiểm tra gói này có phải do HashiCorp phát hành không
  -> kiểm tra nội dung gói có khớp với hash trong lock file không
  -> hợp lệ thì mới cài vào .terraform/
```

Nếu gói provider không đúng nguồn hoặc nội dung không khớp, Terraform sẽ không coi đó là provider hợp lệ.

Nói ngắn gọn:

```text
version trong .terraform.lock.hcl -> dùng đúng version provider
hash trong .terraform.lock.hcl    -> file provider tải về đúng nội dung
signed by HashiCorp               -> provider này do HashiCorp phát hành
```

Sau lệnh này, Terraform chỉ chuẩn bị môi trường local. Nó chưa tạo S3, DynamoDB hoặc KMS trên AWS.

Nếu thư mục bootstrap đã có `backend.tf` trỏ tới S3 nhưng bucket chưa tồn tại, `terraform init` sẽ lỗi vì Terraform cố kết nối backend trước khi tạo resource. Đó là lý do file backend S3 được để tạm trong `remote-state/backend.tf`.

#### 5.2. `terraform fmt`

Chạy:

```powershell
terraform fmt
```

Lệnh này chỉ xử lý format file `.tf`:

```text
*.tf trong thư mục hiện tại
  -> căn spacing
  -> căn dấu =
  -> chuẩn hóa style HCL
```

Tác động:

- Có thể sửa nội dung file `.tf` trên máy local.
- Không gọi AWS API.
- Không đọc credential AWS.
- Không tạo state.
- Không tạo resource.

Giá trị tạo ra:

- File Terraform dễ review hơn.
- Diff sạch hơn khi commit.
- Tránh mỗi người format một kiểu.

Nếu command in ra tên file, ví dụ:

```text
backend.tf
```

nghĩa là file đó vừa được format lại.

Trong lần chạy này, `terraform fmt` không in ra file nào:

```text
PS ...\terraform\bootstrap\backend> terraform fmt
PS ...\terraform\bootstrap\backend>
```

Nghĩa là các file `.tf` hiện đã đúng format, nên Terraform không cần sửa gì. Nếu nó in ra `backend.tf`, `main.tf` hoặc file khác, nghĩa là file đó vừa được căn lại spacing.

#### 5.3. `terraform validate`

Chạy:

```powershell
terraform validate
```

Lệnh này kiểm tra cấu hình Terraform có hợp lệ về mặt cú pháp và liên kết nội bộ không.

Nó kết hợp:

| Thành phần | Terraform kiểm tra gì |
|---|---|
| `variables.tf` | Biến được khai báo đúng type chưa. |
| `locals.tf` | `local.common_tags` có tham chiếu biến tồn tại không. |
| `providers.tf` | Provider block viết đúng schema không. |
| `main.tf` | Resource type, argument, reference có hợp lệ không. |
| `outputs.tf` | Output có tham chiếu resource tồn tại không. |
| AWS provider schema | Biết `aws_s3_bucket`, `aws_kms_key`, `aws_dynamodb_table` cần field nào. |

Tác động:

```text
Terraform code
  -> kiểm tra syntax/reference/schema
  -> không tạo AWS resource
  -> không so sánh với AWS hiện tại
  -> không biết bucket thật đã tồn tại hay chưa
```

Giá trị tạo ra:

- Xác nhận code Terraform đọc được.
- Bắt lỗi sớm như sai tên resource, sai field, thiếu provider.

Ví dụ `validate` có thể bắt lỗi:

```text
Reference to undeclared input variable
Unsupported argument
Invalid resource type
```

Nhưng `validate` chưa bắt được lỗi kiểu:

```text
S3 bucket name đã bị người khác dùng
IAM user không đủ quyền
AWS region sai
```

Những lỗi đó chỉ lộ ra khi `plan` hoặc `apply` gọi AWS API.

Output thực tế trong lần chạy này:

```text
Success! The configuration is valid.
```

Ý nghĩa:

```text
Terraform đọc được versions.tf, providers.tf, variables.tf, locals.tf, main.tf, outputs.tf
Các reference như var.project, local.common_tags, aws_s3_bucket.terraform_state.id đều hợp lệ
AWS provider schema chấp nhận các resource/argument đang dùng
```

Nhưng dòng `Success` này chưa có nghĩa là resource đã tồn tại hay có quyền tạo resource. Nó chỉ xác nhận code Terraform hợp lệ trước khi gọi AWS để lập plan.

#### 5.4. `terraform plan -out=tfplan`

Chạy:

```powershell
terraform plan -out=tfplan
```

Đây là bước Terraform ghép mọi thứ lại với nhau để tính “nếu apply thì sẽ làm gì”.

Đầu vào của `plan`:

| Đầu vào | Vai trò |
|---|---|
| File `.tf` | Mô tả hạ tầng mong muốn: S3, DynamoDB, KMS, policy. |
| `terraform.tfvars` | Gán giá trị thật cho `var.aws_region`, `var.project`, `var.account_id`, `var.owner`, `var.state_bucket_name`. |
| `locals.tf` | Tạo `local.common_tags` từ các biến đã có giá trị. |
| AWS credential local | Cho provider quyền gọi AWS API để đọc trạng thái thật. |
| Terraform state hiện tại | Cho Terraform biết resource nào đã được quản lý trước đó. Lần đầu là local state rỗng. |
| AWS provider | Dịch resource Terraform thành API tương ứng của AWS. |

Luồng xử lý dễ hiểu:

```text
1. Terraform đọc code .tf
   -> biết mình muốn có S3 bucket, DynamoDB table, KMS key

2. Terraform đọc terraform.tfvars
   -> biết tên bucket thật là gì
   -> biết region/account/project/owner là gì

3. Terraform đọc state hiện tại
   -> biết trước đó nó đã quản lý resource nào chưa
   -> lần bootstrap đầu tiên thì local state thường đang rỗng

4. Terraform hỏi AWS
   -> bucket này đã tồn tại chưa?
   -> DynamoDB table này đã tồn tại chưa?
   -> KMS key này đã tồn tại chưa?

5. Terraform so sánh
   -> code muốn có resource
   -> state chưa ghi nhận resource
   -> AWS hiện tại chưa có resource do Terraform quản lý

6. Terraform kết luận
   -> cần tạo resource mới
   -> in ra Plan: 9 to add, 0 to change, 0 to destroy

7. Vì có -out=tfplan
   -> Terraform lưu kế hoạch này vào file tfplan
```

Nói ngắn gọn:

```text
plan = xem trước Terraform sẽ làm gì
plan chưa tạo resource thật
```

`terraform plan` không chỉ đọc file local. Nó có thể gọi AWS API để kiểm tra trạng thái thật. Nhưng ở bước này Terraform chỉ đọc/kiểm tra, chưa tạo S3, chưa tạo DynamoDB, chưa tạo KMS.

Terraform cũng tính thứ tự tạo resource. Ví dụ:

```text
KMS key
  -> KMS alias
  -> S3 encryption config

S3 bucket
  -> versioning
  -> public access block
  -> ownership controls
  -> bucket policy

DynamoDB table
  -> state lock table
```

Terraform hiểu thứ tự này nhờ các tham chiếu trong code như:

```hcl
aws_kms_key.terraform_state.arn
aws_s3_bucket.terraform_state.id
aws_s3_bucket.terraform_state.arn
```

Tác động:

- Có gọi AWS API để đọc/check tài nguyên.
- Chưa tạo, sửa, xóa resource.
- Tạo file plan local tên `tfplan`.

Giải thích dễ hiểu về file `tfplan`:

```text
tfplan
  -> file local được tạo bởi terraform plan -out=tfplan
  -> chứa kế hoạch Terraform vừa tính xong
  -> terraform apply "tfplan" sẽ chạy đúng kế hoạch trong file này
```

Nếu không dùng `-out=tfplan`, bạn vẫn xem được plan trên màn hình, nhưng khi chạy `terraform apply`, Terraform sẽ tính plan lại một lần nữa.

Nếu dùng `-out=tfplan`, luồng sẽ là:

```text
terraform plan -out=tfplan
  -> xem trước kế hoạch
  -> lưu kế hoạch vào file tfplan

terraform apply "tfplan"
  -> không tự tính kế hoạch mới
  -> dùng đúng kế hoạch đã lưu trong tfplan
```

Điều này hữu ích vì kế hoạch bạn review chính là kế hoạch sẽ được apply.

Ghi nhớ về `tfplan`:

- `tfplan` là file sinh ra sau khi chạy lệnh, không phải file code.
- `tfplan` nằm local trong thư mục đang chạy Terraform.
- Không nên commit `tfplan` lên Git.
- Nếu chạy lại `terraform plan -out=tfplan`, file `tfplan` cũ trong cùng thư mục sẽ bị ghi đè.
- Nếu đổi code `.tf`, đổi `terraform.tfvars`, hoặc state/AWS thật đã thay đổi, nên chạy lại `terraform plan -out=tfplan` để tạo plan mới.
- Không nên dùng một file `tfplan` quá cũ để apply.

Vì sao không nên đẩy `tfplan` lên Git?

```text
tfplan là kết quả tạm thời của một lần plan cụ thể
tfplan có thể chứa giá trị lấy từ biến, state hoặc provider
tfplan có thể không còn đúng sau khi code/state/AWS thay đổi
tfplan không cần cho người khác đọc source Terraform
```

Trong repo nên commit:

```text
*.tf
*.tfvars.example
.terraform.lock.hcl
```

Không nên commit:

```text
tfplan
*.tfplan
terraform.tfstate
terraform.tfstate.backup
.terraform/
terraform.tfvars
```

Nếu lỡ có `tfplan` trong thư mục và chạy lại:

```powershell
terraform plan -out=tfplan
```

Terraform sẽ ghi plan mới vào đúng tên file `tfplan`. Nói đơn giản là file cũ bị thay bằng file mới trên máy local. Git chỉ bị ảnh hưởng nếu bạn cố tình `git add tfplan`, nhưng mình không nên làm vậy.

Nếu lỡ commit `tfplan` lên Git:

```text
Người khác git pull
  -> cũng nhận file tfplan đó

Người đó chạy terraform plan -out=tfplan
  -> Terraform ghi đè tfplan trên máy người đó
  -> Git sẽ thấy tfplan bị modified
```

Việc này không giúp ích gì, vì mỗi người/mỗi CI nên tự tạo plan mới từ code, biến, state và AWS tại thời điểm chạy. Repo này đã cấu hình `.gitignore` để bỏ qua:

```text
tfplan
*.tfplan
```

Giá trị tạo ra:

- Một bản kế hoạch cố định cho lần apply tiếp theo.
- Bạn nhìn được Terraform định tạo bao nhiêu resource.
- Nếu dùng `-out=tfplan`, lệnh `apply "tfplan"` sẽ apply đúng bản plan đã xem, tránh việc plan lại ra kết quả khác.

Kết quả mong đợi lần đầu:

```text
Plan: 9 to add, 0 to change, 0 to destroy.
```

Output thực tế trong lần chạy này có các phần quan trọng:

```text
Terraform used the selected providers to generate the following execution plan.
Resource actions are indicated with the following symbols:
  + create
 <= read (data resources)
```

Ý nghĩa:

| Ký hiệu | Nghĩa |
|---|---|
| `+ create` | Terraform sẽ tạo resource mới trên AWS. |
| `<= read` | Terraform sẽ đọc/tính data source trong lúc apply, không tạo resource riêng. |

Trong plan có dòng:

```text
# data.aws_iam_policy_document.terraform_state will be read during apply
# (config refers to values not yet known)
```

Nó nghĩa là bucket policy chưa thể tính đủ ngay tại plan, vì policy cần ARN của S3 bucket:

```text
aws_s3_bucket.terraform_state.arn
```

Mà bucket ARN chỉ chắc chắn có sau khi bucket được tạo. Vì vậy Terraform đánh dấu nhiều giá trị là:

```text
(known after apply)
```

Đây không phải lỗi. Nó nghĩa là Terraform biết giá trị đó sẽ có sau khi AWS tạo resource thật.

Các resource chính trong plan:

```text
aws_kms_key.terraform_state will be created
aws_kms_alias.terraform_state will be created
aws_s3_bucket.terraform_state will be created
aws_s3_bucket_versioning.terraform_state will be created
aws_s3_bucket_server_side_encryption_configuration.terraform_state will be created
aws_s3_bucket_public_access_block.terraform_state will be created
aws_s3_bucket_ownership_controls.terraform_state will be created
aws_s3_bucket_policy.terraform_state will be created
aws_dynamodb_table.terraform_lock will be created
```

Các giá trị đáng chú ý Terraform đã tính được ngay từ `.tfvars` và `locals.tf`:

```text
name   = "terraform-state-lock"
bucket = "newgate2601-terraform-state-150914615641-ap-southeast-1"
name   = "alias/newgate2601/terraform-state"

tags_all = {
  "AccountId"   = "150914615641"
  "Component"   = "terraform-backend"
  "Environment" = "bootstrap"
  "ManagedBy"   = "Terraform"
  "Owner"       = "tony"
  "Project"     = "newgate2601"
}
```

Đoạn trên chứng minh:

```text
terraform.tfvars
  -> cấp giá trị cho var.project, var.owner, var.account_id, var.aws_region

locals.tf
  -> gom các var đó thành local.common_tags

providers.tf
  -> đưa local.common_tags vào default_tags

AWS provider
  -> áp tag đó lên các resource bằng tags_all
```

Cuối plan:

```text
Plan: 9 to add, 0 to change, 0 to destroy.

Changes to Outputs:
  + kms_key_arn       = (known after apply)
  + lock_table_name   = "terraform-state-lock"
  + state_bucket_name = "newgate2601-terraform-state-150914615641-ap-southeast-1"

Saved the plan to: tfplan
```

Ý nghĩa:

- `9 to add`: Terraform sẽ tạo đúng 9 resource quản lý backend.
- `0 to change`: không sửa resource nào đã có.
- `0 to destroy`: không xóa resource nào.
- `kms_key_arn = (known after apply)`: KMS ARN chỉ biết sau khi AWS tạo key.
- `Saved the plan to: tfplan`: kế hoạch đã được đóng gói vào file `tfplan`; bước apply sau sẽ dùng đúng kế hoạch này.

Nếu Terraform hỏi:

```text
var.project
Enter a value:
```

nghĩa là biến đó chưa có trong `terraform.tfvars`, chưa có `default`, và cũng chưa được truyền qua `-var`.

#### 5.5. `terraform apply "tfplan"`

Chạy:

```powershell
terraform apply "tfplan"
```

Lệnh này lấy kế hoạch đã lưu trong `tfplan` và bắt đầu gọi AWS API để tạo resource thật.

Nó kết hợp:

| Thành phần | Vai trò khi apply |
|---|---|
| `tfplan` | Danh sách hành động đã được tính ở bước plan. |
| AWS provider | Gọi API tạo KMS, S3, DynamoDB. |
| AWS credential | Quyền thực thi các API đó. |
| Terraform state local | Ghi lại resource nào đã tạo thành công và ID thật của chúng. |

Tác động vào AWS:

```text
AWS KMS
  -> tạo KMS key
  -> tạo alias alias/<project>/terraform-state

Amazon S3
  -> tạo bucket state
  -> bật versioning
  -> bật SSE-KMS encryption
  -> bật block public access
  -> bật ownership controls
  -> gắn bucket policy ép HTTPS và encryption

Amazon DynamoDB
  -> tạo table terraform-state-lock
  -> tạo partition key LockID
  -> bật point-in-time recovery
```

Tác động vào local state:

```text
terraform.tfstate
  -> lưu mapping Terraform address với AWS resource id thật
```

Ví dụ mapping ý tưởng:

```text
aws_s3_bucket.terraform_state
  -> bucket newgate2601-terraform-state-150914615641-ap-southeast-1

aws_dynamodb_table.terraform_lock
  -> table terraform-state-lock

aws_kms_key.terraform_state
  -> key arn:aws:kms:...
```

Giá trị tạo ra:

- Hạ tầng backend thật trong AWS.
- Local state biết chính xác resource nào thuộc quyền quản lý của Terraform.
- Các output có thể đọc được bằng `terraform output`.

Kết quả mong đợi:

```text
Apply complete! Resources: 9 added, 0 changed, 0 destroyed.
```

Output thực tế trong lần chạy này:

```text
aws_kms_key.terraform_state: Creating...
aws_dynamodb_table.terraform_lock: Creating...
aws_s3_bucket.terraform_state: Creating...
aws_s3_bucket.terraform_state: Creation complete after 2s [id=newgate2601-terraform-state-150914615641-ap-southeast-1]
data.aws_iam_policy_document.terraform_state: Reading...
aws_s3_bucket_public_access_block.terraform_state: Creating...
aws_s3_bucket_ownership_controls.terraform_state: Creating...
aws_s3_bucket_versioning.terraform_state: Creating...
data.aws_iam_policy_document.terraform_state: Read complete after 0s [id=1759525144]
aws_s3_bucket_policy.terraform_state: Creating...
```

Đọc đoạn này:

- Terraform tạo KMS key, DynamoDB table và S3 bucket gần như song song vì ba resource này chưa phụ thuộc trực tiếp vào nhau.
- Sau khi S3 bucket tạo xong, Terraform mới đọc `data.aws_iam_policy_document.terraform_state`.
- Bucket policy cần bucket ARN, nên nó chỉ tạo sau khi data policy đã đọc/tính xong.
- Versioning, public access block và ownership controls đều gắn lên bucket vừa tạo.

Đoạn tiếp:

```text
aws_kms_key.terraform_state: Creation complete after 9s [id=38aaa237-5b16-4d7e-811c-c634ae35de52]
aws_kms_alias.terraform_state: Creating...
aws_s3_bucket_server_side_encryption_configuration.terraform_state: Creating...
aws_kms_alias.terraform_state: Creation complete after 0s [id=alias/newgate2601/terraform-state]
aws_s3_bucket_server_side_encryption_configuration.terraform_state: Creation complete after 0s [id=newgate2601-terraform-state-150914615641-ap-southeast-1]
```

Ý nghĩa:

- KMS key tạo xong và có ID thật `38aaa237-5b16-4d7e-811c-c634ae35de52`.
- KMS alias chỉ tạo được sau khi key tồn tại, vì alias phải trỏ tới key.
- S3 encryption config cũng chỉ tạo được sau khi KMS key tồn tại, vì encryption config cần `kms_master_key_id`.

Đoạn DynamoDB:

```text
aws_dynamodb_table.terraform_lock: Still creating... [00m10s elapsed]
aws_dynamodb_table.terraform_lock: Creation complete after 11s [id=terraform-state-lock]
```

Ý nghĩa:

- DynamoDB table thường mất vài giây để AWS chuyển sang trạng thái usable.
- Khi complete, table có ID/name là `terraform-state-lock`.
- Sau này S3 backend dùng table này để lock state.

Kết quả cuối:

```text
Apply complete! Resources: 9 added, 0 changed, 0 destroyed.

Outputs:

kms_key_arn = "arn:aws:kms:ap-southeast-1:150914615641:key/38aaa237-5b16-4d7e-811c-c634ae35de52"
lock_table_name = "terraform-state-lock"
state_bucket_name = "newgate2601-terraform-state-150914615641-ap-southeast-1"
```

Đây là thời điểm AWS đã có backend thật:

```text
S3 bucket     -> newgate2601-terraform-state-150914615641-ap-southeast-1
DynamoDB lock -> terraform-state-lock
KMS key       -> 38aaa237-5b16-4d7e-811c-c634ae35de52
KMS alias     -> alias/newgate2601/terraform-state
```

Nhưng state của bootstrap lúc này vẫn đang nằm local, vì `backend.tf` chưa được copy vào root module trước lệnh apply.

#### 5.6. `terraform output kms_key_arn`

Chạy:

```powershell
terraform output kms_key_arn
```

Lệnh này đọc output từ Terraform state.

Nó không tạo resource mới. Nó chỉ lấy giá trị đã được Terraform biết sau apply:

```text
outputs.tf
  -> output "kms_key_arn"
  -> aws_kms_key.terraform_state.arn
  -> giá trị ARN thật trong state
```

Giá trị này cần cho `remote-state/backend.tf` vì backend S3 phải biết KMS key nào dùng để mã hóa state object.

Ví dụ:

```text
arn:aws:kms:ap-southeast-1:150914615641:key/38aaa237-5b16-4d7e-811c-c634ae35de52
```

Trong lần chạy này, Terraform cũng in output ngay sau `apply`, nên có thể lấy trực tiếp dòng:

```text
kms_key_arn = "arn:aws:kms:ap-southeast-1:150914615641:key/38aaa237-5b16-4d7e-811c-c634ae35de52"
```

Giá trị này phải được đưa vào `remote-state/backend.tf`:

```hcl
kms_key_id = "arn:aws:kms:ap-southeast-1:150914615641:key/38aaa237-5b16-4d7e-811c-c634ae35de52"
```

### 6. Migrate bootstrap state lên S3

Mở file:

```text
terraform/bootstrap/backend/remote-state/backend.tf
```

Kiểm tra các giá trị:

```hcl
bucket         = "newgate2601-terraform-state-150914615641-ap-southeast-1"
key            = "bootstrap/backend/terraform.tfstate"
region         = "ap-southeast-1"
dynamodb_table = "terraform-state-lock"
encrypt        = true
kms_key_id     = "<kms-key-arn-from-terraform-output>"
```

Thay `kms_key_id` bằng output thật từ `terraform output kms_key_arn`.

File này không tạo S3 bucket. Nó chỉ nói với Terraform:

```text
Từ bây giờ state của module này sẽ được đọc/ghi ở đâu.
```

Các thành phần trong backend S3 phối hợp như sau:

| Tham số | Kết nối tới đâu | Dùng để làm gì |
|---|---|---|
| `bucket` | S3 bucket vừa tạo ở bước apply | Nơi lưu file `terraform.tfstate`. |
| `key` | Object path bên trong bucket | Tách state bootstrap khỏi state của module khác. |
| `region` | AWS region của bucket/table | Giúp Terraform gọi đúng endpoint AWS. |
| `dynamodb_table` | DynamoDB table vừa tạo | Lock state khi Terraform chạy. |
| `encrypt` | S3 server-side encryption | Yêu cầu state object được mã hóa. |
| `kms_key_id` | KMS key ARN vừa output | Chỉ định key mã hóa state. |

Luồng state sau migrate:

```text
Trước migrate:
  terraform/bootstrap/backend/terraform.tfstate trên máy local

Sau migrate:
  s3://newgate2601-terraform-state-150914615641-ap-southeast-1/bootstrap/backend/terraform.tfstate
```

Sau đó copy backend config vào root module bootstrap:

```powershell
Copy-Item remote-state\backend.tf backend.tf
```

Vì sao phải copy?

Terraform chỉ tự đọc backend config ở root module hiện tại. File trong `remote-state/backend.tf` chỉ là bản mẫu để giữ an toàn ở lần chạy đầu. Khi copy thành:

```text
terraform/bootstrap/backend/backend.tf
```

Terraform mới xem nó là backend config chính thức của module bootstrap.

Chạy migrate:

```powershell
terraform init -migrate-state
```

Note quan trọng:

Lệnh `terraform init -migrate-state` thường chỉ cần chạy **một lần sau lần `terraform apply` bootstrap đầu tiên**.

Lý do:

```text
Lần apply bootstrap đầu tiên
  -> S3 backend chưa tồn tại
  -> Terraform phải dùng local state
  -> sau apply mới có S3 bucket thật
  -> cần migrate state từ local lên S3

Các lần sau
  -> S3 backend đã tồn tại
  -> backend.tf đã trỏ tới S3
  -> Terraform tự đọc/ghi state trên S3
  -> không cần terraform init -migrate-state nữa
```

Sau khi migrate thành công, luồng chạy bình thường là:

```powershell
terraform init
terraform plan -out=tfplan
terraform apply "tfplan"
```

Chỉ dùng lại `terraform init -migrate-state` khi thật sự muốn chuyển state từ nơi lưu cũ sang nơi lưu mới, ví dụ đổi backend hoặc đổi S3 bucket lưu state.

Lệnh này khác `terraform init` lần đầu ở chỗ:

```text
terraform init
  -> cài provider, cấu hình backend

terraform init -migrate-state
  -> cấu hình backend mới
  -> phát hiện đang có state local
  -> hỏi có muốn copy state local lên backend mới không
```

Nó kết hợp:

| Thành phần | Vai trò |
|---|---|
| `backend.tf` | Backend mới là S3. |
| Local `terraform.tfstate` | State cũ đang nằm trên máy. |
| S3 bucket | Nơi nhận state mới. |
| DynamoDB table | Lock trong lúc ghi state. |
| KMS key | Mã hóa object state khi ghi lên S3. |
| AWS credential | Quyền đọc/ghi S3, DynamoDB và dùng KMS key. |

Khi Terraform hỏi:

```text
Do you want to copy existing state to the new backend?
```

Nhập:

```text
yes
```

Lưu ý: `yes` ở đây là câu trả lời tại dòng `Enter a value:` của Terraform, không phải lệnh PowerShell chạy riêng. Nếu Terraform đã kết thúc và bạn gõ `yes` ở prompt `PS ...>`, PowerShell sẽ báo không tìm thấy lệnh `yes`.

Output thực tế trong lần chạy này:

```text
Do you want to copy existing state to the new backend?
  Pre-existing state was found while migrating the previous "local" backend to the
  newly configured "s3" backend. No existing state was found in the newly
  configured "s3" backend. Do you want to copy this state to the new "s3"
  backend? Enter "yes" to copy and "no" to start with an empty state.

  Enter a value: yes

Successfully configured the backend "s3"!
```

Đọc output này thật kỹ:

| Câu trong output | Ý nghĩa |
|---|---|
| `Pre-existing state was found` | Terraform thấy state local đang có 9 resource vừa apply. |
| `previous "local" backend` | Trước migrate, state nằm trên máy local. |
| `newly configured "s3" backend` | Sau khi copy `backend.tf`, backend mới là S3. |
| `No existing state was found in the newly configured "s3" backend` | Trong S3 key `bootstrap/backend/terraform.tfstate` chưa có state. Đây là đúng lần migrate đầu. |
| `Enter "yes" to copy` | Nếu nhập `yes`, Terraform copy state local lên S3. |
| `Successfully configured the backend "s3"` | Từ giờ module bootstrap đọc/ghi state qua S3 backend. |

Từ lúc này, state của chính bootstrap cũng nằm trên S3.

Tác động thực tế:

```text
Terraform local
  -> đọc local terraform.tfstate
  -> lấy lock qua DynamoDB
  -> ghi state object lên S3 tại key bootstrap/backend/terraform.tfstate
  -> S3 mã hóa object bằng KMS key đã cấu hình
  -> các lần terraform plan/apply sau đọc state từ S3
```

Giá trị tạo ra:

- State không còn phụ thuộc vào một laptop.
- GitLab Runner sau này có thể dùng cùng backend.
- Nếu chạy Terraform từ máy khác, chỉ cần có code + credential + backend config là đọc được state.
- DynamoDB lock giúp tránh hai terminal/CI cùng sửa một state.

Sau migrate, Terraform init lại provider:

```text
Initializing provider plugins...
- Reusing previous version of hashicorp/aws from the dependency lock file
- Using previously-installed hashicorp/aws v5.100.0
```

Khác với lần đầu, lần này provider đã có trong `.terraform`, nên Terraform dùng lại provider đã cài thay vì tải mới.

Warning đã gặp:

```text
Warning: Deprecated Parameter
The parameter "dynamodb_table" is deprecated. Use parameter "use_lockfile" instead.
```

Warning này đến từ `backend.tf`, không phải từ resource `aws_dynamodb_table`. Terraform version mới khuyến nghị cơ chế lock mới hơn. Trong bài này, warning không làm migrate thất bại; backend S3 vẫn được cấu hình thành công.

Không commit các file state local nếu còn sót lại:

```text
terraform.tfstate
terraform.tfstate.backup
*.tfplan
```

### 7. Kiểm tra hoàn thành

Kiểm tra bằng Terraform:

```powershell
terraform init
terraform plan
```

Kết quả mong đợi sau khi đã apply và migrate:

```text
No changes. Your infrastructure matches the configuration.
```

Output thực tế trong lần chạy này khi chạy lại `terraform init`:

```text
Initializing the backend...

Initializing provider plugins...
- Reusing previous version of hashicorp/aws from the dependency lock file
- Using previously-installed hashicorp/aws v5.100.0

Terraform has been successfully initialized!
```

Điểm khác với `init -migrate-state`:

- Không còn hỏi copy state nữa.
- Terraform đã biết backend hiện tại là S3.
- Provider đã có sẵn nên dùng lại bản đã cài.

Output thực tế khi chạy `terraform plan` sau migrate:

```text
aws_kms_key.terraform_state: Refreshing state... [id=38aaa237-5b16-4d7e-811c-c634ae35de52]
aws_s3_bucket.terraform_state: Refreshing state... [id=newgate2601-terraform-state-150914615641-ap-southeast-1]
aws_dynamodb_table.terraform_lock: Refreshing state... [id=terraform-state-lock]
aws_kms_alias.terraform_state: Refreshing state... [id=alias/newgate2601/terraform-state]
aws_s3_bucket_public_access_block.terraform_state: Refreshing state... [id=newgate2601-terraform-state-150914615641-ap-southeast-1]
aws_s3_bucket_ownership_controls.terraform_state: Refreshing state... [id=newgate2601-terraform-state-150914615641-ap-southeast-1]
aws_s3_bucket_server_side_encryption_configuration.terraform_state: Refreshing state... [id=newgate2601-terraform-state-150914615641-ap-southeast-1]
aws_s3_bucket_versioning.terraform_state: Refreshing state... [id=newgate2601-terraform-state-150914615641-ap-southeast-1]
data.aws_iam_policy_document.terraform_state: Reading...
data.aws_iam_policy_document.terraform_state: Read complete after 0s [id=1759525144]
aws_s3_bucket_policy.terraform_state: Refreshing state... [id=newgate2601-terraform-state-150914615641-ap-southeast-1]

No changes. Your infrastructure matches the configuration.
```

Đọc đoạn `Refreshing state`:

```text
Terraform remote state trên S3
  -> biết đang quản lý KMS key, S3 bucket, DynamoDB table, alias, bucket configs
  -> gọi AWS API để refresh trạng thái thật của từng resource
  -> so sánh AWS thật với code .tf hiện tại
```

Kết quả `No changes` nghĩa là:

```text
Code Terraform hiện tại
  == State trên S3
  == Resource thật trên AWS
```

Đây là dấu hiệu hoàn thành tốt nhất của bước 3.1.

Kiểm tra nhanh bằng AWS CLI nếu cần:

```powershell
aws s3api get-bucket-versioning --bucket newgate2601-terraform-state-150914615641-ap-southeast-1
aws s3api get-public-access-block --bucket newgate2601-terraform-state-150914615641-ap-southeast-1
aws dynamodb describe-table --table-name terraform-state-lock --region ap-southeast-1
```

### 8. Lỗi thường gặp

| Lỗi | Cách xử lý nhanh |
|---|---|
| `terraform` is not recognized | Cài Terraform, mở terminal mới, kiểm tra `terraform version`. |
| Terraform hỏi `var.project`, `var.owner`, `var.account_id` | Chưa có `terraform.tfvars`; copy từ `terraform.tfvars.example`. |
| `No valid credential sources found` | Chạy `aws configure`, rồi kiểm tra `aws sts get-caller-identity`. |
| Tên S3 bucket bị trùng | Đổi `state_bucket_name`, nên có project + account id + region. |
| `AccessDenied` | IAM user lab thiếu quyền tạo S3, DynamoDB, KMS hoặc IAM policy. |
| `terraform init -migrate-state` hỏi migrate | Đây là đúng luồng; nhập `yes` nếu backend config đã đúng. |
| Warning `dynamodb_table` deprecated | Ghi nhận warning, bước này vẫn chạy được; refactor lock backend sau. |

### 9. Kết quả sau bước này

Trạng thái sau khi hoàn thành:

```text
[x] Có S3 bucket lưu Terraform state
[x] Có DynamoDB table lock state
[x] Có KMS key mã hóa state
[x] Bootstrap state đã migrate lên S3
[x] Terraform bootstrap sẵn sàng làm backend cho các root module sau
```

Bước tiếp theo là tạo root module đầu tiên cho hạ tầng dùng chung hoặc môi trường `dev`, theo cấu trúc trong `terraform/README.md`.

---

## Bước 3.2 - Tạo root module đầu tiên cho network dev

### 1. Mục tiêu của bước này

Mục tiêu là tạo bộ khung Terraform đầu tiên dùng cho network của môi trường `dev`.

Sau bước 3.1, ta đã có nơi lưu state:

```text
S3 bucket  : lưu terraform.tfstate
DynamoDB   : lock state khi plan/apply
KMS key    : mã hóa state
```

Nhưng bước 3.1 chưa tạo network. Bước 3.2 này chỉ tạo khung để chuẩn bị cho network dev.

Root module sẽ nằm ở:

```text
terraform/environments/dev/network
```

Module VPC dùng lại sẽ nằm ở:

```text
terraform/modules/vpc
```

Sau khi hoàn thành, ta có:

```text
terraform/
├── environments/
│   └── dev/
│       └── network/
│           ├── versions.tf
│           ├── backend.tf
│           ├── providers.tf
│           ├── variables.tf
│           ├── locals.tf
│           ├── main.tf
│           ├── outputs.tf
│           ├── terraform.tfvars.example
│           └── README.md
└── modules/
    └── vpc/
        ├── variables.tf
        ├── main.tf
        ├── outputs.tf
        └── README.md
```

Ghi nhớ ngắn:

| Thành phần | Nghĩa đơn giản |
|---|---|
| `terraform/environments/dev/network` | Nơi chạy `terraform init`, `plan`, `apply` cho network dev. |
| `terraform/modules/vpc` | Bộ code VPC dùng lại. Root module sẽ gọi module này. |
| `backend.tf` | Nói state của network dev lưu ở đâu. |
| `terraform.tfvars` | Giá trị thật của môi trường dev, ví dụ region, CIDR, owner. |

State của network dev sẽ lưu riêng tại key:

```text
dev/network/terraform.tfstate
```

Ở bước này, module VPC chỉ là bộ khung ban đầu: có thư mục và file cần thiết, nhưng chưa tạo VPC thật.

### 2. Trước khi bắt đầu cần có gì

Cần chuẩn bị:

- Đã hoàn thành bước 3.1.
- S3 bucket Terraform state đã tồn tại.
- DynamoDB table lock đã tồn tại.
- KMS key mã hóa state đã tồn tại.
- AWS CLI đã đăng nhập đúng IAM user lab.
- Terraform CLI chạy được.
- Đang đứng ở repository:

```powershell
cd C:\code\springboot-learning
```

Kiểm tra nhanh AWS identity:

```powershell
aws sts get-caller-identity
```

Kết quả mong đợi:

```json
{
  "UserId": "...",
  "Account": "150914615641",
  "Arn": "arn:aws:iam::150914615641:user/tony-lab-admin"
}
```

Kiểm tra backend bootstrap vẫn ổn:

```powershell
cd terraform\bootstrap\backend
terraform init
terraform plan
```

Kết quả mong đợi:

```text
No changes. Your infrastructure matches the configuration.
```

Sau đó quay lại root repository:

```powershell
cd ..\..\..
```

### 3. File Terraform đã chuẩn bị

Các file của root module nằm ở:

```text
terraform/environments/dev/network
```

Gồm:

| File | Vai trò ngắn gọn |
|---|---|
| `versions.tf` | Khai báo Terraform và AWS provider version. |
| `backend.tf` | Cấu hình remote state riêng cho network dev. |
| `providers.tf` | Cấu hình AWS provider và default tags. |
| `variables.tf` | Khai báo input cần truyền vào. |
| `locals.tf` | Gom tên chuẩn và tag chung. |
| `main.tf` | Gọi module dùng lại `modules/vpc`. |
| `outputs.tf` | In ra giá trị quan trọng của network dev. |
| `terraform.tfvars.example` | File mẫu để tạo input thật. |
| `README.md` | Ghi chú phạm vi root module. |

Các file của module VPC dùng lại nằm ở:

```text
terraform/modules/vpc
```

Gồm:

| File | Vai trò ngắn gọn |
|---|---|
| `variables.tf` | Khai báo input mà module VPC nhận từ root module. |
| `main.tf` | Nơi sẽ tạo VPC, subnet, route table ở bước sau. |
| `outputs.tf` | Output trả về cho root module. |
| `README.md` | Tài liệu ngắn cho module VPC. |

Không cần tạo `staging` hoặc `production` ở bước này.

### 4. Tạo bộ khung root module và module dùng lại

#### 4.1. Xác định root module đầu tiên sẽ tạo

Trong tài liệu `terraform/README.md`, cấu trúc môi trường đã được định hướng:

```text
terraform/environments/
├── shared-services/
├── dev/
├── staging/
└── production/
```

Với `dev`, các root module dự kiến là:

```text
terraform/environments/dev/
├── network
├── eks
├── data
└── observability
```

Ta chọn `network` trước vì:

- EKS cần VPC và subnet.
- RDS, MSK, ElastiCache cần private/data subnet.
- VPC endpoint, route table và security group là nền cho các service sau.
- Nếu network sai, các tầng phía trên sẽ sai theo.

Không tạo `eks`, `data`, `observability` ở bước này.

#### 4.2. Tạo thư mục root module `dev/network`

Chạy từ repository root:

```powershell
New-Item -ItemType Directory -Force terraform\environments\dev\network
```

Thư mục này là nơi chạy Terraform cho network dev.

Sau này khi thao tác network dev, luôn vào thư mục này:

```powershell
cd terraform\environments\dev\network
```

Không chạy `terraform apply` ở toàn bộ thư mục `terraform/`, vì Terraform chỉ nên chạy trong đúng root module.

#### 4.3. Tạo thư mục module dùng lại `modules/vpc`

Chạy:

```powershell
New-Item -ItemType Directory -Force terraform\modules\vpc
```

Module này là nơi đặt code tạo VPC thật sau này.

Ở bước này, ta tạo bộ khung trước:

```text
modules/vpc/
├── variables.tf
├── main.tf
├── outputs.tf
└── README.md
```

Lưu ý quan trọng:

```text
Không hard-code giá trị dev trong modules/vpc.
```

Ví dụ không viết như này trong module:

```hcl
cidr_block = "10.20.0.0/16"
```

Mà phải nhận từ biến:

```hcl
cidr_block = var.vpc_cidr
```

Khác biệt của dev, staging và production sẽ nằm trong root module hoặc `.tfvars`, không nằm chết trong module.

#### 4.4. Tạo `versions.tf` cho root module

Tạo file:

```text
terraform/environments/dev/network/versions.tf
```

Nội dung:

```hcl
terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}
```

File này khai báo:

- Terraform CLI tối thiểu là `1.6.0`.
- Root module này dùng AWS provider.
- Dùng AWS provider dòng `5.x`, tránh tự nhảy major version ngoài kiểm soát.

#### 4.5. Tạo `backend.tf` cho state riêng của `dev/network`

Tạo file:

```text
terraform/environments/dev/network/backend.tf
```

Nội dung:

```hcl
terraform {
  backend "s3" {
    bucket         = "newgate2601-terraform-state-150914615641-ap-southeast-1"
    key            = "dev/network/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "terraform-state-lock"
    encrypt        = true
    kms_key_id     = "arn:aws:kms:ap-southeast-1:150914615641:key/38aaa237-5b16-4d7e-811c-c634ae35de52"
  }
}
```

Giá trị cần thay theo account thật của bạn:

| Tham số | Lấy từ đâu |
|---|---|
| `bucket` | Output `state_bucket_name` của bước 3.1. |
| `region` | Region đã chọn, ví dụ `ap-southeast-1`. |
| `dynamodb_table` | Output `lock_table_name` của bước 3.1. |
| `kms_key_id` | Output `kms_key_arn` của bước 3.1. |

Điểm quan trọng nhất là:

```hcl
key = "dev/network/terraform.tfstate"
```

`key` này giúp tách state network dev khỏi state bootstrap.

So sánh:

| Root module | Backend key |
|---|---|
| `terraform/bootstrap/backend` | `bootstrap/backend/terraform.tfstate` |
| `terraform/environments/dev/network` | `dev/network/terraform.tfstate` |

Không dùng chung một key cho nhiều root module.

Sai nguy hiểm:

```hcl
key = "terraform.tfstate"
```

Nếu nhiều root module cùng dùng key chung như trên, state có thể ghi đè hoặc trộn tài nguyên, rất khó sửa.

#### 4.6. Tạo `providers.tf`

Tạo file:

```text
terraform/environments/dev/network/providers.tf
```

Nội dung:

```hcl
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
}
```

Ý nghĩa:

- Mọi resource AWS trong root module này sẽ tạo ở region `var.aws_region`.
- Tag chung được gắn tự động qua `default_tags`.
- Khi nhìn tài nguyên trên AWS Console, ta biết tài nguyên thuộc project, môi trường và component nào.

#### 4.7. Tạo `variables.tf`

Tạo file:

```text
terraform/environments/dev/network/variables.tf
```

Nội dung:

```hcl
variable "aws_region" {
  description = "AWS region for dev network resources."
  type        = string
}

variable "project" {
  description = "Project name used for naming and tagging."
  type        = string
}

variable "environment" {
  description = "Environment name."
  type        = string
}

variable "owner" {
  description = "Owner of the resources."
  type        = string
}

variable "account_id" {
  description = "AWS account id used for tagging and validation."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the dev VPC."
  type        = string
}

variable "availability_zones" {
  description = "Availability zones used by the dev network."
  type        = list(string)
}
```

Các biến này chia làm hai nhóm:

| Nhóm | Biến | Dùng để làm gì |
|---|---|---|
| Biến chung | `aws_region`, `project`, `environment`, `owner`, `account_id` | Provider, naming, tagging, audit. |
| Biến network | `vpc_cidr`, `availability_zones` | Thiết kế VPC và subnet sau này. |

Ở bước tạo bộ khung này, `vpc_cidr` và `availability_zones` có thể chưa được dùng trong resource thật. Nhưng khai báo trước giúp ta biết root module cần nhận những giá trị nào.

#### 4.8. Tạo `locals.tf`

Tạo file:

```text
terraform/environments/dev/network/locals.tf
```

Nội dung:

```hcl
locals {
  name_prefix = "${var.project}-${var.environment}"

  common_tags = {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "Terraform"
    Owner       = var.owner
    Component   = "network"
    AccountId   = var.account_id
  }
}
```

Ý nghĩa:

- `name_prefix` giúp đặt tên resource nhất quán, ví dụ `newgate2601-dev`.
- `common_tags` giúp mọi resource có tag giống nhau.
- `Component = "network"` phân biệt resource network với backend, EKS, data, observability.

Sau này khi có VPC thật, tag sẽ giúp lọc tài nguyên trên AWS Console:

```text
Project     = newgate2601
Environment = dev
Component   = network
ManagedBy   = Terraform
Owner       = tony
```

#### 4.9. Tạo `main.tf` cho root module

Tạo file:

```text
terraform/environments/dev/network/main.tf
```

Ở bước đầu tiên, có thể để root module gọi module VPC nhưng module chưa tạo resource thật:

```hcl
module "vpc" {
  source = "../../../modules/vpc"

  name_prefix        = local.name_prefix
  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones
  tags               = local.common_tags
}
```

Đường dẫn:

```hcl
source = "../../../modules/vpc"
```

Giải thích đường dẫn từ `terraform/environments/dev/network`:

```text
network
  -> đi lên dev
  -> đi lên environments
  -> đi lên terraform
  -> vào modules/vpc
```

Tức là:

```text
../../../modules/vpc
```

Nếu đường dẫn sai, `terraform init` hoặc `terraform validate` sẽ báo không tìm thấy module.

#### 4.10. Tạo `outputs.tf` cho root module

Tạo file:

```text
terraform/environments/dev/network/outputs.tf
```

Nội dung ban đầu:

```hcl
output "name_prefix" {
  description = "Name prefix used by dev network resources."
  value       = local.name_prefix
}
```

Khi module VPC tạo resource thật, ta sẽ bổ sung thêm:

```hcl
output "vpc_id" {
  description = "Dev VPC id."
  value       = module.vpc.vpc_id
}

output "private_subnet_ids" {
  description = "Private subnet ids for application workloads."
  value       = module.vpc.private_subnet_ids
}

output "data_subnet_ids" {
  description = "Isolated subnet ids for data services."
  value       = module.vpc.data_subnet_ids
}
```

Nhưng ở bước tạo bộ khung, chưa cần output các giá trị chưa tồn tại.

#### 4.11. Tạo `terraform.tfvars.example`

Tạo file:

```text
terraform/environments/dev/network/terraform.tfvars.example
```

Nội dung:

```hcl
aws_region = "ap-southeast-1"
project    = "newgate2601"
environment = "dev"
owner      = "tony"
account_id = "150914615641"

vpc_cidr = "10.20.0.0/16"

availability_zones = [
  "ap-southeast-1a",
  "ap-southeast-1b",
  "ap-southeast-1c"
]
```

File `.example` được commit vào Git để làm mẫu.

Khi chạy thật, copy thành:

```powershell
Copy-Item terraform.tfvars.example terraform.tfvars
```

File `terraform.tfvars` thật thường không commit vì có thể chứa giá trị riêng của account hoặc môi trường.

#### 4.12. Tạo `README.md` cho root module

Tạo file:

```text
terraform/environments/dev/network/README.md
```

Nội dung gợi ý:

````md
# Dev Network

Root module này quản lý network nền cho môi trường `dev`.

State backend:

```text
s3://newgate2601-terraform-state-150914615641-ap-southeast-1/dev/network/terraform.tfstate
```

Các tài nguyên dự kiến:

- VPC.
- Public subnet.
- Private application subnet.
- Isolated data subnet.
- Internet Gateway.
- NAT Gateway.
- Route table.
- VPC endpoint.
- Security group nền.
- VPC Flow Logs.

Không đặt tài nguyên `staging` hoặc `production` trong root module này.
````

#### 4.13. Tạo bộ khung cho `modules/vpc`

Tạo file:

```text
terraform/modules/vpc/variables.tf
```

Nội dung:

```hcl
variable "name_prefix" {
  description = "Prefix used for VPC resource names."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
}

variable "availability_zones" {
  description = "Availability zones used by this VPC."
  type        = list(string)
}

variable "tags" {
  description = "Tags applied to VPC resources."
  type        = map(string)
}
```

Tạo file:

```text
terraform/modules/vpc/main.tf
```

Nội dung ban đầu:

```hcl
# Resources will be added in the next network implementation step.
```

Tạo file:

```text
terraform/modules/vpc/outputs.tf
```

Nội dung ban đầu:

```hcl
output "name_prefix" {
  description = "Prefix used for VPC resource names."
  value       = var.name_prefix
}
```

Tạo file:

```text
terraform/modules/vpc/README.md
```

Nội dung gợi ý:

```md
# VPC Module

Module này dùng để tạo VPC và các thành phần network liên quan.

Module không được hard-code giá trị riêng của `dev`, `staging` hoặc `production`.

Các root module sẽ truyền vào:

- `name_prefix`
- `vpc_cidr`
- `availability_zones`
- `tags`
```

Vì module hiện chưa có resource AWS thật, `terraform plan` sẽ chưa tạo VPC. Đây là chủ ý của bước này: kiểm tra root module, backend và đường dẫn tới module trước.

### 5. Tạo file input thật

Lúc này repo mới có file mẫu:

```text
terraform/environments/dev/network/terraform.tfvars.example
```

File mẫu được commit vào Git để người khác biết cần điền biến nào.

File chạy thật là:

```text
terraform/environments/dev/network/terraform.tfvars
```

File này thường không commit vì nó có thể chứa giá trị riêng của account hoặc môi trường.

Đi vào root module:

```powershell
cd terraform\environments\dev\network
```

Copy file biến:

```powershell
Copy-Item terraform.tfvars.example terraform.tfvars
```

Kiểm tra nội dung:

```powershell
Get-Content terraform.tfvars
```

Nếu account id, owner hoặc region khác thì sửa lại trước khi chạy Terraform.

### 6. Chạy Terraform cho root module `dev/network`

Khác với bước 3.1, bước này **không bootstrap backend nữa**.

Ở bước 3.1:

```text
Lần đầu backend S3 chưa tồn tại
  -> phải chạy bằng local state trước
  -> sau đó mới migrate state lên S3
```

Ở bước 3.2:

```text
Backend S3 đã tồn tại
  -> root module dev/network dùng remote state ngay từ đầu
  -> không cần terraform init -migrate-state nếu chưa có local state cũ
```

Điểm cần kiểm tra kỹ nhất trước khi chạy là `backend.tf` của `dev/network` phải dùng key riêng:

```hcl
key = "dev/network/terraform.tfstate"
```

Không được dùng lại key của bootstrap:

```hcl
key = "bootstrap/backend/terraform.tfstate"
```

#### 6.1. `terraform fmt`

Chạy từ thư mục `terraform` để format cả `environments` và `modules`:

```powershell
cd C:\code\springboot-learning\terraform
terraform fmt -recursive
```

Lệnh này format các file `.tf` bên dưới thư mục hiện tại:

```text
terraform/environments/dev/network
terraform/modules/vpc
```

Sau đó quay lại root module `dev/network` để chạy các lệnh tiếp theo:

```powershell
cd environments\dev\network
```

Kết quả mong đợi:

- Nếu file đã đúng format, lệnh có thể không in gì.
- Nếu file được format lại, Terraform in tên file vừa sửa.

Ví dụ output có thể gặp:

```text
main.tf
```

Ý nghĩa là Terraform vừa sửa format file `main.tf`.

Nếu lệnh không in gì:

```text
Không có output
```

thì đó cũng là bình thường. Nó nghĩa là các file đã đúng format.

#### 6.2. `terraform init`

Chạy:

```powershell
terraform init
```

Lệnh này làm ba việc quan trọng:

```text
Đọc backend.tf
  -> kết nối S3 backend tại key dev/network/terraform.tfstate

Đọc versions.tf
  -> tải hoặc dùng lại AWS provider

Đọc main.tf
  -> tải module local ../../../modules/vpc
```

Kết quả mong đợi:

```text
Initializing the backend...
Initializing modules...
Initializing provider plugins...
Terraform has been successfully initialized!
```

Nếu đây là lần đầu root module `dev/network` dùng backend key này, Terraform sẽ tạo state rỗng trên S3 khi cần ghi state.

Khác với bước bootstrap, root module này **không cần** `terraform init -migrate-state` nếu chưa từng có local state cũ.

Giải thích từng dòng quan trọng:

| Output | Ý nghĩa |
|---|---|
| `Initializing the backend...` | Terraform đọc `backend.tf` và kết nối S3 backend. |
| `Initializing modules...` | Terraform đọc block `module "vpc"` trong `main.tf`. |
| `Initializing provider plugins...` | Terraform chuẩn bị AWS provider theo `versions.tf`. |
| `Terraform has been successfully initialized!` | Root module đã sẵn sàng để validate/plan/apply. |

Sau lệnh này, Terraform thường tạo thư mục:

```text
.terraform/
```

và file:

```text
.terraform.lock.hcl
```

Ý nghĩa:

| File/thư mục | Có commit không | Ý nghĩa |
|---|---|---|
| `.terraform/` | Không | Cache provider/module trên máy local. |
| `.terraform.lock.hcl` | Có | Khóa version provider để team dùng nhất quán. |

Nếu `terraform init` hỏi migrate state, cần dừng lại đọc kỹ.

Với root module mới hoàn toàn, thường không cần migrate. Nếu trước đó bạn từng chạy nhầm bằng local state và đã có `terraform.tfstate`, lúc đó mới cân nhắc migrate.

#### 6.3. `terraform validate`

Chạy:

```powershell
terraform validate
```

Kết quả mong đợi:

```text
Success! The configuration is valid.
```

Lệnh này kiểm tra:

- Syntax HCL đúng.
- Module source đúng.
- Biến truyền vào module phù hợp.
- Output không tham chiếu tới giá trị không tồn tại.

Giải thích output:

| Output | Ý nghĩa |
|---|---|
| `Success! The configuration is valid.` | Terraform hiểu toàn bộ cấu hình hiện tại. |
| `Error: Unsupported argument` | Root module truyền biến mà module VPC chưa khai báo. |
| `Error: Reference to undeclared input variable` | Có `var.xxx` nhưng chưa khai báo biến. |
| `Error: Module not found` | Đường dẫn `source = "../../../modules/vpc"` chưa đúng. |

#### 6.4. `terraform plan`

Chạy:

```powershell
terraform plan
```

Vì module VPC hiện chỉ là bộ khung chưa có resource AWS, `terraform plan` sẽ chưa tạo VPC, subnet hay route table.

Nhưng root module vẫn có output:

```hcl
output "name_prefix" {
  description = "Name prefix used by dev network resources."
  value       = local.name_prefix
}
```

Vì vậy nếu backend state của `dev/network` chưa từng lưu output này, kết quả mong đợi có thể là:

```text
Changes to Outputs:
  + name_prefix = "newgate2601-dev"

You can apply this plan to save these new output values to the Terraform state, without changing any real infrastructure.
```

Ý nghĩa:

```text
Terraform chưa tạo resource AWS nào
Nhưng Terraform muốn lưu output name_prefix vào state
Nếu apply plan này thì chỉ ghi output vào state, không tạo hạ tầng thật
```

Sau khi output đã được lưu vào state, chạy `terraform plan` lại mới có thể ra:

```text
No changes. Your infrastructure matches the configuration.
```

Điều này cũng không có nghĩa là bước sai. Nó chỉ nói rằng:

```text
Root module đã đọc được
Backend đã cấu hình được
Module local đã nối được
Chưa có resource AWS nào để tạo
Output trong cấu hình đã khớp với state
```

Nếu bạn đã thêm resource VPC thật vào `modules/vpc/main.tf`, lúc đó `terraform plan` sẽ hiện danh sách resource chuẩn bị tạo.

Giải thích các output thường gặp trong bước này:

| Output | Ý nghĩa |
|---|---|
| `Warning: Deprecated Parameter` với `dynamodb_table` | Terraform version mới cảnh báo cơ chế lock DynamoDB đang deprecated. Đây là warning backend, chưa làm plan fail. |
| `Changes to Outputs` | Có output mới, ví dụ `name_prefix`, chưa được lưu trong state của root module này. |
| `You can apply this plan to save these new output values...` | Nếu apply, Terraform chỉ lưu output vào state, không tạo resource AWS thật. |
| `No changes. Your infrastructure matches the configuration.` | Cấu hình, output và state đã khớp; module hiện chưa có resource AWS thật để tạo. |
| `Plan: 0 to add, 0 to change, 0 to destroy.` | Không có tài nguyên nào sẽ bị tạo/sửa/xóa. |
| `Note: You didn't use the -out option...` | Bạn chạy plan để xem trên màn hình, chưa lưu plan thành file. Đây là note bình thường. |

Nếu sau này module VPC đã có resource thật, output sẽ chuyển thành dạng:

```text
Plan: 10 to add, 0 to change, 0 to destroy.
```

Khi đó phải đọc kỹ từng resource trước khi `terraform apply`.

Ở cuối bước 3.2, có hai cách đi tiếp:

| Cách | Khi nào dùng | Ý nghĩa |
|---|---|---|
| Dừng sau `terraform plan` | Chỉ muốn kiểm tra bộ khung, backend và đường dẫn tới module. | Chưa ghi output vào state. |
| Chạy `terraform apply` | Muốn lưu output `name_prefix` vào remote state ngay. | Không tạo hạ tầng thật nếu plan chỉ có `Changes to Outputs`. |

Nếu chọn apply, đọc kỹ plan và chỉ tiếp tục khi chắc chắn không có dòng resource AWS nào dạng `will be created`.

### 7. Kiểm tra hoàn thành

Kiểm tra thư mục đã có đủ:

```text
terraform/environments/dev/network
terraform/modules/vpc
```

Kiểm tra bằng lệnh:

```powershell
Get-ChildItem terraform\environments\dev\network
Get-ChildItem terraform\modules\vpc
```

Kết quả mong đợi trong `dev/network`:

```text
backend.tf
locals.tf
main.tf
outputs.tf
providers.tf
README.md
terraform.tfvars
terraform.tfvars.example
variables.tf
versions.tf
```

Kết quả mong đợi trong `modules/vpc`:

```text
main.tf
outputs.tf
README.md
variables.tf
```

Kiểm tra Terraform trong root module:

```powershell
cd C:\code\springboot-learning\terraform
terraform fmt -check -recursive
cd environments\dev\network
terraform init
terraform validate
terraform plan
```

Kết quả mong đợi:

```text
Terraform has been successfully initialized!
Success! The configuration is valid.
Changes to Outputs:
  + name_prefix = "newgate2601-dev"
```

Output trên là bình thường ở lần đầu, vì Terraform muốn lưu output `name_prefix` vào state. Nó chưa tạo resource AWS thật.

Nếu output đã từng được lưu vào state trước đó, `terraform plan` có thể ra:

```text
No changes. Your infrastructure matches the configuration.
```

Nếu chọn `terraform apply` để lưu output, chỉ tiếp tục khi plan không có resource AWS nào dạng `will be created`.

Kiểm tra state object trên S3 sau khi đã apply để ghi state:

```powershell
aws s3 ls s3://newgate2601-terraform-state-150914615641-ap-southeast-1/dev/network/
```

Kết quả mong đợi sau khi state được ghi:

```text
terraform.tfstate
```

Nếu chưa có object state nhưng `terraform init` và `terraform plan` vẫn thành công, chưa chắc là lỗi. Với bộ khung chưa có resource, Terraform có thể chưa cần ghi state có nội dung đáng kể.

### 8. Lỗi thường gặp và cách xử lý

| Lỗi | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| `Module not found` | Sai đường dẫn `source = "../../../modules/vpc"`. | Kiểm tra từ `terraform/environments/dev/network` đi lên 3 cấp rồi vào `modules/vpc`. |
| `Unsupported argument` | Root module truyền biến nhưng module VPC chưa khai báo biến đó. | Thêm biến tương ứng vào `terraform/modules/vpc/variables.tf`. |
| `Reference to undeclared input variable` | Dùng `var.xxx` nhưng chưa khai báo trong `variables.tf`. | Khai báo biến trong root module hoặc module đúng nơi. |
| Terraform hỏi `var.aws_region` | Chưa có `terraform.tfvars` hoặc file thiếu biến. | Copy `terraform.tfvars.example` thành `terraform.tfvars`, điền đủ giá trị. |
| `No valid credential sources found` | AWS credential chưa cấu hình. | Chạy `aws configure` hoặc kiểm tra profile đang dùng. |
| `AccessDenied` khi init backend | IAM user thiếu quyền đọc/ghi S3, DynamoDB hoặc KMS. | Kiểm tra quyền với bucket state, lock table và KMS key. |
| `Error loading state: AccessDenied` | Backend bucket đúng nhưng credential không được đọc state key. | Kiểm tra policy S3/KMS và đúng account/region. |
| `Failed to get existing workspaces` | Backend S3 không truy cập được hoặc sai bucket/region. | Kiểm tra `bucket`, `region`, AWS credential và network. |
| Warning `dynamodb_table` deprecated | Terraform version mới khuyến nghị cơ chế lock mới hơn cho S3 backend. | Ghi nhận warning, bước này vẫn chạy được; refactor backend lock sau nếu muốn đồng bộ theo Terraform mới. |
| `Changes to Outputs` với `name_prefix` | Output mới chưa được lưu vào state. | Đây là bình thường; có thể apply nếu plan không tạo/sửa/xóa resource AWS. |
| Plan báo tạo resource staging/production | Đặt nhầm source, biến hoặc code của môi trường khác vào dev. | Dừng lại, sửa lại phạm vi root module; không apply. |
| `terraform fmt -check` fail | File HCL chưa đúng format. | Chạy `terraform fmt -recursive`, xem file nào thay đổi. |

Lỗi cần đặc biệt chú ý:

```text
Backend key dùng sai hoặc trùng với root module khác
```

Ví dụ nếu `dev/network/backend.tf` vô tình dùng:

```hcl
key = "bootstrap/backend/terraform.tfstate"
```

thì phải sửa ngay thành:

```hcl
key = "dev/network/terraform.tfstate"
```

Không chạy `apply` khi backend key chưa chắc chắn đúng.

### 9. Kết quả sau bước này

Trạng thái sau khi hoàn thành:

```text
[x] Đã có root module đầu tiên: terraform/environments/dev/network
[x] Đã có backend key riêng: dev/network/terraform.tfstate
[x] Đã có provider, variables, locals, outputs và tfvars example cho network dev
[x] Đã có bộ khung module dùng lại: terraform/modules/vpc
[x] Terraform init/validate/plan chạy được cho root module đầu tiên
[x] Chưa tạo staging hoặc production
[x] Chưa tạo EKS/RDS/MSK/ElastiCache
```

Sau bước này, repo đã sẵn sàng để viết resource network thật.

Bước tiếp theo là triển khai nội dung thật cho `modules/vpc` và `environments/dev/network`, gồm:

```text
VPC
public subnet
private application subnet
isolated data subnet
Internet Gateway
NAT Gateway
route table
VPC endpoint
security group nền
VPC Flow Logs
```

Khi bước network dev chạy ổn, các root module sau mới lần lượt dùng output của network:

```text
dev/network
  -> dev/eks
  -> dev/data
  -> dev/observability
```

## Bước 3.3 - Triển khai network dev thật bằng module VPC

### 1. Mục tiêu của bước này

Mục tiêu là biến bộ khung `modules/vpc` ở bước 3.2 thành module VPC có resource AWS thật.

Sau bước này ta có network nền cho môi trường `dev`:

```text
VPC
├── public subnet trên 3 AZ
├── private application subnet trên 3 AZ
└── isolated data subnet trên 3 AZ
```

Kèm theo:

```text
Internet Gateway : cho public subnet đi Internet
NAT Gateway      : cho private subnet đi ra ngoài khi cần
Route table      : điều hướng traffic theo từng loại subnet
VPC endpoint     : đi tới một số AWS service qua private network
Security group   : nhóm rule nền cho endpoint/internal traffic
VPC Flow Logs    : ghi log network để audit và troubleshoot
```

Ở bước này **chưa tạo EKS, RDS, MSK, ElastiCache, staging hoặc production**.

### 2. Trước khi bắt đầu cần có gì

Cần chuẩn bị:

- Đã hoàn thành bước 3.2.
- `terraform/environments/dev/network` đã `terraform init` được.
- `terraform/modules/vpc` đã có bộ khung ban đầu.
- Backend key của network dev là:

```hcl
key = "dev/network/terraform.tfstate"
```

- AWS CLI đang dùng đúng account lab.
- Terraform CLI chạy được.

Kiểm tra nhanh:

```powershell
cd C:\code\springboot-learning\terraform\environments\dev\network
aws sts get-caller-identity
terraform validate
terraform plan
```

Nếu `terraform plan` hiện `Changes to Outputs` với `name_prefix`, đó vẫn là trạng thái bình thường của bộ khung bước 3.2.

### 3. File Terraform cần cập nhật

Cập nhật root module:

```text
terraform/environments/dev/network
```

Gồm:

| File | Vai trò ngắn gọn |
|---|---|
| `variables.tf` | Bổ sung input cho subnet CIDR, NAT và VPC endpoint. |
| `terraform.tfvars.example` | Điền CIDR mẫu cho network dev. |
| `terraform.tfvars` | Điền giá trị thật để chạy local. |
| `main.tf` | Truyền biến mới vào `modules/vpc`. |
| `outputs.tf` | Output VPC id, subnet ids và security group ids cần dùng sau. |

Cập nhật module dùng lại:

```text
terraform/modules/vpc
```

Gồm:

| File | Vai trò ngắn gọn |
|---|---|
| `variables.tf` | Bổ sung input của module VPC. |
| `main.tf` | Tạo VPC, subnet, route table, NAT, endpoint và flow logs. |
| `outputs.tf` | Trả VPC id, subnet ids, route table ids và security group ids. |
| `README.md` | Ghi rõ module tạo gì và chưa tạo gì. |

### 4. Cập nhật input network dev

Mở file:

```text
terraform/environments/dev/network/variables.tf
```

Bổ sung các biến:

```hcl
variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets."
  type        = list(string)
}

variable "private_app_subnet_cidrs" {
  description = "CIDR blocks for private application subnets."
  type        = list(string)
}

variable "isolated_data_subnet_cidrs" {
  description = "CIDR blocks for isolated data subnets."
  type        = list(string)
}

variable "nat_gateway_mode" {
  description = "NAT Gateway mode. Use single for lab cost saving, one_per_az for higher availability."
  type        = string
  default     = "single"

  validation {
    condition     = contains(["single", "one_per_az"], var.nat_gateway_mode)
    error_message = "nat_gateway_mode must be single or one_per_az."
  }
}

variable "enable_vpc_flow_logs" {
  description = "Whether to enable VPC Flow Logs."
  type        = bool
  default     = true
}
```

Giải thích nhanh:

| Biến | Dùng để làm gì |
|---|---|
| `public_subnet_cidrs` | CIDR cho subnet public, nơi đặt NAT Gateway hoặc Load Balancer public sau này. |
| `private_app_subnet_cidrs` | CIDR cho workload private, ví dụ compute layer sau này. |
| `isolated_data_subnet_cidrs` | CIDR cho data layer, không route trực tiếp ra Internet. |
| `nat_gateway_mode` | Chọn tiết kiệm chi phí hoặc tăng khả dụng. |
| `enable_vpc_flow_logs` | Bật/tắt log network. |

### 5. Tạo file input thật

Mở file mẫu:

```text
terraform/environments/dev/network/terraform.tfvars.example
```

Bổ sung CIDR theo VPC `10.20.0.0/16`:

```hcl
public_subnet_cidrs = [
  "10.20.0.0/24",
  "10.20.1.0/24",
  "10.20.2.0/24"
]

private_app_subnet_cidrs = [
  "10.20.10.0/24",
  "10.20.11.0/24",
  "10.20.12.0/24"
]

isolated_data_subnet_cidrs = [
  "10.20.20.0/24",
  "10.20.21.0/24",
  "10.20.22.0/24"
]

nat_gateway_mode     = "single"
enable_vpc_flow_logs = true
```

Ý nghĩa cách chia:

| Nhóm subnet | CIDR mẫu | Mục đích |
|---|---|---|
| Public | `10.20.0.0/24` đến `10.20.2.0/24` | Tài nguyên cần route trực tiếp ra Internet. |
| Private app | `10.20.10.0/24` đến `10.20.12.0/24` | Workload private sau này. |
| Isolated data | `10.20.20.0/24` đến `10.20.22.0/24` | Data layer sau này, không đi Internet trực tiếp. |

Với lab cá nhân, `nat_gateway_mode = "single"` giúp giảm chi phí. Với môi trường cần tính sẵn sàng cao hơn, dùng:

```hcl
nat_gateway_mode = "one_per_az"
```

Sau đó copy sang file chạy thật nếu chưa có:

```powershell
cd C:\code\springboot-learning\terraform\environments\dev\network
Copy-Item terraform.tfvars.example terraform.tfvars
```

Nếu `terraform.tfvars` đã tồn tại, cập nhật thủ công các biến mới vào file đó.

### 6. Cập nhật module VPC

Mở file:

```text
terraform/modules/vpc/variables.tf
```

Bổ sung các biến tương ứng với root module:

```hcl
variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets."
  type        = list(string)
}

variable "private_app_subnet_cidrs" {
  description = "CIDR blocks for private application subnets."
  type        = list(string)
}

variable "isolated_data_subnet_cidrs" {
  description = "CIDR blocks for isolated data subnets."
  type        = list(string)
}

variable "nat_gateway_mode" {
  description = "NAT Gateway mode."
  type        = string
}

variable "enable_vpc_flow_logs" {
  description = "Whether to enable VPC Flow Logs."
  type        = bool
}
```

Sau đó cập nhật root module:

```text
terraform/environments/dev/network/main.tf
```

Truyền biến mới vào module:

```hcl
module "vpc" {
  source = "../../../modules/vpc"

  name_prefix               = local.name_prefix
  vpc_cidr                  = var.vpc_cidr
  availability_zones        = var.availability_zones
  public_subnet_cidrs       = var.public_subnet_cidrs
  private_app_subnet_cidrs  = var.private_app_subnet_cidrs
  isolated_data_subnet_cidrs = var.isolated_data_subnet_cidrs
  nat_gateway_mode          = var.nat_gateway_mode
  enable_vpc_flow_logs      = var.enable_vpc_flow_logs
  tags                      = local.common_tags
}
```

Lưu ý format: nếu Terraform căn lại spacing khác, chạy `terraform fmt` là được.

Trong:

```text
terraform/modules/vpc/main.tf
```

Triển khai các nhóm resource theo thứ tự:

```text
1. VPC
2. Internet Gateway
3. Public subnets
4. Private application subnets
5. Isolated data subnets
6. Elastic IP cho NAT Gateway
7. NAT Gateway
8. Route tables
9. Route table associations
10. VPC endpoints
11. Security group nền cho endpoint/internal traffic
12. VPC Flow Logs
```

Không cần viết tất cả trong một block lớn. Nên chia rõ bằng comment ngắn:

```hcl
# VPC
# Subnets
# Internet egress
# Route tables
# VPC endpoints
# Flow logs
```

Nguyên tắc quan trọng:

- Module không hard-code giá trị riêng của `dev`.
- Public subnet có route `0.0.0.0/0` qua Internet Gateway.
- Private application subnet có route `0.0.0.0/0` qua NAT Gateway nếu cần egress.
- Isolated data subnet không có route `0.0.0.0/0` ra Internet Gateway hoặc NAT Gateway.
- Mọi resource có tag chung từ `var.tags`.
- Output đủ thông tin để root module sau dùng lại.

#### 6.1. Cập nhật outputs

Mở file:

```text
terraform/modules/vpc/outputs.tf
```

Bổ sung output tối thiểu:

```hcl
output "vpc_id" {
  description = "VPC id."
  value       = aws_vpc.this.id
}

output "public_subnet_ids" {
  description = "Public subnet ids."
  value       = aws_subnet.public[*].id
}

output "private_app_subnet_ids" {
  description = "Private application subnet ids."
  value       = aws_subnet.private_app[*].id
}

output "isolated_data_subnet_ids" {
  description = "Isolated data subnet ids."
  value       = aws_subnet.isolated_data[*].id
}
```

Mở file:

```text
terraform/environments/dev/network/outputs.tf
```

Output lại từ module:

```hcl
output "vpc_id" {
  description = "Dev VPC id."
  value       = module.vpc.vpc_id
}

output "public_subnet_ids" {
  description = "Dev public subnet ids."
  value       = module.vpc.public_subnet_ids
}

output "private_app_subnet_ids" {
  description = "Dev private application subnet ids."
  value       = module.vpc.private_app_subnet_ids
}

output "isolated_data_subnet_ids" {
  description = "Dev isolated data subnet ids."
  value       = module.vpc.isolated_data_subnet_ids
}
```

Các output này sẽ được dùng ở những bước sau khi tạo compute layer, data layer và observability.

### 7. Chạy Terraform và kiểm tra hoàn thành

Format toàn bộ Terraform:

```powershell
cd C:\code\springboot-learning\terraform
terraform fmt -recursive
```

Đi vào root module network dev:

```powershell
cd environments\dev\network
```

Chạy init lại để Terraform đọc thay đổi module:

```powershell
terraform init
```

Validate:

```powershell
terraform validate
```

Plan ra file để review:

```powershell
terraform plan -out=tfplan
```

Kết quả mong đợi lúc module VPC đã có resource thật:

```text
Plan: N to add, 0 to change, 0 to destroy.
```

Trong plan phải thấy các nhóm resource network như:

```text
aws_vpc
aws_subnet
aws_internet_gateway
aws_route_table
aws_route_table_association
aws_nat_gateway
aws_vpc_endpoint
aws_cloudwatch_log_group
aws_flow_log
```

Đọc kỹ plan trước khi apply.

Chỉ apply khi:

```text
Không có resource staging
Không có resource production
Không có destroy ngoài ý muốn
Backend key vẫn là dev/network/terraform.tfstate
CIDR và AZ đúng như đã chọn
```

Apply:

```powershell
terraform apply "tfplan"
```

#### 7.1. Kiểm tra sau apply

Kiểm tra output:

```powershell
terraform output
```

Kết quả mong đợi có các giá trị:

```text
vpc_id
public_subnet_ids
private_app_subnet_ids
isolated_data_subnet_ids
```

Kiểm tra VPC:

```powershell
aws ec2 describe-vpcs --filters "Name=tag:Name,Values=newgate2601-dev-vpc" --region ap-southeast-1
```

Kiểm tra subnet:

```powershell
aws ec2 describe-subnets --filters "Name=vpc-id,Values=<vpc-id>" --region ap-southeast-1
```

Kết quả mong đợi:

```text
3 public subnets
3 private application subnets
3 isolated data subnets
```

Kiểm tra route table:

```powershell
aws ec2 describe-route-tables --filters "Name=vpc-id,Values=<vpc-id>" --region ap-southeast-1
```

Cần xác nhận:

```text
Public subnet
  -> có route 0.0.0.0/0 tới Internet Gateway

Private application subnet
  -> có route 0.0.0.0/0 tới NAT Gateway nếu bật egress

Isolated data subnet
  -> không có route 0.0.0.0/0 tới Internet Gateway hoặc NAT Gateway
```

Kiểm tra state:

```powershell
aws s3 ls s3://newgate2601-terraform-state-150914615641-ap-southeast-1/dev/network/
```

Kết quả mong đợi:

```text
terraform.tfstate
```

### 8. Lỗi thường gặp và cách xử lý

| Lỗi | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| `InvalidSubnet.Range` | CIDR subnet nằm ngoài `vpc_cidr`. | Kiểm tra lại các dải `10.20.x.0/24` có thuộc `10.20.0.0/16` không. |
| `CIDR conflicts with another subnet` | Hai subnet bị trùng CIDR. | Mỗi subnet phải có CIDR riêng. |
| `InvalidParameterValue: Value (...) for parameter availabilityZone is invalid` | AZ không tồn tại trong region đang dùng. | Kiểm tra region và AZ bằng `aws ec2 describe-availability-zones`. |
| NAT Gateway tạo lâu hoặc fail | Elastic IP/NAT Gateway cần thời gian hoặc thiếu quota. | Đợi vài phút, kiểm tra quota Elastic IP và NAT Gateway. |
| `AccessDenied` khi tạo Flow Logs | IAM user thiếu quyền CloudWatch Logs/IAM role cho flow logs. | Kiểm tra quyền tạo log group, IAM role và `ec2:CreateFlowLogs`. |
| `Unsupported argument` | Root module truyền biến nhưng module chưa khai báo. | Thêm biến vào `terraform/modules/vpc/variables.tf`. |
| `Reference to undeclared resource` | Output hoặc route đang tham chiếu resource chưa có. | Kiểm tra tên resource trong `main.tf` và `outputs.tf`. |
| Plan có resource staging/production | Code hoặc biến bị đặt nhầm phạm vi. | Dừng lại, sửa về `dev/network`, không apply. |
| Data subnet có route ra Internet | Route table gắn sai hoặc reuse route table private app. | Tách route table riêng cho isolated data subnet. |

Lỗi cần đặc biệt chú ý:

```text
Isolated data subnet không được có default route ra Internet.
```

Nếu thấy route như sau trong route table của data subnet:

```text
0.0.0.0/0 -> igw-...
0.0.0.0/0 -> nat-...
```

thì phải sửa trước khi tiếp tục các bước data service sau.

### 9. Kết quả sau bước này

Trạng thái sau khi hoàn thành:

```text
[x] Đã có VPC dev thật
[x] Đã có 3 public subnets
[x] Đã có 3 private application subnets
[x] Đã có 3 isolated data subnets
[x] Đã có route table đúng cho từng nhóm subnet
[x] Private application subnet có egress theo thiết kế NAT
[x] Isolated data subnet không có default route ra Internet
[x] Đã có VPC endpoint nền cần thiết
[x] Đã bật VPC Flow Logs nếu enable_vpc_flow_logs = true
[x] State network dev lưu ở S3 key dev/network/terraform.tfstate
[x] Chưa tạo EKS/RDS/MSK/ElastiCache
[x] Chưa tạo staging hoặc production
```

Sau bước này, network dev đã đủ nền để các root module sau dùng output:

```text
dev/network
  -> dev/eks
  -> dev/data
  -> dev/observability
```
