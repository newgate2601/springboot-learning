# Docker Layer và Build Cache: từ cơ bản đến nâng cao

Tài liệu này giải thích **layer** và **build cache** trong Docker theo cách dễ hiểu, đi từ kiến thức nền tảng đến các kỹ thuật tối ưu Dockerfile trong dự án thực tế.

Mục tiêu sau khi đọc:

- Hiểu Docker image được tạo từ các layer như thế nào.
- Phân biệt image layer và lớp ghi của container.
- Hiểu vì sao sửa một dòng trong Dockerfile có thể làm nhiều bước phải build lại.
- Biết cách Docker xác định một bước có thể dùng cache hay không.
- Biết sắp xếp Dockerfile để build nhanh hơn.
- Biết sử dụng `.dockerignore`, multi-stage build và BuildKit cache mount.
- Tối ưu build cho ứng dụng Spring Boot sử dụng Maven.
- Biết kiểm tra và xử lý các lỗi cache thường gặp.

## Cách đọc tài liệu này

Tài liệu này không chỉ trả lời "lệnh Docker viết thế nào", mà tập trung giải thích **vì sao Docker chạy như vậy**. Khi đọc từng phần, hãy để ý ba câu hỏi:

1. Bước này tạo ra layer hay chỉ thay đổi metadata?
2. Nếu file hoặc instruction thay đổi, cache của bước nào bị mất?
3. Nếu không tối ưu, Docker sẽ phải làm lại việc gì và image cuối sẽ mang theo thứ gì không cần thiết?

Khi hiểu ba câu hỏi này, bạn sẽ biết cách đọc log build, biết vì sao CI chậm hơn local, và biết sửa Dockerfile theo nguyên nhân thay vì thử ngẫu nhiên.

---

## 1. Docker image không phải là một file duy nhất

Khi mới học Docker, ta thường hình dung image là một gói chứa:

- Hệ điều hành tối thiểu.
- Runtime như Java hoặc Node.js.
- Thư viện.
- Source code hoặc file đã build.
- Cấu hình mặc định.

Cách hiểu này đúng ở mức tổng quát. Tuy nhiên, bên trong Docker, image thường được tạo từ nhiều lớp gọi là **layer**.

Ví dụ Dockerfile:

```dockerfile
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/app.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Có thể hình dung image được tạo như sau:

```text
Layer 4: cấu hình ENTRYPOINT
Layer 3: file /app/app.jar
Layer 2: metadata WORKDIR /app
Layer 1: các layer của eclipse-temurin:21-jre
```

Các layer được xếp chồng lên nhau để tạo ra file system và cấu hình cuối cùng của image.

Điểm quan trọng là image cuối cùng nhìn giống một file system hoàn chỉnh, nhưng Docker lưu nó dưới dạng nhiều phần. Khi bạn pull image, Docker không nhất thiết tải lại mọi thứ. Nếu máy đã có một vài layer giống hệt, Docker chỉ cần tải phần còn thiếu. Khi bạn build image, Docker cũng tận dụng đặc điểm này để không phải chạy lại những bước có đầu vào không đổi.

Vì vậy, khi tối ưu Dockerfile, ta không chỉ tối ưu câu lệnh. Ta đang tối ưu cách Docker chia công việc thành các lớp có thể tái sử dụng.

---

## 2. Layer là gì?

Layer là một phần thay đổi của image được tạo ra trong quá trình build.

Mỗi instruction trong Dockerfile có thể tạo:

- Một filesystem layer mới.
- Chỉ một thay đổi metadata.
- Hoặc không tạo layer file system đáng kể, tùy instruction và builder.

Ví dụ:

```dockerfile
FROM ubuntu:24.04
RUN apt-get update && apt-get install -y curl
COPY application.yml /app/application.yml
ENV APP_ENV=production
```

Mô hình đơn giản:

```text
Image cuối cùng
|
|-- ENV APP_ENV=production
|-- COPY application.yml
|-- RUN cài curl
`-- các layer của ubuntu:24.04
```

Layer thường có các đặc điểm:

- Có nội dung riêng.
- Có thể được dùng lại.
- Được nhận diện bằng digest.
- Image layer là bất biến sau khi được tạo.
- Nhiều image có thể chia sẻ cùng một layer.

Cần phân biệt hai ý:

- Layer là kết quả đã được lưu sau một bước build.
- Instruction là dòng lệnh trong Dockerfile tạo ra hoặc cấu hình layer đó.

Hai Dockerfile có instruction nhìn gần giống nhau vẫn có thể tạo cache khác nhau nếu đầu vào khác nhau. Ví dụ `COPY src/ /app/src/` phụ thuộc vào nội dung thư mục `src`; chỉ cần một file trong `src` đổi, kết quả của instruction đó đã khác.

## 3. Vì sao Docker sử dụng layer?

Layer giúp Docker:

1. Không phải lưu nhiều lần cùng một dữ liệu.
2. Chỉ tải những phần chưa có khi pull image.
3. Chỉ đẩy những phần mới khi push image.
4. Dùng lại kết quả của bước build cũ.
5. Tạo container nhanh hơn.

Ví dụ hai image:

```text
service-a:1.0
|
|-- service-a.jar
`-- eclipse-temurin:21-jre

service-b:1.0
|
|-- service-b.jar
`-- eclipse-temurin:21-jre
```

Nếu hai image dùng cùng base image, Docker có thể lưu các layer của `eclipse-temurin:21-jre` một lần và dùng chung.

Trong thực tế, lợi ích này xuất hiện ở nhiều chỗ:

- Máy developer build nhiều lần: các bước cài package hoặc tải dependency có thể được dùng lại.
- CI/CD push image: nếu chỉ layer chứa application thay đổi, registry không cần nhận lại toàn bộ base image.
- Server deploy: nếu server đã có base image, lần pull bản mới chỉ cần kéo layer ứng dụng mới.
- Nhiều service cùng nền tảng: các service Spring Boot cùng dùng JRE có thể chia sẻ phần runtime giống nhau.

Không có layer, mỗi lần image thay đổi Docker sẽ phải xem image như một khối lớn. Chỉ sửa một class nhỏ cũng có thể kéo theo việc lưu, push, pull lại cả khối lớn đó.

## 4. Layer là bất biến

Image layer là **immutable**, nghĩa là không bị sửa trực tiếp sau khi được tạo.

Nếu một bước mới thay đổi file đã có ở layer trước, Docker không quay lại sửa layer cũ. Docker tạo một layer mới chứa thay đổi.

Ví dụ:

```dockerfile
FROM alpine:3.22

RUN echo "phiên bản 1" > /message.txt
RUN echo "phiên bản 2" > /message.txt
```

Mô hình:

```text
Layer 2: /message.txt = "phiên bản 2"
Layer 1: /message.txt = "phiên bản 1"
Base:    Alpine
```

Khi container đọc `/message.txt`, nó nhìn thấy phiên bản ở layer trên cùng:

```text
phiên bản 2
```

Layer dưới vẫn tồn tại trong image.

---

## 5. Xóa file ở layer sau không làm image nhỏ lại như mong đợi

Dockerfile sau tải một file lớn rồi xóa ở instruction tiếp theo:

```dockerfile
FROM alpine:3.22

RUN wget https://example.com/big-file.tar.gz
RUN rm big-file.tar.gz
```

Có thể hình dung:

```text
Layer 2: đánh dấu big-file.tar.gz đã bị xóa
Layer 1: vẫn chứa dữ liệu của big-file.tar.gz
Base:    Alpine
```

Ở file system cuối cùng, file không còn nhìn thấy. Tuy nhiên, dữ liệu của file vẫn tồn tại trong layer trước và vẫn làm tăng kích thước image.

Cách tốt hơn là tải, sử dụng và xóa trong cùng một `RUN`:

```dockerfile
FROM alpine:3.22

RUN wget https://example.com/big-file.tar.gz \
    && tar -xzf big-file.tar.gz \
    && rm big-file.tar.gz
```

Layer được tạo sau instruction này chỉ lưu trạng thái cuối cùng.

> Ghép lệnh có liên quan để không giữ file tạm là tốt. Không nên ghép mọi lệnh thành một `RUN` khổng lồ chỉ để giảm số layer. Dockerfile vẫn cần dễ đọc, dễ cache và dễ bảo trì.

## 6. Whiteout file

Khi một file ở layer dưới bị xóa tại layer trên, hệ thống lưu trữ layer cần ghi nhận rằng file đó không còn xuất hiện trong kết quả cuối cùng.

Docker và chuẩn image thường sử dụng một dạng dấu xóa gọi là **whiteout**.

Mô hình đơn giản:

```text
Layer trên:  "ẩn file /app/old.txt"
Layer dưới:  có file /app/old.txt

Kết quả: container không nhìn thấy /app/old.txt
```

Người dùng Docker hằng ngày không cần thao tác trực tiếp với whiteout. Khái niệm này giải thích vì sao:

- File bị xóa khỏi file system cuối cùng.
- Nhưng dữ liệu ở layer cũ vẫn có thể chiếm dung lượng image.

---

## 7. Image layer và container layer khác nhau

Khi chạy một container, Docker không sửa các image layer. Docker thêm một lớp có thể ghi ở trên cùng, thường gọi là:

- Writable layer.
- Container layer.
- Lớp ghi của container.

Mô hình:

```text
Container đang chạy
|
|-- Container writable layer  <- có thể ghi
|-- Image layer: app.jar       <- chỉ đọc
|-- Image layer: Java runtime  <- chỉ đọc
`-- Base image layers          <- chỉ đọc
```

Khi ứng dụng tạo file:

```text
/app/logs/app.log
```

Nếu đường dẫn đó không được mount volume hoặc bind mount, file được ghi vào writable layer của container.

Khi xóa container:

```text
Writable layer bị xóa
Image layers vẫn còn
```

Đây là lý do dữ liệu quan trọng nên lưu trong volume hoặc bind mount thay vì container layer.

## 8. Copy-on-write

Docker thường sử dụng cơ chế **copy-on-write**.

Giả sử image có file:

```text
/app/config.yml
```

Container chỉ đọc file:

```text
Docker dùng trực tiếp file ở image layer
```

Nếu container sửa file:

```text
1. File được sao chép lên writable layer.
2. Container sửa bản sao.
3. Image layer gốc không thay đổi.
```

Mô hình:

```text
Writable layer: /app/config.yml phiên bản đã sửa
Image layer:    /app/config.yml phiên bản gốc
```

Container nhìn thấy bản ở layer trên cùng.

Copy-on-write giúp nhiều container dùng chung image layer mà vẫn có môi trường ghi riêng.

---

# Phần I: Quan sát layer

## 9. Xem lịch sử các bước của image

Sử dụng:

```bash
docker image history nginx
```

Hoặc:

```bash
docker history nginx
```

Kết quả thường có các cột:

- `IMAGE`: ID của layer hoặc bản ghi.
- `CREATED`: thời điểm tạo.
- `CREATED BY`: instruction hoặc lệnh đã tạo thay đổi.
- `SIZE`: kích thước liên quan.
- `COMMENT`: ghi chú của builder.

Hiển thị đầy đủ lệnh:

```bash
docker history --no-trunc nginx
```

Lưu ý:

- `docker history` hữu ích để quan sát tổng quan.
- Không phải mọi dòng đều tương ứng với một filesystem layer có dữ liệu.
- Image hiện đại có thể được build bằng BuildKit, khiến phần `CREATED BY` hiển thị khác cách viết Dockerfile ban đầu.

## 10. Xem digest của layer

```bash
docker image inspect nginx
```

Có thể lấy danh sách root filesystem layer:

```bash
docker image inspect \
  --format '{{json .RootFS.Layers}}' \
  nginx
