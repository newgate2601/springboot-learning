# GitLab CI/CD vs Jenkins: phân biệt chuyên sâu, ưu nhược điểm và use case thực tế

## 1. Mục tiêu

Tài liệu này chỉ tập trung vào hai công cụ CI/CD rất phổ biến trong doanh nghiệp:

- GitLab CI/CD.
- Jenkins.

Mục tiêu là hiểu rõ:

- GitLab CI/CD và Jenkins khác nhau ở bản chất nào.
- GitLab Runner khác Jenkins Agent thế nào.
- `.gitlab-ci.yml` khác `Jenkinsfile` ra sao.
- Công ty lớn thường dùng kiểu nào.
- Khi nào nên chọn GitLab CI/CD.
- Khi nào nên chọn Jenkins.
- Khi nào dùng cả hai cùng lúc.
- Ví dụ thực tế với Java Spring Boot, Maven, Docker, SonarQube, Harbor/Nexus và Kubernetes.

## 2. Tóm tắt thật ngắn

```text
GitLab CI/CD
  = CI/CD tích hợp sẵn trong GitLab
  = pipeline viết bằng .gitlab-ci.yml
  = rất hợp nếu source code nằm trong GitLab

Jenkins
  = CI/CD server độc lập, lâu đời, cực kỳ linh hoạt
  = pipeline thường viết bằng Jenkinsfile
  = rất hợp với enterprise, legacy system, tích hợp phức tạp
```

Nói dễ hiểu:

```text
GitLab CI/CD giống một bộ DevOps trọn gói.
Jenkins giống một bộ điều phối build/deploy cực kỳ tùy biến.
```

## 3. Bảng so sánh nhanh

| Tiêu chí | GitLab CI/CD | Jenkins |
|---|---|---|
| Bản chất | Tính năng CI/CD nằm trong GitLab | CI/CD server độc lập |
| File pipeline | `.gitlab-ci.yml` | `Jenkinsfile` |
| Ngôn ngữ cấu hình | YAML | Groovy DSL |
| Nơi lưu code phù hợp nhất | GitLab | GitLab, GitHub, Bitbucket, SVN, repo nội bộ |
| Thành phần chạy job | GitLab Runner | Jenkins Agent |
| Thành phần điều phối | GitLab server | Jenkins Controller |
| Độ dễ bắt đầu | Dễ hơn | Khó hơn |
| Mức độ tùy biến | Cao | Rất cao |
| Plugin ecosystem | Vừa đủ, thiên về tích hợp trong GitLab | Rất lớn |
| Vận hành | Nhẹ hơn nếu đã dùng GitLab | Nặng hơn, cần chăm server/plugin/agent |
| Phù hợp nhất | Team dùng GitLab end-to-end | Enterprise, legacy, hệ thống phức tạp |

## 4. So sánh sâu: GitLab CI/CD hơn Jenkins ở đâu, Jenkins hơn GitLab CI/CD ở đâu?

Phần này là phần quan trọng nhất. Không chỉ nói "GitLab dễ hơn" hay "Jenkins linh hoạt hơn", mà phân tích cụ thể hơn ở từng khía cạnh.

## 4.1. Mức độ liền mạch với source code

### GitLab CI/CD hơn Jenkins ở điểm nào?

Nếu source code nằm trên GitLab, GitLab CI/CD mạnh hơn Jenkins ở sự liền mạch.

Khi developer mở Merge Request, mọi thứ nằm cùng một nơi:

```text
Merge Request
  -> diff code
  -> comment review
  -> pipeline status
  -> job log
  -> test report
  -> artifact
  -> security/code quality report
```

Developer không cần chuyển qua Jenkins UI để xem job nào fail. GitLab hiển thị pipeline ngay trong Merge Request.

Ví dụ thực tế:

```text
Developer tạo Merge Request sửa OrderService.
GitLab tự chạy pipeline.
Reviewer mở Merge Request và thấy:
  - unit-test failed
  - job log chỉ ra OrderServiceTest lỗi
  - không cho merge vì pipeline đỏ
```

Với Jenkins, vẫn làm được, nhưng thường phải tích hợp thêm:

```text
GitLab/GitHub webhook
  -> Jenkins job
  -> Jenkins báo trạng thái ngược về Git provider
```

Nếu cấu hình tốt thì cũng mượt, nhưng nó là tích hợp giữa hai hệ thống. GitLab CI/CD thì native.

### Jenkins hơn GitLab CI/CD ở điểm nào?

Jenkins hơn khi source code không nằm cố định ở một nền tảng.

Ví dụ công ty có:

```text
Service mới: GitLab
Service cũ: SVN
Thư viện nội bộ: Bitbucket
Script triển khai: shared folder nội bộ
```

Jenkins có thể đứng giữa và điều phối tất cả. GitLab CI/CD mạnh nhất khi code và workflow nằm trong GitLab, còn Jenkins không cần phụ thuộc vào một Git platform cụ thể.

Kết luận:

```text
Code nằm chủ yếu trên GitLab
  -> GitLab CI/CD liền mạch hơn.

Code nằm rải rác nhiều hệ thống
  -> Jenkins linh hoạt hơn.
```

## 4.2. Độ dễ học và dễ bắt đầu

### GitLab CI/CD hơn Jenkins ở điểm nào?

GitLab CI/CD dễ bắt đầu hơn vì YAML đơn giản hơn Groovy DSL.

Ví dụ muốn chạy Maven test:

```yaml
test:
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn test
```

Người mới nhìn vào có thể hiểu ngay:

```text
Dùng image Maven.
Chạy lệnh mvn test.
```

GitLab cũng không bắt bạn hiểu nhiều thứ ngay từ đầu. Bạn có thể học dần:

```text
job
  -> stage
  -> artifact
  -> cache
  -> rules
  -> include/template
  -> runner executor
```

### Jenkins khó hơn ở đâu?

Jenkinsfile dùng Groovy DSL:

```groovy
pipeline {
  agent any
  stages {
    stage('Test') {
      steps {
        sh 'mvn test'
      }
    }
  }
}
```

Ví dụ cơ bản không khó. Nhưng khi đi sâu, người học phải hiểu thêm:

- Jenkins Controller.
- Jenkins Agent.
- Node label.
- Plugin.
- Credentials binding.
- Declarative pipeline.
- Scripted pipeline.
- Shared library.
- Workspace.
- Jenkins job vs Jenkinsfile.
- Multibranch pipeline.
- Plugin version compatibility.

Vì vậy Jenkins không chỉ là học cú pháp pipeline, mà còn là học cách vận hành một CI server.

Kết luận:

```text
Người mới học CI/CD
  -> GitLab CI/CD dễ vào hơn.

Muốn hiểu enterprise CI/CD sâu
  -> Jenkins đáng học, nhưng đường cong học tập cao hơn.
```

## 4.3. Khả năng biểu diễn pipeline phức tạp

### Jenkins hơn GitLab CI/CD ở điểm nào?

Jenkins mạnh hơn khi pipeline có logic phức tạp vì Jenkinsfile là Groovy DSL, gần với lập trình hơn YAML.

Ví dụ yêu cầu:

```text
Nếu branch là release/*
  -> build bằng JDK 17
Nếu branch là main
  -> build bằng JDK 21
Nếu module payment thay đổi
  -> chạy thêm payment integration test
Nếu deploy production
  -> gọi API tạo change request
  -> chờ approval
  -> deploy
  -> cập nhật ticket
```

Jenkins có thể viết logic kiểu:

```groovy
if (env.BRANCH_NAME.startsWith('release/')) {
  env.JAVA_VERSION = '17'
} else {
  env.JAVA_VERSION = '21'
}
```

Hoặc dùng shared library:

```groovy
@Library('company-ci-lib') _

companyPipeline {
  serviceName = 'payment-service'
  javaVersion = '21'
  deployType = 'kubernetes'
}
```

Shared library giúp công ty đóng gói logic phức tạp vào thư viện dùng chung.

### GitLab CI/CD yếu hơn ở đâu?

GitLab YAML biểu diễn logic bằng `rules`, `extends`, `include`, `needs`, `workflow`, `variables`.

Ví dụ:

```yaml
payment-test:
  stage: test
  script:
    - mvn -pl payment verify
  rules:
    - changes:
        - payment/**/*
```

Làm được, nhưng khi logic quá nhiều nhánh, YAML dễ dài và khó đọc.

Ví dụ YAML có thể trở nên rối khi có:

- Nhiều `rules`.
- Nhiều `extends`.
- Nhiều include lồng nhau.
- Nhiều biến theo môi trường.
- Nhiều job chỉ chạy theo file thay đổi.
- Nhiều service trong monorepo.

Kết luận:

```text
Pipeline chuẩn, ít logic đặc biệt
  -> GitLab CI/CD rõ ràng hơn.

Pipeline có nhiều điều kiện, nhiều hệ thống, nhiều logic doanh nghiệp
  -> Jenkins mạnh hơn.
```

## 4.4. Runner/Agent và môi trường chạy job

Đây là phần rất dễ nhầm. Nhiều người nói:

```text
GitLab Runner mạnh vì chạy container tốt.
Jenkins Agent mạnh vì tùy biến tốt.
```

Câu đó đúng, nhưng chưa đủ. Cần hiểu rõ hơn:

```text
GitLab Runner và Jenkins Agent đều là "nơi thật sự chạy job".

GitLab server / Jenkins Controller chỉ điều phối.
Runner / Agent mới là nơi chạy lệnh như:
  mvn test
  npm test
  docker build
  kubectl apply
```

Khác biệt chính không phải là "cái nào chạy được, cái nào không chạy được". Đa số trường hợp cả hai đều làm được. Khác biệt là:

```text
Làm có tự nhiên không?
Cấu hình có đơn giản không?
Có cần plugin không?
Có dễ chuẩn hóa không?
Có dễ vận hành an toàn không?
```

## 4.4.1. Cùng là máy chạy job, nhưng mô hình tư duy khác nhau

### GitLab Runner

GitLab Runner thường được hiểu theo hướng:

```text
Một runner có thể chạy nhiều job.
Mỗi job có thể chọn Docker image riêng.
Job chạy xong thì container biến mất.
```

Ví dụ:

```yaml
test:
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn test
```

Ở đây bạn không cần quan tâm máy runner đã cài Maven chưa. Chỉ cần runner hỗ trợ Docker executor, job sẽ chạy trong container Maven.

### Jenkins Agent

Jenkins Agent thường được hiểu theo hướng:

```text
Một agent là một node/máy có năng lực cụ thể.
Pipeline chọn agent theo label.
Agent có thể đã cài sẵn tool cần thiết.
```

Ví dụ:

```groovy
pipeline {
  agent { label 'linux-maven' }
  stages {
    stage('Test') {
      steps {
        sh 'mvn test'
      }
    }
  }
}
```

Ở đây bạn đang nói:

```text
Hãy tìm một Jenkins Agent có label linux-maven.
Trên agent đó phải có Maven hoặc môi trường để chạy Maven.
```

Jenkins cũng có thể chạy bằng Docker container, nhưng mô hình truyền thống của Jenkins vẫn xoay quanh agent/node/label nhiều hơn.

## 4.4.2. GitLab làm được agent đặc thù như Jenkins không?

Có, GitLab làm được.

GitLab Runner có thể cài trên máy đặc thù, rồi gắn tag cho runner đó.

Ví dụ bạn có một máy Windows build .NET Framework:

```text
Runner Windows
Tags: windows, dotnet-framework
```

Trong `.gitlab-ci.yml`:

```yaml
build-windows:
  tags:
    - windows
    - dotnet-framework
  script:
    - msbuild MyLegacyApp.sln
```

Ví dụ bạn có một máy nội bộ truy cập được Oracle:

```text
Runner nội bộ
Tags: oracle-client, internal-network
```

Pipeline:

```yaml
integration-test-oracle:
  tags:
    - oracle-client
    - internal-network
  script:
    - ./mvnw verify -Poracle-it
```

Vậy GitLab có làm được không? Có.

Nhưng Jenkins thường tự nhiên hơn trong kiểu này vì Jenkins sinh ra trong thế giới agent/node/label lâu đời. Jenkins UI và plugin ecosystem hỗ trợ quản lý nhiều node đặc thù rất quen thuộc với enterprise.

Ví dụ Jenkins:

```groovy
pipeline {
  agent { label 'oracle-client && internal-network' }

  stages {
    stage('Oracle Integration Test') {
      steps {
        sh './mvnw verify -Poracle-it'
      }
    }
  }
}
```

Kết luận nhỏ:

```text
GitLab CI/CD làm được máy đặc thù bằng runner tags.
Jenkins làm việc này tự nhiên hơn bằng agent labels và node management.
```

## 4.4.3. Jenkins có làm được container sạch như GitLab không?

Có, Jenkins làm được.

Jenkins có thể chạy stage trong Docker container:

```groovy
pipeline {
  agent {
    docker {
      image 'maven:3.9-eclipse-temurin-21'
    }
  }

  stages {
    stage('Test') {
      steps {
        sh 'mvn test'
      }
    }
  }
}
```

Jenkins cũng có Kubernetes plugin để tạo pod tạm thời cho mỗi build:

```groovy
pipeline {
  agent {
    kubernetes {
      yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
    - name: maven
      image: maven:3.9-eclipse-temurin-21
      command:
        - cat
      tty: true
'''
    }
  }

  stages {
    stage('Test') {
      steps {
        container('maven') {
          sh 'mvn test'
        }
      }
    }
  }
}
```

Vậy Jenkins có làm được container sạch không? Có.

Nhưng GitLab CI/CD thường đơn giản hơn cho case này:

```yaml
test:
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn test
```

Ít cấu hình hơn, ít plugin hơn, dễ đọc hơn.

Kết luận nhỏ:

```text
Jenkins làm được container sạch bằng Docker/Kubernetes plugin.
GitLab CI/CD làm việc này tự nhiên và gọn hơn bằng image trong YAML.
```

## 4.4.4. So sánh trực tiếp theo từng loại môi trường chạy

| Nhu cầu | GitLab CI/CD | Jenkins | Ai thuận hơn |
|---|---|---|---|
| Chạy Maven trong Docker image | Rất gọn với `image: maven:...` | Làm được bằng Docker agent/plugin | GitLab |
| Chạy Node.js image riêng | Rất gọn với `image: node:...` | Làm được bằng Docker agent/plugin | GitLab |
| Mỗi job một container sạch | Tự nhiên với Docker/Kubernetes executor | Làm được nhưng cần plugin/cấu hình | GitLab |
| Chạy trên máy Windows đặc thù | Làm được bằng runner tag | Rất quen bằng agent label | Jenkins |
| Chạy trên macOS build iOS | Làm được nếu có GitLab Runner macOS | Làm được bằng Jenkins Agent macOS | Jenkins thường quen hơn |
| Chạy tool nội bộ chỉ cài trên một máy | Làm được bằng specific runner tag | Rất hợp bằng agent label | Jenkins |
| Chạy job trong Kubernetes pod | Làm được bằng Kubernetes executor | Làm được bằng Kubernetes plugin | Hòa, GitLab thường gọn hơn |
| Quản lý nhiều node legacy | Làm được nhưng không phải thế mạnh nhất | Rất mạnh | Jenkins |
| Microservice Docker-based | Rất hợp | Làm được | GitLab |
| Hệ thống mixed Windows/Linux/legacy | Làm được | Rất hợp | Jenkins |

## 4.4.5. Ví dụ cùng một yêu cầu: backend Maven và frontend Node

Yêu cầu:

```text
Job backend dùng Maven 3.9 + JDK 21.
Job frontend dùng Node 22.
Hai job chạy độc lập.
```

### GitLab CI/CD

```yaml
stages:
  - test

backend-test:
  stage: test
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn test

frontend-test:
  stage: test
  image: node:22
  script:
    - cd frontend
    - npm ci
    - npm test
```

GitLab rất gọn vì mỗi job tự chọn image.

### Jenkins

Cách 1: dùng agent đã cài sẵn tool:

```groovy
pipeline {
  agent none

  stages {
    stage('Backend Test') {
      agent { label 'linux-maven-jdk21' }
      steps {
        sh 'mvn test'
      }
    }

    stage('Frontend Test') {
      agent { label 'linux-node22' }
      steps {
        sh '''
          cd frontend
          npm ci
          npm test
        '''
      }
    }
  }
}
```

Cách 2: dùng Docker agent:

```groovy
pipeline {
  agent none

  stages {
    stage('Backend Test') {
      agent {
        docker {
          image 'maven:3.9-eclipse-temurin-21'
        }
      }
      steps {
        sh 'mvn test'
      }
    }

    stage('Frontend Test') {
      agent {
        docker {
          image 'node:22'
        }
      }
      steps {
        sh '''
          cd frontend
          npm ci
          npm test
        '''
      }
    }
  }
}
```

Nhận xét:

```text
GitLab viết ngắn hơn và tự nhiên hơn.
Jenkins làm được, nhưng Jenkinsfile dài hơn.
Nếu Jenkins agent đã được chuẩn hóa tốt thì vẫn ổn.
```

## 4.4.6. Ví dụ cùng một yêu cầu: build .NET Framework cũ trên Windows

