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
- CI pipeline sau này có thể dùng cùng backend nếu được cấp quyền phù hợp.
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

## Bước 3.4 - Dựng network shared-services bằng lại module VPC

### 1. Mục tiêu của bước này

Mục tiêu là tạo network riêng cho nhóm tài nguyên dùng chung, gọi là `shared-services`.

Sau bước này ta có thêm một VPC nền để đặt các thành phần platform dùng chung như:

```text
Amazon ECR
CI/CD integration
Observability nền tảng
Artifact/cache/service dùng chung nếu cần
```

Network này tách khỏi network `dev` để tránh trộn lẫn tài nguyên nền tảng với workload ứng dụng.

Trạng thái mong muốn sau bước này:

```text
terraform/environments/shared-services/network
  -> gọi lại terraform/modules/vpc
  -> tạo VPC shared-services thật
  -> lưu state riêng ở shared-services/network/terraform.tfstate
```

Ở bước này **chưa dựng source control/CI self-hosted, chưa dựng ECR, chưa tạo EKS, RDS, MSK hoặc ElastiCache**.

### 2. Vì sao cần làm bước này

Trong kiến trúc doanh nghiệp, các thành phần nền tảng dùng chung không nên nằm chung network với môi trường ứng dụng `dev`.

Lý do:

- ECR là nơi lưu container image dùng cho nhiều môi trường.
- CI/CD cần vùng hạ tầng dùng chung để tích hợp với AWS service mà không phụ thuộc vòng đời của `dev`.
- Observability nền tảng có thể phục vụ nhiều môi trường.
- Các tài nguyên dùng chung có vòng đời khác với tài nguyên `dev`.
- Khi sau này tạo `staging` hoặc `production`, ta không muốn phải dựng lại các thành phần platform dùng chung từ đầu.

Nếu bỏ qua bước này và đặt toàn bộ platform dùng chung vào VPC `dev`, hệ thống sẽ có các rủi ro:

```text
Xóa dev có thể ảnh hưởng thành phần dùng chung
Thay đổi route/security group của dev có thể làm hỏng CI/CD
Khó phân quyền Terraform state theo phạm vi
Khó tách chi phí và audit giữa platform và workload
Khó nâng cấp lên mô hình nhiều account sau này
```

Trong lab cá nhân, vẫn dùng một AWS account để tiết kiệm chi phí. Tuy nhiên ta vẫn tách bằng VPC, backend key, tag và root module để giữ đúng tư duy vận hành.

### 3. Trước khi bắt đầu cần có gì

Cần chuẩn bị:

- Đã hoàn thành bước 3.3.
- Module dùng lại đã có ở:

```text
terraform/modules/vpc
```

- Terraform remote state backend đã hoạt động.
- AWS CLI đang trỏ đúng account lab.
- Region đang dùng là:

```text
ap-southeast-1
```

- Đã biết account id, ví dụ:

```text
150914615641
```

- Đã chọn CIDR không trùng với `dev`.

Network `dev` hiện đang dùng:

```text
10.20.0.0/16
```

Vì vậy network `shared-services` nên dùng dải khác, ví dụ:

```text
10.10.0.0/16
```

Kiểm tra nhanh AWS identity:

```powershell
aws sts get-caller-identity
```

Kết quả phải đúng account lab, không phải account khác.

### 4. Thao tác chi tiết

#### 4.1. Tạo thư mục root module network cho shared-services

Đi vào thư mục Terraform:

```powershell
cd C:\code\springboot-learning\terraform
```

Tạo thư mục:

```powershell
New-Item -ItemType Directory -Force environments\shared-services\network
```

Sau bước này cấu trúc sẽ có:

```text
terraform/
└── environments/
    └── shared-services/
        ├── README.md
        └── network/
```

`network` là root module riêng cho VPC dùng chung.

#### 4.2. Copy bộ khung từ dev network

Vì `shared-services/network` cũng gọi lại module VPC giống `dev/network`, có thể copy bộ khung từ root module dev:

```powershell
Copy-Item environments\dev\network\versions.tf environments\shared-services\network\versions.tf
Copy-Item environments\dev\network\providers.tf environments\shared-services\network\providers.tf
Copy-Item environments\dev\network\variables.tf environments\shared-services\network\variables.tf
Copy-Item environments\dev\network\locals.tf environments\shared-services\network\locals.tf
Copy-Item environments\dev\network\main.tf environments\shared-services\network\main.tf
Copy-Item environments\dev\network\outputs.tf environments\shared-services\network\outputs.tf
Copy-Item environments\dev\network\terraform.tfvars.example environments\shared-services\network\terraform.tfvars.example
```

Không copy file state local, không copy `.terraform`, không copy `tfplan`.

Nếu có các file sau trong `dev/network`, không đưa sang `shared-services/network`:

```text
.terraform/
.terraform.lock.hcl nếu muốn init lại theo root module mới
terraform.tfstate
terraform.tfstate.backup
terraform.tfvars
tfplan
```

Lý do: mỗi root module phải có state và input thật riêng.

#### 4.3. Tạo backend riêng cho shared-services network

Tạo file:

```text
terraform/environments/shared-services/network/backend.tf
```

Nội dung mẫu:

```hcl
terraform {
  backend "s3" {
    bucket         = "newgate2601-terraform-state-150914615641-ap-southeast-1"
    key            = "shared-services/network/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "terraform-state-lock"
    encrypt        = true
    kms_key_id     = "arn:aws:kms:ap-southeast-1:150914615641:key/38aaa237-5b16-4d7e-811c-c634ae35de52"
  }
}
```

Điểm quan trọng nhất là `key` phải khác với `dev/network`:

```hcl
key = "shared-services/network/terraform.tfstate"
```

Không dùng:

```hcl
key = "dev/network/terraform.tfstate"
```

Nếu dùng nhầm key của dev, Terraform có thể đọc nhầm state và tưởng đang quản lý VPC dev.

#### 4.4. Cập nhật input mẫu cho shared-services

Mở file:

```text
terraform/environments/shared-services/network/terraform.tfvars.example
```

Sửa nội dung theo shared-services:

```hcl
aws_region  = "ap-southeast-1"
project     = "newgate2601"
environment = "shared-services"
owner       = "tony"
account_id  = "150914615641"

vpc_cidr = "10.10.0.0/16"

availability_zones = [
  "ap-southeast-1a",
  "ap-southeast-1b",
  "ap-southeast-1c"
]

public_subnet_cidrs = [
  "10.10.0.0/24",
  "10.10.1.0/24",
  "10.10.2.0/24"
]

private_app_subnet_cidrs = [
  "10.10.10.0/24",
  "10.10.11.0/24",
  "10.10.12.0/24"
]

isolated_data_subnet_cidrs = [
  "10.10.20.0/24",
  "10.10.21.0/24",
  "10.10.22.0/24"
]

nat_gateway_mode     = "single"
enable_vpc_flow_logs = true
```

Ý nghĩa cách chọn CIDR:

| Network | CIDR | Vai trò |
|---|---|---|
| `shared-services` | `10.10.0.0/16` | ECR, CI/CD integration, observability và platform dùng chung. |
| `dev` | `10.20.0.0/16` | Workload ứng dụng dev. |

Không để hai VPC dùng cùng CIDR. Nếu sau này cần peering, Transit Gateway hoặc VPN, CIDR trùng nhau sẽ gây lỗi routing.

#### 4.5. Tạo file input thật

Đi vào root module:

```powershell
cd C:\code\springboot-learning\terraform\environments\shared-services\network
```

Copy file mẫu:

```powershell
Copy-Item terraform.tfvars.example terraform.tfvars
```

Mở `terraform.tfvars` và kiểm tra lại:

```text
environment = "shared-services"
vpc_cidr    = "10.10.0.0/16"
```

Nếu account id, bucket name hoặc KMS ARN của bạn khác ví dụ, sửa lại trước khi chạy Terraform.

#### 4.6. Kiểm tra local name prefix

Mở file:

```text
terraform/environments/shared-services/network/locals.tf
```

Nếu đang dùng logic giống `dev/network`, cần bảo đảm tag và name prefix lấy từ biến:

```hcl
locals {
  name_prefix = "${var.project}-${var.environment}"

  common_tags = {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "Terraform"
    Owner       = var.owner
    AccountId   = var.account_id
    Component   = "network"
  }
}
```

Khi `environment = "shared-services"`, resource name sẽ có dạng:

```text
newgate2601-shared-services-vpc
newgate2601-shared-services-public-ap-southeast-1a
newgate2601-shared-services-private-app-ap-southeast-1a
```

Tên này giúp nhìn trên AWS Console biết ngay resource thuộc platform shared-services.

### 5. File/config/lệnh liên quan

Các file cần tạo hoặc sửa:

| File | Trạng thái | Vai trò |
|---|---|---|
| `terraform/environments/shared-services/network/versions.tf` | Tạo mới | Khai báo Terraform/provider version. |
| `terraform/environments/shared-services/network/providers.tf` | Tạo mới | Cấu hình AWS provider. |
| `terraform/environments/shared-services/network/backend.tf` | Tạo mới | Lưu state riêng cho shared-services network. |
| `terraform/environments/shared-services/network/variables.tf` | Tạo mới | Khai báo input cho root module. |
| `terraform/environments/shared-services/network/locals.tf` | Tạo mới | Tạo name prefix và tag chung. |
| `terraform/environments/shared-services/network/main.tf` | Tạo mới | Gọi `modules/vpc`. |
| `terraform/environments/shared-services/network/outputs.tf` | Tạo mới | Trả VPC id và subnet ids. |
| `terraform/environments/shared-services/network/terraform.tfvars.example` | Tạo mới | Input mẫu có thể commit. |
| `terraform/environments/shared-services/network/terraform.tfvars` | Tạo local | Input thật, không nên commit. |

Các lệnh chính:

```powershell
cd C:\code\springboot-learning\terraform
terraform fmt -recursive
cd environments\shared-services\network
terraform init
terraform validate
terraform plan -out=tfplan
terraform apply "tfplan"
```

Các backend key sau bước này:

```text
bootstrap/backend/terraform.tfstate
dev/network/terraform.tfstate
shared-services/network/terraform.tfstate
```

Tách key như vậy giúp mỗi phần hạ tầng có vòng đời riêng.

### 6. Giải thích từng phần quan trọng

#### 6.1. Vì sao không dùng lại state của dev

Terraform state là bản ghi Terraform đang quản lý resource nào.

Nếu `shared-services/network` dùng chung state với `dev/network`, Terraform sẽ không hiểu đây là hai VPC độc lập. Khi đó có thể xảy ra tình huống nguy hiểm:

```text
Bạn muốn tạo VPC shared-services
Terraform lại thấy state của dev
Plan có thể update hoặc destroy nhầm resource dev
```

Vì vậy mỗi root module cần backend key riêng.

#### 6.2. Vì sao `shared-services` cần VPC riêng

`shared-services` là nơi đặt công cụ nền tảng.

Ví dụ:

```text
ECR lưu image
CI/CD integration gọi AWS service
Artifact/cache dùng chung nếu cần
Monitoring đọc log và metric nền
```

Các thành phần này phục vụ nhiều môi trường. Chúng không thuộc riêng `dev`.

Nếu sau này `dev` bị xóa để tiết kiệm chi phí, các thành phần platform dùng chung vẫn nên tồn tại.

#### 6.3. Vì sao vẫn tạo public, private app và isolated data subnet

Platform shared-services vẫn nên có đủ các lớp mạng chuẩn:

```text
Public subnet
  -> đặt public Load Balancer hoặc NAT Gateway

Private application subnet
  -> đặt service nội bộ hoặc controller dùng chung nếu cần

Isolated data subnet
  -> đặt data service dùng chung nếu sau này thật sự cần
```

Với lab tiết kiệm chi phí, có thể chưa dùng đủ mọi subnet ngay. Tuy nhiên tạo cấu trúc đúng từ đầu giúp không phải sửa kiến trúc network khi thêm ECR integration, observability hoặc platform service khác.

#### 6.4. Vì sao chọn `nat_gateway_mode = "single"` cho lab

`single` nghĩa là dùng ít NAT Gateway hơn để giảm chi phí.

Phù hợp khi:

```text
Lab cá nhân
Chưa yêu cầu High Availability thật
Muốn bảo vệ free credit
Chấp nhận nếu AZ chứa NAT Gateway lỗi thì private subnet có thể mất egress
```

Với môi trường doanh nghiệp hoặc production, nên cân nhắc:

```hcl
nat_gateway_mode = "one_per_az"
```

Đổi lại chi phí NAT Gateway sẽ cao hơn.

#### 6.5. Vì sao CIDR shared-services là `10.10.0.0/16`

Ta đang phân vùng đơn giản:

```text
10.10.0.0/16 -> shared-services
10.20.0.0/16 -> dev
10.30.0.0/16 -> staging sau này
10.40.0.0/16 -> production sau này
```

Cách này dễ nhớ, dễ audit và tránh trùng dải mạng.

### 7. Kiểm tra hoàn thành

Format Terraform:

```powershell
cd C:\code\springboot-learning\terraform
terraform fmt -recursive
```

Đi vào root module:

```powershell
cd environments\shared-services\network
```

Init:

```powershell
terraform init
```

Kết quả mong đợi:

```text
Terraform has been successfully initialized!
```

Validate:

```powershell
terraform validate
```

Kết quả mong đợi:

```text
Success! The configuration is valid.
```

Plan:

```powershell
terraform plan -out=tfplan
```

Trước khi apply, kiểm tra kỹ:

```text
Backend key là shared-services/network/terraform.tfstate
Name/tag có shared-services
CIDR là 10.10.0.0/16
Không có resource dev bị change/destroy
Không có resource staging/production
```

Apply:

```powershell
terraform apply "tfplan"
```

Kiểm tra output:

```powershell
terraform output
```

Kết quả mong đợi có:

```text
vpc_id
public_subnet_ids
private_app_subnet_ids
isolated_data_subnet_ids
```

Kiểm tra VPC trên AWS:

```powershell
aws ec2 describe-vpcs --filters "Name=tag:Name,Values=newgate2601-shared-services-vpc" --region ap-southeast-1
```

Kiểm tra subnet:

```powershell
aws ec2 describe-subnets --filters "Name=tag:Environment,Values=shared-services" --region ap-southeast-1
```

Kết quả mong đợi:

```text
3 public subnets
3 private application subnets
3 isolated data subnets
```

Kiểm tra state trên S3:

```powershell
aws s3 ls s3://newgate2601-terraform-state-150914615641-ap-southeast-1/shared-services/network/
```

Kết quả mong đợi:

```text
terraform.tfstate
```

Kiểm tra không đụng state dev:

```powershell
aws s3 ls s3://newgate2601-terraform-state-150914615641-ap-southeast-1/dev/network/
```

State `dev/network/terraform.tfstate` vẫn tồn tại riêng và không bị thay đổi ngoài ý muốn.

### 8. Lỗi thường gặp và cách xử lý

| Lỗi | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| `Backend configuration changed` | Copy backend từ dev sang nhưng chưa init lại. | Chạy `terraform init -reconfigure` trong `shared-services/network`. |
| Plan hiện resource `dev` | Backend key hoặc tfvars vẫn đang dùng giá trị của dev. | Dừng lại, sửa `backend.tf`, `terraform.tfvars`, `environment`. Không apply. |
| `CIDR conflicts with another subnet` | Dải subnet shared-services bị trùng. | Dùng dải `10.10.x.0/24`, không dùng lại `10.20.x.0/24`. |
| Resource name vẫn có `dev` | `terraform.tfvars` hoặc `locals.tf` chưa sửa đúng. | Kiểm tra `environment = "shared-services"` và `name_prefix`. |
| `AccessDenied` khi tạo resource | IAM user lab thiếu quyền EC2/VPC/CloudWatch/IAM liên quan. | Kiểm tra policy của IAM user/group lab. |
| `InvalidParameterValue` về AZ | AZ không đúng region. | Chạy `aws ec2 describe-availability-zones --region ap-southeast-1`. |
| `Error acquiring the state lock` | Terraform run khác đang giữ lock. | Kiểm tra có terminal/pipeline khác đang chạy không, đợi hoặc xử lý lock cẩn thận. |
| NAT Gateway tạo lâu | NAT Gateway và Elastic IP cần thời gian. | Đợi vài phút, kiểm tra quota Elastic IP/NAT Gateway. |
| S3 state không thấy file | Apply chưa thành công hoặc backend key sai. | Kiểm tra output `terraform init`, `terraform state list`, và `backend.tf`. |

Lỗi cần đặc biệt chú ý:

```text
Không được apply nếu plan có destroy hoặc change tài nguyên dev.
```

Nếu thấy Terraform định sửa VPC có tên:

```text
newgate2601-dev-vpc
```

thì đang nhầm state hoặc nhầm input. Phải dừng lại và sửa trước.

### 9. Kết quả sau bước này

Trạng thái sau khi hoàn thành:

```text
[x] Đã có root module terraform/environments/shared-services/network
[x] Đã gọi lại module terraform/modules/vpc
[x] Đã có VPC shared-services thật
[x] Đã có 3 public subnets cho shared-services
[x] Đã có 3 private application subnets cho shared-services
[x] Đã có 3 isolated data subnets cho shared-services
[x] Đã có route table đúng cho từng nhóm subnet
[x] Đã bật VPC Flow Logs nếu enable_vpc_flow_logs = true
[x] State lưu riêng ở shared-services/network/terraform.tfstate
[x] State dev/network vẫn tách riêng
[x] Chưa dựng ECR
[x] Chưa dựng staging hoặc production
```

Sau bước này, phần network nền đã có đủ hai VPC quan trọng:

```text
shared-services/network
  -> dùng cho ECR, CI/CD integration, observability và platform dùng chung

dev/network
  -> dùng cho EKS dev, data service dev, observability dev
```

Bước tiếp theo là chốt lại quyết định không self-host GitLab trong lab AWS credit 100$ để tránh tốn tài nguyên không cần thiết.

## Bước 3.5 - Chốt không self-host GitLab trong lab AWS credit 100$

### 1. Mục tiêu của bước này

Mục tiêu là dừng nhánh GitLab Self-Managed trong bài thực hành chính để tránh tiêu tốn tài nguyên AWS không cần thiết.

Từ thời điểm này, bài lab dùng:

```text
GitLab.com hoặc GitHub
  -> CI pipeline
  -> Amazon ECR
  -> GitOps repository
  -> Argo CD
  -> Amazon EKS
```

Phần GitLab Self-Managed đã được chuyển sang tài liệu riêng:

```text
AWS/ARCHIVE-GITLAB-SELF-MANAGED-STEP-BY-STEP.md
```

### 2. Quyết định kiến trúc

Không dựng các tài nguyên GitLab Self-Managed trong account lab:

- Không tạo EC2 GitLab application.
- Không tạo EC2 Gitaly.
- Không tạo RDS PostgreSQL riêng cho GitLab.
- Không tạo ElastiCache Redis riêng cho GitLab.
- Không tạo ALB riêng cho GitLab.
- Không tạo S3 buckets GitLab artifacts/uploads/LFS/backups.
- Không tạo Terraform root module `shared-services/gitlab` trong luồng chính.
- Không tạo module `gitlab-self-managed` trong luồng chính.

Lý do: mục tiêu hiện tại là học DevOps deployment topology cho hệ thống Spring Boot trên AWS, không phải học vận hành GitLab như một platform riêng.

### 3. Những gì vẫn giữ nguyên

Các phần không thuộc GitLab vẫn giữ nguyên:

- Terraform backend và remote state.
- `shared-services/network`.
- `dev/network`.
- Module VPC dùng chung.
- Budget/cost alert.
- CloudTrail audit.
- IAM lab account setup.

`shared-services` vẫn có giá trị cho các thành phần dùng chung sau này như ECR, CI/CD integration, artifact/cache hoặc observability. Chỉ bỏ riêng nhánh GitLab Self-Managed.

### 4. Cổng nghiệm thu sau khi dọn GitLab

Hoàn thành khi:

```text
[x] Terraform state shared-services/gitlab không còn resource GitLab
[x] Không còn root module terraform/environments/shared-services/gitlab trong luồng chính
[x] Không còn module terraform/modules/gitlab-self-managed trong luồng chính
[x] Step-by-step chính không còn hướng dẫn dựng GitLab Self-Managed
[x] Network/backend/dev/shared-services khác được giữ nguyên
```

Bước tiếp theo nên làm là chuẩn bị source control/CI bằng GitLab.com hoặc GitHub, sau đó tạo ECR và pipeline build image cho các service Spring Boot.

## Bước 3.6 - Triển khai Amazon ECR dùng chung

### 1. Mục tiêu của bước này

Mục tiêu là tạo lớp **container registry dùng chung** bằng Amazon Elastic Container Registry.

Sau bước này, account lab có các ECR repository để lưu image của 3 service:

```text
gateway
uaa-service
post-service
```

ECR nằm trong phạm vi `shared-services` vì image không thuộc riêng một môi trường `dev`, `staging` hay `production`.

Luồng sau khi hoàn thành:

```text
Developer
  -> GitLab.com hoặc GitHub
  -> CI build image
  -> push image vào Amazon ECR
  -> GitOps dùng image digest để deploy vào EKS
```

Ở bước này **chỉ tạo ECR**.

Không tạo:

- EKS.
- RDS.
- Redis/Valkey.
- Kafka/MSK.
- Argo CD.
- GitLab Self-Managed.
- GitLab Runner self-hosted.

### 2. Vì sao cần làm bước này

Trong topology doanh nghiệp, CI không nên build image rồi deploy trực tiếp bằng file local trên máy.

Image cần một registry trung tâm để:

- Lưu image theo version/digest.
- Cho EKS pull image khi Argo CD sync.
- Cho phép trace từ commit tới image digest.
- Áp lifecycle policy để không giữ image cũ mãi.
- Bật image scan để phát hiện lỗ hổng dependency/container.
- Tách rõ trách nhiệm: CI build/push image, Argo CD deploy image đã có.

ECR là dịch vụ phù hợp vì:

- Tích hợp IAM với AWS.
- EKS pull image từ ECR thuận lợi.
- Không cần tự vận hành registry.
- Chi phí thấp nếu image ít và có lifecycle policy.

Điểm quan trọng:

```text
Không deploy tag latest.
Không sửa image sau khi đã promote.
Không build lại image khi promote dev -> staging -> production.
```

Thứ dùng để deploy phải là digest:

```text
gateway@sha256:...
uaa-service@sha256:...
post-service@sha256:...
```

### 3. Trước khi bắt đầu cần có gì

Cần chuẩn bị:

- Đã hoàn thành bước 3.5.
- Terraform remote state backend đã hoạt động.
- AWS CLI đang trỏ đúng account lab.
- Region đang dùng là:

```text
ap-southeast-1
```

- Đã biết account id:

```text
150914615641
```

- Chưa có root module:

```text
terraform/environments/shared-services/ecr
```

- Chưa có module:

```text
terraform/modules/ecr
```

Kiểm tra nhanh:

```powershell
aws sts get-caller-identity
```

Kết quả phải đúng account lab.

### 4. Thao tác chi tiết

#### 4.1. Tạo module dùng lại `modules/ecr`

Đi vào thư mục Terraform:

```powershell
cd C:\code\springboot-learning\terraform
```

Tạo thư mục:

```powershell
New-Item -ItemType Directory -Force modules\ecr
```

Module này chỉ chứa logic tạo ECR repository và lifecycle policy. Không hard-code tên project, account hoặc environment trong module.

Tạo file:

```text
terraform/modules/ecr/variables.tf
```

Nội dung:

```hcl
variable "name_prefix" {
  description = "Prefix used for ECR repository names."
  type        = string
}

variable "repositories" {
  description = "ECR repositories to create."
  type = map(object({
    image_tag_mutability = optional(string, "IMMUTABLE")
    scan_on_push         = optional(bool, true)
    keep_last_images     = optional(number, 20)
  }))
}

variable "tags" {
  description = "Tags applied to ECR resources."
  type        = map(string)
}
```

Tạo file:

```text
terraform/modules/ecr/main.tf
```

Nội dung:

```hcl
resource "aws_ecr_repository" "this" {
  for_each = var.repositories

  name                 = "${var.name_prefix}/${each.key}"
  image_tag_mutability = each.value.image_tag_mutability

  image_scanning_configuration {
    scan_on_push = each.value.scan_on_push
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = merge(var.tags, {
    Name       = "${var.name_prefix}/${each.key}"
    Repository = each.key
  })
}

resource "aws_ecr_lifecycle_policy" "this" {
  for_each = var.repositories

  repository = aws_ecr_repository.this[each.key].name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep only the most recent ${each.value.keep_last_images} images for lab cost control."
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = each.value.keep_last_images
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}
```

Tạo file:

```text
terraform/modules/ecr/outputs.tf
```

Nội dung:

```hcl
output "repository_urls" {
  description = "ECR repository URLs by logical repository name."
  value = {
    for name, repo in aws_ecr_repository.this : name => repo.repository_url
  }
}

output "repository_arns" {
  description = "ECR repository ARNs by logical repository name."
  value = {
    for name, repo in aws_ecr_repository.this : name => repo.arn
  }
}

output "repository_names" {
  description = "ECR repository names by logical repository name."
  value = {
    for name, repo in aws_ecr_repository.this : name => repo.name
  }
}
```

Tạo file:

```text
terraform/modules/ecr/README.md
```

Nội dung ngắn:

```markdown
# ECR Module

Creates shared Amazon ECR repositories for application container images.

- Immutable image tags.
- Scan on push.
- AES256 encryption.
- Lifecycle policy to keep only a limited number of images.

Images should be deployed by digest from GitOps, not by the `latest` tag.
```

#### 4.2. Tạo root module `shared-services/ecr`

Tạo thư mục:

```powershell
New-Item -ItemType Directory -Force environments\shared-services\ecr
```

Root module này có state riêng:

```text
shared-services/ecr/terraform.tfstate
```

Không để ECR chung state với network. ECR có vòng đời khác VPC:

```text
Network có thể giữ lâu
ECR có thể thêm/xóa repo theo service
CI/CD chỉ cần quyền vào ECR, không cần quyền sửa VPC
```

#### 4.3. Tạo file version và provider

Tạo file:

```text
terraform/environments/shared-services/ecr/versions.tf
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

Tạo file:

```text
terraform/environments/shared-services/ecr/providers.tf
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

#### 4.4. Tạo backend riêng cho ECR

Tạo file:

```text
terraform/environments/shared-services/ecr/backend.tf
```

Nội dung:

```hcl
terraform {
  backend "s3" {
    bucket         = "newgate2601-terraform-state-150914615641-ap-southeast-1"
    key            = "shared-services/ecr/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "terraform-state-lock"
    encrypt        = true
    kms_key_id     = "arn:aws:kms:ap-southeast-1:150914615641:key/38aaa237-5b16-4d7e-811c-c634ae35de52"
  }
}
```

Điểm cần kiểm tra kỹ:

```hcl
key = "shared-services/ecr/terraform.tfstate"
```

