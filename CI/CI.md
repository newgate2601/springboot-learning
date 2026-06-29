# Continuous Integration (CI) từ cơ bản tới nâng cao

## 1. CI là gì?

CI, viết tắt của Continuous Integration, là thực hành tích hợp code thường xuyên vào một nhánh chung, thường là `main`, `master`, `develop`, hoặc một nhánh tích hợp của team. Mỗi lần có thay đổi code, hệ thống CI sẽ tự động chạy một chuỗi kiểm tra như build, test, lint, scan bảo mật, đóng gói artifact và báo kết quả cho developer.

Mục tiêu quan trọng nhất của CI là phát hiện lỗi càng sớm càng tốt. Thay vì đợi vài tuần hoặc vài tháng mới gom code lại để kiểm thử, CI buộc code phải được kiểm tra liên tục theo từng thay đổi nhỏ. Khi lỗi xuất hiện, phạm vi nghi ngờ nhỏ hơn, nguyên nhân dễ truy vết hơn, chi phí sửa thấp hơn.

CI không chỉ là một công cụ như Jenkins, GitHub Actions, GitLab CI, CircleCI hay Azure Pipelines. CI là một quy trình kỹ thuật và văn hóa làm việc. Công cụ chỉ là phần thực thi.

## 2. Vì sao cần CI?

Trong phát triển phần mềm truyền thống, developer thường làm việc trên nhánh hoặc máy cá nhân trong thời gian dài, sau đó mới merge code. Cách này dễ tạo ra các vấn đề:

- Code chạy được trên máy người này nhưng lỗi trên máy người khác.
- Nhiều thay đổi lớn được merge cùng lúc, làm lỗi khó truy vết.
- Test bị chạy thủ công, dễ quên, dễ thiếu, khó lặp lại.
- Build chỉ được kiểm tra vào cuối chu kỳ, khiến lỗi tích tụ.
- Team mất niềm tin vào nhánh chính vì nhánh chính có thể hỏng bất cứ lúc nào.

CI giải quyết các vấn đề này bằng cách:

- Tự động hóa kiểm tra sau mỗi thay đổi.
- Chuẩn hóa môi trường build và test.
- Đưa feedback nhanh cho developer.
- Giữ nhánh chính luôn ở trạng thái có thể build và kiểm thử.
- Tạo nền tảng cho Continuous Delivery và Continuous Deployment.

## 3. CI khác gì CD?

CI tập trung vào việc tích hợp và xác minh code. CD có hai nghĩa phổ biến:

- Continuous Delivery: code sau khi qua CI luôn sẵn sàng để release, nhưng việc deploy production có thể vẫn cần phê duyệt thủ công.
- Continuous Deployment: code sau khi qua pipeline sẽ được tự động deploy tới production nếu đạt đủ điều kiện.

Quan hệ thường gặp:

```text
Code change
  -> CI: build, test, lint, scan, package
  -> Continuous Delivery: chuẩn bị release artifact, deploy staging, chờ approval
  -> Continuous Deployment: tự động deploy production
```

CI là nền móng. Nếu CI yếu, CD sẽ rủi ro vì pipeline có thể đưa code lỗi đi xa hơn trong hệ thống.

## 4. Các nguyên tắc cốt lõi của CI

### 4.1. Tích hợp thường xuyên

Developer nên merge code thường xuyên, lý tưởng là nhiều lần mỗi ngày hoặc ít nhất mỗi ngày một lần. Thay đổi càng nhỏ thì càng dễ review, dễ test, dễ rollback.

### 4.2. Build phải tự động

Không nên phụ thuộc vào việc ai đó nhớ chạy lệnh build. Pipeline phải tự chạy khi có push, pull request, merge request, tag hoặc lịch định kỳ.

### 4.3. Test phải tự động

CI chỉ có giá trị khi nó kiểm tra được hành vi thực tế của phần mềm. Test thủ công vẫn cần trong một số trường hợp, nhưng các kiểm tra lặp lại nên được tự động hóa.

### 4.4. Feedback phải nhanh

Một pipeline mất 2 phút sẽ được developer quan tâm hơn pipeline mất 60 phút. Feedback chậm làm developer chuyển context, quên thay đổi vừa làm, và có xu hướng bỏ qua tín hiệu CI.

### 4.5. Pipeline phải đáng tin

Nếu CI lúc xanh lúc đỏ vì lỗi môi trường, test flaky hoặc cấu hình thiếu ổn định, team sẽ mất niềm tin. Khi CI đỏ, mọi người phải tin rằng có vấn đề thật cần xử lý.

### 4.6. Nhánh chính phải luôn ổn định

Nhánh chính nên luôn build được, test được và có thể tạo artifact. Đây là tiêu chuẩn nền tảng để team làm việc song song mà không giẫm lên nhau.

## 5. Các thành phần cơ bản của CI

Một hệ thống CI thường gồm các thành phần sau:

```text
Source Code Repository
  -> Trigger
  -> CI Server / Orchestrator
  -> Runner / Agent
  -> Pipeline
  -> Jobs
  -> Steps
  -> Cache / Artifact
  -> Report / Notification
```

### 5.1. Source Code Repository

Repository là nơi chứa source code và lịch sử thay đổi. Ví dụ:

- GitHub repository.
- GitLab repository.
- Bitbucket repository.
- Azure Repos.
- Gitea hoặc Git server nội bộ.

CI thường được kết nối với repository thông qua webhook hoặc tích hợp sẵn. Khi có sự kiện như push code hoặc tạo pull request, repository gửi tín hiệu cho hệ thống CI.

### 5.2. Trigger

Trigger là điều kiện làm pipeline chạy. Các trigger phổ biến:

- Push lên một nhánh.
- Tạo hoặc cập nhật pull request.
- Merge vào nhánh chính.
- Tạo tag release.
- Chạy theo lịch, ví dụ mỗi đêm.
- Chạy thủ công bởi developer hoặc release manager.
- Chạy khi pipeline khác hoàn thành.

Ví dụ trigger theo mục đích:

| Mục đích | Trigger thường dùng |
|---|---|
| Kiểm tra pull request | `pull_request`, `merge_request` |
| Kiểm tra nhánh chính | `push` vào `main` |
| Release | `tag` hoặc manual dispatch |
| Kiểm tra định kỳ | schedule hằng ngày |
| Scan dependency | schedule hoặc pull request |

### 5.3. CI Server hoặc Orchestrator

CI server là bộ não điều phối pipeline. Nó nhận trigger, đọc file cấu hình CI, tạo pipeline run, phân bổ job cho runner, thu thập log, lưu artifact và trả trạng thái về repository.