```

Mỗi giá trị `sha256:...` đại diện cho digest của nội dung layer.

Digest được tạo từ nội dung. Nếu nội dung thay đổi, digest thường thay đổi.

## 11. Xem dung lượng Docker đang sử dụng

```bash
docker system df
```

Chi tiết hơn:

```bash
docker system df -v
```

Thông tin có thể gồm:

- Tổng dung lượng image.
- Dung lượng có thể thu hồi.
- Container.
- Local volume.
- Build cache.

Điểm cần chú ý:

```text
Kích thước logic của nhiều image cộng lại
không nhất thiết bằng dung lượng thật trên disk
```

Nguyên nhân là các image có thể chia sẻ layer.

## 12. `docker image ls` và kích thước image

```bash
docker image ls
```

Cột `SIZE` thể hiện kích thước tổng các layer của image theo cách nhìn logic.

Nếu hai image cùng dùng base image 300 MB:

```text
service-a   350 MB
service-b   360 MB
```

Không có nghĩa Docker chắc chắn dùng 710 MB trên disk. Một phần lớn layer có thể được chia sẻ.

---

# Phần II: Build cache

## 13. Build cache là gì?

Build cache là cơ chế cho phép Docker dùng lại kết quả của bước build trước đó.

Ví dụ:

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Lần build đầu:

```bash
docker build -t demo-app:1.0 .
```

Docker phải thực hiện các bước và lưu kết quả.

Lần build thứ hai, nếu đầu vào không thay đổi:

```bash
docker build -t demo-app:1.0 .
```

Docker có thể dùng lại cache:

```text
[1/3] FROM eclipse-temurin:21-jre   CACHED
[2/3] WORKDIR /app                  CACHED
[3/3] COPY target/app.jar app.jar   CACHED
```

Kết quả:

- Build nhanh hơn.
- Không lặp lại thao tác tải dependency hoặc biên dịch không cần thiết.
- Giảm tài nguyên CPU, disk và network.

Cần hiểu build cache như một chuỗi kết quả trung gian. Mỗi bước trong Dockerfile không đứng độc lập hoàn toàn; nó dựa trên trạng thái do bước trước tạo ra. Vì vậy, cache của bước sau chỉ có ý nghĩa nếu chuỗi phía trước vẫn giống như lần build cũ.

Ví dụ, hai instruction `RUN echo hello` giống nhau nhưng đứng sau hai base image khác nhau thì không thể xem là cùng một kết quả:

```dockerfile
FROM alpine:3.22
RUN echo hello
```

khác với:

```dockerfile
FROM ubuntu:24.04
RUN echo hello
```

Câu lệnh giống nhau, nhưng filesystem đầu vào khác nhau. Docker phải xem cả "bước này làm gì" và "bước này chạy trên trạng thái nào".

## 14. Cache không chỉ là “instruction giống nhau”

Docker không đơn giản nhìn thấy cùng một dòng rồi luôn dùng cache.

Builder xem xét nhiều thông tin, có thể gồm:

- Instruction hiện tại.
- Các đối số của instruction.
- Kết quả hoặc cache key của bước trước.
- Nội dung file liên quan đến `COPY` và `ADD`.
- Build argument được sử dụng.
- Mount và tùy chọn của bước build.
- Nền tảng CPU và hệ điều hành mục tiêu.
- Metadata liên quan.

Có thể hình dung:

```text
Cache key của bước hiện tại
    =
instruction hiện tại
+ đầu vào của instruction
+ trạng thái/cache key của bước trước
```

Nếu cache key khớp với kết quả đã lưu, Docker có thể dùng cache.

Điều này giải thích vì sao chỉ sửa một dòng ở gần đầu Dockerfile có thể làm nhiều bước phía sau chạy lại. Không phải vì Docker "quên cache", mà vì chuỗi đầu vào đã đổi. Cache cũ của các bước sau được tạo trên trạng thái cũ, nên không còn chắc chắn đúng cho trạng thái mới.

Ví dụ:

```dockerfile
FROM alpine:3.22
RUN apk add --no-cache curl
COPY app.sh /app.sh
RUN chmod +x /app.sh
```

Nếu đổi base image từ `alpine:3.22` sang `alpine:3.23`, bước `RUN apk add` không thể mặc nhiên dùng kết quả cũ. Cùng một lệnh `apk add` nhưng chạy trên base image khác, package index khác, thư viện nền khác. Các bước sau cũng phải được đánh giá lại theo chuỗi mới.

## 15. Cache invalidation là gì?

**Cache invalidation** là khi cache của một bước không còn hợp lệ và bước đó phải chạy lại.

Ví dụ:

```dockerfile
FROM alpine:3.22
RUN apk add --no-cache curl
COPY app.sh /app.sh
RUN chmod +x /app.sh
```

Nếu `app.sh` thay đổi:

```text
FROM                         -> có thể dùng cache
RUN apk add                  -> có thể dùng cache
COPY app.sh                  -> cache bị mất
RUN chmod                    -> phải chạy lại
```

Khi một bước không dùng được cache, các bước sau thường cũng phải được đánh giá và tạo lại theo chuỗi dependency mới.

Mô hình:

```text
Bước 1: cache hợp lệ
    |
Bước 2: cache hợp lệ
    |
Bước 3: đầu vào thay đổi
    |
    +--> bước 3 build lại
           |
           +--> bước 4 không thể dùng chuỗi cache cũ
```

Không phải lúc nào mất cache cũng là xấu. Nếu dependency thật sự thay đổi, build lại là đúng. Vấn đề cần tránh là mất cache vì lý do không liên quan, ví dụ:

- File log thay đổi nhưng bị `COPY . .` kéo vào image.
- `.git` thay đổi làm build context đổi.
- Copy toàn bộ source trước khi tải dependency.
- Dùng build arg thay đổi liên tục dù không cần.

Tối ưu cache không phải là cố làm mọi thứ luôn `CACHED`. Mục tiêu là: phần nào thật sự thay đổi thì build lại, phần nào không liên quan thì được giữ lại.

## 16. Vì sao thứ tự Dockerfile rất quan trọng?

Đặt bước ít thay đổi trước và bước thường thay đổi sau giúp tận dụng cache tốt hơn.

Dockerfile chưa tối ưu:

```dockerfile
FROM node:24
WORKDIR /app

COPY . .
RUN npm ci

CMD ["npm", "start"]
```

Mỗi khi sửa bất kỳ file source nào:

```text
COPY . . thay đổi
    |
    +--> RUN npm ci phải chạy lại
```

Trong khi dependency chỉ thay đổi khi `package-lock.json` hoặc `package.json` thay đổi.

Dockerfile tốt hơn:

```dockerfile
FROM node:24
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

CMD ["npm", "start"]
```

Khi chỉ sửa source code:

```text
COPY package files  -> cache
RUN npm ci          -> cache
COPY source         -> chạy lại
```

Quy tắc:

```text
Ít thay đổi  -> đặt trước
Hay thay đổi -> đặt sau
```

Một cách nghĩ thực tế là chia Dockerfile thành các vùng:

```text
Vùng nền tảng:
    FROM, package hệ thống, user, thư mục làm việc

Vùng dependency:
    pom.xml, package-lock.json, requirements.txt, go.mod

Vùng source:
    src, static asset, template

Vùng runtime:
    command, entrypoint, healthcheck, cấu hình mặc định
```

Vùng dependency thường đắt nhất vì phải tải nhiều thứ từ mạng. Nếu đặt source code trước dependency, mỗi lần sửa code sẽ kéo theo tải dependency lại. Nếu đặt dependency trước source, Docker có cơ hội giữ lại phần đắt tiền đó.

---

## 17. Cache của `FROM`

```dockerfile
FROM eclipse-temurin:21-jre
```

Docker có thể dùng base image đã có trong local cache.

Tuy nhiên, tag như `21-jre` có thể được cập nhật để trỏ đến image mới.

Build thông thường không phải lúc nào cũng tự kiểm tra phiên bản mới nhất từ registry theo cách người dùng mong đợi.

Muốn kiểm tra và kéo base image mới:

```bash
docker build --pull -t demo-app .
```

Khác biệt:

- `--pull`: cố lấy phiên bản mới hơn của base image.
- `--no-cache`: không dùng build cache cho các bước build.

Có thể kết hợp:

```bash
docker build --pull --no-cache -t demo-app .
```

## 18. Cache của `RUN`

Ví dụ:

```dockerfile
RUN apt-get update && apt-get install -y curl
```

Docker không chạy `apt-get update` chỉ để kiểm tra repository đã có package mới hay chưa. Nếu cache key vẫn hợp lệ, Docker có thể dùng lại kết quả cũ.

Điều đó có nghĩa:

```text
Repository bên ngoài thay đổi
không tự động làm cache của RUN mất hiệu lực
```

Muốn cập nhật, có thể:

- Build với `--no-cache`.
- Thay đổi dependency hoặc version được pin.
- Dùng cơ chế chủ động làm mới cache trong CI.

Ví dụ Debian/Ubuntu nên kết hợp update và install:

```dockerfile
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
```

Không nên tách:

```dockerfile
RUN apt-get update
RUN apt-get install -y curl
```

Vì bước `apt-get update` có thể dùng cache cũ trong khi bước cài package đã thay đổi, gây lỗi hoặc sử dụng metadata cũ.

## 19. Cache của `COPY`

Ví dụ:

```dockerfile
COPY src/ /app/src/
```

Builder xem xét nội dung và metadata phù hợp của các file được copy để xác định cache.

Nếu nội dung file trong `src/` thay đổi, cache của bước `COPY` thường bị invalid.

Thời gian sửa file không phải lúc nào cũng là yếu tố duy nhất quyết định. Builder tập trung vào checksum và thông tin đầu vào cần thiết.

Điều quan trọng:

- Copy phạm vi càng rộng thì càng dễ mất cache.
- `COPY . .` phụ thuộc vào gần như toàn bộ build context.
- File log, file IDE hoặc output build thay đổi cũng có thể làm cache mất hiệu lực nếu không bị loại bởi `.dockerignore`.

## 20. Cache của `ADD`

`ADD` có nhiều khả năng hơn `COPY`, ví dụ:

- Copy file từ build context.
- Tự giải nén một số archive local.
- Trong builder hiện đại, có thể hỗ trợ thêm một số nguồn khác.

Vì hành vi rộng hơn, nên ưu tiên:

```dockerfile
COPY ...
```

khi chỉ cần copy file thông thường.

Điều này làm Dockerfile rõ ý định hơn và dễ dự đoán cache hơn.

## 21. Cache của `ARG`

```dockerfile
ARG APP_VERSION
RUN echo "$APP_VERSION" > /app-version.txt
```

Build:

```bash
docker build \
  --build-arg APP_VERSION=1.0.0 \
  -t demo-app:1.0.0 .
