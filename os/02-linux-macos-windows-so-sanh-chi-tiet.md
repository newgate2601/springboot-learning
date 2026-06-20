# Linux, macOS và Windows — Giải thích và so sánh chi tiết

> Tài liệu này giải thích ba họ hệ điều hành máy tính phổ biến: **Linux**, **macOS** và
> **Windows**. Nội dung tập trung vào cách chúng hoạt động, điểm khác nhau, ưu nhược điểm
> và trường hợp sử dụng thực tế. Các ví dụ được trình bày trực tiếp, không dùng ẩn dụ.
>
> Thời điểm cập nhật: **21/06/2026**.

---

## Mục lục

- [1. Hệ điều hành là gì?](#1-hệ-điều-hành-là-gì)
- [2. Bức tranh tổng quát](#2-bức-tranh-tổng-quát)
- [3. Linux](#3-linux)
- [4. macOS](#4-macos)
- [5. Windows](#5-windows)
- [6. So sánh kiến trúc và cách sử dụng](#6-so-sánh-kiến-trúc-và-cách-sử-dụng)
- [7. So sánh filesystem và đường dẫn](#7-so-sánh-filesystem-và-đường-dẫn)
- [8. So sánh terminal và dòng lệnh](#8-so-sánh-terminal-và-dòng-lệnh)
- [9. Cài đặt và quản lý phần mềm](#9-cài-đặt-và-quản-lý-phần-mềm)
- [10. Tài khoản, quyền và bảo mật](#10-tài-khoản-quyền-và-bảo-mật)
- [11. Process, service và quản trị hệ thống](#11-process-service-và-quản-trị-hệ-thống)
- [12. Lập trình và phát triển phần mềm](#12-lập-trình-và-phát-triển-phần-mềm)
- [13. Container, Docker và máy ảo](#13-container-docker-và-máy-ảo)
- [14. Game, đồ họa và sáng tạo nội dung](#14-game-đồ-họa-và-sáng-tạo-nội-dung)
- [15. Server, cloud và doanh nghiệp](#15-server-cloud-và-doanh-nghiệp)
- [16. Ưu và nhược điểm tổng hợp](#16-ưu-và-nhược-điểm-tổng-hợp)
- [17. Chọn hệ điều hành theo use case](#17-chọn-hệ-điều-hành-theo-use-case)
- [18. Các hiểu lầm thường gặp](#18-các-hiểu-lầm-thường-gặp)
- [19. Ví dụ một dự án Spring Boot trên ba hệ điều hành](#19-ví-dụ-một-dự-án-spring-boot-trên-ba-hệ-điều-hành)
- [20. Kết luận ngắn gọn](#20-kết-luận-ngắn-gọn)
- [21. Nguồn tham khảo chính thức](#21-nguồn-tham-khảo-chính-thức)

---

## 1. Hệ điều hành là gì?

**Hệ điều hành — Operating System, viết tắt là OS** — là phần mềm hệ thống quản lý phần
cứng và cung cấp môi trường để ứng dụng hoạt động.

Một máy tính thường có các lớp sau:

```text
Người dùng
    ↓
Ứng dụng: trình duyệt, IDE, game, Microsoft Word, Docker...
    ↓
Hệ điều hành: Linux, macOS hoặc Windows
    ↓
Phần cứng: CPU, RAM, SSD, GPU, card mạng, bàn phím...
```

Hệ điều hành thực hiện các công việc chính:

1. Quản lý CPU và quyết định process nào được chạy.
2. Cấp phát và thu hồi RAM.
3. Quản lý file, thư mục và thiết bị lưu trữ.
4. Điều khiển thiết bị thông qua driver.
5. Quản lý tài khoản và quyền truy cập.
6. Cung cấp network stack để ứng dụng giao tiếp qua mạng.
7. Cung cấp API và system call cho chương trình.
8. Hiển thị giao diện đồ họa hoặc cung cấp môi trường dòng lệnh.

Ví dụ, khi một chương trình Java đọc file:

```text
Mã Java
→ Java I/O API
→ JVM
→ system call của hệ điều hành
→ filesystem
→ driver lưu trữ
→ SSD
```

Java không tự điều khiển SSD. JVM yêu cầu hệ điều hành mở và đọc file. Hệ điều hành kiểm
tra đường dẫn, quyền truy cập, vị trí dữ liệu và giao tiếp với thiết bị lưu trữ.

### 1.1 Kernel là gì?

**Kernel — nhân hệ điều hành** — là phần lõi có quyền cao nhất của hệ điều hành. Kernel
được nạp vào RAM khi máy khởi động và tiếp tục chạy cho tới khi máy tắt.

Ứng dụng thông thường không được phép tự ý điều khiển CPU, đọc mọi vùng RAM hay gửi lệnh
trực tiếp tới SSD. Nếu ứng dụng được làm như vậy, một lỗi trong Chrome cũng có thể ghi đè
RAM của PostgreSQL hoặc xóa dữ liệu của hệ điều hành. Thay vào đó, ứng dụng phải gửi yêu
cầu tới kernel thông qua **system call — lời gọi hệ thống**.

Ví dụ một chương trình gọi `read()` để đọc file:

```text
Ứng dụng đang chạy ở user mode
→ gọi system call read()
→ CPU chuyển sang kernel mode
→ kernel kiểm tra file descriptor và quyền
→ filesystem xác định dữ liệu nằm ở đâu
→ driver yêu cầu thiết bị lưu trữ đọc dữ liệu
→ kernel trả dữ liệu về vùng nhớ của ứng dụng
→ CPU quay lại user mode
```

Hai chế độ quan trọng:

- **User mode**: chế độ có quyền hạn chế, nơi phần lớn ứng dụng chạy. Một ứng dụng không
  được tùy ý truy cập bộ nhớ của ứng dụng khác hoặc chạy lệnh CPU đặc quyền.
- **Kernel mode**: chế độ đặc quyền, nơi kernel và phần driver quan trọng chạy. Code ở đây
  có thể truy cập toàn bộ RAM và thiết bị; lỗi nghiêm trọng có thể làm treo cả máy.

Kernel chịu trách nhiệm chính cho:

| Trách nhiệm | Kernel thực hiện cụ thể |
|---|---|
| Process và thread | Tạo, dừng, lập lịch và chuyển CPU giữa các thread |
| Bộ nhớ | Cấp phát trang nhớ, virtual memory, swap, bảo vệ vùng nhớ |
| File | Mở, đọc, ghi file thông qua filesystem |
| Thiết bị | Giao tiếp với driver của SSD, GPU, USB, card mạng |
| Mạng | Xử lý TCP/IP, socket, packet và firewall ở mức hệ thống |
| Bảo mật | Kiểm tra user, quyền, capability và chính sách truy cập |
| IPC | Cho các process giao tiếp bằng pipe, signal, shared memory, socket |

Kernel **không phải toàn bộ hệ điều hành**. Kernel không phải là trình duyệt, desktop,
shell, trình quản lý package hay trình soạn thảo. Một máy chỉ có kernel mà thiếu các
thành phần user space gần như chưa thể phục vụ người dùng bình thường.

```text
Hệ điều hành hoàn chỉnh
├── Kernel
├── Thư viện hệ thống
├── Chương trình quản lý service
├── Shell và công cụ dòng lệnh
├── Giao diện đồ họa
├── Trình quản lý package
└── Ứng dụng hệ thống
```

Ba họ hệ điều hành trong tài liệu dùng ba kernel khác nhau:

```text
Ubuntu/Fedora/Debian → Linux kernel
macOS                → XNU kernel
Windows              → Windows NT kernel
```

Vì kernel khác nhau, binary và driver viết trực tiếp cho một kernel thường không thể chạy
nguyên trạng trên kernel khác.

### 1.2 Unix là gì?

**Unix** ban đầu là một hệ điều hành được phát triển tại Bell Labs từ cuối thập niên 1960
và đầu thập niên 1970. Từ Unix về sau hình thành một họ hệ điều hành, tập hợp tư tưởng
thiết kế và các tiêu chuẩn về API, shell, lệnh và hành vi hệ thống.

Cần phân biệt ba cách dùng từ:

1. **UNIX** viết hoa thường liên quan tới nhãn hiệu và hệ thống được chứng nhận phù hợp
   với **Single UNIX Specification**.
2. **Unix** có thể chỉ họ hệ điều hành có nguồn gốc lịch sử từ Unix.
3. **Unix-like — giống Unix** chỉ hệ thống có cách sử dụng và nhiều API giống Unix nhưng
   không nhất thiết chứa code Unix gốc hoặc có chứng nhận UNIX.

Các ý tưởng Unix phổ biến:

- Hệ thống nhiều người dùng.
- Mỗi chương trình chạy trong một process.
- Quyền owner/group/others.
- Cây thư mục bắt đầu từ `/`.
- Thiết bị và nhiều tài nguyên được biểu diễn qua giao diện giống file.
- Ghép nhiều công cụ nhỏ bằng pipe.
- Tự động hóa bằng shell script.
- API process như `fork`, `exec`, signal và file descriptor.

Ví dụ pipe:

```bash
ps aux | grep java
```

Lệnh `ps` xuất danh sách process. Ký hiệu `|` chuyển output đó thành input cho `grep`.
`grep` chỉ giữ lại các dòng chứa chữ `java`.

Quan hệ giữa Unix, Linux và macOS:

```text
Unix lịch sử
├── sinh ra nhiều nhánh Unix/BSD và tiêu chuẩn POSIX/UNIX
├── ảnh hưởng mạnh tới macOS
└── ảnh hưởng cách thiết kế của Linux

Linux
├── không dùng XNU
├── không phải code kernel Unix gốc
└── là hệ điều hành Unix-like khi kết hợp kernel với user space

macOS
├── dùng XNU
├── có thành phần BSD/Unix
└── các phiên bản phù hợp được Apple chứng nhận UNIX

Windows
├── dùng Windows NT kernel
├── không phải Unix
└── có thể cung cấp môi trường Linux qua WSL
```

**POSIX** là bộ tiêu chuẩn mô tả nhiều API và hành vi để chương trình có thể portable giữa
các hệ thống kiểu Unix. POSIX không phải một hệ điều hành. Nó giống một bản đặc tả mà hệ
điều hành và công cụ cố gắng tuân theo.

Ví dụ, Linux và macOS đều có `fork()`, `exec()`, file descriptor và permission kiểu Unix.
Điều đó giúp nhiều chương trình C và shell script có thể port giữa hai hệ thống. Tuy nhiên
chúng vẫn khác kernel, framework đồ họa, driver, filesystem mặc định và nhiều option của
công cụ dòng lệnh.

### 1.3 “Môi trường Unix thuận tiện cho lập trình” nghĩa chính xác là gì?

Câu này không có nghĩa chỉ Unix mới lập trình được. Windows cũng là môi trường phát triển
tốt. Ý nghĩa cụ thể là nhiều server production và công cụ backend dùng quy ước kiểu Unix,
nên Linux/macOS cho phép sử dụng trực tiếp những khái niệm quen thuộc đó:

- Đường dẫn như `/var/log/app.log`.
- Quyền `chmod`, owner và group.
- Shell Bash/Zsh.
- Pipe và redirect.
- SSH để đăng nhập server.
- Process, signal và port.
- Shell script dùng trong CI/CD.

Ví dụ một backend developer có thể chạy:

```bash
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
curl http://localhost:8080/actuator/health
ps aux | grep java
kill -TERM 1234
```

Trên Linux, các lệnh này chạy trực tiếp trong môi trường thường cũng được dùng ở server.
Trên macOS, cách thao tác gần giống vì macOS có user space kiểu Unix. Trên Windows,
developer có thể viết lệnh PowerShell tương đương hoặc chạy chúng trong WSL.

---

## 2. Bức tranh tổng quát

| Tiêu chí | Linux | macOS | Windows |
|---|---|---|---|
| Đơn vị phát triển | Cộng đồng và nhiều tổ chức | Apple | Microsoft |
| Mô hình mã nguồn | Kernel và phần lớn hệ sinh thái là mã nguồn mở | Kết hợp mã nguồn mở và mã nguồn đóng | Chủ yếu mã nguồn đóng |
| Kernel | Linux kernel | XNU | Windows NT kernel |
| Phần cứng chính thức | Rất nhiều loại máy và kiến trúc CPU | Máy Mac của Apple | Máy từ rất nhiều hãng |
| Giao diện | Tùy distribution và desktop environment | Giao diện do Apple kiểm soát | Giao diện do Microsoft kiểm soát |
| Mục tiêu nổi bật | Server, cloud, container, lập trình, hệ thống nhúng | Máy cá nhân Apple, sáng tạo nội dung, phát triển cho Apple | Máy cá nhân, doanh nghiệp, game, phần mềm thương mại |
| Dòng lệnh phổ biến | Bash, Zsh, Fish | Zsh, Bash | PowerShell, Command Prompt; có thể dùng WSL |
| Filesystem thường gặp | ext4, XFS, Btrfs | APFS | NTFS |
| Cách cài phần mềm | Package manager của distribution | App Store, file `.dmg`, Homebrew | Microsoft Store, file `.exe`/`.msi`, WinGet |
| Khả năng tùy biến hệ thống | Rất cao | Thấp đến trung bình | Trung bình |

Ba tên này không hoàn toàn cùng một cấp khái niệm:

- **Linux** chính xác là tên kernel. Một hệ điều hành hoàn chỉnh dùng Linux kernel thường
  được phân phối dưới dạng Ubuntu, Debian, Fedora, Arch Linux, Red Hat Enterprise Linux...
- **macOS** là một hệ điều hành hoàn chỉnh do Apple phát triển cho máy Mac.
- **Windows** là một họ hệ điều hành hoàn chỉnh do Microsoft phát triển.

---

## 3. Linux

### 3.1 Linux là gì?

Linux bắt đầu từ một **kernel**. Kernel là thành phần chạy với đặc quyền cao nhất, quản lý
CPU, RAM, process, driver, filesystem và các thiết bị.

Để có một hệ điều hành sử dụng được, Linux kernel được kết hợp với:

- Các thư viện hệ thống.
- Shell như Bash hoặc Zsh.
- Công cụ dòng lệnh.
- Trình quản lý package.
- Hệ thống khởi động và quản lý service như `systemd`.
- Giao diện đồ họa nếu người dùng cần.
- Các ứng dụng đi kèm.

Tập hợp này được gọi là một **Linux distribution**, thường viết ngắn là **distro**.

Ví dụ:

| Distribution | Mục tiêu thường gặp |
|---|---|
| Ubuntu | Người mới, desktop, server, cloud |
| Debian | Ổn định, server, nền tảng cho distro khác |
| Fedora | Công nghệ Linux mới, lập trình và desktop |
| Red Hat Enterprise Linux | Doanh nghiệp, hỗ trợ thương mại dài hạn |
| Rocky Linux, AlmaLinux | Server tương thích hệ sinh thái RHEL |
| Arch Linux | Tùy biến cao, người dùng muốn tự cấu hình |
| Kali Linux | Kiểm thử bảo mật; không phải lựa chọn mặc định cho người mới |

### 3.2 Linux không đồng nghĩa với Ubuntu

Ubuntu dùng Linux kernel, nhưng Ubuntu chỉ là một trong nhiều distribution.

```text
Linux kernel
├── Ubuntu
├── Debian
├── Fedora
├── Arch Linux
└── Nhiều distribution khác
```

Hai distribution có thể dùng cùng Linux kernel nhưng khác nhau về:

- Phiên bản thư viện.
- Package manager.
- Chu kỳ cập nhật.
- Giao diện đồ họa.
- Cấu hình mặc định.
- Chính sách hỗ trợ.

Đúng là phần lớn code kernel có thể giống nhau, nhưng cần hiểu chính xác chữ “cùng”.
Ubuntu và Fedora không nhất thiết dùng đúng cùng một file kernel binary hoặc cùng phiên
bản kernel tại một thời điểm. Chúng đều lấy mã nguồn từ dự án Linux kernel, sau đó mỗi
distribution có thể:

- Chọn phiên bản kernel khác nhau.
- Bật hoặc tắt các tùy chọn lúc biên dịch.
- Thêm bản vá.
- Đóng gói bộ kernel module khác nhau.
- Chọn lịch cập nhật và thời gian hỗ trợ khác nhau.

Ví dụ giả định:

```text
Ubuntu
├── Linux kernel 6.x do Ubuntu đóng gói
├── systemd
├── GNU coreutils
├── glibc
├── apt + package .deb
├── GNOME đã được Ubuntu cấu hình
└── repository của Ubuntu

Fedora
├── Linux kernel 6.y do Fedora đóng gói
├── systemd
├── GNU coreutils
├── glibc
├── dnf + package .rpm
├── GNOME theo cấu hình Fedora
└── repository của Fedora
```

Hai hệ thống đều gọi system call của Linux kernel, nhưng trải nghiệm sử dụng và phần mềm
được cài có thể khác.

#### User space là gì?

**User space** là toàn bộ code chạy ngoài kernel mode: shell, thư viện, service, desktop,
package manager và ứng dụng. Đây là phần tạo ra phần lớn khác biệt mà người dùng nhìn
thấy giữa các distribution.

Ví dụ khi chạy:

```bash
ls -l
```

Luồng đơn giản là:

```text
Shell Bash đọc câu lệnh
→ chạy chương trình ls thuộc GNU coreutils
→ ls gọi system call để đọc thư mục
→ Linux kernel trả metadata file
→ ls định dạng và in kết quả ra terminal
```

Trong luồng này:

- Bash và `ls` thuộc user space.
- System call và truy cập filesystem do kernel xử lý.
- Terminal hiển thị kết quả cũng thuộc user space.

#### Từng điểm khác nhau giữa các distribution

**Phiên bản thư viện** là phiên bản của code dùng chung mà ứng dụng liên kết tới. Ví dụ
`glibc`, OpenSSL hoặc thư viện đồ họa. Ứng dụng biên dịch với thư viện quá mới có thể
không chạy trên distro cũ vì thiếu symbol hoặc ABI cần thiết.

**Package manager** là công cụ tìm, cài, nâng cấp và gỡ phần mềm cùng dependency:

```bash
# Ubuntu: package .deb, repository Ubuntu
sudo apt install nginx

# Fedora: package .rpm, repository Fedora
sudo dnf install nginx
```

Hai lệnh đều cài Nginx nhưng package, đường dẫn cấu hình bổ sung, phiên bản và chính sách
cập nhật có thể khác.

**Chu kỳ cập nhật** là tốc độ distribution phát hành phiên bản mới:

- Distro ưu tiên ổn định giữ phiên bản lâu và backport bản vá bảo mật.
- Distro cập nhật nhanh cung cấp kernel/thư viện mới sớm hơn nhưng thay đổi thường xuyên.
- Bản LTS ưu tiên thời gian hỗ trợ dài cho máy cần ít nâng cấp lớn.

**Giao diện đồ họa** không nằm trong Linux kernel. Ubuntu có thể dùng GNOME, Kubuntu dùng
KDE Plasma và Xubuntu dùng Xfce, dù tất cả đều dùng Linux kernel.

**Cấu hình mặc định** gồm firewall, service được bật, layout desktop, filesystem, chính
sách `sudo`, repository và tham số kernel. Hai distro cài cùng phần mềm vẫn có thể khởi
động hoặc cấu hình nó khác nhau.

**Chính sách hỗ trợ** quy định thời gian có bản vá, ai chịu trách nhiệm hỗ trợ, phần mềm
nào được cam kết tương thích và doanh nghiệp có nhận SLA hay không.

#### Cùng kernel thì ứng dụng có chạy giống hệt không?

Không. Ứng dụng còn phụ thuộc user space.

Ví dụ một binary native cần `glibc` phiên bản mới:

```text
Kernel tương thích
nhưng glibc trên máy quá cũ
→ chương trình vẫn không khởi động
```

Ngược lại, container Ubuntu có thể chạy trên Fedora host:

```text
Ubuntu user space trong container
→ gọi Linux system call
→ Fedora host cung cấp Linux kernel
```

Đây là lý do container có thể mang user space của distro A chạy trên kernel Linux của
distro B, miễn là system call và kiến trúc CPU tương thích.

Ví dụ cài Git:

```bash
# Ubuntu/Debian
sudo apt install git

# Fedora
sudo dnf install git

# Arch Linux
sudo pacman -S git
```

### 3.3 Giao diện đồ họa trên Linux

Linux không chỉ có màn hình dòng lệnh. Desktop Linux có thể dùng các **desktop
environment** khác nhau:

- GNOME.
- KDE Plasma.
- Xfce.
- Cinnamon.

Desktop environment quyết định thanh taskbar, menu ứng dụng, cửa sổ, trình quản lý file,
phím tắt và phần lớn trải nghiệm đồ họa.

Ubuntu thường dùng GNOME. Một distribution khác có thể dùng KDE Plasma nhưng vẫn chạy
cùng loại Linux kernel.

### 3.4 Điểm mạnh của Linux

#### Mã nguồn mở và khả năng kiểm soát cao

Người dùng có thể kiểm tra mã nguồn, tự biên dịch kernel, thay desktop environment hoặc
chỉ cài những thành phần cần thiết.

Một server Linux có thể không cài giao diện đồ họa. Điều này làm giảm dung lượng, RAM sử
dụng và số thành phần cần cập nhật.

#### Rất phù hợp với server

Linux được sử dụng rộng rãi cho:

- Web server.
- Database server.
- Kubernetes node.
- Máy ảo trên cloud.
- CI/CD runner.
- Router và thiết bị mạng.
- Hệ thống nhúng.

Ví dụ triển khai một Spring Boot service:

```bash
java -jar catalog-service.jar
```

Hoặc chạy nó dưới dạng service do `systemd` quản lý:

```bash
sudo systemctl start catalog-service
sudo systemctl status catalog-service
sudo journalctl -u catalog-service
```

#### Hệ sinh thái lập trình và DevOps tốt

Phần lớn công cụ server, container và cloud hỗ trợ Linux đầu tiên hoặc hoạt động tự nhiên
trên Linux:

- Docker Engine.
- Kubernetes.
- Nginx.
- Apache HTTP Server.
- PostgreSQL.
- Redis.
- Prometheus.
- Terraform.
- Ansible.

#### Tùy biến cao

Người dùng có thể thay:

- Kernel.
- Shell.
- Desktop environment.
- Window manager.
- Filesystem.
- Hệ thống init.
- Bộ công cụ dòng lệnh.

Mức tùy biến này hữu ích cho server, thiết bị nhúng và môi trường nghiên cứu hệ điều hành.

#### Chi phí bản quyền

Nhiều distribution có thể tải và sử dụng miễn phí. Một số bản doanh nghiệp thu phí cho
hỗ trợ kỹ thuật, chứng nhận và cập nhật dài hạn chứ không đơn thuần bán quyền được chạy
Linux.

### 3.5 Hạn chế của Linux

#### Phần mềm desktop thương mại chưa đầy đủ

Một số ứng dụng không có bản Linux chính thức, ví dụ một số sản phẩm Adobe hoặc phần mềm
chuyên ngành chỉ hỗ trợ Windows.

Giải pháp thay thế tồn tại, nhưng không phải lúc nào cũng tương thích hoàn toàn về:

- Định dạng file.
- Plugin.
- Font.
- Quy trình cộng tác trong doanh nghiệp.

#### Driver và phần cứng có thể cần kiểm tra trước

Phần cứng phổ biến thường được hỗ trợ tốt, nhưng một số thiết bị mới hoặc thiết bị có
driver độc quyền có thể cần cài đặt bổ sung. Laptop có cảm biến đặc biệt, đầu đọc vân tay
hoặc phần mềm điều khiển riêng của nhà sản xuất có thể không hoạt động đầy đủ.

#### Trải nghiệm khác nhau giữa các distribution

Hướng dẫn cho Ubuntu không chắc áp dụng nguyên vẹn cho Fedora. Tên package, cấu hình và
phiên bản phần mềm có thể khác nhau.

#### Người mới phải học thêm khái niệm

Để quản trị Linux hiệu quả, người dùng thường cần hiểu:

- Terminal.
- Quyền file.
- User và group.
- Package manager.
- Process và service.
- Log.
- Cấu trúc thư mục.

### 3.6 Khi nào nên dùng Linux?

Linux là lựa chọn tốt khi:

- Xây dựng server hoặc dịch vụ backend.
- Làm DevOps, cloud, Docker hoặc Kubernetes.
- Học hệ điều hành, mạng và bảo mật.
- Muốn kiểm soát sâu hệ thống.
- Cần hệ điều hành nhẹ cho máy cũ hoặc thiết bị nhỏ.
- Phát triển phần mềm chạy production trên Linux.
- Xây dựng hệ thống nhúng.

Linux có thể không phải lựa chọn thuận tiện nhất khi phần mềm bắt buộc chỉ có trên Windows
hoặc macOS.

---

## 4. macOS

### 4.1 macOS là gì?

macOS là hệ điều hành do Apple phát triển cho máy Mac. Apple kiểm soát cả:

- Thiết kế phần cứng.
- Chip Apple silicon trên các máy Mac hiện đại.
- Hệ điều hành.
- Driver.
- Nhiều ứng dụng hệ thống.

macOS dùng kernel **XNU**. Hệ thống có nguồn gốc Unix và được Apple chứng nhận theo tiêu
chuẩn UNIX cho các phiên bản phù hợp. Vì vậy macOS có nhiều công cụ và cách tổ chức quen
thuộc với người dùng Unix/Linux, nhưng macOS không phải Linux.

```text
macOS
├── XNU kernel
├── Thành phần BSD/Unix
├── Framework và API của Apple
├── Giao diện macOS
└── Ứng dụng hệ thống của Apple
```

Tên **XNU** thường được giải thích là “X is Not Unix”. XNU là hybrid kernel kết hợp:

- **Mach**: nền tảng cho task, thread, virtual memory và message passing.
- **BSD**: nhiều phần kiểu Unix như process, user/group, permission, network stack và
  API POSIX.
- **I/O Kit**: framework driver của Apple.

Luồng hoạt động đơn giản:

```text
Ứng dụng macOS
→ Swift/Objective-C và framework Apple
→ thư viện hệ thống
→ system call
→ XNU
→ driver/phần cứng Mac
```

Do đó “macOS có nền tảng Unix” không có nghĩa macOS dùng Linux kernel. macOS dùng XNU;
Linux distribution dùng Linux kernel.

### 4.2 macOS chỉ chạy chính thức trên phần cứng Apple

Apple thiết kế macOS cho:

- MacBook Air.
- MacBook Pro.
- iMac.
- Mac mini.
- Mac Studio.
- Mac Pro.

Điều này khác Windows và Linux, vốn chạy trên phần cứng từ nhiều nhà sản xuất.

Việc cài macOS không chính thức lên máy tính không phải của Apple thường được gọi là
Hackintosh. Cách này có vấn đề về giấy phép, driver, cập nhật và độ ổn định; không phù hợp
cho môi trường làm việc cần tin cậy.

### 4.3 Apple silicon và kiến trúc CPU

Máy Mac hiện đại chủ yếu dùng **Apple silicon**, thuộc kiến trúc ARM64. Máy Mac cũ từng
dùng CPU Intel thuộc kiến trúc x86-64.

Điều này ảnh hưởng đến binary:

```text
Ứng dụng biên dịch cho x86-64 ≠ ứng dụng biên dịch cho ARM64
```

Apple cung cấp Rosetta 2 để hỗ trợ chạy nhiều ứng dụng Intel trên Apple silicon, nhưng
nhà phát triển vẫn nên dùng bản ARM64 hoặc universal binary khi có thể.

Ví dụ Docker image:

```bash
# Image có thể có nhiều kiến trúc
docker pull postgres

# Ép dùng image x86-64 trên máy ARM64 nếu thật sự cần
docker run --platform linux/amd64 postgres
```

Ép chạy khác kiến trúc có thể dùng mô phỏng và làm giảm hiệu năng. Cần kiểm tra image hỗ
trợ `linux/arm64` khi làm việc trên Apple silicon.

**Apple silicon** là tên chung cho các dòng chip Apple thiết kế cho Mac. Một system on a
chip có thể tích hợp CPU, GPU, bộ điều khiển bộ nhớ và media engine.

**Unified memory** nghĩa là CPU và GPU có thể dùng chung vùng bộ nhớ vật lý thay vì luôn
cần hai vùng RAM tách biệt. Cách này có thể giảm việc sao chép dữ liệu, nhưng dung lượng
RAM vẫn hữu hạn và trên phần lớn Mac hiện đại không thể nâng cấp sau khi mua.

**Universal binary** là ứng dụng chứa cả code ARM64 và x86-64:

```text
Một ứng dụng
├── binary ARM64 cho Apple silicon
└── binary x86-64 cho Mac Intel
```

Nếu ứng dụng chỉ có binary Intel, Rosetta 2 có thể dịch code x86-64 để chạy trên Apple
silicon. Rosetta không bảo đảm mọi driver, kernel extension hoặc công cụ cũ đều chạy được.

### 4.4 Điểm mạnh của macOS

#### Phần cứng và phần mềm được tích hợp chặt

Apple kiểm soát số lượng model máy ít hơn hệ sinh thái PC. Driver, quản lý pin, màn hình,
trackpad, sleep/wake và cập nhật được kiểm thử trên tập phần cứng xác định.

Kết quả thường thấy:

- Thời lượng pin tốt trên laptop.
- Trackpad và gesture ổn định.
- Sleep/wake nhất quán.
- Ít phải tự tìm driver.

#### Môi trường Unix thuận tiện cho lập trình

macOS có terminal và nhiều công cụ Unix. Nhà phát triển có thể dùng:

```bash
ssh
curl
grep
find
chmod
ps
kill
```

Homebrew thường được dùng để cài công cụ:

```bash
brew install git
brew install openjdk
brew install maven
```

Giải thích từng lệnh Unix trong ví dụ:

| Lệnh | Công việc cụ thể |
|---|---|
| `ssh user@server` | Mở terminal được mã hóa tới server |
| `curl URL` | Gửi HTTP request để thử API hoặc tải dữ liệu |
| `grep text file` | Tìm các dòng chứa chuỗi |
| `find path ...` | Tìm file/thư mục |
| `chmod` | Thay quyền đọc, ghi, thực thi |
| `ps` | Xem process |
| `kill` | Gửi signal tới process |

Điểm tiện cho backend developer là cú pháp và khái niệm này gần với Linux server. Tuy
nhiên macOS không phải Linux: macOS không dùng `systemd`, package `.deb`, Linux `/proc`
hay đúng toàn bộ option của GNU command.

#### Bắt buộc hoặc thuận lợi nhất cho phát triển hệ sinh thái Apple

Để phát triển và ký ứng dụng iOS, iPadOS hoặc macOS bằng công cụ chính thức, nhà phát
triển cần Xcode trên macOS.

Use case:

```text
Viết ứng dụng iPhone
→ cần Xcode
→ Xcode chạy trên macOS
→ cần máy Mac hoặc dịch vụ Mac từ xa phù hợp
```

#### Không có máy Mac thì có lập trình iOS được không?

Câu trả lời cần chia thành hai mức:

- Bạn có thể **học Swift**, viết thuật toán và viết một phần code đa nền tảng trên
  Windows/Linux.
- Nhưng để **build ứng dụng iOS chính thức, chạy iOS Simulator, ký app, tạo archive và
  phát hành**, pipeline cuối cùng cần Xcode trên macOS.

Xcode chứa:

- **iOS SDK**: framework và API cần để biên dịch app cho iOS.
- **Swift/Clang toolchain**: compiler và linker.
- **iOS Simulator**: môi trường mô phỏng iPhone/iPad.
- **Code signing tools**: công cụ ký binary.
- **Debugger và Instruments**: tìm lỗi, đo CPU, RAM, network và hiệu năng.
- Công cụ archive, validate và upload build.

Luồng phát triển native đầy đủ:

```text
Viết Swift/SwiftUI
→ Xcode biên dịch bằng iOS SDK
→ chạy trên Simulator hoặc iPhone thật
→ debug/test
→ ký bằng certificate + provisioning profile
→ tạo archive
→ đưa lên TestFlight/App Store Connect
```

Nếu máy cá nhân là Windows, có bốn phương án:

1. **Thuê hoặc kết nối Mac từ xa**: code có thể được sửa từ Windows, nhưng build và ký
   diễn ra trên Mac.
2. **Dùng Flutter, React Native hoặc .NET MAUI**: viết phần lớn code trên Windows, nhưng
   bước tạo bản iOS vẫn cần Mac build host.
3. **Dùng CI có macOS runner**: phù hợp cho build tự động; debug giao diện hằng ngày vẫn
   thuận tiện hơn nếu có Mac.
4. **Học Swift trên Windows/Linux**: đủ để học ngôn ngữ, không cung cấp đầy đủ iOS SDK,
   Simulator và quy trình ký app.

Ví dụ Flutter:

```text
Windows
→ viết Dart, chạy bản Android/web
→ push code
→ Mac build agent chạy Xcode
→ tạo bản iOS đã ký
```

**iPhone thật không thay thế cho Mac.** iPhone là thiết bị chạy app để test; Mac + Xcode
là môi trường build, cài, debug và ký app.

#### Các keyword iOS quan trọng

| Keyword | Ý nghĩa |
|---|---|
| Swift | Ngôn ngữ lập trình |
| Objective-C | Ngôn ngữ cũ hơn vẫn có trong nhiều codebase Apple |
| SwiftUI | Framework khai báo giao diện hiện đại |
| UIKit | Framework giao diện iOS lâu đời, rất phổ biến |
| Xcode | IDE và bộ công cụ build/test/debug của Apple |
| iOS SDK | API và thư viện để build cho iOS |
| Simulator | Môi trường thiết bị mô phỏng chạy trên Mac |
| Certificate | Chứng thư xác nhận danh tính bên ký ứng dụng |
| Provisioning profile | Liên kết app ID, certificate, quyền và thiết bị/phân phối |
| TestFlight | Phân phối bản thử nghiệm |
| App Store Connect | Quản lý build, tester, metadata và phát hành |

Swift không đồng nghĩa với Xcode. Có compiler Swift chưa có nghĩa là có iOS SDK,
Simulator và công cụ ký của Apple.

#### Tốt cho một số công việc sáng tạo

macOS có hệ sinh thái mạnh cho:

- Final Cut Pro.
- Logic Pro.
- Các ứng dụng thiết kế và dựng phim chuyên nghiệp.
- Quản lý màu và màn hình trên phần cứng Apple.

#### Tích hợp với thiết bị Apple

Người dùng iPhone, iPad, Apple Watch và Mac có thể sử dụng các tính năng đồng bộ như
AirDrop, Handoff, iCloud và clipboard giữa thiết bị.

### 4.5 Hạn chế của macOS

#### Chi phí phần cứng

macOS đi kèm máy Mac. Người dùng không thể hợp pháp mua một máy PC bất kỳ rồi cài macOS
như cài một Linux distribution.

#### Khả năng nâng cấp và sửa chữa bị giới hạn

Trên nhiều máy Mac hiện đại, RAM và bộ nhớ được tích hợp chặt vào bo mạch. Người mua cần
chọn dung lượng phù hợp ngay từ đầu.

Ví dụ: nếu mua máy có 16 GB RAM, thông thường không thể mua thanh RAM rồi tự nâng lên
32 GB sau đó.

#### Game ít hơn Windows

Số lượng game hỗ trợ macOS và mức độ tối ưu driver/game thường thấp hơn Windows. Một số
game chỉ phát hành cho Windows hoặc không hỗ trợ anti-cheat trên macOS.

#### Tùy biến hệ thống thấp hơn Linux

Người dùng không thể thay kernel, cài macOS trên nhiều loại phần cứng hoặc thay đổi sâu
toàn bộ giao diện theo cách Linux cho phép.

#### Khác biệt ARM64 có thể gây vấn đề tương thích

Một công cụ cũ, plugin native hoặc Docker image chỉ có x86-64 có thể cần Rosetta hoặc mô
phỏng. Nhà phát triển cần phân biệt:

- Kiến trúc của máy host.
- Kiến trúc của JDK.
- Kiến trúc của native library.
- Kiến trúc của container image.

### 4.6 Khi nào nên dùng macOS?

macOS là lựa chọn tốt khi:

- Phát triển ứng dụng iOS, iPadOS hoặc macOS.
- Muốn laptop có sự tích hợp phần cứng/phần mềm cao.
- Đã dùng nhiều thiết bị Apple.
- Làm video, âm thanh hoặc thiết kế với phần mềm tối ưu cho Mac.
- Lập trình web/backend và muốn môi trường Unix trên máy cá nhân.

Cụm “muốn môi trường Unix trên máy cá nhân” nghĩa là muốn dùng terminal, SSH, permission,
shell script, process và đường dẫn kiểu Unix ngay trên desktop mà không phải mở WSL hay
Linux VM. Đây là tiện ích về workflow, không phải điều kiện bắt buộc để làm backend.

| Nhu cầu | macOS | Windows + WSL | Linux desktop |
|---|---|---|---|
| Công cụ Unix | Có trực tiếp, nhưng là BSD/macOS | Có Linux trong WSL 2 | Có trực tiếp |
| Ứng dụng Windows | Hạn chế | Chạy tự nhiên | Hạn chế hoặc cần VM/lớp tương thích |
| Build iOS | Có Xcode | Cần Mac từ xa/build agent | Cần Mac từ xa/build agent |
| Gần Linux production | Gần về cách dùng, khác kernel | WSL 2 dùng Linux kernel | Gần nhất |
| Game PC | Hạn chế hơn | Phù hợp nhất | Tùy từng game |

macOS có thể không tối ưu nếu ưu tiên chính là game Windows, cần tự nâng cấp phần cứng
thường xuyên hoặc muốn tùy biến hệ điều hành ở mức rất sâu.

---

## 5. Windows

### 5.1 Windows là gì?

Windows là họ hệ điều hành do Microsoft phát triển. Trên máy tính cá nhân hiện đại,
Windows 11 là dòng Windows chính đang được Microsoft cung cấp và hỗ trợ.

Windows dùng kiến trúc dựa trên **Windows NT kernel**. Nó không dựa trên Linux kernel và
không phải Unix.

Windows được cài trên máy từ nhiều nhà sản xuất:

- Dell.
- HP.
- Lenovo.
- ASUS.
- Acer.
- Microsoft Surface.
- Máy tính người dùng tự lắp.

### 5.2 Windows có nhiều edition

Các edition phục vụ nhu cầu khác nhau, ví dụ:

- Windows Home: người dùng cá nhân.
- Windows Pro: thêm tính năng quản trị và doanh nghiệp.
- Windows Enterprise: triển khai và quản trị quy mô tổ chức.
- Windows Server: vai trò server và hạ tầng doanh nghiệp.

Không nên hiểu Windows Home và Windows Server là một sản phẩm giống nhau chỉ khác giao
diện. Windows Server có vai trò, chính sách hỗ trợ và cách cấp phép riêng.

### 5.3 Điểm mạnh của Windows

#### Tương thích phần mềm desktop rộng

Nhiều phần mềm thương mại và phần mềm chuyên ngành ưu tiên Windows:

- Microsoft Office desktop.
- Phần mềm kế toán.
- Phần mềm CAD/CAM.
- Công cụ doanh nghiệp nội bộ.
- Ứng dụng quản lý thiết bị của nhà sản xuất.
- Nhiều phần mềm chỉ cung cấp file cài đặt `.exe` hoặc `.msi`.

#### Hệ sinh thái game mạnh

Windows thường là lựa chọn phù hợp nhất cho PC gaming do:

- Nhiều game phát hành cho Windows.
- DirectX.
- Driver GPU được tối ưu rộng rãi.
- Hỗ trợ thiết bị ngoại vi.
- Nhiều hệ thống anti-cheat nhắm đến Windows.

#### Phần cứng đa dạng

Người dùng có thể chọn:

- Laptop giá thấp đến workstation cao cấp.
- Máy tự lắp.
- Nhiều loại GPU.
- Nhiều lựa chọn nâng cấp RAM và SSD.
- Thiết bị ngoại vi từ nhiều hãng.

#### Phù hợp với hạ tầng doanh nghiệp Microsoft

Windows hoạt động tốt với:

- Active Directory.
- Group Policy.
- Microsoft 365.
- Microsoft Intune.
- Windows Server.
- PowerShell.
- Hệ sinh thái .NET và Visual Studio.

#### WSL cải thiện trải nghiệm lập trình

**Windows Subsystem for Linux — WSL** cho phép chạy môi trường Linux trong Windows.

Ví dụ:

```powershell
wsl --install
```

Sau khi cài Ubuntu trong WSL, người dùng có thể chạy:

```bash
sudo apt update
sudo apt install git
```

WSL hữu ích khi cần:

- Công cụ Linux.
- Bash.
- Package manager của Linux.
- Môi trường gần với server production.
- Docker Desktop sử dụng backend WSL 2.

WSL không biến Windows thành Linux. Windows vẫn là host OS; một môi trường Linux chạy
thông qua cơ chế ảo hóa và tích hợp của WSL.

#### WSL thực chất là gì?

WSL là tính năng của Windows cho phép cài và chạy một hoặc nhiều Linux distribution với
tích hợp terminal, filesystem, network và process.

Các lớp trong WSL 2:

```text
Máy tính vật lý
→ Windows là host OS
→ lớp ảo hóa nhẹ của WSL 2
→ Linux kernel do Microsoft cung cấp
→ Ubuntu user space
→ Bash, apt, Git, JDK, Maven, PostgreSQL...
```

- **Host OS** là hệ điều hành chính điều khiển máy vật lý: Windows.
- **Linux kernel** xử lý system call từ chương trình Linux.
- **Ubuntu** cung cấp user space: Bash, `apt`, thư viện, cấu trúc thư mục và package.
- Windows Terminal chỉ là ứng dụng terminal hiển thị shell; nó không phải WSL.

#### “Cài Ubuntu trong WSL” nghĩa là gì?

Nó không cài Ubuntu đè lên Windows và không tạo dual boot. Quá trình này:

1. Bật WSL và nền tảng ảo hóa cần thiết.
2. Tải root filesystem của Ubuntu.
3. Tạo distribution Ubuntu với filesystem và tài khoản Linux riêng.
4. Khi mở Ubuntu, WSL khởi động môi trường Linux để Bash và ứng dụng chạy.

Kiểm tra distro:

```powershell
wsl --list --verbose
```

Ví dụ:

```text
NAME      STATE    VERSION
Ubuntu   Running  2
```

Mở Ubuntu:

```powershell
wsl -d Ubuntu
```

Kiểm tra từ bên trong:

```bash
cat /etc/os-release
uname -a
pwd
```

- `/etc/os-release` cho biết user space là Ubuntu.
- `uname -a` cho biết Linux kernel đang chạy.
- `pwd` thường bắt đầu ở `/home/<user>`.

Bạn có thể cài nhiều distro:

```text
Windows
├── Ubuntu WSL
├── Debian WSL
└── Kali WSL
```

Mỗi distro có package và root filesystem riêng.

#### WSL 1 và WSL 2

| Tiêu chí | WSL 1 | WSL 2 |
|---|---|---|
| Linux kernel thật | Không; dịch Linux system call sang Windows | Có Linux kernel trong utility VM nhẹ |
| Tương thích system call | Thấp hơn | Cao hơn |
| Docker/Linux container | Hạn chế | Phù hợp hơn |
| I/O trong filesystem Linux | Không có kiến trúc filesystem giống WSL 2 | Tốt nếu project nằm trong filesystem WSL |
| Truy cập file Windows | Trực tiếp và thường nhanh | Có qua `/mnt/c`, nhưng workload nhiều file có thể chậm |

Với backend hiện đại, WSL 2 thường là lựa chọn mặc định.

#### Windows và WSL nhìn thấy file của nhau

Từ Ubuntu WSL, ổ `C:` thường ở:

```text
/mnt/c
```

```text
C:\code\catalog
↕
/mnt/c/code/catalog
```

Từ Windows Explorer, filesystem Ubuntu có thể truy cập qua:

```text
\\wsl$\Ubuntu\home\tony
```

Quy tắc thực dụng:

- Công cụ chạy trong Linux — Maven Linux, npm Linux, Git Linux — thì đặt project ở
  `/home/tony/...`.
- Công cụ chạy bằng Windows — Visual Studio, MSBuild — thì đặt project trên `C:\...`.
- Không để hai bộ Git Windows/Linux sửa lẫn lộn nếu chưa hiểu line ending và permission.

Ranh giới NTFS ↔ filesystem Linux có thêm lớp chuyển đổi. Maven, Gradle và npm thao tác
rất nhiều file nhỏ nên vị trí project có thể ảnh hưởng hiệu năng rõ rệt.

#### Windows và Linux gọi chương trình của nhau

Từ PowerShell:

```powershell
wsl uname -a
wsl bash -lc "java -version"
```

Từ WSL:

```bash
notepad.exe README.md
explorer.exe .
```

VS Code hoặc IntelliJ có thể hiển thị giao diện trên Windows nhưng dùng toolchain,
terminal và project nằm trong WSL.

#### Vì sao WSL tốt cho backend?

Backend production rất thường chạy trên Linux. WSL cho developer Windows một môi trường
gần production hơn:

| Nhu cầu backend | WSL cung cấp |
|---|---|
| Bash và shell script | Chạy trực tiếp trong Linux |
| JDK/Maven/Gradle Linux | Dùng đúng toolchain Linux |
| Docker container Linux | Chạy qua backend WSL 2 |
| PostgreSQL/Redis/Nginx | Cài trong distro hoặc container |
| Permission Unix | `chmod`, owner, group |
| SSH | Quản trị Linux server |
| CI parity | Script gần giống Linux CI |
| Path Linux | `/home`, `/var`, `/tmp` |

Ví dụ chạy Spring Boot:

```bash
sudo apt update
sudo apt install openjdk-21-jdk git
git clone https://example.com/catalog-service.git
cd catalog-service
./mvnw test
./mvnw spring-boot:run
```

Developer vẫn dùng Chrome, Office và ứng dụng Windows, nhưng build backend trong Linux.

#### WSL khác máy Linux riêng thế nào?

- Windows vẫn là OS chính và quản lý vòng đời tổng thể.
- WSL có lớp tích hợp/ảo hóa; không boot như một PC Linux độc lập.
- Network, GPU, USB và service có một số khác biệt với server Linux thật.
- File dưới `/mnt/c` có đặc tính I/O và permission khác filesystem Linux native.
- WSL không phù hợp để kiểm thử Linux driver hoặc kernel ở mức thấp.
- Production server thường không chạy trong WSL; WSL chủ yếu là môi trường phát triển.

#### Khi nào không cần WSL?

- Chỉ phát triển ứng dụng Windows/.NET desktop.
- Toolchain và production đều là Windows.
- Chính sách công ty không cho bật ảo hóa/WSL.
- Cần kiểm thử server Linux hoàn chỉnh rất sát production; VM hoặc máy Linux riêng phù
  hợp hơn.

### 5.4 Hạn chế của Windows

#### Môi trường Unix không phải môi trường gốc

Nhiều hướng dẫn backend và DevOps dùng Bash, đường dẫn Unix và quyền file Unix. Trên
Windows, người dùng phải chuyển đổi sang PowerShell hoặc dùng WSL.

Ví dụ biến môi trường:

```bash
# Linux/macOS
export SPRING_PROFILES_ACTIVE=dev
```

```powershell
# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE = "dev"
```

#### Khác biệt đường dẫn và script

Windows thường dùng:

```text
C:\code\project
```

Linux/macOS thường dùng:

```text
/home/user/project
/Users/user/project
```

Một script giả định `/bin/bash`, dùng `chmod` hoặc dùng dấu `:` để phân cách đường dẫn có
thể không chạy trực tiếp trong PowerShell.

#### Cập nhật và phần mềm cài đặt từ nhiều nguồn

Ứng dụng Windows truyền thống có thể tự cung cấp updater riêng. Người dùng dễ có nhiều cơ
chế cập nhật cùng lúc:

- Windows Update.
- Microsoft Store.
- Updater riêng của ứng dụng.
- Công cụ quản lý package.
- Driver updater của nhà sản xuất.

#### Bloatware tùy nhà sản xuất

Một số máy cài sẵn nhiều ứng dụng của nhà sản xuất hoặc phần mềm dùng thử. Đây thường là
đặc điểm của bản cài trên máy OEM, không phải tất cả hệ thống Windows đều giống nhau.

#### Giấy phép

Windows là phần mềm thương mại. Giá giấy phép có thể đã được tính vào giá máy hoặc được
mua riêng.

### 5.5 Khi nào nên dùng Windows?

Windows là lựa chọn tốt khi:

- Chơi game PC.
- Dùng phần mềm chuyên ngành chỉ hỗ trợ Windows.
- Làm việc trong doanh nghiệp dùng Active Directory và Microsoft 365.
- Phát triển ứng dụng Windows hoặc .NET desktop.
- Muốn nhiều lựa chọn phần cứng và khả năng tự lắp máy.
- Cần Microsoft Office desktop với mức tương thích cao.

Windows kết hợp WSL cũng là môi trường phát triển backend tốt, đặc biệt khi người dùng vẫn
cần phần mềm Windows hằng ngày.

---

## 6. So sánh kiến trúc và cách sử dụng

### 6.1 Các lớp kiến trúc

Một hệ điều hành desktop/server có thể nhìn theo các lớp:

```text
Ứng dụng
→ runtime/framework/API hệ điều hành
→ thư viện hệ thống
→ system call
→ kernel
→ driver
→ phần cứng
```

So sánh cụ thể:

| Lớp | Linux distribution | macOS | Windows |
|---|---|---|---|
| Ứng dụng | ELF Linux, web app, Java app | Mach-O app, app bundle | PE `.exe`, app Windows |
| API/framework | POSIX, GTK/Qt, glibc API | Cocoa, AppKit, SwiftUI, POSIX | Win32, WinUI, .NET, DirectX |
| Thư viện hệ thống | glibc/musl và thư viện distro | Darwin libraries, Foundation | Windows system DLL |
| Kernel | Linux | XNU | Windows NT |
| Driver | Linux kernel module/driver | I/O Kit và driver Apple | Windows Driver Model |
| Hardware | Rất rộng | Model Mac Apple hỗ trợ | Rất rộng |

Ứng dụng Java thêm JVM vào giữa:

```text
Spring Boot
→ Java API
→ JVM dành cho từng OS/CPU
→ system call của OS
→ kernel tương ứng
```

Cùng file `.jar` có thể chạy ở ba hệ điều hành vì mỗi nơi dùng một JVM khác nhau. JVM
Linux biết gọi Linux system call; JVM Windows biết gọi Windows API/system call.

### 6.2 So sánh kernel chi tiết

| Hệ điều hành | Kernel | Kiểu mô tả phổ biến | Ý nghĩa thực tế |
|---|---|---|---|
| Linux | Linux kernel | Monolithic, hỗ trợ module | Nhiều subsystem/driver chạy trong kernel space; module có thể nạp động |
| macOS | XNU | Hybrid | Kết hợp Mach, BSD và I/O Kit |
| Windows | Windows NT | Hybrid | Executive services, kernel, HAL và driver cùng tạo nền tảng NT |

**Monolithic kernel** không có nghĩa toàn bộ code bị đóng thành một file không thể thay
đổi. Linux cho phép **kernel module** được nạp hoặc gỡ lúc chạy, ví dụ module driver.
“Monolithic” chủ yếu nói nhiều dịch vụ lõi và driver chạy trong cùng kernel address space.

**Hybrid kernel** là cách thiết kế pha trộn ý tưởng microkernel và monolithic. Tên gọi này
không tự động chứng minh hệ thống nhanh, chậm, an toàn hay kém an toàn hơn.

**Driver** là code giúp OS điều khiển một loại phần cứng. Driver GPU nhận yêu cầu đồ họa
từ OS/ứng dụng và chuyển thành lệnh GPU hiểu được. Driver lỗi ở kernel mode có thể làm
treo cả máy.

**Scheduler** là bộ lập lịch quyết định thread nào được dùng CPU, chạy trên core nào và
trong bao lâu. Workload server nhiều request khác workload game cần frame time ổn định,
nên không thể chỉ nhìn tên kernel để kết luận hiệu năng.

### 6.3 API và binary không tương thích tự động

CPU x86-64 giống nhau không đủ để binary chạy được ở mọi OS:

```text
app.exe Windows
→ định dạng PE
→ gọi Win32/Windows API
→ Windows loader và NT kernel

app Linux
→ định dạng ELF
→ dùng glibc/Linux ABI
→ Linux loader và Linux kernel

app macOS
→ định dạng Mach-O
→ dùng framework Apple
→ macOS loader và XNU
```

Muốn chạy ứng dụng Windows trên Linux cần port mã nguồn, máy ảo hoặc lớp tương thích như
Wine/Proton. Đổi đuôi `.exe` thành tên khác không chuyển đổi API và định dạng binary.

### 6.4 Boot và quản lý service

Luồng khởi động đã giản lược:

```text
Firmware UEFI
→ bootloader/boot manager
→ kernel được nạp
→ kernel nhận diện CPU, RAM, driver và mount filesystem
→ process/service manager khởi động user space
→ màn hình đăng nhập hoặc server sẵn sàng
```

| Công việc | Linux phổ biến | macOS | Windows |
|---|---|---|---|
| Boot/user-space manager | `systemd` trên nhiều distro | `launchd` | Service Control Manager và thành phần Windows |
| Service definition | unit file | plist | Windows Service |
| Log | journal/syslog/file | Unified Logging | Event Log |
| Quản trị | `systemctl`, `journalctl` | `launchctl`, Console | Services, PowerShell, Event Viewer |

Use case: chạy Spring Boot sau mỗi lần boot.

```text
Linux  → systemd unit
macOS  → launchd job
Windows→ Windows Service hoặc task/service wrapper
```

Linux thường thuận tiện cho server vì unit file, cấu hình và log dễ tự động hóa từ dòng
lệnh. Windows phù hợp nếu tổ chức đã quản trị service bằng Group Policy, PowerShell và
công cụ Microsoft. macOS làm được nhưng ít được dùng làm server production phổ thông.

### 6.5 Giao diện đồ họa

Linux tách các thành phần linh hoạt hơn:

```text
Linux kernel
→ display stack
→ desktop environment như GNOME/KDE
→ application
```

Người dùng có thể không cài GUI trên server hoặc thay desktop environment.

macOS và Windows cung cấp desktop do chính Apple/Microsoft kiểm soát. Người dùng thay
theme và cấu hình nhưng không thay toàn bộ desktop stack dễ dàng như Linux.

Hệ quả:

- Linux server tối giản được số package và service.
- Linux desktop có nhiều lựa chọn nhưng trải nghiệm khác nhau theo distro.
- macOS nhất quán hơn vì Apple kiểm soát model máy và UI.
- Windows nhất quán về nền tảng nhưng trải nghiệm có thể khác do ứng dụng OEM.

### 6.6 Mô hình phần cứng và driver

| Tình huống | Linux | macOS | Windows |
|---|---|---|---|
| PC tự lắp | Hỗ trợ rộng; phải kiểm tra Wi-Fi, GPU, thiết bị đặc biệt | Không hỗ trợ chính thức | Lựa chọn rộng nhất |
| Laptop OEM | Tùy model; chức năng đặc biệt có thể thiếu driver | Chỉ máy Mac, tích hợp chặt | Nhà sản xuất thường cung cấp driver |
| Apple silicon | Linux có dự án hỗ trợ nhưng không thay thế đầy đủ macOS cho mọi nhu cầu | Nền tảng chính thức | Không phải nền tảng cài Windows native phổ thông như PC |
| Server x86-64/ARM64 | Rất phổ biến | Không phải mục tiêu chính | Windows Server hỗ trợ phần cứng được chứng nhận |
| Thiết bị nhúng | Rất mạnh | Không phải use case | Có sản phẩm riêng nhưng Linux phổ biến hơn |

**OEM** là hãng bán máy hoàn chỉnh như Dell, HP, Lenovo. OEM có thể cài driver và tiện ích
riêng. **PC tự lắp** là người dùng tự chọn mainboard, CPU, GPU, RAM và thiết bị.

### 6.7 Mức kiểm soát và mô hình cập nhật

| Khía cạnh | Linux | macOS | Windows |
|---|---|---|---|
| Ai kiểm soát OS | Dự án kernel + distro + người dùng | Apple | Microsoft |
| Ai kiểm soát hardware | Nhiều hãng/người dùng | Apple | Nhiều hãng/người dùng |
| Đổi kernel | Có thể, cần kiến thức | Không theo cách được hỗ trợ | Không theo cách thông thường |
| Đổi desktop | Rất linh hoạt | Hạn chế | Hạn chế |
| Cập nhật app | Package manager/repository và nguồn khác | App Store, updater, Homebrew | Store, WinGet, updater riêng |
| Cập nhật driver | Kernel/package/vendor | Gắn chặt với update Apple | Windows Update/vendor |

Linux cho nhiều quyền quyết định nhất nhưng người dùng chịu thêm trách nhiệm tương thích.
macOS giảm lựa chọn để Apple kiểm thử tập cấu hình hẹp hơn. Windows đứng giữa: phần cứng
rộng, còn nền tảng OS do Microsoft kiểm soát.

### 6.8 So sánh theo use case kỹ thuật

| Use case | Linux | macOS | Windows |
|---|---|---|---|
| Web/API production | Rất phù hợp: nhẹ, container native, automation tốt | Hiếm dùng | Dùng khi app/IIS/.NET/Windows dependency yêu cầu |
| Backend trên máy cá nhân | Rất gần production | Unix workflow tốt, Docker qua VM | Tốt; WSL 2 bổ sung Linux workflow |
| iOS development | Chỉ viết code chung; build cuối cần Mac | Lựa chọn bắt buộc cho Xcode | Chỉ viết code chung; build cuối cần Mac |
| Windows desktop | Không build/test native đầy đủ | Không tối ưu | Tốt nhất |
| Kubernetes node | Nền tảng chính | Dùng cluster local qua VM/container | Dev local tốt; production thường Linux node |
| Game PC | Tốt dần nhưng phải kiểm tra game | Danh mục nhỏ hơn | Phù hợp nhất |
| Máy cũ | Có distro nhẹ | Bị giới hạn model macOS hỗ trợ | Tùy yêu cầu phiên bản Windows |
| Doanh nghiệp Microsoft | Có thể tích hợp | Có thể quản lý bằng MDM | Tích hợp sâu nhất |
| Tùy biến/nghiên cứu OS | Tốt nhất | Hạn chế | Hạn chế |

### 6.9 Vì sao không thể nói OS nào luôn nhanh hơn?

Hiệu năng phải gắn với workload:

- **Game** phụ thuộc GPU driver, DirectX/Vulkan, anti-cheat và tối ưu của nhà phát triển.
- **Build Java** phụ thuộc CPU, SSD, RAM, antivirus scanning và số file.
- **Database** phụ thuộc filesystem, page cache, I/O scheduler và cấu hình.
- **Laptop** phụ thuộc power management, firmware và driver.
- **Container** trên Linux dùng kernel host trực tiếp; macOS/Windows thường cần Linux VM.

Ví dụ Mac ARM64 có thể build một dự án Java rất nhanh nhưng image chỉ có `amd64` phải mô
phỏng sẽ chậm. Windows native có thể chơi game nhanh hơn Linux trên cùng máy, trong khi
Linux có thể chạy Docker workload nhẹ hơn vì không cần Linux VM trung gian.

---

## 7. So sánh filesystem và đường dẫn

### 7.1 File, thư mục, ổ đĩa và filesystem khác nhau thế nào?

- **File** là đơn vị dữ liệu có tên: source code, ảnh, database file.
- **Directory/thư mục** chứa tên và tham chiếu tới file/thư mục khác.
- **Partition/phân vùng** là một vùng logic trên thiết bị lưu trữ.
- **Volume** là đơn vị lưu trữ mà OS có thể mount; có thể tương ứng một partition hoặc cấu
  trúc phức tạp hơn.
- **Filesystem** là quy tắc và cấu trúc dữ liệu dùng để đặt tên, lưu, tìm và bảo vệ file.
- **Mount** là gắn một filesystem vào cây thư mục hoặc drive letter để ứng dụng truy cập.

SSD chỉ cung cấp các block dữ liệu. Filesystem quyết định:

- File có tên gì.
- Block nào thuộc file nào.
- Kích thước file.
- Thời gian tạo/sửa.
- Owner và permission.
- Thư mục cha.
- Block nào còn trống.
- Cách phục hồi metadata sau khi mất điện.

```text
Ứng dụng muốn đọc /home/tony/a.txt
→ OS phân tích path
→ filesystem tìm metadata của a.txt
→ xác định block dữ liệu
→ block layer/driver đọc SSD
→ trả byte cho ứng dụng
```

### 7.2 Cây thư mục và mount point

Linux/macOS có một cây bắt đầu từ root `/`:

```text
/
├── home hoặc Users
├── etc
├── var
├── tmp
└── ...
```

Một SSD phụ có thể được mount tại `/data`:

```text
/data/orders.db
```

Ứng dụng không cần biết `/data` nằm trên SSD thứ hai; nó chỉ dùng path.

Windows truyền thống biểu diễn volume bằng drive letter:

```text
C:\Users\Tony
D:\Data
```

Windows cũng hỗ trợ mount volume vào folder, nhưng drive letter vẫn là cách người dùng
gặp nhiều nhất.

### 7.3 Path tuyệt đối, path tương đối và thư mục làm việc

Path tuyệt đối chỉ rõ từ root/drive:

```text
/home/tony/project/pom.xml
C:\code\project\pom.xml
```

Path tương đối được tính từ **current working directory**:

```text
config/application.yml
```

Nếu working directory là `/home/tony/project`, path trên trở thành:

```text
/home/tony/project/config/application.yml
```

Lỗi phổ biến: ứng dụng chạy đúng trong IDE nhưng lỗi khi chạy service vì working directory
khác. Không nên giả định file cấu hình luôn nằm tương đối với nơi user mở terminal.

### 7.4 Dấu phân cách và code portable

| Hệ thống | Dấu phân cách thường dùng |
|---|---|
| Linux | `/` |
| macOS | `/` |
| Windows | `\`; nhiều API chấp nhận `/` |

Trong Java:

```java
Path config = Path.of("config", "application.yml");
```

Java tự dùng quy tắc phù hợp OS. Không nên hard-code:

```java
String path = "config\\application.yml";
```

### 7.5 Metadata là gì?

**Metadata** là dữ liệu mô tả file, không phải nội dung chính:

- Tên.
- Kích thước.
- Owner/group.
- Permission hoặc ACL.
- Thời điểm tạo/sửa/truy cập.
- File type.
- Extended attributes.
- Vị trí block.

Copy file sang filesystem khác có thể giữ nội dung nhưng mất metadata. Ví dụ copy shell
script từ ext4 sang nơi không bảo toàn executable bit có thể khiến script không còn quyền
chạy.

### 7.6 Case sensitivity

Linux thường phân biệt:

```text
UserService.java
userservice.java
```

Windows thường **case-preserving nhưng case-insensitive**:

- Giữ cách viết `UserService.java`.
- Tìm `userservice.java` vẫn có thể trỏ tới cùng file.

macOS mặc định thường tương tự Windows nhưng có thể dùng APFS case-sensitive.

Lỗi Git thực tế:

```text
Developer tạo: userService.ts
Code import : UserService.ts
```

Máy Windows/macOS mặc định có thể vẫn chạy. Linux CI phân biệt hoa/thường nên build lỗi.

### 7.7 Link: hard link và symbolic link

**Symbolic link — symlink** là file đặc biệt chứa đường dẫn tới mục tiêu:

```bash
ln -s /opt/app/releases/2.0 /opt/app/current
```

`current` trỏ tới release `2.0`. Đổi symlink có thể chuyển phiên bản triển khai nhanh.

**Hard link** là thêm một tên thư mục trỏ tới cùng file data/inode. Xóa một tên chưa xóa
dữ liệu nếu hard link khác còn tồn tại.

Windows cũng có symbolic link, hard link và junction, nhưng quyền tạo và hành vi công cụ
có khác. Git repository dùng symlink cần được kiểm thử trên Windows.

### 7.8 Journaling, checksum, snapshot, copy-on-write

**Journaling** ghi lại dự định thay đổi metadata trước khi hoàn tất. Sau mất điện, filesystem
có thể dùng journal để đưa cấu trúc về trạng thái nhất quán. Journaling không thay thế
backup và không bảo đảm nội dung file mới nhất không mất.

**Checksum** là giá trị tính từ dữ liệu/metadata để phát hiện thay đổi hoặc lỗi hỏng. Nếu
bit trên ổ bị thay đổi, checksum không khớp có thể báo lỗi. Khả năng tự sửa còn phụ thuộc
có bản sao dữ liệu tốt hay không.

**Snapshot** ghi lại trạng thái volume/subvolume tại một thời điểm. Snapshot thường tạo
nhanh vì ban đầu không sao chép toàn bộ dữ liệu.

**Copy-on-write — CoW** nghĩa là khi sửa block đang được chia sẻ, filesystem ghi block mới
thay vì ghi đè block cũ:

```text
Snapshot và volume hiện tại cùng trỏ block A
→ sửa file
→ ghi block B mới
→ volume hiện tại trỏ B
→ snapshot vẫn trỏ A
```

Snapshot hữu ích để rollback nhưng không phải backup nếu snapshot nằm cùng ổ và ổ hỏng.

### 7.9 ACL, permission và mã hóa

**Permission bits** kiểu Unix có quyền read/write/execute cho owner, group và others.

**ACL — Access Control List** cho phép quy tắc chi tiết hơn, ví dụ:

```text
Tony       : đọc + ghi
Developers : đọc
Auditors   : chỉ đọc
Guest      : không truy cập
```

**Mã hóa filesystem/ổ đĩa** biến dữ liệu thành dạng không đọc được nếu thiếu khóa. Nó bảo
vệ khi laptop hoặc ổ bị đánh cắp, nhưng khi user đã đăng nhập và volume được mở khóa,
malware chạy bằng quyền user vẫn có thể đọc những file user được phép đọc.

### 7.10 ext4, XFS, Btrfs, APFS và NTFS

| Filesystem | Thường dùng | Điểm mạnh | Điểm cần hiểu |
|---|---|---|---|
| ext4 | Linux desktop/server | Ổn định, công cụ trưởng thành, phổ biến | Ít tính năng quản lý volume tích hợp hơn Btrfs |
| XFS | Linux server, dữ liệu lớn | Mở rộng tốt, I/O song song, file lớn | Thiết kế và công cụ quản trị khác ext4 |
| Btrfs | Linux cần snapshot/CoW | Snapshot, subvolume, checksum, CoW | Cần hiểu workload và cách vận hành |
| APFS | macOS/iPhone/iPad | CoW, snapshot, clone, mã hóa, tối ưu hệ sinh thái Apple | Hỗ trợ ngoài Apple hạn chế hơn |
| NTFS | Windows | ACL phong phú, journaling, alternate data streams, nén/mã hóa | Permission và metadata khác Unix |

Không có filesystem “tốt nhất” cho mọi workload. Database có thể quan tâm latency và
fsync; laptop quan tâm mã hóa và snapshot; media server quan tâm file lớn.

### 7.11 Các thư mục thường gặp

| Mục đích | Linux | macOS | Windows |
|---|---|---|---|
| Home user | `/home/tony` | `/Users/tony` | `C:\Users\Tony` |
| Cấu hình hệ thống | `/etc` | `/etc` và hệ thống Apple | Registry, `ProgramData`, thư mục app |
| Log | `/var/log`/journal | Unified Log, `/var/log` | Event Log, thư mục app |
| File tạm | `/tmp` | `/tmp` | `%TEMP%` |
| App cài chung | `/usr`, `/opt` | `/Applications` | `C:\Program Files` |
| Dữ liệu app user | dotfiles, `.config` | `~/Library` | `%APPDATA%`, `%LOCALAPPDATA%` |

### 7.12 Vì sao OS không luôn đọc/ghi filesystem của nhau?

Để ghi an toàn, OS cần driver hiểu đầy đủ:

- Cấu trúc metadata.
- Journaling/CoW.
- Permission và ACL.
- Mã hóa.
- Quy tắc tên file.
- Cách phục hồi lỗi.

Windows không mặc định dùng ext4 như volume desktop thông thường; macOS không mặc định
ghi NTFS đầy đủ; Linux có driver cho nhiều filesystem nhưng mức hỗ trợ khác nhau. Thiết
bị trao đổi thường dùng exFAT vì nhiều OS hỗ trợ, nhưng exFAT không có đầy đủ permission
và journaling như filesystem hệ thống.

### 7.13 Filesystem ảnh hưởng developer thế nào?

- Case sensitivity làm import sai chỉ lỗi trên Linux.
- Line ending CRLF/LF làm shell script lỗi.
- Executable bit ảnh hưởng `mvnw`, script deploy.
- File watcher ảnh hưởng hot reload.
- Antivirus trên Windows có thể scan hàng nghìn file build.
- Project WSL đặt trên `/mnt/c` có thể chậm hơn `/home`.
- Docker bind mount trên macOS/Windows đi qua Linux VM.
- Symlink và ACL có hành vi khác.

Checklist đa nền tảng:

```text
□ Dùng Path API, không ghép separator thủ công
□ Chuẩn hóa tên file đúng chữ hoa/thường
□ Dùng .gitattributes cho line ending
□ Không phụ thuộc hard-coded home/tmp path
□ Test trên OS production
□ Không giả định permission giống nhau
```

---

## 8. So sánh terminal và dòng lệnh

### 8.1 Linux và macOS

Hai hệ thống có môi trường dòng lệnh kiểu Unix. Nhiều lệnh cơ bản giống nhau:

```bash
pwd
ls -la
cd project
cp source.txt target.txt
mv old.txt new.txt
rm file.txt
ps aux
kill 1234
```

Nhưng công cụ trên macOS thường là biến thể BSD, còn nhiều Linux distribution dùng công
cụ GNU. Tham số của một số lệnh có thể khác nhau.

Ví dụ một lệnh `sed` chạy trên GNU/Linux chưa chắc chạy nguyên vẹn trên macOS BSD `sed`.

### 8.2 Windows

Windows có hai môi trường chính:

- Command Prompt, còn gọi là `cmd.exe`.
- PowerShell.

PowerShell làm việc với object thay vì chỉ truyền chuỗi văn bản.

```powershell
Get-Process |
    Where-Object { $_.CPU -gt 10 } |
    Sort-Object CPU -Descending
```

Lệnh trên:

1. Lấy danh sách process.
2. Chọn process đã dùng hơn 10 giây CPU.
3. Sắp xếp theo CPU giảm dần.

Các alias như `ls` có thể tồn tại trong PowerShell, nhưng hành vi không nhất thiết giống
lệnh GNU `ls`.

### 8.3 So sánh lệnh thường dùng

| Công việc | Linux/macOS | Windows PowerShell |
|---|---|---|
| Liệt kê file | `ls -la` | `Get-ChildItem -Force` |
| Xem thư mục hiện tại | `pwd` | `Get-Location` |
| Sao chép file | `cp a.txt b.txt` | `Copy-Item a.txt b.txt` |
| Xóa file | `rm a.txt` | `Remove-Item a.txt` |
| Xem process | `ps aux` | `Get-Process` |
| Dừng process | `kill PID` | `Stop-Process -Id PID` |
| Đặt biến môi trường | `export A=value` | `$env:A = "value"` |
| Tìm chuỗi | `grep text file` | `Select-String text file` |
| Tìm executable | `which java` | `Get-Command java` |

### 8.4 Script có portable không?

Không nên giả định một script shell chạy trên cả ba hệ điều hành.

```text
script.sh  → Bash/Zsh trên Linux hoặc macOS
script.ps1 → PowerShell trên Windows, cũng có thể chạy nơi đã cài PowerShell
script.bat → Command Prompt trên Windows
```

Để tăng tính portable trong dự án Java:

- Cung cấp cả `mvnw` và `mvnw.cmd`.
- Dùng Java API để xử lý đường dẫn.
- Không hard-code `/tmp` hoặc `C:\temp`.
- Dùng biến môi trường cho cấu hình.
- Chạy CI trên hệ điều hành production.

---

## 9. Cài đặt và quản lý phần mềm

### 9.1 Linux

Package manager cài phần mềm từ repository của distribution.

```bash
sudo apt update
sudo apt install nginx
sudo apt upgrade
```

Package manager quản lý:

- Phiên bản.
- Dependency.
- File đã cài.
- Cập nhật.
- Gỡ cài đặt.

Các định dạng/package system phổ biến:

- `.deb` trên Debian/Ubuntu.
- `.rpm` trên Fedora/RHEL.
- Snap.
- Flatpak.
- AppImage.

### 9.2 macOS

Các cách phổ biến:

- Mac App Store.
- Tải file `.dmg` hoặc `.pkg`.
- Homebrew cho công cụ phát triển.

```bash
brew install maven
brew install --cask visual-studio-code
brew update
brew upgrade
```

Homebrew không phải thành phần mặc định của macOS; người dùng phải cài riêng.

### 9.3 Windows

Các cách phổ biến:

- Microsoft Store.
- File cài `.exe`.
- Windows Installer `.msi`.
- WinGet.
- Công cụ doanh nghiệp như Intune hoặc Configuration Manager.

```powershell
winget search Git.Git
winget install Git.Git
winget upgrade --all
```

### 9.4 Khác biệt quan trọng

Trên Linux, repository tập trung là cách cài phần mềm truyền thống. Windows và macOS có
store/package manager nhưng người dùng vẫn thường tải bộ cài trực tiếp từ website.

Khi tải bộ cài trực tiếp, cần:

- Dùng website chính thức.
- Kiểm tra chữ ký số nếu có.
- Tránh website đóng gói lại installer.
- Bật cập nhật bảo mật.

---

## 10. Tài khoản, quyền và bảo mật

### 10.1 Linux/macOS: owner, group và permission bits

Ví dụ:

```bash
ls -l deploy.sh
```

Kết quả:

```text
-rwxr-x--- 1 tony developers 1200 Jun 20 10:00 deploy.sh
```

Ý nghĩa:

```text
owner tony       : đọc, ghi, thực thi
group developers : đọc, thực thi
người khác       : không có quyền
```

Thay đổi quyền:

```bash
chmod 750 deploy.sh
```

Chạy tác vụ quản trị:

```bash
sudo systemctl restart nginx
```

`sudo` không có nghĩa là mọi lệnh đều an toàn. Nó chỉ chạy lệnh với quyền cao hơn nếu
chính sách cho phép.

### 10.2 Windows: ACL và UAC

Windows dùng Access Control List — ACL — để quy định quyền chi tiết cho user và group.

Ví dụ một thư mục có thể cho phép:

- Nhóm Developers: đọc và ghi.
- Nhóm Auditors: chỉ đọc.
- User khác: không truy cập.
- Administrator: toàn quyền.

User Account Control — UAC — yêu cầu xác nhận khi chương trình muốn thực hiện hành động
cần quyền quản trị.

### 10.3 Cơ chế bảo mật tích hợp

| Linux | macOS | Windows |
|---|---|---|
| User/group, permission, ACL | Unix permission, ACL | NTFS ACL |
| SELinux hoặc AppArmor tùy distro | Gatekeeper, sandbox, System Integrity Protection | Microsoft Defender, SmartScreen, UAC |
| Package repository và chữ ký package | Code signing, notarization | Code signing, Windows Security |
| Firewall tùy hệ thống | Firewall tích hợp | Windows Defender Firewall |

Không hệ điều hành nào tự động an toàn trong mọi cấu hình. Mức an toàn phụ thuộc vào:

- Cập nhật bảo mật.
- Mật khẩu và MFA.
- Quyền tối thiểu.
- Nguồn cài phần mềm.
- Cấu hình firewall.
- Mã hóa ổ đĩa.
- Sao lưu.
- Cách người dùng xử lý file và đường link không tin cậy.

### 10.4 Malware

**Malware — malicious software — phần mềm độc hại** là code được tạo ra hoặc bị chỉnh sửa
để thực hiện hành vi có hại mà chủ máy không mong muốn: đánh cắp dữ liệu, mã hóa file đòi
tiền, theo dõi, phá hệ thống hoặc dùng tài nguyên máy.

Malware là tên nhóm, không phải chỉ một loại virus.

| Loại | Cách hoạt động | Ví dụ hậu quả |
|---|---|---|
| Virus | Gắn vào file/chương trình khác và lây khi file chạy | Làm hỏng hoặc sửa file |
| Worm | Tự lan qua mạng/lỗ hổng mà ít cần user thao tác | Lây nhiều máy trong tổ chức |
| Trojan | Giả làm phần mềm hợp lệ để user tự cài | Mở cửa hậu, đánh cắp mật khẩu |
| Ransomware | Mã hóa hoặc khóa dữ liệu rồi đòi tiền | Không mở được tài liệu/database |
| Spyware | Theo dõi thao tác và thu thập thông tin | Mất lịch sử duyệt web, tài khoản |
| Keylogger | Ghi phím bấm | Lộ mật khẩu, nội dung chat |
| Rootkit | Che giấu malware và duy trì quyền cao | Khó phát hiện/gỡ bỏ |
| Backdoor | Tạo đường truy cập bí mật | Kẻ tấn công quay lại máy |
| Cryptominer | Dùng CPU/GPU để đào tiền mã hóa | Máy chậm, tăng điện/chi phí cloud |
| Bot | Biến máy thành thành viên botnet | Gửi spam, DDoS, quét mục tiêu khác |

**Virus** chỉ là một loại malware. Câu “máy có virus” thường được dùng rộng, nhưng về kỹ
thuật có thể đó là trojan, ransomware hoặc spyware.

#### Malware vào máy bằng cách nào?

1. User chạy file đính kèm hoặc phần mềm giả.
2. Dịch vụ public có lỗ hổng chưa vá bị khai thác.
3. Mật khẩu yếu hoặc credential bị lộ.
4. Package/library dependency bị cài mã độc.
5. Macro, script hoặc browser extension độc hại.
6. USB/thiết bị hoặc tài liệu khai thác lỗ hổng.
7. Kẻ tấn công đã vào một máy và di chuyển sang máy khác.

**Lỗ hổng — vulnerability** là điểm yếu trong code/cấu hình. **Exploit** là kỹ thuật hoặc
code lợi dụng điểm yếu đó. Malware có thể được cài sau khi exploit thành công.

#### Phishing không nhất thiết là malware

**Phishing** là lừa người dùng cung cấp mật khẩu, OTP hoặc chạy hành động nguy hiểm. Một
email giả trang đăng nhập có thể đánh cắp tài khoản mà không cài file malware nào.

```text
Email giả
→ user bấm link
→ website giống Microsoft/Google
→ user nhập mật khẩu
→ kẻ tấn công nhận credential
```

#### Windows bị nhắm tới như thế nào?

Windows có lượng desktop/doanh nghiệp lớn và nhiều phần mềm cũ, tài liệu Office, installer
`.exe`, script PowerShell và hệ thống domain. Điều đó tạo mục tiêu kinh tế hấp dẫn.

Cơ chế bảo vệ gồm:

- Microsoft Defender Antivirus.
- SmartScreen cảnh báo file/site không tin cậy.
- UAC hạn chế nâng quyền âm thầm.
- Windows Update vá OS.
- Firewall.
- BitLocker mã hóa ổ đĩa ở edition/cấu hình phù hợp.

Nhưng Defender không thể cứu mọi tình huống. Nếu user tự cấp quyền admin cho trojan hoặc
credential domain bị đánh cắp, kẻ tấn công vẫn có thể gây thiệt hại.

#### Linux server bị tấn công cụ thể ra sao?

**Đánh cắp SSH key**: private key cho phép đăng nhập server. Nếu key không được bảo vệ và
bị lấy, kẻ tấn công có thể đăng nhập như chủ key.

```text
Laptop developer bị chiếm
→ file private key bị lấy
→ attacker SSH vào server được key cho phép
```

**Dịch vụ chưa vá**: Nginx, framework, VPN, SSH server hoặc ứng dụng có lỗ hổng public.
Attacker gửi request đặc biệt để thực thi code hoặc đọc dữ liệu.

**Cryptominer**: server cloud có CPU mạnh và chạy 24/7. Attacker cài miner để dùng tài
nguyên của nạn nhân; hóa đơn cloud tăng và service thật bị chậm.

**Supply-chain attack**: code độc hại đi vào qua dependency, build plugin, image, package
repository hoặc tài khoản maintainer bị chiếm.

```text
Dependency bị cài mã độc
→ CI tải dependency
→ code độc chạy trong CI
→ đọc token/secret
→ tấn công registry hoặc production
```

**Secret bị lộ**: password database, API key, cloud token hoặc private key bị commit vào
Git, log ra console hoặc nhúng trong container image.

Linux không “miễn nhiễm” malware. Linux server thường không bị kiểu user tải game crack,
nhưng là mục tiêu giá trị cao vì chứa dữ liệu, secret và quyền truy cập hạ tầng.

#### macOS bị tấn công cụ thể ra sao?

**Ứng dụng giả mạo**: user tải một file giống công cụ hợp lệ, sau đó cấp quyền cài.

**Infostealer**: malware tìm cookie trình duyệt, password, crypto wallet, SSH key và token.

**Lạm dụng Accessibility**: quyền Accessibility có thể cho app quan sát/điều khiển thao
tác giao diện. Cấp nhầm quyền này cho app độc hại rất nguy hiểm.

**Configuration profile/MDM giả**: profile có thể thay cấu hình certificate, proxy, VPN
hoặc quản lý thiết bị. User không nên cài profile không rõ nguồn.

macOS có Gatekeeper, notarization, sandbox và System Integrity Protection, nhưng user vẫn
có thể bị lừa cấp quyền hoặc malware khai thác lỗ hổng.

#### Bảo vệ thực tế trên cả ba OS

- Cập nhật OS, browser, JDK, package và ứng dụng.
- Dùng password manager và MFA.
- Không chạy hằng ngày bằng quyền admin/root nếu không cần.
- Chỉ tải phần mềm từ nguồn chính thức.
- Kiểm tra domain và chữ ký số.
- Không commit secret; dùng secret manager.
- Sao lưu theo nguyên tắc có bản tách khỏi máy.
- Giới hạn port public và quyền service account.
- Quét dependency/container image trong CI.
- Với server: theo dõi log, process, network và chi phí bất thường.

**Antivirus không thay thế backup.** Ransomware có thể mã hóa cả file đồng bộ; cần backup
có versioning hoặc bản offline/immutable phù hợp.

---

## 11. Process, service và quản trị hệ thống

### 11.1 Xem process

Linux:

```bash
ps aux
top
htop
```

macOS:

```bash
ps aux
top
```

Ngoài ra có ứng dụng Activity Monitor.

Windows:

```powershell
Get-Process
Get-Process -Name java
```

Ngoài ra có Task Manager và Resource Monitor.

### 11.2 Quản lý service

Linux với `systemd`:

```bash
sudo systemctl start nginx
sudo systemctl stop nginx
sudo systemctl enable nginx
systemctl status nginx
journalctl -u nginx
```

macOS với `launchd`:

```bash
launchctl list
```

Windows:

```powershell
Get-Service
Start-Service -Name Spooler
Stop-Service -Name Spooler
```

Hoặc sử dụng ứng dụng Services.

### 11.3 Log

| Hệ điều hành | Nơi/công cụ thường dùng |
|---|---|
| Linux | `journalctl`, `/var/log` |
| macOS | Console, Unified Logging, lệnh `log` |
| Windows | Event Viewer, `Get-WinEvent` |

Ví dụ Windows:

```powershell
Get-WinEvent -LogName System -MaxEvents 20
```

---

## 12. Lập trình và phát triển phần mềm

### 12.1 Lập trình backend Java

Cả ba hệ điều hành đều chạy được:

- JDK.
- Maven.
- Gradle.
- IntelliJ IDEA.
- Visual Studio Code.
- Git.
- PostgreSQL.
- Docker Desktop hoặc Docker Engine theo nền tảng.

Mã Java thuần thường portable nhờ JVM, nhưng dự án vẫn có thể phụ thuộc hệ điều hành nếu:

- Gọi executable native.
- Dùng JNI.
- Hard-code đường dẫn.
- Phụ thuộc permission Unix.
- Dùng shell script.
- Phân biệt chữ hoa/thường sai.
- Dùng native library chỉ có cho một kiến trúc.

### 12.2 Frontend và web

Node.js, npm, pnpm, Yarn và các trình duyệt phổ biến đều hỗ trợ cả ba.

Vấn đề thường gặp là filesystem watcher và số lượng file:

- Cách theo dõi thay đổi file khác nhau giữa OS.
- Thư mục `node_modules` có rất nhiều file.
- Mount source từ Windows/macOS vào Linux container có thể chậm hơn chạy source trực tiếp
  trong filesystem Linux.

Trên Windows với WSL, nên đặt project Linux ở:

```text
/home/tony/project
```

thay vì luôn đặt ở:

```text
/mnt/c/project
```

nếu workload tạo và đọc rất nhiều file nhỏ. Hiệu năng cụ thể cần đo trên máy thật.

### 12.3 Phát triển ứng dụng native

| Mục tiêu | Hệ điều hành/công cụ phù hợp |
|---|---|
| iOS, iPadOS, macOS | macOS + Xcode |
| Windows desktop với WinUI/WPF | Windows + Visual Studio |
| Linux desktop | Linux + toolkit phù hợp |
| Android | Cả ba, dùng Android Studio |
| Web/backend | Cả ba |

### 12.4 .NET

.NET hiện đại chạy đa nền tảng, nên ASP.NET Core có thể phát triển và chạy trên Linux,
macOS hoặc Windows. Tuy nhiên:

- WPF và WinForms là công nghệ Windows desktop.
- Một số API Windows không portable.
- ASP.NET Core production có thể chạy trên Linux rất tốt.

### 12.5 C/C++

C/C++ phụ thuộc compiler, ABI, system API và kiến trúc CPU nhiều hơn Java.

Ví dụ:

```text
Binary biên dịch cho Windows x86-64
không chạy trực tiếp trên Linux x86-64
```

Dù CPU giống nhau, định dạng executable và API hệ điều hành khác:

- Windows thường dùng PE.
- Linux thường dùng ELF.
- macOS dùng Mach-O.

---

## 13. Container, Docker và máy ảo

### 13.1 Container Linux cần Linux kernel

Container chia sẻ kernel với host hoặc với một Linux VM trung gian.

Trên Linux:

```text
Docker container Linux
→ Docker Engine
→ Linux kernel của host
```

Trên macOS:

```text
Docker container Linux
→ Docker Desktop
→ Linux virtual machine
→ macOS host
```

Trên Windows dùng Docker Desktop với WSL 2:

```text
Docker container Linux
→ Docker Desktop/WSL 2
→ Linux kernel trong môi trường ảo hóa
→ Windows host
```

Vì macOS không dùng Linux kernel và Windows không dùng Linux kernel, chúng cần một lớp
Linux VM để chạy Linux container.

### 13.2 Container không phải máy ảo đầy đủ

Linux container không chứa một Linux kernel riêng. Nó chứa user space, thư viện và ứng
dụng; kernel được chia sẻ.

```text
Ubuntu container trên Fedora host
```

Điều này có nghĩa:

- User space trong container có thể giống Ubuntu.
- Kernel vẫn là kernel Linux của host.
- Container không chạy một kernel Ubuntu riêng.

### 13.3 Kiến trúc CPU

Trên Mac Apple silicon:

```text
Host: arm64
Image tốt nhất: linux/arm64
```

Trên PC Intel/AMD phổ biến:

```text
Host: x86-64/amd64
Image tốt nhất: linux/amd64
```

Multi-architecture image cung cấp nhiều binary dưới cùng một tag. Docker chọn biến thể
phù hợp với host.

### 13.4 Máy ảo

**Máy ảo — Virtual Machine, VM** là một máy tính được tạo bằng phần mềm. VM có CPU ảo,
RAM ảo, ổ đĩa ảo, card mạng ảo và chạy guest OS có kernel riêng.

Các khái niệm:

- **Host**: máy/OS thật đang cung cấp tài nguyên.
- **Guest**: hệ điều hành chạy bên trong VM.
- **Hypervisor**: phần mềm/lớp quản lý VM và chia CPU, RAM, thiết bị.
- **Virtual disk**: file hoặc volume được guest nhìn như ổ đĩa.
- **Virtual NIC**: card mạng ảo của guest.

```text
Phần cứng
→ host OS/hypervisor
→ virtual hardware
→ guest kernel
→ guest user space
→ ứng dụng trong VM
```

Ví dụ Windows host chạy Ubuntu VM:

```text
Laptop Windows có 16 GB RAM
→ cấp Ubuntu VM 4 GB RAM, 2 vCPU, disk 40 GB
→ Ubuntu boot Linux kernel riêng
→ cài Nginx/PostgreSQL trong Ubuntu
```

`vCPU` là CPU logic mà hypervisor trình bày cho VM, không phải tự sinh thêm CPU vật lý.
Nếu cấp quá nhiều vCPU/RAM cho nhiều VM, chúng vẫn tranh tài nguyên máy thật.

#### VM và container khác nhau

| Tiêu chí | VM | Container |
|---|---|---|
| Kernel | Guest có kernel riêng | Chia sẻ kernel host/VM trung gian |
| Có thể chạy OS khác kernel | Có | Không theo cách container thông thường |
| Khởi động | Boot OS, thường chậm hơn | Khởi động process, thường nhanh hơn |
| Dung lượng | Thường lớn hơn | Thường nhỏ hơn |
| Cách ly | Ranh giới VM mạnh hơn theo mặc định | Ranh giới process/namespace |
| Use case | OS khác, kernel khác, lab hạ tầng | Đóng gói và scale ứng dụng |

Ví dụ:

```text
Windows host muốn chạy Linux kernel
→ dùng Ubuntu VM hoặc WSL 2

Linux host muốn chạy Ubuntu app tách biệt
→ thường chỉ cần Ubuntu container
```

#### Snapshot VM là gì?

Snapshot lưu trạng thái disk và có thể gồm trạng thái memory/config của VM tại một thời
điểm. Sau khi thử cấu hình nguy hiểm, có thể quay lại snapshot.

```text
Snapshot A: Windows vừa cài sạch
→ cài phần mềm thử nghiệm
→ hệ thống lỗi
→ revert Snapshot A
```

Snapshot không thay thế backup:

- Snapshot thường phụ thuộc disk gốc.
- Quá nhiều snapshot làm chuỗi disk phức tạp và có thể giảm hiệu năng.
- Host/storage hỏng có thể mất cả VM lẫn snapshot.

#### Network mode của VM

- **NAT**: guest ra Internet qua host; máy ngoài thường không truy cập guest trực tiếp nếu
  chưa port-forward.
- **Bridged**: guest xuất hiện như máy riêng trong LAN và nhận IP riêng.
- **Host-only**: mạng riêng giữa host và guest.

Use case lab backend:

```text
Ubuntu VM bridged
→ IP 192.168.1.50
→ laptop khác trong LAN gọi http://192.168.1.50:8080
```

#### Khi nào chọn VM?

- Cần Windows guest trên Linux hoặc Linux guest trên Windows.
- Cần kernel riêng để test module, firewall hoặc system service.
- Học cài OS, partition, boot và network.
- Chạy phần mềm cũ trong OS cũ đã được cô lập.
- Tạo lab nhiều server: app, database, load balancer.
- Cần snapshot toàn môi trường.

#### Hạn chế của VM

- Tốn RAM/disk hơn container.
- Boot và update cả guest OS.
- Cần quản lý patch cho từng VM.
- GPU/USB passthrough có thể phức tạp.
- Khác kiến trúc CPU có thể cần emulation.

Trên Mac Apple silicon ARM64, Linux ARM64 VM chạy tự nhiên hơn Linux x86-64 VM. Chạy guest
x86-64 có thể cần mô phỏng và chậm hơn. Windows x86 truyền thống cũng không tự nhiên chạy
như Windows ARM trong môi trường ARM.

---

## 14. Game, đồ họa và sáng tạo nội dung

### 14.1 Game: các lớp cần hiểu

Một game không chỉ là một file `.exe`. Nó phụ thuộc nhiều lớp:

```text
Game
→ game engine và API đồ họa
→ shader/compiler
→ driver GPU
→ kernel/OS
→ GPU, CPU, input, audio
```

Ngoài ra có thể có launcher, DRM, anti-cheat, mod manager và dịch vụ online. Chỉ cần một
lớp không hỗ trợ OS thì game có thể không chạy.

#### Native game là gì?

Game native Linux được nhà phát triển build cho Linux, thường dùng binary ELF và API như
Vulkan/OpenGL. Game native macOS được build cho macOS/Metal. Game Windows thường là PE
`.exe` và dùng Win32/DirectX.

Native không tự động nhanh hơn lớp tương thích; chất lượng port và driver mới quyết định.

#### Steam là gì và giúp Linux thế nào?

**Steam** là nền tảng phân phối/quản lý game của Valve. Steam cung cấp:

- Mua và tải game.
- Cập nhật.
- Cloud save.
- Community.
- Controller configuration.
- Steam Play để chọn lớp tương thích cho game Windows.

Steam tạo một workflow thống nhất: người dùng không phải tự cấu hình Wine thủ công cho
mỗi game trong nhiều trường hợp.

#### Wine là gì?

**Wine** là lớp tương thích triển khai nhiều Windows API trên Unix-like OS. Wine không
chạy toàn bộ Windows trong VM và không chứa Windows kernel.

```text
Game gọi Win32 API
→ Wine chuyển/triển khai lời gọi phù hợp
→ Linux API/system call
→ Linux kernel
```

Wine dịch hành vi API, registry, DLL và môi trường Windows cần thiết. Tương thích không
thể tuyệt đối vì Windows API rất lớn và game có thể dùng hành vi đặc biệt.

#### Proton là gì?

**Proton** là compatibility tool của Valve cho Steam Play, dựa trên Wine và nhiều thành
phần bổ sung. Mục tiêu là chạy game Windows trên Linux với cấu hình tích hợp trong Steam.

```text
Game Windows
→ Proton
├── Wine: Windows API
├── DXVK: Direct3D 9/10/11 → Vulkan
├── VKD3D-Proton: Direct3D 12 → Vulkan
├── thành phần audio/input/video
└── patch theo nhu cầu game
→ Linux driver
→ GPU
```

**DXVK** chuyển lệnh Direct3D 9/10/11 sang Vulkan.  
**VKD3D-Proton** chuyển Direct3D 12 sang Vulkan.  
**Vulkan** là API đồ họa đa nền tảng, mức thấp, được driver Linux hiện đại hỗ trợ.

Đây là một lý do lớn Linux gaming cải thiện: game DirectX không còn luôn cần bản render
engine Linux riêng; Proton có thể chuyển lời gọi sang Vulkan.

#### Steam Deck ảnh hưởng ra sao?

Steam Deck là máy chơi game của Valve chạy SteamOS dựa trên Linux. Vì Valve cần nhiều game
Windows chạy trên thiết bị Linux, hãng đầu tư vào Proton, Mesa, shader pipeline, controller
và tương thích game. Đầu tư này cũng mang lợi ích cho desktop Linux.

#### Anti-cheat là gì?

**Cheat** là phần mềm/can thiệp tạo lợi thế gian lận: nhìn xuyên tường, tự ngắm, sửa memory.
**Anti-cheat** tìm và chặn hành vi đó.

Một số anti-cheat chạy sâu trong hệ thống, kiểm tra process, driver hoặc kernel. Proton
thay đổi môi trường chạy, nên anti-cheat có thể:

- Không nhận ra môi trường.
- Coi lớp tương thích là bất thường.
- Cần nhà phát triển bật cấu hình hỗ trợ Proton/Linux.
- Dùng kernel driver Windows không thể chạy trên Linux kernel.

Do đó game offline có thể chạy tốt nhưng game competitive dùng anti-cheat lại không chạy.
Easy Anti-Cheat và BattlEye có cơ chế hỗ trợ Proton trong những trường hợp nhà phát triển
game tích hợp/bật hỗ trợ, không có nghĩa mọi game dùng chúng tự động tương thích.

#### Launcher riêng là gì?

Launcher là ứng dụng khởi động trước game để:

- Đăng nhập tài khoản.
- Cập nhật game.
- Kiểm tra license/DRM.
- Chọn server hoặc phiên bản.

Ví dụ một game mua trên Steam vẫn mở launcher của publisher. Game có thể tương thích
Proton nhưng launcher update hỏng, web login lỗi hoặc DRM không tương thích, khiến người
dùng không vào được game.

#### Driver GPU ảnh hưởng thế nào?

Driver GPU triển khai Vulkan/OpenGL và quản lý GPU. Proton/DXVK cần driver Vulkan tốt.

- AMD/Intel Linux thường dùng nhiều thành phần trong Mesa.
- NVIDIA cung cấp driver của hãng và ngày càng cải thiện hỗ trợ Linux.
- Driver quá cũ có thể thiếu extension Vulkan hoặc có bug.

**Mesa** là tập hợp implementation mã nguồn mở cho API đồ họa trên Linux, không phải tên
một GPU.

#### Shader và shader cache

**Shader** là chương trình nhỏ chạy trên GPU để xử lý vertex, pixel và tính toán đồ họa.
Game/driver phải biên dịch shader phù hợp GPU. Biên dịch lúc chơi có thể gây giật ngắn
— shader compilation stutter.

Shader cache lưu kết quả đã biên dịch để lần sau không phải làm lại. Steam/Proton và driver
có các cơ chế cache/pre-caching giúp giảm stutter trong nhiều trường hợp.

#### Mod và mod manager

**Mod** là nội dung/code do cộng đồng thay đổi game. Mod có thể:

- Thay texture.
- Thêm map.
- Sửa gameplay.
- Nạp DLL vào process.
- Dùng mod manager chỉ có Windows.

Game gốc chạy qua Proton không bảo đảm mod manager, script extender hoặc DLL injection
hoạt động. Path và case sensitivity cũng có thể làm mod lỗi.

#### Thiết bị ngoại vi

Controller phổ biến thường được Steam Input hỗ trợ tốt. Nhưng thiết bị đặc biệt như:

- Vô lăng và force feedback.
- VR headset.
- RGB controller.
- Chuột/bàn phím cần phần mềm macro của hãng.
- Thiết bị âm thanh có app cấu hình riêng.

có thể thiếu driver hoặc phần mềm cấu hình trên Linux.

#### Vì sao Linux gaming đã cải thiện?

| Thành phần | Cải thiện mang lại |
|---|---|
| Proton/Wine | Chạy nhiều game Windows mà không cần port native |
| DXVK/VKD3D-Proton | Chuyển DirectX sang Vulkan hiệu quả |
| Vulkan driver | Nền tảng render đa hãng tốt hơn |
| Steam Play | Cấu hình/cập nhật Proton thuận tiện |
| Steam Deck | Tạo động lực thương mại để Valve và studio hỗ trợ Linux |
| Mesa/driver | Tương thích và hiệu năng GPU tốt hơn |
| Shader cache | Giảm một số hiện tượng giật do biên dịch shader |
| Anti-cheat support | Một số game online có thể chạy khi studio bật hỗ trợ |

#### Vì sao Windows vẫn thường là lựa chọn game an toàn nhất?

- Game được phát triển và QA trên Windows trước.
- DirectX là nền tảng chính của nhiều game.
- Anti-cheat kernel Windows hoạt động đúng môi trường mục tiêu.
- Launcher, mod manager và peripheral software thường có bản Windows.
- Driver “day-one” và tối ưu game mới thường ưu tiên Windows.

Trước khi chuyển sang Linux, kiểm tra từng game:

```text
□ Game có native Linux hay dùng Proton?
□ Anti-cheat có được studio bật hỗ trợ?
□ Launcher/DRM hoạt động?
□ GPU và driver hỗ trợ Vulkan?
□ Mod/mod manager hoạt động?
□ Controller/VR/vô lăng được hỗ trợ?
```

### 14.2 Đồ họa 3D và CAD

Windows thường có độ tương thích rộng với:

- CAD.
- Phần mềm kỹ thuật.
- Plugin chuyên ngành.
- Driver workstation.

Linux mạnh trong:

- Render farm.
- VFX pipeline.
- Workstation kỹ thuật dùng phần mềm hỗ trợ Linux.
- Compute bằng GPU trên server.

macOS mạnh với một số quy trình:

- Video.
- Âm thanh.
- Ứng dụng tối ưu cho Apple silicon.

Lựa chọn phải dựa trên danh sách phần mềm và plugin thực tế, không chỉ dựa trên tên OS.

### 14.3 Video và âm thanh

macOS phù hợp nếu dùng Final Cut Pro hoặc Logic Pro. Windows phù hợp với nhiều cấu hình
phần cứng và phần mềm dựng phim. Linux có các công cụ sáng tạo nhưng cần kiểm tra định
dạng, codec, plugin và yêu cầu cộng tác.

---

## 15. Server, cloud và doanh nghiệp

### 15.1 Linux server

**Server** là máy hoặc chương trình cung cấp dịch vụ cho client qua mạng. Một laptop cũng
có thể tạm làm server, nhưng production server thường được cấu hình để chạy ổn định, có
giám sát, backup và kiểm soát truy cập.

#### Web server và API server

**Web server** nhận HTTP request và trả HTML, file tĩnh hoặc chuyển request tới application.
Nginx và Apache HTTP Server là ví dụ.

**API server** chạy business logic và trả JSON/Protobuf, ví dụ Spring Boot:

```text
Browser/mobile
→ HTTPS
→ Nginx
→ Spring Boot API
→ PostgreSQL
```

Linux phù hợp vì Nginx, JDK, service manager, log, network và automation được hỗ trợ tốt;
máy có thể chạy không cần GUI, giảm package không cần thiết.

#### Database server

Database server lưu và truy vấn dữ liệu: PostgreSQL, MySQL, MongoDB, Redis tùy loại.

Linux phù hợp vì:

- Filesystem và I/O có nhiều lựa chọn/tuning.
- Service chạy ổn định dưới `systemd`.
- Tự động hóa backup và replication bằng tool/script.
- Cloud/database ecosystem hỗ trợ mạnh.
- Có thể giới hạn CPU, RAM, file descriptor và quyền user.

Điều này không có nghĩa database không chạy tốt trên Windows. SQL Server đặc biệt có hệ
sinh thái mạnh trên Windows và cũng hỗ trợ Linux ở nhiều kịch bản.

#### Container host

**Container host** là máy cung cấp kernel và container runtime để chạy container.

Linux container cần Linux kernel. Trên Linux server:

```text
Container
→ container runtime
→ Linux kernel host
```

Không cần Linux VM trung gian như Docker Desktop trên macOS/Windows. Vì vậy Linux là lựa
chọn tự nhiên cho production Linux container.

#### Kubernetes

**Kubernetes** là hệ thống điều phối container. Nó:

- Chọn node để chạy pod.
- Khởi động lại workload lỗi.
- Scale số replica.
- Cung cấp service discovery/networking.
- Rolling update.
- Gắn storage và secret/config.

**Node** là máy tham gia cluster. **Pod** là đơn vị chạy cơ bản, chứa một hoặc nhiều
container. Linux node phổ biến vì phần lớn container production là Linux container và
các cơ chế namespace/cgroup nằm trong Linux kernel.

```text
Kubernetes cluster
├── Linux node A → pod Spring Boot
├── Linux node B → pod Spring Boot
└── Linux node C → monitoring
```

#### Cloud VM

**Cloud VM** là máy ảo thuê theo tài nguyên/thời gian từ AWS, Azure, Google Cloud hoặc nhà
cung cấp khác. Linux image thường:

- Nhỏ và khởi động nhanh.
- Không cần desktop GUI.
- Dễ bootstrap bằng cloud-init/shell.
- Có nhiều distro server LTS.
- Phù hợp stack web/container mã nguồn mở.
- Thường có mô hình chi phí giấy phép thuận lợi hơn Windows VM.

Windows cloud VM vẫn cần khi ứng dụng phụ thuộc Windows, IIS, Active Directory hoặc phần
mềm chỉ có bản Windows.

#### Reverse proxy và load balancer

**Reverse proxy** đứng trước application server:

```text
Client
→ reverse proxy
→ một hoặc nhiều backend
```

Nó có thể:

- Kết thúc TLS/HTTPS.
- Route `/api` và `/images` tới service khác nhau.
- Nén response.
- Giới hạn request.
- Ẩn địa chỉ backend.

**Load balancer** phân phối request giữa nhiều backend:

```text
Client
→ load balancer
├── app-1
├── app-2
└── app-3
```

Nginx, HAProxy và Envoy được dùng rộng trên Linux. Đây là hệ sinh thái và kinh nghiệm vận
hành tích lũy, không phải Windows về kỹ thuật không thể reverse proxy.

#### Monitoring, metrics, logs và alert

**Monitoring** là quan sát sức khỏe hệ thống.

- **Metric**: số đo theo thời gian, ví dụ CPU 70%, request/second, p99 latency.
- **Log**: sự kiện dạng text/structured, ví dụ request lỗi.
- **Trace**: đường đi của một request qua nhiều service.
- **Alert**: cảnh báo khi điều kiện xấu xảy ra.

Prometheus thường thu metric; Grafana hiển thị dashboard; log stack thu thập log.

Linux phù hợp vì `/proc`, cgroup, system metrics và các exporter/agent server có hệ sinh
thái trưởng thành.

#### CI/CD runner

**CI — Continuous Integration** tự động build/test khi code thay đổi.  
**CD — Continuous Delivery/Deployment** tự động chuẩn bị hoặc triển khai release.

**Runner/agent** là máy thực thi job:

```text
Developer push Git
→ CI runner Linux
→ Maven test
→ build container
→ scan
→ push image
→ deploy Kubernetes
```

Nếu production là Linux container, Linux runner giảm khác biệt về shell, path, permission
và kiến trúc build.

#### Vì sao “không cần GUI” là lợi thế?

GUI không xấu, nhưng server thường được quản trị từ xa bằng SSH/API. Không cài GUI giúp:

- Ít package hơn.
- Ít service nền hơn.
- Ít RAM/disk hơn.
- Giảm bề mặt tấn công.
- Ít thành phần phải vá.

GUI không phải yếu tố duy nhất; cấu hình, patch và quyền mới quyết định bảo mật.

#### Vì sao cấu hình text và shell hữu ích?

Cấu hình có thể lưu trong Git, review và áp dụng tự động:

```text
nginx.conf
systemd unit
Dockerfile
Kubernetes YAML
Ansible playbook
Terraform code
```

Thay vì kỹ thuật viên bấm tay trên 100 server, automation áp dụng cùng cấu hình có kiểm
soát. Windows cũng tự động hóa tốt bằng PowerShell/Desired State Configuration; Linux chỉ
có lịch sử và hệ sinh thái rất mạnh quanh text/shell.

### 15.2 Windows Server

#### Active Directory Domain Services

**Active Directory Domain Services — AD DS** là dịch vụ thư mục và danh tính tập trung.
Nó lưu user, computer, group và chính sách trong domain.

```text
Nhân viên đăng nhập laptop công ty
→ laptop hỏi domain controller
→ AD xác thực tài khoản
→ user nhận quyền vào file share/app theo group
```

**Domain Controller — DC** là Windows Server chạy AD DS và xử lý xác thực/domain data.
Doanh nghiệp dùng AD vì không muốn tạo tài khoản rời trên từng máy.

#### Group Policy

**Group Policy** áp cấu hình tập trung cho user/computer trong domain:

- Chính sách mật khẩu.
- Khóa màn hình.
- Firewall.
- Mapping network drive.
- Chặn/bật tính năng.
- Triển khai certificate hoặc cấu hình bảo mật.

Windows Server phù hợp vì AD DS và Group Policy là thành phần cốt lõi của hệ sinh thái
Microsoft, tích hợp sâu với Windows client.

#### Windows file server

File server cung cấp thư mục dùng chung qua SMB:

```text
\\fileserver\Finance
```

Nó tích hợp NTFS ACL và AD group:

```text
Finance group → đọc/ghi
Auditor group → chỉ đọc
User khác     → từ chối
```

Linux/Samba cũng cung cấp SMB và tham gia AD, nhưng tổ chức Microsoft-centric thường chọn
Windows Server để có công cụ và support đồng bộ.

#### IIS

**Internet Information Services — IIS** là web server của Microsoft. IIS phù hợp với:

- Ứng dụng ASP.NET/.NET.
- Windows Authentication.
- Certificate và quản trị Windows.
- Application Pool.
- Hạ tầng doanh nghiệp Microsoft.

Spring Boot vẫn có thể chạy trên Windows mà không cần IIS; IIS có thể làm reverse proxy
hoặc tổ chức dùng web server khác.

#### SQL Server

**Microsoft SQL Server** là hệ quản trị cơ sở dữ liệu của Microsoft. Nó chạy được trên
Windows và Linux trong các phiên bản/kịch bản hỗ trợ. Windows thường được chọn khi tổ chức
đã dùng:

- Windows Authentication/AD.
- Tool quản trị Microsoft.
- Ứng dụng vendor chứng nhận trên Windows.
- Quy trình backup/monitoring Windows hiện có.

Không nên kết luận SQL Server bắt buộc Windows trong mọi trường hợp.

#### Ứng dụng phụ thuộc Windows

Một ứng dụng có thể cần:

- Win32 API.
- COM/DCOM.
- Registry.
- Windows Service.
- NTFS ACL.
- Office automation.
- Driver/phần mềm vendor chỉ có Windows.

Khi dependency này không portable, Windows Server là lựa chọn đúng hơn việc cố ép lên
Linux.

#### PowerShell và quản trị tập trung

PowerShell cho phép quản lý service, registry, certificate, AD, IIS và remote server bằng
object/cmdlet:

```powershell
Get-Service
Get-WinEvent -LogName System
Get-ADUser -Filter *
```

Windows Server không chỉ quản trị bằng GUI; môi trường lớn dùng PowerShell và công cụ
configuration management để tự động hóa.

Doanh nghiệp có thể dùng cả Linux và Windows Server. Ví dụ:

```text
Active Directory     → Windows Server
Spring Boot API      → Linux
PostgreSQL           → Linux
Máy nhân viên        → Windows
Thiết bị thiết kế    → macOS
```

Không cần chọn duy nhất một hệ điều hành cho toàn bộ tổ chức.

### 15.3 macOS trong doanh nghiệp

macOS thường là **endpoint** — máy cuối do nhân viên trực tiếp sử dụng — hơn là server
production phổ thông.

#### MDM là gì?

**Mobile Device Management — MDM** là hệ thống quản lý thiết bị tập trung. Với Mac, MDM
có thể:

- Enroll thiết bị công ty.
- Yêu cầu mã hóa FileVault.
- Cấu hình Wi-Fi/VPN/certificate.
- Cài app và profile.
- Kiểm tra compliance.
- Khóa hoặc xóa dữ liệu doanh nghiệp khi thiết bị mất.

MDM không giống antivirus; nó là nền tảng áp cấu hình và quản trị vòng đời thiết bị.

#### Identity và Single Sign-On

**Identity provider — IdP** quản lý danh tính và đăng nhập, ví dụ tài khoản công ty.
**Single Sign-On — SSO** cho phép đăng nhập một lần rồi truy cập nhiều dịch vụ được cấp
quyền.

Mac trong doanh nghiệp cần tích hợp identity, MDM, certificate, VPN và security agent để
đáp ứng chính sách như Windows endpoint.

#### Vì sao đội iOS cần Mac?

- Xcode chỉ chạy trên macOS.
- iOS SDK và Simulator thuộc Xcode.
- Build/sign/archive cần toolchain Apple.
- CI phát hành iOS cần macOS runner và certificate/provisioning phù hợp.

#### Vì sao đội thiết kế/sáng tạo dùng Mac?

Không phải mọi designer đều cần Mac. Mac thường được chọn khi workflow dùng Final Cut
Pro, Logic Pro hoặc phần mềm/plugin được tối ưu và chuẩn hóa cho phần cứng Apple. Nếu công
ty dùng CAD/plugin Windows thì Windows có thể phù hợp hơn.

#### Vì sao Mac hiếm làm production server phổ thông?

- Phần lớn cloud VM/container ecosystem dùng Linux/Windows.
- macOS được cấp phép và phân phối gắn với phần cứng Apple.
- Ít lựa chọn server hardware/cloud instance hơn.
- Công cụ web/database có thể chạy, nhưng lợi ích so với Linux server thường không đủ.
- Doanh nghiệp vẫn cần Mac host cho build/test Apple, nhưng đó là nhiệm vụ chuyên biệt.

Use case Mac server/build host:

```text
Git push
→ CI macOS runner
→ Xcode build + test
→ code signing
→ upload TestFlight
```

### 15.4 Một doanh nghiệp thường dùng nhiều OS cùng lúc

```text
Laptop nhân viên văn phòng → Windows
Laptop mobile developer    → macOS
Spring Boot production     → Linux
Domain identity            → Windows Server/identity cloud
iOS build runner           → macOS
Kubernetes node            → Linux
```

Lựa chọn theo workload tốt hơn việc bắt mọi hệ thống dùng một OS.

### 15.5 Bảng chọn server theo yêu cầu

| Yêu cầu | Lựa chọn thường hợp lý | Lý do chính |
|---|---|---|
| Spring Boot + PostgreSQL + Docker | Linux | Container native, automation và ecosystem |
| Active Directory Domain Controller | Windows Server | AD DS là vai trò Windows Server |
| ASP.NET Core không phụ thuộc Windows | Linux hoặc Windows | .NET đa nền tảng; chọn theo vận hành |
| App dùng COM/Office automation | Windows Server | Phụ thuộc Windows API/phần mềm |
| Kubernetes worker | Linux | Linux container và kernel primitives |
| Build/ký iOS | macOS | Cần Xcode/toolchain Apple |
| File share cho Windows domain | Windows Server hoặc Samba | Chọn theo skill/support/tích hợp |
| Reverse proxy | Linux rất phổ biến | Nginx/HAProxy/Envoy ecosystem |
| SQL Server | Windows hoặc Linux | Phụ thuộc tính năng, chứng nhận và đội vận hành |
| Máy quản trị cá nhân | Bất kỳ | Cần SSH/RDP/VPN và công cụ phù hợp |

---

## 16. Ưu và nhược điểm tổng hợp

### 16.1 Linux

**Ưu điểm**

- Mã nguồn mở.
- Tùy biến rất cao.
- Rất mạnh cho server, cloud và container.
- Package manager tốt.
- Có thể chạy nhẹ mà không cần GUI.
- Nhiều công cụ phát triển và quản trị hệ thống.
- Hỗ trợ nhiều kiến trúc CPU và thiết bị.

**Nhược điểm**

- Một số phần mềm thương mại không có bản Linux.
- Có thể phải xử lý driver hoặc cấu hình thủ công.
- Nhiều distribution làm người mới khó chọn.
- Kiến thức giữa các distro không hoàn toàn giống nhau.
- Một số game và anti-cheat không hỗ trợ.

### 16.2 macOS

**Ưu điểm**

- Tích hợp phần cứng và phần mềm tốt.
- Môi trường Unix thuận tiện cho lập trình.
- Cần thiết cho phát triển ứng dụng Apple.
- Pin, trackpad và sleep/wake thường tốt trên MacBook.
- Hệ sinh thái sáng tạo nội dung mạnh.
- Tích hợp tốt với iPhone và iPad.

**Nhược điểm**

- Chỉ chạy chính thức trên máy Apple.
- Chi phí đầu vào có thể cao.
- Khả năng nâng cấp phần cứng hạn chế.
- Ít game hơn Windows.
- Tùy biến thấp hơn Linux.
- Có thể gặp vấn đề binary/container giữa ARM64 và x86-64.

### 16.3 Windows

**Ưu điểm**

- Hỗ trợ phần mềm desktop và game rộng.
- Phần cứng rất đa dạng.
- Tích hợp tốt với hệ sinh thái doanh nghiệp Microsoft.
- Visual Studio, .NET desktop và công cụ Windows mạnh.
- WSL cung cấp môi trường Linux thuận tiện.
- Driver và thiết bị ngoại vi phổ biến thường hỗ trợ Windows.

**Nhược điểm**

- Môi trường Unix không phải native; thường cần WSL.
- Khác biệt path và script có thể gây lỗi đa nền tảng.
- Chất lượng phần mềm cài sẵn khác nhau theo nhà sản xuất.
- Nhiều cơ chế cài đặt và cập nhật phần mềm.
- Cần giấy phép thương mại.

---

## 17. Chọn hệ điều hành theo use case

| Nhu cầu chính | Lựa chọn thường phù hợp | Lý do |
|---|---|---|
| Học Linux, OS, network | Linux | Truy cập và quan sát hệ thống trực tiếp |
| Backend Java/Spring Boot | Linux, macOS hoặc Windows + WSL | Cả ba dùng được; Linux gần production phổ biến |
| DevOps/Kubernetes | Linux hoặc macOS/Windows có Linux VM | Công cụ và production chủ yếu xoay quanh Linux |
| iOS development | macOS | Xcode yêu cầu macOS |
| Windows desktop development | Windows | API và công cụ Windows native |
| PC gaming | Windows | Danh mục game và driver rộng nhất |
| Video với Final Cut Pro | macOS | Final Cut Pro chỉ có trên macOS |
| Phần mềm kế toán/CAD chỉ có Windows | Windows | Tương thích ứng dụng bắt buộc |
| Server web/API | Linux | Phổ biến, nhẹ, tự động hóa tốt |
| Active Directory | Windows Server | Dịch vụ và công cụ Microsoft |
| Máy cũ cấu hình thấp | Linux nhẹ | Có distro/desktop environment dùng ít tài nguyên |
| Máy cá nhân dùng iPhone/iPad | macOS | Đồng bộ hệ sinh thái Apple |
| Tự lắp và nâng cấp PC | Windows hoặc Linux | Hỗ trợ phần cứng PC rộng |

### 17.1 Sinh viên lập trình nên chọn gì?

Nếu học Java, web và database:

- Đã có Windows: giữ Windows, cài WSL 2 và học cả PowerShell lẫn Bash.
- Đã có Mac: dùng macOS, Homebrew và Docker; chú ý kiến trúc ARM64.
- Muốn học Linux sâu: cài Ubuntu/Fedora, dual boot hoặc dùng VM.

Không cần mua máy mới chỉ để học Spring Boot nếu máy hiện tại chạy được JDK, IDE và
database.

### 17.2 Backend developer nên chọn gì?

Một lựa chọn thực dụng:

```text
Máy cá nhân: Windows + WSL, macOS hoặc Linux
CI: Linux
Container: Linux image
Production: Linux
```

Điều quan trọng là chạy test trong môi trường gần production và không đưa giả định riêng
của máy phát triển vào code.

### 17.3 DevOps/SRE nên chọn gì?

Nên thành thạo Linux dù máy cá nhân là Windows hay macOS, vì thường phải làm việc với:

- Linux server.
- SSH.
- Container.
- Kubernetes.
- Systemd.
- Filesystem permission.
- Network namespace.
- Log và process.

### 17.4 Người dùng văn phòng nên chọn gì?

Chọn theo hệ sinh thái công ty:

- Công ty dùng Microsoft 365, Active Directory và phần mềm Windows: Windows.
- Công ty hỗ trợ Mac và người dùng đã có hệ sinh thái Apple: macOS.
- Linux phù hợp nếu ứng dụng chủ yếu chạy trên web và bộ phận IT hỗ trợ Linux desktop.

### 17.5 Người làm sáng tạo nên chọn gì?

Liệt kê chính xác trước khi chọn:

1. Ứng dụng bắt buộc.
2. Plugin bắt buộc.
3. Codec và định dạng file.
4. Thiết bị capture/audio.
5. Yêu cầu GPU.
6. Cách cộng tác với đồng nghiệp.

Nếu ứng dụng bắt buộc là Final Cut Pro thì chọn Mac. Nếu công cụ hoặc plugin chỉ hỗ trợ
Windows thì chọn Windows. Linux chỉ nên chọn sau khi xác nhận toàn bộ workflow.

---

## 18. Các hiểu lầm thường gặp

### 18.1 “Linux không có giao diện đồ họa”

Sai. Linux có nhiều desktop environment như GNOME, KDE Plasma và Xfce. Server thường bỏ
GUI vì không cần, không phải vì Linux không có GUI.

### 18.2 “macOS là Linux”

Sai. macOS và Linux có nhiều lệnh giống nhau do đều có môi trường kiểu Unix, nhưng dùng
kernel, framework, API và hệ sinh thái khác nhau.

### 18.3 “Windows không dùng được terminal”

Sai. Windows có PowerShell, Command Prompt, Windows Terminal và WSL.

### 18.4 “Java chạy ở đâu cũng giống hệt nhau”

Không hoàn toàn. Bytecode chạy qua JVM, nhưng chương trình vẫn có thể bị ảnh hưởng bởi:

- Path.
- Encoding.
- Font.
- Line ending.
- Time zone.
- Case sensitivity.
- Native library.
- Quyền file.
- Kiến trúc CPU.

### 18.5 “Linux luôn miễn phí”

Phần lớn distribution có thể dùng miễn phí, nhưng hỗ trợ doanh nghiệp, chứng nhận, quản
trị và một số sản phẩm liên quan có thể tính phí.

### 18.6 “macOS không có virus”

Sai. macOS có cơ chế bảo vệ tích hợp nhưng vẫn có malware, lỗ hổng và phishing.

### 18.7 “Windows luôn chậm”

Sai. Hiệu năng phụ thuộc phần cứng, driver, ứng dụng nền và workload. Một máy Windows cấu
hình tốt có thể nhanh hơn máy Linux hoặc Mac cấu hình thấp trong cùng tác vụ.

### 18.8 “Container loại bỏ mọi khác biệt hệ điều hành”

Sai. Container giảm khác biệt user space nhưng vẫn chịu ảnh hưởng bởi:

- Kernel.
- Kiến trúc CPU.
- Filesystem mount.
- Network.
- Quyền.
- Tài nguyên host.

---

## 19. Ví dụ một dự án Spring Boot trên ba hệ điều hành

Giả sử repository có:

```text
catalog-service/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── Dockerfile
└── src/
```

### 19.1 Chạy trên Linux

```bash
cd ~/code/catalog-service
chmod +x mvnw
./mvnw spring-boot:run
```

### 19.2 Chạy trên macOS

```bash
cd ~/code/catalog-service
chmod +x mvnw
./mvnw spring-boot:run
```

Cú pháp gần Linux, nhưng cần kiểm tra JDK ARM64 nếu dùng Apple silicon:

```bash
java -version
uname -m
```

### 19.3 Chạy trên Windows PowerShell

```powershell
Set-Location C:\code\catalog-service
.\mvnw.cmd spring-boot:run
```

### 19.4 Chạy trên Windows WSL

```bash
cd ~/code/catalog-service
chmod +x mvnw
./mvnw spring-boot:run
```

Trong trường hợp này, lệnh chạy trong Linux environment của WSL, không phải trực tiếp
trong PowerShell.

### 19.5 Port

Nếu ứng dụng nghe ở port `8080`, cả ba đều có thể truy cập:

```text
http://localhost:8080
```

Kiểm tra process đang dùng port:

Linux:

```bash
ss -ltnp | grep 8080
```

macOS:

```bash
lsof -iTCP:8080 -sTCP:LISTEN
```

Windows:

```powershell
Get-NetTCPConnection -LocalPort 8080
```

### 19.6 Biến môi trường

Linux/macOS:

```bash
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
.\mvnw.cmd spring-boot:run
```

### 19.7 File executable

Trên Linux/macOS, Git lưu executable bit. Nếu `mvnw` mất quyền chạy:

```bash
chmod +x mvnw
git update-index --chmod=+x mvnw
```

Windows không sử dụng executable bit theo cùng cách trên NTFS, nên lỗi có thể chỉ xuất
hiện khi CI Linux checkout repository.

### 19.8 Line ending

Windows truyền thống dùng `CRLF`; Linux/macOS thường dùng `LF`.

Một shell script có CRLF có thể lỗi trên Linux:

```text
/bin/bash^M: bad interpreter
```

Có thể cấu hình `.gitattributes`:

```gitattributes
*.sh text eol=lf
*.bat text eol=crlf
*.cmd text eol=crlf
```

### 19.9 Docker Compose

Lệnh hiện đại nhìn chung giống nhau:

```bash
docker compose up --build
```

Nhưng cách Docker hoạt động bên dưới khác:

- Linux: container dùng Linux kernel của host.
- macOS: Docker Desktop chạy Linux VM.
- Windows: Linux container thường chạy qua WSL 2/Linux VM.

### 19.10 Production

Ví dụ pipeline:

```text
Developer dùng Windows/macOS/Linux
→ push Git
→ Linux CI chạy test
→ build Linux container image
→ deploy lên Linux Kubernetes cluster
```

Để pipeline ổn định:

- Không hard-code path theo OS.
- Chuẩn hóa line ending.
- Dùng UTF-8.
- Dùng container image đa kiến trúc nếu cần.
- Chạy test trên Linux CI.
- Cố định phiên bản JDK và dependency.

---

## 20. Kết luận ngắn gọn

Không có hệ điều hành tốt nhất cho mọi người.

**Chọn Linux nếu** ưu tiên server, cloud, container, tự động hóa, khả năng kiểm soát và học
sâu về hệ thống.

**Chọn macOS nếu** cần phát triển ứng dụng Apple, thích phần cứng/phần mềm tích hợp, làm
việc trong hệ sinh thái Apple hoặc muốn một Unix desktop được Apple quản lý đồng bộ.

**Chọn Windows nếu** cần game, phần mềm desktop/chuyên ngành phổ biến, hệ sinh thái doanh
nghiệp Microsoft hoặc nhiều lựa chọn phần cứng.

Một kỹ sư phần mềm không nhất thiết chỉ biết một hệ điều hành. Ví dụ thực tế phổ biến là:

```text
Dùng Windows hoặc macOS trên máy cá nhân
→ dùng Linux qua WSL, VM hoặc container
→ triển khai ứng dụng lên Linux server
```

Điểm cần học không chỉ là vị trí nút bấm. Cần hiểu các khác biệt có ảnh hưởng trực tiếp
đến phần mềm:

- Filesystem và đường dẫn.
- Quyền truy cập.
- Process và service.
- Shell và script.
- Kiến trúc CPU.
- Binary format.
- Container và virtual machine.
- Công cụ cài đặt, cập nhật và quản trị.

---

## 21. Nguồn tham khảo chính thức

- [The Linux Kernel Archives](https://www.kernel.org/)
- [Tài liệu Linux kernel](https://docs.kernel.org/)
- [Ubuntu Desktop](https://ubuntu.com/desktop)
- [Ubuntu Server](https://ubuntu.com/server)
- [Apple macOS](https://www.apple.com/macos/)
- [Apple Developer — macOS](https://developer.apple.com/macos/)
- [Apple Open Source — XNU](https://github.com/apple-oss-distributions/xnu)
- [Microsoft Windows](https://www.microsoft.com/windows/)
- [Microsoft Learn — Windows](https://learn.microsoft.com/windows/)
- [Microsoft Learn — Windows Subsystem for Linux](https://learn.microsoft.com/windows/wsl/)
- [Microsoft Learn — So sánh WSL 1 và WSL 2](https://learn.microsoft.com/windows/wsl/compare-versions)
- [Microsoft Learn — Làm việc giữa filesystem Windows và WSL](https://learn.microsoft.com/windows/wsl/filesystems)
- [Microsoft Learn — PowerShell](https://learn.microsoft.com/powershell/)
- [Apple Developer — Xcode Support](https://developer.apple.com/support/xcode/)
- [The Open Group — UNIX](https://www.opengroup.org/membership/forums/platform/unix)
- [ValveSoftware — Proton](https://github.com/ValveSoftware/Proton)