Yêu cầu:

```text
Ứng dụng cũ cần Windows Server.
Cần Visual Studio Build Tools.
Cần MSBuild.
Không dễ đóng gói vào Docker Linux.
```

### GitLab CI/CD

Bạn cần cài GitLab Runner trên máy Windows:

```text
Windows Runner
Tags: windows, msbuild
Executor: shell
```

Pipeline:

```yaml
build-legacy-dotnet:
  tags:
    - windows
    - msbuild
  script:
    - msbuild LegacyApp.sln /p:Configuration=Release
```

Làm được.

### Jenkins

Bạn cần Jenkins Agent trên Windows:

```text
Windows Agent
Label: windows-msbuild
```

Jenkinsfile:

```groovy
pipeline {
  agent { label 'windows-msbuild' }

  stages {
    stage('Build') {
      steps {
        bat 'msbuild LegacyApp.sln /p:Configuration=Release'
      }
    }
  }
}
```

Nhận xét:

```text
Cả hai làm được.
Jenkins thường quen thuộc hơn với mô hình agent Windows lâu đời.
GitLab làm được bằng runner tag, nhưng GitLab CI thường sáng hơn trong workflow container hiện đại.
```

## 4.4.7. Ví dụ cùng một yêu cầu: deploy qua mạng nội bộ

Yêu cầu:

```text
Server staging nằm trong mạng nội bộ.
Runner public không truy cập được.
Job CI cần SSH hoặc kubectl vào staging.
```

### GitLab CI/CD

Cài GitLab Runner trong mạng nội bộ:

```text
Internal GitLab Runner
Tags: internal, staging
```

Pipeline:

```yaml
deploy-staging:
  tags:
    - internal
    - staging
  script:
    - ssh deploy@staging-server 'docker compose up -d'
```

### Jenkins

Cài Jenkins Agent trong mạng nội bộ:

```text
Internal Jenkins Agent
Label: internal-staging
```

Jenkinsfile:

```groovy
pipeline {
  agent { label 'internal-staging' }

  stages {
    stage('Deploy Staging') {
      steps {
        sh "ssh deploy@staging-server 'docker compose up -d'"
      }
    }
  }
}
```

Nhận xét:

```text
Cả hai làm được bằng cách đặt runner/agent trong mạng nội bộ.
Khác biệt không nằm ở khả năng truy cập mạng.
Khác biệt nằm ở hệ sinh thái quản lý runner/agent và cách pipeline được chuẩn hóa.
```

## 4.4.8. Về bảo mật runner/agent

Runner/Agent là nơi chạy code, nên đây là vùng rủi ro cao.

### GitLab CI/CD

GitLab thường phân runner theo tag:

```text
shared-runner
docker-runner
internal-runner
production-runner
```

Job chọn runner:

```yaml
deploy-production:
  tags:
    - production-runner
  script:
    - ./deploy-prod.sh
```

Cần chú ý:

- Không cho branch không tin cậy dùng production runner.
- Không để secret production trên runner dùng chung.
- Protected variable phải đi với protected branch/tag.
- Shell runner cần được quản lý kỹ vì job chạy trực tiếp trên máy.

### Jenkins

Jenkins thường phân agent theo label và permission:

```text
trusted-linux
untrusted-pr
production-deploy
windows-build
```

Pipeline:

```groovy
agent { label 'production-deploy' }
```

Cần chú ý:

- Không cho job pull request không tin cậy chạy trên agent có secret.
- Không để credential production dùng quá rộng.
- Không chạy build nặng hoặc job untrusted trên controller.
- Dọn workspace để tránh job sau đọc file job trước.

Kết luận bảo mật:

```text
GitLab dễ kiểm soát theo protected branch/tag/variable nếu dùng đúng.
Jenkins rất mạnh về phân quyền/credential, nhưng cấu hình sai cũng nguy hiểm hơn vì quá linh hoạt.
```

## 4.4.9. Docker build: bên nào hơn?

Docker build là case thú vị vì cả hai đều làm được, nhưng đều có bẫy.

### GitLab CI/CD

Ví dụ dùng Docker-in-Docker:

```yaml
build-image:
  image: docker:27
  services:
    - docker:27-dind
  script:
    - docker build -t my-service:$CI_COMMIT_SHA .
```

Ưu điểm:

- YAML gọn.
- Rất phổ biến trong GitLab CI.
- Dễ chuẩn hóa.

Nhược điểm:

- Docker-in-Docker cần cấu hình bảo mật cẩn thận.
- Cache layer nếu không cấu hình tốt sẽ chậm.
- Privileged runner có rủi ro.

### Jenkins

Ví dụ Jenkins Agent có Docker daemon:

```groovy
pipeline {
  agent { label 'docker-builder' }

  stages {
    stage('Build Image') {
      steps {
        sh 'docker build -t my-service:${GIT_COMMIT} .'
      }
    }
  }
}
```

Ưu điểm:

- Dễ tận dụng Docker cache trên agent lâu dài.
- Dễ dùng agent riêng cho build image.
- Dễ tích hợp tool scan/signing nội bộ.

Nhược điểm:

- Agent lâu dài dễ nhiễm trạng thái.
- Docker socket trên agent là quyền rất mạnh.
- Cần tự quản lý disk cleanup, cache cleanup.

Kết luận:

```text
GitLab CI/CD gọn hơn cho Docker build chuẩn.
Jenkins linh hoạt hơn nếu build image cần cache đặc thù, signing, scan, hoặc tool nội bộ.
```

## 4.4.10. Kubernetes executor/pod agent

Cả hai đều có thể chạy job trong Kubernetes.

### GitLab Kubernetes executor

GitLab Runner có thể tạo pod cho từng job:

```text
GitLab Runner
  -> nhận job
  -> tạo Kubernetes pod
  -> pod chạy script
  -> pod kết thúc
```

Pipeline vẫn rất gọn:

```yaml
test:
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn test
```

### Jenkins Kubernetes plugin

Jenkins cũng có thể tạo pod agent:

```text
Jenkins Controller
  -> Kubernetes plugin
  -> tạo pod agent
  -> pod chạy stage
  -> pod kết thúc
```

Nhưng Jenkinsfile thường dài hơn nếu cần khai báo pod nhiều container.

Kết luận:

```text
Nếu chỉ cần mỗi job một container đơn giản
  -> GitLab Kubernetes executor thường gọn hơn.

Nếu cần pod agent phức tạp nhiều container, custom workspace, shared library logic
  -> Jenkins vẫn rất mạnh nhưng cấu hình dài hơn.
```

## 4.4.11. Kết luận 4.4: không phải làm được hay không, mà là làm có tự nhiên không

Ví dụ:

```yaml
backend-test:
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn test

frontend-test:
  image: node:22
  script:
    - npm ci
    - npm test
```

Mỗi job chọn một Docker image riêng. Không cần cài Maven và Node chung trên cùng một máy.

Kết luận cuối:

```text
Nếu môi trường chạy job có thể mô tả bằng Docker image
  -> GitLab CI/CD thường gọn, sạch, dễ chuẩn hóa hơn.

Nếu môi trường chạy job là một máy đặc thù với nhiều tool cài tay
  -> Jenkins thường tự nhiên, linh hoạt, dễ điều phối hơn.

Nếu cần mạng nội bộ
  -> cả hai đều làm được bằng self-hosted runner/agent trong mạng nội bộ.

Nếu cần Windows/macOS
  -> cả hai đều làm được, Jenkins thường quen hơn trong enterprise legacy.

Nếu cần Kubernetes pod tạm thời
  -> cả hai đều làm được, GitLab thường ngắn hơn, Jenkins tùy biến hơn.

Nếu cần Docker build đơn giản
  -> GitLab thường gọn hơn.

Nếu cần Docker build có cache/signing/scan/tool nội bộ phức tạp
  -> Jenkins thường linh hoạt hơn.
```

## 4.5. Plugin và tích hợp hệ thống

### Jenkins hơn GitLab CI/CD ở điểm nào?

Jenkins có plugin ecosystem rất lớn. Đây là lý do Jenkins sống rất dai trong enterprise.

Jenkins có plugin cho:

- GitLab.
- GitHub.
- Bitbucket.
- SVN.
- Maven.
- Gradle.
- Docker.
- Kubernetes.
- SonarQube.
- Nexus/Artifactory.
- Jira.
- ServiceNow.
- LDAP/Active Directory.
- Email/Slack/Teams.
- Credentials.
- SSH.
- Pipeline utility.

Ví dụ doanh nghiệp có quy trình:

```text
Build app
  -> scan SonarQube
  -> publish Nexus
  -> tạo change request trên ServiceNow
  -> chờ approval
  -> deploy production
  -> cập nhật Jira ticket
  -> gửi mail báo cáo
```