```

Nếu đổi thành:

```bash
docker build \
  --build-arg APP_VERSION=1.0.1 \
  -t demo-app:1.0.1 .
```

Bước sử dụng `APP_VERSION` và các bước phụ thuộc sau đó có thể phải build lại.

Không nên dùng build arg chứa secret:

```dockerfile
ARG DB_PASSWORD
```

Build arg có thể xuất hiện trong metadata, lịch sử build hoặc hệ thống cache. Hãy dùng BuildKit secret mount cho secret cần thiết trong lúc build.

## 22. `ENV` và cache

```dockerfile
ENV APP_ENV=production
RUN echo "$APP_ENV" > /env.txt
```

Thay đổi `ENV` làm thay đổi cấu hình image và có thể ảnh hưởng cache của bước sau sử dụng môi trường đó.

Khác biệt cơ bản:

- `ARG`: chủ yếu tồn tại trong lúc build, trừ khi được ghi vào image.
- `ENV`: trở thành biến môi trường mặc định của image và container.

---

# Phần III: Build context và `.dockerignore`

## 23. Build context là gì?

Trong lệnh:

```bash
docker build -t demo-app .
```

Dấu `.` là **build context**.

Build context là tập hợp file mà builder có thể sử dụng trong quá trình build.

Ví dụ:

```text
project/
|-- Dockerfile
|-- pom.xml
|-- src/
|-- target/
|-- .git/
`-- logs/
```

Nếu build tại `project/`:

```bash
docker build .
```

Các file trong thư mục này có thể thuộc build context, trừ những file bị loại bởi `.dockerignore`.

Dockerfile không thể tùy ý `COPY` file nằm ngoài context:

```dockerfile
COPY ../secret.txt /app/
```

Lệnh này thường không hợp lệ nếu `secret.txt` nằm ngoài build context.

## 24. Vì sao build context lớn là vấn đề?

Build context lớn có thể:

- Mất thời gian đọc và truyền dữ liệu cho builder.
- Làm cache key phải xử lý nhiều file.
- Dễ vô tình copy file không cần thiết.
- Làm image lớn.
- Có nguy cơ đưa secret vào quá trình build.
- Làm `COPY . .` mất cache thường xuyên.

Build context lớn ảnh hưởng cả khi Dockerfile không copy toàn bộ. Trước khi builder xử lý `COPY`, Docker vẫn cần xác định những file nào thuộc context và truyền chúng cho builder tùy môi trường build. Với remote builder hoặc CI, chi phí truyền context có thể đáng kể.

Kiểm tra log build:

```text
transferring context: 800MB
```

Nếu project nhỏ nhưng context hàng trăm MB, thường có file không cần thiết như:

- `.git`.
- `target`.
- `node_modules`.
- Log.
- File backup.
- Dữ liệu database local.
- File IDE.

Một dấu hiệu khác là log `transferring context` thay đổi liên tục dù bạn chỉ sửa một file nhỏ. Khi đó hãy kiểm tra xem output build, log, cache local hoặc thư mục tool có đang bị đưa vào context hay không.

## 25. `.dockerignore`

`.dockerignore` hoạt động gần giống `.gitignore`, dùng để loại file khỏi build context.

Ví dụ cho dự án Spring Boot:

```dockerignore
.git
.idea
.vscode
*.iml
*.log

target

.env
*.pem
*.key

docker-compose*.yml
README.md
```

Không nên copy mẫu này một cách máy móc. Chỉ loại những file Dockerfile không cần.

Ví dụ nếu Dockerfile cần:

```dockerfile
COPY target/app.jar app.jar
```

thì không được ignore toàn bộ `target`.

`.dockerignore` là một phần của thiết kế cache. Nó không chỉ giúp context nhỏ hơn, mà còn giúp cache ổn định hơn vì loại bỏ những file thay đổi thường xuyên nhưng không liên quan đến image.

## 26. Mẫu `.dockerignore` khi build Maven bên trong Docker

Nếu Dockerfile copy source rồi chạy Maven:

```dockerignore
.git
.idea
.vscode
*.iml
*.log

target

.env
*.pem
*.key
```

`target` không cần gửi vào context vì Maven trong container sẽ tạo lại.

## 27. Mẫu `.dockerignore` khi build JAR bên ngoài Docker

Nếu pipeline đã chạy Maven trên host và Dockerfile chỉ copy JAR:

```dockerfile
COPY target/my-app.jar app.jar
```

Có thể dùng:

```dockerignore
**
!target/my-app.jar
!Dockerfile
```

Ý nghĩa:

1. Ignore mọi thứ.
2. Cho phép đúng file JAR cần thiết.
3. Cho phép Dockerfile nếu công cụ build cần.

Cách này tạo context rất nhỏ và giảm khả năng file không liên quan làm mất cache.

## 28. Kiểm tra `.dockerignore` cẩn thận

Nếu ignore nhầm file:

```text
COPY failed: file not found
```

Hoặc builder báo file không có trong build context.

Khi debug:

1. Xem lại source của `COPY`.
2. Xem quy tắc `.dockerignore`.
3. Kiểm tra context trong lệnh `docker build`.
4. Chạy build với log rõ hơn:

```bash
docker build --progress=plain .
```

---

# Phần IV: Viết Dockerfile tận dụng cache

## 29. Nguyên tắc sắp xếp instruction

Thứ tự thường phù hợp:

```text
1. Base image
2. Package hệ thống ít thay đổi
3. File mô tả dependency
4. Tải dependency
5. Source code
6. Build ứng dụng
7. Cấu hình runtime
```

Mục tiêu là tách:

- Phần ổn định, ít thay đổi.
- Phần thay đổi thường xuyên.

## 30. Ví dụ chưa tối ưu với Maven

```dockerfile
FROM maven:3.9-eclipse-temurin-21

WORKDIR /workspace

COPY . .

RUN mvn clean package -DskipTests

CMD ["java", "-jar", "target/app.jar"]
```

Vấn đề:

- `COPY . .` phụ thuộc vào toàn bộ project.
- Sửa một file Java làm bước Maven chạy lại hoàn toàn.
- Dependency Maven có thể phải tải lại nếu cache trước không dùng được.
- Image cuối chứa Maven, source code và output trung gian.
- Image runtime lớn hơn cần thiết.

## 31. Tách dependency Maven khỏi source code

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/target/app.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Khi chỉ sửa source:

```text
COPY pom.xml                 -> cache
RUN dependency:go-offline   -> cache
COPY src                    -> chạy lại
RUN package                 -> chạy lại
```

Khi `pom.xml` thay đổi:

```text
COPY pom.xml                 -> thay đổi
RUN dependency:go-offline   -> chạy lại
Các bước sau                -> chạy lại
```

Đây là hành vi hợp lý vì dependency có thể đã thay đổi.

## 32. Multi-stage build

Multi-stage build cho phép dùng một stage để build và một stage nhỏ hơn để chạy.

Nếu không dùng multi-stage build, Dockerfile thường chỉ có một stage:

```dockerfile
FROM maven:3.9-eclipse-temurin-21

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

USER 10001
ENTRYPOINT ["java", "-jar", "target/app.jar"]
```

Khi build theo cách này, image cuối chính là image Maven sau khi đã thêm source code và output build. Lúc container chạy, nó chỉ cần JRE và file JAR, nhưng bên trong image vẫn còn Maven, JDK, source, thư mục `target`, Maven local repository nếu đã được tạo trong layer, compiler và nhiều công cụ chỉ phục vụ giai đoạn build.

Luồng chạy khi không dùng multi-stage:

```text
docker build
-> lấy image Maven/JDK
-> copy source vào image
-> chạy Maven để tạo JAR
-> image cuối giữ nguyên toàn bộ môi trường build

docker run
-> chạy JAR trong cùng image Maven/JDK đó
```

Điểm bất lợi là build và runtime bị trộn vào nhau. Một thứ chỉ cần trong lúc build vẫn đi theo production image. Image thường lớn hơn, kéo/push lâu hơn, scan security ra nhiều package hơn, và nếu có lỗi cấu hình `COPY` hoặc `.dockerignore`, source code hoặc file trung gian dễ bị đóng gói nhầm.

Khi dùng multi-stage, Dockerfile tách rõ hai phần:

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app
COPY --from=build /workspace/target/app.jar app.jar

USER 10001
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Luồng chạy khi dùng multi-stage:

```text
docker build
-> stage build dùng Maven/JDK để compile và package
-> stage runtime bắt đầu từ JRE nhỏ hơn
-> copy đúng file JAR từ stage build sang stage runtime
-> image cuối chỉ là stage runtime

docker run
-> chạy JAR trong image runtime gọn hơn
```

Stage `build` vẫn tồn tại trong quá trình build và vẫn có thể được Docker cache. Nhưng kết quả được publish/tag cuối cùng là stage cuối, tức `runtime`. Những file không được `COPY --from=build` sang stage runtime sẽ không nằm trong image cuối.

Image cuối không cần chứa:

- Maven.
- Source code.
- Maven local repository.
- Compiler.
- File build trung gian.

Lợi ích:

- Image nhỏ hơn vì chỉ giữ JRE và artifact cần chạy, không giữ Maven/JDK/source/output phụ.
- Push, pull và deploy nhanh hơn do số layer và dung lượng runtime image giảm.
- Ít bề mặt tấn công hơn: production image có ít binary/package hơn, nên ít CVE không liên quan hơn và ít công cụ có thể bị lợi dụng nếu container bị xâm nhập.
- Tách rõ môi trường build và runtime: build cần Maven/JDK, runtime chỉ cần JRE. Điều này giúp Dockerfile dễ review và dễ áp chính sách bảo mật hơn.
- Giảm nguy cơ lộ source code, test resource, file cấu hình build hoặc dependency cache trong image chạy thật.
- Stage build vẫn được cache, nên lợi ích cache của Docker không mất đi. Khi `pom.xml` không đổi, các bước tải dependency ở stage build vẫn có thể được dùng lại.
- Dễ debug từng giai đoạn bằng `docker build --target build .`, ví dụ kiểm tra JAR được tạo ra trước khi sang runtime stage.

So sánh nhanh:

| Tiêu chí | Không dùng multi-stage | Có dùng multi-stage |
|---|---|---|
| Image cuối | Chứa cả tool build và runtime | Chỉ chứa phần cần chạy |
| Dung lượng | Thường lớn hơn | Thường nhỏ hơn |
| Bảo mật | Nhiều package, nhiều binary hơn | Ít bề mặt tấn công hơn |
| Tái tạo build | Có thể tái tạo nhưng runtime bị trộn với build | Tách rõ build và runtime |
| Debug | Dễ thấy mọi thứ trong một image nhưng dễ lẫn lộn | Debug theo stage bằng `--target` |
| Cache | Vẫn có cache, nhưng artifact và tool build nằm trong image cuối | Stage build vẫn cache, image cuối vẫn gọn |

Điểm dễ nhầm: multi-stage không làm bước Maven tự nhiên nhanh hơn ở lần đầu. Lần build đầu vẫn phải tải dependency và package. Lợi ích chính là image cuối sạch hơn, nhỏ hơn, an toàn hơn. Tốc độ build tốt hơn đến từ cách sắp xếp instruction và cache mount; multi-stage giúp các tối ưu đó không làm bẩn runtime image.

Trong dự án production, multi-stage thường là mặc định tốt vì nó phản ánh đúng vòng đời ứng dụng:

```text
Source code + tool build -> artifact
Artifact + runtime       -> container chạy thật
```

Hai việc này khác nhau, nên nên nằm ở hai stage khác nhau.

## 33. Cache giữa các stage

Mỗi stage có chuỗi dependency riêng.

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS dependencies
WORKDIR /workspace
COPY pom.xml .
RUN mvn dependency:go-offline

FROM dependencies AS build
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=build /workspace/target/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Nếu source thay đổi nhưng `pom.xml` không đổi, stage dependency có thể dùng cache.

## 34. Build một stage cụ thể

```bash
docker build --target build -t demo-app-build .
```

Hữu ích khi:

- Debug stage build.
- Chạy test trong một stage riêng.
- Kiểm tra file output.
- CI cần build đến một giai đoạn nhất định.

Ví dụ:

```dockerfile
FROM build AS test
RUN mvn test

FROM build AS package
RUN mvn package -DskipTests
```

Build stage test:

```bash
docker build --target test .
```

---

# Phần V: BuildKit và cache nâng cao

## 35. BuildKit là gì?

BuildKit là hệ thống build hiện đại của Docker.

BuildKit cung cấp nhiều khả năng:

- Build song song các bước không phụ thuộc nhau.
- Quản lý cache tốt hơn.
- Cache mount.
- Secret mount.
- SSH mount.
- Xuất và nhập cache.
- Multi-platform build.
- Log build rõ ràng hơn.

Trên Docker phiên bản hiện đại và Docker Desktop, BuildKit thường đã được sử dụng mặc định.

Có thể dùng:

```bash
docker buildx build .
```

Hoặc:

```bash
docker build --progress=plain .
```

## 36. Cache mount là gì?

Build cache thông thường lưu kết quả của toàn bộ instruction.

Cache mount tạo một thư mục cache có thể được tái sử dụng giữa các lần chạy instruction.

Điểm quan trọng: instruction cache và cache mount không giống nhau.

- Instruction cache: nếu cache hợp lệ, Docker bỏ qua cả instruction và dùng lại layer đã có.
- Cache mount: nếu instruction vẫn phải chạy lại, thư mục cache bên trong instruction vẫn có dữ liệu cũ để công cụ dùng lại.

Không dùng cache mount:

```dockerfile
RUN mvn package -DskipTests
```

Nếu instruction này mất cache vì source thay đổi, Maven chạy lại trong một filesystem mới của build step. Maven có thể phải tải lại dependency vào `/root/.m2` trong layer tạm của step đó. Nếu layer cũ không được dùng lại, dữ liệu trong `/root/.m2` của layer cũ không giúp gì cho lần chạy mới.

Có dùng cache mount:

Ví dụ Maven:

```dockerfile
# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests
```

Khi instruction chạy, BuildKit gắn một thư mục cache bền vững vào đúng path `/root/.m2`. Maven đọc và ghi dependency ở đó như thư mục bình thường. Khi build kết thúc, nội dung cache không đi vào image cuối, nhưng BuildKit giữ lại để lần build sau mount tiếp.

Nếu bước `RUN` phải chạy lại vì source thay đổi, Maven vẫn có thể dùng dependency đã lưu trong cache mount `/root/.m2`.

So sánh:

```text
Instruction cache hợp lệ:
    Không chạy Maven.

Instruction cache không hợp lệ:
    Maven chạy lại.
    Nhưng cache mount giúp không phải tải lại toàn bộ dependency.
```

Luồng cụ thể:

```text
Lần build 1:
RUN mvn package
-> Maven tải dependency
-> dependency được ghi vào cache mount /root/.m2

Lần build 2, chỉ sửa source:
COPY src ./src bị thay đổi
RUN mvn package phải chạy lại
-> Maven thấy dependency đã có trong /root/.m2
-> chỉ compile/package lại phần cần thiết
```

Hai loại cache bổ sung cho nhau.

Một cách nhớ ngắn:

```text
Instruction cache trả lời câu hỏi:
"Có cần chạy lại bước này không?"

Cache mount trả lời câu hỏi:
"Nếu phải chạy lại, có dữ liệu trung gian nào dùng lại được không?"
```

Vì vậy cache mount đặc biệt hữu ích cho package manager và compiler, nơi một bước có thể phải chạy lại nhưng không nhất thiết phải tải hoặc tính toán lại mọi thứ từ đầu.

## 37. Cache mount cho Maven

Dockerfile:

```dockerfile
# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .

RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Vì sao tách thành hai lệnh Maven?

```text
COPY pom.xml
RUN mvn dependency:go-offline
COPY src
RUN mvn package
```

`pom.xml` thường ít thay đổi hơn source code. Bước `dependency:go-offline` chỉ phụ thuộc vào `pom.xml`, nên khi sửa class Java, Docker vẫn có thể dùng lại instruction cache của bước tải dependency. Bước package vẫn chạy lại, nhưng cache mount `/root/.m2` giúp Maven không phải tải lại dependency.

Không dùng cache mount:

```text
Sửa source
-> COPY src mất cache
-> RUN mvn package chạy lại
-> nếu layer chứa ~/.m2 không còn dùng được, Maven có thể tải lại nhiều dependency
```

Có dùng cache mount:

```text
Sửa source
-> COPY src mất cache
-> RUN mvn package chạy lại
-> Maven dùng lại dependency trong cache mount
-> build chủ yếu tốn thời gian compile/test/package
```

Lợi ích:

- Dependency Maven không trở thành một phần của image runtime vì `/root/.m2` là cache mount của build step, không phải nội dung được copy sang stage runtime.
- Khi source thay đổi, dependency đã tải có thể được dùng lại. Lợi ích lớn nhất nằm ở vòng lặp dev/CI: sửa code nhiều lần nhưng dependency ít đổi.
- Khi `pom.xml` thay đổi, Maven vẫn phải resolve lại dependency, nhưng cache mount vẫn có thể giữ các artifact cũ trùng phiên bản, nên không nhất thiết tải lại từ đầu.
- Build ổn định hơn trong môi trường mạng chậm hoặc registry Maven không ổn định, vì nhiều artifact đã có local.
- CI có thể build nhanh hơn nếu cache BuildKit được giữ trên runner hoặc được import từ cache backend.
- Tách được cache build khỏi image runtime: image chạy thật vẫn gọn, nhưng build vẫn nhanh.

Một luồng thường gặp trong vòng đời Maven:

```text
Lần đầu:
    mvn dependency:go-offline
    -> tải dependency vào cache mount
    mvn package
    -> dùng dependency đã có, compile source

Sửa 1 class Java:
    dependency:go-offline
    -> thường CACHED vì pom.xml không đổi
    mvn package
    -> chạy lại, nhưng đọc dependency từ cache mount

Thêm dependency vào pom.xml:
    dependency:go-offline
    -> chạy lại
    -> dependency cũ dùng lại, dependency mới tải thêm
    mvn package
    -> chạy lại theo dependency mới
```

Nếu project có nhiều module Maven, cần copy các file `pom.xml` theo cấu trúc module trước khi chạy `dependency:go-offline`; nếu chỉ copy root `pom.xml`, Maven có thể thiếu thông tin module và cache dependency không đạt hiệu quả mong muốn.

## 38. Chọn ID cho cache mount

Có thể đặt ID rõ ràng:

```dockerfile
RUN --mount=type=cache,id=maven-repository,target=/root/.m2 \
    mvn package -DskipTests
```

ID hữu ích khi muốn:

- Nhiều bước chia sẻ cùng cache.
- Phân biệt cache giữa các project.
- Phân biệt cache theo phiên bản công cụ hoặc nền tảng.

Ví dụ:

```dockerfile
ARG CACHE_ID=maven-demo

RUN --mount=type=cache,id=${CACHE_ID},target=/root/.m2 \
    mvn package -DskipTests
```

## 39. Cache mount không nằm trong image cuối

Nội dung cache mount chỉ hỗ trợ quá trình build.

Ví dụ:

```dockerfile
RUN --mount=type=cache,target=/root/.m2 \
    mvn package
```

Dependency trong `/root/.m2` không tự động được ghi vào layer của instruction theo cách thư mục thông thường được ghi.

Điều này giúp:

- Cache có thể lớn mà không làm image runtime lớn.
- Không đưa file tải trung gian vào image.

## 40. Chế độ chia sẻ cache

Cache mount hỗ trợ cách chia sẻ khi nhiều build chạy đồng thời.

Ví dụ:

```dockerfile
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn package -DskipTests
```

Các chế độ thường gặp:

- `shared`: nhiều tiến trình có thể dùng đồng thời.
- `private`: mỗi writer có cache riêng.
- `locked`: lần sử dụng khác chờ cho đến khi cache được mở khóa.

Lựa chọn phụ thuộc công cụ có an toàn khi nhiều build cùng ghi vào cache hay không.

## 41. Cache package manager

Ví dụ APT:

```dockerfile
# syntax=docker/dockerfile:1