Không dùng nhầm:

```hcl
key = "shared-services/network/terraform.tfstate"
key = "dev/network/terraform.tfstate"
```

#### 4.5. Tạo variables, locals và main

Tạo file:

```text
terraform/environments/shared-services/ecr/variables.tf
```

Nội dung:

```hcl
variable "aws_region" {
  description = "AWS region for shared ECR resources."
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
  description = "AWS account id used for tagging."
  type        = string
}

variable "repositories" {
  description = "ECR repositories to create for Spring Boot services."
  type = map(object({
    image_tag_mutability = optional(string, "IMMUTABLE")
    scan_on_push         = optional(bool, true)
    keep_last_images     = optional(number, 20)
  }))
}
```

Tạo file:

```text
terraform/environments/shared-services/ecr/locals.tf
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
    Component   = "ecr"
    AccountId   = var.account_id
  }
}
```

Tạo file:

```text
terraform/environments/shared-services/ecr/main.tf
```

Nội dung:

```hcl
module "ecr" {
  source = "../../../modules/ecr"

  name_prefix  = local.name_prefix
  repositories = var.repositories
  tags         = local.common_tags
}
```

Tạo file:

```text
terraform/environments/shared-services/ecr/outputs.tf
```

Nội dung:

```hcl
output "repository_urls" {
  description = "ECR repository URLs by logical repository name."
  value       = module.ecr.repository_urls
}

output "repository_arns" {
  description = "ECR repository ARNs by logical repository name."
  value       = module.ecr.repository_arns
}

output "repository_names" {
  description = "ECR repository names by logical repository name."
  value       = module.ecr.repository_names
}
```

#### 4.6. Tạo input mẫu và input thật

Tạo file:

```text
terraform/environments/shared-services/ecr/terraform.tfvars.example
```

Nội dung:

```hcl
aws_region  = "ap-southeast-1"
project     = "newgate2601"
environment = "shared-services"
owner       = "tony"
account_id  = "150914615641"

repositories = {
  gateway = {
    image_tag_mutability = "IMMUTABLE"
    scan_on_push         = true
    keep_last_images     = 20
  }
  uaa-service = {
    image_tag_mutability = "IMMUTABLE"
    scan_on_push         = true
    keep_last_images     = 20
  }
  post-service = {
    image_tag_mutability = "IMMUTABLE"
    scan_on_push         = true
    keep_last_images     = 20
  }
}
```

Copy thành input thật:

```powershell
cd C:\code\springboot-learning\terraform\environments\shared-services\ecr
Copy-Item terraform.tfvars.example terraform.tfvars
```

Nếu project, owner hoặc account id khác, sửa `terraform.tfvars` trước khi chạy.

Tên repository thực tế sẽ có dạng:

```text
newgate2601-shared-services/gateway
newgate2601-shared-services/uaa-service
newgate2601-shared-services/post-service
```

#### 4.7. Mốc dừng trước khi tự chạy Terraform

Đến đây là kết thúc phần **chuẩn bị file/config** cho bước 3.6.

Trạng thái mong đợi trên máy local:

```text
[x] Đã có terraform/modules/ecr/variables.tf
[x] Đã có terraform/modules/ecr/main.tf
[x] Đã có terraform/modules/ecr/outputs.tf
[x] Đã có terraform/modules/ecr/README.md
[x] Đã có terraform/environments/shared-services/ecr/backend.tf
[x] Đã có terraform/environments/shared-services/ecr/versions.tf
[x] Đã có terraform/environments/shared-services/ecr/providers.tf
[x] Đã có terraform/environments/shared-services/ecr/variables.tf
[x] Đã có terraform/environments/shared-services/ecr/locals.tf
[x] Đã có terraform/environments/shared-services/ecr/main.tf
[x] Đã có terraform/environments/shared-services/ecr/outputs.tf
[x] Đã có terraform/environments/shared-services/ecr/terraform.tfvars.example
[x] Đã có terraform/environments/shared-services/ecr/terraform.tfvars
```

Ở mốc này **chưa có ECR repository nào được tạo trên AWS** nếu chưa chạy Terraform.

Các bước tiếp theo từ `terraform fmt`, `terraform init`, `terraform validate`, `terraform plan` tới `terraform apply` là phần tự chạy ở mục kiểm tra hoàn thành bên dưới.

### 5. File/config/lệnh liên quan

Các file cần tạo:

| File | Vai trò |
|---|---|
| `terraform/modules/ecr/variables.tf` | Input module ECR. |
| `terraform/modules/ecr/main.tf` | Tạo ECR repository và lifecycle policy. |
| `terraform/modules/ecr/outputs.tf` | Trả repository URL, ARN và name. |
| `terraform/modules/ecr/README.md` | Ghi phạm vi module. |
| `terraform/environments/shared-services/ecr/backend.tf` | State riêng cho ECR. |
| `terraform/environments/shared-services/ecr/versions.tf` | Terraform/provider version. |
| `terraform/environments/shared-services/ecr/providers.tf` | AWS provider. |
| `terraform/environments/shared-services/ecr/variables.tf` | Input root module. |
| `terraform/environments/shared-services/ecr/locals.tf` | Name prefix và tag chung. |
| `terraform/environments/shared-services/ecr/main.tf` | Gọi module ECR. |
| `terraform/environments/shared-services/ecr/outputs.tf` | Expose output ECR. |
| `terraform/environments/shared-services/ecr/terraform.tfvars.example` | Input mẫu có thể commit. |
| `terraform/environments/shared-services/ecr/terraform.tfvars` | Input thật cho lab. |

Các lệnh chính:

```powershell
cd C:\code\springboot-learning\terraform
terraform fmt -recursive

cd C:\code\springboot-learning\terraform\environments\shared-services\ecr
terraform init
terraform validate
terraform plan -out=tfplan
terraform apply "tfplan"
```

### 6. Giải thích từng phần quan trọng

#### 6.1. Vì sao ECR nằm trong `shared-services`

Image được build một lần rồi dùng cho nhiều môi trường:

```text
CI build gateway@sha256:abc
  -> dev dùng digest đó
  -> staging promote đúng digest đó
  -> production promote đúng digest đó
```

Nếu ECR nằm riêng trong `dev`, khi destroy dev để tiết kiệm chi phí có thể làm mất image đã build. Vì vậy ECR là tài nguyên dùng chung.

#### 6.2. Vì sao ECR có state riêng

ECR không phụ thuộc VPC. Nếu để chung state với network, mỗi lần sửa repository hoặc lifecycle policy sẽ kéo theo rủi ro đụng state VPC.

Tách state giúp:

- CI/CD sau này có thể plan/apply ECR mà không có quyền sửa network.
- Cleanup ECR độc lập với VPC.
- Dễ audit chi phí và owner.

#### 6.3. Vì sao bật immutable tag

Nếu tag có thể bị ghi đè, cùng một tag có thể trỏ tới hai image khác nhau ở hai thời điểm.

Ví dụ lỗi:

```text
gateway:dev hôm qua -> sha256:aaa
gateway:dev hôm nay -> sha256:bbb
```

Khi debug incident, rất khó biết production từng chạy image nào.

Vì vậy:

```text
Tag có thể dùng để tra cứu
Digest mới là giá trị deploy
```

#### 6.4. Vì sao có lifecycle policy

ECR tính phí theo dung lượng lưu trữ image. Lab build nhiều lần sẽ sinh nhiều image.

Lifecycle policy:

```text
Giữ 20 image mới nhất mỗi repository
Xóa image cũ hơn
```

Con số 20 đủ cho lab rollback gần đây, nhưng không giữ rác mãi.

#### 6.5. Vì sao dùng AES256 thay vì KMS riêng

Với lab cá nhân, ECR encryption `AES256` là mặc định hợp lý:

- Ít cấu hình hơn.
- Không cần tạo thêm KMS key riêng cho ECR.
- Đủ cho bước học registry, immutable image và GitOps digest.

Khi nâng cấp production, có thể đổi sang KMS key riêng nếu yêu cầu kiểm soát key chặt hơn.

### 7. Kiểm tra hoàn thành

Format Terraform:

```powershell
cd C:\code\springboot-learning\terraform
terraform fmt -recursive
```

Đi vào root module:

```powershell
cd C:\code\springboot-learning\terraform\environments\shared-services\ecr
```

Init:

```powershell
terraform init
```

Kết quả mong đợi:

```text
Terraform has been successfully initialized!
```

Validate:

```powershell
terraform validate
```

Kết quả mong đợi:

```text
Success! The configuration is valid.
```

Plan:

```powershell
terraform plan -out=tfplan
```

Plan chỉ nên tạo các nhóm resource:

```text
aws_ecr_repository
aws_ecr_lifecycle_policy
```

Với 3 repository, kết quả mong đợi:

```text
Plan: 6 to add, 0 to change, 0 to destroy.
```

Không apply nếu plan có:

```text
aws_vpc
aws_subnet
aws_nat_gateway
aws_eks_cluster
aws_db_instance
aws_elasticache_*
aws_msk_*
```

Apply:

```powershell
terraform apply "tfplan"
```

Sau apply, xem output:

```powershell
terraform output repository_urls
```

Kết quả mong đợi có dạng:

```text
gateway      = "150914615641.dkr.ecr.ap-southeast-1.amazonaws.com/newgate2601-shared-services/gateway"
uaa-service  = "150914615641.dkr.ecr.ap-southeast-1.amazonaws.com/newgate2601-shared-services/uaa-service"
post-service = "150914615641.dkr.ecr.ap-southeast-1.amazonaws.com/newgate2601-shared-services/post-service"
```

Kiểm tra bằng AWS CLI:

```powershell
aws ecr describe-repositories --region ap-southeast-1 --query "repositories[?contains(repositoryName, 'newgate2601-shared-services')].[repositoryName,imageTagMutability,imageScanningConfiguration.scanOnPush]" --output table
```

Kết quả mong đợi:

```text
newgate2601-shared-services/gateway       IMMUTABLE  True
newgate2601-shared-services/uaa-service   IMMUTABLE  True
newgate2601-shared-services/post-service  IMMUTABLE  True
```

Kiểm tra lifecycle policy:

```powershell
aws ecr get-lifecycle-policy --repository-name newgate2601-shared-services/gateway --region ap-southeast-1
```

Kết quả mong đợi có:

```text
imageCountMoreThan
countNumber: 20
expire
```

Kiểm tra state trên S3:

```powershell
aws s3 ls s3://newgate2601-terraform-state-150914615641-ap-southeast-1/shared-services/ecr/
```

Kết quả mong đợi:

```text
terraform.tfstate
```

### 8. Lỗi thường gặp và cách xử lý

| Lỗi | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| `RepositoryAlreadyExistsException` | Repository đã được tạo tay hoặc state bị mất. | Import vào state hoặc đổi tên sau khi kiểm tra kỹ. Không tạo trùng bằng tay. |
| `InvalidParameterException` về repository name | Tên repository có ký tự không hợp lệ. | Dùng chữ thường, số, dấu gạch ngang và slash hợp lệ như `project/env/service`. |
| Plan muốn tạo VPC/RDS/EKS | Đang chạy nhầm thư mục hoặc backend. | Dừng lại, kiểm tra `pwd`, `backend.tf`, `terraform state list`. |
| `AccessDeniedException` | IAM user thiếu quyền ECR. | Bổ sung quyền ECR cần thiết cho lab admin hoặc role chạy Terraform. |
| `Error acquiring the state lock` | Có Terraform run khác đang giữ lock. | Đợi run kia xong, không force-unlock nếu chưa chắc. |
| CI push bị `no basic auth credentials` | Chưa login Docker vào ECR. | Dùng `aws ecr get-login-password` trước khi push. |
| Push tag cũ bị lỗi | ECR đang immutable tag. | Đây là đúng. Tạo tag mới hoặc dùng commit SHA, không ghi đè tag cũ. |

Lỗi cần đặc biệt chú ý:

```text
Không dùng AWS Console tạo/sửa repository sau khi Terraform đã quản lý.
```

Nếu sửa tay trên Console, lần plan sau có thể drift.

### 9. Kết quả sau bước này

Trạng thái sau khi hoàn thành:

```text
[x] Đã có module terraform/modules/ecr
[x] Đã có root module terraform/environments/shared-services/ecr
[x] State ECR lưu riêng ở shared-services/ecr/terraform.tfstate
[x] Đã tạo ECR repository cho gateway
[x] Đã tạo ECR repository cho uaa-service
[x] Đã tạo ECR repository cho post-service
[x] ECR repository bật immutable tag
[x] ECR repository bật scan on push
[x] ECR repository có lifecycle policy giữ 20 image mới nhất
[x] Chưa tạo EKS
[x] Chưa tạo RDS
[x] Chưa tạo Redis/Valkey
[x] Chưa tạo Kafka/MSK
[x] Chưa tạo Argo CD
```

Sau bước này, bài lab có registry dùng chung:

```text
shared-services/ecr
  -> gateway image
  -> uaa-service image
  -> post-service image
```

Bước tiếp theo nên làm là chuẩn bị source control và CI SaaS để build image, login ECR, push image và ghi lại image digest cho GitOps.

## Bước 3.7 - Chuẩn bị source control và CI SaaS build/push image

### 1. Mục tiêu của bước này

Mục tiêu là chuẩn bị repo ứng dụng để CI SaaS có thể:

```text
Pull/Merge Request
  -> compile
  -> test
  -> package JAR

Merge main/develop
  -> build Docker image
  -> push image vào Amazon ECR
  -> xuất image digest để GitOps dùng ở bước sau
```

Ở bước này ta chuẩn bị file/config trong 3 source repo ứng dụng.

Ba repo chuẩn của bài lab:

```text
https://gitlab.com/newgate2601/social-media-app-gateway
https://gitlab.com/newgate2601/social-media-app-uaa
https://gitlab.com/newgate2601/social-media-app-post
```

Mỗi repo cần có:

```text
Dockerfile
.dockerignore
.gitlab-ci.yml
src/main/resources/application-k8s.yaml
k8s/deployment.yaml
```

đủ để GitLab CI build Spring Boot app thành container image, push vào đúng ECR repository và chuẩn bị manifest Kubernetes cho bước EKS/GitOps sau.

Ở bước này **chưa tạo EKS, chưa cài Argo CD và chưa deploy ứng dụng**.

### 2. Vì sao cần làm bước này

Sau bước 3.6, ECR là nơi lưu image. Nhưng ECR chỉ là registry, không tự build image.

Cần CI SaaS để tự động hóa luồng:

```text
Developer push code
  -> GitLab CI chạy test
  -> GitLab CI build JAR
  -> GitLab CI build Docker image
  -> GitLab CI push image vào ECR
  -> GitLab CI ghi lại image digest
```

Không nên build image thủ công trên máy cá nhân rồi push bằng tay cho mỗi lần deploy, vì:

- Khó trace image được build từ commit nào.
- Dễ quên chạy test.
- Dễ push nhầm tag.
- Khó rollback theo digest.
- Không phù hợp GitOps sau này.

Điểm quan trọng:

```text
Git giữ source code.
GitLab CI build/test/package.
ECR giữ image.
GitOps dùng image digest.
Argo CD deploy image đó vào EKS ở bước sau.
```

### 3. Trước khi bắt đầu cần có gì

Cần chuẩn bị:

- Đã hoàn thành phần chuẩn bị file của bước 3.6.
- ECR repository sẽ được tạo bằng Terraform ở bước 3.6 trước khi chạy pipeline push image.
- Đã tạo tay 3 GitLab project private, trống, không initialize README.
- 3 local repo đang trỏ remote `origin` về GitLab hoặc có remote `gitlab` riêng.
- 3 local repo đã commit/push branch `staging` lên GitLab.
- GitLab project có quyền chạy CI/CD pipeline.
- Dockerfile nằm ở root từng repo.
- AWS account lab vẫn là:

```text
150914615641
```

- Region vẫn là:

```text
ap-southeast-1
```

Mapping repo source sang ECR repository:

| Local repo | GitLab project | ECR repository |
|---|---|---|
| `C:\code\social-media-app\social-media-app-gateway` | `newgate2601/social-media-app-gateway` | `newgate2601-shared-services/gateway` |
| `C:\code\social-media-app\social-media-app-uaa` | `newgate2601/social-media-app-uaa` | `newgate2601-shared-services/uaa-service` |
| `C:\code\social-media-app\social-media-app-post` | `newgate2601/social-media-app-post` | `newgate2601-shared-services/post-service` |

Không còn repo/service `service-registry` trong luồng AWS/EKS. Kubernetes Service Discovery thay thế Eureka Registry.

Với repo gateway, pipeline push image vào repository:

```text
newgate2601-shared-services/gateway
```

Với repo UAA, pipeline phải dùng:

```text
newgate2601-shared-services/uaa-service
```

Với repo Post, pipeline phải dùng:

```text
newgate2601-shared-services/post-service
```

### 4. Thao tác chi tiết

#### 4.0. Tạo GitLab project và nối remote cho 3 repo

Trên GitLab.com, tạo tay 3 project private:

```text
newgate2601/social-media-app-gateway
newgate2601/social-media-app-uaa
newgate2601/social-media-app-post
```

Khi tạo project:

- Chọn namespace `newgate2601`.
- Visibility: `Private`.
- Không tick `Initialize repository with a README`.
- Không bật SAST/Secret Detection ở bước tạo project nếu muốn giữ pipeline tối giản trước.

Nếu local repo cũ đang trỏ `origin` về GitHub và muốn chuyển hẳn sang GitLab, chạy trong từng repo:

```powershell
git remote remove origin
git remote add origin https://gitlab.com/newgate2601/social-media-app-gateway.git
git push -u origin staging
```

Repo UAA:

```powershell
cd C:\code\social-media-app\social-media-app-uaa
git remote remove origin
git remote add origin https://gitlab.com/newgate2601/social-media-app-uaa.git
git push -u origin staging
```

Repo Post:

```powershell
cd C:\code\social-media-app\social-media-app-post
git remote remove origin
git remote add origin https://gitlab.com/newgate2601/social-media-app-post.git
git push -u origin staging
```

Nếu muốn giữ GitHub cũ để tham chiếu, không xóa `origin`; thêm GitLab bằng remote riêng:

```powershell
git remote add gitlab https://gitlab.com/newgate2601/social-media-app-gateway.git
git push -u gitlab staging
```

Sau khi push, kiểm tra:

```powershell
git remote -v
git status
git branch -vv
```

Kết quả mong đợi:

```text
Branch staging đã tracking remote GitLab
Working tree clean
GitLab project không còn empty repository
```

#### 4.1. Chuẩn bị Dockerfile chạy kiểu production image

Mở file:

```text
Dockerfile
```

Nội dung nên là multi-stage build. Ví dụ cho `gateway`:

```dockerfile
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /workspace/target/*.jar app.jar

USER spring:spring

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Ý nghĩa:

- Stage `build` dùng Maven + JDK để package JAR.
- Stage runtime chỉ dùng JRE nhỏ hơn.
- Container chạy bằng user `spring`, không chạy bằng root.
- App expose port khớp `server.port` của từng service.
- Image cuối không cần giữ toàn bộ Maven cache/source build trung gian.

Port theo repo:

| Repo | Port |
|---|---:|
| `social-media-app-gateway` | `8081` |
| `social-media-app-uaa` | `8082` |
| `social-media-app-post` | `8088` |

#### 4.2. Chuẩn bị GitLab CI pipeline cho từng repo

Mở file:

```text
.gitlab-ci.yml
```

Đặt file này ở root của cả 3 repo.

Điểm khác nhau duy nhất giữa 3 repo là biến `ECR_REPOSITORY_NAME`:

| Repo | `ECR_REPOSITORY_NAME` |
|---|---|
| `social-media-app-gateway` | `newgate2601-shared-services/gateway` |
| `social-media-app-uaa` | `newgate2601-shared-services/uaa-service` |
| `social-media-app-post` | `newgate2601-shared-services/post-service` |

Mẫu nội dung cho repo gateway:

```yaml
stages:
  - test
  - package
  - image

variables:
  AWS_REGION: "ap-southeast-1"
  AWS_ACCOUNT_ID: "150914615641"
  ECR_REGISTRY: "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
  ECR_REPOSITORY_NAME: "newgate2601-shared-services/gateway"
  IMAGE_TAG: "${CI_COMMIT_SHORT_SHA}-${CI_PIPELINE_IID}"
  MAVEN_CLI_OPTS: "-B"

test:
  stage: test
  image: maven:3.9.9-eclipse-temurin-21
  script:
    - mvn $MAVEN_CLI_OPTS -DskipTests compile

