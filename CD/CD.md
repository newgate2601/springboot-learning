# Continuous Delivery và Continuous Deployment (CD) từ cơ bản tới nâng cao

## 1. CD là gì?

CD là viết tắt thường gặp của hai khái niệm gần nhau nhưng không hoàn toàn giống nhau:

- Continuous Delivery: phần mềm sau khi qua CI luôn ở trạng thái sẵn sàng để triển khai. Việc triển khai lên production có thể cần người phê duyệt thủ công.
- Continuous Deployment: phần mềm sau khi qua pipeline và đạt đủ điều kiện sẽ được tự động triển khai lên production mà không cần người bấm nút.

Nói ngắn gọn:

```text
CI kiểm tra code có đáng tin không.
Continuous Delivery chuẩn bị để release bất cứ lúc nào.
Continuous Deployment tự động release khi đủ điều kiện.
```

CD không chỉ là bước `deploy` trong pipeline. CD là toàn bộ cách một team đưa phần mềm từ source code tới môi trường chạy thật một cách lặp lại được, kiểm soát được và có thể rollback khi có sự cố.

## 2. CD giải quyết vấn đề gì?

Nếu không có CD, việc release thường gặp các vấn đề:

- Deploy bằng tay, mỗi người làm một kiểu.
- Thiếu checklist, dễ quên bước quan trọng.
- Không biết chính xác phiên bản nào đang chạy ở môi trường nào.
- Khó rollback khi production lỗi.
- Môi trường staging khác production quá nhiều.
- Secret bị copy thủ công, dễ lộ hoặc dùng nhầm.
- Deploy phụ thuộc vào một vài người có kinh nghiệm.
- Release lớn, ít diễn ra, rủi ro cao.

CD giúp:

- Tự động hóa các bước lặp lại.
- Chuẩn hóa quy trình deploy.
- Giảm sai sót do thao tác tay.
- Tăng khả năng truy vết: commit nào, artifact nào, ai approve, deploy lúc nào.
- Giảm rủi ro bằng rollout từng phần, smoke test, health check và rollback.
- Tạo nền tảng để release thường xuyên hơn.

## 3. CI và CD khác nhau thế nào?

CI tập trung vào kiểm tra code:

```text
Code change
  -> compile
  -> unit test
  -> integration test
  -> lint
  -> security scan
  -> build artifact
```

CD tập trung vào đưa artifact đã kiểm tra tới môi trường chạy:

```text
Artifact
  -> publish
  -> deploy dev
  -> deploy staging
  -> smoke test
  -> approval
  -> deploy production
  -> monitor
  -> rollback nếu cần
```

CI trả lời câu hỏi:

> Code này có đủ tốt để tích hợp vào nhánh chính không?

CD trả lời câu hỏi:

> Artifact này có thể được triển khai an toàn tới môi trường thật không?

Một nguyên tắc rất quan trọng: CD tốt phải dựa trên CI tốt. Nếu CI không đủ kiểm tra, CD chỉ giúp đưa lỗi lên production nhanh hơn.

## 4. Continuous Delivery và Continuous Deployment khác nhau thế nào?

### 4.1. Continuous Delivery

Continuous Delivery nghĩa là mỗi thay đổi sau khi qua pipeline đều sẵn sàng để release. Tuy nhiên, bước production thường cần một hành động thủ công như:

- Release manager bấm approve.
- Product owner xác nhận thời điểm release.
- Team vận hành kiểm tra cửa sổ bảo trì.
- Security hoặc compliance phê duyệt.

Luồng phổ biến:

```text
Merge vào main
  -> CI pass
  -> build artifact
  -> deploy staging
  -> smoke test
  -> manual approval
  -> deploy production
```

Continuous Delivery phù hợp khi:

- Hệ thống có rủi ro cao.
- Production cần kiểm soát thời điểm thay đổi.
- Công ty có quy trình audit hoặc compliance.
- Team chưa đủ tự tin để deploy production tự động hoàn toàn.

### 4.2. Continuous Deployment

Continuous Deployment nghĩa là nếu pipeline pass, thay đổi sẽ tự động lên production.

Luồng phổ biến:

```text
Merge vào main
  -> CI pass
  -> build artifact
  -> deploy staging
  -> automated test
  -> deploy production
  -> post-deploy verification
```

Continuous Deployment phù hợp khi:

- Test tự động đủ mạnh.
- Rollback hoặc roll-forward nhanh.
- Hệ thống có observability tốt.
- Thay đổi nhỏ, release thường xuyên.
- Team có khả năng xử lý sự cố tốt.

Continuous Deployment không có nghĩa là deploy bừa. Nó cần nhiều lớp bảo vệ hơn, không ít hơn.

## 5. Các thành phần cơ bản của CD

Một hệ thống CD thường gồm:

```text
Source Code
  -> CI Pipeline
  -> Artifact
  -> Artifact Registry
  -> Deployment Pipeline
  -> Environment
  -> Verification
  -> Monitoring
  -> Rollback
```

### 5.1. Source Code

Source code là nơi bắt đầu của thay đổi. CD thường chạy khi:

- Merge vào `main`.
- Tạo Git tag.
- Tạo release trên GitHub/GitLab.
- Chạy workflow thủ công.
- Pipeline CI hoàn thành.