FROM ubuntu:24.04

RUN rm -f /etc/apt/apt.conf.d/docker-clean \
    && echo 'Binary::apt::APT::Keep-Downloaded-Packages "true";' \
       > /etc/apt/apt.conf.d/keep-cache

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update \
    && apt-get install -y --no-install-recommends curl
```

Với APT, có hai loại dữ liệu đáng chú ý:

- `/var/lib/apt`: metadata về package index sau `apt-get update`.
- `/var/cache/apt`: file `.deb` đã tải về.

Không dùng cache mount, mỗi lần instruction cài package phải chạy lại thì APT có thể phải tải lại package index và file `.deb`. Trong CI runner mới, chuyện này xảy ra rất thường xuyên.

Có dùng cache mount, BuildKit giữ lại metadata và package đã tải. Lần sau nếu instruction phải chạy lại, APT có thể dùng lại phần đã có trong cache mount thay vì tải toàn bộ từ mirror.

`sharing=locked` hữu ích vì nhiều tiến trình APT cùng ghi vào một cache có thể làm lock file hoặc metadata bị tranh chấp. Với `locked`, build sau chờ build trước dùng xong cache rồi mới ghi tiếp.

Ví dụ npm:

```dockerfile
RUN --mount=type=cache,target=/root/.npm \
    npm ci
```

Với npm, thư mục `/root/.npm` chứa tarball/cache package. `npm ci` vẫn cài lại `node_modules` theo `package-lock.json`, nhưng có thể lấy package từ cache local thay vì tải lại từ registry.

Ví dụ Gradle:

```dockerfile
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew build
```

Với Gradle, `/root/.gradle` có thể chứa dependency cache, wrapper distribution và build cache tùy cấu hình. Nếu project dùng Gradle nhiều, cache mount thường giảm đáng kể thời gian tải dependency.

Nguyên tắc chọn cache package manager:

- Cache đúng thư mục package manager thật sự dùng.
- Không cache thư mục output cuối nếu output đó cần nằm trong image theo cách rõ ràng.
- Dùng `sharing=locked` cho tool dễ bị lỗi khi nhiều build cùng ghi metadata.
- Tách cache theo project nếu dependency hoặc quyền truy cập khác nhau.
- Không đặt secret vào thư mục cache.

Một số ví dụ thường gặp:

| Tool | Thư mục cache hay dùng | Ghi chú |
|---|---|---|
| Maven | `/root/.m2` | Chứa artifact dependency và metadata Maven |
| Gradle | `/root/.gradle` | Có thể chứa wrapper, dependency, build cache |
| npm | `/root/.npm` | Cache tarball package, không thay thế `node_modules` |
| pnpm | `/root/.local/share/pnpm/store` hoặc store đã cấu hình | Nên khớp với cấu hình `pnpm store path` |
| pip | `/root/.cache/pip` | Cache wheel/download, vẫn nên pin dependency |
| apt | `/var/cache/apt`, `/var/lib/apt` | Cần cấu hình giữ package cache trong image Ubuntu/Debian |

Cache package manager làm build nhanh hơn, nhưng không thay thế lock file. Nếu không có lock file hoặc version rõ ràng, build có thể nhanh nhưng vẫn khó tái tạo.

---

## 42. Secret mount trong lúc build

Không nên:

```dockerfile
ARG MAVEN_PASSWORD
RUN echo "$MAVEN_PASSWORD"
```

Thay vào đó, dùng BuildKit secret:

```dockerfile
# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src

RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    mvn package -DskipTests
```

Build:

```bash
docker build \
  --secret id=maven_settings,src="$HOME/.m2/settings.xml" \
  -t demo-app .
```

PowerShell:

```powershell
docker build `
  --secret "id=maven_settings,src=$HOME\.m2\settings.xml" `
  -t demo-app .
```

Secret mount:

- Chỉ xuất hiện trong instruction cần nó.
- Không được copy tự động vào image.
- An toàn hơn `ARG` hoặc `COPY` secret vào image.

Lưu ý: nếu lệnh chủ động copy secret sang nơi khác, secret vẫn có thể bị ghi vào layer. Công cụ không thể bảo vệ khỏi một Dockerfile cố tình làm lộ secret.

## 43. SSH mount

Khi build cần clone repository riêng tư qua SSH:

```dockerfile
# syntax=docker/dockerfile:1

FROM alpine:3.22

RUN apk add --no-cache git openssh-client

RUN --mount=type=ssh \
    git clone git@github.com:company/private-repo.git /src
```

Build:

```bash
docker build --ssh default .
```

SSH mount chuyển tiếp quyền truy cập cần thiết trong lúc build mà không copy private key vào image.

---

# Phần VI: Xuất và nhập build cache

## 44. Vì sao CI thường không có cache?

Máy developer thường build nhiều lần trên cùng một Docker host nên cache local còn tồn tại.

CI có thể:

- Tạo runner mới cho mỗi job.
- Xóa disk sau khi job kết thúc.
- Chạy build trên nhiều máy khác nhau.

Kết quả:

```text
Build local: nhanh
Build CI: luôn tải và build lại
```

Để giải quyết, BuildKit có thể xuất cache ra một nơi dùng chung và nhập lại ở lần build sau.

## 45. Cache trong registry

Ví dụ với `docker buildx build`:

```bash
docker buildx build \
  --cache-from type=registry,ref=registry.example.com/team/app:buildcache \
  --cache-to type=registry,ref=registry.example.com/team/app:buildcache,mode=max \
  -t registry.example.com/team/app:1.0.0 \
  --push \
  .
```

Ý nghĩa:

- `--cache-from`: thử lấy cache từ registry.
- `--cache-to`: xuất cache mới lên registry.
- `mode=max`: xuất nhiều thông tin cache hơn để tăng khả năng tái sử dụng.
- `--push`: đẩy image kết quả lên registry.

Cache image và runtime image có thể dùng các reference riêng.

Registry cache phù hợp khi CI runner là máy tạm thời. Thay vì hy vọng cache còn trên disk của runner, pipeline đẩy cache lên registry sau mỗi lần build và lần build sau kéo cache từ registry về.

Luồng chạy:

```text
CI job bắt đầu
-> buildx đọc cache từ registry.example.com/team/app:buildcache
-> Dockerfile được build
-> bước nào khớp cache thì dùng lại
-> bước nào thay đổi thì chạy mới
-> buildx đẩy image runtime app:1.0.0
-> buildx đẩy cache mới vào app:buildcache
```

Nếu không dùng registry cache:

```text
Runner mới
-> không có local build cache
-> tải lại dependency
-> build lại nhiều layer
-> job sau trên runner khác lặp lại từ đầu
```

Nếu có dùng registry cache:

```text
Runner mới
-> import cache từ registry
-> dùng lại layer dependency/base/build step phù hợp
-> chỉ rebuild phần thay đổi
```

Lợi ích so với không dùng:

- CI ổn định tốc độ hơn vì cache không phụ thuộc runner cũ còn sống hay không.
- Nhiều runner có thể chia sẻ cùng cache, phù hợp autoscaling runner.
- Cache có thể dùng xuyên branch, xuyên pipeline, thậm chí xuyên máy developer nếu được cấp quyền.
- `mode=max` lưu nhiều metadata hơn, giúp cache hit tốt hơn cho multi-stage build so với cache tối thiểu.
- Tách runtime image và cache reference giúp deploy chỉ dùng image thật, còn cache phục vụ build pipeline.

Cần lưu ý quyền ghi registry. Không nên để mọi branch hoặc fork không tin cậy ghi đè cache chính của `main`.

Khi dùng registry cache, nên tách hai loại tag:

```text
registry.example.com/team/app:1.0.0       -> image dùng để deploy
registry.example.com/team/app:buildcache  -> cache phục vụ build
```

Không nên deploy từ tag cache. Cache reference có thể chứa metadata và layer phục vụ build, không phải hợp đồng runtime ổn định cho môi trường production.

Một số lưu ý vận hành:

- Registry phải hỗ trợ kiểu cache mà buildx dùng.
- Cache có thể lớn, cần retention policy.
- Build từ fork hoặc branch không tin cậy chỉ nên đọc cache chính, không nên ghi cache chính.
- Nếu build multi-platform, cache nên phân biệt hoặc hỗ trợ đúng platform.
- Khi cache bị nghi ngờ hỏng hoặc nhiễu, có thể đổi cache ref hoặc prune cache thay vì tắt cache vĩnh viễn.

## 46. Inline cache

```bash
docker buildx build \
  --cache-to type=inline \
  --cache-from type=registry,ref=registry.example.com/team/app:latest \
  -t registry.example.com/team/app:latest \
  --push \
  .
```

Inline cache lưu metadata cache cùng image.

Luồng dùng inline cache:

```text
Build lần 1
-> build image app:latest
-> ghi metadata cache vào chính image đó
-> push app:latest

Build lần 2
-> pull/read app:latest làm cache source
-> dùng metadata trong image để quyết định bước nào cache hit
-> push image mới kèm metadata cache mới
```

Ưu điểm:

- Cấu hình đơn giản.
- Dùng cùng image reference.
- Dễ áp dụng cho project nhỏ hoặc pipeline chỉ có một image chính.
- Không cần quản lý thêm tag cache riêng như `app:buildcache`.

Hạn chế:

- Không linh hoạt và đầy đủ bằng cache backend riêng trong một số pipeline phức tạp.
- Cache đi cùng image runtime, nên khó tách chính sách retention giữa image deploy và cache build.
- Thường không tối ưu bằng registry cache riêng khi build nhiều stage, nhiều target hoặc nhiều branch.
- Nếu image reference bị xóa theo retention policy, metadata cache cũng mất theo.

So với không dùng cache export/import, inline cache vẫn giúp CI runner mới có điểm bắt đầu để reuse cache. So với registry cache riêng, nó đơn giản hơn nhưng ít linh hoạt hơn.

Inline cache phù hợp nhất khi:

- Project nhỏ hoặc vừa.
- Pipeline chỉ build một target chính.
- Bạn đã luôn push image sau mỗi build.
- Không muốn quản lý thêm tag cache.

Registry cache riêng phù hợp hơn khi:

- Có nhiều stage hoặc nhiều target.
- Muốn cache nhiều hơn phần image cuối.
- Muốn retention của cache khác retention của image deploy.
- Muốn cache theo branch, service hoặc platform.

## 47. Local cache

Xuất cache ra thư mục:

```bash
docker buildx build \
  --cache-to type=local,dest=./build-cache \
  -t demo-app \
  --load \
  .