package:
  stage: package
  image: maven:3.9.9-eclipse-temurin-21
  script:
    - mvn $MAVEN_CLI_OPTS -DskipTests clean package
  artifacts:
    paths:
      - target/*.jar
    expire_in: 1 hour

build-image:
  stage: image
  image: docker:27
  services:
    - name: docker:27-dind
      command: ["--tls=false"]
  variables:
    DOCKER_HOST: tcp://docker:2375
    DOCKER_TLS_CERTDIR: ""
  before_script:
    - apk add --no-cache aws-cli
    - aws --version
    - docker --version
  script:
    - export IMAGE_URI="${ECR_REGISTRY}/${ECR_REPOSITORY_NAME}:${IMAGE_TAG}"
    - aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$ECR_REGISTRY"
    - docker build --pull -t "$IMAGE_URI" .
    - docker push "$IMAGE_URI"
    - export IMAGE_DIGEST="$(aws ecr describe-images --repository-name "$ECR_REPOSITORY_NAME" --image-ids imageTag="$IMAGE_TAG" --region "$AWS_REGION" --query 'imageDetails[0].imageDigest' --output text)"
    - echo "IMAGE_URI=${ECR_REGISTRY}/${ECR_REPOSITORY_NAME}@${IMAGE_DIGEST}" | tee image.env
    - echo "IMAGE_TAG=${IMAGE_TAG}" | tee -a image.env
  artifacts:
    reports:
      dotenv: image.env
    paths:
      - image.env
    expire_in: 7 days
  rules:
    - if: '$CI_COMMIT_BRANCH == "main"'
    - if: '$CI_COMMIT_BRANCH == "staging"'
    - if: '$CI_COMMIT_BRANCH == "develop"'
```

Pipeline này gồm 3 stage:

| Stage | Mục đích |
|---|---|
| `test` | Smoke check compile để pipeline không phụ thuộc DB thật khi chưa dựng môi trường test riêng. |
| `package` | Build JAR và lưu artifact ngắn hạn. |
| `image` | Build Docker image, push ECR và xuất digest. |

Lý do tạm thời chưa chạy `mvn test`: `uaa-service` và `post-service` có `@SpringBootTest` load JPA context, cần PostgreSQL. Trước khi có test profile bằng H2/Testcontainers hoặc service container PostgreSQL trong CI, chạy `mvn test` sẽ fail dù code compile được.

Tạo thêm file:

```text
.dockerignore
```

Nội dung tối thiểu:

```text
.git
.gitignore
.gitlab-ci.yml
.idea
target
*.iml
*.log
k8s
```

File này giúp Docker build context nhỏ hơn, không đưa Git history, output `target` cũ hoặc manifest Kubernetes vào image build.

#### 4.2.1. Chuẩn bị Kubernetes profile cho 3 repo

Mỗi service cần profile riêng cho Kubernetes:

```text
src/main/resources/application-k8s.yaml
```

Nguyên tắc:

- Không dùng Eureka.
- Không hard-code secret thật.
- Datasource/Redis/URL service lấy từ environment variable.
- Bật health probe cho Kubernetes.

Gateway khác UAA/Post ở chỗ Gateway cần Kubernetes discovery để tự tìm service được label:

```yaml
spring:
  cloud:
    kubernetes:
      discovery:
        enabled: true
        service-labels:
          "gateway.discovery/enabled": "true"
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
```

UAA/Post không cần đăng ký vào registry nào. Kubernetes Service object đã là service discovery.

#### 4.2.2. Chuẩn bị Kubernetes manifest tối thiểu cho 3 repo

Mỗi repo cần folder:

```text
k8s/
  deployment.yaml
```

UAA và Post cần thêm secret mẫu:

```text
k8s/
  secret.example.yaml
```

Trong `deployment.yaml`, luôn dùng profile `k8s`:

```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: k8s
```

Service nào muốn Gateway tự route tới thì gắn label trên Kubernetes Service:

```yaml
metadata:
  labels:
    gateway.discovery/enabled: "true"
```

Gateway cần ServiceAccount/Role/RoleBinding để đọc `services`, `endpoints`, `pods`, `endpointslices` trong namespace. Không cấp `cluster-admin`.

#### 4.3. Cấu hình GitLab CI truy cập AWS

Trong mô hình doanh nghiệp, GitLab CI không nên giữ AWS access key dài hạn.

Flow chuẩn:

```text
GitLab CI job
  -> phát hành OIDC ID token
  -> AWS IAM trust policy kiểm tra token
  -> assume IAM role tạm thời
  -> nhận temporary credentials
  -> login ECR
  -> push image
```

Ưu điểm:

- Không có secret AWS dài hạn nằm trong GitLab.
- Có thể giới hạn quyền theo project, branch, environment.
- Credential tự hết hạn.
- Audit CloudTrail thấy rõ role nào được assume.
- Dễ tách role cho `dev`, `staging`, `production`.

##### 4.3.1. GitLab OIDC assume role là đường chính

Thiết kế role tối thiểu:

| Role | Dùng cho | Quyền chính |
|---|---|---|
| `gitlab-ci-ecr-dev-role` | Pipeline build/push image dev | Push image vào 3 ECR repositories. |
| `gitlab-ci-gitops-dev-role` | Nếu CI cần gọi AWS để lấy metadata | Thường không cần nếu chỉ mở MR vào GitOps. |
| `argocd-dev-role` hoặc EKS node/pod role | Runtime pull image/call AWS | Pull ECR, đọc secret qua ESO nếu cần. |

Trong GitLab CI, job dùng OIDC token rồi gọi:

```text
aws sts assume-role-with-web-identity
```

Các biến GitLab cần lưu chỉ là metadata không nhạy cảm:

| Variable | Secret? | Ví dụ | Ghi chú |
|---|---:|---|---|
| `AWS_REGION` | No | `ap-southeast-1` | Region ECR. |
| `AWS_ACCOUNT_ID` | No | `150914615641` | Account chứa ECR. |
| `AWS_ROLE_ARN` | No | `arn:aws:iam::150914615641:role/gitlab-ci-ecr-dev-role` | Role cho CI assume. |
| `ECR_REPOSITORY_NAME` | No | `newgate2601-shared-services/post-service` | Mỗi repo app dùng giá trị riêng. |

Các biến này vẫn nên đặt trong:

```text
Settings
  -> CI/CD
  -> Variables
```

Nhưng chúng không phải secret dài hạn. Có thể `Visible` hoặc `Masked` đều được; với thói quen an toàn, vẫn có thể để `Masked` nếu GitLab chấp nhận format.

##### 4.3.2. Lab fallback: dùng AWS access key nếu chưa cấu hình OIDC

Nếu mục tiêu là thực hành nhanh trong lab cá nhân và chưa dựng OIDC trust, có thể tạm dùng IAM access key.

Đây là shortcut để học pipeline, không phải mô hình production chuẩn.

Trên từng GitLab project, vào:

```text
Settings
  -> CI/CD
  -> Variables
```

Tạo các biến:

| Variable | Masked | Protected | Ghi chú |
|---|---:|---:|---|
| `AWS_ACCESS_KEY_ID` | Yes | Tùy branch strategy | Access key của IAM user/role dùng tạm trong lab. |
| `AWS_SECRET_ACCESS_KEY` | Yes | Tùy branch strategy | Secret key tương ứng. |
| `AWS_SESSION_TOKEN` | Yes | Tùy trường hợp | Chỉ cần nếu dùng temporary credentials. |

Không commit các giá trị này vào repo.

Khi bấm **Add variable** trên GitLab, điền như sau cho từng biến AWS secret:

| Field | Giá trị nên chọn | Ghi chú |
|---|---|---|
| `Type` | `Variable` | Dùng biến môi trường bình thường cho CI job. |
| `Environments` | `All (default)` | Pipeline mọi environment đều đọc được. |
| `Visibility` | `Masked` | Không in thẳng secret ra job log. |
| `Protect variable` | Bỏ tick nếu branch `staging` chưa protected | Nếu bật mà branch không protected, job sẽ không thấy biến. |
| `Expand variable reference` | Bỏ tick | Giá trị AWS secret nên được giữ nguyên raw string. |
| `Key` | `AWS_ACCESS_KEY_ID` hoặc `AWS_SECRET_ACCESS_KEY` | Nhập đúng tên biến mà `.gitlab-ci.yml` đang dùng. |
| `Value` | Giá trị lấy từ AWS IAM access key | Không paste vào code, commit, issue hoặc chat. |

Với flow lab đang chạy pipeline trên branch `staging`, nếu chưa cấu hình `staging` là protected branch thì phải **bỏ tick Protect variable** cho `AWS_ACCESS_KEY_ID` và `AWS_SECRET_ACCESS_KEY`.

Khi tạo AWS access key cho lab:

```text
IAM
  -> Users
  -> chọn IAM user dùng cho GitLab CI
  -> Security credentials
  -> Access keys
  -> Create access key
```

Ở màn hình **Access key best practices & alternatives**, chọn:

```text
Application running outside AWS
```

Lý do: GitLab SaaS runner chạy bên ngoài AWS.

Ở bước description, đặt tên dễ nhận diện:

```text
gitlab-ci-ecr-push
```

Sau khi AWS tạo key, copy:

| AWS hiển thị | GitLab variable |
|---|---|
| `Access key` hoặc `Access key ID` | `AWS_ACCESS_KEY_ID` |
| `Secret access key` | `AWS_SECRET_ACCESS_KEY` |

`Secret access key` chỉ hiện một lần. Nếu lỡ đóng màn hình mà chưa copy, không thể xem lại secret cũ; phải tạo key mới.

Nếu access key hoặc secret key từng bị lộ qua ảnh chụp màn hình, chat, log hoặc commit:

1. Vào lại IAM user.
2. Deactivate hoặc delete access key đã lộ.
3. Tạo access key mới.
4. Cập nhật lại GitLab variables bằng key mới.

Không dùng root access key cho GitLab CI.

Phải cấu hình cơ chế AWS auth cho cả 3 project:

```text
newgate2601/social-media-app-gateway
newgate2601/social-media-app-uaa
newgate2601/social-media-app-post
```

#### 4.4. Quyền AWS tối thiểu cho CI push ECR

Principal dùng bởi GitLab CI cần quyền ECR tối thiểu như sau:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:CompleteLayerUpload",
        "ecr:DescribeImages",
        "ecr:DescribeRepositories",
        "ecr:InitiateLayerUpload",
        "ecr:PutImage",
        "ecr:UploadLayerPart"
      ],
      "Resource": [
        "arn:aws:ecr:ap-southeast-1:150914615641:repository/newgate2601-shared-services/gateway",
        "arn:aws:ecr:ap-southeast-1:150914615641:repository/newgate2601-shared-services/uaa-service",
        "arn:aws:ecr:ap-southeast-1:150914615641:repository/newgate2601-shared-services/post-service"
      ]
    }
  ]
}
```

Không cấp quyền rộng kiểu `AdministratorAccess` cho CI nếu chỉ cần push image.

#### 4.5. Mốc dừng trước khi tự chạy pipeline

Đến đây là kết thúc phần **chuẩn bị file/config** cho bước 3.7.

Trạng thái mong đợi trên máy local:

```text
[x] Đã có Dockerfile kiểu production image
[x] Đã có .dockerignore
[x] Đã có .gitlab-ci.yml build/test/package/push ECR
[x] Mỗi repo dùng đúng ECR_REPOSITORY_NAME riêng
[x] Đã có application-k8s.yaml cho gateway, uaa-service, post-service
[x] Đã có k8s/deployment.yaml cho gateway, uaa-service, post-service
[x] UAA/Post có k8s/secret.example.yaml cho DB/Redis secret mẫu
[x] Pipeline dùng image tag theo commit + pipeline id
[x] Pipeline xuất IMAGE_URI dạng digest vào image.env
[x] Không có AWS secret nào được commit vào repo
```

Ở mốc này **chưa có image nào được push lên ECR** nếu chưa chạy GitLab pipeline.

Trước khi tự chạy pipeline, cần đảm bảo:

```text
[ ] Bước 3.6 đã apply và 3 ECR repositories đã tồn tại
[ ] Cả 3 repo đã được push lên GitLab.com
[ ] Cả 3 GitLab project đã có AWS auth: OIDC assume role hoặc lab fallback access key
[ ] IAM role/principal của CI có quyền push vào cả 3 ECR repositories
[ ] Branch chạy pipeline là main, staging hoặc develop
```

### 5. File/config/lệnh liên quan

Các file cần tạo hoặc sửa:

| File | Trạng thái | Vai trò |
|---|---|---|
| `Dockerfile` | Cập nhật | Build Spring Boot app thành production container image. |
| `.dockerignore` | Tạo mới | Giảm Docker build context và tránh đưa file thừa vào image. |
| `.gitlab-ci.yml` | Cập nhật | Chạy test, package, build image và push image vào ECR. |
| `src/main/resources/application-k8s.yaml` | Tạo mới | Profile chạy trên Kubernetes, không dùng Eureka. |
| `k8s/deployment.yaml` | Tạo mới | Manifest Kubernetes tối thiểu cho service. |
| `k8s/secret.example.yaml` | Tạo mới nếu service cần secret | Mẫu Secret local, không chứa secret thật. |

Các lệnh tham khảo để kiểm tra local trước khi push GitLab:

```powershell
docker build -t springboot-learning:local .
```

Chạy container local nếu đã có PostgreSQL phù hợp:

```powershell
docker run --rm -p 8086:8086 springboot-learning:local
```

Các lệnh này chỉ kiểm tra local, không push image lên ECR.

### 6. Giải thích từng phần quan trọng

#### 6.1. Vì sao không push vào GitLab Container Registry nữa

Pipeline cũ push image vào:

```text
$CI_REGISTRY_IMAGE
```

Đây là registry của GitLab.

Trong topology AWS/EKS của bài lab, runtime chính là AWS. Vì vậy image nên nằm ở ECR để:

- EKS/ECS pull thuận lợi hơn.
- IAM và audit nằm cùng AWS account.
- GitOps manifest dùng ECR URI ổn định.
- Không phải tạo Kubernetes imagePullSecret riêng cho GitLab registry ngay từ đầu.

#### 6.2. Vì sao tag dùng `${CI_COMMIT_SHORT_SHA}-${CI_PIPELINE_IID}`

ECR repository đang bật immutable tag.

Nếu chỉ dùng:

```text
abc1234
```

thì khi retry pipeline cùng commit, push lại cùng tag có thể lỗi vì tag cũ đã tồn tại.

Dùng:

```text
abc1234-57
```

giúp:

- Vẫn trace được commit.
- Mỗi pipeline có tag riêng.
- Không ghi đè image cũ.
- Hợp với immutable tag.

#### 6.3. Vì sao vẫn cần digest

Tag giúp con người đọc, digest giúp máy chắc chắn.

Ví dụ:

```text
newgate2601-shared-services/gateway:abc1234-57
newgate2601-shared-services/gateway@sha256:...
```

GitOps ở bước sau nên dùng dạng digest:

```yaml
image: 150914615641.dkr.ecr.ap-southeast-1.amazonaws.com/newgate2601-shared-services/gateway@sha256:...
```

Như vậy khi Argo CD sync, nó pull đúng image đã được CI push.

#### 6.4. Vì sao Dockerfile không chạy `mvn spring-boot:run`

`mvn spring-boot:run` phù hợp lúc dev local, nhưng không phải cách tốt để chạy container production-like.

Với image deploy thật, nên:

```text
Build JAR ở build stage
Copy JAR sang runtime stage
Chạy java -jar app.jar
```

Cách này giúp image runtime nhỏ hơn, ít tool thừa hơn và gần với cách deploy thật hơn.

#### 6.5. Vì sao chưa tự động cập nhật GitOps ở bước này

Tài liệu V2 có luồng:

```text
CI push ECR
  -> tạo MR/PR cập nhật GitOps image digest
```

Nhưng ở thời điểm bước 3.7, GitOps repository và Argo CD chưa được tạo. Vì vậy pipeline hiện chỉ xuất digest ra artifact `image.env`.

Đến bước GitOps/Argo CD sau, có thể mở rộng pipeline để tự tạo MR/PR cập nhật file values hoặc manifest.

### 7. Kiểm tra hoàn thành

Kiểm tra file tồn tại:

```powershell
Test-Path .\Dockerfile
Test-Path .\.gitlab-ci.yml
```

Kiểm tra trong `.gitlab-ci.yml` có các biến chính:

```text
AWS_REGION
AWS_ACCOUNT_ID
ECR_REGISTRY
ECR_REPOSITORY_NAME
IMAGE_TAG
```

Kiểm tra GitLab CI syntax:

```text
GitLab.com
  -> Project
  -> Build
  -> Pipeline editor
  -> Validate
```

Sau khi bạn tự chạy pipeline, kết quả mong đợi:

```text
test        -> passed
package     -> passed
build-image -> passed
```

Artifact `image.env` có dạng:

```text
IMAGE_URI=150914615641.dkr.ecr.ap-southeast-1.amazonaws.com/newgate2601-shared-services/gateway@sha256:...
IMAGE_TAG=abc1234-57
```

Kiểm tra trên ECR:

```powershell
aws ecr describe-images `
  --repository-name newgate2601-shared-services/gateway `
  --region ap-southeast-1 `
  --query "imageDetails[].{Tags:imageTags,Digest:imageDigest,PushedAt:imagePushedAt}" `
  --output table
```

### 8. Lỗi thường gặp và cách xử lý

| Lỗi | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| `RepositoryNotFoundException` | Chưa apply bước 3.6 hoặc sai `ECR_REPOSITORY_NAME`. | Tạo ECR trước, kiểm tra đúng repository name. |
| `no basic auth credentials` | Docker chưa login ECR hoặc AWS auth sai. | Kiểm tra OIDC role/access key fallback, region và lệnh login. |
| `AccessDeniedException` | IAM role/principal của CI thiếu quyền ECR. | Bổ sung quyền push ECR tối thiểu. |
| `Cannot connect to the Docker daemon` | GitLab runner không bật Docker-in-Docker đúng cách. | Kiểm tra runner có hỗ trợ privileged Docker executor. |
| `ImageTagAlreadyExistsException` | Repository immutable tag và tag đã tồn tại. | Dùng tag duy nhất theo commit + pipeline id. |
| Maven test fail | Test hoặc cấu hình datasource cần DB local. | Tách profile test, dùng H2/testcontainers hoặc mock datasource. |
| Build image quá lâu | Maven dependency tải lại nhiều lần. | Thêm cache Maven trong pipeline ở bước tối ưu sau. |

Lỗi cần đặc biệt chú ý:

```text
Không đưa AWS access key vào .gitlab-ci.yml, Dockerfile, README hoặc commit history.
```

Secret chỉ đặt trong GitLab CI/CD Variables.

### 9. Kết quả sau bước này

Trạng thái sau khi hoàn thành phần chuẩn bị:

```text
[x] Repo có Dockerfile production-like
[x] Repo có GitLab CI pipeline
[x] CI không còn push mặc định vào GitLab Container Registry
[x] CI target sang Amazon ECR ở ap-southeast-1
[x] CI dùng tag không ghi đè
[x] CI xuất image digest để dùng cho GitOps
[x] Chưa tạo EKS
[x] Chưa cài Argo CD
[x] Chưa deploy service lên Kubernetes
```

Sau khi bạn tự chạy pipeline thành công, bài lab có image đầu tiên trong ECR:

```text
newgate2601-shared-services/gateway:<commit>-<pipeline>
newgate2601-shared-services/gateway@sha256:...
```

Bước tiếp theo nên làm là thiết kế GitOps repository và promotion model ở bước 3.8. Sau đó mới triển khai EKS dev ở bước 4.1.

## Bước 3.8 - Thiết kế GitOps repository và promotion model

### 1. Mục tiêu

Bước này chỉ thiết kế repo GitOps và cách promotion giữa các môi trường.

Không lặp lại cách tạo ECR, tạo GitLab repo, cấu hình CI variables hoặc chạy pipeline. Những phần đó thuộc bước 3.6 và 3.7.

Ở bước này **chưa tạo EKS, chưa cài Argo CD, chưa tạo RDS/Redis thật và chưa deploy application lên Kubernetes**.

### 2. Vì sao cần bước này

App repo và GitOps repo có trách nhiệm khác nhau:

| Loại repo | Trách nhiệm | Không nên chứa |
|---|---|---|
| App repo | Source code, test, Dockerfile, CI build/push image | Config deploy của mọi môi trường |
| GitOps repo | Desired state Kubernetes theo môi trường | Source code app, secret thật, lịch sử mọi image đã build |

GitOps repo là nơi Argo CD đọc để biết cluster **nên đang chạy cái gì**. Nó không phải nơi lưu tất cả image của tất cả developer.

### 3. Enterprise baseline

Mô hình chuẩn doanh nghiệp của bài lab này:

```text
App repository
  -> merge request
  -> compile/test/scan
  -> build container image
  -> push image vào ECR
  -> tạo deploy candidate
  -> mở MR/PR cập nhật GitOps repo

GitOps repository
  -> lưu desired state theo environment
  -> chỉ thay đổi digest image được chọn deploy
  -> không chứa secret thật

Argo CD
  -> watch GitOps repo
  -> sync desired state vào EKS

AWS Secrets Manager
  -> External Secrets Operator
  -> Kubernetes Secret
  -> Pod env/volume

EKS runtime permission
  -> IRSA hoặc EKS Pod Identity
  -> không nhét AWS access key vào container
```

Các nguyên tắc phải giữ:

- CI **không deploy trực tiếp** vào cluster bằng `kubectl apply`.
- CI chỉ build, scan, push image và đề xuất thay đổi GitOps bằng MR/PR.
- Argo CD là thành phần deploy vào EKS.
- Deploy bằng image digest, không dùng `latest`.
- Promote `dev -> staging -> production` bằng **cùng một digest**, không rebuild.
- Secret thật nằm ở AWS Secrets Manager hoặc secret store tương đương.
- Kubernetes Secret là kết quả sync/runtime, không phải source of truth lâu dài.
- Quyền AWS của workload dùng IRSA/EKS Pod Identity, không dùng env access key trong pod.

Lab có thể dùng vài shortcut để học nhanh, nhưng tài liệu phải luôn phân biệt rõ:

| Chủ đề | Enterprise path | Lab fallback |
|---|---|---|
| CI truy cập AWS | GitLab OIDC assume IAM role | IAM access key trong GitLab variables |
| Secret runtime | AWS Secrets Manager + External Secrets Operator | Kubernetes Secret tạo tay |
| Deploy app | Argo CD sync từ GitOps | `kubectl apply` để debug tạm |
| Promote release | MR/approval đổi digest trong GitOps | Copy digest thủ công khi học cơ chế |

### 4. Quan hệ giữa nhiều image và GitOps

Trong dự án thật, ECR sẽ có rất nhiều image:

```text
developer A push commit -> image tag a1b2c3-10
developer B push commit -> image tag d4e5f6-11
hotfix push commit      -> image tag 998877-12
```

Không phải image nào build ra cũng được deploy.

GitOps repo chỉ lưu **image đang được chọn để chạy ở từng environment**:

```text
dev        -> có thể cập nhật thường xuyên
staging    -> chỉ nhận image đã qua dev hoặc MR được duyệt
production -> chỉ nhận image đã promote từ staging
```

Vì vậy, không hiểu bước này là "dev ngồi viết image thủ công mỗi lần có build mới". Cách đúng là:

```text
App repo CI
  -> build image
  -> push ECR
  -> lấy digest
  -> tạo MR/PR sang GitOps repo để đổi digest của environment phù hợp

Reviewer
  -> xem MR/PR
  -> merge nếu muốn deploy

Argo CD
  -> thấy GitOps đổi
  -> sync vào cluster
```

Trong lab, có thể copy digest thủ công 1-2 lần để hiểu cơ chế. Nhưng flow chuẩn là CI hoặc release job tự mở MR/PR cập nhật GitOps.

### 5. Deploy candidate record

Mỗi pipeline thành công nên xuất ra thông tin candidate, thường nằm trong artifact `image.env`:

```text
SERVICE_NAME=post-service
IMAGE_REPOSITORY=150914615641.dkr.ecr.ap-southeast-1.amazonaws.com/newgate2601-shared-services/post-service
IMAGE_TAG=11c46479-1
IMAGE_DIGEST=sha256:...
IMAGE_URI=150914615641.dkr.ecr.ap-southeast-1.amazonaws.com/newgate2601-shared-services/post-service@sha256:...
SOURCE_COMMIT=11c46479...
PIPELINE_ID=...
```

Release/GitOps job dùng record này để cập nhật đúng environment.

Ví dụ mapping về mặt ý nghĩa:

| Environment | Ai/Job được quyền cập nhật | Ý nghĩa |
|---|---|---|
| `dev` | CI trên branch `staging` hoặc manual deploy job | Bản đang test tích hợp. |
| `staging` | Promotion job hoặc MR được duyệt | Bản chuẩn bị release. |
| `production` | Promotion job có approval | Bản chạy thật. |

### 6. Cấu trúc GitOps repository

GitOps repository là repo riêng dùng để lưu trạng thái mong muốn của Kubernetes.

Cấu trúc đề xuất cho bài lab:

```text
social-media-app-gitops/
├── charts/
│   └── springboot-service/
├── applications/
│   ├── gateway/
│   │   ├── values-dev.yaml
│   │   ├── values-staging.yaml
│   │   └── values-production.yaml
│   ├── uaa-service/
│   │   ├── values-dev.yaml
│   │   ├── values-staging.yaml
│   │   └── values-production.yaml
│   └── post-service/
│       ├── values-dev.yaml
│       ├── values-staging.yaml
│       └── values-production.yaml
├── argocd/
│   ├── dev/
│   │   ├── gateway.yaml
│   │   ├── uaa-service.yaml
│   │   └── post-service.yaml
│   ├── staging/
│   │   ├── gateway.yaml
│   │   ├── uaa-service.yaml
│   │   └── post-service.yaml
│   └── production/
│       ├── gateway.yaml
│       ├── uaa-service.yaml
│       └── post-service.yaml
└── README.md
```

Giải thích các thành phần trong cây:

| Thành phần | Vai trò | Ví dụ nội dung |
|---|---|---|
| `charts/` | Chứa Helm chart hoặc template nền do platform/team DevOps chuẩn hóa. | Template `Deployment`, `Service`, `Ingress`, `ServiceAccount`, probes, resources, security context. |
| `charts/springboot-service/` | Chart nền cho các service Spring Boot có kiểu deploy giống nhau. | Dùng được cho HTTP service thông thường; không bắt buộc dùng cho service có nhu cầu quá khác. |
| `applications/` | Chứa cấu hình deploy riêng của từng application. | Mỗi service có một folder riêng. |
| `applications/gateway/` | Values deploy cho gateway theo từng môi trường. | `values-dev.yaml`, `values-staging.yaml`, `values-production.yaml`. |
| `applications/uaa-service/` | Values deploy cho UAA theo từng môi trường. | Image digest, port, env, secret name, replica, resource size. |
| `applications/post-service/` | Values deploy cho Post theo từng môi trường. | Image digest, DB secret, Redis secret, URL gọi `uaa-service`. |
| `values-dev.yaml` | Desired config của service ở môi trường dev. | Thường cập nhật nhanh hơn để test tích hợp. |
| `values-staging.yaml` | Desired config của service ở môi trường staging. | Chỉ nhận digest đã được chọn/promote từ dev. |
| `values-production.yaml` | Desired config của service ở production. | Chỉ đổi qua approval/release process. |
| `argocd/` | Chứa manifest Argo CD Application. | File cho Argo CD biết nên sync service nào, chart nào, values nào. |
| `argocd/dev/` | Argo CD Application cho môi trường dev. | Trỏ tới `values-dev.yaml`. |
| `argocd/staging/` | Argo CD Application cho staging. | Trỏ tới `values-staging.yaml`. |
| `argocd/production/` | Argo CD Application cho production. | Trỏ tới `values-production.yaml`. |
| `README.md` | Ghi quy ước vận hành repo GitOps. | Cách promote, rollback, naming, ownership, approval. |

`charts/` là nơi chứa **Helm chart/template nền**. Có thể hiểu đơn giản:

```text
Helm chart = bộ template Kubernetes manifest
values.yaml = dữ liệu đầu vào để render template đó
```

Điểm dễ nhầm: doanh nghiệp không coi mọi service là giống hệt nhau. Các service có business, dependency, port, env, secret, scaling và routing khác nhau. Phần có thể chuẩn hóa thường là **khung vận hành Kubernetes**:

```text
thường giống nhau:
  cách khai báo Deployment
  cách khai báo Service
  readiness/liveness probes
  resources requests/limits
  securityContext
  labels/annotations chuẩn
  serviceAccount/RBAC pattern

thường khác nhau:
  service name
  port
  image repository/digest
  env vars
  secret names
  replica/resource size theo môi trường
  ingress/routing rule
  dependency như DB/Redis/Kafka/S3
```

Vì vậy có vài cách làm thực tế:

| Cách | Khi nào dùng | Nhận xét |
|---|---|---|
| Shared/base Helm chart | Nhiều service có kiểu deploy gần giống nhau. | Platform team giữ chuẩn chung; app team chỉ truyền values. |
| Chart riêng cho từng service | Service có runtime/routing/job/sidecar khác rõ rệt. | Linh hoạt hơn, nhưng dễ lặp chuẩn nếu không kiểm soát. |
| Kustomize base/overlay | Team không muốn Helm hoặc muốn patch YAML thuần. | Dễ nhìn manifest, nhưng template logic ít hơn Helm. |

Với 3 service hiện tại:

| Service | Có thể dùng chart nền không? | Lý do |
|---|---|---|
| `uaa-service` | Có | HTTP Spring Boot service, cần DB secret, Service nội bộ. |
| `post-service` | Có | HTTP Spring Boot service, cần DB/Redis/client URL. |
| `gateway` | Có thể dùng ban đầu, nhưng dễ tách riêng sau | Gateway có routing/discovery/ingress đặc thù hơn service thường. |

Nói gọn: chart nền không có nghĩa là service giống nhau. Nó chỉ giúp chuẩn hóa phần Kubernetes lặp lại. Service vẫn khác nhau qua values, và service đủ đặc biệt thì tách chart/overlay riêng.

Lợi ích nếu dùng chart nền đúng chỗ:

- Ít lặp manifest.
- Dễ áp chung chuẩn security/resource/probe cho mọi service.
- Khi cần sửa chuẩn Deployment, sửa trong chart một lần.
- Mỗi environment vẫn có values riêng để khác image digest, resource size, replica, endpoint hoặc feature flag.

Ở bước 3.8 chỉ cần thiết kế cấu trúc và nội dung mẫu. Chưa cần tạo Argo CD Application thật vì chưa có EKS cluster.

### 7. Values theo environment nên chứa gì

File values không chứa "mọi image từng build". Nó chỉ chứa **image đang được chọn để deploy cho environment đó**.

Ví dụ `applications/post-service/values-dev.yaml`:

```yaml
service:
  name: post-service
  port: 8088

image:
  repository: 150914615641.dkr.ecr.ap-southeast-1.amazonaws.com/newgate2601-shared-services/post-service
  digest: "sha256:<digest-duoc-chon-cho-dev>"

spring:
  profile: k8s

database:
  secretName: post-service-db

redis:
  secretName: post-service-redis

clients:
  uaaServiceUrl: http://uaa-service:8082
```

`digest` ở đây không phải giá trị cố định do developer tự gõ mỗi ngày. Nó là **trạng thái deploy hiện tại của environment**.

Ví dụ dễ hiểu:

```text
ECR có 20 image của post-service
dev chỉ đang chạy 1 image trong số đó
values-dev.yaml chỉ ghi digest của 1 image đang được chọn cho dev
```

Khi có image mới, pipeline không sửa source code app. Nó tạo một thay đổi nhỏ ở GitOps repo, thường là MR/PR, để đề xuất đổi digest của environment muốn deploy:

```diff
 image:
   repository: 150914615641.dkr.ecr.ap-southeast-1.amazonaws.com/newgate2601-shared-services/post-service
-  digest: sha256:<digest-cu-dang-chay-o-dev>
+  digest: sha256:<digest-moi-muon-deploy-len-dev>
```

Nếu MR/PR được merge, Argo CD thấy GitOps đổi và sync cluster. Nếu không merge, image mới vẫn nằm trong ECR nhưng không được deploy.

Với staging/production cũng tương tự, nhưng digest được promote có kiểm soát:

```text
values-dev.yaml        -> image đang chạy ở dev
values-staging.yaml    -> image đã được chọn cho staging
values-production.yaml -> image đã được approve cho production
```

Trong team nhiều dev, đây là điểm quan trọng:

- ECR giữ lịch sử nhiều image.
- GitLab pipeline tạo deploy candidate.
- GitOps environment file chỉ giữ bản đang được deploy.
- Merge/revert GitOps commit chính là deploy/rollback.

### 8. Secret trong GitOps

Không lưu secret thật trong GitOps repo.

Ở giai đoạn chưa có External Secrets Operator, có thể tạo secret thủ công sau khi có cluster:

```text
uaa-service-db
post-service-db
post-service-redis
```

Các file `k8s/secret.example.yaml` trong service repo chỉ là mẫu để biết cần key nào. Không commit file chứa password thật.

Khi sang môi trường chuẩn hơn:

```text
AWS Secrets Manager
  -> External Secrets Operator
  -> Kubernetes Secret
  -> Pod env
```

### 9. Quy tắc đặt image trong manifest

Manifest hoặc Helm values nên ghép image theo dạng:

```text
<repository>@<digest>
```

Không dùng:

```text
latest
```

Không nên dùng riêng tag cho deploy chính:

```text
<repository>:<tag>
```

Tag vẫn hữu ích để đọc bằng mắt trong ECR/GitLab, nhưng digest mới là khóa deploy ổn định.

### 10. Cổng nghiệm thu

Đánh dấu hoàn thành bước 3.8 khi:

```text
[x] Hiểu GitOps chỉ lưu image đang được chọn cho từng environment
[x] Có cấu trúc GitOps repo/folder cho dev, staging, production
[x] Có quy ước values theo environment
[x] Có quy ước promote bằng cùng digest
[x] Có quy ước secret không nằm trong GitOps
[x] Chưa tạo EKS
[x] Chưa cài Argo CD
[x] Chưa deploy app
```

### 11. Lỗi thiết kế thường gặp

| Lỗi | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| GitOps bị cập nhật bằng mọi image mới | Nhầm GitOps với image history. | Chỉ merge digest muốn deploy; các image khác nằm ở ECR như build history. |
| CI deploy thẳng vào cluster | Trộn CI và CD. | CI chỉ mở MR/PR; Argo CD sync từ GitOps. |
| Rebuild lại khi promote production | Không bảo toàn artifact. | Promote cùng digest đã qua dev/staging. |
| Secret thật nằm trong GitOps | Nhầm config và secret. | Dùng AWS Secrets Manager + External Secrets Operator. |
| Ép mọi service vào một chart chung dù khác quá nhiều | Chart phình to, nhiều `if/else`, khó hiểu. | Tách chart/overlay riêng cho service đặc biệt. |
| Mỗi service tự có manifest khác nhau hoàn toàn | Dễ drift và khó maintain. | Chuẩn hóa phần giống nhau bằng chart nền/library/base; chỉ tách riêng phần thật sự khác. |

### 12. Kết quả sau bước này

Sau bước 3.8, bạn có mô hình GitOps/CD đủ sạch để tạo GitOps skeleton:

```text
App repo và GitOps repo đã tách trách nhiệm
GitOps repo lưu desired state theo environment
Promotion đi bằng MR/PR đổi digest
Argo CD là bên deploy
Secret thật không nằm trong Git
```

Bước tiếp theo là tạo GitOps repository skeleton ở bước 3.9. Sau đó mới triển khai EKS dev ở bước 4.1.

## Bước 3.9 - Tạo GitOps repository skeleton

### 1. Mục tiêu

Bước này tạo khung ban đầu cho GitOps repository.

Nói đơn giản: từ bước này trở đi, ta có một repo riêng để mô tả **muốn Kubernetes chạy cái gì**. Repo này chưa làm cluster chạy ngay, vì chưa có EKS và chưa cài Argo CD. Nó chỉ chuẩn bị cấu trúc để các bước sau dùng.

Ở bước này **chưa tạo EKS, chưa cài Argo CD, chưa connect cluster và chưa deploy app**.

### 2. Người mới cần hiểu gì trước

GitOps repository giống như "bản thiết kế vận hành" của Kubernetes.

```text
App repo
  -> chứa code Java/Spring Boot
  -> build ra Docker image

GitOps repo
  -> chứa cấu hình Kubernetes muốn chạy
  -> nói image nào được deploy ở dev/staging/production
  -> Argo CD đọc repo này để sync vào cluster
```

Nếu app repo là nơi developer viết tính năng, thì GitOps repo là nơi team vận hành/release quyết định phiên bản nào được chạy ở môi trường nào.

Một image mới build xong chưa có nghĩa là tự động chạy production. Nó chỉ trở thành bản đang chạy khi GitOps repo được cập nhật và Argo CD sync.

### 3. Tên repo đề xuất

Tạo một project GitLab private mới:

```text
social-media-app-gitops
```

Repo này tách khỏi 3 repo service:

```text
social-media-app-gateway
social-media-app-uaa
social-media-app-post
```

Lý do tách riêng:

- App repo thay đổi theo feature.
- GitOps repo thay đổi theo deploy/release.
- Rollback deploy có thể làm bằng revert commit GitOps.
- Argo CD chỉ cần quyền đọc GitOps repo, không cần đọc toàn bộ source code app.
- Team có thể phân quyền approval deploy riêng với approval code.

### 4. Cấu trúc thư mục cần tạo

Cấu trúc ban đầu:

```text
social-media-app-gitops/
├── charts/
│   └── springboot-service/
│       ├── Chart.yaml
│       ├── values.yaml
│       └── templates/
│           ├── deployment.yaml
│           ├── service.yaml
│           ├── serviceaccount.yaml
│           └── _helpers.tpl
├── applications/
│   ├── gateway/
│   │   ├── values-dev.yaml
│   │   ├── values-staging.yaml
│   │   └── values-production.yaml
│   ├── uaa-service/
│   │   ├── values-dev.yaml
│   │   ├── values-staging.yaml
│   │   └── values-production.yaml
│   └── post-service/
│       ├── values-dev.yaml
│       ├── values-staging.yaml
│       └── values-production.yaml
├── argocd/
│   ├── dev/
│   │   ├── gateway.yaml
│   │   ├── uaa-service.yaml
│   │   └── post-service.yaml
│   ├── staging/
│   │   ├── gateway.yaml
│   │   ├── uaa-service.yaml
│   │   └── post-service.yaml
│   └── production/
│       ├── gateway.yaml
│       ├── uaa-service.yaml
│       └── post-service.yaml
└── README.md
```

Giải thích cho người mới:

| File/folder | Hiểu đơn giản là gì |
|---|---|
| `charts/springboot-service/` | Bộ khuôn mẫu Kubernetes cho Spring Boot service thông thường. |
| `Chart.yaml` | Thông tin tên/version của Helm chart. |
| `values.yaml` | Giá trị mặc định của chart. |
| `templates/deployment.yaml` | Khuôn để tạo Kubernetes Deployment. |
| `templates/service.yaml` | Khuôn để tạo Kubernetes Service. |
| `templates/serviceaccount.yaml` | Khuôn để tạo ServiceAccount nếu service cần identity riêng. |
| `templates/_helpers.tpl` | Helper đặt tên label/name cho chart, tránh lặp template. |
| `applications/<service>/values-*.yaml` | Config riêng của từng service theo từng môi trường. |
| `argocd/<env>/<service>.yaml` | Argo CD Application manifest, dùng ở bước sau khi đã có cluster. |
| `README.md` | Quy ước vận hành GitOps repo. |

### 5. Vì sao chưa viết secret thật

GitOps repo không phải nơi lưu password.

Không đưa các giá trị này vào GitOps:

```text
database password
redis password
JWT secret
AWS access key
private key
token thật
```

Trong GitOps chỉ nên ghi **tên secret** mà application sẽ đọc:

```yaml
database:
  secretName: post-service-db
```

Giá trị thật đi theo flow khác:

```text
AWS Secrets Manager
  -> External Secrets Operator
  -> Kubernetes Secret
  -> Pod đọc secret
```

Ở lab chưa có EKS/ESO thì chỉ cần giữ `secretName` trong values. Tạo secret thật là việc của bước sau.

### 6. Values file nên viết như thế nào

Ví dụ `applications/post-service/values-dev.yaml`:

```yaml
nameOverride: post-service

replicaCount: 1

image:
  repository: 150914615641.dkr.ecr.ap-southeast-1.amazonaws.com/newgate2601-shared-services/post-service
  digest: ""

service:
  port: 8088

spring:
  profile: k8s

env:
  APP_CLIENTS_UAA_SERVICE_URL: http://uaa-service:8082

secrets:
  database: post-service-db
  redis: post-service-redis

resources:
  requests:
    cpu: 100m
    memory: 256Mi
  limits:
    cpu: 500m
    memory: 768Mi
```

Ở skeleton ban đầu, `digest` có thể để rỗng:

```yaml
digest: ""
```

Lý do: bước này chỉ tạo khung GitOps. Digest thật sẽ được CI/release MR cập nhật sau khi chọn image muốn deploy.

Với người mới, hãy nhớ:

```text
repository = kho image nằm ở đâu
digest     = đúng bản image nào được chọn chạy
```

### 7. Dev, staging, production khác nhau ở đâu

Cùng một service có thể có 3 file values:

```text
values-dev.yaml
values-staging.yaml
values-production.yaml
```

Những thứ có thể khác nhau:

| Khác nhau | Dev | Staging | Production |
|---|---|---|---|
| `replicaCount` | Ít | Gần production | Nhiều hơn |
| `resources` | Nhỏ | Vừa | Theo tải thật |
| `image.digest` | Đổi thường xuyên | Promote có chọn lọc | Chỉ release đã duyệt |
| feature flag | Có thể bật thử | Gần production | Cẩn trọng |
| external endpoint | Dev endpoint | Staging endpoint | Production endpoint |

Những thứ không nên khác nhau tùy tiện:

- Cách đặt label.
- Cách đặt probe.
- Security context.
- Cách đọc secret.
- Cách expose service nội bộ.

Các phần này nên được chuẩn hóa bằng chart/template/base.

### 8. Argo CD files để làm gì

Các file trong `argocd/` chưa chạy ở bước này. Chúng là manifest để sau này cài Argo CD xong thì apply.

Ví dụ ý nghĩa của `argocd/dev/post-service.yaml`:

```text
Nói với Argo CD rằng:
  hãy deploy post-service
  dùng chart springboot-service
  lấy values từ applications/post-service/values-dev.yaml
  sync vào namespace dev
```

Người mới có thể hiểu Argo CD Application như một "đăng ký deploy". Có file này, Argo CD mới biết nó cần theo dõi app nào trong GitOps repo.

### 9. Promotion sẽ diễn ra như thế nào

Promotion không phải là build lại image.

Flow đúng:

```text
Build image một lần
  -> test ở dev
  -> nếu ổn, promote cùng digest sang staging
  -> nếu staging ổn, promote cùng digest sang production
```

Ví dụ:

```text
post-service digest sha256:abc
  -> values-dev.yaml
  -> values-staging.yaml
  -> values-production.yaml
```

Lợi ích:

- Biết chính xác production đang chạy đúng image đã test.
- Không bị chuyện "build lại cùng code nhưng image khác".
- Rollback bằng cách revert GitOps commit.

### 10. Mốc dừng thực hành

Sau khi tạo GitOps skeleton, dừng lại trước khi sang EKS.

Không cần:

- Apply Argo CD Application.
- Chạy `helm install`.
- Chạy `kubectl apply`.
- Tạo Kubernetes Secret thật.
- Điền digest production thật.

Những việc đó để sau khi đã có EKS dev và Argo CD.

### 11. Cổng nghiệm thu

Đánh dấu hoàn thành bước 3.9 khi:

```text
[x] Có GitOps repo riêng
[x] Có cấu trúc charts/applications/argocd
[x] Có values-dev/staging/production cho 3 service
[x] Values chưa chứa secret thật
[x] Digest có thể để rỗng hoặc placeholder rõ ràng
[x] README ghi rõ GitOps repo dùng để deploy, không chứa source app
[x] Chưa deploy gì lên Kubernetes
```

### 12. Lỗi thường gặp

| Lỗi | Vì sao sai | Cách sửa |
|---|---|---|
| Đưa password vào values | GitOps repo nằm trong Git history, rất khó xóa sạch secret. | Chỉ ghi `secretName`, giá trị thật để Secrets Manager/ESO. |
| Copy toàn bộ manifest từ app repo vào GitOps mà không chuẩn hóa | Sau này 3 service drift, khó sửa đồng loạt. | Chuẩn hóa phần giống nhau bằng chart/base. |
| Ép gateway giống hệt service thường | Gateway có routing/ingress/discovery đặc thù. | Ban đầu có thể dùng chart nền, sau này tách chart/overlay riêng nếu cần. |
| Điền digest bừa để cho đủ file | Người đọc tưởng đó là image thật. | Để `digest: ""` hoặc placeholder rõ ràng. |
| Tạo GitOps repo nhưng vẫn deploy bằng CI | Mất ý nghĩa GitOps. | CI mở MR/PR; Argo CD sync. |

### 13. Kết quả sau bước này

Sau bước 3.9, bạn có repo GitOps skeleton để bước EKS/Argo CD dùng tiếp:

```text
GitOps repo đã có khung
Service config đã tách theo môi trường
Secret thật chưa vào Git
Digest thật chưa bắt buộc ở bước này
Sẵn sàng sang EKS dev
```

Bước tiếp theo là triển khai EKS dev ở bước 4.1.


---

## Bước 4.1 - Triển khai EKS dev

### 1. Mục tiêu của bước này

Bước này dựng nền Kubernetes dev bằng Terraform theo kiến trúc doanh nghiệp, dùng capacity nhỏ để học:

- Module `terraform/modules/eks` dùng lại được cho các môi trường.
- Root module `terraform/environments/dev/eks` có state riêng, đọc network dev đã tạo.
- EKS control plane do AWS vận hành; ENI kết nối VPC và EC2 managed node group dùng private app subnet của dev.
- Quyền người vận hành qua IAM role và EKS Access Entry.
- VPC CNI dùng IAM role riêng qua IRSA; node role không kiêm quyền CNI.
- Bật control-plane logs; mã hóa EBS của node; bắt buộc IMDSv2.
- Quản lý phiên bản Kubernetes, AMI và add-on bằng cấu hình được review.
- Kiểm tra node Ready, DNS, quyền truy cập và khả năng pull image.

**Chưa cài Argo CD, chưa deploy 3 service, chưa tạo RDS/Redis/MSK.** Ba add-on thiết yếu `vpc-cni`, `kube-proxy` và `coredns` thuộc bootstrap ở bước này vì node và DNS cần chúng. EBS CSI, Metrics Server và các add-on mở rộng thuộc bước 4.2.

“Chuẩn doanh nghiệp” ở đây là nền tảng về phân quyền, khả năng tái tạo, audit và quản lý thay đổi. Hoàn thành 4.1 chưa có nghĩa hệ thống đã production-ready; observability, policy workload, reliability và DR còn các bước sau.

### 2. Vì sao cần làm bước này

ECR giữ image; GitOps giữ desired state; EKS cung cấp nơi chạy Pod. Ba phần có vòng đời và quyền khác nhau.

```text
Terraform network state
  -> VPC dev + private app subnets + route/NAT
  -> Terraform EKS state
       -> EKS API + IAM + node group + networking add-ons

Platform operator role
  -> EKS Access Entry
  -> kubectl quản trị cluster

App CI
  -> ECR
  -> GitOps MR ở các bước sau

Argo CD ở bước 4.7
  -> đọc GitOps branch master
  -> deploy vào EKS
```

Không cấp quyền quản trị Kubernetes cho app CI. Role tạo hạ tầng cũng không tự động được cấp quyền Kubernetes: quyền đó phải xuất hiện rõ trong Access Entry.

Baseline cho bài thực hành:

| Hạng mục | Cấu hình ở dev | Khi vận hành môi trường quan trọng |
|---|---|---|
| Node | 2 node On-Demand, private subnet nhiều AZ | Capacity theo tải, kiểm chứng đủ sức chịu mất node/AZ |
| API endpoint | Private bật; public chỉ mở CIDR quản trị nếu chưa có đường private | Có thể đóng public hoàn toàn khi có VPN/management runner |
| Identity | Role qua SSO/STS; Access Entry tường minh | Phân tách operator, read-only, break-glass và workload |
| Network egress | Dùng NAT dev đang có | Review egress, endpoint và NAT theo AZ |
| Version | Pin Kubernetes, add-on và AMI release | Nâng cấp qua MR, kiểm thử trước rồi rollout |
| State | S3 mã hóa + lock, key riêng | Pipeline hạ tầng có review/approval và quyền giới hạn |

Network dev hiện dùng một NAT Gateway. Đây là giới hạn availability của lab: node nhiều AZ **không biến một NAT thành HA**. Không lấy cấu hình này làm bằng chứng hệ thống đã chịu lỗi một AZ.

### 3. Trước khi bắt đầu cần có gì

#### 3.1. Công cụ và AWS identity

Dùng PowerShell trên máy Windows. Kiểm tra:

```powershell
aws --version
terraform version
kubectl version --client
aws sts get-caller-identity
```

Dùng Terraform >= 1.6 và AWS CLI v2 có lệnh `eks describe-cluster-versions`. kubectl cần phiên bản tương thích với control plane; nên chọn cùng minor version.

Account của lab là `150914615641`, region `ap-southeast-1`. Dừng nếu `get-caller-identity` trả về account khác.

Phiên chạy Terraform cần quyền quản lý EKS, EC2 launch template/security group, IAM role/policy/OIDC, CloudWatch Logs, và đọc/ghi backend S3 + lock + sử dụng KMS backend. `iam:PassRole` phải giới hạn vào các role hạ tầng được phép chuyển cho EKS/EC2. Nếu tổ chức dùng permissions boundary/SCP, cần áp dụng quy ước đó vào các role trong module trước khi plan.

Không dùng AWS root hoặc access key của app CI để tạo cluster. Dùng profile SSO/assume-role cho hạ tầng, ví dụ tên profile do bạn tự cấu hình:

```powershell
$env:AWS_PROFILE = "infra-dev"
$env:AWS_REGION = "ap-southeast-1"
aws sts get-caller-identity
```

`infra-dev` là ví dụ tên profile, không phải profile đã được tạo sẵn bởi tài liệu.

#### 3.2. Chuẩn bị role quản trị Kubernetes

Cần một IAM role tồn tại, ví dụ `newgate2601-dev-platform-operator`, được người vận hành assume bằng SSO hoặc STS. Role này thuộc bootstrap identity, không nên bị xóa cùng cluster.

Trên AWS Console: **IAM -> Roles -> chọn role quản trị đã cấp cho bạn -> copy ARN**. Với IAM Identity Center, lấy ARN IAM role đầy đủ, bao gồm path `aws-reserved/sso.amazonaws.com/...` nếu có.

Phải dùng:

```text
arn:aws:iam::150914615641:role/<role-path-and-name>
```

Không dùng ARN phiên đăng nhập:

```text
arn:aws:sts::150914615641:assumed-role/<role>/<session>
```

Nếu chưa có role/profile này, hoàn thành phần identity trước khi apply: trust policy chỉ cho principal quản trị phù hợp assume; người dùng được xác thực MFA/SSO; role có `eks:DescribeCluster` trên cluster dev để tạo kubeconfig. Quyền Kubernetes của role sẽ được cấp bằng Access Entry trong Terraform bên dưới. Không thay bằng `principal: "*"` hoặc IAM user tùy tiện để chạy cho qua.

Nguồn: [EKS Access Entries](https://docs.aws.amazon.com/eks/latest/userguide/access-entries.html) và [yêu cầu principal của Access Entry](https://docs.aws.amazon.com/eks/latest/userguide/creating-access-entries.html).


##### 3.2.1. Nếu đã dùng IAM Identity Center

Dùng IAM role của permission set dành cho platform dev. Nhờ quản trị viên cấp
`eks:DescribeCluster` trên cluster dev nếu permission set chưa có quyền đó.
Chạy `aws configure sso --profile platform-dev`, điền start URL/SSO region của tổ chức,
chọn đúng account và permission set, rồi `aws sso login --profile platform-dev`.
ARN đưa vào tfvars lấy từ IAM Roles, không lấy trực tiếp trường `Arn` của STS.

##### 3.2.2. Nếu đang đi theo IAM user lab ở bước 2.3

Các bước trước tạo `tony-lab-admin`, chưa tạo role operator. Vì vậy không thể chỉ
đặt `AWS_PROFILE=platform-dev` rồi mong profile tồn tại. Với lab cá nhân, tạo role
qua Console dưới đây bằng IAM admin hiện có; không dùng nhánh này cho SSO.

1. Vào **IAM → Users → tony-lab-admin → Summary**, copy ARN user và ARN MFA device
   trong **Security credentials**. User phải có MFA để assume role theo trust bên dưới.
2. Vào **IAM → Roles → Create role → Custom trust policy**. Dùng JSON sau, thay
   ARN user nếu tên/path thực tế khác:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {"AWS": "arn:aws:iam::150914615641:user/tony-lab-admin"},
    "Action": "sts:AssumeRole",
    "Condition": {"Bool": {"aws:MultiFactorAuthPresent": "true"}}
  }]
}
```

3. Chưa gắn policy admin AWS vào role này. Đặt tên
   `newgate2601-dev-platform-operator`, tạo role, mở **Permissions → Add permissions
   → Create inline policy → JSON** và lưu policy tên `DescribeDevEks`:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": "eks:DescribeCluster",
    "Resource": "arn:aws:eks:ap-southeast-1:150914615641:cluster/newgate2601-dev-eks"
  }]
}
```

4. IAM user nguồn cũng cần được phép `sts:AssumeRole` tới ARN role này. Group
   `LabAdmin` có `AdministratorAccess` ở bước 2.3 đã bao gồm quyền đó, trừ khi có
   explicit deny/SCP/boundary. Với user giới hạn quyền, quản trị viên cấp riêng
   action này trên đúng role; không cần nâng thành admin để dùng kubectl.
5. Xem các profile hiện có bằng `aws configure list-profiles`. Chọn profile đang
   dùng thành công ở bước network làm nguồn. Ví dụ dưới dùng `lab-admin`; thay
   bằng tên thực tế, không tạo thêm access key chỉ để đổi tên profile.

```powershell
aws configure set role_arn arn:aws:iam::150914615641:role/newgate2601-dev-platform-operator --profile platform-dev
aws configure set source_profile lab-admin --profile platform-dev
aws configure set mfa_serial arn:aws:iam::150914615641:mfa/REPLACE_WITH_REAL_MFA_DEVICE --profile platform-dev
aws configure set region ap-southeast-1 --profile platform-dev
aws sts get-caller-identity --profile platform-dev
```

Lệnh cuối hỏi OTP và phải trả về account lab, ARN dạng
`arn:aws:sts::150914615641:assumed-role/newgate2601-dev-platform-operator/...`.
Đây là **kết quả kiểm tra phiên assume**, còn tfvars dùng ARN `arn:aws:iam::...:role/...`.
Chưa thể chạy kubectl vì cluster/Access Entry chưa được tạo. Role operator lab
này chỉ đủ tạo kubeconfig và gọi Kubernetes; các lệnh AWS đọc add-on, IAM, EC2,
CloudWatch trong bài dùng profile hạ tầng. Các root Helm ở bước sau cần profile
hạ tầng có quyền backend/AWS và cấu hình assume operator cho Kubernetes.

Nguồn: [AWS CLI assume-role profile và MFA](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-role.html).

#### 3.3. Kiểm tra network dev có thật

```powershell
Set-Location D:\AWS\springboot-learning\terraform\environments\dev\network
terraform output vpc_id
terraform output private_app_subnet_ids
```

Cần có VPC và ít nhất hai private app subnet ở hai AZ. Kiểm tra trong **VPC -> Subnets**:

- Các subnet thuộc đúng VPC dev.
- Auto-assign public IPv4 tắt.
- Route table có đường ra NAT hoạt động để tải image/gọi AWS API.
- VPC có DNS resolution và DNS hostnames bật.
- Không chọn isolated data subnet cho node.

Output còn trong state không đủ chứng minh tài nguyên còn tồn tại: kiểm tra AWS Console nếu đã từng destroy thủ công.

#### 3.4. Chọn đường quản trị API

Module mặc định chỉ bật private endpoint. Có hai cách sử dụng:

1. Nếu có VPN/management host hoặc runner kết nối được VPC: giữ `admin_public_cidrs = []`, và cho phép security group của management host vào cluster TCP 443 qua `management_security_group_ids`.
2. Với máy Windows lab chưa có đường private: đặt `admin_public_cidrs` thành public IPv4 thực tế của mạng quản trị với hậu tố `/32`. Private endpoint vẫn bật cho node; public endpoint vẫn yêu cầu IAM authentication và Kubernetes authorization.

Lấy địa chỉ public IPv4:

```powershell
$eksAdminIp = (Invoke-RestMethod -Uri "https://checkip.amazonaws.com").Trim()
"$eksAdminIp/32"
```

Không dùng IP LAN như `192.168.x.x`; không mở `0.0.0.0/0`. Nếu VPN/proxy đổi địa chỉ egress, dùng đúng địa chỉ mà máy gửi request ra AWS.

Nguồn: [EKS cluster endpoint](https://docs.aws.amazon.com/eks/latest/userguide/cluster-endpoint.html).

#### 3.5. Chọn phiên bản thay vì chép số cũ

Tra phiên bản còn standard support ở region:

```powershell
aws eks describe-cluster-versions --region ap-southeast-1 --version-status STANDARD_SUPPORT --output table
```

Chọn một minor version còn standard support và ghi vào `kubernetes_version`. Với mỗi add-on, tra phiên bản tương thích:

```powershell
$eksVersion = Read-Host "Nhap Kubernetes minor version da chon, vi du 1.35"
aws eks describe-addon-versions --region ap-southeast-1 --kubernetes-version $eksVersion --addon-name vpc-cni --output table
aws eks describe-addon-versions --region ap-southeast-1 --kubernetes-version $eksVersion --addon-name kube-proxy --output table
aws eks describe-addon-versions --region ap-southeast-1 --kubernetes-version $eksVersion --addon-name coredns --output table
$eksAmiParameter = "/aws/service/eks/optimized-ami/$eksVersion/amazon-linux-2023/x86_64/standard/recommended/release_version"
aws ssm get-parameter --region ap-southeast-1 --name $eksAmiParameter --query Parameter.Value --output text
```

Ghi nguyên chuỗi release của AMI và chuỗi version có `-eksbuild.` của add-on vào tfvars. Khi chạy lại sau vài tuần, Terraform vẫn dùng phiên bản đã chọn; nâng cấp bằng thay đổi cấu hình có review. Không tự truy vấn “latest” trong mỗi lần apply.

Nguồn: [vòng đời version EKS](https://docs.aws.amazon.com/eks/latest/userguide/kubernetes-versions.html), [AWS CLI describe-cluster-versions](https://docs.aws.amazon.com/cli/latest/reference/eks/describe-cluster-versions.html) và [managed node group Terraform](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/eks_node_group.html).


Đọc kết quả theo thứ tự, không chọn ngẫu nhiên mỗi dòng một version:

| Giá trị | Lấy từ đâu | Dùng ở đâu |
|---|---|---|
| Minor Kubernetes | `clusterVersions[].clusterVersion`, còn standard support | `kubernetes_version` |
| CNI/kube-proxy/CoreDNS | `addons[].addonVersions[].addonVersion`, kiểm tra `compatibilities` và kiến trúc `amd64` | Ba trường `addon_versions` |
| Release AL2023 | `Parameter.Value` từ đường dẫn SSM đúng minor và x86_64 | `node.ami_release` |

`--version-status STANDARD_SUPPORT` là giá trị hợp lệ của tham số mới;
không nhầm với `--status standard-support` cũ. Chuỗi AMI release có dạng
`1.xx.y-YYYYMMDD`, không phải ID `ami-...`. Không dùng AMI AL2 hoặc ARM cho
`ami_type = AL2023_x86_64_STANDARD` và instance `t3.large`.

Module đã bật engine NetworkPolicy của VPC CNI để bước 4.2 áp dụng policy.
Kiểm tra schema của **đúng bản CNI đã chọn** trước khi điền input:

```powershell
$eksCniVersion = Read-Host "Nhap version VPC CNI da chon"
aws eks describe-addon-configuration --region ap-southeast-1 --addon-name vpc-cni --addon-version $eksCniVersion --query configurationSchema --output text
```

Schema cần có `enableNetworkPolicy`; cấu hình trong module truyền chuỗi `"true"`
theo schema CNI. Bật engine chưa tạo NetworkPolicy, chưa chặn traffic giữa các Pod.
Nguồn: [cấu hình NetworkPolicy của Amazon VPC CNI](https://docs.aws.amazon.com/eks/latest/userguide/cni-network-policy-configure.html).

### 4. Thao tác chi tiết

#### 4.1. Tạo cấu trúc file

Trong repo `D:\AWS\springboot-learning`, tạo:

```text
terraform/
├── modules/
│   └── eks/
│       ├── versions.tf
│       ├── variables.tf
│       ├── main.tf
│       ├── outputs.tf
│       └── README.md
└── environments/
    └── dev/
        └── eks/
            ├── versions.tf
            ├── backend.tf
            ├── providers.tf
            ├── variables.tf
            ├── main.tf
            ├── outputs.tf
            ├── terraform.tfvars.example
            ├── .gitignore
            ├── README.md
            ├── CONFIG-GUIDE.md
            └── terraform.tfvars          # input local, không commit
```

```powershell
Set-Location D:\AWS\springboot-learning
New-Item -ItemType Directory -Force terraform\modules\eks
New-Item -ItemType Directory -Force terraform\environments\dev\eks
```

Chỉ tạo cấu hình dev. Module không chứa account ID, tên môi trường hoặc subnet ID cố định.

#### 4.2. Module EKS: versions.tf và variables.tf

File `terraform/modules/eks/versions.tf`:

```hcl
terraform {
  required_version = ">= 1.6.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.99.1, < 6.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = ">= 4.0, < 5.0"
    }
  }
}
```

Ở đây giữ cùng major AWS provider 5 với các root hiện có. Commit `.terraform.lock.hcl` được sinh ở root EKS; việc đổi major provider phải là thay đổi riêng được kiểm tra.

File `terraform/modules/eks/variables.tf`:

```hcl
variable "name" {
  type = string
}
variable "vpc_id" {
  type = string
}
variable "subnet_ids" {
  type = list(string)
  validation {
    condition     = length(distinct(var.subnet_ids)) >= 2
    error_message = "Use private application subnets in at least two AZs."
  }
}
variable "kubernetes_version" {
  type = string
  validation {
    condition     = can(regex("^1[.][0-9]+$", var.kubernetes_version))
    error_message = "Set a Kubernetes minor version such as 1.35; verify standard support in AWS."
  }
}
variable "operator_role_arn" {
  type = string
  validation {
    condition     = can(regex("^arn:aws:iam::[0-9]{12}:role/.+", var.operator_role_arn))
    error_message = "Use a permanent IAM role ARN, not an STS session ARN."
  }
}
variable "admin_public_cidrs" {
  type    = list(string)
  default = []
  validation {
    condition = alltrue([
      for cidr in var.admin_public_cidrs :
      can(cidrnetmask(cidr)) && endswith(cidr, "/32")
    ])
    error_message = "This baseline accepts only individual IPv4 /32 management addresses."
  }
}
variable "management_security_group_ids" {
  type    = set(string)
  default = []
}
variable "node" {
  type = object({
    instance_type = string
    desired_size  = number
    min_size      = number
    max_size      = number
    ami_release   = string
    disk_size     = number
  })
  validation {
    condition     = var.node.disk_size >= 20 && var.node.disk_size == floor(var.node.disk_size)
    error_message = "Use an integer root volume size of at least 20 GiB for this AL2023 baseline."
  }
  validation {
    condition     = can(regex("^[0-9]+[.][0-9]+[.][0-9]+-[0-9]{8}$", var.node.ami_release))
    error_message = "Set the AL2023 release_version returned by SSM, not an AMI ID or placeholder."
  }
  validation {
    condition = (
      var.node.min_size >= 1 &&
      alltrue([for size in [var.node.min_size, var.node.desired_size, var.node.max_size] : size == floor(size)]) &&
      var.node.min_size <= var.node.desired_size &&
      var.node.desired_size <= var.node.max_size
    )
    error_message = "Require integer sizes with 1 <= min_size <= desired_size <= max_size."
  }
}
variable "addon_versions" {
  type = object({
    vpc_cni    = string
    kube_proxy = string
    coredns    = string
  })
  validation {
    condition     = alltrue([for version in values(var.addon_versions) : can(regex("^v[0-9]+[.][0-9]+[.][0-9]+-eksbuild[.][0-9]+$", version))])
    error_message = "Pin all three add-ons to full versions returned by describe-addon-versions."
  }
}
variable "tags" {
  type = map(string)
}
```

Validation kiểm tra ít nhất hai subnet ID khác nhau. `data.aws_subnet.selected` và `lifecycle.precondition` trong `main.tf` kiểm tra VPC, auto-assign public IPv4 và ít nhất hai AZ từ dữ liệu AWS thật. Route tới NAT và số IP còn trống vẫn phải kiểm tra riêng.

**Hiểu hợp đồng đầu vào trước khi tạo resource:** `type` kiểm tra hình dạng dữ liệu;
`validation` chặn subnet trùng, ARN phiên STS, placeholder version và số node lẻ.
Validation không gọi AWS nên không chứng minh version còn hỗ trợ hay role tồn tại.
Biến không có `default` phải được root truyền vào; module không tự đọc tfvars của dev.

#### 4.3. Module EKS: main.tf

File `terraform/modules/eks/main.tf`:

```hcl
data "aws_subnet" "selected" {
  for_each = toset(var.subnet_ids)
  id       = each.value
}

resource "aws_iam_role" "cluster" {
  name = format("%s-cluster", var.name)
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { Service = "eks.amazonaws.com" }
    }]
  })
  tags = var.tags
}

resource "aws_iam_role_policy_attachment" "cluster" {
  role       = aws_iam_role.cluster.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

resource "aws_cloudwatch_log_group" "cluster" {
  name              = format("/aws/eks/%s/cluster", var.name)
  retention_in_days = 30
  tags              = var.tags
}

resource "aws_eks_cluster" "this" {
  name     = var.name
  role_arn = aws_iam_role.cluster.arn
  version  = var.kubernetes_version


  lifecycle {
    precondition {
      condition     = alltrue([for subnet in data.aws_subnet.selected : subnet.vpc_id == var.vpc_id && !subnet.map_public_ip_on_launch])
      error_message = "EKS subnets must belong to the selected VPC and disable public IPv4 assignment."
    }
    precondition {
      condition     = length(toset([for subnet in data.aws_subnet.selected : subnet.availability_zone])) >= 2
      error_message = "EKS requires subnets in at least two AZs."
    }
  }
  bootstrap_self_managed_addons = false
  enabled_cluster_log_types = [
    "api", "audit", "authenticator", "controllerManager", "scheduler"
  ]

  access_config {
    authentication_mode                         = "API"
    bootstrap_cluster_creator_admin_permissions = false
  }

  kubernetes_network_config {
    ip_family = "ipv4"
  }

  vpc_config {
    subnet_ids              = var.subnet_ids
    endpoint_private_access = true
    endpoint_public_access  = length(var.admin_public_cidrs) > 0
    public_access_cidrs     = length(var.admin_public_cidrs) > 0 ? var.admin_public_cidrs : null
  }

  depends_on = [
    aws_iam_role_policy_attachment.cluster,
    aws_cloudwatch_log_group.cluster
  ]
  tags = var.tags
}

resource "aws_security_group_rule" "management_api" {
  for_each                 = var.management_security_group_ids
  type                     = "ingress"
  from_port                = 443
  to_port                  = 443
  protocol                 = "tcp"
  security_group_id        = aws_eks_cluster.this.vpc_config[0].cluster_security_group_id
  source_security_group_id = each.value
  description              = "Private API access from management"
}

resource "aws_eks_access_entry" "operator" {
  cluster_name  = aws_eks_cluster.this.name
  principal_arn = var.operator_role_arn
  type          = "STANDARD"
  tags          = var.tags
}

resource "aws_eks_access_policy_association" "operator" {
  cluster_name  = aws_eks_cluster.this.name
  principal_arn = aws_eks_access_entry.operator.principal_arn
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"
  access_scope {
    type = "cluster"
  }
}

data "tls_certificate" "oidc" {
  url = aws_eks_cluster.this.identity[0].oidc[0].issuer
}

resource "aws_iam_openid_connect_provider" "this" {
  url             = aws_eks_cluster.this.identity[0].oidc[0].issuer
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.oidc.certificates[0].sha1_fingerprint]
  tags            = var.tags
}

locals {
  oidc_host = replace(aws_eks_cluster.this.identity[0].oidc[0].issuer, "https://", "")
}

resource "aws_iam_role" "cni" {
  name = format("%s-vpc-cni", var.name)
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = "sts:AssumeRoleWithWebIdentity"
      Principal = {
        Federated = aws_iam_openid_connect_provider.this.arn
      }
      Condition = {
        StringEquals = {
          (format("%s:aud", local.oidc_host)) = "sts.amazonaws.com"
          (format("%s:sub", local.oidc_host)) = "system:serviceaccount:kube-system:aws-node"
        }
      }
    }]
  })
  tags = var.tags
}

resource "aws_iam_role_policy_attachment" "cni" {
  role       = aws_iam_role.cni.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
}

resource "aws_eks_addon" "cni" {
  cluster_name                = aws_eks_cluster.this.name
  addon_name                  = "vpc-cni"
  configuration_values        = jsonencode({ enableNetworkPolicy = "true" })
  addon_version               = var.addon_versions.vpc_cni
  service_account_role_arn    = aws_iam_role.cni.arn
  resolve_conflicts_on_create = "NONE"
  resolve_conflicts_on_update = "NONE"
  depends_on                  = [aws_iam_role_policy_attachment.cni]
  tags                        = var.tags
}

resource "aws_iam_role" "node" {
  name = format("%s-node", var.name)
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
  tags = var.tags
}

resource "aws_iam_role_policy_attachment" "node" {
  for_each = toset([
    "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy",
    "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPullOnly"
  ])
  role       = aws_iam_role.node.name
  policy_arn = each.value
}

resource "aws_launch_template" "node" {
  name_prefix = format("%s-node-", var.name)

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  block_device_mappings {
    device_name = "/dev/xvda"
    ebs {
      volume_type           = "gp3"
      volume_size           = var.node.disk_size
      encrypted             = true
      delete_on_termination = true
    }
  }

  tag_specifications {
    resource_type = "instance"
    tags          = merge(var.tags, { Name = format("%s-node", var.name) })
  }

  tag_specifications {
    resource_type = "volume"
    tags          = var.tags
  }

  tags = var.tags
}

resource "aws_eks_node_group" "system" {
  cluster_name    = aws_eks_cluster.this.name
  node_group_name = "system"
  node_role_arn   = aws_iam_role.node.arn
  subnet_ids      = var.subnet_ids
  version         = var.kubernetes_version
  ami_type        = "AL2023_x86_64_STANDARD"
  release_version = var.node.ami_release
  capacity_type   = "ON_DEMAND"
  instance_types  = [var.node.instance_type]

  scaling_config {
    desired_size = var.node.desired_size
    min_size     = var.node.min_size
    max_size     = var.node.max_size
  }

  update_config {
    max_unavailable = 1
  }

  launch_template {
    id      = aws_launch_template.node.id
    version = tostring(aws_launch_template.node.latest_version)
  }

  depends_on = [
    aws_iam_role_policy_attachment.node,
    aws_eks_addon.cni
  ]
  tags = var.tags
}

resource "aws_eks_addon" "after_compute" {
  for_each = {
    kube-proxy = var.addon_versions.kube_proxy
    coredns    = var.addon_versions.coredns
  }
  cluster_name                = aws_eks_cluster.this.name
  addon_name                  = each.key
  addon_version               = each.value
  resolve_conflicts_on_create = "NONE"
  resolve_conflicts_on_update = "NONE"
  depends_on                  = [aws_eks_node_group.system]
  tags                        = var.tags
}
```

Thứ tự bootstrap là **cluster -> OIDC/IRSA -> VPC CNI -> node group -> CoreDNS/kube-proxy**. Không đặt CoreDNS làm điều kiện phải healthy trước khi có node chạy nó.

Node group dùng launch template không gắn custom security group nên EKS gắn cluster security group để node liên lạc với control plane. Không thêm SSH key, không mở port 22. Đây là security group bootstrap của EKS; chính sách phân tách traffic giữa workload vẫn cần NetworkPolicy ở các bước sau.

`http_put_response_hop_limit = 1` hạn chế Pod thông thường lấy node credentials qua IMDS. Workload cần AWS dùng IRSA/Pod Identity; host-network Pod vẫn cần được kiểm soát bằng policy và quyền triển khai. Đây không phải cơ chế sandbox tuyệt đối.

Nguồn: [IAM role của node](https://docs.aws.amazon.com/eks/latest/userguide/create-node-role.html), [IRSA cho VPC CNI](https://docs.aws.amazon.com/eks/latest/userguide/cni-iam-role.html), [launch template EKS](https://docs.aws.amazon.com/eks/latest/userguide/launch-templates.html) và [Terraform EKS add-on](https://registry.terraform.io/providers/hashicorp/aws/5.99.1/docs/resources/eks_addon).

**Đọc `main.tf` theo thứ tự phụ thuộc, không theo thứ tự dòng:**

```text
Subnet lookup + IAM cluster + log group
  → EKS control plane
      → Access Entry + Access Policy (operator)
      → OIDC provider → CNI IAM role/policy → VPC CNI
          → managed node group + launch template + node IAM policies
              → CoreDNS và kube-proxy
```

Terraform suy ra dependency từ tham chiếu resource. `depends_on` bổ sung điều kiện
mà tham chiếu ARN chưa thể hiện, chẳng hạn role đã tồn tại nhưng policy chưa gắn.

| Khối | Vì sao cần | Điểm cần đọc trong plan |
|---|---|---|
| `data.aws_subnet.selected` | Đọc subnet thật theo từng ID | Đúng VPC; ít nhất hai AZ; public IP assignment tắt |
| `lifecycle.precondition` | Chặn subnet sai trước khi tạo cluster | Không thay thế kiểm tra NAT/route table |
| `aws_iam_role.cluster` | Cho dịch vụ EKS assume role | Trust là `eks.amazonaws.com`, không phải user |
| `aws_cloudwatch_log_group.cluster` | Terraform quản lý retention từ đầu | Đúng tên log group và 30 ngày |
| `aws_eks_cluster.this` | Control plane do EKS quản lý | Version, API access mode, endpoints, năm loại log |
| `aws_eks_access_entry` + association | Cho role operator quyền Kubernetes | IAM role đúng; scope cluster chỉ dành cho quản trị |
| OIDC + role `cni` | Pod aws-node lấy quyền tạo/quản lý network qua IRSA | Audience STS và subject đúng service account |
| `aws_eks_addon.cni` | Cài CNI trước compute | Version pin, role riêng, engine NetworkPolicy bật |
| `aws_launch_template.node` | Cấu hình EC2 do node group tạo | IMDSv2, root EBS gp3 mã hóa; không gắn public IP/SSH key |
| `aws_eks_node_group.system` | Quản lý vòng đời EC2 worker | AL2023 x86_64, release pin, private subnets, On-Demand |
| `aws_eks_addon.after_compute` | CoreDNS có node để được schedule | Hai managed add-on, không tạo bản unmanaged trùng |

EKS tự quản lý quyền join cluster cho managed node group; không đăng ký node role
dưới Access Entry loại `STANDARD` và không cấp cluster-admin cho node. Operator,
cluster service role, node role và CNI role là bốn danh tính khác nhau.

Launch template không đặt AMI ID/user data vì EKS chọn AMI tối ưu và bootstrap
node dựa vào `ami_type`/`release_version`. Không cấu hình `disk_size` thêm ở node
group khi đã đặt đĩa trong template. Không đặt custom security group nếu chưa tự
thiết kế các rule node/control plane; hiện EKS gắn cluster security group.

`http_put_response_hop_limit = 1` hạn chế Pod thường truy cập IMDS, nhưng không
cô lập tuyệt đối hostNetwork/Pod đặc quyền. Controller ở 4.2 phải có identity AWS
riêng; không sửa hop limit để cho controller dùng tạm quyền node.

#### 4.4. Module EKS: outputs.tf

File `terraform/modules/eks/outputs.tf`:

```hcl
output "cluster_name" {
  value = aws_eks_cluster.this.name
}
output "cluster_endpoint" {
  value = aws_eks_cluster.this.endpoint
}
output "cluster_security_group_id" {
  value = aws_eks_cluster.this.vpc_config[0].cluster_security_group_id
}
output "oidc_provider_arn" {
  value = aws_iam_openid_connect_provider.this.arn
}
output "node_role_arn" {
  value = aws_iam_role.node.arn
}
output "vpc_cni_role_arn" {
  value = aws_iam_role.cni.arn
}
```

Output là giá trị công khai của module cho root gọi nó, không phải tài nguyên mới.
`cluster_endpoint` là URL Kubernetes API, không phải URL ứng dụng. OIDC provider
ARN sẽ được dùng cho IRSA ở bước sau; node role ARN phục vụ kiểm tra quyền pull ECR.
Root phải xuất lại output để các state khác đọc được qua `terraform_remote_state`.

#### 4.5. Root dev: versions.tf, backend.tf và providers.tf

File `terraform/environments/dev/eks/versions.tf`:

```hcl
terraform {
  required_version = ">= 1.6.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.99"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }
}
```

File `terraform/environments/dev/eks/backend.tf`:

```hcl
terraform {
  backend "s3" {
    bucket         = "newgate2601-terraform-state-150914615641-ap-southeast-1"
    key            = "dev/eks/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "terraform-state-lock"
    encrypt        = true
    kms_key_id     = "arn:aws:kms:ap-southeast-1:150914615641:key/38aaa237-5b16-4d7e-811c-c634ae35de52"
  }
}
```

Bucket, lock table và KMS ARN lấy từ backend network hiện có. Nếu bạn đã thay backend, dùng thông tin thực tế. **Key phải là `dev/eks/terraform.tfstate`**, không ghi đè `dev/network/terraform.tfstate`.

Giữ cơ chế DynamoDB lock đang dùng để không trộn thêm một migration backend vào bước dựng EKS. Không copy thư mục `.terraform` của network sang EKS.

File `terraform/environments/dev/eks/providers.tf`:

```hcl
provider "aws" {
  region              = var.aws_region
  allowed_account_ids = [var.account_id]
  default_tags {
    tags = local.tags
  }
}
```

**Ba file này xử lý ba việc khác nhau:**

- `versions.tf` ràng buộc phiên bản công cụ/provider; không tự cài Terraform.
  Root `~> 5.99` và module `>= 5.99.1, < 6.0` kết hợp thành khoảng chung;
  lock file quyết định bản chính xác sau init.
- `backend.tf` chọn nơi giữ state. Đối chiếu bucket, DynamoDB lock table và KMS
  với output bootstrap 3.1. Chỉ key đổi thành `dev/eks/terraform.tfstate`; không
  copy key của network. Backend không nhận `var.*` và không dùng AWS provider.
- `providers.tf` chọn account/region cho resource. `allowed_account_ids` ngăn
  chạy nhầm account ở provider, không bảo vệ thay cho quyền truy cập backend.
  Credentials lấy từ profile, không viết vào HCL.

#### 4.6. Root dev: variables.tf

File `terraform/environments/dev/eks/variables.tf`:

```hcl
variable "aws_region" {
  type = string
}
variable "account_id" {
  type = string
}
variable "project" {
  type = string
}
variable "environment" {
  type = string
  validation {
    condition     = var.environment == "dev"
    error_message = "This root module is reserved for dev."
  }
}
variable "owner" {
  type = string
}
variable "kubernetes_version" {
  type = string
}
variable "operator_role_arn" {
  type = string
}
variable "admin_public_cidrs" {
  type    = list(string)
  default = []
}
variable "management_security_group_ids" {
  type    = set(string)
  default = []
}
variable "node" {
  type = object({
    instance_type = string
    desired_size  = number
    min_size      = number
    max_size      = number
    ami_release   = string
    disk_size     = number
  })
}
variable "addon_versions" {
  type = object({
    vpc_cni    = string
    kube_proxy = string
    coredns    = string
  })
}
```

Root khai báo input người học điền. Object `node` gom capacity/AMI/đĩa;
`addon_versions` gom ba version độc lập. Root truyền các giá trị xuống module,
nơi kiểm tra chi tiết. `environment` chỉ nhận `dev` để không vô tình tạo staging
bên trong state dev. Đổi environment cần root/backend riêng.

#### 4.7. Root dev: main.tf và outputs.tf

File `terraform/environments/dev/eks/main.tf`:

```hcl
locals {
  name = format("%s-%s-eks", var.project, var.environment)
  tags = {
    Project     = var.project
    Environment = var.environment
    Owner       = var.owner
    ManagedBy   = "Terraform"
  }
}

data "terraform_remote_state" "network" {
  backend = "s3"
  config = {
    bucket = "newgate2601-terraform-state-150914615641-ap-southeast-1"
    key    = "dev/network/terraform.tfstate"
    region = var.aws_region
  }
}

module "eks" {
  source = "../../../modules/eks"

  name                          = local.name
  vpc_id                        = data.terraform_remote_state.network.outputs.vpc_id
  subnet_ids                    = data.terraform_remote_state.network.outputs.private_app_subnet_ids
  kubernetes_version            = var.kubernetes_version
  operator_role_arn             = var.operator_role_arn
  admin_public_cidrs            = var.admin_public_cidrs
  management_security_group_ids = var.management_security_group_ids
  node                          = var.node
  addon_versions                = var.addon_versions
  tags                          = local.tags
}
```

Remote state chỉ dùng để đọc output network; root EKS không tạo lại VPC/NAT. Quyền đọc remote state cho phép đọc nội dung state object, nên chỉ cấp cho role hạ tầng, không cấp cho app CI.

File `terraform/environments/dev/eks/outputs.tf`:

```hcl
output "cluster_name" {
  value = module.eks.cluster_name
}
output "cluster_endpoint" {
  value = module.eks.cluster_endpoint
}
output "cluster_security_group_id" {
  value = module.eks.cluster_security_group_id
}
output "oidc_provider_arn" {
  value = module.eks.oidc_provider_arn
}
output "node_role_arn" {
  value = module.eks.node_role_arn
}
output "vpc_cni_role_arn" {
  value = module.eks.vpc_cni_role_arn
}
```

Luồng giá trị của root là:

```text
terraform.tfvars → var.project/environment/owner → local.name và local.tags
network state   → outputs.vpc_id/private_app_subnet_ids → module.eks
terraform.tfvars → version/role/CIDR/node/add-ons         → module.eks
module.eks outputs → root outputs → kubectl và các root bước sau
```

`source = ../../../modules/eks` tính từ thư mục root EKS, không tính từ file bài học.
Đọc remote state yêu cầu `s3:GetObject` và `kms:Decrypt` theo backend thực tế.
Nếu thiếu output network, sửa/hoàn thành network trước; không nhập subnet đoán
vào module để bỏ qua nguồn state.

#### 4.8. Tạo input mẫu và điền terraform.tfvars

Tạo file `terraform/environments/dev/eks/terraform.tfvars.example` với nội dung dưới đây.
File mẫu được commit để người học sau biết cần điền gì; file thật chỉ nằm local.
Nếu repo đã có file thật, mở và sửa các placeholder còn lại, không ghi đè giá trị đã chọn.

```powershell
Set-Location D:\AWS\springboot-learning\terraform\environments\dev\eks
if (-not (Test-Path terraform.tfvars)) {
  Copy-Item terraform.tfvars.example terraform.tfvars
}
notepad terraform.tfvars
```

File `terraform/environments/dev/eks/terraform.tfvars.example`:

```hcl
aws_region  = "ap-southeast-1"
account_id  = "150914615641"
project     = "newgate2601"
environment = "dev"
owner       = "tony"

kubernetes_version = "REPLACE_WITH_STANDARD_SUPPORT_VERSION"
operator_role_arn  = "REPLACE_WITH_REAL_IAM_ROLE_ARN"

# Private-only: [] va can co duong quan tri vao VPC.
# May Windows chua co duong private: ["YOUR_ACTUAL_PUBLIC_IPV4/32"].
admin_public_cidrs            = []
management_security_group_ids = []

node = {
  instance_type = "t3.large"
  desired_size  = 2
  min_size      = 2
  max_size      = 3
  ami_release   = "REPLACE_WITH_SSM_RELEASE_VERSION"
  disk_size     = 30
}

addon_versions = {
  vpc_cni    = "REPLACE_WITH_COMPATIBLE_EKSBUILD_VERSION"
  kube_proxy = "REPLACE_WITH_COMPATIBLE_EKSBUILD_VERSION"
  coredns    = "REPLACE_WITH_COMPATIBLE_EKSBUILD_VERSION"
}
```

Thay toàn bộ `REPLACE_...` bằng kết quả tra cứu ở mục 3. Không chạy apply với placeholder. Nếu `admin_public_cidrs = []` và chưa có đường private, máy Windows sẽ không chạy kubectl được dù cluster tạo thành công.

Hai node On-Demand dành cho dev có 3 Java service và platform add-on ở các bước sau. `max_size = 3` không tự bật autoscaling; Cluster Autoscaler/Karpenter chưa được cài. Kiểm tra EC2 vCPU quota và sức chứa subnet trước khi tăng node.

#### 4.9. Kiểm tra và review plan

Tạo `terraform/environments/dev/eks/.gitignore` (chỉ áp dụng cho root EKS dev):

```gitignore
.terraform/
*.tfplan
*.tfplan.json
tfplan
*.tfstate*
kubeconfig*

terraform.tfvars
*.auto.tfvars
```

Giữ `.terraform.lock.hcl` trong Git. Không commit state, kubeconfig hoặc saved plan.

```powershell
Set-Location D:\AWS\springboot-learning\terraform\environments\dev\eks
terraform fmt -recursive ..\..\..\modules\eks
terraform fmt
terraform init
terraform validate
terraform plan -out=eks-dev.tfplan
terraform show -no-color eks-dev.tfplan
```

Kết quả mong đợi: validation thành công; plan chỉ tạo EKS dev và tài nguyên phụ trợ IAM/OIDC/logs/launch template/node group/add-on. Số resource có thể thay đổi theo phiên bản cấu hình; không nghiệm thu chỉ bằng một con số.

Đọc plan và kiểm tra:

- Account, region, cluster name và subnet thuộc dev.
- Không tạo lại VPC, không sửa network/shared-services.
- Private endpoint bật; public CIDR đúng lựa chọn quản trị.
- Chỉ platform operator được cấp cluster-admin; không có app CI trong Access Entry.
- Node role không gắn `AmazonEKS_CNI_Policy`.
- CNI trust giới hạn `kube-system:aws-node` và audience STS.
- Không có private key/AWS access key/password trong plan.
- Không có thao tác replace/delete ngoài ý định.

Trong doanh nghiệp: mở MR cho module/root/lock file, chạy fmt/validate/security checks và review plan; apply do pipeline hạ tầng có quyền phù hợp thực hiện trên revision đã duyệt. Lệnh local bên dưới là cách thực hành cùng quy trình cho lab cá nhân. Pipeline app vẫn chỉ xử lý image/GitOps.


##### 4.9.1. Hiểu từng lệnh và dấu hiệu thành công

Chạy từng lệnh, kiểm tra `$LASTEXITCODE` bằng `0` rồi mới chạy lệnh kế tiếp.
PowerShell không tự dừng một chuỗi lệnh chỉ vì chương trình native trả lỗi.

| Lệnh | Nó làm gì | Kết quả mong đợi / điều chưa chứng minh |
|---|---|---|
| `terraform fmt -recursive ../../../modules/eks` | Chuẩn hóa HCL module | Có thể in tên file được sửa; không truy cập AWS |
| `terraform fmt` | Chuẩn hóa root hiện tại | Không đổi ý nghĩa cấu hình |
| `terraform init` | Kết nối backend, tìm module, cài provider | Báo initialized; chưa tạo EKS |
| `terraform validate` | Kiểm tra cấu trúc và provider schema | `Success! The configuration is valid.`; không chứng minh quyền/subnet/version AWS hợp lệ |
| `terraform plan -out=eks-dev.tfplan` | Đọc input/state/AWS và lập thay đổi | Xem action và từng thuộc tính; chưa tạo cluster |
| `terraform show -no-color eks-dev.tfplan` | Hiển thị saved plan vừa tạo | Đúng plan sẽ đưa vào apply, không phải kết quả apply |

`(known after apply)` ở endpoint/OIDC/node role là bình thường vì resource chưa
tồn tại. Nếu backend hỏi migrate state ở lần tạo root mới, dừng kiểm tra working
directory và `.terraform`: root mới không cần lấy state của network làm state EKS.
Không dùng `init -upgrade` mỗi lần; chỉ dùng khi có chủ đích nâng provider.

Để chỉ kiểm tra cấu hình mà không kết nối backend, có thể dùng một bản sao root
và module trong thư mục kiểm tra rồi `terraform init -backend=false` và
`terraform validate`. Kết quả đó không thay thế plan với backend/input thật.

#### 4.10. Apply đúng saved plan

**Từ đây mới phát sinh tài nguyên tính phí.** Xem giá EKS, EC2, EBS, CloudWatch và network của region trước buổi học; node giảm về 0 vẫn không xóa phí control plane. Nguồn: [Amazon EKS Pricing](https://aws.amazon.com/eks/pricing/).

```powershell
terraform apply eks-dev.tfplan
terraform output
```

Nếu thay tfvars hoặc source sau khi plan, tạo và review plan mới. Không dùng `-target` để bỏ qua dependency hay sửa lỗi bootstrap.

Theo dõi **EKS -> Clusters -> newgate2601-dev-eks**:

1. Overview: cluster `Active`.
2. Compute: node group `system` chuyển `Active`.
3. Add-ons: VPC CNI, kube-proxy, CoreDNS chuyển `Active`.
4. Access: có role operator đã chọn.
5. Networking: subnet và endpoint đúng thiết kế.
6. Logging/CloudWatch: có log group `/aws/eks/newgate2601-dev-eks/cluster`.

`terraform apply eks-dev.tfplan` thực thi saved plan và không hỏi lại như
`terraform apply` không truyền file. Chỉ chạy sau khi đã đọc plan. Khi thành công,
Terraform báo apply complete và in outputs; xác nhận thêm trên AWS/kubectl.
Nếu apply dở dang, tài nguyên đã tạo có thể vẫn tính phí. Sửa nguyên nhân rồi
chạy lại plan để Terraform tiếp tục dựa trên state, không xóa state làm lại.

Apply có thể kéo dài nhiều phút. Khi lỗi, đọc message và trạng thái resource trước khi retry; không tạo một cluster khác bằng Console để thay thế Terraform.

#### 4.11. Kết nối kubectl bằng role operator

Mở PowerShell khác, dùng profile đã assume đúng role `operator_role_arn`. Ví dụ profile đặt tên `platform-dev`:

```powershell
$env:AWS_PROFILE = "platform-dev"
aws sts get-caller-identity
aws eks update-kubeconfig --profile platform-dev --region ap-southeast-1 --name newgate2601-dev-eks --alias newgate2601-dev
kubectl config current-context
kubectl cluster-info
kubectl get nodes -o wide
```

Không thêm `--role-arn` trỏ lại chính role mà profile đã assume: điều đó có thể đòi quyền self-assume không được cấp. Tham số `--profile` ở lệnh trên giúp kubeconfig lưu profile dùng lấy token.

Nếu profile sử dụng SSO, đăng nhập bằng `aws sso login --profile platform-dev` trước. Profile name chỉ là ví dụ; role thật phải khớp Access Entry. Kubeconfig dùng AWS CLI lấy token theo profile; khi quay lại shell khác cần bảo đảm AWS identity vẫn đúng.

Nguồn: [tạo kubeconfig cho EKS](https://docs.aws.amazon.com/eks/latest/userguide/create-kubeconfig.html).

#### 4.12. Kiểm tra node, DNS và audit

```powershell
kubectl wait --for=condition=Ready nodes --all --timeout=300s
kubectl get nodes -L topology.kubernetes.io/zone
kubectl -n kube-system get pods -o wide
kubectl -n kube-system rollout status daemonset/aws-node --timeout=300s
kubectl -n kube-system rollout status daemonset/kube-proxy --timeout=300s
kubectl -n kube-system rollout status deployment/coredns --timeout=300s
kubectl auth can-i get nodes
```

Mong đợi hai node `Ready`, kiểm tra phân bố AZ thực tế; tất cả Pod nền healthy. `EXTERNAL-IP` của node nên là `<none>`; kiểm chứng thêm ở **EC2 -> Instances -> Networking -> Public IPv4 address** để xác nhận không có public IP.

`kubectl get nodes` kiểm tra worker đã đăng ký; `Ready` không chứng minh ứng dụng
chạy được. Các lệnh rollout kiểm tra DaemonSet/Deployment đã đạt số replica mong
muốn. Nếu lỗi, xem `kubectl -n kube-system get events --sort-by=.metadata.creationTimestamp`
và `kubectl -n kube-system describe pod <ten-pod>` trước khi sửa cấu hình.

Test DNS bằng Pod tạm; không dùng image của service vì database chưa sẵn sàng:

```powershell
kubectl run eks-dns-check --image=busybox:1.36.1 --restart=Never --command -- sh -c "nslookup kubernetes.default.svc.cluster.local"
kubectl wait --for=jsonpath='{.status.phase}'=Succeeded pod/eks-dns-check --timeout=180s
kubectl logs eks-dns-check
kubectl delete pod eks-dns-check
```

Kỳ vọng log trả về địa chỉ service `kubernetes.default.svc.cluster.local` và Pod `Succeeded`. Image test dùng tag phiên bản để thao tác nhanh; môi trường có registry allowlist phải mirror image đã duyệt vào ECR và dùng digest. Đây là Pod kiểm tra tạm, không phải chuẩn chọn image cho workload.

Pod BusyBox pull từ Docker Hub nên **không chứng minh pull ECR**. Kiểm tra riêng
quyền node bằng image ECR đã push ở bước 3.7. Lấy URI kèm digest thật từ candidate
record; không đưa tag/placeholder của tài liệu vào lệnh:

```powershell
$eksProbeImage = Read-Host "Nhap ECR image URI@sha256:digest da build o buoc 3.7"
kubectl run eks-ecr-check --image=$eksProbeImage --image-pull-policy=Always --restart=Never --command -- java -version
kubectl wait --for=jsonpath='{.status.phase}'=Succeeded pod/eks-ecr-check --timeout=180s
kubectl logs eks-ecr-check
kubectl delete pod eks-ecr-check
```

Image của ba service có Java runtime; `java -version` kết thúc mà không khởi động
Spring Boot/kết nối DB. Thành công chứng minh kubelet lấy image ECR bằng quyền
node và chạy container được, chưa chứng minh business service healthy. Nếu wait
lỗi, giữ Pod để đọc `kubectl describe pod eks-ecr-check`, rồi xóa sau khi chẩn đoán.

Kiểm tra IRSA:

```powershell
kubectl -n kube-system get serviceaccount aws-node -o yaml
aws eks describe-addon --profile infra-dev --region ap-southeast-1 --cluster-name newgate2601-dev-eks --addon-name vpc-cni --query "addon.{Status:status,Role:serviceAccountRoleArn,Health:health}" --output json
```

Role ARN phải là `newgate2601-dev-eks-vpc-cni`, không phải node role. Trong CloudWatch Logs, kiểm tra log stream audit/authenticator có dữ liệu sau thao tác kubectl; có độ trễ ngắn là bình thường.

#### 4.13. Lưu kết quả và kết thúc buổi

Lưu cluster name, region, version đã chọn, node AZ và kết quả DNS vào ghi chú thực hành. Commit module, root config không chứa secret, và lock file qua MR của repo hạ tầng.

Không chỉnh GitOps digest hay apply Argo CD Application ở bước này. Repo GitOps vẫn dùng branch `master`.

Nếu dừng buổi học sau riêng bước 4.1, dùng shell identity hạ tầng:

```powershell
$env:AWS_PROFILE = "infra-dev"
Set-Location D:\AWS\springboot-learning\terraform\environments\dev\eks
aws sts get-caller-identity
terraform plan -destroy -out=eks-dev-destroy.tfplan
terraform show -no-color eks-dev-destroy.tfplan
terraform apply eks-dev-destroy.tfplan
```

Chỉ apply destroy plan sau khi kiểm tra nó xóa đúng EKS dev của buổi học. Network có state riêng nên NAT và các resource network **vẫn còn tính phí**; cleanup network thuộc checklist cuối buổi tương ứng.

Nếu đã đi tiếp và có LoadBalancer, Ingress, PVC/EBS hoặc add-on ở state khác, phải dọn workload và dependency trước, rồi mới destroy EKS. Không xóa state hoặc bucket backend để “xóa tài nguyên”.

### 5. File/config/lệnh liên quan

| Thành phần | Vai trò |
|---|---|
| `terraform/modules/eks` | Hợp đồng module dùng chung: IAM, EKS, node group, bootstrap add-on |
| `terraform/environments/dev/eks` | Ghép network dev, version, capacity và identity |
| `dev/eks/terraform.tfstate` | State EKS riêng trong backend S3 |
| `dev/network/terraform.tfstate` | Nguồn output VPC/subnet, chỉ đọc |
| `terraform.tfvars.example` | Mẫu input được commit |
| `terraform.tfvars` | Input thật local; không chứa credential, không commit |
| `.terraform.lock.hcl` | Khóa provider version/checksum đã chọn |
| `eks-dev.tfplan` | Plan local để review/apply, không commit |
| Kubeconfig local | Cấu hình truy cập API, không đặt trong GitOps |

### 6. Giải thích từng phần quan trọng

- **Access Entry và IAM policy là hai lớp.** `eks:DescribeCluster` cho phép gọi AWS API, không tự cấp quyền đọc Pod. Access Policy cấp quyền Kubernetes cho principal cụ thể.
- **Cluster-admin chỉ cho role platform.** Đây là role đặc quyền để bootstrap/vận hành, không dùng cho developer thường ngày hoặc app CI. Read-only và quyền theo namespace cần bổ sung theo vai trò công việc trước khi mở cluster cho nhóm.
- **IRSA không phải quyền người dùng.** OIDC trust của CNI chỉ cho service account `aws-node` assume role. Mỗi controller/workload AWS khác cần role riêng.
- **CoreDNS Pending trước khi node có mặt là khác với lỗi sau bootstrap.** Nghiệm thu chỉ khi node và add-on đã ổn định.
- **Terraform AWS provider không cần Kubernetes provider để tạo nền này.** Tránh cấu hình Kubernetes/Helm provider trỏ tới cluster chưa tồn tại trong cùng lượt bootstrap.
- **Pin AMI không có nghĩa ngừng vá.** Pin tạo thay đổi có thể review; cần lên lịch cập nhật bản vá và rolling node group ở bước vận hành.
- **Namespace, ResourceQuota, LimitRange, Pod Security và workload RBAC** là phần platform baseline sẽ được triển khai ở 4.2/trước 4.8. Chưa đưa workload thật vào cluster chỉ vì node đã Ready.
- **Mã hóa có nhiều lớp.** Backend dùng KMS hiện có; EBS node bật encrypted; log retention 30 ngày là cấu hình dev. Tổ chức yêu cầu customer-managed KMS key, retention dài hoặc log archive bất biến cần bổ sung policy/key tương ứng trước môi trường quan trọng.
- **Egress chưa được siết hoàn chỉnh.** NAT và security group bootstrap không thay thế egress policy. NetworkPolicy, endpoint và kiểm soát image thuộc giai đoạn hardening, không được coi là đã có.

### 7. Kiểm tra hoàn thành

Chỉ đánh dấu hoàn thành khi có kết quả kiểm tra thật:

```text
[ ] Terraform module EKS dùng lại được, root dev có state riêng
[ ] Identity/account/region đúng và plan được review
[ ] Cluster Active, managed node group Active
[ ] Hai node Ready; đã kiểm tra AZ và private IP
[ ] VPC CNI, kube-proxy, CoreDNS Active/healthy
[ ] DNS test và ECR pull test thành công, hai Pod test đã xóa
[ ] Operator role truy cập được, creator không được cấp admin tự động
[ ] App CI không có cluster-admin
[ ] VPC CNI dùng IRSA role riêng; node role không có CNI policy
[ ] API endpoint đúng đường quản trị/CIDR đã chọn
[ ] Audit/authenticator logs có dữ liệu
[ ] Provider lock được commit; version thực tế đã chọn được ghi trong ghi chú/version record của repo (không chỉ placeholder)
[ ] Chưa deploy app/Argo CD/data service
[ ] Đã chốt tiếp tục buổi học hoặc cleanup tài nguyên
```

Đây là checklist để người thực hành điền, không phải xác nhận AWS đã được triển khai khi tài liệu được viết.

### 8. Lỗi thường gặp và cách xử lý

| Hiện tượng | Nguyên nhân thường gặp | Cách kiểm tra/xử lý |
|---|---|---|
| AWS CLI không biết `describe-cluster-versions` | CLI cũ | Cập nhật AWS CLI v2; tra version tại EKS Console và tài liệu chính thức |
| Init báo access denied S3/KMS/lock | Sai profile hoặc thiếu quyền backend | Kiểm tra STS identity, policy của role và key policy |
| NodeCreationFailure | Route/NAT, subnet, IAM, AMI hoặc CNI lỗi | Xem health node group; kiểm tra route và role CNI trước khi retry |
| CNI báo AccessDenied | Sai OIDC trust/audience/service account hoặc policy | So role annotation với trust `kube-system:aws-node`; kiểm tra add-on health |
| kubectl timeout | Private-only nhưng máy không có route, hoặc sai public CIDR | Sửa đường quản trị/CIDR bằng Terraform; không mở toàn Internet |
| kubectl Unauthorized/Forbidden | Profile sai role hoặc Access Entry chưa đúng | So IAM role ARN với entry; không chép ARN STS session |
| CoreDNS Pending sau khi apply | Node chưa Ready/thiếu capacity/taint | Xem `kubectl describe pod` và node; giải quyết scheduling |
| Pod test ImagePullBackOff | Egress/DNS hoặc registry giới hạn | Xem events; kiểm tra NAT và dùng registry/image đã duyệt |
| Add-on conflict | Có cấu hình cũ do tạo tay | Review khác biệt rồi quản lý bằng Terraform; không mặc định overwrite |
| AMI/add-on không tương thích | Copy version của minor khác | Tra lại bằng đúng `kubernetes_version` rồi tạo plan mới |
| Hai node cùng AZ | Phân bố node thực tế chưa đạt kỳ vọng | Kiểm tra subnet/AZ/ASG capacity; chưa đánh dấu HA nếu chưa kiểm chứng |
| Plan có destroy VPC hoặc đổi môi trường | Chạy nhầm root/state hoặc biến | Dừng apply, kiểm tra backend key và working directory |
| Ngừng EC2 mà phí còn tăng | EKS control plane/NAT/logs còn tồn tại | Dùng cleanup theo dependency, không chỉ stop EC2 |

### 9. Kết quả sau bước này

Sau khi thực hành thành công, ta có nền EKS dev được quản lý bằng Terraform, node private chạy được, IAM tách vai trò và có bằng chứng kiểm tra DNS/audit.

Bước tiếp theo theo V2 là **Bước 4.2 - Cài add-on nền cho EKS dev**: tiếp tục EBS CSI, Metrics Server, identity cho controller và các policy/namespace nền. Argo CD vẫn ở bước 4.7, deploy 3 service ở bước 4.8.


---

> **Phạm vi phần tiếp theo:** các file bên dưới đã được chuẩn bị để thực hành; chưa có kết quả apply AWS. Lệnh trong code block là lệnh người học chạy sau khi kiểm tra account, region, context và plan. Không đánh dấu nghiệm thu bằng việc chỉ tạo file.
>
> **Quy ước đường dẫn:** repo hạ tầng là `D:\AWS\springboot-learning`; GitOps là `D:\AWS\social-media-app-gitops`. Branch GitOps là `master`. Các file `terraform.tfvars.example` phải được copy thành `terraform.tfvars` và thay placeholder trước khi dùng. Không copy state hoặc thư mục `.terraform` giữa các root.
>
> **Thứ tự state:** network → eks → addons → data → argocd → observability. State của addons/argocd dùng Helm provider nên identity thực thi phải có quyền Kubernetes và đường mạng tới API. Root EKS chỉ dùng AWS/TLS provider; không gộp Helm vào lượt tạo cluster.
>
> **Giới hạn source hiện tại:** ba app chưa có luồng nghiệp vụ Kafka/S3; upload hiện dùng Cloudinary. Các Job course-probe kiểm tra hạ tầng và bài Kafka/S3 độc lập. UAA/post còn `permitAll()`, schema dev dùng `ddl-auto=update`. Không gọi việc dựng thành công hạ tầng là hoàn tất bảo mật/nghiệp vụ production.


---

## Bước 4.2 - Cài add-on nền cho EKS dev

### 1. Mục tiêu của bước này

Cài EBS CSI, Metrics Server, AWS Load Balancer Controller (LBC), External Secrets Operator (ESO), namespace và policy nền. Mỗi controller có IAM role riêng.

### 2. Vì sao cần làm bước này

Node Ready chưa có ALB, EBS volume, metrics cho HPA hoặc cơ chế lấy secret. Phải cài CRD/controller trước khi apply tài nguyên phụ thuộc.

### 3. Trước khi bắt đầu cần có gì

4.1 đạt node/DNS nghiệm thu; profile hạ tầng assume được operator role, có đường tới EKS API. Review/apply network tag-only change và EKS NetworkPolicy change trước khi bật policy.

### 4. Thao tác chi tiết


1. Mở `terraform/environments/dev/addons`. Copy `terraform.tfvars.example` thành `terraform.tfvars`, điền operator role ở `kubernetes_role_arn`. Không dùng app CI role.
2. Tra và pin version EBS addon tương thích Kubernetes; tra ba Helm chart. LBC image/policy đang cùng `v2.14.1`: chọn chart tương thích; nếu đổi version thì đổi cả policy JSON và image. ESO chart phải cung cấp API `external-secrets.io/v1`.

```powershell
Set-Location D:\AWS\springboot-learning\terraform\environments\dev\addons
Copy-Item terraform.tfvars.example terraform.tfvars
aws eks describe-addon-versions --region ap-southeast-1 --addon-name aws-ebs-csi-driver --kubernetes-version <MINOR_DA_CHON>
helm repo add eks https://aws.github.io/eks-charts
helm repo add external-secrets https://charts.external-secrets.io
helm repo add metrics-server https://kubernetes-sigs.github.io/metrics-server/
helm repo update
helm search repo eks/aws-load-balancer-controller --versions
helm search repo external-secrets/external-secrets --versions
helm search repo metrics-server/metrics-server --versions
```

Thay mọi `<...>`/`REPLACE_...` trước khi chạy. Dùng `helm show chart <repo/chart> --version <version>` để kiểm tra appVersion của chart.

3. Review plan: ESO chỉ đọc prefix `newgate2601/dev/*`, EBS role dùng EBS CSI policy, LBC dùng JSON vendored. Không gắn cả ba policy vào node role.

```powershell
terraform init
terraform fmt
terraform validate
terraform plan -out=addons-dev.tfplan
terraform show -no-color addons-dev.tfplan
terraform apply addons-dev.tfplan
```

4. Xác nhận context dev, tạo namespace/quota/NetworkPolicy/SecretStore/StorageClass:

```powershell
kubectl config current-context
kubectl apply --dry-run=server -k D:\AWS\social-media-app-gitops\platform\dev
kubectl apply -k D:\AWS\social-media-app-gitops\platform\dev
kubectl -n social-media-dev get resourcequota,limitrange,networkpolicy,secretstore
kubectl get storageclass gp3-retain
```

Namespace dùng Pod Security restricted. Rule egress cho DNS, HTTPS và các data port; HTTPS hiện mở port 443 để AWS/API hoạt động, chưa phải domain allowlist. Chỉ nghiệm thu NetworkPolicy khi aws-node có network-policy agent và kiểm tra chặn traffic thành công.


### 5. File/config/lệnh liên quan

- `terraform/environments/dev/addons/`
- `terraform/modules/irsa/`
- `terraform/policies/aws-load-balancer-controller-v2.14.1.json`
- `social-media-app-gitops/platform/dev/`

### 6. Giải thích từng phần quan trọng

Root addons dùng AWS provider cho IAM/EBS addon và Helm provider cho controller. Atomic của Helm không thay kiểm tra logs. gp3-retain giữ EBS khi xóa PVC; cần cleanup volume riêng. SecretStore dùng IRSA của ESO controller, không chứa access key.

### 7. Kiểm tra hoàn thành


```powershell
kubectl -n kube-system get pods
kubectl -n external-secrets get pods
kubectl top nodes
kubectl -n social-media-dev get secretstore aws-secrets-manager
```

Cần controller healthy, top nodes có số liệu, SecretStore Ready; network-policy agent chạy. Ghi bằng chứng Pod Security từ chối workload không phù hợp bằng server-side dry-run.


### 8. Lỗi thường gặp và cách xử lý

Helm Unauthorized: kiểm tra operator role/trust. Timeout: kiểm tra private route hoặc public /32. Unknown SecretStore: ESO CRD/version sai. LBC không tìm subnet: thiếu tags/IP. Không tắt kubelet TLS để che lỗi metrics.

### 9. Kết quả sau bước này

Controller và namespace dev sẵn sàng; chưa deploy app. Chuyển 4.3.


---

## Bước 4.3 - Triển khai RDS PostgreSQL dev

### 1. Mục tiêu của bước này

Một RDS private, hai database logic và hai app user riêng; RDS quản lý master password trong Secrets Manager.

### 2. Vì sao cần làm bước này

App không dùng master account. Một instance với hai database tiết kiệm lab nhưng không tách blast radius như hai RDS độc lập.

### 3. Trước khi bắt đầu cần có gì

4.2 đạt; chọn PostgreSQL engine version/node class được hỗ trợ tại region; isolated subnets nhiều AZ và SG nguồn EKS đúng.

### 4. Thao tác chi tiết


1. Trong `dev/data/terraform.tfvars` bật `enable_rds=true`; giữ cache/MSK/S3 false. Điền postgres_version và final_snapshot_identifier duy nhất cho buổi học. Các version của dịch vụ chưa bật có thể để placeholder.

```powershell
Set-Location D:\AWS\springboot-learning\terraform\environments\dev\data
terraform init
terraform validate
terraform plan -out=data-dev.tfplan
terraform show -no-color data-dev.tfplan
terraform apply data-dev.tfplan
terraform output
```

Plan tạo RDS private, SG 5432 từ EKS, hai secret metadata và role db-bootstrap. Master secret do RDS tạo; hai app secret chưa có value cho đến khi bootstrap.

2. Review/apply root shared-services/ecr để có repository course-tools, rồi build image công cụ một lần. Chọn tag mới mỗi lần build vì ECR immutable.

```powershell
Set-Location D:\AWS\springboot-learning\terraform\environments\shared-services\ecr
terraform plan -out=ecr-tools.tfplan
terraform show -no-color ecr-tools.tfplan
terraform apply ecr-tools.tfplan
Set-Location D:\AWS\springboot-learning\course-tools
$toolsRegistry = "150914615641.dkr.ecr.ap-southeast-1.amazonaws.com"
$toolsImage = "$toolsRegistry/newgate2601-shared-services/course-tools:course-20260909-a"
aws ecr get-login-password --region ap-southeast-1 | docker login --username AWS --password-stdin $toolsRegistry
docker build -t $toolsImage .
docker push $toolsImage
aws ecr describe-images --repository-name newgate2601-shared-services/course-tools --image-ids imageTag=course-20260909-a --query "imageDetails[0].imageDigest" --output text
```

Đợi image scan, review findings trước khi dùng. Ghi digest vào `operations/dev/db-bootstrap.yaml`; điền MASTER_SECRET_ARN và RDS_HOST bằng output Terraform. ARN là metadata, không phải password.

3. Chạy Job một lần:

```powershell
kubectl apply --dry-run=server -f D:\AWS\social-media-app-gitops\operations\dev\db-bootstrap.yaml
kubectl apply -f D:\AWS\social-media-app-gitops\operations\dev\db-bootstrap.yaml
kubectl -n social-media-dev wait --for=condition=Complete job/db-bootstrap --timeout=600s
kubectl -n social-media-dev logs job/db-bootstrap
```

Job dùng TLS verify-full với CA RDS; tạo social_media_app_uaa/uaa_app và social_media_app_post/post_app. Password được sinh trong bộ nhớ, ghi trực tiếp vào Secret Manager JSON; không in password. Chạy lại giữ credential cũ nếu secret đã có.

4. Sau khi xác minh, xóa Job và ServiceAccount db-bootstrap. Role này đọc master secret, không được dùng cho Pod ứng dụng. Chỉ tạo lại khi cần thao tác quản trị được review.


### 5. File/config/lệnh liên quan

- `terraform/modules/rds-postgresql/`
- `terraform/environments/dev/data/`
- `course-tools/`
- `social-media-app-gitops/operations/dev/db-bootstrap.yaml`

### 6. Giải thích từng phần quan trọng

manage_master_user_password tránh quản lý master secret_string trong Terraform. App database owner đủ cho ddl-auto update ở dev; production cần migration role và runtime role hẹp hơn. Snapshot ID phải không trùng tên đã tồn tại.

### 7. Kiểm tra hoàn thành

RDS Available, Publicly accessible=No, encryption/backup đúng; Job Complete; hai app secret có url/username/password và không dùng master username. JDBC URL dùng sslmode=verify-full với CA /etc/rds/ca.pem.

### 8. Lỗi thường gặp và cách xử lý

Timeout: route/SG/NetworkPolicy. TLS lỗi: host/CA sai, không tắt verify để nghiệm thu. Secret chưa có version: Job chưa ghi thành công. DB tồn tại sai owner: kiểm tra có chủ đích, không ghi đè tùy tiện.

### 9. Kết quả sau bước này

Hai database logic và app credentials sẵn sàng sau bootstrap thật. Chuyển 4.4.


---

## Bước 4.4 - Triển khai ElastiCache Redis dev

### 1. Mục tiêu của bước này

Tạo Redis OSS private có TLS, authentication và mã hóa at-rest; xuất secret JSON cho post-service. Redis OSS là lựa chọn cụ thể trong phạm vi Redis/Valkey của V2.

### 2. Vì sao cần làm bước này

Cả server Redis và Spring client phải đồng ý dùng TLS; chỉ đổi host từ redis local sang ElastiCache là chưa đủ.

### 3. Trước khi bắt đầu cần có gì

Tra Redis engine version/node type hỗ trợ region. Chọn một node dev hoặc primary+replica khi học HA.

### 4. Thao tác chi tiết


1. Giữ enable_rds=true và bật enable_cache=true trong dev/data/terraform.tfvars. Điền redis_version. cache_replicas=0 cho lab đầu; 1 cho bài failover và cần review chi phí.
2. Tạo và đọc plan; chỉ được bổ sung cache/secret, không thay RDS ngoài ý định.

```powershell
Set-Location D:\AWS\springboot-learning\terraform\environments\dev\data
terraform plan -out=cache-dev.tfplan
terraform show -no-color cache-dev.tfplan
terraform apply cache-dev.tfplan
terraform output cache_identifier
terraform output cache_secret_arn
```

3. Đợi ElastiCache Available. Secret newgate2601/dev/post-service-redis chứa host/port/password. Values post-service đã có SPRING_DATA_REDIS_SSL_ENABLED=true và ExternalSecret mapping.
4. Chưa deploy post nếu RDS/secret/cache chưa ready. Không copy token vào YAML.


### 5. File/config/lệnh liên quan

- `terraform/modules/elasticache/`
- `terraform/environments/dev/data/terraform.tfvars`
- `social-media-app-gitops/applications/post-service/values-dev.yaml`

### 6. Giải thích từng phần quan trọng

num_cache_clusters=1+replicas, failover chỉ bật khi có replica. Random password/cache secret value có trong encrypted Terraform state; sensitive chỉ che output, không xóa khỏi state. Backend read phải giới hạn. SET là trạng thái auth ban đầu, không phải workflow rotation đầy đủ.

### 7. Kiểm tra hoàn thành

ElastiCache Available, endpoint private, TLS/auth enabled; secret có đúng key. Sau 4.8, request app thực sự truy cập cache thành công và logs không in credential.

### 8. Lỗi thường gặp và cách xử lý

SSL lỗi: env client chưa bật. NOAUTH: secret/key mapping sai. Vẫn gọi redis local: env chưa vào Pod hoặc chưa rollout. Version/class không hỗ trợ: tra lại trước plan.

### 9. Kết quả sau bước này

Cache và đường secret đã có; tiếp tục MSK ở 4.5.


---

## Bước 4.5 - Triển khai Amazon MSK Kafka dev

### 1. Mục tiêu của bước này

MSK Provisioned hai broker ở hai AZ, IAM authentication trên TLS 9098; bài kiểm tra producer/consumer, retry, duplicate và DLQ độc lập.

### 2. Vì sao cần làm bước này

MSK dùng IAM permissions ngoài security group. Có bootstrap endpoint chưa chứng minh producer/consumer được cấp đúng quyền hoặc ứng dụng có tích hợp Kafka.

### 3. Trước khi bắt đầu cần có gì

Sẵn sàng chi phí MSK theo giờ. Chọn Kafka version/node class hỗ trợ region. Hai isolated subnet phải ở hai AZ khác nhau. Image course-tools đã build và scan ở 4.3.

### 4. Thao tác chi tiết


1. Bật enable_msk=true trong dev/data/terraform.tfvars, giữ các dịch vụ đang dùng true; điền kafka_version. Module dùng hai broker, replication.factor=2 và min.insync.replicas=2.

```powershell
Set-Location D:\AWS\springboot-learning\terraform\environments\dev\data
terraform plan -out=msk-dev.tfplan
terraform show -no-color msk-dev.tfplan
terraform apply msk-dev.tfplan
terraform output kafka_bootstrap_brokers
terraform output course_probe_role_arn
```

2. Đợi MSK Active. Kiểm tra public access Disabled, client TLS, IAM bật và unauthenticated=false. Security group chỉ mở 9098 từ EKS cùng broker traffic nội bộ.
3. Điền digest course-tools, KAFKA_BOOTSTRAP vào operations/dev/course-probe.yaml. S3 chưa tạo thì đặt S3_BUCKET thành chuỗi rỗng, không giữ placeholder.
4. Chạy một Job mỗi lần, tránh hai consumer cùng group course-probe tranh partition:

```powershell
kubectl apply -f D:\AWS\social-media-app-gitops\operations\dev\course-probe.yaml
kubectl -n social-media-dev wait --for=condition=Complete job/course-probe --timeout=600s
kubectl -n social-media-dev logs job/course-probe
```

Job tạo topic course-events và course-events-dlq, gửi một event đúng hai lần và một poison event. Nó retry poison ba lần, đưa vào DLQ rồi commit offset; duplicate được bỏ qua trong bộ nhớ của lượt chạy. Chỉ coi test đạt khi log PASS và Job Complete. Trước chạy lại, xóa Job cũ (Pod template Job là immutable).

**Giới hạn bài tập:** dedupe hiện chỉ trong một process, chưa bền qua restart. Exactly-once nghiệp vụ cần durable inbox/unique constraint/outbox và kiểm thử transaction. Ba Spring service hiện chưa dùng Kafka; Job chứng minh tầng hạ tầng, không chứng minh nghiệp vụ app.


### 5. File/config/lệnh liên quan

- `terraform/modules/msk/`
- `terraform/environments/dev/data/main.tf`
- `course-tools/connectivity.py`
- `social-media-app-gitops/operations/dev/course-probe.yaml`

### 6. Giải thích từng phần quan trọng

IAM data-plane kafka-cluster:* khác AWS API kafka:*. Policy probe giới hạn cluster, topic course-* và group course-*. MSK log retention 24h phù hợp lab. Với RF=2/minISR=2, mất một broker làm producer acks=all dừng ghi; đây ưu tiên durability, không phải HA ghi như cụm 3 broker RF3/minISR2.

### 7. Kiểm tra hoàn thành

MSK Active, hai broker/two AZ, TLS/IAM đúng; Job Complete, một good event xử lý, một duplicate bị bỏ, poison được acknowledged vào DLQ. Ghi log không chứa token.

### 8. Lỗi thường gặp và cách xử lý

SASL access denied: IRSA trust/audience hoặc ARN topic/group sai. Timeout: dùng port9098 IAM endpoint, SG/NP/data subnet. NotEnoughReplicas: minISR không đạt. Topic exists được xử lý idempotent; không xóa topic cũ để che lỗi.

### 9. Kết quả sau bước này

Có bằng chứng Kafka ở mức lab. Tiếp tục S3/secret 4.6.


---

## Bước 4.6 - Triển khai S3 và Secrets Manager dev

### 1. Mục tiêu của bước này

Bucket ứng dụng private, versioning, TLS-only và role scoped prefix course/; hoàn thiện hợp đồng secret theo environment.

### 2. Vì sao cần làm bước này

S3 bucket private không có nghĩa workload tự đọc được; workload cần IAM riêng. Metadata secret trong GitOps khác secret value trong Secrets Manager.

### 3. Trước khi bắt đầu cần có gì

Data/IRSA đã có. Chốt account/region và bucket name globally unique. Hiểu versioning làm DeleteObject tạo delete marker, chưa xóa các version.

### 4. Thao tác chi tiết


1. Trong dev/data/terraform.tfvars bật enable_s3=true; giữ các flag trước. Review/apply:

```powershell
Set-Location D:\AWS\springboot-learning\terraform\environments\dev\data
terraform plan -out=s3-dev.tfplan
terraform show -no-color s3-dev.tfplan
terraform apply s3-dev.tfplan
terraform output bucket_name
```

2. Kiểm tra S3 Console: Block Public Access toàn bộ, BucketOwnerEnforced, SSE-S3, versioning enabled; bucket policy từ chối HTTP. Không gắn public-read ACL.
3. Điền S3_BUCKET vào course-probe.yaml. Nếu chỉ kiểm tra S3, để KAFKA_BOOTSTRAP rỗng; nếu MSK còn chạy, giữ endpoint để kiểm tra cả hai. Xóa Job cũ rồi apply/chờ Complete.
4. Kiểm tra secret contract:

| Secret AWS | Kubernetes Secret | Key |
|---|---|---|
| newgate2601/dev/uaa-service-db | uaa-service-db | url, username, password |
| newgate2601/dev/post-service-db | post-service-db | url, username, password |
| newgate2601/dev/post-service-redis | post-service-redis | host, port, password |

ESO chỉ đọc prefix môi trường; master secret RDS nằm ngoài prefix đó. Không tạo ExternalSecret để phát master credential cho app.
5. Bucket course-tools probe chỉ cho đọc/ghi object prefix course/. Khi tích hợp S3 vào nghiệp vụ post-service, tạo role và service-account binding riêng với prefix ứng dụng; không tái dùng role probe có quyền tạo Kafka topic.

S3 roundtrip trong Job không tự thay Cloudinary upload. Phần migration nghiệp vụ được ghi ở checklist 4.8/6.1, không tự đổi source trong khóa hạ tầng này.


### 5. File/config/lệnh liên quan

- `terraform/modules/s3-app/`
- `terraform/environments/dev/data/main.tf`
- `social-media-app-gitops/platform/dev/secret-store.yaml`
- `social-media-app-gitops/operations/dev/course-probe.yaml`

### 6. Giải thích từng phần quan trọng

force_destroy=false ngăn Terraform xóa bucket có dữ liệu. Lifecycle chỉ hết hạn version cũ sau 7 ngày và bỏ multipart dang dở; không xóa object hiện tại. SSE-S3 là mã hóa managed; yêu cầu CMK riêng phải bổ sung key/policy có review.

### 7. Kiểm tra hoàn thành

Job PASS S3 put/get/delete; AWS Console có version/delete marker đúng kỳ vọng; anonymous access bị từ chối; role không có s3:* trên toàn account. Không có secret value trong Git.

### 8. Lỗi thường gặp và cách xử lý

AccessDenied: kiểm tra prefix course/, role IRSA, TLS. BucketNameAlreadyExists: chọn tên khác. Delete không giải phóng storage ngay: kiểm tra versions và lifecycle. SM pending deletion: restore secret cũ hoặc chọn tên mới có chủ đích, không force-delete để né lỗi.

### 9. Kết quả sau bước này

Data plane và hợp đồng secrets đã được chuẩn bị. Cài Argo CD ở 4.7.


---

## Bước 4.7 - Cài Argo CD và GitOps dev

### 1. Mục tiêu của bước này

Argo CD đọc repo private branch master, AppProject giới hạn repo/namespace/resource kinds. App CI không chạy kubectl.

### 2. Vì sao cần làm bước này

Tách thay đổi source/image khỏi lựa chọn release. GitOps commit là lịch sử desired state; Argo sync vào cluster sau review.

### 3. Trước khi bắt đầu cần có gì

4.2–4.6 đạt theo phase sử dụng; SecretStore Ready; GitOps đã push master; ba digest thực chưa bắt buộc lúc cài Argo nhưng phải có trước sync app.

### 4. Thao tác chi tiết


1. Copy dev/argocd/terraform.tfvars.example thành terraform.tfvars, điền operator role và pinned argo_chart_version. Dùng helm repo add argo https://argoproj.github.io/argo-helm rồi helm search repo argo/argo-cd --versions để chọn.

```powershell
Set-Location D:\AWS\springboot-learning\terraform\environments\dev\argocd
terraform init
terraform validate
terraform plan -out=argocd-dev.tfplan
terraform show -no-color argocd-dev.tfplan
terraform apply argocd-dev.tfplan
kubectl -n argocd get pods
kubectl -n argocd port-forward svc/argocd-server 8443:443
```

Argo server chỉ là ClusterIP; truy cập https://localhost:8443. Certificate ban đầu có thể self-signed: xác minh đang port-forward đúng cluster. Không bật ingress public/admin mặc định.

2. Lấy initial admin password trên máy cá nhân; không copy vào Git, ảnh hoặc log chia sẻ:

```powershell
$argoPasswordBase64 = kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}"
[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($argoPasswordBase64))
```

Đăng nhập admin, đổi password ngay. Doanh nghiệp cấu hình SSO/RBAC theo nhóm; initial admin chỉ phục vụ bootstrap, disable admin khi SSO đã được kiểm chứng và có break-glass.

3. GitLab -> Project GitOps -> Settings -> Repository -> Deploy tokens: tạo token read_repository có expiry. Trong Argo UI -> Settings -> Repositories -> Connect repo: Git/HTTPS, URL GitOps, username/token. Đây là credential riêng cho Argo đọc repo, không dùng token mở MR của CI. Token được Argo giữ trong Kubernetes Secret; không lưu vào Terraform values/state hoặc YAML.

4. Apply AppProject trước Applications:

```powershell
kubectl apply -f D:\AWS\social-media-app-gitops\argocd\projects\dev.yaml
```

Project chỉ cho GitOps URL và namespace social-media-dev, không cấp cluster-scoped resources. Namespace/SecretStore/StorageClass do bootstrap platform quản lý riêng. Manifest app targetRevision=master và sync manual; không apply cả dev/staging/production vào cùng cluster.

5. Chưa bấm Sync trước khi điền digest ở 4.8. Chart giờ fail rõ khi digest rỗng/sai định dạng, thay vì tạo image NOT_SET.


### 5. File/config/lệnh liên quan

- `terraform/environments/dev/argocd/`
- `social-media-app-gitops/argocd/projects/dev.yaml`
- `social-media-app-gitops/argocd/dev/`

### 6. Giải thích từng phần quan trọng

AppProject là giới hạn logic của Argo, không thay toàn bộ cluster RBAC. Helm Argo bootstrap dùng quyền platform và phải được quản lý đặc quyền. ignoreDifferences replicas giúp Argo không giành quyền scale của HPA. SecretStore namespace không nằm trong app project whitelist vì bootstrap quản lý nó.

### 7. Kiểm tra hoàn thành

Argo healthy; repository Connection status Successful; project không cho namespace khác; chỉ dev Applications được chuẩn bị. Initial password đã đổi; token read-only có expiry. Chưa tự sync production.

### 8. Lỗi thường gặp và cách xử lý

Repository auth: token scope/expiry/username. InvalidSpecError: sai repo/namespace whitelist. ComparisonError digest: chưa chọn image, không nới helper về latest. Namespace không tồn tại: hoàn thành platform bootstrap.

### 9. Kết quả sau bước này

Argo CD dev sẵn sàng đọc GitOps master. Chuyển 4.8 deploy.


---

## Bước 4.8 - Deploy gateway, uaa-service và post-service vào EKS

### 1. Mục tiêu của bước này

Build/test/scan image, chọn digest, mở GitOps MR, sync dev và kiểm tra request qua gateway. Cấu hình HPA, PDB, probes và rollout có kiểm soát.

### 2. Vì sao cần làm bước này

CI success không chứng minh workload chạy được. Cần kiểm tra secret, TLS DB/cache, discovery, resource budget và đường ingress.

### 3. Trước khi bắt đầu cần có gì

RDS/cache app secret có value; SecretStore Ready; Argo repository connected; ECR có image thật. Chỉ bật ingress sau khi review ứng dụng permitAll; mặc định ingress vẫn tắt.

### 4. Thao tác chi tiết


#### 4.1. Chọn candidate từ CI hiện có

Ba app dùng `.gitlab-ci.yml` trong từng repo. Cấu hình này build/push image vào ECR và xuất artifact `image.env`; chưa tự mở GitOps MR. Job có tên `test` hiện chỉ compile với `-DskipTests`, chưa chạy bộ kiểm thử, và pipeline chưa có scan gate. Không coi pipeline xanh hiện tại là đã đạt tiêu chuẩn release doanh nghiệp.

1. Chạy pipeline trên branch được rules cho phép (`main`, `staging`, `develop`), kiểm tra build/push thành công.
2. Lấy digest từ `IMAGE_URI` trong `image.env` hoặc đối chiếu ECR. Chỉ dùng phần `sha256:...` cho `image.digest`, không dùng tag hoặc commit SHA.
3. Tạo nhánh candidate trong repo GitOps từ `master`, cập nhật đúng `applications/<service>/values-dev.yaml`, review diff, push nhánh và mở MR vào `master` bằng GitLab UI.
4. Reviewer kiểm tra provenance image, test/scan evidence và cấu hình trước khi merge. Sau merge, Argo CD sync theo các mục tiếp theo; CI không chạy `kubectl`.

Đây là thao tác MR thủ công có review cho bài thực hành. Hướng nâng cấp doanh nghiệp là bổ sung test thật, scan và OIDC trực tiếp vào pipeline đang dùng, rồi tự động mở MR bằng release job được quản lý tập trung. Chưa có các gate đó thì giữ mục nghiệm thu tương ứng ở trạng thái chưa đạt.

Root `terraform/environments/shared-services/ci` vẫn là cấu hình IAM/OIDC chuẩn bị cho nâng cấp: trust giới hạn đúng project/branch `staging`, audience `sts.amazonaws.com`. Chỉ tạo IAM role không tự chuyển pipeline hiện tại từ access key sang OIDC; cần cập nhật pipeline trong một thay đổi riêng có kiểm tra.

#### 4.2. Review chart và image selection

Chart đã thêm ConfigMap, CA RDS, startup/readiness/liveness probes, /tmp writable, non-root UID10001, read-only root FS, HPA, PDB và topology spread. Gateway cần token Kubernetes để discovery; backend không automount token.

```powershell
Set-Location D:\AWS\social-media-app-gitops
helm lint charts/springboot-service -f applications/gateway/values-dev.yaml
helm template gateway charts/springboot-service -f applications/gateway/values-dev.yaml
helm template uaa-service charts/springboot-service -f applications/uaa-service/values-dev.yaml
helm template post-service charts/springboot-service -f applications/post-service/values-dev.yaml
```

Chỉ chạy render sau khi chọn digest thật. Kiểm tra image@sha256, SecretStore, namespace đích, env SSL Redis và CA JDBC. Không chỉnh registry ECR sang môi trường khác khi promote; image shared dùng cùng repository.

#### 4.3. Đăng ký và sync app

```powershell
kubectl apply -f D:\AWS\social-media-app-gitops\argocd\dev
```

Trong Argo UI, sync uaa-service-dev, post-service-dev, sau đó gateway-dev. Theo dõi ExternalSecret Ready trước khi kết luận lỗi app. Nếu startup đã fail do secret chưa tồn tại, đợi secret rồi rollout/sync lại có kiểm soát.

```powershell
kubectl -n social-media-dev get externalsecret
kubectl -n social-media-dev get deployments,pods,services,hpa,pdb
kubectl -n social-media-dev rollout status deployment/uaa-service --timeout=600s
kubectl -n social-media-dev rollout status deployment/post-service --timeout=600s
kubectl -n social-media-dev rollout status deployment/gateway --timeout=600s
kubectl -n social-media-dev port-forward svc/gateway 8081:8081
```

Kubernetes Service thực tế trong repo là `gateway`, không phải tên gợi ý `gateway-service` trong bảng V2. Gateway routes theo `/services/<service-name>/**`. Từ terminal khác, gọi endpoint đã có trong source:

```powershell
Invoke-WebRequest http://localhost:8081/actuator/health/readiness
Invoke-WebRequest http://localhost:8081/services/uaa-service/api/v1/user/tiny/list
```

Endpoint nghiệp vụ có thể cần query/body/test data; response hợp lệ theo contract app quan trọng hơn chỉ HTTP200 health. Flow RTC/web-push nằm ngoài ba service đang triển khai; không chọn endpoint phụ thuộc chúng để nghiệm thu phạm vi này.

#### 4.4. Bật ALB khi sẵn sàng

Mở examples/ingress-dev.yaml, điền domain, ACM certificate cùng region và IP quản trị /32. Merge overlay ingress vào gateway values-dev.yaml rồi mở MR; không apply file overlay bằng kubectl (nó là Helm values). Sau merge master, Argo sync tạo ALB qua LBC.

ACM certificate phải Issued; DNS record domain trỏ ALB. Truy cập HTTPS và kiểm tra target health. Giữ inbound-cidrs giới hạn người học vì source permitAll; không coi đây là public production release. Chặn/không expose actuator nhạy cảm khi thiết kế ingress production.

#### 4.5. Nghiệm thu phạm vi thật

Post/uaa kết nối PostgreSQL và post kết nối Redis là test app. Kafka/S3 Job 4.5–4.6 là test hạ tầng. V2 yêu cầu kết nối nghiệp vụ Kafka/S3: đánh dấu **chưa đạt nghiệp vụ** cho đến khi app được tích hợp, không dùng kết quả Job để đánh dấu thay.


### 5. File/config/lệnh liên quan

- `social-media-app-*/.gitlab-ci.yml`
- `terraform/environments/shared-services/ci/`
- `social-media-app-gitops/charts/springboot-service/`
- `social-media-app-gitops/applications/`
- `social-media-app-gitops/examples/ingress-dev.yaml`

### 6. Giải thích từng phần quan trọng

MR chứa đúng digest, không có secret. ECR tag có thể giúp tra cứu nhưng deploy dùng digest. replicas bỏ khỏi Deployment khi HPA bật; Argo ignoreDifferences tránh giành scale. PDB dev tắt ở min1 để không chặn drain; bài HA bật min2/PDB. Secret env không tự đổi trong process sau ESO refresh: cần rolling restart.

### 7. Kiểm tra hoàn thành

Ba Deployment Available, Argo Synced/Healthy, ExternalSecret Ready; gateway discovery có uaa/post; DB/cache flow thật đạt; HPA có metrics; image digest khớp ECR/GitOps. Ghi rõ các nghiệp vụ ngoài phạm vi/chưa tích hợp.

### 8. Lỗi thường gặp và cách xử lý

ImagePullBackOff: digest/role ECR; không dùng latest. CreateContainerConfigError: secret key chưa có. CrashLoop: TLS, DB schema hoặc phụ thuộc thiếu. Pending: requests/quota/topology. Scan đỏ có thể đúng vì source dependency/secret hiện có: sửa nguồn trước, không tắt gate.

### 9. Kết quả sau bước này

Dev có quy trình deploy và bộ kiểm tra. Chưa tự tuyên bố hoàn tất production/nghiệp vụ Kafka-S3; tiếp tục observability.


---

## Bước 4.9 - Hoàn thiện observability và security dev

### 1. Mục tiêu của bước này

CloudWatch logs/metrics, alarm có receiver, audit và kiểm tra security baseline. Thêm evidence thay vì chỉ dựa dashboard trống.

### 2. Vì sao cần làm bước này

Không thể kiểm thử reliability nếu không đo được thời gian phục hồi, lỗi và backlog. Alarm INSUFFICIENT_DATA không phải hệ thống khỏe.

### 3. Trước khi bắt đầu cần có gì

Workload có traffic dev, node resources còn đủ. Chọn CloudWatch Observability addon version tương thích; biết chi phí Container Insights/log ingestion.

### 4. Thao tác chi tiết


1. Copy observability/terraform.tfvars.example thành terraform.tfvars, điền observability_addon_version, alarm_email nếu muốn nhận cảnh báo. Điền CacheClusterId thật (không phải replication-group ID) vào cache_node_ids.

```powershell
Set-Location D:\AWS\springboot-learning\terraform\environments\dev\observability
terraform init
terraform validate
terraform plan -out=observability-dev.tfplan
terraform show -no-color observability-dev.tfplan
terraform apply observability-dev.tfplan
kubectl -n amazon-cloudwatch get pods
```

Role agent dùng IRSA, không gắn policy lên mọi node. Log groups containerinsights giữ 7 ngày; RDS/MSK logs riêng thuộc state data. Xác nhận email SNS nếu đã tạo subscription; PendingConfirmation không nhận alert.

2. CloudWatch -> Logs: tìm application/dataplane/host/performance của cluster, filter theo namespace/pod. Kiểm tra audit log EKS, RDS PostgreSQL và MSK broker. Gửi request thật rồi đối chiếu timestamp.
3. CloudWatch -> Metrics: node memory; RDS CPU; Redis memory và Evictions; MSK consumer lag. Alarm mẫu Kafka chỉ có dữ liệu khi group course-probe/topic course-events đang tạo metric. CloudWatch thiếu metric: tra dimensions thực tế, không đổi missing thành notBreaching để che lỗi.
4. CloudWatch -> Alarms: thử sự kiện có kiểm soát hoặc ngưỡng tạm thấp trong MR, đợi hai evaluation periods; ghi ALARM/OK và email nhận. Hoàn nguyên ngưỡng sau bài test.

5. Security checks:
- Pod chạy UID10001, readOnlyRootFilesystem=true; /tmp là emptyDir.
- Namespace restricted; API/controller RBAC tách khỏi app CI.
- NetworkPolicy thực sự chặn kết nối không được phép. Chạy test Pod/Deployment được gắn label không nằm trong allowlist và kiểm tra truy cập backend bị chặn; health từ gateway vẫn chạy.
- ESO chỉ đọc secret prefix dev; không đọc master RDS.
- Source hiện chưa có structured log/trace end-to-end hoàn chỉnh. Cấu hình trace ID là bài app cần bổ sung; không lấy log collector làm bằng chứng app đã tracing.


### 5. File/config/lệnh liên quan

- `terraform/modules/observability/`
- `terraform/environments/dev/observability/`
- `social-media-app-gitops/platform/dev/network-policy.yaml`
- `social-media-app-gitops/charts/springboot-service/templates/deployment.yaml`

### 6. Giải thích từng phần quan trọng

CloudWatchAgentServerPolicy là managed policy cho controller chuyên dụng; cần review nếu tổ chức yêu cầu custom scope. treat_missing_data=missing giữ trạng thái thiếu số liệu rõ ràng. Default-deny chỉ có tác dụng nếu CNI enforce. Pod Security không thay authentication nghiệp vụ.

### 7. Kiểm tra hoàn thành

Logs có request thật, metrics có dimensions đúng, SNS receiver confirmed, alarm thử được ALARM→OK. Ghi bằng chứng denied network path và allowed app path. Danh sách permitAll/migration/Kafka-S3 còn lại vẫn mở.

### 8. Lỗi thường gặp và cách xử lý

AccessDenied agent: kiểm tra IRSA serviceaccount annotations/trust. Logs không có: agent scheduling/quota, SG egress, stdout app. Consumer lag thiếu: group chưa active hoặc dimensions sai. PDB/HPA không hoạt động: pod count và metrics.

### 9. Kết quả sau bước này

Có quan sát/alert nền và security evidence. Sang reliability 4.10.


---

## Bước 4.10 - Kiểm thử reliability và rollback dev

### 1. Mục tiêu của bước này

Thực hiện drill có mục tiêu, phạm vi, kết quả đo được và cách phục hồi; gồm Pod/node, GitOps rollback, cache, Kafka/DLQ, restore DB và secret rotation.

### 2. Vì sao cần làm bước này

Tài nguyên Active không chứng minh ứng dụng chịu lỗi. Backup tồn tại không chứng minh restore và RPO/RTO đạt.

### 3. Trước khi bắt đầu cần có gì

Chỉ dev; có snapshot/backup cần giữ; cửa sổ lab, đủ thời gian cleanup. Tạo bản ghi từ evidence/acceptance-template.json, ghi timestamp trước/sau. Không thực hiện drill trên dữ liệu cần giữ mà chưa có backup.

### 4. Thao tác chi tiết


#### 4.1. Pod và node disruption

Xóa một Pod gateway theo tên thật, quan sát Deployment tạo Pod mới và health phục hồi. Sau đó chọn HA mode: ít nhất hai replicas, HPA min2, PDB maxUnavailable1, kiểm tra capacity spare trước drain. Nếu Pod replacement không schedule được thì dừng.

```powershell
kubectl -n social-media-dev get pods -o wide
kubectl -n social-media-dev delete pod <POD_GATEWAY_DA_CHON>
kubectl -n social-media-dev rollout status deployment/gateway
kubectl drain <NODE_DA_CHON> --ignore-daemonsets
kubectl uncordon <NODE_DA_CHON>
```

Không thêm force/disable-eviction để bỏ PDB. EmptyDir /tmp có thể làm drain yêu cầu xác nhận mất dữ liệu tạm; chỉ dùng --delete-emptydir-data khi đã kiểm tra đây là scratch data, không phải dữ liệu nghiệp vụ. Đo thời gian lỗi, phục hồi và số request thất bại.

#### 4.2. GitOps rollback

Ghi digest A, deploy candidate B, theo dõi rollout. Tạo nhánh rollback từ master hiện tại và revert commit đổi digest B; mở MR, review và merge; Argo sync trở lại A. Không dùng kubectl set image làm nguồn cấu hình dài hạn. Rollback image không tự rollback database migration.

#### 4.3. Cache và Kafka

Để mô phỏng cache mất kết nối, trong nhánh GitOps drill thay post Redis host bằng hostname không tồn tại (env override), sync dev, đo timeout/error, rồi revert. Không xóa cả replication group chỉ để thử lỗi ứng dụng. App không có fallback thì ghi fail và yêu cầu sửa, không gọi crash là thành công.

Chạy course-probe Kafka, xem retry/DLQ; nghiệm thu chỉ mức lab/in-memory. Với RF2/minISR2, mất broker làm dừng ghi theo thiết kế; không giảm minISR chỉ để báo pass HA.

#### 4.4. Restore DB vào instance tách biệt

RDS Console -> Automated backups/Snapshots -> chọn recovery point, ghi thời điểm và snapshot identifier. Dùng root dev/restore với snapshot_identifier đã chọn, cùng private subnet, SG nguồn EKS. Không restore đè DB đang chạy.

Review/apply root restore, lấy endpoint mới; dùng session DB kiểm tra số bảng/row và checksum dữ liệu test so với mốc đã ghi. Đo RPO (khoảng mất dữ liệu) và RTO (từ bắt đầu restore đến test đạt). Không chỉ đo trạng thái Available. Xóa instance restore sau nghiệm thu bằng root restore; giữ snapshot theo chính sách đã chốt.

#### 4.5. Secret rotation

DB app credential: đổi password database và cập nhật đúng secret JSON trong cùng cửa sổ bảo trì; đợi ESO Ready, rolling restart Pod, kiểm tra request và credential cũ bị từ chối. Không đổi master RDS rồi tưởng app user cũng đã rotate.

Cache: dùng quy trình ROTATE cho token mới để tạm chấp nhận cũ+mới, cập nhật secret/client, restart/kiểm tra rồi SET chỉ token mới. Cấu hình random_password ban đầu không tự cung cấp workflow này; ghi change plan cụ thể trước khi thử. Không in token, không dùng command chứa password trong history.

Cuối từng drill ghi: mục tiêu, thao tác, expected, observed, timestamp, lỗi, cách sửa, trạng thái pass/fail. Evidence mẫu không được đánh dấu pass trước khi chạy.


### 5. File/config/lệnh liên quan

- `social-media-app-gitops/evidence/acceptance-template.json`
- `terraform/environments/dev/restore/`
- `social-media-app-gitops/scripts/promote.py`
- `social-media-app-gitops/operations/dev/`

### 6. Giải thích từng phần quan trọng

PDB chỉ hạn chế disruption tự nguyện, không bảo vệ trước node chết. Pod restart không sửa lỗi dữ liệu. Secret env chỉ đọc lúc khởi động process. Restore instance cần giữ network/EKS còn sống để kiểm tra.

### 7. Kiểm tra hoàn thành

Có evidence cho từng drill, RPO/RTO đo được, rollback quay đúng digest A, secret cũ bị từ chối sau chuyển đổi, không còn Job/restore instance/drill override. Drill fail giữ status fail và action item.

### 8. Lỗi thường gặp và cách xử lý

Drain treo: PDB hoặc thiếu node/replica; kiểm tra trước force. Restore không login: master credential/recovery point khác; không đổi secret app active sang DB restore. Argo OutOfSync sau sửa tay: revert/commit đúng desired state.

### 9. Kết quả sau bước này

Dev chỉ qua gate khi các drill cần thiết đạt. Giữ evidence trước khi dọn dev để học staging.


---

## Bước 5.1 - Dựng staging theo buổi học từ module đã chạy ở dev

### 1. Mục tiêu của bước này

Tái sử dụng module với root/state/CIDR và secret namespace staging riêng. Không nhân bản sửa tay module.

### 2. Vì sao cần làm bước này

Promotion cần kiểm tra cùng artifact trong môi trường gần production nhưng có ranh giới vận hành độc lập. Account lab chỉ chạy một môi trường lớn tại một thời điểm.

### 3. Trước khi bắt đầu cần có gì

Dev gate đã có evidence; ghi ba digest đạt và snapshot/schema migration cần giữ. Cleanup dev resources tính tiền theo 7.1 trước khi tạo staging, giữ shared ECR/backend có chủ đích.

### 4. Thao tác chi tiết


1. Mỗi thư mục staging/network, eks, addons, data, argocd, observability đã có root .tf và terraform.tfvars.example. Copy từng example thành terraform.tfvars và điền theo buổi staging. Không copy file backend/state dev.
2. Kiểm tra network CIDR 10.30.0.0/16, subnet CIDR không trùng dev/shared. EKS name phải newgate2601-staging-eks; secret prefix newgate2601/staging; GitOps namespace social-media-staging.
3. Dựng lần lượt: network → eks → addons → data → argocd → observability. Mỗi root init/validate/plan/review/apply riêng; không dùng vòng lặp apply toàn môi trường.

```powershell
Set-Location D:\AWS\springboot-learning\terraform\environments\staging\network
Copy-Item terraform.tfvars.example terraform.tfvars
terraform init
terraform plan -out=staging-network.tfplan
terraform show -no-color staging-network.tfplan
terraform apply staging-network.tfplan
```

Lặp quy trình tương tự từng root, chỉ sau khi gate của root trước đạt. Bật data flags theo phase như dev. Dùng kubeconfig alias staging và kiểm tra current-context trước mọi kubectl.

4. Sau addons, apply -k platform/staging. Chạy DB bootstrap staging bằng cùng tools digest đã kiểm tra. Tạo app databases/credentials mới, không dùng password dev.
5. Values staging đặt ddl-auto=validate. Vì source chưa có migration quản lý version, cần xuất/review baseline schema từ dev hoặc hoàn thiện Flyway/Liquibase rồi apply qua migration role trước khi deploy app. Không đổi validate thành update để cho staging pass.
6. Chỉ apply AppProject staging và argocd/staging trên cluster staging. Không apply cả ba thư mục môi trường. API endpoint/SG/domain/CIDR quản trị cần điền riêng theo môi trường.


### 5. File/config/lệnh liên quan

- `terraform/environments/staging/{network,eks,addons,data,argocd,observability}/`
- `social-media-app-gitops/platform/staging/`
- `social-media-app-gitops/operations/staging/`
- `social-media-app-gitops/argocd/projects/staging.yaml`

### 6. Giải thích từng phần quan trọng

Các root gọi cùng modules bằng source tương đối; khác biệt ở tfvars/state. Staging schema validate là gate thật. Replica2/DoNotSchedule cần node trải AZ và capacity; không chỉ tăng replicas trên một node.

### 7. Kiểm tra hoàn thành

Không có dependency trỏ dev, đúng state key staging, subnet/secret/context riêng. Node/DNS/ESO/data/platform gate đạt trước promotion. Schema được quản lý và audit; chưa có baseline thì staging app gate còn mở.

### 8. Lỗi thường gặp và cách xử lý

No outputs: root phụ thuộc chưa apply. Subnet dev trong plan: config sai, dừng. Schema-validation missing table: chạy migration đã review, không nới ddl-auto. Secret name pending deletion từ buổi trước: restore/tên phù hợp, không force-delete mặc định.

### 9. Kết quả sau bước này

Staging nền sẵn sàng nhận artifact dev; tiếp tục 5.2.


---

## Bước 5.2 - Promote image dev sang staging bằng cùng digest

### 1. Mục tiêu của bước này

Đổi đúng image.digest trong values-staging.yaml bằng digest đã đạt dev, qua MR vào master. Không build lại.

### 2. Vì sao cần làm bước này

Cùng source commit vẫn có thể build image khác do base/dependency thay đổi. Digest đảm bảo staging kiểm tra đúng artifact dev đã dùng.

### 3. Trước khi bắt đầu cần có gì

Ba digest dev đã lưu, còn tồn tại trong ECR; staging nền và schema đạt; có người review MR và kế hoạch rollback.

### 4. Thao tác chi tiết


1. Bắt đầu nhánh release từ GitOps master mới nhất:

```powershell
Set-Location D:\AWS\social-media-app-gitops
git switch master
git pull --ff-only
git switch -c release/staging-course-01
python scripts/promote.py --from-env dev --to-env staging --service all
git diff -- applications
```

Script chỉ sửa file local; nó kiểm tra digest hợp lệ, chỉ cho dev→staging hoặc staging→production. Không push/merge/sync. Nếu mới dọn dev, values-dev vẫn lưu digest và evidence của lần đã test.

2. Review diff: ba trường image.digest thay đổi; không tự đổi host, secret, namespace, resource hoặc schema. Commit/push nhánh release, mở MR target master trong GitLab, attach evidence dev và staging preflight.
3. Reviewer xác minh digest có trong ECR và không bị policy lifecycle xóa. Merge MR sau approval. Argo staging sync manual; kiểm tra current cluster và ba Application staging.
4. Test lại gateway/API, DB/cache, Job Kafka/S3 nếu phase đó bật; HPA/PDB và một rollout/rollback rehearsal. Ghi evidence staging cho từng service/digest.
5. Nếu lỗi, revert MR promotion trong GitOps rồi sync; không build artifact mới trong bước promotion. Sửa code cần pipeline candidate mới và đi lại dev gate.


### 5. File/config/lệnh liên quan

- `social-media-app-gitops/scripts/promote.py`
- `social-media-app-gitops/applications/*/values-staging.yaml`
- `social-media-app-gitops/evidence/acceptance-template.json`

### 6. Giải thích từng phần quan trọng

Branch release là workflow review, môi trường staging vẫn là values-staging trên master. Một branch master có thể lưu desired state cả ba môi trường. Không cần branch source tên production để deploy production.

### 7. Kiểm tra hoàn thành

Digest running Pod == GitOps staging == digest dev đã đạt; có MR/evidence; rollback target còn trong ECR; staging sync không sửa dev.

### 8. Lỗi thường gặp và cách xử lý

Digest rỗng: chọn artifact thật trước script. ECR image missing: restore/push đúng artifact theo provenance, không gán tag giả. MR conflict: rebase và review lại digest, không dùng theirs/ours mù.

### 9. Kết quả sau bước này

Staging chỉ đạt khi test thật pass. Dọn staging sau buổi, chuẩn bị production mô phỏng 6.1.


---

## Bước 6.1 - Chuẩn bị production mô phỏng hoặc apply ngắn hạn

### 1. Mục tiêu của bước này

Hoàn thiện root/tfvars và checklist production, phân biệt rõ cấu hình chuẩn bị với nghiệm thu hệ thống chạy thật.

### 2. Vì sao cần làm bước này

Không biến khoản credit lab thành production hoạt động liên tục. Nền cloud an toàn không bù được thiếu authentication, migration hoặc business integration.

### 3. Trước khi bắt đầu cần có gì

Evidence staging đạt, budget/cost estimate, RPO/RTO mục tiêu, reviewer, cửa sổ rollback và cleanup.

### 4. Thao tác chi tiết


1. Mặc định **chỉ mô phỏng**: review production roots/tfvars.example và GitOps values, không apply AWS. Production network dùng CIDR10.40.0.0/16 và one_per_az; data đặt Multi-AZ/cache replica, backup7 ngày/deletion protection; API private-only yêu cầu đường quản trị private.
2. Điền bảng trong `evidence/production-review.yaml`. Mọi gate còn false phải có action item, không đổi thành true chỉ vì file đã được sinh.

Các gate nguồn hiện phải giải quyết trước live production:
- UAA/post còn permitAll(): cần authn/authz thật, tests cho quyền truy cập và quản lý token.
- Schema chưa có migration versioned: cần baseline/migration và tách migration/runtime DB role.
- Nghiệp vụ Kafka/S3 chưa tích hợp; probe chỉ chứng minh hạ tầng.
- Source/dependency/image scan phải pass; credentials hard-code nếu có phải chuyển secret store và rotate.
- Structured logs/trace ID end-to-end chưa có evidence.
- Durable Kafka idempotency/outbox, backup restore và app rollback phải đạt.
- MSK2broker/minISR2 không có HA ghi khi mất một broker; thiết kế production cần capacity/topology phù hợp.

3. Ước tính chi phí theo giờ bằng AWS Pricing Calculator cho EKS, nodes, NAT theo AZ, ALB, RDS Multi-AZ, cache replica, MSK, logs, EBS/snapshots. Lưu thời lượng và ngưỡng dừng; không dùng giá cũ trong tài liệu như giá cam kết.
4. Nếu có buổi live sau khi đóng gate: copy từng example thành tfvars, điền version/role/domain, dựng từng root theo thứ tự như staging. Review plan riêng, chỉ dùng account/context đã chốt.
5. Production Argo vẫn sync manual. Repository credentials read-only, bot MR riêng, protected master/approval. Người tạo candidate không tự duyệt release của mình trong mô hình có nhiều người.


### 5. File/config/lệnh liên quan

- `terraform/environments/production/`
- `social-media-app-gitops/applications/*/values-production.yaml`
- `social-media-app-gitops/argocd/projects/production.yaml`
- `social-media-app-gitops/evidence/production-review.yaml`

### 6. Giải thích từng phần quan trọng

Deletion protection cần tắt có review trước cleanup, không force-delete. DoNotSchedule/multi-AZ cần capacity thực. Mô phỏng cấu hình là kết quả hợp lệ của phase lab V2 nhưng không được gọi là production-ready.

### 7. Kiểm tra hoàn thành

Có threat model, cost estimate, RPO/RTO, restore evidence, rollback plan; gate false giữ rõ. Live chỉ khi business/security gate đóng; simulation không tạo resource.

### 8. Lỗi thường gặp và cách xử lý

Plan cố tạo tài nguyên ngoài production: sai backend/variables. Private-only không kết nối: management route chưa có. Schema validation fail: thiếu migration. Budget không đủ: giữ mô phỏng, không hạ bảo mật để chạy cho xong.

### 9. Kết quả sau bước này

Có release preflight production rõ ràng; chuyển 6.2 mô phỏng promotion hoặc live khi đã đủ điều kiện.


---

## Bước 6.2 - Promote staging sang production bằng cùng digest

### 1. Mục tiêu của bước này

Chuẩn bị hoặc thực hiện MR production từ đúng digest đã đạt staging; sync manual sau approval.

### 2. Vì sao cần làm bước này

Production release cần traceability từ source → pipeline → ECR digest → staging evidence → MR → deployment, và rollback target.

### 3. Trước khi bắt đầu cần có gì

6.1 gate được review. Nếu còn gate mở thì chỉ tạo/review bản mô phỏng, không sync live. ECR giữ cả digest mới và rollback.

### 4. Thao tác chi tiết


1. Từ master mới nhất tạo nhánh release production:

```powershell
Set-Location D:\AWS\social-media-app-gitops
git switch master
git pull --ff-only
git switch -c release/production-course-01
python scripts/promote.py --from-env staging --to-env production --service all
git diff -- applications
```

2. MR phải có digest từng service, link staging evidence, schema compatibility, downtime expectation, người chịu trách nhiệm và rollback commit/digest. Không rebuild image.
3. Với mô phỏng: review diff/manifest bằng Helm template sau khi có digest thật; ghi simulated, không ghi deployed. Không cần tạo cluster production chỉ để kiểm tra format MR.
4. Với live đủ điều kiện: reviewer approve/merge master, operator chọn đúng Argo production rồi Sync thủ công. Kiểm tra rollout, health/business flow và alarm, giữ observation window đã định.
5. Rollback bằng MR revert chỉ khi schema backward-compatible. Nếu migration breaking, làm phương án đã rehearsal; không chỉ rollback image rồi hy vọng DB tương thích.


### 5. File/config/lệnh liên quan

- `social-media-app-gitops/scripts/promote.py`
- `social-media-app-gitops/applications/*/values-production.yaml`
- `social-media-app-gitops/argocd/production/`
- `social-media-app-gitops/evidence/production-review.yaml`

### 6. Giải thích từng phần quan trọng

Approval release không phải approval tạo mọi tài nguyên. Argo manual sync là một gate riêng sau Git merge. Digest giữ nguyên qua môi trường; secret/config thay theo môi trường.

### 7. Kiểm tra hoàn thành

MR chỉ chọn artifact staging đã đạt; traceability đầy đủ, rollback còn sẵn. Trạng thái simulated hoặc live được ghi đúng; live cần evidence actual digest và test.

### 8. Lỗi thường gặp và cách xử lý

Argo production trỏ cluster staging: kiểm tra registration/context, dừng sync. Digest thiếu ECR: không rebuild dưới tag cũ. Failed rollout: dùng rollback plan, không hotfix desired state bằng kubectl.

### 9. Kết quả sau bước này

Hoàn thành bài promotion đúng phạm vi mô phỏng/live đã chọn. Dọn tài nguyên theo 7.1.


---

## Bước 7.1 - Cleanup bắt buộc sau mỗi buổi thực hành

### 1. Mục tiêu của bước này

Xóa tài nguyên lab theo dependency, giữ có chủ đích backend/ECR/evidence/backup và kiểm tra tài nguyên tính tiền bị bỏ quên.

### 2. Vì sao cần làm bước này

Destroy EKS trước controller có thể để lại ALB, finalizer, EBS. Stop EC2 không dừng phí control plane/NAT/data service. Xóa state không xóa resource.

### 3. Trước khi bắt đầu cần có gì

Ghi context/account/environment, snapshot/digest cần giữ, evidence, retention và người chịu trách nhiệm. Không có lab môi trường khác dùng chung tài nguyên định xóa.

### 4. Thao tác chi tiết


#### 4.1. Dọn workload khi controller còn sống

1. Revert/merge GitOps tắt ingress, Argo sync. Đợi LBC xóa ALB/target group do ingress quản lý. Kiểm tra ALB Console, không chỉ xem ingress đã biến mất.
2. Xóa app qua Argo có kiểm soát hoặc sync xóa manifests trước khi xóa Applications. Applications hiện không có resources-finalizer tự cascade mặc định: chỉ xóa Application có thể để workload lại. Kiểm tra Deployment/Service/Ingress/PVC/Job thật còn hay không.
3. Xóa Job db-bootstrap/course-probe và ServiceAccount tạm. Kiểm tra PVC/PV; gp3-retain giữ EBS. Ghi snapshot/backup trước khi xóa volume riêng.
4. Không gỡ LBC/ESO/EBS CSI trước khi resource phụ thuộc đã dọn. Không xóa finalizer bằng force để làm dashboard sạch.

#### 4.2. Review destroy plan theo từng root

Thứ tự đề xuất cho môi trường dev:
- restore (nếu có instance kiểm tra);
- observability, sau khi đã lưu logs/evidence cần giữ;
- argocd, sau khi workload đã dọn;
- data, sau khi dữ liệu/backup/S3 versions đã được xử lý;
- addons;
- eks;
- network.

Mỗi root chạy quy trình riêng, không viết vòng lặp destroy mọi thư mục:

```powershell
Set-Location D:\AWS\springboot-learning\terraform\environments\dev\restore
aws sts get-caller-identity
terraform plan -destroy -out=cleanup.tfplan
terraform show -no-color cleanup.tfplan
terraform apply cleanup.tfplan
```

Chỉ chạy root đã từng apply, đã init và có state tương ứng. Thay thư mục cho từng root theo thứ tự; không chạy ví dụ restore nếu chưa tạo restore. Saved plan phải được tạo mới khi config/state thay đổi.

#### 4.3. Xử lý các tài nguyên cố ý được giữ

- RDS: production deletion_protection cần tắt bằng change plan trước destroy; final snapshot ID phải duy nhất. Snapshot vẫn tính phí sau xóa DB.
- S3 app: force_destroy=false. Đếm object/version/delete marker, export/backup phần cần giữ, rồi xóa phiên bản có chủ đích hoặc giữ bucket và ghi chi phí. Không xóa backend bucket.
- Secrets Manager: recovery_window=7 ngày; secret pending deletion không phải lỗi và có thể gây trùng tên khi dựng lại. Restore khi hợp lý, không force-delete mặc định.
- EBS Retain: kiểm tra attachment, snapshot và tag trước xóa volume orphan.
- ECR: giữ digest đang được chọn/rollback qua môi trường và tools digest. Lifecycle không được xóa artifact đang chạy.
- CloudWatch: export bằng chứng cần giữ trước destroy log groups; retention không phải archive bất biến.

#### 4.4. Kiểm tra AWS Console sau cleanup

Kiểm tra đúng region/account: EKS cluster, EC2/ASG, NAT Gateway, EIP unattached, ALB/target groups, RDS/replica/restore instance, ElastiCache, MSK, EBS/PV orphan, snapshots, log groups. Data state không quản lý ALB do LBC tạo nên phải kiểm tra riêng.

Backend S3/KMS/lock, ECR, CloudTrail, Budget, domain/hosted zone có thể giữ nếu có chủ đích. Network shared-services có NAT riêng: không quên review phí dù dev đã destroy. Cost Explorer có độ trễ; kết hợp inventory thực tế và billing hôm sau.

#### 4.5. Đóng khóa học bằng bằng chứng

Dùng evidence/acceptance-template.json cho từng step: chưa chạy = not_run, mô phỏng = simulated, test thất bại = failed. Không đánh dấu done toàn khóa chỉ vì đã có code.

V2 DoD còn yêu cầu nghiệp vụ Kafka/S3, auth, migration, durable retry/idempotency và tracing. Với source hiện tại, các mục đó phải giữ mở đến khi được tích hợp và test; bộ tài liệu/cấu hình này cung cấp nền, bài kiểm tra và gate rõ ràng.


### 5. File/config/lệnh liên quan

- `social-media-app-gitops/evidence/acceptance-template.json`
- `social-media-app-gitops/evidence/production-review.yaml`
- `terraform/environments/{dev,staging,production}/`
- `terraform/environments/shared-services/`

### 6. Giải thích từng phần quan trọng

Terraform destroy chỉ xử lý resource trong state của root đang chạy. Helm/controller-created resources cần cleanup khi cluster còn hoạt động. Snapshot, retained PV, ECR image versions và bucket versions có vòng đời riêng.

### 7. Kiểm tra hoàn thành

Inventory không còn tài nguyên hourly lớn bị bỏ quên; còn lại được ghi tên/ARN/lý do/retention. Logs/evidence/digest/backup cần giữ có nơi lưu. Billing được kiểm tra lại; state/backend còn nguyên cho audit.

### 8. Lỗi thường gặp và cách xử lý

DependencyViolation: kiểm tra ENI/ALB/NAT còn dùng, không xóa state. RDS protected: change protection có review. BucketNotEmpty: xử lý cả version/delete marker. Helm provider unreachable: đã dọn cluster sai thứ tự, cần phục hồi quyền/kết nối và xử lý resource tồn dư theo inventory.

### 9. Kết quả sau bước này

Kết thúc buổi học với bằng chứng và chi phí được kiểm soát. Hoàn tất phần hướng dẫn V2; các gate thực hành chỉ đóng bằng kết quả thật.


---

## Nguồn đối chiếu cho phần 4.2–7.1

- [EKS networking và NetworkPolicy](https://docs.aws.amazon.com/eks/latest/userguide/cni-network-policy.html)
- [LBC installation và policy](https://kubernetes-sigs.github.io/aws-load-balancer-controller/latest/deploy/installation/)
- [ESO AWS access](https://external-secrets.io/latest/provider/aws-access/)
- [RDS managed master credentials](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/rds-secrets-manager.html)
- [MSK IAM actions và resource ARN](https://docs.aws.amazon.com/msk/latest/developerguide/kafka-actions.html)
- [MSK IAM Python signer](https://github.com/aws/aws-msk-iam-sasl-signer-python)
- [CloudWatch Observability EKS add-on](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/install-CloudWatch-Observability-EKS-addon.html)
- [Argo CD Projects](https://argo-cd.readthedocs.io/en/stable/user-guide/projects/)
- [GitLab OIDC AWS](https://docs.gitlab.com/ci/cloud_services/aws/)
- [AWS Pricing Calculator](https://calculator.aws/)

Các version/giá/quota phải kiểm tra lại trong buổi học. Các trường placeholder là đầu vào chưa biết; không thay bằng dữ liệu đoán.
