# Docker Volume và Bind Mount: từ cơ bản đến nâng cao

Tài liệu này giải thích cách lưu trữ dữ liệu trong Docker bằng **volume** và **bind mount**. Nội dung đi từ kiến thức nền tảng đến các tình huống thực tế như Docker Compose, sao lưu dữ liệu, phân quyền, xử lý lỗi và lựa chọn cách lưu trữ phù hợp.

Mục tiêu sau khi đọc:

- Hiểu vì sao dữ liệu bên trong container có thể bị mất.
- Phân biệt volume, bind mount và `tmpfs`.
- Biết cách tạo, sử dụng, kiểm tra và xóa volume.
- Biết cách mount thư mục hoặc file từ máy host vào container.
- Sử dụng volume và bind mount trong Docker Compose.
- Sao lưu, phục hồi và di chuyển dữ liệu.
- Hiểu các lỗi thường gặp về đường dẫn và quyền truy cập.
- Biết nên dùng giải pháp nào trong môi trường phát triển và production.

---

## 1. Vấn đề dữ liệu bên trong container

Mỗi container có một lớp file system riêng để chương trình đọc và ghi dữ liệu.

Ví dụ, chạy một container Ubuntu:

```bash
docker run --name demo-container ubuntu \
  sh -c "echo 'Xin chào Docker' > /data.txt"
```

File `/data.txt` đang nằm trong lớp ghi của container `demo-container`.

Có thể kiểm tra:

```bash
docker exec demo-container cat /data.txt
```

Nếu chỉ dừng rồi chạy lại cùng container, dữ liệu vẫn còn:

```bash
docker stop demo-container
docker start demo-container
docker exec demo-container cat /data.txt
```

Nhưng nếu xóa container rồi tạo một container mới, file sẽ không còn:

```bash
docker rm -f demo-container
docker run --name demo-container ubuntu cat /data.txt
```

Lệnh trên báo lỗi vì container mới có một lớp file system mới.

### 1.1 Điều quan trọng cần nhớ

```text
Dừng container                         -> dữ liệu trong container vẫn còn
Xóa container                          -> lớp ghi của container bị xóa
Tạo container mới từ cùng một image   -> không lấy lại dữ liệu cũ
```

Container nên được xem là thành phần **có thể tạo lại và thay thế**. Dữ liệu quan trọng không nên phụ thuộc vào vòng đời của container.

Các ví dụ dữ liệu cần được lưu bên ngoài lớp ghi của container:

- Dữ liệu PostgreSQL, MySQL, MongoDB hoặc Redis.
- File người dùng tải lên.
- File cấu hình cần chỉnh từ bên ngoài.
- Source code đang phát triển.
- Chứng chỉ và khóa bảo mật.
- Dữ liệu do ứng dụng sinh ra cần được giữ lâu dài.

Docker cung cấp cơ chế **mount** để gắn một vùng lưu trữ từ bên ngoài vào một đường dẫn bên trong container.

---

## 2. Mount là gì?

Mount có thể hiểu đơn giản là:

> Làm cho một vùng lưu trữ bên ngoài xuất hiện tại một thư mục bên trong container.

Ví dụ:

```text
Vùng lưu trữ bên ngoài
          |
          | mount
          v
Container: /var/lib/postgresql/data
```

Khi PostgreSQL ghi file vào `/var/lib/postgresql/data`, dữ liệu thật được ghi vào vùng lưu trữ đã mount. Xóa container không nhất thiết xóa vùng lưu trữ đó.

Docker thường sử dụng ba kiểu mount:

1. **Volume**: Docker tạo và quản lý vùng lưu trữ.
2. **Bind mount**: sử dụng trực tiếp file hoặc thư mục có sẵn trên máy host.
3. **tmpfs mount**: dữ liệu chỉ nằm trong bộ nhớ của host và mất khi container dừng.

Tài liệu này tập trung vào volume và bind mount.

---

## 3. So sánh nhanh volume và bind mount

| Tiêu chí | Volume | Bind mount |
|---|---|---|
| Nơi lưu | Do Docker quản lý | Đường dẫn cụ thể trên host |
| Cần biết đường dẫn thật trên host | Không | Có |
| Dễ di chuyển giữa các máy | Tốt hơn | Phụ thuộc cấu trúc thư mục host |
| Phù hợp lưu dữ liệu database | Rất phù hợp | Có thể dùng nhưng thường không ưu tiên |
| Phù hợp chia sẻ source code lúc phát triển | Không thuận tiện bằng bind mount | Rất phù hợp |
| Docker CLI quản lý được | Có | Không quản lý nội dung thư mục host |
| Nguy cơ ghi đè file trên host | Thấp hơn | Cao hơn nếu mount nhầm |
| Backup | Qua container tạm hoặc driver | Dùng công cụ file system thông thường |
| Production | Thường được ưu tiên cho dữ liệu ứng dụng | Dùng khi cần file host cụ thể |

Quy tắc chọn nhanh:

```text
Dữ liệu do ứng dụng tạo ra và cần giữ lâu dài -> Volume
Source code hoặc file cần sửa trực tiếp từ host -> Bind mount
Dữ liệu tạm, nhạy cảm, không cần lưu lại       -> tmpfs
```

---

# Phần I: Docker Volume

## 4. Volume là gì?

Volume là vùng lưu trữ bền vững do Docker quản lý.

Khi tạo volume:

```bash
docker volume create app-data
```

Docker tạo một vùng lưu trữ có tên `app-data`. Ta không cần tự chọn đường dẫn vật lý cho nó.

Có thể hình dung:

```text
Docker host
|
|-- Docker quản lý volume: app-data
|          |
|          +---- dữ liệu thật
|
+-- Container
           |
           +---- /app/data  <--- mount app-data vào đây
```

Volume tồn tại độc lập với container:

```text
Tạo volume -> tạo container -> ghi dữ liệu -> xóa container
    |                                             |
    +---------------- volume vẫn còn -------------+
```

## 5. Các lệnh quản lý volume cơ bản

### 5.1 Liệt kê volume

```bash
docker volume ls
```

Ví dụ kết quả:

```text
DRIVER    VOLUME NAME
local     app-data
local     postgres-data
```

### 5.2 Tạo named volume

```bash
docker volume create app-data
```

`app-data` được gọi là **named volume**, nghĩa là volume có tên rõ ràng do người dùng đặt.

### 5.3 Xem thông tin chi tiết

```bash
docker volume inspect app-data
```

Thông tin thường gồm:

- Tên volume.
- Driver đang dùng.
- Thời điểm tạo.
- Mount point trên Docker host.
- Label.
- Các tùy chọn của driver.

Ví dụ rút gọn:

```json
[
  {
    "Name": "app-data",
    "Driver": "local",
    "Mountpoint": "/var/lib/docker/volumes/app-data/_data"
  }
]
```

Không nên sửa trực tiếp dữ liệu tại `Mountpoint`. Hãy truy cập dữ liệu thông qua container để tránh lỗi quyền, dữ liệu hỏng hoặc phụ thuộc vào cấu trúc nội bộ của Docker.

Trên Docker Desktop cho Windows và macOS, đường dẫn này thường nằm trong máy ảo Linux của Docker, không phải một thư mục Windows hoặc macOS thông thường.

### 5.4 Xóa một volume

```bash
docker volume rm app-data
```

Docker không cho xóa volume đang được container sử dụng.

### 5.5 Xóa các volume không còn được sử dụng

```bash
docker volume prune
```

Lệnh này có thể xóa dữ liệu không thể khôi phục. Luôn kiểm tra trước:

```bash
docker volume ls
docker ps -a --filter volume=app-data
```

## 6. Gắn volume vào container

Có hai cú pháp phổ biến:

- `--mount`: dài hơn nhưng rõ ràng, dễ đọc và ít gây nhầm.
- `-v` hoặc `--volume`: ngắn gọn và được sử dụng rộng rãi.

### 6.1 Sử dụng `--mount`

```bash
docker run -d \
  --name nginx-volume-demo \
  --mount type=volume,source=web-data,target=/usr/share/nginx/html \
  nginx
```

Ý nghĩa:

- `type=volume`: kiểu mount là volume.
- `source=web-data`: tên volume.
- `target=/usr/share/nginx/html`: vị trí volume xuất hiện trong container.

Nếu `web-data` chưa tồn tại, Docker tự tạo volume.

### 6.2 Sử dụng `-v`

```bash
docker run -d \
  --name nginx-volume-demo \
  -v web-data:/usr/share/nginx/html \
  nginx
```

Cấu trúc:

```text
-v <tên-volume>:<đường-dẫn-trong-container>
```

### 6.3 Kiểm tra mount của container

```bash
docker inspect nginx-volume-demo
```

Muốn chỉ lấy phần mount:

```bash
docker inspect \
  --format '{{json .Mounts}}' \
  nginx-volume-demo
```

## 7. Thử nghiệm tính bền vững của volume

Tạo volume:

```bash
docker volume create demo-data
```

Tạo container và ghi dữ liệu:

```bash
docker run --rm \
  --mount type=volume,source=demo-data,target=/data \
  alpine \
  sh -c "echo 'Dữ liệu vẫn còn' > /data/message.txt"
```

Container có `--rm`, vì vậy nó bị xóa ngay sau khi chạy xong. Volume vẫn tồn tại.

Dùng container khác để đọc:

```bash
docker run --rm \
  --mount type=volume,source=demo-data,target=/data \
  alpine \
  cat /data/message.txt
```

Kết quả:

```text
Dữ liệu vẫn còn
```

Điều này chứng minh volume không phụ thuộc vào một container cụ thể.

## 8. Named volume và anonymous volume

### 8.1 Named volume

Named volume có tên do người dùng đặt:

```bash
docker run -v postgres-data:/var/lib/postgresql/data postgres
```

Ưu điểm:

- Dễ nhận biết mục đích.
- Dễ dùng lại.
- Dễ backup và restore.
- Dễ khai báo trong Docker Compose.

### 8.2 Anonymous volume

Anonymous volume không có tên dễ đọc do người dùng đặt:

```bash
docker run -v /var/lib/postgresql/data postgres
```

Docker tự tạo một tên ID dài:

```text
f24f73b0a34c7d0f...
```

Anonymous volume vẫn bền vững sau khi container bị xóa, trừ khi container được chạy với `--rm` hoặc bị xóa bằng tùy chọn xóa volume.

Nhược điểm:

- Khó biết volume thuộc ứng dụng nào.
- Dễ tạo volume rác.
- Khó quản lý và backup.

Trong phần lớn trường hợp, nên dùng named volume.

## 9. Chế độ chỉ đọc

Một container có thể chỉ đọc dữ liệu từ volume:

```bash
docker run --rm \
  --mount type=volume,source=app-config,target=/config,readonly \
  alpine \
  cat /config/application.yml
```

Cú pháp ngắn:

```bash
docker run --rm \
  -v app-config:/config:ro \
  alpine \
  cat /config/application.yml
```

`ro` là viết tắt của **read-only**.

Chế độ chỉ đọc hữu ích khi:

- Container chỉ cần đọc cấu hình.
- Nhiều container đọc cùng một bộ dữ liệu.
- Muốn giảm nguy cơ ứng dụng vô tình sửa hoặc xóa dữ liệu.

## 10. Nhiều container dùng chung một volume

Container ghi dữ liệu:

```bash
docker run -d \
  --name writer \
  -v shared-data:/data \
  alpine \
  sh -c "while true; do date >> /data/time.log; sleep 5; done"
```

Container đọc dữ liệu:

```bash
docker run --rm \
  -v shared-data:/data:ro \
  alpine \
  cat /data/time.log
```

Hai container nhìn thấy cùng dữ liệu.

Tuy nhiên, Docker chỉ cung cấp vùng lưu trữ chung. Docker không tự xử lý:

- Khóa file.
- Xung đột khi nhiều tiến trình cùng ghi.
- Transaction.
- Đồng bộ dữ liệu cấp ứng dụng.

Ứng dụng hoặc loại file system phải hỗ trợ việc truy cập đồng thời.

## 11. Ví dụ volume với PostgreSQL