Ví dụ:

- Jenkins controller.
- GitHub Actions service.
- GitLab CI coordinator.
- CircleCI cloud.
- Azure DevOps Pipelines.
- TeamCity server.

CI server chịu trách nhiệm:

- Quản lý pipeline definition.
- Quản lý queue.
- Điều phối runner.
- Quản lý secret.
- Hiển thị log và trạng thái.
- Kết nối với repository, registry, deployment target.

### 5.4. Runner hoặc Agent

Runner là nơi job thực sự chạy. Runner có thể là:

- Máy ảo tạm thời.
- Container.
- Máy vật lý.
- Kubernetes pod.
- Self-hosted server trong mạng nội bộ.

Runner thực thi các lệnh như:

```bash
mvn test
npm ci
docker build .
gradle build
pytest
go test ./...
```

Phân loại runner:

| Loại runner | Đặc điểm |
|---|---|
| Hosted runner | Do nền tảng CI cung cấp, dễ dùng, ít bảo trì |
| Self-hosted runner | Team tự vận hành, linh hoạt, truy cập được tài nguyên nội bộ |
| Ephemeral runner | Tạo mới cho mỗi job, sạch và an toàn hơn |
| Persistent runner | Tồn tại lâu dài, có thể nhanh hơn nhưng dễ nhiễm trạng thái cũ |

### 5.5. Pipeline

Pipeline là toàn bộ quy trình CI được định nghĩa bằng cấu hình. Một pipeline gồm nhiều stage, job và step.

Ví dụ pipeline đơn giản:

```text
Pipeline
  Stage 1: Validate
    Job: lint
    Job: unit-test
  Stage 2: Build
    Job: package
  Stage 3: Security
    Job: dependency-scan
    Job: secret-scan
```

Pipeline nên mô tả rõ:

- Khi nào chạy.
- Chạy trên môi trường nào.
- Cần biến môi trường nào.
- Chạy những lệnh nào.
- Job nào phụ thuộc job nào.
- Artifact nào cần lưu.
- Điều kiện pass/fail là gì.

### 5.6. Stage

Stage là nhóm logic của các job. Stage giúp pipeline dễ đọc và dễ kiểm soát thứ tự.

Các stage phổ biến:

- Checkout.
- Install dependencies.
- Lint.
- Unit test.
- Integration test.
- Build.
- Package.
- Security scan.
- Publish artifact.
- Deploy staging.

Không phải công cụ CI nào cũng có khái niệm stage rõ ràng. GitHub Actions dùng `jobs` và dependency thông qua `needs`; GitLab CI có `stages`; Jenkins pipeline có `stage`.

### 5.7. Job

Job là một đơn vị công việc chạy trên runner. Mỗi job thường có môi trường riêng.

Ví dụ:

- `unit-test`.
- `build-docker-image`.
- `frontend-lint`.
- `backend-integration-test`.
- `sast-scan`.

Job có thể chạy song song nếu không phụ thuộc nhau. Đây là cách quan trọng để tăng tốc pipeline.

### 5.8. Step

Step là từng lệnh hoặc action nhỏ trong job.

Ví dụ một job test Java:

```yaml
steps:
  - checkout source code
  - setup JDK 21
  - restore Maven cache
  - run mvn test
  - publish test report
```

Step càng rõ ràng thì log càng dễ đọc. Không nên gom quá nhiều logic phức tạp vào một dòng shell dài nếu nó làm pipeline khó debug.

### 5.9. Artifact

Artifact là file được tạo ra từ pipeline và cần lưu lại sau khi job kết thúc.

Ví dụ:

- File `.jar`, `.war`.
- Docker image metadata.
- Test report.
- Coverage report.
- Build log.
- File binary release.
- Screenshot từ end-to-end test.

Artifact khác cache. Artifact là kết quả cần xem lại hoặc dùng ở stage sau. Cache là dữ liệu phụ để tăng tốc.

### 5.10. Cache

Cache giúp pipeline chạy nhanh hơn bằng cách tái sử dụng dữ liệu như dependency đã tải.

Ví dụ cache:

- Maven: `~/.m2/repository`.
- Gradle: `~/.gradle/caches`.
- npm: `~/.npm`.
- pnpm store.
- pip cache.
- Go module cache.
- Docker build layer cache.

Cache cần có key hợp lý. Nếu key quá rộng, cache có thể dùng nhầm dependency cũ. Nếu key quá hẹp, cache thường xuyên miss và không giúp được nhiều.

Ví dụ key cache tốt thường dựa trên lockfile:

```text
cache-key = operating-system + package-lock.json hash
cache-key = operating-system + pom.xml hash
cache-key = operating-system + pnpm-lock.yaml hash
```

### 5.11. Logs

Log là nguồn thông tin chính để debug pipeline. Log tốt cần:

- Có tên job và step rõ ràng.
- Hiển thị lệnh quan trọng.
- Không in secret.
- Không quá nhiễu.
- Có thời gian hoặc duration nếu công cụ hỗ trợ.

Một pipeline khó debug thường có log bị che giấu quá nhiều, hoặc ngược lại in ra quá nhiều thông tin không liên quan.

### 5.12. Status Check

Status check là kết quả CI gắn vào commit hoặc pull request. Nó cho biết code có đạt điều kiện merge không.

Các trạng thái phổ biến:

- Pending: đang chạy.
- Success: pass.
- Failure: fail.
- Cancelled: bị hủy.
- Skipped: bỏ qua theo điều kiện.

Repository thường có branch protection để yêu cầu một số status check phải pass trước khi merge.

## 6. Vòng đời của một pipeline CI

Một pipeline thường đi qua các bước:

1. Developer push code hoặc cập nhật pull request.
2. Repository gửi event tới CI.
3. CI server đọc cấu hình pipeline.
4. CI server tạo pipeline run.
5. Job được đưa vào queue.
6. Runner nhận job.
7. Runner checkout source code.
8. Runner chuẩn bị môi trường.
9. Runner chạy từng step.
10. CI thu log, report, artifact.
11. CI trả trạng thái về pull request hoặc commit.
12. Developer xem kết quả và sửa nếu cần.

Điểm quan trọng: pipeline phải tái lập được. Cùng một commit, cùng một cấu hình, cùng một dependency lockfile nên cho ra kết quả giống nhau.

## 7. File cấu hình CI

Mỗi nền tảng CI có cách khai báo khác nhau, nhưng ý tưởng giống nhau.

### 7.1. GitHub Actions