Jenkins thường có plugin hoặc cách tích hợp sẵn cho các bước này.

### GitLab CI/CD yếu hơn ở đâu?

GitLab CI/CD cũng tích hợp nhiều thứ, nhưng thường theo hướng:

```text
Viết script trong YAML
Gọi API
Dùng Docker image/tool CLI
```

Ví dụ gọi Jira hoặc ServiceNow vẫn làm được bằng `curl`, nhưng cảm giác sẽ thủ công hơn:

```yaml
create-change-request:
  script:
    - curl -X POST "$SERVICENOW_URL" -H "Authorization: Bearer $TOKEN"
```

GitLab mạnh nếu bạn đi theo ecosystem GitLab. Jenkins mạnh nếu bạn phải nối rất nhiều hệ thống khác nhau.

Kết luận:

```text
Tích hợp trong hệ GitLab
  -> GitLab CI/CD gọn.

Tích hợp nhiều hệ thống enterprise/legacy
  -> Jenkins thường mạnh hơn.
```

## 4.6. Khả năng chuẩn hóa nhiều project

### GitLab CI/CD hơn Jenkins ở điểm nào?

GitLab rất mạnh khi công ty có nhiều service giống nhau và muốn áp template.

Ví dụ công ty có 100 Spring Boot service. Team DevOps tạo template:

```yaml
include:
  - project: devops/ci-templates
    file: java-spring-boot.yml
```

Mỗi service chỉ cần khai báo vài biến:

```yaml
variables:
  SERVICE_NAME: order-service
  JAVA_VERSION: "21"
```

Ưu điểm:

- Pipeline các service giống nhau.
- Dễ enforce chuẩn chung.
- Dễ update template một nơi.
- Developer ít phải viết pipeline.

### Jenkins hơn ở đâu?

Jenkins cũng có shared library:

```groovy
@Library('company-ci-lib') _

springBootPipeline {
  serviceName = 'order-service'
  javaVersion = '21'
}
```

Shared library mạnh hơn GitLab template ở khả năng viết logic lập trình phức tạp.

Ví dụ:

```text
Nếu service type là public-api
  -> thêm API security scan
Nếu service type là batch
  -> bỏ qua smoke test HTTP
Nếu team là payment
  -> yêu cầu approval từ payment-lead
```

Jenkins shared library có thể biểu diễn logic này đẹp hơn YAML nếu logic quá nhiều.

Kết luận:

```text
Chuẩn hóa pipeline dạng template đơn giản
  -> GitLab CI/CD rất tốt.

Chuẩn hóa pipeline có logic phức tạp như framework nội bộ
  -> Jenkins shared library mạnh hơn.
```

## 4.7. Vận hành và bảo trì

### GitLab CI/CD hơn Jenkins ở điểm nào?

Nếu công ty đã dùng GitLab, CI/CD là một phần của GitLab. Bạn không cần vận hành thêm một Jenkins Controller riêng.

Bạn vẫn phải vận hành GitLab Runner, nhưng tổng thể ít mảnh ghép hơn:

```text
GitLab
  -> GitLab Runner
```

Trong khi Jenkins thường có:

```text
Jenkins Controller
  -> Jenkins Agent
  -> Plugin
  -> Credentials
  -> Backup Jenkins Home
  -> Upgrade Jenkins
  -> Upgrade plugins
```

Jenkins vận hành không khó nếu có team tốt, nhưng tốn công hơn.

### Jenkins yếu hơn ở đâu?

Jenkins dễ bị các vấn đề:

- Plugin quá nhiều.
- Plugin cũ không tương thích.
- Upgrade Jenkins làm job lỗi.
- Jenkins Home phình to.
- Agent offline.
- Workspace đầy disk.
- Credential scope không rõ.
- Job cấu hình bằng UI không version control.

GitLab CI/CD cũng có rủi ro vận hành, nhưng nếu pipeline được chuẩn hóa bằng YAML và runner sạch, thường dễ kiểm soát hơn Jenkins legacy.

Kết luận:

```text
Muốn ít vận hành hơn, nhất là khi đã dùng GitLab
  -> GitLab CI/CD có lợi thế.

Chấp nhận vận hành nhiều để đổi lấy khả năng tùy biến cao
  -> Jenkins phù hợp.
```

## 4.8. Bảo mật và phân quyền

### GitLab CI/CD hơn Jenkins ở điểm nào?

GitLab có lợi thế vì source code, merge request, protected branch, protected tag, variable và environment nằm cùng một hệ thống.

Ví dụ:

```text
PROD_KUBE_CONFIG chỉ cho protected tag dùng.
Merge request từ branch thường không dùng được secret production.
Deploy production cần manual job.
```

Trong `.gitlab-ci.yml`:

```yaml
deploy-production:
  script:
    - ./deploy-prod.sh
  when: manual
  rules:
    - if: '$CI_COMMIT_TAG'
```

Kết hợp với protected variable, GitLab kiểm soát khá gọn.

### Jenkins mạnh hơn ở đâu?

Jenkins mạnh khi công ty cần mô hình phân quyền và credential rất tùy biến qua plugin:

- LDAP/Active Directory.
- Role-based authorization.
- Folder-level permission.
- Credential scope theo folder/job.
- Approval bằng input step.
- Tích hợp hệ thống change-management.

Ví dụ Jenkins folder:

```text
Folder: payment
  -> chỉ payment-dev được build
  -> chỉ payment-release-manager được deploy production
  -> credential production chỉ nằm trong folder payment
```

Jenkins làm được rất sâu, nhưng cần cấu hình cẩn thận. Nếu cấu hình sai, Jenkins cũng dễ thành rủi ro lớn.

Kết luận:

```text
Security gắn với Git workflow, protected branch/tag, GitLab variable
  -> GitLab CI/CD gọn và dễ kiểm soát hơn.

Security enterprise nhiều tầng, folder/job/credential phức tạp
  -> Jenkins mạnh, nhưng cần quản trị tốt.
```

## 4.9. Monorepo và pipeline lớn

### GitLab CI/CD mạnh ở đâu?

GitLab hỗ trợ:

- `rules:changes`.
- `needs`.
- Child pipeline.
- Multi-project pipeline.
- Include template.

Ví dụ chỉ chạy job khi thư mục backend thay đổi:

```yaml
backend-test:
  script:
    - mvn -pl backend test
  rules:
    - changes:
        - backend/**/*
```

Với monorepo vừa phải, GitLab làm tốt.

### Jenkins mạnh ở đâu?

Jenkins mạnh nếu monorepo cần logic tự sinh pipeline hoặc logic phân tích thay đổi phức tạp.

Ví dụ:

```groovy
def changedServices = sh(
  script: './scripts/detect-changed-services.sh',
  returnStdout: true
).trim().split('\n')

for (service in changedServices) {
  stage("Test ${service}") {
    sh "./scripts/test-service.sh ${service}"
  }
}
```

Jenkins có thể lập trình pipeline động dễ hơn YAML.

Kết luận:

```text
Monorepo vừa phải, rule theo folder rõ ràng
  -> GitLab CI/CD đủ tốt.

Monorepo lớn, cần sinh pipeline động bằng logic riêng
  -> Jenkins thường linh hoạt hơn.
```

## 4.10. Hệ thống legacy

### Jenkins hơn GitLab CI/CD rõ nhất ở đâu?

Đây là một trong những điểm Jenkins thắng rất rõ.

Ví dụ công ty có hệ thống:

```text
App Java 8 chạy trên WebLogic.
Build bằng Ant.
Source code một phần trong SVN.
Deploy bằng script shell cũ.
Artifact lưu vào Nexus.
Trước khi deploy phải gọi hệ thống change request nội bộ.
Server production chỉ truy cập được từ một máy jump server.
```

Jenkins xử lý kiểu này rất hợp vì:

- Có thể checkout SVN.
- Có thể chạy Ant.
- Có thể chạy trên agent đã cài WebLogic tool.
- Có thể SSH qua jump server.
- Có thể gọi script cũ.
- Có thể gắn approval.
- Có thể publish Nexus.

GitLab CI/CD vẫn làm được nhiều phần, nhưng thường sẽ phải viết nhiều script thủ công hơn và setup runner đặc thù.

Kết luận:

```text
Hệ thống hiện đại, Docker/Kubernetes, GitLab repo
  -> GitLab CI/CD rất hợp.

Hệ thống cũ, nhiều script/tool nội bộ, deploy phức tạp
  -> Jenkins thường hợp hơn.
```

## 4.11. Developer experience

### GitLab CI/CD tốt hơn ở đâu?