```bash
docker run -d \
  --name postgres-demo \
  -e POSTGRES_USER=app \
  -e POSTGRES_PASSWORD=secret \
  -e POSTGRES_DB=appdb \
  -v postgres-data:/var/lib/postgresql/data \
  postgres:17
```

Đường dẫn `/var/lib/postgresql/data` là nơi image PostgreSQL lưu dữ liệu.

Kiểm tra:

```bash
docker volume inspect postgres-data
docker inspect postgres-demo --format '{{json .Mounts}}'
```

Xóa container nhưng giữ dữ liệu:

```bash
docker rm -f postgres-demo
```

Tạo lại container với cùng volume:

```bash
docker run -d \
  --name postgres-demo \
  -e POSTGRES_USER=app \
  -e POSTGRES_PASSWORD=secret \
  -e POSTGRES_DB=appdb \
  -v postgres-data:/var/lib/postgresql/data \
  postgres:17
```

Database cũ vẫn nằm trong `postgres-data`.

> Không nên mount dữ liệu của hai PostgreSQL server đang chạy cùng lúc vào cùng một data directory. Điều đó có thể làm hỏng dữ liệu.

---

# Phần II: Bind mount

## 12. Bind mount là gì?

Bind mount gắn trực tiếp một file hoặc thư mục trên máy host vào container.

Ví dụ:

```text
Host: D:\projects\my-app\src
                  |
                  | bind mount
                  v
Container: /app/src
```

Khi sửa file trong thư mục host, container thấy thay đổi gần như ngay lập tức. Khi container sửa file, thay đổi cũng xuất hiện trên host nếu mount cho phép ghi.

Bind mount đặc biệt phù hợp với:

- Chia sẻ source code khi lập trình.
- Mount file cấu hình.
- Mount file log để đọc bằng công cụ trên host.
- Cấp cho container quyền truy cập một file hoặc thư mục cụ thể của host.

## 13. Sử dụng bind mount với `--mount`

Linux hoặc macOS:

```bash
docker run --rm \
  --mount type=bind,source="$(pwd)",target=/workspace \
  alpine \
  ls -la /workspace
```

PowerShell:

```powershell
docker run --rm `
  --mount "type=bind,source=$($PWD.Path),target=/workspace" `
  alpine `
  ls -la /workspace
```

Ý nghĩa:

- `type=bind`: kiểu mount là bind mount.
- `source`: đường dẫn tồn tại trên host.
- `target`: đường dẫn tuyệt đối trong container.

Với `--mount`, đường dẫn nguồn phải tồn tại. Nếu không, Docker báo lỗi. Hành vi này giúp phát hiện sớm lỗi gõ sai đường dẫn.

## 14. Sử dụng bind mount với `-v`

Linux hoặc macOS:

```bash
docker run --rm \
  -v "$(pwd):/workspace" \
  alpine \
  ls -la /workspace
```

PowerShell:

```powershell
docker run --rm `
  -v "${PWD}:/workspace" `
  alpine `
  ls -la /workspace
```

Cấu trúc:

```text
-v <đường-dẫn-host>:<đường-dẫn-container>
```

Một điểm khác biệt quan trọng:

- `--mount` báo lỗi nếu thư mục nguồn không tồn tại.
- `-v` có thể tự tạo đường dẫn nguồn bị thiếu dưới dạng thư mục.

Vì vậy, với bind mount, `--mount` thường an toàn và rõ ràng hơn.

## 15. Mount một file cấu hình

Giả sử host có file:

```text
./config/nginx.conf
```

Có thể mount file này vào container:

```bash
docker run --rm \
  --mount type=bind,source="$(pwd)/config/nginx.conf",target=/etc/nginx/nginx.conf,readonly \
  nginx \
  nginx -t
```

PowerShell:

```powershell
docker run --rm `
  --mount "type=bind,source=$($PWD.Path)\config\nginx.conf,target=/etc/nginx/nginx.conf,readonly" `
  nginx `
  nginx -t
```

Lưu ý:

- Source phải là file nếu target được dùng như file.
- Nên dùng `readonly` nếu container không cần sửa cấu hình.
- Trên Windows, nên dùng đường dẫn tuyệt đối để tránh lỗi phân tích ký tự ổ đĩa.

## 16. Chế độ chỉ đọc của bind mount

```bash
docker run --rm \
  --mount type=bind,source="$(pwd)/config",target=/app/config,readonly \
  alpine \
  sh -c "echo test > /app/config/new-file.txt"
```

Lệnh ghi file sẽ thất bại vì mount chỉ đọc.

Cú pháp ngắn:

```bash
docker run --rm \
  -v "$(pwd)/config:/app/config:ro" \
  alpine
```

Nên dùng read-only theo mặc định nếu container chỉ cần đọc. Đây là một biện pháp bảo vệ đơn giản nhưng hiệu quả.

## 17. Dữ liệu có sẵn tại target bị che khuất

Giả sử image đã có:

```text
/app/config/default.yml
```

Khi mount một thư mục host vào `/app/config`:

```bash
docker run \
  --mount type=bind,source="$(pwd)/config",target=/app/config \
  my-image
```

Nội dung cũ trong `/app/config` không bị xóa, nhưng bị mount che khuất nên container không nhìn thấy nó.

```text
Trước khi mount:
/app/config/default.yml  <- nhìn thấy

Sau khi mount:
/app/config              <- hiển thị nội dung thư mục host
Nội dung có sẵn trong image bị che khuất
```

Muốn thấy lại nội dung cũ, cần tạo container không có mount hoặc mount vào vị trí khác.

Đây là nguyên nhân phổ biến của lỗi:

- Mount `./app` vào `/app` làm che mất file đã được build trong image.
- Mount thư mục trống vào thư mục cấu hình làm ứng dụng báo thiếu file.
- Mount nhầm vào `/usr`, `/etc` hoặc thư mục hệ thống làm container không khởi động được.

## 18. Ví dụ bind mount khi phát triển Spring Boot

Giả sử source code nằm tại thư mục hiện tại:

```powershell
docker run --rm -it `
  --mount "type=bind,source=$($PWD.Path),target=/workspace" `
  --workdir /workspace `
  maven:3.9-eclipse-temurin-21 `
  mvn spring-boot:run