File thường nằm trong:

```text
.github/workflows/ci.yml
```

Ví dụ:

```yaml
name: CI

on:
  pull_request:
  push:
    branches:
      - main

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
          cache: maven
      - run: ./mvnw test
```

Các khái niệm chính:

- `name`: tên workflow.
- `on`: trigger.
- `jobs`: danh sách job.
- `runs-on`: loại runner.
- `steps`: các bước trong job.
- `uses`: dùng action có sẵn.
- `run`: chạy lệnh shell.
- `with`: truyền input cho action.
- `env`: biến môi trường.
- `secrets`: dữ liệu nhạy cảm.

### 7.2. GitLab CI

File thường là:

```text
.gitlab-ci.yml
```

Ví dụ:

```yaml
stages:
  - test
  - build

unit-test:
  stage: test
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn test

package:
  stage: build
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn package -DskipTests
  artifacts:
    paths:
      - target/*.jar
```

Các khái niệm chính:

- `stages`: thứ tự stage.
- `image`: Docker image dùng để chạy job.
- `script`: lệnh thực thi.
- `artifacts`: file lưu lại.
- `cache`: cache dependency.
- `rules`: điều kiện chạy job.
- `needs`: dependency giữa job.

### 7.3. Jenkins Pipeline

File thường là:

```text
Jenkinsfile
```

Ví dụ:

```groovy
pipeline {
  agent any

  stages {
    stage('Test') {
      steps {
        sh './mvnw test'
      }
    }

    stage('Package') {
      steps {
        sh './mvnw package -DskipTests'
      }
    }
  }
}
```

Các khái niệm chính:

- `pipeline`: định nghĩa pipeline.
- `agent`: nơi pipeline chạy.
- `stages`: nhóm stage.
- `stage`: từng giai đoạn.
- `steps`: các lệnh.
- `post`: hành động sau khi chạy, ví dụ gửi thông báo.
- `environment`: biến môi trường.

## 8. Build trong CI

Build là quá trình biến source code thành artifact có thể chạy hoặc kiểm tra. Với từng ngôn ngữ, build khác nhau:

| Hệ sinh thái | Lệnh build thường gặp | Artifact |
|---|---|---|
| Java Maven | `mvn package` | `.jar`, `.war` |
| Java Gradle | `gradle build` | `.jar`, `.war` |
| Node.js | `npm run build` | `dist/`, bundle |
| Go | `go build` | binary |
| .NET | `dotnet build` | DLL, package |
| Python | `python -m build` | wheel, sdist |

Build trong CI nên:

- Dùng lệnh giống local càng nhiều càng tốt.
- Không phụ thuộc vào file chưa commit.
- Dùng dependency lockfile.
- Tách rõ build và test nếu cần tối ưu thời gian.
- Lưu artifact nếu stage sau cần dùng.

## 9. Test trong CI

Test là trái tim của CI. Một pipeline chỉ build thành công nhưng không test thì chưa đủ bảo vệ hệ thống.

### 9.1. Unit Test

Unit test kiểm tra một đơn vị nhỏ như function, class, module. Đặc điểm:

- Chạy nhanh.
- Ít phụ thuộc hạ tầng ngoài.
- Dễ định vị lỗi.
- Nên chạy trong mọi pull request.

Ví dụ Java:

```bash
./mvnw test
```

Ví dụ Node.js:

```bash
npm test
```

### 9.2. Integration Test

Integration test kiểm tra nhiều thành phần phối hợp với nhau. Ví dụ:

- Service gọi database.
- API gọi message broker.
- Repository chạy query thật.
- Module backend gọi external service mock.

Integration test thường cần Docker, Testcontainers, database tạm hoặc service giả lập.

Điểm cần chú ý:

- Tạo dữ liệu test độc lập.
- Dọn dữ liệu sau test.
- Tránh phụ thuộc thứ tự test.
- Không dùng chung database production.
- Cố gắng chạy song song được.

### 9.3. End-to-End Test

End-to-end test kiểm tra luồng người dùng từ đầu đến cuối. Ví dụ:

- Login.
- Tạo đơn hàng.
- Thanh toán giả lập.
- Kiểm tra trạng thái đơn hàng.

E2E test có giá trị cao nhưng thường chậm và dễ flaky hơn. Nên chọn lọc các luồng quan trọng nhất thay vì cố test mọi chi tiết bằng E2E.

### 9.4. Contract Test

Contract test kiểm tra hợp đồng giữa service provider và consumer. Nó hữu ích trong kiến trúc microservices.

Ví dụ:

- Consumer kỳ vọng API `/users/{id}` trả field `id`, `name`, `status`.
- Provider phải đảm bảo response vẫn đúng contract.

Contract test giúp phát hiện breaking change trước khi deploy.

### 9.5. Smoke Test

Smoke test là kiểm tra nhanh sau khi build hoặc deploy để biết hệ thống có sống không.

Ví dụ:

- Gọi `/health`.
- Gọi một API đọc dữ liệu đơn giản.
- Kiểm tra app khởi động được.

Smoke test không thay thế test đầy đủ, nhưng rất hữu ích để phát hiện lỗi nghiêm trọng sớm.

### 9.6. Test Pyramid

Một chiến lược test phổ biến:

```text
       E2E Tests
    Integration Tests
       Unit Tests
```

Nên có nhiều unit test, số lượng integration test vừa phải, và ít E2E test nhưng bao phủ luồng quan trọng.

## 10. Lint, format và static analysis

CI không chỉ kiểm tra chương trình chạy đúng mà còn kiểm tra chất lượng code.

### 10.1. Lint

Lint phát hiện lỗi style, cú pháp nguy hiểm hoặc pattern không nên dùng.

Ví dụ:

- ESLint cho JavaScript/TypeScript.
- Checkstyle cho Java.
- Flake8 hoặc Ruff cho Python.
- golangci-lint cho Go.

### 10.2. Format

Format giúp code đồng nhất. Ví dụ:

- Prettier.
- Spotless.
- google-java-format.
- Black.
- gofmt.

Trong CI, có hai cách:

- Kiểm tra format và fail nếu sai.
- Tự format rồi tạo commit, thường dùng bot riêng.

Với hầu hết team, cách đầu tiên đơn giản và minh bạch hơn.

### 10.3. Static Analysis

Static analysis phân tích code mà không chạy chương trình. Nó có thể tìm:

- Null pointer risk.
- Dead code.
- Code duplication.
- Complexity quá cao.
- Bug pattern.
- Security issue.

Ví dụ công cụ:

- SonarQube.
- SpotBugs.
- PMD.
- CodeQL.
- Semgrep.

## 11. Security trong CI

CI là nơi rất phù hợp để tự động hóa kiểm tra bảo mật.

### 11.1. Secret Scanning

Secret scanning tìm thông tin nhạy cảm bị commit nhầm:

- API key.
- Password.
- Private key.
- Token.
- Connection string.

Nếu phát hiện secret thật đã commit, không chỉ xóa khỏi code là đủ. Cần revoke hoặc rotate secret đó vì nó đã xuất hiện trong lịch sử git.

### 11.2. Dependency Scanning

Dependency scanning kiểm tra thư viện có lỗ hổng đã biết không.

Ví dụ:

- npm audit.
- OWASP Dependency-Check.
- Snyk.
- Dependabot.
- GitLab Dependency Scanning.

Điểm cần chú ý:

- Không phải vulnerability nào cũng ảnh hưởng trực tiếp.
- Cần phân loại theo severity và exploitability.
- Lockfile giúp kiểm soát phiên bản chính xác.

### 11.3. SAST

SAST, Static Application Security Testing, phân tích source code để tìm lỗi bảo mật như:

- SQL injection.
- Command injection.
- Path traversal.
- Insecure deserialization.
- Hardcoded secret.
- Weak cryptography.

SAST nên chạy trong CI, nhưng cần tuning để tránh quá nhiều false positive.

### 11.4. Container Image Scanning

Nếu project build Docker image, CI nên scan image để tìm:

- OS package vulnerability.
- Library vulnerability.
- Image chạy bằng root.
- Base image quá cũ.
- Secret trong layer.

Ví dụ công cụ:

- Trivy.
- Grype.
- Docker Scout.
- Snyk Container.

### 11.5. SBOM

SBOM, Software Bill of Materials, là danh sách thành phần phần mềm có trong artifact. Nó giống như bảng thành phần của một sản phẩm phần mềm.

SBOM hữu ích để:

- Truy vết dependency.
- Đánh giá ảnh hưởng khi có CVE mới.
- Tuân thủ yêu cầu bảo mật.
- Hỗ trợ supply chain security.

Định dạng phổ biến:

- CycloneDX.
- SPDX.

### 11.6. Signing và provenance

Ở mức nâng cao, pipeline có thể ký artifact hoặc image để chứng minh nguồn gốc.

Các khái niệm:

- Artifact signing: ký file build.
- Image signing: ký Docker image.
- Provenance: metadata mô tả artifact được build từ commit nào, pipeline nào, runner nào.
- SLSA: framework tăng độ tin cậy của software supply chain.

## 12. Secret và biến môi trường

CI thường cần secret để truy cập:

- Package registry.
- Container registry.
- Cloud provider.
- Database test.
- Service nội bộ.
- Signing key.

Nguyên tắc quản lý secret:

- Không commit secret vào repository.
- Dùng secret store của CI platform.
- Giới hạn quyền theo nguyên tắc least privilege.
- Không in secret ra log.
- Không cấp production secret cho pipeline pull request từ fork.
- Rotate secret định kỳ.
- Tách secret theo môi trường: dev, staging, production.

Biến môi trường không phải lúc nào cũng là secret. Ví dụ `JAVA_VERSION=21` không nhạy cảm. Nhưng `DATABASE_PASSWORD` là secret.

## 13. Dependency management trong CI

CI chỉ ổn định khi dependency được quản lý chặt.

### 13.1. Lockfile

Lockfile ghi lại phiên bản dependency chính xác.

Ví dụ:

- `package-lock.json`.
- `pnpm-lock.yaml`.
- `yarn.lock`.
- `poetry.lock`.
- `Gemfile.lock`.
- `go.sum`.

Với Maven, dependency được khai báo trong `pom.xml`; có thể dùng Maven Enforcer Plugin để kiểm soát phiên bản và rule.

### 13.2. Reproducible Build

Reproducible build nghĩa là cùng source code và cùng input sẽ tạo ra output giống nhau. Điều này giúp:

- Debug dễ hơn.
- Artifact đáng tin hơn.
- Release có thể kiểm chứng.
- Giảm lỗi do môi trường.

### 13.3. Dependency caching

Cache dependency giúp nhanh hơn, nhưng không nên làm build phụ thuộc vào cache. Nếu xóa cache mà build fail, pipeline có vấn đề.

Một pipeline tốt phải chạy được từ môi trường sạch.

## 14. Branching strategy và CI

Chiến lược branch ảnh hưởng trực tiếp đến CI.

### 14.1. Trunk-Based Development

Developer merge thường xuyên vào nhánh chính. Feature lớn được che bằng feature flag.

Ưu điểm:

- Tích hợp liên tục đúng nghĩa.
- Ít merge conflict.
- Feedback nhanh.
- Phù hợp với CI/CD hiện đại.

Thách thức:

- Cần test tốt.
- Cần feature flag.
- Cần discipline cao.

### 14.2. Git Flow

Git Flow dùng nhiều nhánh dài hạn như `develop`, `release`, `hotfix`, `main`.

Ưu điểm:

- Rõ quy trình release.
- Phù hợp một số tổ chức release theo đợt.

Nhược điểm:

- Tích hợp chậm hơn.
- Merge phức tạp hơn.
- Dễ xa rời tinh thần CI nếu feature branch sống quá lâu.

### 14.3. Feature Branch

Feature branch phổ biến với pull request. Để không làm yếu CI:

- Branch nên sống ngắn.
- Pull request nên nhỏ.
- Rebase hoặc merge thường xuyên từ nhánh chính.
- CI phải chạy trên mỗi pull request.

## 15. Pull Request CI

Pull request CI là lớp bảo vệ trước khi code vào nhánh chính.

Các kiểm tra nên có:

- Build.
- Unit test.
- Lint.
- Format check.
- Static analysis cơ bản.
- Dependency scan nếu không quá chậm.
- Test ảnh hưởng theo module nếu monorepo lớn.

Branch protection nên yêu cầu:

- CI pass.
- Review đủ số lượng.
- Không cho merge khi branch outdated nếu rủi ro cao.
- Không bypass trừ trường hợp khẩn cấp được ghi lại.

## 16. Main Branch CI

Sau khi merge vào nhánh chính, CI nên chạy lại. Lý do:

- Pull request có thể pass riêng nhưng fail khi kết hợp với thay đổi khác.
- Merge commit có thể tạo trạng thái mới.
- Artifact release thường nên build từ nhánh chính.

Main branch CI thường có thêm:

- Build artifact chính thức.
- Publish snapshot package.
- Build Docker image.
- Deploy staging.
- Full integration test.

## 17. Tag và release CI

Khi tạo tag như `v1.2.3`, pipeline release có thể:

- Build artifact từ tag.
- Chạy test tối thiểu hoặc đầy đủ.
- Tạo changelog.
- Publish package.
- Build và push Docker image.
- Ký artifact.
- Tạo GitHub Release hoặc GitLab Release.
- Deploy production sau approval.

Tag release nên immutable về mặt quy trình. Không nên xóa rồi tạo lại tag cùng tên trừ khi team có quy định rất rõ.

## 18. Matrix build

Matrix build chạy cùng một job với nhiều biến thể.

Ví dụ:

- Test trên nhiều phiên bản Java: 17, 21.
- Test trên nhiều hệ điều hành: Ubuntu, Windows, macOS.
- Test nhiều phiên bản Node.js.
- Test nhiều database: PostgreSQL, MySQL.

Ví dụ GitHub Actions:

```yaml
strategy:
  matrix:
    java: ["17", "21"]
    os: [ubuntu-latest, windows-latest]
```

Matrix build hữu ích nhưng có thể làm pipeline tốn thời gian và chi phí. Nên dùng cho thư viện, framework, SDK hoặc sản phẩm cần hỗ trợ nhiều môi trường.

## 19. Parallelization

Pipeline nhanh thường biết chạy song song.

Ví dụ:

```text
lint       \
unit-test   -> package -> image-scan
typecheck  /
```

Các kỹ thuật:

- Tách lint, test, typecheck thành job song song.
- Chia test suite thành nhiều shard.
- Chạy frontend và backend song song.
- Chỉ chạy test module bị ảnh hưởng trong monorepo.
- Dùng cache hợp lý.

Nhưng song song quá mức cũng có giá:

- Tốn runner.
- Log phân tán.
- Cấu hình phức tạp.
- Dễ tạo race condition nếu dùng chung tài nguyên.

## 20. Flaky test

Flaky test là test lúc pass lúc fail mà không có thay đổi code liên quan.

Nguyên nhân thường gặp:

- Phụ thuộc thời gian thực.
- Phụ thuộc thứ tự test.
- Dùng dữ liệu shared.
- Race condition.
- Network không ổn định.
- Timeout quá thấp.
- Test dựa vào animation hoặc UI timing.
- Không dọn state sau khi chạy.

Cách xử lý:

- Điều tra và sửa nguyên nhân.
- Gắn nhãn flaky để theo dõi.
- Không lặng lẽ retry mọi thứ mà không đo lường.
- Cô lập dữ liệu test.
- Dùng fake clock khi test logic thời gian.
- Tăng observability cho test fail.

Retry có thể dùng như biện pháp tạm thời, nhưng nếu lạm dụng sẽ che giấu lỗi thật.

## 21. Pipeline performance

Một pipeline tốt không chỉ đúng mà còn nhanh.

### 21.1. Đo trước khi tối ưu

Cần biết job nào chậm nhất:

- Dependency install.
- Build.
- Unit test.
- Integration test.
- Docker build.
- Security scan.

Tối ưu nên dựa trên số liệu duration, không chỉ cảm giác.

### 21.2. Kỹ thuật tăng tốc

- Dùng dependency cache.
- Dùng Docker layer cache.
- Tách job chạy song song.
- Chỉ chạy job nặng khi cần.
- Dùng incremental build nếu đáng tin.
- Chia test suite.
- Tránh tải lại tool không cần thiết.
- Dùng image CI đã cài sẵn tool phổ biến.

### 21.3. Giữ pipeline dễ hiểu

Tối ưu quá mức có thể làm pipeline khó bảo trì. Một pipeline chậm hơn 1 phút nhưng dễ hiểu đôi khi tốt hơn pipeline nhanh hơn một chút nhưng ai cũng sợ sửa.

## 22. Monorepo CI

Monorepo chứa nhiều project trong cùng repository. CI cho monorepo có thách thức riêng:

- Số lượng module lớn.
- Pipeline dễ chậm.
- Dependency giữa module phức tạp.
- Không nên build toàn bộ nếu chỉ đổi một phần nhỏ.

Kỹ thuật thường dùng:

- Path filter: chỉ chạy job khi file liên quan thay đổi.
- Affected graph: xác định module bị ảnh hưởng.
- Remote build cache.
- Test sharding.
- Ownership theo thư mục.
- Pipeline template dùng lại.

Ví dụ path filter:

```text
Nếu thay đổi src/backend/** -> chạy backend CI
Nếu thay đổi src/frontend/** -> chạy frontend CI
Nếu thay đổi pom.xml -> chạy toàn bộ Java CI
Nếu thay đổi shared/** -> chạy các module phụ thuộc shared
```

## 23. Microservices CI

Với microservices, mỗi service có pipeline riêng hoặc dùng pipeline template chung.

CI cho microservices cần chú ý:

- Mỗi service build và test độc lập.
- Contract test giữa service.
- Versioning API.
- Docker image tag rõ ràng.
- Không deploy hàng loạt nếu chỉ một service thay đổi.
- Kiểm tra backward compatibility.

Image tag nên bao gồm thông tin truy vết:

```text
service-name:git-sha
service-name:1.4.2
service-name:1.4.2-build.57
```

Không nên chỉ dùng `latest` cho release vì khó rollback và khó audit.

## 24. Docker trong CI

Docker thường được dùng trong CI theo hai cách:

- Dùng container làm môi trường chạy job.
- Build container image cho ứng dụng.

### 24.1. Docker image cho môi trường CI

Thay vì cài tool mỗi lần chạy, team có thể tạo image CI chứa sẵn:

- JDK.
- Maven hoặc Gradle.
- Node.js.
- Docker CLI.
- Scanner.
- Browser cho E2E test.

Ưu điểm:

- Pipeline nhanh hơn.
- Môi trường nhất quán.
- Dễ kiểm soát phiên bản tool.

Nhược điểm:

- Cần bảo trì image.
- Image quá lớn làm pull chậm.
- Phải scan image định kỳ.

### 24.2. Docker build

Docker build trong CI nên:

- Dùng `.dockerignore`.
- Dùng base image cụ thể, tránh tag quá mơ hồ.
- Tận dụng layer cache.
- Không copy secret vào image.
- Không chạy app bằng root nếu không cần.
- Gắn label metadata.
- Scan image sau build.

Ví dụ label hữu ích:

```text
org.opencontainers.image.revision=<git-sha>
org.opencontainers.image.source=<repo-url>
org.opencontainers.image.version=<version>
```

## 25. Database trong CI

Nhiều test cần database. Có vài cách phổ biến:

### 25.1. Service container

CI khởi động database container cạnh job test.

Ví dụ:

- PostgreSQL container.
- MySQL container.
- Redis container.
- Kafka container.

Ưu điểm:

- Gần môi trường thật.
- Dễ chạy trong CI.

Nhược điểm:

- Chậm hơn unit test.
- Cần health check.
- Cần quản lý dữ liệu test.

### 25.2. Testcontainers

Testcontainers cho phép test tự khởi động container cần thiết. Rất phổ biến trong Java.

Ưu điểm:

- Test tự mô tả dependency hạ tầng.
- Chạy được local và CI giống nhau.
- Cô lập tốt hơn.

Nhược điểm:

- Cần Docker trong môi trường CI.
- Có thể chậm nếu không tối ưu reuse hoặc image pull.

### 25.3. In-memory database

Ví dụ H2 cho Java. Nhanh nhưng có rủi ro:

- Không giống database thật.
- SQL dialect khác.
- Behavior transaction khác.

Nên dùng in-memory database cho test phù hợp, không nên dùng để thay thế hoàn toàn integration test với database thật.

## 26. Quality gates

Quality gate là điều kiện bắt buộc để pipeline pass hoặc để code được merge.

Ví dụ:

- Unit test pass.
- Coverage không giảm dưới 80%.
- Không có vulnerability critical.
- Không có secret.
- Code duplication dưới ngưỡng.
- Sonar quality gate pass.
- Docker image không có CVE critical chưa được chấp nhận.

Quality gate cần cân bằng. Nếu quá lỏng, không bảo vệ được hệ thống. Nếu quá cứng mà nhiều false positive, developer sẽ tìm cách né.

## 27. Code coverage

Coverage đo tỷ lệ code được test chạy qua. Các loại coverage:

- Line coverage.
- Branch coverage.
- Function coverage.
- Statement coverage.

Coverage hữu ích nhưng không nói toàn bộ chất lượng test. Test có thể chạy qua dòng code nhưng không assert đúng hành vi.

Cách dùng coverage tốt:

- Theo dõi xu hướng.
- Chặn pull request làm coverage giảm mạnh.
- Tập trung vào module quan trọng.
- Kết hợp review chất lượng test.

Không nên chạy theo 100% coverage một cách máy móc.

## 28. Notification và feedback loop

CI cần báo kết quả đúng người, đúng lúc.

Kênh thông báo:

- Pull request status.
- Email.
- Slack, Microsoft Teams.
- Dashboard.
- Comment tự động.

Thông báo tốt:

- Nói rõ job nào fail.
- Link thẳng tới log.
- Không spam khi không cần.
- Có ownership rõ ràng.

Nếu mọi người bị bắn quá nhiều thông báo, họ sẽ bỏ qua tất cả.

## 29. Observability cho CI

CI cũng là một hệ thống cần quan sát.

Các chỉ số nên theo dõi:

- Pipeline duration trung bình.
- Queue time.
- Failure rate.
- Flaky test rate.
- Cache hit rate.
- Runner utilization.
- Cost per pipeline.
- Mean time to repair failed main branch.

Dashboard CI giúp team nhìn thấy vấn đề hệ thống thay vì chỉ xử lý từng lần fail riêng lẻ.

## 30. Runner security

Runner là nơi chạy code, nên có rủi ro bảo mật cao.

Nguy cơ:

- Pull request độc hại đọc secret.
- Job để lại file nhạy cảm cho job sau.
- Runner persistent bị nhiễm trạng thái.
- Script CI chạy lệnh nguy hiểm.
- Docker socket bị lạm dụng để chiếm host.

Biện pháp:

- Dùng runner tạm thời cho job không tin cậy.
- Không cấp secret cho pull request từ fork.
- Giới hạn quyền token mặc định.
- Cô lập runner theo project hoặc trust level.
- Xóa workspace sau job.
- Không mount Docker socket nếu không cần.
- Cập nhật runner thường xuyên.

## 31. Permission và token

Pipeline thường có token để:

- Checkout repository.
- Comment pull request.
- Push package.
- Push image.
- Tạo release.
- Deploy.

Nguyên tắc:

- Cấp quyền nhỏ nhất có thể.
- Tách token read-only và write.
- Token release không dùng cho PR untrusted.
- Dùng OIDC thay vì long-lived cloud key nếu nền tảng hỗ trợ.
- Audit quyền định kỳ.

OIDC trong CI cho phép pipeline lấy credential tạm thời từ cloud provider dựa trên identity của workflow, thay vì lưu access key dài hạn trong secret.

## 32. Environment promotion

CI thường kết hợp với CD để đưa artifact qua các môi trường:

```text
Build once
  -> Test
  -> Publish artifact
  -> Deploy dev
  -> Deploy staging
  -> Approval
  -> Deploy production
```

Nguyên tắc quan trọng: build một lần, promote cùng artifact qua các môi trường. Không nên rebuild riêng cho production từ source nếu artifact staging đã test khác artifact production.

Khác biệt giữa môi trường nên nằm ở configuration, không phải binary.

## 33. Artifact repository và registry

Artifact cần nơi lưu trữ:

- Maven repository.
- npm registry.
- Docker registry.
- NuGet feed.
- PyPI-compatible registry.
- Generic artifact store.

Ví dụ:

- GitHub Packages.
- GitLab Package Registry.
- Nexus Repository.
- JFrog Artifactory.
- AWS ECR.
- Azure Container Registry.
- Google Artifact Registry.

Artifact nên có:

- Version rõ ràng.
- Metadata commit.
- Build timestamp.
- Checksums.
- Signature nếu cần.
- Retention policy.

## 34. Versioning trong CI

Versioning giúp xác định artifact nào đang chạy.

Các kiểu version:

- Semantic Versioning: `MAJOR.MINOR.PATCH`.
- Calendar Versioning: `2026.06.29`.
- Build number: `1.2.3-build.45`.
- Git SHA: `a1b2c3d`.

Ví dụ tag Docker tốt:

```text
my-service:1.8.0
my-service:1.8.0-a1b2c3d
my-service:a1b2c3d
```

Git SHA rất hữu ích để truy vết chính xác code nào tạo ra artifact.

## 35. CI cho project Java Spring Boot

Với Spring Boot, pipeline CI thường gồm:

1. Checkout code.
2. Setup JDK.
3. Cache Maven hoặc Gradle.
4. Chạy unit test.
5. Chạy integration test.
6. Package `.jar`.
7. Build Docker image nếu cần.
8. Scan dependency.
9. Scan image.
10. Publish artifact.

Ví dụ Maven:

```bash
./mvnw clean verify
```

Trong Maven:

- `test`: chạy unit test.
- `package`: đóng gói artifact.
- `verify`: chạy toàn bộ kiểm tra đến phase verify, thường bao gồm integration test nếu cấu hình.
- `install`: cài artifact vào local repository.
- `deploy`: publish artifact tới remote repository.

Plugin thường gặp:

- `maven-surefire-plugin`: chạy unit test.
- `maven-failsafe-plugin`: chạy integration test.
- `jacoco-maven-plugin`: coverage.
- `maven-checkstyle-plugin`: style.
- `spotbugs-maven-plugin`: static analysis.
- `owasp-dependency-check-maven`: dependency vulnerability.
- `spring-boot-maven-plugin`: build executable jar hoặc image.

## 36. Ví dụ CI cho Spring Boot bằng GitHub Actions

```yaml
name: CI

on:
  pull_request:
  push:
    branches:
      - main

permissions:
  contents: read

jobs:
  verify:
    name: Maven Verify
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

      - name: Verify
        run: ./mvnw --batch-mode clean verify
```

Giải thích:

- `permissions: contents: read` giảm quyền mặc định của token.
- `actions/checkout` lấy source code.
- `setup-java` cài JDK và bật cache Maven.
- `--batch-mode` giúp log Maven phù hợp với CI hơn.
- `clean verify` build lại từ trạng thái sạch và chạy các kiểm tra đến phase verify.

## 37. Ví dụ CI có Docker image

```yaml
name: CI

on:
  push:
    branches:
      - main

permissions:
  contents: read
  packages: write

jobs:
  build-image:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
          cache: maven

      - run: ./mvnw --batch-mode clean package

      - name: Build image
        run: docker build -t ghcr.io/example/my-service:${{ github.sha }} .

      - name: Login registry
        run: echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u "${{ github.actor }}" --password-stdin

      - name: Push image
        run: docker push ghcr.io/example/my-service:${{ github.sha }}
```

Trong pipeline production thực tế, nên bổ sung image scan, metadata label và permission chặt hơn.

## 38. CI anti-patterns

### 38.1. Pipeline chỉ chạy trên máy một người

Nếu build phụ thuộc vào máy cá nhân, đó không phải CI đáng tin. CI phải chạy trên môi trường tự động và có thể tái lập.

### 38.2. Test bị tắt vì chậm

Nếu test chậm, cần tối ưu hoặc phân tầng, không nên tắt toàn bộ. Tắt test làm CI mất vai trò bảo vệ.

### 38.3. Main branch đỏ quá lâu

Nhánh chính fail trong thời gian dài khiến mọi người không biết lỗi mới hay lỗi cũ. Cần ưu tiên sửa main branch đỏ.

### 38.4. Pipeline quá nhiều logic shell khó đọc

Nếu file CI chứa nhiều script phức tạp, khó test và khó bảo trì. Nên đưa logic lặp lại vào script versioned trong repo, ví dụ `scripts/ci-test.sh`, nhưng vẫn giữ pipeline rõ ràng.

### 38.5. Secret xuất hiện trong log

Đây là lỗi nghiêm trọng. Cần mask secret, tránh `set -x` khi có secret, và rotate secret nếu bị lộ.

### 38.6. Dùng `latest` ở mọi nơi

Tag `latest` làm build khó tái lập. Nên pin version cho tool, image và dependency quan trọng.

### 38.7. Không phân biệt trusted và untrusted code

Pull request từ fork hoặc contributor bên ngoài không nên có cùng quyền và secret như code đã merge.

## 39. Thiết kế pipeline tốt

Một pipeline tốt thường có các đặc điểm:

- Dễ đọc.
- Feedback nhanh.
- Fail sớm.
- Log đủ thông tin.
- Tách job độc lập.
- Có cache nhưng không phụ thuộc cache.
- Có artifact rõ ràng.
- Secret được bảo vệ.
- Quyền được giới hạn.
- Có quality gate hợp lý.
- Có đường debug khi fail.

Thứ tự kiểm tra nên đặt từ nhanh đến chậm:

```text
format/lint
  -> unit test
  -> build
  -> integration test
  -> security scan
  -> package/publish
```

Tuy nhiên không có một công thức duy nhất. Pipeline nên phản ánh rủi ro và kiến trúc của project.

## 40. Chiến lược fail fast

Fail fast nghĩa là phát hiện lỗi sớm và dừng các bước không cần thiết.

Ví dụ:

- Nếu lint fail, không cần build Docker image.
- Nếu unit test fail, không cần deploy staging.
- Nếu compile fail, không cần chạy security scan tốn thời gian.

Nhưng cũng có lúc không nên fail quá sớm. Ví dụ trong pull request, team có thể muốn lint, unit test và typecheck cùng chạy song song để developer nhận đủ lỗi trong một lần.

## 41. Conditional jobs

Job có thể chạy theo điều kiện:

- Chỉ chạy khi branch là `main`.
- Chỉ chạy khi có tag.
- Chỉ chạy khi file trong thư mục nhất định thay đổi.
- Chỉ chạy khi commit message không chứa `[skip ci]`.
- Chỉ deploy khi được approval.

Conditional job giúp tiết kiệm thời gian và chi phí, nhưng nếu lạm dụng có thể làm pipeline khó hiểu.

## 42. Manual approval

Manual approval thường dùng trước bước nhạy cảm:

- Deploy production.
- Publish package public.
- Apply database migration production.
- Rotate key.

Approval không nên thay thế kiểm tra tự động. Nó là lớp kiểm soát bổ sung cho hành động rủi ro cao.

## 43. Database migration trong CI

Migration cần được kiểm tra trước khi deploy.

CI có thể:

- Validate migration syntax.
- Chạy migration trên database tạm.
- Chạy app sau migration.
- Kiểm tra rollback nếu hệ thống hỗ trợ.
- Kiểm tra migration không phá dữ liệu mẫu.

Với công cụ như Flyway hoặc Liquibase, pipeline có thể chạy:

```bash
mvn test
mvn flyway:validate
mvn liquibase:validate
```

Tùy project chỉ dùng một trong hai công cụ.

## 44. Handling flaky infrastructure

Không chỉ test mới flaky, hạ tầng CI cũng có thể không ổn định:

- Registry timeout.
- Network chập chờn.
- Runner thiếu disk.
- Docker pull bị rate limit.
- Cloud service tạm lỗi.

Biện pháp:

- Retry có chọn lọc cho thao tác network.
- Mirror dependency quan trọng.
- Theo dõi runner disk và CPU.
- Dọn cache cũ.
- Dùng registry nội bộ nếu cần.
- Không retry lỗi test logic một cách mù quáng.

## 45. Cost trong CI

CI dùng tài nguyên thật: runner minutes, CPU, RAM, storage, network, license scanner.

Cách kiểm soát cost:

- Không chạy job nặng cho thay đổi docs.
- Hủy pipeline cũ khi có commit mới trên cùng branch.
- Dùng cache.
- Tối ưu Docker image size.
- Dọn artifact cũ.
- Dùng runner phù hợp kích thước job.
- Theo dõi job nào đắt nhất.

Tuy vậy, không nên tối ưu cost bằng cách bỏ hết kiểm tra quan trọng. Chi phí bug production thường cao hơn chi phí CI.

## 46. Governance và compliance

Trong tổ chức lớn, CI còn phục vụ compliance:

- Ai đã approve release.
- Artifact build từ commit nào.
- Test nào đã chạy.
- Security scan kết quả ra sao.
- Dependency có license phù hợp không.
- Production deploy lúc nào.

Pipeline nên lưu bằng chứng:

- Build log.
- Test report.
- Scan report.
- SBOM.
- Approval record.
- Release note.

Retention policy cần đủ dài theo yêu cầu audit nhưng không gây phình storage vô hạn.

## 47. Checklist CI cơ bản

Một project mới nên có tối thiểu:

- Pipeline chạy trên pull request.
- Pipeline chạy trên nhánh chính.
- Build tự động.
- Unit test tự động.
- Lint hoặc format check.
- Dependency cache.
- Test report hoặc log dễ đọc.
- Branch protection yêu cầu CI pass.
- Secret không nằm trong code.

## 48. Checklist CI nâng cao

Khi project trưởng thành hơn, có thể bổ sung:

- Integration test bằng container.
- Coverage report.
- Static analysis.
- Dependency scanning.
- Secret scanning.
- Container image scanning.
- SBOM.
- Artifact signing.
- Matrix build.
- Test sharding.
- Monorepo affected build.
- Runner isolation.
- OIDC cho cloud credentials.
- Observability dashboard cho CI.
- Release pipeline có approval.

## 49. Quy trình xử lý khi CI fail

Khi CI fail, nên làm theo thứ tự:

1. Xác định job fail.
2. Đọc log từ dòng lỗi đầu tiên có ý nghĩa.
3. Kiểm tra lỗi có tái hiện local không.
4. Xác định fail do code, test, config hay hạ tầng.
5. Nếu do code, sửa và push commit nhỏ.
6. Nếu do flaky test, tạo issue hoặc sửa ngay nếu ảnh hưởng lớn.
7. Nếu do hạ tầng, ghi nhận và cải thiện retry/monitoring.
8. Không merge khi chưa hiểu rủi ro.

Một lỗi CI tốt là lỗi nói cho developer biết cần sửa gì. Nếu log khó hiểu, cải thiện log cũng là một phần của việc sửa CI.

## 50. CI maturity model

Có thể nhìn mức độ trưởng thành CI theo 5 cấp:

### Cấp 1: Thủ công

- Build và test chủ yếu chạy trên máy developer.
- Không có pipeline ổn định.
- Nhánh chính có thể hỏng lâu.

### Cấp 2: CI cơ bản

- Có pipeline trên pull request.
- Build và unit test tự động.
- Developer bắt đầu dựa vào status check.

### Cấp 3: CI đáng tin

- Pipeline nhanh và ổn định.
- Branch protection rõ ràng.
- Test report, coverage, lint đầy đủ.
- Main branch hiếm khi đỏ lâu.

### Cấp 4: CI bảo mật và tối ưu

- Có security scan.
- Có dependency management tốt.
- Có cache, parallelization, runner strategy.
- Có observability cho CI.

### Cấp 5: CI/CD hiện đại

- Artifact được build một lần và promote qua môi trường.
- Có signing, SBOM, provenance.
- Deploy có kiểm soát.
- Pipeline phục vụ cả engineering velocity và compliance.

## 51. Một pipeline mẫu từ cơ bản tới nâng cao

```text
Pull Request Pipeline
  - checkout
  - setup runtime
  - restore cache
  - format check
  - lint
  - unit test
  - build
  - publish test report

Main Branch Pipeline
  - checkout
  - setup runtime
  - restore cache
  - full test
  - integration test
  - build artifact
  - dependency scan
  - build Docker image
  - image scan
  - publish artifact/image

Release Pipeline
  - checkout tag
  - verify artifact provenance
  - generate SBOM
  - sign artifact
  - publish release
  - deploy staging
  - smoke test
  - manual approval
  - deploy production
  - post-deploy smoke test
```

## 52. Câu hỏi thiết kế CI cho một project

Khi thiết kế CI, hãy hỏi:

- Project dùng ngôn ngữ và build tool nào?
- Lệnh build chuẩn là gì?
- Test nào cần chạy trên mọi pull request?
- Test nào chỉ cần chạy trên main hoặc nightly?
- Artifact cuối cùng là gì?
- Có Docker image không?
- Có cần deploy không?
- Có secret nào cần dùng không?
- Pull request từ fork có được chạy không?
- Runner cần truy cập tài nguyên nội bộ không?
- Pipeline hiện tại chậm ở đâu?
- Rủi ro lớn nhất của project là bug logic, security, compatibility hay release?

Trả lời các câu hỏi này trước khi viết YAML giúp pipeline đúng nhu cầu hơn.

## 53. Tóm tắt

CI là thực hành tích hợp code thường xuyên và kiểm tra tự động để giữ phần mềm luôn ở trạng thái đáng tin. Một hệ thống CI gồm repository, trigger, CI server, runner, pipeline, job, step, cache, artifact, log và status check. Ở mức cơ bản, CI cần build và test tự động trên pull request. Ở mức nâng cao, CI bao gồm security scanning, dependency control, artifact management, runner isolation, observability, signing, SBOM và promotion qua nhiều môi trường.

CI tốt không phải là pipeline dài nhất, mà là pipeline đưa feedback nhanh, đúng, dễ hiểu và đủ mạnh để bảo vệ hệ thống. Khi CI đáng tin, team có thể thay đổi phần mềm thường xuyên hơn mà vẫn kiểm soát được rủi ro.