Không phải thay đổi nào cũng cần deploy production ngay. Ví dụ sửa tài liệu có thể không cần chạy CD.

### 5.2. Artifact

Artifact là kết quả build được triển khai. Ví dụ:

- File `.jar` của Spring Boot.
- Docker image.
- File `.war`.
- Binary của Go.
- Bundle frontend trong thư mục `dist`.
- Helm chart.
- Package được publish lên registry.

Trong CD hiện đại, artifact nên được build một lần rồi promote qua nhiều môi trường.

```text
Build once
  -> deploy dev
  -> deploy staging
  -> deploy production
```

Không nên build riêng một artifact mới cho production nếu artifact đã test ở staging là artifact khác. Làm vậy khiến kết quả test staging mất giá trị.

### 5.3. Artifact Registry

Artifact registry là nơi lưu artifact. Ví dụ:

- Docker Hub.
- GitHub Container Registry.
- GitLab Container Registry.
- AWS ECR.
- Azure Container Registry.
- Google Artifact Registry.
- Nexus Repository.
- JFrog Artifactory.

Registry nên lưu metadata để truy vết:

- Git commit SHA.
- Version.
- Build time.
- Pipeline run ID.
- Người hoặc workflow tạo artifact.
- Checksum hoặc signature nếu cần.

### 5.4. Deployment Pipeline

Deployment pipeline là quy trình đưa artifact vào môi trường chạy.

Một pipeline CD thường có các bước:

```text
select artifact
  -> load configuration
  -> authenticate
  -> deploy
  -> wait for rollout
  -> run smoke test
  -> notify result
```

Pipeline CD nên rõ ràng:

- Deploy artifact nào?
- Deploy tới môi trường nào?
- Dùng configuration nào?
- Cần secret nào?
- Điều kiện pass/fail là gì?
- Rollback bằng cách nào?

### 5.5. Environment

Environment là môi trường chạy ứng dụng. Các môi trường phổ biến:

- Local: máy developer.
- Dev: môi trường phát triển chung.
- Test hoặc QA: môi trường kiểm thử.
- Staging: mô phỏng production càng gần càng tốt.
- Production: môi trường phục vụ người dùng thật.

Ví dụ luồng promotion:

```text
dev
  -> test
  -> staging
  -> production
```

Không phải hệ thống nào cũng cần nhiều môi trường. Dự án nhỏ có thể chỉ cần staging và production. Điều quan trọng là mỗi môi trường có mục đích rõ ràng.

### 5.6. Configuration

Configuration là phần thay đổi theo môi trường, ví dụ:

- Database URL.
- Redis host.
- API endpoint bên ngoài.
- Feature flag.
- Logging level.
- Resource limit.
- Domain name.

Nguyên tắc:

```text
Artifact giống nhau.
Configuration khác nhau.
```

Ví dụ cùng một Docker image:

```text
my-service:1.4.2-a1b2c3d
```

có thể chạy ở staging với database staging, và chạy ở production với database production. Không cần build lại image.

### 5.7. Secret

Secret là dữ liệu nhạy cảm:

- Database password.
- API key.
- Token deploy.
- Cloud credential.
- Private key.
- Certificate.

Secret không nên:

- Commit vào Git.
- In ra log.
- Truyền qua chat.
- Lưu trong file `.env` không được bảo vệ.
- Hard-code trong YAML pipeline.

Secret nên được quản lý bằng:

- GitHub Actions Secrets.
- GitLab CI/CD Variables.
- Jenkins Credentials.
- Kubernetes Secret.
- HashiCorp Vault.
- AWS Secrets Manager.
- Azure Key Vault.
- Google Secret Manager.

Với cloud hiện đại, nên ưu tiên OIDC để lấy credential tạm thời thay vì lưu access key dài hạn trong CI/CD.

## 6. Vòng đời của một lần deploy

Một lần deploy thường đi qua các bước:

1. Developer merge code hoặc tạo tag release.
2. CI chạy build, test, scan.
3. Pipeline tạo artifact.
4. Artifact được publish lên registry.
5. CD chọn artifact cần deploy.
6. CD lấy configuration và secret đúng môi trường.
7. CD thực hiện deploy.
8. Hệ thống chờ rollout hoàn tất.
9. CD chạy smoke test hoặc health check.
10. Monitoring theo dõi lỗi sau deploy.
11. Nếu ổn, release được ghi nhận thành công.
12. Nếu lỗi, rollback hoặc roll-forward.

Điểm quan trọng: deploy không kết thúc ngay khi lệnh deploy chạy xong. Deploy chỉ nên được coi là thành công khi ứng dụng mới thật sự healthy và phục vụ được request.

## 7. Release, deploy và rollout khác nhau thế nào?

Ba từ này hay bị dùng lẫn nhau.

### 7.1. Release

Release là việc đưa một phiên bản phần mềm vào trạng thái có thể sử dụng hoặc công bố. Release có thể bao gồm:

- Tạo version.
- Tạo release note.
- Publish artifact.
- Bật feature flag.
- Công bố cho người dùng.

### 7.2. Deploy

Deploy là hành động đưa artifact vào môi trường chạy.

Ví dụ:

```bash
kubectl apply -f deployment.yaml
docker compose up -d
helm upgrade --install my-service ./chart
```