```

Ý nghĩa:

- Source code trên host xuất hiện tại `/workspace`.
- Maven chạy trong container.
- Chỉnh source code trên host thì container đọc được nội dung mới.
- Không cần cài Maven trực tiếp trên host.

Có thể dùng named volume riêng để cache Maven:

```powershell
docker run --rm -it `
  --mount "type=bind,source=$($PWD.Path),target=/workspace" `
  --mount "type=volume,source=maven-cache,target=/root/.m2" `
  --workdir /workspace `
  maven:3.9-eclipse-temurin-21 `
  mvn spring-boot:run
```

Đây là cách kết hợp hai loại mount:

- Bind mount cho source code cần sửa thường xuyên.
- Volume cho cache dependency do công cụ tạo ra.

---

# Phần III: Volume và bind mount trong Docker Compose

## 19. Named volume trong Compose

Ví dụ PostgreSQL:

```yaml
services:
  postgres:
    image: postgres:17
    environment:
      POSTGRES_USER: app
      POSTGRES_PASSWORD: secret
      POSTGRES_DB: appdb
    volumes:
      - postgres-data:/var/lib/postgresql/data

volumes:
  postgres-data:
```

Chạy:

```bash
docker compose up -d
```

Compose thường tạo tên volume thật theo cấu trúc:

```text
<tên-project>_<tên-volume>
```

Ví dụ project là `shop`:

```text
shop_postgres-data
```

Kiểm tra:

```bash
docker compose config
docker volume ls
docker compose ps
```

## 20. Bind mount trong Compose

Cú pháp ngắn:

```yaml
services:
  app:
    image: eclipse-temurin:21
    working_dir: /workspace
    volumes:
      - ./:/workspace
    command: ./mvnw spring-boot:run
```

Đường dẫn `./` được tính tương đối từ thư mục chứa file Compose chính.

Cú pháp dài, rõ nghĩa hơn:

```yaml
services:
  app:
    image: eclipse-temurin:21
    working_dir: /workspace
    volumes:
      - type: bind
        source: .
        target: /workspace
        read_only: false
    command: ./mvnw spring-boot:run
```

## 21. Kết hợp bind mount và volume trong Compose

Ví dụ ứng dụng Spring Boot và PostgreSQL:

```yaml
services:
  app:
    image: maven:3.9-eclipse-temurin-21
    working_dir: /workspace
    command: mvn spring-boot:run
    volumes:
      - type: bind
        source: .
        target: /workspace
      - type: volume
        source: maven-cache
        target: /root/.m2
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/appdb
      SPRING_DATASOURCE_USERNAME: app
      SPRING_DATASOURCE_PASSWORD: secret
    depends_on:
      - postgres

  postgres:
    image: postgres:17
    environment:
      POSTGRES_USER: app
      POSTGRES_PASSWORD: secret
      POSTGRES_DB: appdb
    volumes:
      - type: volume
        source: postgres-data
        target: /var/lib/postgresql/data

volumes:
  maven-cache:
  postgres-data:
```

Vai trò của từng mount:

| Mount | Loại | Mục đích |
|---|---|---|
| `.` → `/workspace` | Bind mount | Chia sẻ source code |
| `maven-cache` → `/root/.m2` | Volume | Cache dependency Maven |
| `postgres-data` → data directory | Volume | Giữ dữ liệu PostgreSQL |

## 22. Vòng đời volume với Docker Compose

Dừng và xóa container, network nhưng giữ volume:

```bash
docker compose down
```

Dữ liệu trong named volume vẫn còn.

Xóa cả volume được khai báo bởi Compose:

```bash
docker compose down --volumes
```

Hoặc:

```bash
docker compose down -v
```

Đây là lệnh nguy hiểm nếu volume chứa database.

Khởi động lại:

```bash
docker compose up -d
```

Nếu volume chưa bị xóa, Compose tiếp tục dùng dữ liệu cũ.

## 23. External volume

Đôi khi volume được tạo và quản lý ngoài project Compose:

```bash
docker volume create company-postgres-data
```

Khai báo:

```yaml
services:
  postgres:
    image: postgres:17
    volumes:
      - company-postgres-data:/var/lib/postgresql/data

volumes:
  company-postgres-data:
    external: true
```

Với `external: true`:

- Compose không tự tạo volume nếu nó chưa tồn tại.
- Compose không coi volume này thuộc vòng đời project.
- Phù hợp khi nhiều project hoặc hệ thống triển khai cùng sử dụng một volume đã được quản lý riêng.

Có thể ánh xạ tên logic sang tên thật:

```yaml
volumes:
  postgres-data:
    external: true
    name: company-postgres-data
```

Service vẫn dùng tên logic:

```yaml
services:
  postgres:
    volumes:
      - postgres-data:/var/lib/postgresql/data
```

## 24. Tên volume cố định trong Compose

```yaml
volumes:
  postgres-data:
    name: company-postgres-data
```

Compose sử dụng chính xác tên `company-postgres-data`, không thêm prefix project.

Khác với `external: true`, nếu volume chưa tồn tại thì Compose có thể tạo nó.

---

# Phần IV: Kiến thức nâng cao

## 25. Volume driver

Mỗi volume sử dụng một driver. Driver mặc định là `local`:

```bash
docker volume create --driver local app-data
```

Kiểm tra:

```bash
docker volume inspect app-data
```

Volume driver cho phép Docker dùng nhiều hệ thống lưu trữ khác nhau, ví dụ:

- Local disk.
- NFS.
- Hệ thống lưu trữ cloud.
- Storage plugin của bên thứ ba.

Ứng dụng vẫn mount dữ liệu vào một đường dẫn trong container, còn driver quyết định dữ liệu thật nằm ở đâu và được kết nối như thế nào.

## 26. Ví dụ volume dùng NFS

Ví dụ minh họa trên Linux:

```bash
docker volume create \
  --driver local \
  --opt type=nfs \
  --opt o=addr=192.168.1.100,rw,nfsvers=4 \
  --opt device=:/exports/app-data \
  app-nfs-data
```