Developer thường thích GitLab CI/CD khi:

- Pipeline nằm ngay trong Merge Request.
- Log dễ mở từ GitLab.
- Không cần biết Jenkins job tên gì.
- Không cần xin quyền Jenkins riêng.
- File `.gitlab-ci.yml` nằm ngay repo.

Ví dụ:

```text
MR bị đỏ.
Developer click vào failed job.
Đọc log.
Sửa code.
Push lại.
Pipeline chạy lại.
```

Rất liền mạch.

### Jenkins tốt hơn ở đâu?

Jenkins tốt hơn với release engineer hoặc DevOps engineer cần dashboard điều phối nhiều job:

```text
Build nightly
Release UAT
Deploy production
Rollback
Run database script
Trigger downstream job
```

Jenkins UI có thể được tổ chức thành nhiều folder/job phục vụ vận hành release.

Kết luận:

```text
Developer hằng ngày làm MR và fix pipeline
  -> GitLab CI/CD thường mượt hơn.

Release/operation cần điều phối job phức tạp
  -> Jenkins thường quen thuộc hơn trong enterprise.
```

## 4.12. Kết luận phần so sánh sâu

Không nên nhớ kiểu:

```text
GitLab tốt hơn Jenkins
```

hoặc:

```text
Jenkins tốt hơn GitLab
```

Nên nhớ chính xác hơn:

```text
GitLab CI/CD hơn Jenkins ở:
  - tích hợp trực tiếp với GitLab Merge Request
  - dễ bắt đầu hơn
  - pipeline YAML chuẩn hóa tốt
  - Docker/container workflow gọn
  - ít mảnh ghép hơn nếu đã dùng GitLab
  - phù hợp microservice hiện đại

Jenkins hơn GitLab CI/CD ở:
  - tùy biến pipeline cực sâu
  - plugin ecosystem lớn
  - tích hợp enterprise/legacy rất mạnh
  - agent đặc thù linh hoạt
  - shared library có thể thành framework CI nội bộ
  - phù hợp hệ thống cũ, nhiều tool nội bộ, nhiều nguồn code
```

Một câu rất thực tế:

```text
GitLab CI/CD giúp team chuẩn hóa tốt hơn.
Jenkins giúp team xử lý ngoại lệ tốt hơn.
```

Nếu hệ thống càng hiện đại và chuẩn hóa, GitLab CI/CD càng sáng. Nếu hệ thống càng cũ, càng nhiều ngoại lệ, càng nhiều tích hợp đặc biệt, Jenkins càng có đất diễn.

## 5. GitLab CI/CD là gì?

GitLab CI/CD là hệ thống CI/CD tích hợp trực tiếp trong GitLab. Khi code nằm trên GitLab, bạn chỉ cần thêm file:

```text
.gitlab-ci.yml
```

GitLab sẽ đọc file này và tạo pipeline khi có sự kiện như:

- Push code.
- Tạo merge request.
- Merge vào branch chính.
- Tạo tag.
- Chạy pipeline thủ công.
- Chạy theo lịch.

Ví dụ đơn giản:

```yaml
stages:
  - test

test:
  stage: test
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn --batch-mode test
```

Giải thích:

- `stages`: danh sách giai đoạn.
- `test`: tên job.
- `stage: test`: job này thuộc stage `test`.
- `image`: Docker image dùng để chạy job.
- `script`: lệnh thực thi.

Flow cơ bản:

```text
Developer push code lên GitLab
        |
        v
GitLab tạo pipeline
        |
        v
GitLab Runner chạy job
        |
        v
Kết quả hiện trên Merge Request
```

## 6. Jenkins là gì?

Jenkins là một CI/CD server độc lập, mã nguồn mở, được dùng rất nhiều trong doanh nghiệp và hệ thống cũ.

Jenkins thường có kiến trúc:

```text
Jenkins Controller
  -> nhận webhook từ Git
  -> đọc Jenkinsfile
  -> điều phối pipeline
  -> gửi việc cho Jenkins Agent
  -> thu log, artifact, trạng thái
```

Pipeline thường viết trong:

```text
Jenkinsfile
```

Ví dụ đơn giản:

```groovy
pipeline {
  agent any

  stages {
    stage('Test') {
      steps {
        sh './mvnw --batch-mode test'
      }
    }
  }
}
```

Giải thích:

- `pipeline`: khai báo pipeline.
- `agent any`: chạy trên agent bất kỳ phù hợp.
- `stages`: danh sách giai đoạn.
- `stage('Test')`: một giai đoạn tên Test.
- `steps`: các bước trong giai đoạn.
- `sh`: chạy lệnh shell trên Linux agent.

Flow cơ bản:

```text
Developer push code lên GitLab/GitHub/Bitbucket
        |
        v
Webhook gọi Jenkins
        |
        v
Jenkins Controller nhận event
        |
        v
Jenkins Agent chạy pipeline
        |
        v
Jenkins báo trạng thái ngược về Git provider
```

## 7. Khác biệt bản chất

### 7.1. GitLab CI/CD là native với GitLab

Nếu code nằm trong GitLab, GitLab CI/CD rất tự nhiên vì cùng một nền tảng có:

- Git repository.
- Merge request.
- Code review.
- Pipeline.
- Runner.
- Artifact.
- Container registry.
- Package registry.
- Security scan.
- Environment.
- Deployment.

Developer có thể xem pipeline ngay trong merge request.

### 7.2. Jenkins là hệ thống độc lập

Jenkins không phụ thuộc vào một Git platform cụ thể. Jenkins có thể kết nối với:

- GitLab.
- GitHub.
- Bitbucket.
- SVN.
- Git server nội bộ.
- Script nội bộ.
- Tool legacy.

Vì vậy Jenkins rất mạnh trong môi trường enterprise có nhiều hệ thống cũ.

## 8. GitLab Runner vs Jenkins Agent

### 8.1. GitLab Runner là gì?

GitLab Runner là chương trình chạy job cho GitLab CI/CD.

GitLab server không trực tiếp chạy lệnh `mvn test` hay `docker build`. Nó giao việc cho runner.

```text
GitLab Server
  -> tạo pipeline
  -> giao job cho GitLab Runner
  -> Runner chạy script
  -> Runner gửi log/kết quả về GitLab
```

GitLab Runner có nhiều executor:

| Executor | Ý nghĩa |
|---|---|
| Shell executor | Chạy lệnh trực tiếp trên máy runner |
| Docker executor | Mỗi job chạy trong Docker container |
| Kubernetes executor | Mỗi job chạy trong Kubernetes pod |
| SSH executor | Chạy job qua SSH |
| Custom executor | Tự định nghĩa cách chạy |

Ví dụ Docker executor:

```yaml
test:
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn test
```

Job này chạy trong container Maven.

### 8.2. Jenkins Agent là gì?

Jenkins Agent là máy hoặc container chạy job do Jenkins Controller giao.

```text
Jenkins Controller
  -> quản lý pipeline
  -> gửi stage/step tới Agent
  -> Agent chạy lệnh
  -> gửi log/kết quả về Controller
```

Agent có thể là:

- Máy Linux.
- Máy Windows.
- Máy macOS.
- Docker container.
- Kubernetes pod.
- VM nội bộ.
- Server có tool đặc biệt như Android SDK, Xcode, Oracle client.

Ví dụ:

```groovy
pipeline {
  agent { label 'linux-maven' }

  stages {
    stage('Test') {
      steps {
        sh 'mvn test'
      }
    }
  }
}
```

Ý nghĩa:

```text
Chạy pipeline trên Jenkins Agent có label linux-maven.
```

### 8.3. So sánh Runner và Agent

| Tiêu chí | GitLab Runner | Jenkins Agent |
|---|---|---|
| Nhận job từ | GitLab server | Jenkins Controller |
| File pipeline | `.gitlab-ci.yml` | `Jenkinsfile` |
| Cách chọn môi trường | `image`, `tags`, executor | `agent`, `label`, node, Docker/Kubernetes plugin |
| Container support | Rất tự nhiên với Docker executor | Làm tốt nhưng thường cần plugin/cấu hình |
| Tùy biến máy chạy | Tốt | Rất mạnh |
| Phù hợp | Pipeline chuẩn, GitLab ecosystem | Agent đặc thù, legacy, enterprise |

## 9. Pipeline model: YAML vs Jenkinsfile

### 9.1. GitLab dùng YAML

```yaml
stages:
  - build
  - test
  - package

build:
  stage: build
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn --batch-mode compile

test:
  stage: test
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn --batch-mode test

package:
  stage: package
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn --batch-mode package -DskipTests
  artifacts:
    paths:
      - target/*.jar
```