```

Nhập lại:

```bash
docker buildx build \
  --cache-from type=local,src=./build-cache \
  -t demo-app \
  --load \
  .
```

Thư mục cache có thể lớn. Không nên commit nó vào Git.

Local cache phù hợp khi muốn giữ cache trong filesystem ngoài Docker builder, ví dụ:

- Chạy build trong script local và muốn chuyển cache giữa các workspace.
- CI có cơ chế lưu/restore thư mục cache riêng.
- Môi trường không có registry cache hoặc không muốn đẩy cache lên registry.

Luồng chạy trong CI có cache thư mục:

```text
Trước build:
-> restore ./build-cache từ CI cache storage

Build:
-> --cache-from type=local,src=./build-cache
-> --cache-to type=local,dest=./build-cache-new

Sau build:
-> lưu ./build-cache-new lại vào CI cache storage
```

Nên ghi cache mới ra thư mục khác rồi thay thế theo cơ chế của CI, vì một số backend local cache không thích ghi đè trực tiếp vào thư mục đang đọc.

So với registry cache:

- Local cache dễ dùng trong môi trường nội bộ, không cần quyền push registry.
- Nhưng khó chia sẻ giữa nhiều runner nếu CI không hỗ trợ lưu/restore thư mục tốt.
- Dọn dẹp thủ công hơn, dễ phình dung lượng nếu không có retention.

Local cache không nên đặt trong build context nếu Dockerfile có `COPY . .`, vì chính thư mục cache đó có thể bị gửi vào context, làm build chậm và làm cache mất hiệu lực. Nếu bắt buộc lưu trong workspace, hãy thêm nó vào `.dockerignore`:

```dockerignore
build-cache
build-cache-new
```

## 48. Cache theo nhánh trong CI

Một chiến lược phổ biến:

```text
Feature branch:
1. Đọc cache của chính branch.
2. Nếu thiếu, đọc thêm cache của main.
3. Ghi cache mới cho feature branch.
```

Mục tiêu:

- Feature branch tận dụng dependency và base layer từ `main`.
- Không để mọi branch ghi đè cùng một cache reference.
- Có chính sách dọn cache cũ.

Nếu mọi branch cùng ghi vào một cache tag như `app:buildcache`, branch thử nghiệm có thể làm cache chính bị nhiễu. Ví dụ một branch đổi base image, đổi package manager hoặc build thêm tool tạm thời; cache đó có thể làm các branch khác mất cache hit hoặc khó debug hơn.

Chiến lược rõ hơn:

```text
main:
  cache-from: app:cache-main
  cache-to:   app:cache-main

feature/login:
  cache-from: app:cache-feature-login
  cache-from: app:cache-main
  cache-to:   app:cache-feature-login
```

Luồng này có nghĩa:

- Feature branch ưu tiên cache của chính nó để tận dụng các lần commit gần nhất trên branch.
- Nếu cache branch chưa có hoặc thiếu layer, nó fallback sang cache `main`.
- Feature branch chỉ ghi cache của nó, không phá cache `main`.
- Khi merge xong, cache branch có thể bị xóa theo retention policy.

Lợi ích so với không chia cache theo nhánh:

- Build đầu tiên của branch vẫn nhanh hơn nhờ cache từ `main`.
- Build các commit tiếp theo trên branch nhanh hơn nhờ cache riêng của branch.
- Cache `main` sạch hơn, đại diện cho trạng thái production/stable.
- Dễ dọn cache cũ theo branch đã đóng hoặc PR đã merge.

Trong repo có nhiều service, nên thêm tên service vào cache key/reference để tránh cache của service này ghi đè service khác.

Ví dụ reference rõ ràng hơn:

```text
app-a:cache-main
app-a:cache-feature-login
app-b:cache-main
app-b:cache-feature-payment
```

Cache tốt là cache có phạm vi rõ ràng. Phạm vi quá rộng dễ nhiễu; phạm vi quá hẹp thì cache hit thấp. Với CI, thường bắt đầu từ `service + branch`, sau đó fallback về `service + main`.

---

# Phần VII: Tối ưu Spring Boot và Maven

## 49. Cách 1: Build JAR ngoài Docker

Pipeline:

```text
Maven trên host/CI -> tạo JAR -> Docker chỉ đóng gói JAR
```

Dockerfile:

```dockerfile
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/springboot-learning.jar app.jar

USER 10001

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Ưu điểm:

- Dockerfile đơn giản.
- Build Maven và test dễ tách thành bước CI riêng.
- Build context có thể rất nhỏ.
- Tận dụng trực tiếp cache dependency của CI hoặc máy developer, ví dụ cache `~/.m2` do CI restore trước khi chạy `mvn package`.
- Dễ chạy test, static analysis, coverage và publish test report bằng công cụ CI trước khi build image.
- Docker build rất nhanh vì chỉ copy một file JAR vào image runtime.
- Phù hợp khi tổ chức đã chuẩn hóa pipeline Maven bên ngoài Docker.

Nhược điểm:

- Môi trường build JAR nằm ngoài Docker.
- Cần đảm bảo Java và Maven trên CI nhất quán.
- Không tận dụng multi-stage build để đóng gói toàn bộ quy trình.
- Nếu máy developer và CI khác phiên bản JDK/Maven, có thể sinh artifact khác nhau hoặc lỗi chỉ xuất hiện ở một môi trường.
- Dockerfile không mô tả đầy đủ cách tạo artifact; người đọc phải xem thêm pipeline CI.
- Cần kiểm soát tên JAR và chắc chắn file `target/springboot-learning.jar` đã tồn tại trước khi chạy `docker build`.

Luồng chạy:

```text
mvn clean package
-> tạo target/springboot-learning.jar

docker build
-> dùng image JRE
-> copy JAR vào /app/app.jar
-> tạo runtime image
```

So với build Maven trong Docker, cách này nhanh và đơn giản nếu CI đã cache Maven tốt. Đổi lại, tính nhất quán phụ thuộc vào môi trường bên ngoài Docker.

Khi dùng cách này, pipeline thường nên có thứ tự rõ ràng:

```text
1. Checkout source
2. Restore cache Maven của CI
3. Chạy test
4. Chạy mvn package
5. Build Docker image chỉ từ JAR đã tạo
6. Push image
```

Nếu bước 4 chưa chạy mà đã chạy `docker build`, Docker sẽ báo không tìm thấy JAR. Vì Dockerfile không mô tả cách tạo JAR, trách nhiệm đó nằm ở pipeline.

Nên dùng cách build JAR ngoài Docker khi:

- Team đã có CI Maven chuẩn, cache `~/.m2` tốt và test report đầy đủ.
- Muốn Dockerfile runtime thật ngắn, dễ đọc.
- Không cần build image từ source ở mọi môi trường.
- Build image chỉ là bước đóng gói artifact đã được kiểm thử.

Không nên dùng cách này nếu mục tiêu là "clone repo rồi chỉ cần `docker build` là ra image", vì Dockerfile không tự build artifact.

## 50. Cách 2: Build Maven trong Docker