Sau đó sử dụng như volume thông thường:

```bash
docker run --rm \
  -v app-nfs-data:/data \
  alpine \
  ls -la /data
```

Compose:

```yaml
services:
  app:
    image: alpine
    command: sh -c "ls -la /data && sleep infinity"
    volumes:
      - app-data:/data

volumes:
  app-data:
    driver: local
    driver_opts:
      type: nfs
      o: addr=192.168.1.100,rw,nfsvers=4
      device: ":/exports/app-data"
```

Trong thực tế cần kiểm tra:

- Kết nối mạng từ Docker host đến NFS server.
- Firewall.
- Quyền export trên NFS server.
- UID và GID.
- Khả năng khóa file.
- Hiệu năng và độ ổn định.

## 27. Quyền truy cập: UID và GID

Trên Linux, quyền file dựa vào:

- UID: mã người dùng.
- GID: mã nhóm.
- Quyền đọc, ghi và thực thi.

Container không tạo ra một hệ thống quyền hoàn toàn tách biệt. Một process có UID `1000` trong container có thể tạo file mang UID `1000` trên vùng lưu trữ được mount.

Ví dụ:

```bash
docker run --rm \
  --mount type=bind,source="$(pwd)/data",target=/data \
  --user 1000:1000 \
  alpine \
  sh -c "echo hello > /data/message.txt"
```

Nếu thư mục host không cho UID `1000` ghi, lệnh sẽ báo:

```text
Permission denied
```

### 27.1 Cách xử lý thường dùng

1. Chạy container bằng UID/GID phù hợp:

```bash
docker run --user "$(id -u):$(id -g)" ...
```

2. Đặt owner phù hợp cho thư mục dữ liệu trên host.

3. Trong image, tạo user ứng dụng cố định và cấp quyền cho thư mục cần ghi:

```dockerfile
RUN addgroup --system app \
    && adduser --system --ingroup app app \
    && mkdir -p /app/data \
    && chown -R app:app /app

USER app
```

4. Dùng entrypoint để điều chỉnh quyền trước khi hạ xuống user không phải root. Cần tránh `chown -R` trên volume lớn mỗi lần khởi động vì rất chậm.

Không nên dùng `chmod 777` như giải pháp mặc định. Cách này mở quyền quá rộng và thường chỉ che giấu việc UID/GID đang cấu hình sai.

## 28. SELinux trên một số hệ Linux

Trên hệ thống bật SELinux như Fedora, RHEL hoặc CentOS, quyền Unix đúng vẫn có thể chưa đủ. Bind mount có thể cần relabel:

```bash
docker run -v "$(pwd)/data:/data:Z" my-image
```

Hoặc:

```bash
docker run -v "$(pwd)/data:/data:z" my-image
```

Khác biệt khái quát:

- `:Z`: gán nhãn riêng cho một container.
- `:z`: cho phép nhiều container chia sẻ nội dung.

Không thêm `:Z` hoặc `:z` một cách tùy tiện vào thư mục hệ thống quan trọng vì việc relabel có thể ảnh hưởng đến host.

## 29. Bind propagation

Bind propagation quyết định các mount được tạo bên trong một mount có được truyền giữa host và container hay không.

Các chế độ thường gặp:

- `rprivate`: mặc định, không truyền mount con.
- `rshared`: mount con có thể truyền hai chiều.
- `rslave`: thường nhận mount từ phía nguồn nhưng không truyền ngược đầy đủ.

Ví dụ:

```bash
docker run \
  --mount type=bind,source=/mnt,target=/mnt,bind-propagation=rshared \
  my-image
```

Đây là chủ đề nâng cao, thường chỉ cần khi:

- Chạy Docker-in-Docker hoặc công cụ quản lý container.
- Container quản lý mount.
- Chạy một số agent hệ thống, công cụ backup hoặc storage plugin.

Ứng dụng web thông thường gần như không cần thay đổi bind propagation.

## 30. `VOLUME` trong Dockerfile

Dockerfile có thể khai báo:

```dockerfile
VOLUME ["/app/data"]
```

Lệnh này đánh dấu `/app/data` là vị trí dữ liệu nên được lưu bên ngoài lớp ghi của container.

Nếu người chạy container không chỉ định named volume hoặc bind mount, Docker có thể tạo anonymous volume cho vị trí này.

Điều cần lưu ý:

- `VOLUME` không đặt được tên volume.
- `VOLUME` không chỉ định được đường dẫn host.
- `VOLUME` không thay thế cấu hình runtime hoặc Compose.
- Dễ tạo anonymous volume khó quản lý.

Với ứng dụng nội bộ, nhiều đội chọn mô tả đường dẫn cần lưu trong tài liệu và khai báo mount rõ ràng trong Compose hoặc hệ thống triển khai thay vì phụ thuộc quá nhiều vào `VOLUME`.

## 31. Copy dữ liệu ban đầu vào volume mới

Khi mount một **volume trống** vào một thư mục trong image đã có dữ liệu, Docker có thể sao chép dữ liệu có sẵn từ thư mục đó vào volume.

Ví dụ image có:

```text
/app/data/default.json
```

Lần đầu mount volume trống:

```bash
docker run -v app-data:/app/data my-image
```

`default.json` có thể được copy vào `app-data`.

Có thể tắt hành vi copy bằng `volume-nocopy` với cú pháp `--mount`:

```bash
docker run \
  --mount type=volume,source=app-data,target=/app/data,volume-nocopy \
  my-image
```

Điểm này khác bind mount. Bind mount thường che khuất ngay nội dung có sẵn tại target, không tự copy dữ liệu từ image sang thư mục host.

## 32. Hiệu năng trên Docker Desktop

Trên Linux, bind mount dùng trực tiếp file system của host nên thường có hiệu năng tốt.

Trên Docker Desktop cho Windows hoặc macOS:

- Linux container chạy trong một Linux VM.
- Bind mount phải chia sẻ file giữa host và VM.
- Tác vụ có rất nhiều file nhỏ có thể chậm hơn, ví dụ `node_modules`, Maven cache hoặc Gradle cache.