### 7.3. Rollout

Rollout là quá trình phiên bản mới dần thay thế phiên bản cũ.

Ví dụ trong Kubernetes:

```text
Replica cũ: 3 pod version 1.0
Replica mới: tăng dần pod version 1.1
Khi pod mới healthy, pod cũ giảm dần
```

Một deploy có thể bắt đầu thành công nhưng rollout thất bại nếu pod mới không healthy.

## 8. Các kiểu trigger trong CD

CD có thể chạy theo nhiều trigger:

| Trigger | Ý nghĩa |
|---|---|
| Push vào `main` | Deploy tự động lên dev hoặc staging |
| Tạo Git tag | Release một phiên bản cụ thể |
| Manual dispatch | Người có quyền chọn môi trường và artifact để deploy |
| CI workflow completed | CD chạy sau khi CI pass |
| Schedule | Deploy hoặc đồng bộ định kỳ |
| GitOps sync | Công cụ như Argo CD tự đồng bộ theo Git |

Với người mới học, nên bắt đầu bằng hai trigger:

- Merge vào `main` để deploy staging.
- Tạo tag `vX.Y.Z` để deploy production.

## 9. Versioning trong CD

Versioning giúp biết chính xác phiên bản nào đang chạy.

Các cách đặt version phổ biến:

- Semantic Versioning: `1.2.3`.
- Git tag: `v1.2.3`.
- Commit SHA: `a1b2c3d`.
- Build number: `1.2.3-build.45`.
- Calendar version: `2026.06.30`.

Với Docker image, nên tránh chỉ dùng `latest` cho production.

Ví dụ tốt:

```text
ghcr.io/example/my-service:1.2.3
ghcr.io/example/my-service:1.2.3-a1b2c3d
ghcr.io/example/my-service:a1b2c3d
```

Ví dụ không tốt nếu dùng một mình:

```text
ghcr.io/example/my-service:latest
```

Vấn đề của `latest` là không nói rõ đang chạy code nào. Khi cần rollback hoặc debug, bạn sẽ rất khó truy vết.

## 10. Build once, deploy many

Đây là nguyên tắc cốt lõi của CD.

Thay vì:

```text
Build artifact cho staging
  -> test staging
Build artifact khác cho production
  -> deploy production
```

nên làm:

```text
Build artifact một lần
  -> deploy artifact đó lên staging
  -> test staging
  -> promote đúng artifact đó lên production
```

Lợi ích:

- Artifact production đã được test ở staging.
- Dễ truy vết.
- Dễ rollback.
- Tránh lỗi do build không tái lập.
- Giảm khác biệt giữa môi trường.

Nếu cần khác biệt giữa staging và production, hãy đưa khác biệt vào configuration, không đưa vào binary hoặc image.

## 11. Deployment target

CD có thể deploy tới nhiều loại target.

### 11.1. Server truyền thống

Ví dụ một máy Linux chạy Spring Boot bằng `systemd`.

Flow:

```text
Upload file jar
  -> update config
  -> restart service
  -> check health endpoint
```

Ví dụ lệnh:

```bash
scp target/app.jar user@server:/opt/my-service/app.jar
ssh user@server "sudo systemctl restart my-service"
curl https://example.com/actuator/health
```

Cách này dễ hiểu nhưng cần kiểm soát tốt quyền SSH, backup file cũ và rollback.

### 11.2. Docker Compose

Phù hợp cho dự án nhỏ hoặc môi trường single server.

Flow:

```text
Pull image mới
  -> docker compose up -d
  -> check container health
```

Ví dụ:

```bash
docker compose pull
docker compose up -d
docker compose ps
```

### 11.3. Kubernetes

Phù hợp cho hệ thống cần scale, rollout, self-healing và quản lý nhiều service.

Flow:

```text
Update image tag
  -> kubectl apply hoặc helm upgrade
  -> wait rollout
  -> smoke test
```

Ví dụ:

```bash
kubectl set image deployment/my-service my-service=ghcr.io/example/my-service:1.2.3
kubectl rollout status deployment/my-service
```

### 11.4. Platform as a Service

Ví dụ:

- Heroku.
- Render.
- Railway.
- Fly.io.
- AWS Elastic Beanstalk.
- Google Cloud Run.
- Azure App Service.

Các nền tảng này giảm phần vận hành hạ tầng, phù hợp khi mới học hoặc dự án nhỏ.

## 12. Các chiến lược deploy phổ biến

### 12.1. Recreate deployment

Recreate là dừng phiên bản cũ rồi khởi động phiên bản mới.

```text
Stop old version
  -> Start new version
```

Ưu điểm:

- Đơn giản.
- Dễ hiểu.
- Phù hợp môi trường dev hoặc hệ thống nhỏ.

Nhược điểm:

- Có downtime.
- Nếu phiên bản mới lỗi, người dùng bị ảnh hưởng ngay.

### 12.2. Rolling update

Rolling update thay thế dần phiên bản cũ bằng phiên bản mới.

```text
Old: v1 v1 v1
Step 1: v2 v1 v1
Step 2: v2 v2 v1
Step 3: v2 v2 v2
```

Ưu điểm:

- Giảm downtime.
- Được Kubernetes hỗ trợ tốt.
- Phù hợp với nhiều hệ thống backend.