```dockerfile
# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .

RUN --mount=type=cache,id=maven-cache,target=/root/.m2 \
    mvn dependency:go-offline

COPY src ./src

RUN --mount=type=cache,id=maven-cache,target=/root/.m2 \
    mvn package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

USER 10001

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Ưu điểm:

- Môi trường build nhất quán.
- Không cần cài Maven trên host.
- Cache Maven được tái sử dụng.
- Image cuối không chứa Maven và source.
- Dockerfile mô tả trọn quy trình từ source đến runtime image, dễ tái tạo trên máy khác.
- Build dùng đúng image Maven/JDK đã khai báo, giảm khác biệt giữa máy developer và CI.
- Kết hợp được multi-stage build và cache mount: build nhanh hơn nhưng runtime image vẫn gọn.
- Dễ mở rộng cho pipeline multi-platform hoặc buildx vì toàn bộ build nằm trong Docker.

Luồng chạy:

```text
docker buildx build
-> stage build dùng maven:3.9-eclipse-temurin-21
-> mount cache /root/.m2
-> tải dependency nếu cache chưa có
-> copy source và package JAR
-> stage runtime dùng eclipse-temurin:21-jre
-> copy JAR sang runtime image
```

So với build JAR ngoài Docker:

- Nhất quán hơn vì JDK/Maven nằm trong Dockerfile.
- Ít phụ thuộc setup của host/CI hơn.
- Thường chậm hơn ở lần đầu vì phải build trong Docker và cần chuẩn bị cache mount.
- Cần BuildKit nếu muốn dùng `RUN --mount=type=cache`.

Nếu CI có registry cache hoặc local cache cho BuildKit, cách này thường cân bằng tốt giữa tính tái tạo và tốc độ.

Khi dùng cách này, Dockerfile trở thành tài liệu build đầy đủ hơn:

```text
Input:  source code + pom.xml
Output: runtime image chạy được
```

Người khác không cần biết máy host có Maven hay không. CI runner cũng không cần cài JDK/Maven ngoài Docker. Điều cần chuẩn bị là Docker/BuildKit và quyền pull base image.

Đổi lại, cần chú ý:

- Lần build đầu có thể chậm vì phải tải base image Maven và dependency.
- Nếu CI runner luôn mới, nên dùng registry cache hoặc local cache cho BuildKit.
- Nếu project cần Maven settings private repository, nên dùng BuildKit secret mount thay vì copy `settings.xml`.
- Nếu build cần test integration phụ thuộc service ngoài, nên cân nhắc tách stage test hoặc chạy test ngoài Docker tùy pipeline.

## 51. Vấn đề với wildcard JAR

Instruction:

```dockerfile
COPY --from=build /workspace/target/*.jar app.jar
```

có thể lỗi nếu `target` có nhiều JAR, ví dụ:

- JAR chính.
- Original JAR.
- Sources JAR.
- Javadoc JAR.

Vấn đề không chỉ là lỗi build. Wildcard còn làm Dockerfile kém rõ ràng:

- Nếu có đúng một file khớp, build chạy được.
- Nếu có nhiều file khớp, Docker có thể báo lỗi vì đích `app.jar` không phải directory.
- Nếu plugin đổi tên artifact, Dockerfile vẫn nhìn có vẻ đúng nhưng build fail ở bước copy.
- Người đọc không biết image runtime đang copy artifact nào nếu không kiểm tra cấu hình Maven.

Cách ổn định hơn là cấu hình tên output cố định trong Maven:

```xml
<build>
    <finalName>app</finalName>
</build>
```

Sau đó:

```dockerfile
COPY --from=build /workspace/target/app.jar app.jar
```

Luồng ổn định hơn:

```text
Maven luôn tạo /workspace/target/app.jar
Docker luôn copy đúng /workspace/target/app.jar
Runtime image luôn chạy /app/app.jar
```

Lợi ích:

- Dockerfile rõ ràng và dễ review hơn.
- Tránh lỗi ngẫu nhiên khi thêm plugin tạo `sources.jar`, `javadoc.jar` hoặc `original-*.jar`.
- CI fail sớm và dễ hiểu nếu artifact chính không được tạo.
- Dễ kết hợp với Spring Boot layered JAR vì các bước sau có đường dẫn artifact cố định.

Một lựa chọn khác là copy theo tên artifact Maven đã biết:

```dockerfile
COPY --from=build /workspace/target/springboot-learning-0.0.1-SNAPSHOT.jar app.jar
```

Cách này rõ ràng, nhưng mỗi lần đổi version trong Maven có thể phải sửa Dockerfile. Vì vậy với dự án học tập hoặc service nội bộ, cấu hình `<finalName>app</finalName>` thường dễ vận hành hơn.

## 52. Spring Boot layered JAR

Spring Boot executable JAR thường chứa nhiều loại nội dung:

- Dependency ít thay đổi.
- Spring Boot loader.
- Snapshot dependency.
- Class và resource của ứng dụng thay đổi thường xuyên.

Nếu copy toàn bộ JAR vào một layer:

```dockerfile
COPY app.jar app.jar
```

Sửa một class tạo JAR mới và làm layer chứa toàn bộ JAR thay đổi.

Spring Boot hỗ trợ cách tách nội dung JAR thành các layer logic. Tùy phiên bản Spring Boot và công cụ build, có thể dùng layertools hoặc công cụ tương ứng để extract layer.

Ví dụ khái niệm:

```text
Layer 1: dependencies
Layer 2: spring-boot-loader
Layer 3: snapshot-dependencies
Layer 4: application
```

Khi chỉ source ứng dụng thay đổi:

```text
dependencies          -> dùng lại
spring-boot-loader    -> dùng lại
snapshot-dependencies -> có thể dùng lại
application           -> thay đổi
```

Điều này giúp:

- Push image nhanh hơn.
- Pull image cập nhật nhanh hơn.
- Registry lưu ít dữ liệu trùng lặp hơn.

Nếu không dùng layered JAR:

```text
Sửa một class
-> Maven tạo lại app.jar
-> Docker thấy app.jar đổi
-> layer chứa app.jar đổi toàn bộ
-> push/pull lại layer JAR lớn
```

Nếu dùng layered JAR:

```text
Sửa một class
-> layer application đổi
-> layer dependencies có thể giữ nguyên
-> registry và server deploy chỉ cần xử lý phần thay đổi
```

Layered JAR không nhất thiết làm thời gian compile nhanh hơn. Nó chủ yếu tối ưu cách Docker lưu, push và pull nội dung ứng dụng sau khi JAR đã được tạo.

## 53. Ví dụ Dockerfile cho Spring Boot layered JAR

Một mẫu phổ biến:

```dockerfile
# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests

FROM eclipse-temurin:21-jre AS extract

WORKDIR /layers

COPY --from=build /workspace/target/app.jar application.jar

RUN java -Djarmode=tools \
    -jar application.jar extract \
    --layers \
    --destination extracted

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=extract /layers/extracted/dependencies/ ./
COPY --from=extract /layers/extracted/spring-boot-loader/ ./
COPY --from=extract /layers/extracted/snapshot-dependencies/ ./
COPY --from=extract /layers/extracted/application/ ./

USER 10001

ENTRYPOINT ["java", "-jar", "application.jar"]
```

> Cú pháp extract và cách chạy layered JAR có thể khác theo phiên bản Spring Boot. Hãy kiểm tra phiên bản plugin Spring Boot của dự án trước khi áp dụng nguyên mẫu.

Điểm chính của ví dụ là mỗi nhóm nội dung được `COPY` riêng để tạo ranh giới cache tốt hơn.

Khi áp dụng thật, cần kiểm tra hai thứ:

1. JAR có bật layer metadata không.
2. Cách chạy sau khi extract có đúng với phiên bản Spring Boot đang dùng không.

Một số phiên bản Spring Boot dùng `jarmode=layertools`, một số phiên bản mới có cơ chế tools khác. Vì vậy không nên copy nguyên mẫu mà không chạy thử `java -jar app.jar list` hoặc lệnh tương ứng để xem các layer thực tế.

## 54. Có nên luôn tách Spring Boot JAR thành layer?

Không nhất thiết.

Nên cân nhắc khi:

- JAR lớn.
- Dependency ít đổi nhưng source đổi thường xuyên.
- Image được push và pull nhiều lần.
- CI/CD cần giảm thời gian truyền image.
- Có nhiều service dùng dependency tương tự.

Có thể không cần khi:

- Ứng dụng rất nhỏ.
- Build và deploy ít.
- Dockerfile đơn giản quan trọng hơn vài giây tối ưu.
- Nền tảng đã dùng buildpack và tự tối ưu layer.

Quyết định thực tế:

```text
Nếu bottleneck là compile/test Maven:
    Ưu tiên cache mount, tách dependency, CI cache.

Nếu bottleneck là push/pull image:
    Cân nhắc Spring Boot layered JAR.

Nếu bottleneck là image quá lớn:
    Ưu tiên multi-stage, runtime image nhỏ, bỏ file thừa.
```

Không nên dùng layered JAR chỉ vì "nghe tối ưu". Hãy dùng khi nó giải quyết đúng vấn đề: layer JAR lớn thay đổi quá thường xuyên làm registry/deploy chậm.

---

# Phần VIII: Các hiểu nhầm thường gặp

## 55. “Càng ít layer thì image càng tốt”

Không hoàn toàn đúng.

Ít layer có thể giúp tránh lưu file tạm ở layer trước. Tuy nhiên, ghép mọi thứ vào một instruction lớn có thể:

- Làm cache thô hơn.
- Chỉ cần thay đổi nhỏ cũng phải chạy lại nhiều công việc.
- Dockerfile khó đọc.
- Khó debug.

Nên tạo layer theo ranh giới logic:

- Cài package hệ thống.
- Tải dependency ứng dụng.
- Copy source.
- Build ứng dụng.

Tối ưu mục tiêu thực tế:

```text
Image đủ nhỏ
+ cache hiệu quả
+ Dockerfile dễ hiểu
+ build có thể tái tạo
```

Ví dụ, hai instruction này không nhất thiết tốt hơn chỉ vì ít layer:

```dockerfile
RUN apt-get update \
    && apt-get install -y curl \
    && mvn dependency:go-offline \
    && mvn package \
    && rm -rf /var/lib/apt/lists/*
```

Nó trộn package hệ thống, dependency Maven và build ứng dụng vào một bước. Sửa source có thể làm nhiều việc không liên quan phải chạy lại. Tốt hơn là ghép những việc cần chung layer, nhưng vẫn tách theo ranh giới cache hợp lý.

## 56. “Xóa file ở instruction sau sẽ giảm image”

Sai trong phần lớn trường hợp.

```dockerfile
RUN download-big-file
RUN rm big-file
```

File vẫn tồn tại trong layer trước.

Hãy tạo và xóa file tạm trong cùng instruction:

```dockerfile
RUN download-big-file \
    && use-big-file \
    && rm big-file
```

## 57. “Dùng `--no-cache` là image chắc chắn mới nhất”

Không đủ.

```bash
docker build --no-cache .
```

Lệnh này không dùng build cache cho các bước, nhưng base image local có thể vẫn chưa phải bản mới nhất của tag.

Muốn chủ động kiểm tra base image:

```bash
docker build --pull --no-cache .
```

Tuy vậy, repository package và nguồn bên ngoài vẫn có cơ chế cache, mirror hoặc version riêng.

## 58. “Tag base image cố định nghĩa là nội dung cố định”

Tag:

```dockerfile
FROM eclipse-temurin:21-jre
```

có thể được cập nhật theo thời gian.

Muốn build có tính tái tạo cao hơn, có thể pin bằng digest:

```dockerfile
FROM eclipse-temurin:21-jre@sha256:<digest>
```

Ưu điểm:

- Cùng digest cho cùng nội dung.
- Tránh tag thay đổi ngoài ý muốn.

Đổi lại:

- Không tự nhận bản vá bảo mật mới.
- Cần quy trình cập nhật digest chủ động.

Giải pháp tốt là pin digest và dùng công cụ tự động tạo pull request khi base image có bản mới.

## 59. “Cache luôn an toàn để chia sẻ”

Không phải mọi cache đều nên chia sẻ tùy ý.

Rủi ro:

- Cache chứa artifact từ source không tin cậy.
- Cache bị ghi đồng thời không an toàn.
- Cache có thể vô tình chứa thông tin nhạy cảm nếu Dockerfile ghi secret vào layer.
- Cache khác kiến trúc hoặc cấu hình không tương thích.

Nên:

- Phân tách cache theo project hoặc trust boundary.
- Không đưa secret vào layer/cache.
- Giới hạn quyền ghi cache registry.
- Xóa cache cũ theo chính sách.

---

# Phần IX: Debug build cache

## 60. Hiển thị log build đầy đủ

```bash
docker build --progress=plain -t demo-app .
```

Với buildx:

```bash
docker buildx build --progress=plain -t demo-app --load .
```

Tìm các trạng thái như:

```text
CACHED
DONE
transferring context
```

## 61. Build không dùng cache

```bash
docker build --no-cache -t demo-app .
```

Chỉ nên dùng khi:

- Debug vấn đề cache.
- Cần làm mới toàn bộ dependency bên ngoài.
- Kiểm tra build có thật sự tái tạo được từ đầu.
- Pipeline định kỳ cần clean build.

Không nên dùng `--no-cache` cho mọi build hằng ngày vì làm mất lợi ích lớn nhất của Docker build.

Khi nghi cache sai, hãy dùng `--no-cache` như công cụ kiểm chứng:

```text
Build có cache lỗi, build --no-cache thành công:
    Có thể Dockerfile phụ thuộc dữ liệu bên ngoài nhưng cache không biết.

Build --no-cache cũng lỗi:
    Vấn đề có thể nằm ở Dockerfile, dependency, network hoặc source hiện tại.
```

Sau khi tìm được nguyên nhân, nên sửa Dockerfile hoặc chiến lược cache thay vì giữ `--no-cache` vĩnh viễn.

## 62. Xóa build cache

Xem dung lượng:

```bash
docker system df
```

Xóa build cache không dùng:

```bash
docker builder prune
```

Với buildx:

```bash
docker buildx prune
```

Xóa mạnh hơn:

```bash
docker builder prune --all
```

Lệnh này có thể làm các lần build sau chậm đáng kể vì phải tạo lại cache.

## 63. Vì sao một bước mất cache ngoài dự kiến?

Kiểm tra:

1. File nào được `COPY` trước bước đó?
2. Có dùng `COPY . .` không?
3. `.dockerignore` có loại file log, `.git`, output build và file IDE không?
4. Build arg có thay đổi không?
5. Base image có thay đổi không?
6. Platform build có thay đổi không?
7. Dockerfile có thay đổi instruction hoặc whitespace liên quan không?
8. Cache có còn tồn tại trên builder hiện tại không?
9. CI có chạy trên runner mới không?
10. Cache từ registry có được import đúng không?

Cách debug có hệ thống:

```text
1. Chạy build với --progress=plain.
2. Tìm bước đầu tiên không CACHED ngoài dự kiến.
3. Nhìn các instruction trước bước đó.
4. Kiểm tra file nào được COPY vào trước đó.
5. Kiểm tra .dockerignore.
6. Nếu ở CI, kiểm tra cache-from/cache-to có thật sự chạy không.
```

Đừng bắt đầu từ bước cuối bị chậm. Hãy tìm bước đầu tiên mất cache, vì các bước sau thường chỉ là hậu quả.

## 64. Vì sao cache cũ vẫn được dùng dù package bên ngoài đã cập nhật?

Ví dụ:

```dockerfile
RUN apk add curl
```

Docker cache dựa trên đầu vào build, không theo dõi liên tục repository bên ngoài.

Cách xử lý:

- Pin version package nếu cần tái tạo.
- Chủ động build với `--no-cache` theo lịch.
- Cập nhật base image.
- Dùng build arg làm cache-busting có kiểm soát.

Ví dụ:

```dockerfile
ARG PACKAGE_REFRESH=unknown
RUN echo "refresh=$PACKAGE_REFRESH" \
    && apk add --no-cache curl
```

Build:

```bash
docker build \
  --build-arg PACKAGE_REFRESH=2026-06-25 \
  .
```

Không nên dùng giá trị ngẫu nhiên ở mọi build nếu không có lý do, vì nó phá cache hoàn toàn.

## 65. Kiểm tra image sau khi build

```bash
docker image ls demo-app
docker history demo-app
docker image inspect demo-app
```

Chạy thử:

```bash
docker run --rm demo-app
```

Kiểm tra file:

```bash
docker run --rm \
  --entrypoint sh \
  demo-app \
  -c "find /app -maxdepth 2 -type f"
```

Nếu image không có shell, có thể debug stage build riêng hoặc dùng công cụ phân tích image phù hợp.

---

# Phần X: Bảo mật và tính tái tạo

## 66. Không copy secret vào image rồi xóa

Dockerfile nguy hiểm:

```dockerfile
COPY settings.xml /root/.m2/settings.xml
RUN mvn package
RUN rm /root/.m2/settings.xml
```

Secret vẫn có thể tồn tại trong layer của bước `COPY`.

Ngay cả cách này cũng không an toàn:

```dockerfile
COPY settings.xml /root/.m2/settings.xml
RUN mvn package && rm /root/.m2/settings.xml
```

Vì `COPY` đã tạo layer trước đó.

Hãy dùng secret mount:

```dockerfile
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    mvn package
```

## 67. Không dùng `COPY . .` trước khi xử lý secret

Nếu context chứa:

```text
.env
private-key.pem
credentials.json
```

thì:

```dockerfile
COPY . .
```

có thể đưa secret vào image, kể cả ứng dụng không dùng chúng.

Phòng tránh:

- Thêm secret vào `.dockerignore`.
- Chỉ `COPY` đúng file cần thiết.
- Quét image trong CI.
- Dùng secret manager và BuildKit secret mount.

## 68. Pin dependency

Build có thể dùng cache tốt nhưng không có tính tái tạo nếu dependency không được khóa phiên bản.

Ví dụ không ổn định:

```dockerfile
RUN npm install some-package@latest
```

Hoặc:

```dockerfile
RUN apt-get install -y some-package
```

khi không có chính sách version phù hợp.

Nên:

- Dùng lock file.
- Pin version quan trọng.
- Pin base image bằng digest khi cần.
- Có quy trình cập nhật dependency và bản vá.

Cache giúp build nhanh. Pin version giúp build có thể dự đoán. Hai việc này khác nhau.

Một build tốt cần cả hai:

```text
Cache tốt:
    Lặp lại công việc ít hơn.

Pin tốt:
    Khi phải lặp lại, kết quả vẫn dự đoán được.
```

Nếu chỉ có cache mà không pin version, build hôm nay có thể dùng cache và thành công, nhưng build sạch tuần sau có thể lấy dependency khác. Nếu chỉ pin mà không cache, build có thể đúng nhưng chậm. Hai mục tiêu này bổ sung cho nhau.

---

# Phần XI: Bài thực hành

## 69. Bài 1: Quan sát cache cơ bản

Tạo Dockerfile:

```dockerfile
FROM alpine:3.22

RUN echo "Bước ít thay đổi"

COPY message.txt /message.txt

CMD ["cat", "/message.txt"]
```

Tạo `message.txt`:

```text
Phiên bản 1
```

Build lần đầu:

```bash
docker build --progress=plain -t layer-demo:1 .
```

Build lần hai:

```bash
docker build --progress=plain -t layer-demo:1 .
```

Sửa `message.txt` thành:

```text
Phiên bản 2
```

Build lại và quan sát:

- Bước nào dùng cache?
- Bước nào chạy lại?
- Vì sao?

## 70. Bài 2: Quan sát file bị xóa nhưng image vẫn lớn

Tạo file lớn để thử nghiệm:

Linux hoặc macOS:

```bash
dd if=/dev/zero of=large-file.bin bs=1M count=100
```

PowerShell:

```powershell
$file = [System.IO.File]::Create("large-file.bin")
$file.SetLength(100MB)
$file.Close()
```

Dockerfile chưa tốt:

```dockerfile
FROM alpine:3.22
COPY large-file.bin /large-file.bin
RUN rm /large-file.bin
```

Build:

```bash
docker build -t layer-delete-demo .
docker image ls layer-delete-demo
docker history layer-delete-demo
```

Quan sát:

- File không tồn tại trong container cuối.
- Image vẫn chứa layer có file lớn.

Sau bài thực hành, xóa file thử nghiệm nếu không còn cần.

## 71. Bài 3: So sánh thứ tự `COPY`

Dockerfile A:

```dockerfile
FROM node:24
WORKDIR /app
COPY . .
RUN npm ci
```

Dockerfile B:

```dockerfile
FROM node:24
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
```

Thực hiện:

1. Build cả hai.
2. Sửa một file source.
3. Build lại.
4. So sánh bước `npm ci`.

## 72. Bài 4: Cache Maven bằng BuildKit

Dockerfile:

```dockerfile
# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests
```

Build:

```bash
docker build --progress=plain --target build .
```

Sửa một file Java rồi build lại.

Quan sát:

- Instruction Maven phải chạy lại.
- Dependency trong cache mount không cần tải lại toàn bộ.

## 73. Bài 5: Tối ưu build context

Kiểm tra log:

```bash
docker build --progress=plain .
```

Tìm:

```text
transferring context
```

Sau đó:

1. Thêm `.git`, `target`, log và file IDE vào `.dockerignore`.
2. Build lại.
3. So sánh kích thước context và thời gian build.

---

# Phần XII: Bảng tra cứu nhanh

## 74. Các lệnh thường dùng

| Mục đích | Lệnh |
|---|---|
| Build image | `docker build -t app .` |
| Build và hiện log đầy đủ | `docker build --progress=plain -t app .` |
| Không dùng build cache | `docker build --no-cache -t app .` |
| Kéo lại base image | `docker build --pull -t app .` |
| Xem lịch sử image | `docker history app` |
| Xem metadata image | `docker image inspect app` |
| Xem dung lượng Docker | `docker system df -v` |
| Xóa build cache không dùng | `docker builder prune` |
| Xóa cache buildx | `docker buildx prune` |
| Build một stage | `docker build --target build .` |

## 75. Checklist tối ưu Dockerfile

- Base image có phù hợp và đủ nhỏ không?
- Base image có được cập nhật hoặc pin theo chính sách không?
- Instruction ít thay đổi đã được đặt trước chưa?
- File dependency có được copy trước source code không?
- Có dùng `COPY . .` quá sớm không?
- `.dockerignore` đã loại `.git`, log, output build và secret chưa?
- File tạm có được xóa trong cùng instruction tạo ra nó không?
- Build và runtime đã được tách bằng multi-stage build chưa?
- Package manager có dùng cache mount không?
- Secret có được truyền bằng secret mount không?
- CI có import và export cache không?
- Image cuối có chứa compiler, source hoặc cache không cần thiết không?
- Build sạch với `--no-cache` có thành công không?
- Đã kiểm tra `docker history` và kích thước image chưa?

## 76. Quy tắc ghi nhớ

```text
Layer dưới không bị sửa.
Thay đổi được ghi thành layer mới.
Xóa ở layer sau không xóa dữ liệu khỏi layer trước.
```

```text
Bước ít thay đổi đặt trước.
Bước hay thay đổi đặt sau.
Copy dependency trước source code.
```

```text
Instruction cache giúp bỏ qua cả bước.
Cache mount giúp bước chạy lại nhưng không tải lại mọi thứ.
```

```text
Build cache làm build nhanh.
Multi-stage làm image runtime gọn.
.dockerignore làm context nhỏ và an toàn hơn.
```

---

## 77. Kết luận

Docker image là một tập hợp các layer bất biến được xếp chồng lên nhau. Khi container chạy, Docker thêm writable layer riêng ở trên image.

Build cache tận dụng lại kết quả của các bước cũ. Cache hiệu quả nhất khi Dockerfile được sắp xếp theo mức độ thay đổi:

```text
Base image
    |
Package hệ thống
    |
File dependency
    |
Tải dependency
    |
Source code
    |
Build ứng dụng
```

Với ứng dụng Spring Boot sử dụng Maven, một hướng triển khai tốt thường gồm:

- Multi-stage build.
- Copy `pom.xml` trước source.
- Cache mount cho `/root/.m2`.
- Runtime image chỉ chứa JRE và JAR.
- `.dockerignore` loại file không cần thiết.
- Secret mount cho Maven settings hoặc credential.
- Registry cache cho pipeline CI dùng runner tạm thời.

Không cần tối ưu Dockerfile đến mức khó đọc. Một Dockerfile tốt là Dockerfile:

- Build đủ nhanh.
- Image đủ gọn.
- Không làm lộ secret.
- Có thể tái tạo.
- Và người khác trong đội có thể hiểu, sửa, debug được.