YAML dễ đọc với pipeline chuẩn, nhưng khi có quá nhiều `rules`, `extends`, `include`, `variables`, file có thể dài và khó debug.

### 9.2. Jenkins dùng Groovy DSL

```groovy
pipeline {
  agent { label 'linux-maven' }

  stages {
    stage('Build') {
      steps {
        sh 'mvn --batch-mode compile'
      }
    }

    stage('Test') {
      steps {
        sh 'mvn --batch-mode test'
      }
    }

    stage('Package') {
      steps {
        sh 'mvn --batch-mode package -DskipTests'
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
      }
    }
  }
}
```

Groovy mạnh hơn YAML vì viết được logic phức tạp hơn, nhưng cũng dễ bị lạm dụng khiến pipeline khó đọc.

## 10. Ưu điểm của GitLab CI/CD

### 10.1. Tích hợp sâu với GitLab

Pipeline hiển thị trực tiếp trong merge request:

```text
Merge Request
  -> pipeline pass/fail
  -> job log
  -> artifact
  -> test report
  -> security/code quality report nếu cấu hình
```

Developer không phải nhảy qua nhiều hệ thống.

### 10.2. Dễ bắt đầu

Với project Java đơn giản:

```yaml
test:
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn test
```

Người mới thường hiểu GitLab CI nhanh hơn Jenkins.

### 10.3. Runner container-friendly

GitLab CI rất hợp với Docker:

```yaml
image: node:22

test:
  script:
    - npm ci
    - npm test
```

Mỗi job có thể chọn image khác nhau.

### 10.4. Template và include tốt

Công ty có thể tạo template chung:

```yaml
include:
  - project: devops/ci-templates
    file: java-maven.yml
```

Rất hợp nếu có nhiều microservice giống nhau.

### 10.5. Registry và package tích hợp

GitLab có thể tích hợp:

- Container Registry.
- Package Registry.
- Dependency Proxy.

Pipeline build image rồi push về registry khá liền mạch.

## 11. Nhược điểm của GitLab CI/CD

### 11.1. Phụ thuộc GitLab ecosystem

Nếu code không nằm trên GitLab, lợi thế giảm.

### 11.2. YAML lớn khó bảo trì

Ví dụ:

```yaml
rules:
  - if: '$CI_COMMIT_BRANCH == "main"'
    changes:
      - backend/**/*
  - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
```

Không sai, nhưng nếu lặp nhiều sẽ khó đọc.

### 11.3. GitLab self-managed cần vận hành nghiêm túc

Nếu công ty tự host GitLab, cần lo:

- Upgrade.
- Backup.
- Database.
- Object storage.
- Container registry storage.
- Runner capacity.
- Security patch.
- Monitoring.

### 11.4. Runner dùng sai dễ rủi ro

Shell runner trên máy lâu dài có thể bị nhiễm trạng thái:

```text
Job A để lại file.
Job B chạy sau bị ảnh hưởng.
```

Runner có secret production không nên chạy job từ branch không tin cậy.

## 12. Ưu điểm của Jenkins

### 12.1. Cực kỳ linh hoạt

Jenkins có thể:

- Build Java, Node.js, .NET, Go, Python.
- Gọi script shell cũ.
- Tích hợp Nexus, Artifactory, SonarQube, Jira, ServiceNow.
- Deploy VM, Kubernetes, OpenShift.
- Chạy job theo lịch.
- Tạo job có tham số.
- Approval thủ công.
- Multi-branch pipeline.

### 12.2. Plugin ecosystem rất lớn

Jenkins có plugin cho:

- Git.
- GitLab.
- GitHub.
- Bitbucket.
- Maven.
- Gradle.
- Docker.
- Kubernetes.
- SonarQube.
- Credentials.
- LDAP/AD.
- Email/Slack.

Đây là lý do Jenkins tồn tại rất mạnh trong enterprise.

### 12.3. Tốt với hệ thống legacy

Nhiều công ty có script cũ:

```bash
./deploy_to_uat.sh
./publish_to_nexus.sh
./restart_legacy_service.sh
```

Jenkins có thể bọc chúng vào pipeline mà không cần thay toàn bộ hệ thống ngay.

### 12.4. Agent rất tùy biến

Bạn có thể có agent:

```text
linux-maven
windows-dotnet
macos-ios
docker-builder
oracle-client
high-memory
gpu
```

Pipeline chọn agent theo label:

```groovy
agent { label 'oracle-client' }
```

## 13. Nhược điểm của Jenkins

### 13.1. Cần vận hành nhiều

Jenkins cần quản lý:

- Controller.
- Agent.
- Plugin.
- Credential.
- Backup.
- Upgrade.
- Disk cleanup.
- Security.
- Permission.

Nếu không có người chăm, Jenkins rất dễ thành hệ thống khó kiểm soát.

### 13.2. Plugin debt

Plugin vừa là sức mạnh vừa là nợ kỹ thuật:

- Plugin cũ không maintain.
- Plugin xung đột version.
- Upgrade Jenkins làm plugin lỗi.
- Plugin có lỗ hổng bảo mật.
- Job phụ thuộc plugin nên khó migrate.

### 13.3. Job cấu hình bằng UI dễ mất kiểm soát

Job cấu hình bằng UI có vấn đề:

- Không version control.
- Không review được.
- Khó biết ai sửa.
- Khó tái tạo trên Jenkins mới.

Best practice là dùng `Jenkinsfile`.

### 13.4. Jenkinsfile có thể quá phức tạp

Vì Jenkinsfile dùng Groovy, team có thể viết logic quá nhiều, làm pipeline giống một chương trình khó maintain.

## 14. Security: GitLab CI/CD vs Jenkins

### 14.1. Secret trong GitLab CI/CD

GitLab dùng CI/CD Variables:

```text
Settings
  -> CI/CD
  -> Variables
```

Có thể cấu hình:

- Masked.
- Protected.
- Environment scoped.
- File variable.

Ví dụ:

```text
DOCKER_USERNAME
DOCKER_PASSWORD
PROD_KUBE_CONFIG
```

Protected variable chỉ dùng cho protected branch/tag như `main` hoặc `v1.2.3`.

### 14.2. Secret trong Jenkins

Jenkins dùng Credentials store.

Credential có thể là:

- Username/password.
- Secret text.
- SSH private key.
- Certificate.
- File secret.

Ví dụ:

```groovy
withCredentials([usernamePassword(
  credentialsId: 'docker-registry',
  usernameVariable: 'DOCKER_USER',
  passwordVariable: 'DOCKER_PASS'
)]) {
  sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin registry.example.com'
}
```

### 14.3. Rủi ro chung

Cả hai đều nguy hiểm nếu cấu hình sai:

- In secret ra log.
- Cấp secret production cho branch không tin cậy.
- Runner/agent dùng chung giữa trusted và untrusted job.
- Docker socket bị lạm dụng.
- Job có quyền quá rộng.
- Script CI chạy lệnh nguy hiểm.

Best practice:

```text
Tách runner/agent theo trust level.
Không cấp secret production cho merge request không tin cậy.
Dùng protected branch/tag.
Dùng quyền nhỏ nhất.
Không in secret ra log.
Không chạy job untrusted trên agent có network production.
Dọn workspace sau job.
Cập nhật runner/agent/plugin thường xuyên.
```

## 15. Artifact, cache và registry

### 15.1. GitLab CI/CD

Artifact:

```yaml
package:
  script:
    - mvn package
  artifacts:
    paths:
      - target/*.jar
```

Cache:

```yaml
cache:
  key: maven
  paths:
    - .m2/repository
```

Build image:

```yaml
build-image:
  image: docker:27
  services:
    - docker:27-dind
  script:
    - docker login -u "$CI_REGISTRY_USER" -p "$CI_REGISTRY_PASSWORD" "$CI_REGISTRY"
    - docker build -t "$CI_REGISTRY_IMAGE:$CI_COMMIT_SHA" .
    - docker push "$CI_REGISTRY_IMAGE:$CI_COMMIT_SHA"
```

### 15.2. Jenkins

Artifact:

```groovy
archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
```

Build image:

```groovy
sh 'docker build -t registry.example.com/my-service:${GIT_COMMIT} .'
sh 'docker push registry.example.com/my-service:${GIT_COMMIT}'
```

Cache trong Jenkins thường phụ thuộc cách setup agent:

- Maven local repo trên agent.
- Docker volume.
- Shared cache.
- Nexus proxy.

## 16. SonarQube

SonarQube rất phổ biến ở công ty lớn để kiểm tra chất lượng code.

### 16.1. GitLab CI/CD + SonarQube