Nhược điểm:

- Trong một khoảng thời gian, hai version cùng chạy.
- Cần đảm bảo backward compatibility.
- Database migration phải được thiết kế cẩn thận.

### 12.3. Blue-green deployment

Blue-green dùng hai môi trường gần như giống nhau:

- Blue: version hiện tại.
- Green: version mới.

Luồng:

```text
Traffic đang vào Blue
Deploy version mới vào Green
Test Green
Chuyển traffic từ Blue sang Green
Giữ Blue để rollback nhanh
```

Ưu điểm:

- Rollback nhanh bằng cách chuyển traffic lại.
- Test được môi trường mới trước khi nhận traffic thật.

Nhược điểm:

- Tốn tài nguyên gấp đôi trong lúc deploy.
- Database migration phức tạp hơn.

### 12.4. Canary deployment

Canary đưa version mới tới một phần nhỏ người dùng trước.

Ví dụ:

```text
5% traffic -> version mới
95% traffic -> version cũ
Nếu ổn:
25% -> 50% -> 100%
```

Ưu điểm:

- Giảm rủi ro.
- Phát hiện lỗi trên traffic thật với phạm vi nhỏ.
- Phù hợp hệ thống lớn.

Nhược điểm:

- Cần metrics tốt.
- Cần routing/load balancer hỗ trợ chia traffic.
- Debug có thể khó hơn vì nhiều version cùng chạy.

### 12.5. Feature flag

Feature flag tách deploy code khỏi release tính năng.

Ví dụ:

```text
Deploy code mới lên production nhưng tắt tính năng mới.
Sau đó bật tính năng cho internal user.
Nếu ổn, bật cho 10%, 50%, rồi 100% user.
```

Ưu điểm:

- Rollback tính năng nhanh mà không cần deploy lại.
- Test được code trong production.
- Hỗ trợ release theo nhóm user.

Nhược điểm:

- Code phức tạp hơn.
- Cần dọn flag cũ.
- Flag sai có thể gây lỗi khó hiểu.

## 13. Health check, readiness và smoke test

CD cần biết deploy có thật sự thành công không.

### 13.1. Health check

Health check kiểm tra ứng dụng còn sống không.

Với Spring Boot Actuator:

```text
/actuator/health
```

Kết quả thường có dạng:

```json
{
  "status": "UP"
}
```

Health check nên kiểm tra các thành phần quan trọng nhưng không nên quá nặng. Nếu health check gọi quá nhiều dependency bên ngoài, hệ thống có thể bị restart không cần thiết.

### 13.2. Readiness check

Readiness trả lời câu hỏi:

> Ứng dụng đã sẵn sàng nhận traffic chưa?

Ứng dụng có thể đã start nhưng chưa sẵn sàng vì:

- Chưa connect database.
- Chưa load cache.
- Chưa migrate xong.
- Chưa warm up.

Kubernetes dùng readiness probe để quyết định pod có được đưa vào Service hay không.

### 13.3. Smoke test

Smoke test là bộ test nhỏ chạy sau deploy để kiểm tra chức năng sống còn.

Ví dụ:

- Gọi health endpoint.
- Gọi API login test.
- Gọi API đọc dữ liệu cơ bản.
- Kiểm tra trang chủ trả HTTP 200.
- Kiểm tra một transaction đơn giản ở môi trường staging.

Smoke test không thay thế unit test hay integration test. Nó là lớp xác nhận nhanh sau deploy.

## 14. Rollback và roll-forward

Khi deploy lỗi, có hai hướng xử lý:

- Rollback: quay về phiên bản trước.
- Roll-forward: deploy phiên bản mới hơn để sửa lỗi.

### 14.1. Rollback

Rollback phù hợp khi:

- Phiên bản trước vẫn tương thích.
- Lỗi nghiêm trọng cần khôi phục nhanh.
- Database chưa thay đổi phá vỡ tương thích.

Ví dụ Kubernetes:

```bash
kubectl rollout undo deployment/my-service
```

Ví dụ Docker image:

```bash
docker compose pull
docker compose up -d
```

với image tag được đổi lại về version trước.

### 14.2. Roll-forward

Roll-forward phù hợp khi:

- Lỗi đã hiểu rõ và có bản fix nhanh.
- Rollback không an toàn vì database đã migrate.
- Phiên bản cũ không còn tương thích.

Trong hệ thống trưởng thành, team thường chuẩn bị cả hai khả năng. Nhưng với database migration, rollback không phải lúc nào cũng dễ, nên cần thiết kế migration theo hướng an toàn.

## 15. Database migration trong CD

Database là phần dễ làm CD phức tạp.

Vấn đề thường gặp:

- Code mới cần cột mới nhưng database chưa có.
- Xóa cột khiến version cũ lỗi.
- Rename cột làm cả version cũ và mới khó tương thích.
- Migration chạy lâu, lock bảng.
- Rollback database khó hơn rollback code.

Nguyên tắc an toàn:

### 15.1. Expand and contract

Thay đổi database nên chia thành nhiều bước.

Ví dụ muốn rename cột `name` thành `full_name`:

```text
Bước 1: Thêm cột full_name, giữ cột name.
Bước 2: Code ghi cả name và full_name.
Bước 3: Backfill dữ liệu từ name sang full_name.
Bước 4: Code đọc full_name.
Bước 5: Sau khi chắc chắn, xóa name ở release sau.
```