Gợi ý:

- Bind mount source code cần chỉnh sửa.
- Đặt cache và dependency vào named volume.
- Không bind mount toàn bộ thư mục nếu ứng dụng chỉ cần vài file.
- Với WSL 2, source code nằm trong file system Linux thường nhanh hơn source nằm trên ổ Windows đối với công cụ chạy trong Linux.

Ví dụ:

```yaml
services:
  app:
    volumes:
      - .:/workspace
      - maven-cache:/root/.m2

volumes:
  maven-cache:
```

## 33. Không lưu secret tùy tiện trong volume hoặc bind mount

Mount file secret tốt hơn ghi secret trực tiếp vào image, nhưng vẫn cần cẩn thận:

- File trên host phải có quyền đọc hạn chế.
- Không commit secret vào Git.
- Không mount cả thư mục chứa nhiều thông tin nhạy cảm nếu container chỉ cần một file.
- Nên dùng secret manager hoặc cơ chế secrets của nền tảng triển khai trong production.
- Mount secret ở chế độ chỉ đọc.

Ví dụ tối thiểu:

```bash
docker run --rm \
  --mount type=bind,source="$(pwd)/secrets/db-password",target=/run/secrets/db-password,readonly \
  my-app
```

Ứng dụng đọc secret từ:

```text
/run/secrets/db-password
```

---

# Phần V: Backup, restore và di chuyển dữ liệu

## 34. Backup named volume bằng container tạm

Giả sử cần backup volume `postgres-data` vào thư mục hiện tại.

Linux hoặc macOS:

```bash
docker run --rm \
  --mount type=volume,source=postgres-data,target=/data,readonly \
  --mount type=bind,source="$(pwd)",target=/backup \
  alpine \
  tar -czf /backup/postgres-data.tar.gz -C /data .
```

PowerShell:

```powershell
docker run --rm `
  --mount type=volume,source=postgres-data,target=/data,readonly `
  --mount "type=bind,source=$($PWD.Path),target=/backup" `
  alpine `
  tar -czf /backup/postgres-data.tar.gz -C /data .
```

Container tạm thực hiện:

1. Mount volume cần backup vào `/data`.
2. Mount thư mục host vào `/backup`.
3. Nén nội dung `/data`.
4. Ghi file backup ra host.

## 35. Restore named volume

Tạo volume mới:

```bash
docker volume create postgres-data-restored
```

Linux hoặc macOS:

```bash
docker run --rm \
  --mount type=volume,source=postgres-data-restored,target=/data \
  --mount type=bind,source="$(pwd)",target=/backup,readonly \
  alpine \
  tar -xzf /backup/postgres-data.tar.gz -C /data
```

PowerShell:

```powershell
docker run --rm `
  --mount type=volume,source=postgres-data-restored,target=/data `
  --mount "type=bind,source=$($PWD.Path),target=/backup,readonly" `
  alpine `
  tar -xzf /backup/postgres-data.tar.gz -C /data
```

Kiểm tra:

```bash
docker run --rm \
  -v postgres-data-restored:/data:ro \
  alpine \
  ls -la /data
```

## 36. Backup database đúng cách

Copy trực tiếp file của database đang chạy có thể tạo bản backup không nhất quán. Database có thể đang ghi dở hoặc giữ dữ liệu trong bộ nhớ.

Với PostgreSQL, nên ưu tiên công cụ logic như `pg_dump`:

```bash
docker exec postgres-demo \
  pg_dump -U app -d appdb > appdb.sql
```

PowerShell:

```powershell
docker exec postgres-demo `
  pg_dump -U app -d appdb |
  Set-Content -Encoding utf8 appdb.sql
```

Phục hồi:

```bash
cat appdb.sql | docker exec -i postgres-demo psql -U app -d appdb
```

Hoặc PowerShell:

```powershell
Get-Content -Raw appdb.sql |
  docker exec -i postgres-demo psql -U app -d appdb
```

Lựa chọn backup phụ thuộc database:

- PostgreSQL: `pg_dump`, `pg_dumpall`, base backup.
- MySQL: `mysqldump`, MySQL Shell dump.
- MongoDB: `mongodump`.
- Redis: RDB hoặc AOF theo cơ chế của Redis.

Backup volume cấp file system vẫn hữu ích, nhưng nên dừng database hoặc dùng cơ chế snapshot nhất quán nếu database yêu cầu.

## 37. Di chuyển volume sang máy khác

Quy trình đơn giản:

```text
Máy A:
volume -> tạo file backup -> chuyển file