```yaml
sonarqube-check:
  image: maven:3.9-eclipse-temurin-21
  stage: test
  variables:
    SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"
  cache:
    key: "${CI_JOB_NAME}"
    paths:
      - .sonar/cache
  script:
    - mvn --batch-mode verify sonar:sonar
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == "main"'
```

`SONAR_TOKEN` nên để trong GitLab CI/CD Variables.

### 16.2. Jenkins + SonarQube

```groovy
pipeline {
  agent { label 'linux-maven' }

  stages {
    stage('SonarQube') {
      steps {
        withSonarQubeEnv('sonarqube') {
          sh 'mvn --batch-mode verify sonar:sonar'
        }
      }
    }
  }
}
```

Jenkins thường dùng SonarQube plugin để cấu hình server và token.

## 17. Docker image và Harbor

### 17.1. GitLab CI/CD + Harbor

```yaml
build-image:
  stage: package
  image: docker:27
  services:
    - docker:27-dind
  variables:
    IMAGE_TAG: "$HARBOR_REGISTRY/my-team/my-service:$CI_COMMIT_SHA"
  script:
    - echo "$HARBOR_PASSWORD" | docker login "$HARBOR_REGISTRY" -u "$HARBOR_USERNAME" --password-stdin
    - docker build -t "$IMAGE_TAG" .
    - docker push "$IMAGE_TAG"
```

### 17.2. Jenkins + Harbor

```groovy
stage('Build Image') {
  steps {
    withCredentials([usernamePassword(
      credentialsId: 'harbor-credential',
      usernameVariable: 'HARBOR_USERNAME',
      passwordVariable: 'HARBOR_PASSWORD'
    )]) {
      sh '''
        echo "$HARBOR_PASSWORD" | docker login harbor.example.com -u "$HARBOR_USERNAME" --password-stdin
        docker build -t harbor.example.com/my-team/my-service:${GIT_COMMIT} .
        docker push harbor.example.com/my-team/my-service:${GIT_COMMIT}
      '''
    }
  }
}
```

## 18. Deploy Kubernetes

### 18.1. GitLab CI/CD deploy Kubernetes

```yaml
deploy-staging:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl set image deployment/my-service my-service="$IMAGE_TAG" -n staging
    - kubectl rollout status deployment/my-service -n staging
    - curl --fail https://staging.example.com/actuator/health
  environment:
    name: staging
  rules:
    - if: '$CI_COMMIT_BRANCH == "main"'
```

### 18.2. Jenkins deploy Kubernetes

```groovy
stage('Deploy Staging') {
  steps {
    withKubeConfig([credentialsId: 'staging-kubeconfig']) {
      sh '''
        kubectl set image deployment/my-service my-service=harbor.example.com/my-team/my-service:${GIT_COMMIT} -n staging
        kubectl rollout status deployment/my-service -n staging
        curl --fail https://staging.example.com/actuator/health
      '''
    }
  }
}
```

Production nên có approval hoặc protected environment.

## 19. Manual approval

### 19.1. GitLab CI/CD

```yaml
deploy-production:
  stage: deploy
  script:
    - ./deploy-prod.sh
  environment:
    name: production
  when: manual
  rules:
    - if: '$CI_COMMIT_TAG'
```

Ý nghĩa:

```text
Chỉ deploy production khi có tag và có người bấm manual job.
```

### 19.2. Jenkins

```groovy
stage('Approval') {
  steps {
    input message: 'Deploy to production?', ok: 'Deploy'
  }
}

stage('Deploy Production') {
  steps {
    sh './deploy-prod.sh'
  }
}
```

Jenkins rất mạnh với workflow cần approval hoặc input tùy biến.

## 20. Use case thực tế: Spring Boot enterprise

Giả sử project:

```text
Spring Boot
Maven
Docker
SonarQube
Harbor
Kubernetes
Staging và production
```

Pipeline mong muốn:

```text
Merge request:
  -> compile
  -> unit test
  -> SonarQube scan

Main branch:
  -> compile
  -> test
  -> package jar
  -> build Docker image
  -> push Harbor
  -> deploy staging
  -> smoke test

Git tag:
  -> manual approval
  -> deploy production
```

## 21. GitLab CI/CD full example

```yaml
stages:
  - validate
  - package
  - image
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"
  IMAGE_TAG: "$HARBOR_REGISTRY/my-team/my-service:$CI_COMMIT_SHA"

cache:
  key: "$CI_PROJECT_NAME-maven"
  paths:
    - .m2/repository

validate:
  stage: validate
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn --batch-mode clean verify sonar:sonar
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == "main"'

package:
  stage: package
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn --batch-mode package -DskipTests
  artifacts:
    paths:
      - target/*.jar
    expire_in: 7 days
  rules:
    - if: '$CI_COMMIT_BRANCH == "main"'

build-image:
  stage: image
  image: docker:27
  services:
    - docker:27-dind
  script:
    - echo "$HARBOR_PASSWORD" | docker login "$HARBOR_REGISTRY" -u "$HARBOR_USERNAME" --password-stdin
    - docker build -t "$IMAGE_TAG" .
    - docker push "$IMAGE_TAG"
  rules:
    - if: '$CI_COMMIT_BRANCH == "main"'

deploy-staging:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl set image deployment/my-service my-service="$IMAGE_TAG" -n staging
    - kubectl rollout status deployment/my-service -n staging
    - curl --fail https://staging.example.com/actuator/health
  environment:
    name: staging
  rules:
    - if: '$CI_COMMIT_BRANCH == "main"'

deploy-production:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl set image deployment/my-service my-service="$IMAGE_TAG" -n production
    - kubectl rollout status deployment/my-service -n production
    - curl --fail https://example.com/actuator/health
  environment:
    name: production
  when: manual
  rules:
    - if: '$CI_COMMIT_TAG'
```

Điểm chính:

- `validate` chạy cho merge request và `main`.
- `package`, `build-image`, `deploy-staging` chạy trên `main`.
- `deploy-production` chạy khi có tag và cần bấm manual.
- Secret không để trong YAML.
- Image tag dùng `$CI_COMMIT_SHA` để truy vết.

## 22. Jenkins full example

```groovy
pipeline {
  agent { label 'linux-docker-maven' }

  environment {
    HARBOR_REGISTRY = 'harbor.example.com'
    IMAGE_NAME = 'my-team/my-service'
    IMAGE_TAG = "${HARBOR_REGISTRY}/${IMAGE_NAME}:${GIT_COMMIT}"
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Validate') {
      steps {
        withSonarQubeEnv('sonarqube') {
          sh './mvnw --batch-mode clean verify sonar:sonar'
        }
      }
    }

    stage('Package') {
      when {
        branch 'main'
      }
      steps {
        sh './mvnw --batch-mode package -DskipTests'
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
      }
    }

    stage('Build Image') {
      when {
        branch 'main'
      }
      steps {
        withCredentials([usernamePassword(
          credentialsId: 'harbor-credential',
          usernameVariable: 'HARBOR_USERNAME',
          passwordVariable: 'HARBOR_PASSWORD'
        )]) {
          sh '''
            echo "$HARBOR_PASSWORD" | docker login "$HARBOR_REGISTRY" -u "$HARBOR_USERNAME" --password-stdin
            docker build -t "$IMAGE_TAG" .
            docker push "$IMAGE_TAG"
          '''
        }
      }
    }

    stage('Deploy Staging') {
      when {
        branch 'main'
      }
      steps {
        withKubeConfig([credentialsId: 'staging-kubeconfig']) {
          sh '''
            kubectl set image deployment/my-service my-service="$IMAGE_TAG" -n staging
            kubectl rollout status deployment/my-service -n staging
            curl --fail https://staging.example.com/actuator/health
          '''
        }
      }
    }

    stage('Approval Production') {
      when {
        buildingTag()
      }
      steps {
        input message: 'Deploy to production?', ok: 'Deploy'
      }
    }

    stage('Deploy Production') {
      when {
        buildingTag()
      }
      steps {
        withKubeConfig([credentialsId: 'production-kubeconfig']) {
          sh '''
            kubectl set image deployment/my-service my-service="$IMAGE_TAG" -n production
            kubectl rollout status deployment/my-service -n production
            curl --fail https://example.com/actuator/health
          '''
        }
      }
    }
  }

  post {
    success {
      echo 'Pipeline completed successfully'
    }
    failure {
      echo 'Pipeline failed'
    }
  }
}
```

Điểm chính:

- Jenkins dùng `withCredentials` để lấy credential.
- Jenkins dùng `withKubeConfig` nếu có Kubernetes plugin.
- `when { branch 'main' }` giới hạn stage theo branch.
- `input` tạo approval thủ công.
- Jenkinsfile mạnh nhưng dài hơn và cần hiểu Groovy DSL.