Không nên vừa rename cột vừa deploy code chỉ đọc cột mới trong một bước nếu hệ thống cần rolling update.

### 15.2. Migration phải được kiểm tra trước

CI/CD nên:

- Validate migration.
- Chạy migration trên database test.
- Kiểm tra app start được sau migration.
- Đo thời gian migration nếu bảng lớn.
- Có kế hoạch backup trước migration rủi ro.

Với Spring Boot, các công cụ phổ biến:

- Flyway.
- Liquibase.

Ví dụ:

```bash
./mvnw flyway:validate
./mvnw liquibase:validate
```

Tùy project chỉ dùng một trong hai công cụ.

## 16. Approval trong CD

Approval là bước phê duyệt trước hành động rủi ro.

Thường dùng trước:

- Deploy production.
- Chạy database migration production.
- Publish package public.
- Xóa dữ liệu.
- Thay đổi hạ tầng lớn.

Approval nên ghi nhận:

- Ai phê duyệt.
- Phê duyệt lúc nào.
- Artifact/version nào được deploy.
- Môi trường nào được deploy.

Approval không nên thay thế test tự động. Nếu pipeline chưa kiểm tra đủ, việc có người bấm approve cũng không làm release an toàn hơn nhiều.

## 17. Secret và permission trong CD

CD thường có quyền mạnh hơn CI vì nó có thể thay đổi production.

Nguyên tắc:

- Cấp quyền nhỏ nhất có thể.
- Tách secret theo môi trường.
- Token staging không được deploy production.
- Không cấp secret production cho pull request không tin cậy.
- Dùng approval trước khi job có quyền production chạy.
- Rotate secret định kỳ.
- Audit ai có quyền sửa secret.

Ví dụ phân tách:

```text
STAGING_DEPLOY_TOKEN
PRODUCTION_DEPLOY_TOKEN
```

Không nên dùng một token toàn quyền cho mọi môi trường.

## 18. CD với GitHub Actions

GitHub Actions có các khái niệm quan trọng cho CD:

- Workflow: file YAML trong `.github/workflows`.
- Job: nhóm bước chạy trên runner.
- Step: từng lệnh hoặc action.
- Environment: môi trường như `staging`, `production`.
- Secret: dữ liệu nhạy cảm.
- Protection rules: yêu cầu approval trước khi deploy environment.

Ví dụ cấu trúc:

```text
.github/workflows/
  ci.yml
  cd-staging.yml
  cd-production.yml
```

Có thể tách CI và CD thành các file riêng để dễ đọc.

## 19. Ví dụ CD staging cho Spring Boot bằng Docker image

Ví dụ này giả định:

- App Spring Boot đã có Dockerfile.
- Image được push lên GitHub Container Registry.
- Mỗi lần push vào `main` sẽ deploy staging.

```yaml
name: CD Staging

on:
  push:
    branches:
      - main

permissions:
  contents: read
  packages: write

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
          cache: maven

      - name: Build jar
        run: ./mvnw --batch-mode clean package

      - name: Login to GHCR
        run: echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u "${{ github.actor }}" --password-stdin

      - name: Build image
        run: docker build -t ghcr.io/example/my-service:${{ github.sha }} .

      - name: Push image
        run: docker push ghcr.io/example/my-service:${{ github.sha }}

  deploy-staging:
    runs-on: ubuntu-latest
    needs: build-and-push
    environment: staging
    steps:
      - name: Deploy to staging
        run: |
          echo "Deploy image ghcr.io/example/my-service:${{ github.sha }} to staging"

      - name: Smoke test
        run: |
          curl --fail https://staging.example.com/actuator/health
```

Giải thích:

- `on.push.branches.main`: workflow chạy khi có push vào `main`.
- `permissions`: giới hạn quyền workflow.
- `build-and-push`: build `.jar`, build Docker image và push image.
- `${{ github.sha }}`: dùng commit SHA làm image tag để truy vết.
- `needs: build-and-push`: chỉ deploy sau khi build thành công.
- `environment: staging`: gắn job với environment staging.
- `curl --fail`: smoke test fail nếu health endpoint trả lỗi.

Trong thực tế, bước deploy sẽ không chỉ `echo`, mà có thể SSH vào server, gọi Helm, gọi Kubernetes hoặc gọi API của cloud provider.

## 20. Ví dụ CD production bằng Git tag và manual approval

Ví dụ này deploy production khi tạo tag dạng `v1.2.3`.

```yaml
name: CD Production

on:
  push:
    tags:
      - "v*.*.*"

permissions:
  contents: read
  packages: read

jobs:
  deploy-production:
    runs-on: ubuntu-latest
    environment: production
    steps:
      - name: Select version
        run: echo "Deploying version ${{ github.ref_name }}"

      - name: Deploy to production
        run: |
          echo "Deploy version ${{ github.ref_name }} to production"

      - name: Smoke test
        run: |
          curl --fail https://example.com/actuator/health
```

Giải thích:

- `tags: "v*.*.*"`: chỉ chạy khi push tag như `v1.2.3`.
- `environment: production`: có thể cấu hình GitHub Environment để yêu cầu approval.
- `${{ github.ref_name }}`: tên tag, ví dụ `v1.2.3`.
- Production nên dùng version rõ ràng, không dùng `latest`.