Máy B:
tạo volume mới -> restore file backup -> chạy container
```

Cần đảm bảo:

- Cùng loại ứng dụng hoặc phiên bản tương thích.
- UID/GID và quyền file phù hợp.
- Kiến trúc CPU và định dạng dữ liệu tương thích.
- Không restore dữ liệu database sang phiên bản không hỗ trợ.
- Kiểm tra checksum của file backup.

Ví dụ tạo checksum:

```bash
sha256sum postgres-data.tar.gz
```

PowerShell:

```powershell
Get-FileHash .\postgres-data.tar.gz -Algorithm SHA256
```

---

# Phần VI: Xử lý lỗi

## 38. Container báo `Permission denied`

Kiểm tra user trong container:

```bash
docker exec my-container id
```

Kiểm tra quyền thư mục:

```bash
docker exec my-container ls -ld /app/data
```

Kiểm tra cấu hình mount:

```bash
docker inspect my-container --format '{{json .Mounts}}'
```

Nguyên nhân thường gặp:

- UID/GID trong container không có quyền ghi.
- Mount đang ở chế độ read-only.
- SELinux chặn truy cập.
- Docker Desktop chưa được phép truy cập ổ đĩa hoặc thư mục.
- File đang bị chương trình khác khóa.

## 39. Mount nhầm file thành thư mục

Lỗi thường xảy ra khi source không tồn tại và cú pháp `-v` tự tạo một thư mục.

Ví dụ muốn mount file:

```bash
docker run -v "$(pwd)/app.yml:/app/app.yml" my-image
```

Nếu `app.yml` không tồn tại, Docker có thể tạo thư mục tên `app.yml`. Ứng dụng sau đó báo target không phải file.

Cách tránh:

1. Tạo file trước.
2. Dùng đường dẫn tuyệt đối.
3. Ưu tiên `--mount` để Docker báo lỗi khi source không tồn tại.

## 40. Sửa file trên host nhưng container không thấy

Kiểm tra:

- Có mount đúng container không?
- Source có đúng đường dẫn tuyệt đối không?
- Target có đúng đường dẫn ứng dụng đang đọc không?
- Container có đang dùng file được copy vào image thay vì file mount không?
- Công cụ có cache nội dung không?
- Trên Docker Desktop, thư mục có được chia sẻ và cho phép truy cập không?

Xem mount thật:

```bash
docker inspect my-container --format '{{range .Mounts}}{{println .Type .Source "->" .Destination}}{{end}}'
```

Đọc file trực tiếp từ container:

```bash
docker exec my-container cat /đường/dẫn/file
```

## 41. Dữ liệu database biến mất sau `docker compose down`

Kiểm tra:

1. Có chạy `docker compose down -v` không?
2. Database có thật sự ghi vào đúng thư mục đã mount không?
3. Tên project Compose có thay đổi không?
4. Có chạy Compose từ file hoặc thư mục khác không?
5. Có đổi tên volume trong file Compose không?

Liệt kê:

```bash
docker volume ls
docker compose config
```

Tìm container dùng một volume:

```bash
docker ps -a --filter volume=postgres-data
```

## 42. Không xóa được volume

Docker báo volume đang được sử dụng nếu còn container tham chiếu đến nó, kể cả container đã dừng.

Tìm container:

```bash
docker ps -a --filter volume=app-data
```

Xóa container nếu chắc chắn không còn cần:

```bash
docker rm <container-id>
docker volume rm app-data
```

Không nên ép xóa trước khi xác định dữ liệu và container liên quan.

## 43. Volume rác tăng dần

Nguyên nhân thường gặp:

- Image có lệnh `VOLUME`, tạo anonymous volume.
- Chạy nhiều container thử nghiệm mà không xóa volume.
- Đổi tên project Compose.
- Pipeline CI tạo volume nhưng không dọn dẹp.

Kiểm tra dung lượng Docker:

```bash
docker system df
docker system df -v
```

Xóa volume không được sử dụng:

```bash
docker volume prune
```

Chỉ chạy sau khi đã kiểm tra vì dữ liệu bị xóa không có thùng rác để phục hồi.

---

# Phần VII: Thiết kế và thực hành tốt

## 44. Cách chọn loại mount

### Dùng volume khi

- Lưu dữ liệu database.
- Lưu dữ liệu ứng dụng cần tồn tại sau khi thay container.
- Lưu cache do công cụ tạo ra.
- Không cần người dùng sửa file trực tiếp từ host.
- Muốn Docker quản lý vòng đời và vị trí lưu trữ.
- Muốn sử dụng storage driver.

### Dùng bind mount khi

- Chia sẻ source code trong môi trường phát triển.
- Mount một file cấu hình cụ thể.
- Container cần đọc dữ liệu có sẵn trên host.
- Muốn xem hoặc chỉnh file trực tiếp bằng công cụ trên host.
- Cần tích hợp với cấu trúc thư mục đã có của máy host.

### Dùng tmpfs khi

- Dữ liệu chỉ cần tồn tại lúc container chạy.
- Dữ liệu nhạy cảm không nên ghi xuống disk.
- Dữ liệu tạm cần tốc độ truy cập bộ nhớ.

Ví dụ:

```bash
docker run --rm \
  --mount type=tmpfs,target=/app/temp \
  my-image
```

## 45. Nguyên tắc an toàn

1. Không lưu dữ liệu quan trọng chỉ trong lớp ghi của container.
2. Đặt tên volume theo mục đích, ví dụ `shop-postgres-data`.
3. Dùng `readonly` nếu container không cần ghi.
4. Không bind mount toàn bộ thư mục hệ thống như `/`, `/etc` hoặc Docker socket nếu không thực sự hiểu rủi ro.
5. Không dùng `chmod 777` như cách sửa lỗi quyền mặc định.
6. Backup định kỳ và thử restore, không chỉ kiểm tra rằng file backup tồn tại.
7. Không xóa volume khi chưa xác định nội dung và container liên quan.
8. Với database đang chạy, sử dụng công cụ backup chuyên dụng hoặc snapshot nhất quán.
9. Không lưu password và secret trong Git.
10. Ghi rõ trong tài liệu ứng dụng đường dẫn nào cần mount và dữ liệu nào cần backup.

## 46. Rủi ro khi mount Docker socket

Một số công cụ hướng dẫn:

```bash
-v /var/run/docker.sock:/var/run/docker.sock
```

Container có quyền truy cập Docker socket thường có thể điều khiển Docker daemon, tạo container đặc quyền, mount file system host và có khả năng chiếm quyền host.

Vì vậy:

- Không mount Docker socket chỉ vì muốn container chạy được.
- Chỉ dùng cho công cụ thật sự cần quản lý Docker.
- Giới hạn người có quyền triển khai container đó.
- Cân nhắc socket proxy hoặc giải pháp có quyền hạn hẹp hơn.

## 47. Không đưa dữ liệu runtime vào image

Image nên chứa:

- Mã ứng dụng đã build.
- Runtime.
- Thư viện.
- Cấu hình mặc định không nhạy cảm.

Image không nên chứa:

- Dữ liệu database đang chạy.
- File người dùng upload.
- Log runtime.
- Secret production.
- Cache cần thay đổi liên tục.

Mô hình tốt:

```text
Image     = ứng dụng có thể triển khai lại
Container = tiến trình đang chạy
Volume    = dữ liệu cần tồn tại lâu dài
```

## 48. Log có nên lưu bằng volume?

Có thể lưu log vào volume, nhưng với container hiện đại thường ưu tiên ghi log ra:

```text
stdout
stderr
```

Sau đó Docker hoặc nền tảng vận hành thu thập log.

Ví dụ:

```bash
docker logs -f my-container
```

Lợi ích:

- Không cần quản lý file log trong container.
- Dễ tích hợp hệ thống thu thập log.
- Tránh volume log tăng dung lượng không kiểm soát.

Chỉ lưu file log bằng volume khi ứng dụng hoặc quy trình vận hành thực sự yêu cầu. Khi đó cần cấu hình rotate và giới hạn dung lượng.

---

# Phần VIII: Bài thực hành

## 49. Bài 1: Kiểm chứng dữ liệu trong container bị mất

```bash
docker run --name lesson-1 alpine \
  sh -c "echo hello > /message.txt"