## 23. Khi nào nên chọn GitLab CI/CD?

Nên chọn GitLab CI/CD khi:

- Source code đang nằm trên GitLab.
- Team muốn một nền tảng thống nhất.
- Pipeline tương đối chuẩn: build, test, scan, package, deploy.
- Công ty muốn merge request và pipeline cùng một chỗ.
- Có nhiều microservice cần dùng chung CI template.
- Muốn dùng GitLab Runner với Docker/Kubernetes executor.
- Muốn giảm phụ thuộc vào Jenkins plugin.

Ví dụ phù hợp:

```text
Công ty có 100 service Java.
Tất cả code nằm trên GitLab.
Mỗi service đều build Maven, scan SonarQube, build image, push Harbor.
Team DevOps tạo template .gitlab-ci.yml dùng chung.
```

## 24. Khi nào nên chọn Jenkins?

Nên chọn Jenkins khi:

- Công ty đã có Jenkins ổn định.
- Có nhiều hệ thống legacy.
- Cần plugin hoặc tích hợp đặc thù.
- Source code nằm ở nhiều nơi khác nhau.
- Pipeline có logic phức tạp.
- Cần agent đặc biệt.
- Cần tích hợp tool nội bộ cũ.
- Team có người vận hành Jenkins tốt.

Ví dụ phù hợp:

```text
Công ty có GitLab, SVN cũ, Nexus, SonarQube, server legacy, script deploy nội bộ.
Một số app build Java 8, một số Java 21, một số .NET, một số chạy trên Windows.
Deploy phải gọi hệ thống change request nội bộ.
```

## 25. Khi nào dùng cả GitLab CI/CD và Jenkins?

Trong công ty lớn, dùng cả hai là bình thường.

Ví dụ:

```text
GitLab CI/CD:
  - kiểm tra merge request
  - lint
  - unit test
  - build image đơn giản

Jenkins:
  - release production
  - deploy legacy system
  - gọi tool nội bộ
  - orchestrate pipeline phức tạp
```

Hoặc:

```text
GitLab quản lý source code và merge request.
Jenkins nhận webhook từ GitLab để chạy pipeline.
```

Flow:

```text
Developer push code lên GitLab
        |
        v
GitLab gửi webhook tới Jenkins
        |
        v
Jenkins chạy Jenkinsfile
        |
        v
Jenkins báo trạng thái về GitLab Merge Request
```

Kiểu này hay gặp ở công ty đã dùng Jenkins trước, sau đó chuyển source code sang GitLab nhưng chưa migrate toàn bộ CI/CD.

## 26. Migration từ Jenkins sang GitLab CI/CD

Nên migrate khi:

- Jenkins nhiều job lặp lại.
- Pipeline chủ yếu build/test/package chuẩn.
- Source code đã nằm trên GitLab.
- Team muốn pipeline gần merge request hơn.
- Jenkins plugin quá nhiều, khó bảo trì.

Không nên migrate vội khi:

- Jenkins đang xử lý nhiều tích hợp legacy.
- Pipeline có logic Groovy phức tạp.
- Có nhiều agent đặc thù.
- Chưa có GitLab Runner đủ mạnh.
- Chưa có template CI chuẩn.

Cách migrate an toàn:

```text
Bước 1: Chọn một service đơn giản.
Bước 2: Viết .gitlab-ci.yml tương đương Jenkinsfile.
Bước 3: Chạy song song Jenkins và GitLab CI một thời gian.
Bước 4: So sánh artifact, thời gian, kết quả test.
Bước 5: Chuyển merge request check sang GitLab CI.
Bước 6: Tắt job Jenkins cũ cho service đó.
Bước 7: Lặp lại với service khác.
```

Không nên migrate toàn bộ một lần.

## 27. Best practice cho GitLab CI/CD

- Dùng Docker executor hoặc Kubernetes executor cho job cần môi trường sạch.
- Tách runner trusted và untrusted.
- Dùng protected variables cho secret production.
- Dùng `rules` rõ ràng.
- Dùng `include` và template cho nhiều repo.
- Dùng cache có key hợp lý.
- Lưu artifact cần thiết, không lưu quá nhiều.
- Không hard-code secret trong `.gitlab-ci.yml`.
- Dùng image version cụ thể.
- Giữ pipeline ngắn và dễ đọc.

## 28. Best practice cho Jenkins

- Dùng Jenkinsfile thay vì cấu hình job thủ công trên UI.
- Dùng shared library cho logic lặp lại.
- Giới hạn plugin, chỉ cài plugin thật sự cần.
- Cập nhật Jenkins và plugin định kỳ.
- Tách controller và agent rõ ràng.
- Không chạy build nặng trên controller.
- Dùng credential store, không hard-code secret.
- Tách agent trusted và untrusted.
- Dọn workspace sau build nếu cần.
- Backup Jenkins home.
- Dùng Configuration as Code nếu hệ thống lớn.

## 29. Lỗi thường gặp với GitLab CI/CD

### 29.1. Dùng shell runner cho mọi thứ

Shell runner tiện nhưng dễ nhiễm trạng thái. Docker executor thường sạch hơn cho CI chuẩn.

### 29.2. Đặt secret không protected

Nếu secret production không được protected, branch không an toàn có thể dùng nhầm secret.

### 29.3. YAML quá nhiều logic

Nếu `.gitlab-ci.yml` quá dài, hãy tách template:

```yaml
include:
  - local: .gitlab/ci/java.yml
```

### 29.4. Cache sai key

Cache quá rộng có thể dùng dependency cũ. Cache quá hẹp thì không tăng tốc được.

## 30. Lỗi thường gặp với Jenkins

### 30.1. Cài quá nhiều plugin

Plugin càng nhiều, rủi ro upgrade và security càng cao.

### 30.2. Job không version control

Job cấu hình bằng UI khó audit. Nên đưa logic vào Jenkinsfile.

### 30.3. Chạy job trên controller

Controller nên điều phối, không nên chạy build nặng.

### 30.4. Credential dùng quá rộng

Một credential production không nên dùng cho mọi job.

### 30.5. Không dọn workspace

Workspace cũ có thể làm build không tái lập.

## 31. Câu hỏi phỏng vấn hay gặp

### 31.1. GitLab Runner khác Jenkins Agent thế nào?

```text
GitLab Runner nhận job từ GitLab CI/CD.
Jenkins Agent nhận job từ Jenkins Controller.
Cả hai đều là nơi thực sự chạy lệnh build/test/deploy.
```

GitLab Runner gắn với GitLab ecosystem. Jenkins Agent thường linh hoạt hơn trong enterprise legacy.

### 31.2. Khi nào dùng Jenkins thay vì GitLab CI/CD?

Khi cần:

- Tích hợp legacy.
- Plugin đặc thù.
- Nhiều source code platform.
- Agent đặc biệt.
- Pipeline logic rất phức tạp.
- Công ty đã có Jenkins ổn định.

### 31.3. Khi nào dùng GitLab CI/CD thay vì Jenkins?

Khi:

- Code nằm trên GitLab.
- Pipeline chuẩn.
- Muốn merge request và pipeline cùng một chỗ.
- Muốn giảm vận hành Jenkins.
- Muốn dùng template CI thống nhất cho nhiều repo.

### 31.4. Công ty lớn thường dùng cái nào?

Thường là cả hai tùy giai đoạn:

```text
Hệ thống mới, code trên GitLab
  -> GitLab CI/CD

Hệ thống cũ, tích hợp phức tạp
  -> Jenkins

Công ty đang chuyển đổi DevOps
  -> GitLab quản lý code, Jenkins vẫn chạy CI/CD
```

## 32. Kết luận thực tế

Nếu bạn mới học, nên học theo thứ tự:

```text
1. GitLab CI/CD để hiểu pipeline hiện đại, YAML, runner, stage/job.
2. Jenkins để hiểu enterprise CI/CD, agent, plugin, Jenkinsfile, legacy integration.
```

Điểm cần nhớ:

```text
GitLab CI/CD mạnh ở sự liền mạch và chuẩn hóa.
Jenkins mạnh ở sự linh hoạt và khả năng tích hợp hệ thống phức tạp.
```

Trong công ty lớn, lựa chọn không chỉ dựa vào công nghệ. Nó còn phụ thuộc:

- Hệ thống đang có từ trước.
- Chính sách bảo mật.
- Năng lực vận hành.
- Khách hàng.
- Compliance.
- Chi phí migration.
- Đội ngũ đã quen công cụ nào.

CI/CD tool chỉ là bộ điều phối. Giá trị thật nằm ở pipeline rõ ràng, runner/agent an toàn, artifact truy vết được, secret được bảo vệ và feedback nhanh cho developer.