Trong GitHub repository, có thể cấu hình:

```text
Settings
  -> Environments
  -> production
  -> Required reviewers
```

Khi đó job production sẽ dừng lại chờ người có quyền approve.

## 21. CD bằng SSH tới server Linux

Với dự án nhỏ, bạn có thể deploy tới một server Linux qua SSH.

Luồng:

```text
Build image
  -> push image
  -> SSH vào server
  -> docker compose pull
  -> docker compose up -d
  -> health check
```

Ví dụ step deploy:

```yaml
- name: Deploy via SSH
  uses: appleboy/ssh-action@v1.0.3
  with:
    host: ${{ secrets.STAGING_HOST }}
    username: ${{ secrets.STAGING_USER }}
    key: ${{ secrets.STAGING_SSH_KEY }}
    script: |
      cd /opt/my-service
      docker compose pull
      docker compose up -d
      docker compose ps
```

Lưu ý:

- SSH key phải để trong secret.
- User deploy không nên là `root` nếu không cần.
- Server cần có quyền pull image từ registry.
- Nên backup hoặc giữ version cũ để rollback.
- Không in secret ra log.

## 22. CD với Kubernetes

Với Kubernetes, CD thường dùng:

- `kubectl`.
- Helm.
- Kustomize.
- Argo CD hoặc Flux theo GitOps.

Ví dụ deploy bằng `kubectl`:

```yaml
- name: Configure kubeconfig
  run: |
    mkdir -p ~/.kube
    echo "${{ secrets.KUBE_CONFIG }}" > ~/.kube/config

- name: Deploy
  run: |
    kubectl set image deployment/my-service my-service=ghcr.io/example/my-service:${{ github.sha }}
    kubectl rollout status deployment/my-service --timeout=120s
```

Giải thích:

- `KUBE_CONFIG`: credential để kết nối cluster, cần bảo vệ kỹ.
- `kubectl set image`: cập nhật image cho Deployment.
- `kubectl rollout status`: chờ rollout hoàn tất.

Trong production, nên cân nhắc OIDC hoặc cloud identity thay vì lưu kubeconfig dài hạn trong secret.

## 23. CD với Helm

Helm giúp đóng gói manifest Kubernetes thành chart.

Ví dụ:

```bash
helm upgrade --install my-service ./helm/my-service \
  --namespace production \
  --set image.repository=ghcr.io/example/my-service \
  --set image.tag=1.2.3-a1b2c3d
```

Ưu điểm:

- Quản lý version release.
- Dễ rollback.
- Tách template và values theo môi trường.
- Phù hợp nhiều service có cấu trúc giống nhau.

Cấu trúc thường gặp:

```text
helm/
  my-service/
    Chart.yaml
    values.yaml
    values-staging.yaml
    values-production.yaml
    templates/
```

Lưu ý: không nên đưa secret thô vào `values.yaml` rồi commit lên Git.

## 24. GitOps trong CD

GitOps là cách quản lý trạng thái triển khai bằng Git.

Thay vì pipeline trực tiếp gọi `kubectl` vào cluster, pipeline sẽ cập nhật repository chứa manifest. Công cụ như Argo CD hoặc Flux sẽ tự đồng bộ cluster theo Git.

Luồng:

```text
App repository
  -> CI build image
  -> push image
  -> update deployment repository
  -> Argo CD sync
  -> Kubernetes cluster
```

Ưu điểm:

- Git là nguồn sự thật.
- Dễ audit thay đổi.
- Dễ rollback bằng Git revert.
- Giảm quyền trực tiếp từ CI vào cluster.
- Phát hiện drift giữa Git và cluster.

Nhược điểm:

- Cần học thêm công cụ.
- Cần tách app repo và deployment repo hợp lý.
- Quy trình ban đầu phức tạp hơn deploy trực tiếp.

## 25. Observability sau deploy

CD không dừng ở deploy. Sau deploy cần quan sát hệ thống.

Các tín hiệu quan trọng:

- Error rate.
- Request latency.
- Request throughput.
- CPU, RAM, disk.
- Pod restart count.
- HTTP 5xx.
- Database connection.
- Queue lag.
- Log lỗi mới.
- Business metrics quan trọng.

Ví dụ sau deploy production, cần theo dõi:

```text
5 phút đầu: lỗi nghiêm trọng, service có lên không.
30 phút đầu: latency, error rate, log bất thường.
24 giờ đầu: business metric, bug report, resource usage.
```

Một pipeline tốt có thể gửi link dashboard vào Slack/Teams sau deploy để team theo dõi nhanh.

## 26. Notification trong CD

CD nên thông báo đúng người, đúng lúc.

Thông báo thường gặp:

- Deploy staging thành công.
- Deploy production đang chờ approval.
- Deploy production thành công.
- Deploy thất bại.
- Rollback đã chạy.

Kênh thông báo:

- Slack.
- Microsoft Teams.
- Email.
- GitHub Deployment status.
- Jira hoặc ticket system.

Không nên gửi quá nhiều thông báo không quan trọng. Nếu mọi thứ đều ồn, sự cố thật sẽ bị bỏ qua.