docker start lesson-1
docker exec lesson-1 cat /message.txt

docker rm -f lesson-1

docker run --name lesson-1 alpine \
  cat /message.txt
```

Câu hỏi:

- Vì sao container mới không có `/message.txt`?
- Dừng container khác gì xóa container?

## 50. Bài 2: Giữ dữ liệu bằng named volume

```bash
docker volume create lesson-data

docker run --rm \
  -v lesson-data:/data \
  alpine \
  sh -c "echo volume > /data/type.txt"

docker run --rm \
  -v lesson-data:/data:ro \
  alpine \
  cat /data/type.txt
```

Câu hỏi:

- Vì sao container đầu tiên đã bị xóa nhưng dữ liệu vẫn còn?
- Vì sao container thứ hai nên mount read-only?

## 51. Bài 3: Chia sẻ file bằng bind mount

Tạo thư mục `bind-demo` và file `message.txt` trên host.

Linux hoặc macOS:

```bash
mkdir -p bind-demo
echo "Xin chào" > bind-demo/message.txt

docker run --rm \
  --mount type=bind,source="$(pwd)/bind-demo",target=/data \
  alpine \
  cat /data/message.txt
```

PowerShell:

```powershell
New-Item -ItemType Directory -Force .\bind-demo
Set-Content -Encoding utf8 .\bind-demo\message.txt "Xin chào"

docker run --rm `
  --mount "type=bind,source=$($PWD.Path)\bind-demo,target=/data" `
  alpine `
  cat /data/message.txt
```

Sau đó sửa `message.txt` trên host và chạy lại container.

## 52. Bài 4: PostgreSQL với Docker Compose

Tạo file `compose.yml`:

```yaml
services:
  postgres:
    image: postgres:17
    environment:
      POSTGRES_USER: student
      POSTGRES_PASSWORD: student-password
      POSTGRES_DB: lesson
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

volumes:
  postgres-data:
```

Chạy:

```bash
docker compose up -d
docker compose exec postgres \
  psql -U student -d lesson \
  -c "CREATE TABLE notes (id serial primary key, content text);"

docker compose exec postgres \
  psql -U student -d lesson \
  -c "INSERT INTO notes(content) VALUES ('Dữ liệu trong volume');"
```

Dừng project:

```bash
docker compose down
docker compose up -d
```

Kiểm tra dữ liệu:

```bash
docker compose exec postgres \
  psql -U student -d lesson \
  -c "SELECT * FROM notes;"
```

Cuối bài có thể dọn dẹp:

```bash
docker compose down -v
```

Chỉ chạy lệnh cuối khi chắc chắn không cần dữ liệu nữa.

---

# Phần IX: Bảng lệnh tra cứu nhanh

## 53. Lệnh volume

| Mục đích | Lệnh |
|---|---|
| Liệt kê volume | `docker volume ls` |
| Tạo volume | `docker volume create <tên>` |
| Xem chi tiết | `docker volume inspect <tên>` |
| Xóa volume | `docker volume rm <tên>` |
| Xóa volume không dùng | `docker volume prune` |
| Tìm container dùng volume | `docker ps -a --filter volume=<tên>` |
| Xem dung lượng Docker | `docker system df -v` |

## 54. Mẫu lệnh mount

Named volume:

```bash
docker run \
  --mount type=volume,source=app-data,target=/app/data \
  my-image
```

Named volume chỉ đọc:

```bash
docker run \
  --mount type=volume,source=app-data,target=/app/data,readonly \
  my-image
```

Bind mount:

```bash
docker run \
  --mount type=bind,source=/host/path,target=/container/path \
  my-image
```

Bind mount chỉ đọc:

```bash
docker run \
  --mount type=bind,source=/host/path,target=/container/path,readonly \
  my-image
```

tmpfs:

```bash
docker run \
  --mount type=tmpfs,target=/app/temp \
  my-image
```

## 55. Checklist trước khi triển khai

- Dữ liệu nào phải tồn tại sau khi container bị thay thế?
- Dữ liệu đó nên dùng volume hay bind mount?
- Target trong container có đúng đường dẫn ứng dụng sử dụng không?
- Container có thực sự cần quyền ghi không?
- UID/GID có phù hợp không?
- Dữ liệu có chứa secret không?
- Đã có phương án backup chưa?
- Đã thử restore bản backup chưa?
- Có giới hạn dung lượng hoặc cơ chế dọn dẹp không?
- Lệnh `docker compose down -v` có thể xóa dữ liệu nào?
- Nếu chạy nhiều container, hệ thống lưu trữ có hỗ trợ truy cập đồng thời không?
- Nếu dùng Docker Desktop, hiệu năng bind mount có đáp ứng không?

---

## 56. Kết luận

Điểm quan trọng nhất:

```text
Container có thể bị xóa và tạo lại.
Dữ liệu quan trọng phải có vòng đời độc lập với container.
```

Sử dụng:

- **Volume** cho dữ liệu bền vững do ứng dụng tạo ra, đặc biệt là database và cache.
- **Bind mount** cho source code, cấu hình và dữ liệu cần truy cập trực tiếp từ host.
- **Read-only mount** khi container chỉ cần đọc.
- **Backup chuyên dụng** cho database và luôn thử quy trình restore.

Nếu chưa chắc nên chọn gì, hãy bắt đầu với quy tắc:

```text
Database và dữ liệu ứng dụng -> named volume
Source code khi phát triển    -> bind mount
File cấu hình                 -> bind mount read-only
Dữ liệu tạm                   -> tmpfs
```