## 27. Audit và compliance

Trong môi trường doanh nghiệp, CD cần lưu dấu vết:

- Ai deploy?
- Ai approve?
- Deploy version nào?
- Artifact build từ commit nào?
- Test nào đã pass?
- Deploy vào môi trường nào?
- Thời điểm deploy?
- Có rollback không?

Các dữ liệu nên lưu:

- Pipeline log.
- Deployment record.
- Test report.
- Security scan report.
- SBOM.
- Release note.
- Approval record.

Audit không chỉ dành cho compliance. Khi có sự cố, trace tốt giúp team tìm nguyên nhân nhanh hơn.

## 28. Security trong CD

CD có thể là điểm rủi ro lớn vì nó có quyền thay đổi production.

Rủi ro:

- Secret bị lộ trong log.
- Token deploy quá nhiều quyền.
- Runner bị chiếm quyền.
- Workflow từ pull request không tin cậy dùng được secret.
- Image độc hại được deploy.
- Artifact bị thay đổi sau khi build.

Biện pháp:

- Dùng permission tối thiểu.
- Tách environment secret.
- Bật approval cho production.
- Không chạy deploy production từ code chưa được review.
- Scan dependency và container image.
- Ký image nếu cần.
- Pin version action thay vì dùng tham chiếu không rõ ràng.
- Dùng runner tạm thời cho job rủi ro.
- Không in secret ra log.

## 29. SBOM, signing và provenance

Khi hệ thống trưởng thành hơn, CD có thể bổ sung:

- SBOM: danh sách thành phần phần mềm trong artifact.
- Signing: ký artifact hoặc container image.
- Provenance: bằng chứng artifact được build từ source nào, bởi pipeline nào.

Công cụ thường gặp:

- Syft để tạo SBOM.
- Grype hoặc Trivy để scan.
- Cosign để ký container image.
- SLSA framework để quản lý provenance.

Những phần này đặc biệt quan trọng với hệ thống cần bảo mật hoặc compliance cao.

## 30. CD anti-patterns

### 30.1. Deploy bằng tay trên server

SSH vào server rồi copy file thủ công dễ gây sai sót và khó audit.

Nếu vẫn cần SSH, hãy đưa thao tác vào pipeline hoặc script versioned trong Git.

### 30.2. Build lại riêng cho production

Nếu staging test artifact A nhưng production chạy artifact B, staging không còn đảm bảo nhiều.

Nên build một lần và promote cùng artifact.

### 30.3. Dùng `latest` cho production

`latest` làm khó truy vết và rollback.

Nên dùng tag rõ ràng như version hoặc commit SHA.

### 30.4. Không có rollback plan

Deploy mà không biết quay lại thế nào là rất rủi ro.

Trước khi deploy production, cần biết:

- Version trước là gì?
- Rollback bằng lệnh nào?
- Database có tương thích không?
- Ai quyết định rollback?

### 30.5. Secret nằm trong repository

Secret trong Git rất khó thu hồi hoàn toàn vì Git lưu lịch sử.

Nếu lỡ commit secret, cần rotate secret ngay.

### 30.6. Không kiểm tra sau deploy

Lệnh deploy thành công không đảm bảo app chạy tốt.

Cần health check, smoke test và monitoring.

### 30.7. Migration phá vỡ rolling update

Nếu version cũ và version mới không cùng chạy được với schema mới, rolling update có thể gây lỗi.

Cần thiết kế migration tương thích ngược.

## 31. Thiết kế pipeline CD tốt

Một pipeline CD tốt thường có:

- Artifact rõ ràng.
- Version rõ ràng.
- Deploy có thể lặp lại.
- Log dễ đọc.
- Secret được bảo vệ.
- Quyền được giới hạn.
- Có smoke test.
- Có health check.
- Có rollback plan.
- Có approval cho môi trường nhạy cảm.
- Có notification.
- Có audit trail.
- Có observability sau deploy.

Thứ tự cơ bản:

```text
select artifact
  -> deploy
  -> wait rollout
  -> smoke test
  -> notify
```

Thứ tự nâng cao:

```text
select artifact
  -> verify signature/provenance
  -> deploy canary
  -> watch metrics
  -> increase traffic
  -> complete rollout
  -> notify
```

## 32. CD cho project Java Spring Boot

Với Spring Boot, CD thường gồm:

1. Build `.jar`.
2. Build Docker image.
3. Push image lên registry.
4. Deploy image tới server hoặc Kubernetes.
5. Chạy health check qua Spring Boot Actuator.
6. Theo dõi log và metrics.
7. Rollback nếu cần.

Endpoint nên có:

```text
/actuator/health
/actuator/info
/actuator/metrics
```

Trong `application.yml`, có thể bật health endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      probes:
        enabled: true
```

Với Kubernetes, Spring Boot có thể cung cấp liveness và readiness:

```text
/actuator/health/liveness
/actuator/health/readiness
```

## 33. Ví dụ pipeline tổng thể CI/CD

Một flow hợp lý cho project Spring Boot:

```text
Pull Request
  -> compile
  -> unit test
  -> integration test
  -> lint/security scan

Merge vào main
  -> build jar
  -> build Docker image
  -> push image với tag commit SHA
  -> deploy staging
  -> smoke test staging

Tạo Git tag v1.2.3
  -> chọn artifact tương ứng
  -> approval production
  -> deploy production
  -> smoke test production
  -> monitor
```

Với người mới học, không cần làm tất cả ngay. Có thể đi theo từng cấp:

```text
Cấp 1: deploy thủ công nhưng có checklist.
Cấp 2: deploy staging tự động.
Cấp 3: production deploy bằng manual approval.
Cấp 4: rollback tự động hoặc bán tự động.
Cấp 5: canary/blue-green, metrics gate, GitOps.
```

## 34. Checklist CD cơ bản

Một project mới nên có tối thiểu:

- [ ] Có artifact rõ ràng.
- [ ] Artifact có version.
- [ ] Không dùng `latest` một mình cho production.
- [ ] Có registry lưu artifact.
- [ ] Có môi trường staging.
- [ ] Có cách deploy staging tự động.
- [ ] Có secret management.
- [ ] Có health check.
- [ ] Có smoke test sau deploy.
- [ ] Có log để debug.
- [ ] Có rollback plan.
- [ ] Có tài liệu deploy.

## 35. Checklist CD nâng cao

Khi project trưởng thành hơn, có thể bổ sung:

- [ ] Production approval.
- [ ] Blue-green hoặc canary deployment.
- [ ] Feature flag.
- [ ] Database migration strategy.
- [ ] Artifact signing.
- [ ] SBOM.
- [ ] Provenance.
- [ ] GitOps bằng Argo CD hoặc Flux.
- [ ] Dashboard sau deploy.
- [ ] Alert tự động theo error rate.
- [ ] Audit trail đầy đủ.
- [ ] OIDC thay cho long-lived cloud key.

## 36. Quy trình xử lý khi deploy fail

Khi deploy fail, nên làm theo thứ tự:

1. Xác định fail ở bước nào: deploy, rollout, smoke test hay monitoring.
2. Đọc log pipeline.
3. Đọc log ứng dụng.
4. Kiểm tra health endpoint.
5. Kiểm tra version đang chạy.
6. Kiểm tra configuration và secret.
7. Kiểm tra database migration.
8. Quyết định rollback hoặc roll-forward.
9. Ghi lại nguyên nhân và cách phòng tránh.

Không nên chỉ bấm rerun nhiều lần nếu chưa hiểu lỗi. Rerun có thể che giấu lỗi thật hoặc làm hệ thống tệ hơn, nhất là với migration.

## 37. Câu hỏi thiết kế CD cho một project

Khi thiết kế CD, hãy hỏi:

- Artifact cuối cùng là gì?
- Artifact được lưu ở đâu?
- Version được đặt thế nào?
- Có những môi trường nào?
- Ai được deploy production?
- Deploy production có cần approval không?
- Secret được quản lý ở đâu?
- Rollback bằng cách nào?
- Database migration chạy lúc nào?
- Health check là endpoint nào?
- Smoke test gồm những bước nào?
- Nếu deploy fail, ai nhận thông báo?
- Log và metrics xem ở đâu?
- Có cần blue-green, canary hoặc feature flag không?
- Có yêu cầu audit hoặc compliance không?

Trả lời các câu hỏi này trước khi viết YAML sẽ giúp pipeline đúng nhu cầu hơn.

## 38. Lộ trình học CD thực hành

Nếu đang học DevOps, có thể học CD theo thứ tự:

### Tuần 1: CD cơ bản

- Hiểu Continuous Delivery và Continuous Deployment.
- Viết checklist deploy thủ công.
- Deploy Spring Boot lên server bằng `systemd` hoặc Docker Compose.
- Thêm health endpoint.

### Tuần 2: CD với GitHub Actions

- Build Docker image.
- Push image lên registry.
- Deploy staging tự động.
- Dùng GitHub Secrets.
- Chạy smoke test sau deploy.

### Tuần 3: Production release

- Tạo Git tag.
- Deploy production bằng manual approval.
- Viết rollback script.
- Ghi release note.

### Tuần 4: CD nâng cao

- Tìm hiểu rolling update, blue-green, canary.
- Tìm hiểu database migration an toàn.
- Tìm hiểu GitOps với Argo CD.
- Thêm monitoring sau deploy.

## 39. Tóm tắt

CD là thực hành đưa phần mềm từ artifact đã kiểm tra tới môi trường chạy một cách tự động, lặp lại được và kiểm soát được. Continuous Delivery giúp phần mềm luôn sẵn sàng release, còn Continuous Deployment tự động đưa thay đổi lên production khi đủ điều kiện.

Một hệ thống CD tốt cần artifact rõ ràng, version rõ ràng, secret an toàn, deploy pipeline đáng tin, health check, smoke test, rollback plan, approval phù hợp và observability sau deploy. Với Spring Boot, CD thường xoay quanh việc build Docker image, push registry, deploy tới server hoặc Kubernetes, kiểm tra `/actuator/health` và theo dõi log/metrics.

CD không phải mục tiêu cuối cùng là "deploy thật nhanh bằng mọi giá". Mục tiêu đúng là release thường xuyên hơn nhưng vẫn kiểm soát được rủi ro. Khi CD được thiết kế tốt, deploy trở thành một việc bình thường, có thể lặp lại, dễ quan sát và dễ khôi phục khi có sự cố.
