# Elastic fundamentals

> Tài liệu này diễn giải và mở rộng **đúng theo các chủ đề trong trang Elastic fundamentals**. Những nội dung chuyên sâu như query, mapping, analyzer, shard, tối ưu hiệu năng hay tích hợp Spring Boot sẽ được dành cho các bài học sau.

## Mục tiêu của bài

Sau bài này, chúng ta cần trả lời được:

- Elastic là gì và giải quyết nhóm bài toán nào?
- Ba solution chính của Elastic khác nhau ra sao?
- Elastic Stack gồm những thành phần chính nào?
- Dữ liệu đi qua Elastic Stack theo luồng nào?
- Có thể triển khai Elastic bằng những hình thức nào?
- Khi đọc tài liệu Elastic, cần hiểu version và availability như thế nào?
- Có thể tiếp tục học bằng những nguồn chính thức nào?

---

## 1. What is Elastic? — Elastic là gì?

Theo tài liệu chính thức, Elastic cung cấp một nền tảng mã nguồn mở dành cho:

- **search** — tìm kiếm;
- **analytics** — phân tích dữ liệu;
- **AI** — các khả năng tìm kiếm và xử lý có hỗ trợ trí tuệ nhân tạo;
- các giải pháp dựng sẵn cho **observability** và **security**.

### Hiểu đơn giản

Elastic là một hệ sinh thái giúp tổ chức đưa dữ liệu vào, tìm kiếm dữ liệu, phân tích dữ liệu và quan sát kết quả qua giao diện trực quan.

Ví dụ một công ty có nhiều loại dữ liệu:

- danh sách sản phẩm;
- tài liệu nội bộ;
- log của ứng dụng;
- số liệu CPU, RAM;
- sự kiện đăng nhập;
- cảnh báo bảo mật.

Các dữ liệu này khác nhau về mục đích nhưng đều có chung một nhu cầu: **cần tìm và phân tích nhanh trong một lượng dữ liệu lớn**. Đây là nền tảng chung mà Elastic cung cấp.

### “Open source platform” nghĩa là gì?

Trong ngữ cảnh của phần giới thiệu, có thể hiểu Elastic được xây dựng trên một nền tảng mở và có khả năng mở rộng. Người dùng có thể triển khai các thành phần cốt lõi trên hạ tầng của mình hoặc sử dụng dịch vụ được Elastic quản lý.

“Open source” không đồng nghĩa với:

- mọi tính năng đều miễn phí;
- dùng Elastic Cloud không mất phí;
- không cần quan tâm đến license.

Nó mô tả nền tảng và cách hệ sinh thái được phát triển, không phải cam kết rằng mọi cách sử dụng đều không có chi phí.

### Search AI Platform là gì?

Elastic mô tả nền tảng của mình là **Search AI Platform**, kết hợp:

- sức mạnh của search;
- khả năng của generative AI;
- tìm kiếm và phân tích gần thời gian thực;
- khả năng xếp kết quả theo độ liên quan.

Hiểu đơn giản, search truyền thống giúp tìm thông tin đã có; generative AI có thể dùng thông tin tìm được để hỗ trợ tạo câu trả lời hoặc trải nghiệm thông minh hơn.

Ví dụ, trong kho tài liệu nội bộ:

1. Người dùng đặt câu hỏi.
2. Hệ thống search tìm các tài liệu liên quan.
3. Mô hình AI dựa trên những tài liệu đó để hỗ trợ tạo câu trả lời.

Phần fundamentals chỉ cần hiểu mối liên hệ tổng quát này. Vector search, semantic search, hybrid search hay RAG là các chủ đề riêng, chưa cần đi sâu ở đây.

### Near real-time là gì?

**Near real-time** nghĩa là “gần thời gian thực”. Dữ liệu mới được đưa vào có thể được tìm kiếm và phân tích sau một khoảng trễ rất ngắn, nhưng không nên hiểu là mọi thay đổi đều xuất hiện tức thời tuyệt đối.

Ví dụ:

- một log lỗi vừa được ứng dụng sinh ra;
- sau một khoảng thời gian rất ngắn, log xuất hiện trong kết quả tìm kiếm hoặc dashboard;
- đội vận hành có thể nhanh chóng điều tra sự cố.

Ở bài này, chỉ cần phân biệt:

- **real-time tuyệt đối**: sự thay đổi phải nhìn thấy ngay tại cùng thời điểm;
- **near real-time**: có một độ trễ nhỏ, nhưng vẫn đủ nhanh cho phần lớn nhu cầu search và phân tích.

### Relevance là gì?

**Relevance** là độ liên quan của kết quả đối với nội dung người dùng đang tìm.

Khi tìm `điện thoại chống nước`, hệ thống không chỉ cần trả về tất cả tài liệu có chứa từ nào đó. Nó còn cần đưa những sản phẩm phù hợp nhất lên trước.

Đây là điểm khác biệt quan trọng giữa:

- tìm một bản ghi chính xác theo mã hoặc ID;
- tìm kiếm nội dung theo ý định của người dùng.

---

## 2. Explore the fundamentals — Các nội dung nền tảng cần khám phá

Trang Elastic fundamentals định hướng người mới qua bốn nhóm kiến thức:

1. **Solutions overview** — Elastic có những nhóm giải pháp nào?
2. **The Elastic Stack** — Các thành phần phối hợp với nhau ra sao?
3. **Deployment options** — Có thể chạy Elastic ở đâu và ai chịu trách nhiệm vận hành?
4. **Versioning and availability** — Cách đọc version và biết tính năng có dùng được hay không.

Bốn nhóm này trả lời bốn câu hỏi khác nhau:

| Nhóm kiến thức | Câu hỏi chính |
|---|---|
| Solutions overview | Elastic giúp giải quyết việc gì? |
| Elastic Stack | Những công cụ nào tham gia? |
| Deployment options | Hệ thống sẽ được chạy và quản lý thế nào? |
| Versioning and availability | Tính năng nào phù hợp với môi trường đang dùng? |

Người mới thường đi thẳng vào câu lệnh Elasticsearch nhưng chưa phân biệt được product, solution và deployment. Học bốn phần này trước giúp đặt mỗi khái niệm vào đúng vị trí.

---

## 3. Solutions overview — Tổng quan các giải pháp

Elastic giới thiệu ba giải pháp lớn dựa trên khả năng search:

1. Elasticsearch;
2. Elastic Observability;
3. Elastic Security.

### 3.1. Elasticsearch

Giải pháp Elasticsearch phù hợp khi cần xây dựng khả năng tìm kiếm, phân tích và trực quan hóa một lượng dữ liệu lớn.

Ví dụ:

- tìm sản phẩm trên website thương mại điện tử;
- tìm tài liệu trong kho kiến thức;
- tìm bài viết theo tiêu đề và nội dung;
- phân tích số lượng sự kiện theo khoảng thời gian;
- xây dựng chức năng search cho ứng dụng.

Nhóm người dùng thường gặp:

- developer;
- software architect;
- data engineer.

### 3.2. Elastic Observability

**Observability** có thể hiểu là khả năng nhìn vào bên trong hệ thống phần mềm để biết:

- hệ thống có đang hoạt động ổn định không;
- bộ phận nào đang chậm;
- lỗi phát sinh ở đâu;
- sự cố bắt đầu từ thời điểm nào;
- các thành phần liên quan với nhau ra sao.

Ví dụ, người dùng báo API thanh toán chậm. Đội vận hành cần quan sát log, metric và thông tin hiệu năng để tìm nguyên nhân. Elastic Observability cung cấp giải pháp phục vụ quá trình đó.

Nhóm người dùng thường gặp:

- DevOps;
- SRE;
- IT Operations;
- developer phụ trách vận hành ứng dụng.

### 3.3. Elastic Security

Elastic Security tập trung vào việc theo dõi dữ liệu để:

- phát hiện hành vi bất thường;
- ngăn chặn hoặc giảm thiểu mối đe dọa;
- điều tra sự cố;
- hỗ trợ phản ứng trước sự cố bảo mật.

Ví dụ, hệ thống ghi nhận một tài khoản đăng nhập bất thường và thực hiện hàng loạt hành động nhạy cảm. Elastic Security có thể hỗ trợ tập hợp sự kiện, phát hiện tín hiệu đáng ngờ và giúp đội bảo mật điều tra.

Nhóm người dùng thường gặp:

- SOC team;
- security analyst;
- IT security administrator.

### 3.4. Ba solution có phải ba hệ thống tách biệt không?

Không nên hiểu chúng là ba nền tảng hoàn toàn không liên quan. Chúng được xây dựng trên nền tảng chung và đều tận dụng khả năng search của Elasticsearch.

Điểm khác nhau chủ yếu là mục tiêu sử dụng:

| Nhu cầu | Solution phù hợp |
|---|---|
| Xây dựng chức năng tìm kiếm cho ứng dụng | Elasticsearch |
| Theo dõi sức khỏe và hiệu năng hệ thống | Elastic Observability |
| Phát hiện và xử lý nguy cơ bảo mật | Elastic Security |

Một tổ chức có thể dùng đồng thời nhiều solution. Ví dụ, website bán hàng dùng Elasticsearch cho tìm kiếm sản phẩm và Elastic Observability để giám sát hệ thống.

---

## 4. The Elastic Stack — Bộ công cụ Elastic

Elastic Stack là tập hợp các sản phẩm phối hợp để:

- thu thập dữ liệu;
- đưa dữ liệu vào hệ thống;
- lưu trữ;
- tìm kiếm và phân tích;
- trực quan hóa.

Luồng tổng quát:

```text
Nguồn dữ liệu
     |
     v
Công cụ ingest/thu thập
     |
     v
Elasticsearch
     |
     v
Kibana
```

### 4.1. Elasticsearch — lưu trữ, tìm kiếm và phân tích

Elasticsearch nằm ở trung tâm của Elastic Stack. Nó đảm nhiệm việc lưu dữ liệu, lập chỉ mục, tìm kiếm và phân tích.

Có thể hình dung Elasticsearch là “bộ máy” xử lý dữ liệu phía sau. Ứng dụng hoặc người dùng gửi yêu cầu đến nó để ghi dữ liệu hoặc tìm kiếm dữ liệu.

Elasticsearch có thể làm việc với nhiều loại dữ liệu, chẳng hạn:

- text có cấu trúc hoặc không có cấu trúc;
- dữ liệu gắn thời gian;
- dữ liệu vị trí địa lý;
- vector;
- các document dạng JSON.

Tài liệu fundamentals cũng nhấn mạnh Elasticsearch là hệ thống phân tán và có thể chạy trên một hoặc nhiều server. Các khái niệm node, index, shard và replica được nhắc đến để giải thích khả năng mở rộng và chịu lỗi; chi tiết của từng khái niệm sẽ thuộc bài kiến trúc Elasticsearch riêng.

### 4.2. Elasticsearch clients

Client giúp ứng dụng giao tiếp với Elasticsearch bằng ngôn ngữ lập trình quen thuộc như Java, Python hoặc Go.

Thay vì tự xử lý mọi chi tiết HTTP, ứng dụng có thể dùng client để:

- gửi request;
- nhận response;
- làm việc với các kiểu dữ liệu phù hợp với ngôn ngữ;
- xử lý xác thực và kết nối thuận tiện hơn.

Trong repository Spring Boot này, Java client sẽ là chủ đề thực hành phù hợp ở giai đoạn sau. Phần fundamentals chỉ cần biết client là cầu nối giữa application và Elasticsearch.

### 4.3. Kibana — khám phá và trực quan hóa

Kibana là giao diện người dùng của Elastic. Nếu Elasticsearch là bộ máy xử lý phía sau, Kibana là nơi con người quan sát và làm việc với dữ liệu.

Kibana hỗ trợ:

- tìm và lọc dữ liệu thô;
- tạo biểu đồ;
- tạo dashboard;
- phân tích dữ liệu địa lý;
- cấu hình alert;
- quản lý nhiều tài nguyên trong Elastic Stack;
- gửi request đến Elasticsearch thông qua Console.

Elasticsearch có thể tồn tại mà không có Kibana, nhưng Kibana được dùng trong phần lớn use case vì giúp việc khám phá, trực quan hóa và quản trị thuận tiện hơn.

### 4.4. Ingest — đưa dữ liệu vào Elastic

**Ingest** là quá trình đưa dữ liệu từ nguồn vào Elasticsearch.

Nguồn dữ liệu có thể là:

- ứng dụng;
- máy chủ;
- file log;
- database;
- cloud service;
- thiết bị hoặc hệ thống mạng.

Elastic cung cấp nhiều cách ingest vì dữ liệu và nhu cầu xử lý không giống nhau. Có dữ liệu chỉ cần chuyển tiếp; có dữ liệu cần làm sạch, đổi cấu trúc hoặc bổ sung thông tin trước khi lưu.

### 4.5. Elastic Agent và Integrations

Elastic Agent là một cách thống nhất để thu thập logs, metrics và những loại dữ liệu khác từ host rồi gửi tới Elastic.

**Integration** là gói tích hợp cho một nguồn dữ liệu hoặc sản phẩm cụ thể. Nó giúp giảm lượng cấu hình thủ công khi kết nối Elastic với các hệ thống phổ biến.

Ví dụ, thay vì tự xác định từng file log và từng trường dữ liệu của một dịch vụ, integration có thể cung cấp sẵn phần cấu hình, cách xử lý dữ liệu và dashboard liên quan.

Elastic Agent có thể được quản lý tập trung thông qua Fleet. Điều này hữu ích khi tổ chức có nhiều máy cần áp dụng cùng chính sách thu thập dữ liệu.

### 4.6. APM

APM là viết tắt của **Application Performance Monitoring** — giám sát hiệu năng ứng dụng.

APM thu thập thông tin như:

- thời gian xử lý request;
- database query;
- lời gọi tới cache;
- external HTTP request;
- lỗi phát sinh trong ứng dụng.

Mục tiêu là giúp đội phát triển xác định vị trí gây chậm hoặc lỗi thay vì chỉ biết chung chung rằng “hệ thống có vấn đề”.

### 4.7. OpenTelemetry Collector

OpenTelemetry là một framework trung lập nhà cung cấp dùng để thu thập, xử lý và xuất dữ liệu telemetry.

**Telemetry** trong ngữ cảnh này gồm những tín hiệu giúp quan sát hệ thống, như logs, metrics và traces.

Ý nghĩa của “trung lập nhà cung cấp” là ứng dụng có thể tạo dữ liệu theo một chuẩn mở thay vì thiết kế hoàn toàn phụ thuộc vào một công cụ quan sát duy nhất. Elastic hỗ trợ tiếp nhận loại dữ liệu này và cũng cung cấp bản phân phối OpenTelemetry phù hợp với Elastic Observability.

### 4.8. Beats

Beats là các chương trình gửi dữ liệu nhẹ được cài trên server để chuyển dữ liệu vận hành tới Elasticsearch.

Các Beats khác nhau được thiết kế cho những loại dữ liệu khác nhau. Tuy nhiên, theo tài liệu Elastic hiện tại, **Elastic Agent đã thay Beats trong phần lớn use case**. Một Elastic Agent có thể thu thập nhiều loại dữ liệu thay vì phải cài nhiều Beat riêng biệt.

Beats vẫn cần được nhận biết vì:

- nhiều hệ thống hiện hữu vẫn đang sử dụng;
- nhiều bài viết và khóa học cũ dùng kiến trúc ELK/Beats;
- nó giúp hiểu quá trình phát triển của Elastic Stack.

### 4.9. Elasticsearch ingest pipelines

Ingest pipeline cho phép thực hiện một chuỗi bước xử lý document trước khi document được lưu vào Elasticsearch.

Ví dụ ở mức khái niệm:

```text
Nhận log thô
  -> tách các trường cần thiết
  -> đổi tên trường
  -> bổ sung thông tin
  -> lưu vào Elasticsearch
```

Mỗi bước xử lý thường được gọi là một **processor**. Cách cấu hình processor cụ thể sẽ là một bài riêng.

### 4.10. Logstash

Logstash là công cụ thu thập và xử lý dữ liệu theo pipeline. Mô hình quen thuộc là:

```text
input -> filter -> output
```

- `input`: dữ liệu đến từ đâu;
- `filter`: dữ liệu được biến đổi thế nào;
- `output`: dữ liệu được gửi đi đâu.

Logstash phù hợp với các pipeline cần kết nối nhiều nguồn hoặc thực hiện biến đổi dữ liệu phức tạp.

Không phải hệ thống Elastic nào cũng bắt buộc dùng Logstash. Tùy use case, dữ liệu có thể đi qua Elastic Agent, OpenTelemetry, ingest pipeline hoặc được ứng dụng gửi trực tiếp.

### 4.11. ELK và Elastic Stack

ELK là tên viết tắt quen thuộc của:

- Elasticsearch;
- Logstash;
- Kibana.

Elastic Stack hiện rộng hơn ELK vì còn có Elastic Agent, APM, OpenTelemetry, Integrations và những thành phần khác. Vì vậy, khi học tài liệu mới, nên dùng “Elastic Stack” để chỉ toàn bộ hệ sinh thái và hiểu “ELK” là tên gọi lịch sử hoặc kiến trúc gồm ba thành phần cụ thể.

### 4.12. Lưu ý khi cài đặt các thành phần

Tài liệu chính thức yêu cầu các sản phẩm thuộc Elastic Stack dùng cùng version tương ứng. Thứ tự cài đặt được đề xuất cho môi trường self-managed là:

1. Elasticsearch;
2. Kibana;
3. Logstash;
4. Elastic Agent hoặc Beats;
5. APM;
6. Elasticsearch Hadoop nếu cần.

Lý do là thành phần được cài sau có thể phụ thuộc vào thành phần đứng trước. Đây là hướng dẫn tổng quát; một deployment thực tế không nhất thiết phải cài tất cả các sản phẩm trong danh sách.

---

## 5. Deployment options — Các phương án triển khai

**Deployment** là một môi trường Elastic đang chạy. Lựa chọn deployment ảnh hưởng đến:

- những tính năng có sẵn;
- mức độ kiểm soát hạ tầng;
- lượng công việc vận hành thủ công;
- cách scale, nâng cấp và monitoring;
- chi phí và trách nhiệm của đội ngũ.

### 5.1. Elastic Cloud Hosted

Elastic Cloud Hosted là dịch vụ được Elastic quản lý, chạy trên cloud provider được hỗ trợ như AWS, Google Cloud hoặc Azure.

Người dùng vẫn có mức kiểm soát đáng kể đối với cấu hình deployment, chẳng hạn topology, phần cứng và version trong phạm vi dịch vụ cung cấp.

Phù hợp khi:

- muốn Elastic quản lý phần lớn công việc hạ tầng;
- vẫn cần khả năng điều chỉnh cluster;
- không muốn tự xây toàn bộ hệ thống vận hành Elastic.

### 5.2. Elastic Cloud Serverless

Serverless che giấu phần lớn hạ tầng bên dưới và tự động scale theo workload. Người dùng tập trung vào dữ liệu và solution thay vì quản lý node hoặc cluster.

Phù hợp khi:

- ưu tiên sự đơn giản trong vận hành;
- muốn bắt đầu nhanh;
- không cần trực tiếp kiểm soát chi tiết cluster;
- chấp nhận mô hình tính phí dựa trên mức sử dụng.

“Serverless” không có nghĩa là không tồn tại server. Nó nghĩa là người sử dụng dịch vụ không phải trực tiếp quản lý các server đó.

### 5.3. Local development

Elastic hỗ trợ khởi động Elasticsearch và Kibana bằng Docker cho mục đích phát triển và kiểm thử local.

Phù hợp khi:

- đang học;
- muốn thử API và tính năng;
- phát triển ứng dụng trên máy cá nhân;
- cần môi trường có thể tạo lại nhanh.

Local development không đại diện đầy đủ cho kiến trúc production về độ sẵn sàng, bảo mật, tài nguyên và khả năng chịu lỗi.

### 5.4. Self-managed

Self-managed nghĩa là tổ chức tự cài đặt, vận hành và bảo trì Elastic Stack trên hạ tầng của mình, có thể là on-premises hoặc private cloud.

Ưu điểm:

- kiểm soát môi trường ở mức cao nhất;
- phù hợp với yêu cầu hạ tầng hoặc chính sách đặc thù.

Đổi lại, tổ chức chịu trách nhiệm nhiều hơn về:

- cài đặt;
- scale;
- nâng cấp;
- giám sát;
- bảo mật;
- xử lý sự cố.

### 5.5. Elastic Cloud Enterprise — ECE

ECE là một sản phẩm self-managed giúp tổ chức cung cấp, quản lý và theo dõi nhiều deployment Elastic ở quy mô lớn từ một nơi tập trung.

Có thể hiểu nó phù hợp với tổ chức muốn có trải nghiệm quản lý kiểu cloud nhưng phải chạy trên hạ tầng do mình kiểm soát.

### 5.6. Elastic Cloud on Kubernetes — ECK

ECK là operator chính thức để triển khai và quản lý Elastic Stack trên Kubernetes.

Phù hợp khi tổ chức:

- đã sử dụng Kubernetes;
- muốn Elastic được quản lý theo mô hình tài nguyên Kubernetes;
- cần tự động hóa việc triển khai và vận hành trong nền tảng Kubernetes hiện có.

### 5.7. So sánh nhanh

| Lựa chọn | Ai quản lý hạ tầng chính? | Mức kiểm soát | Phù hợp để học nhanh? |
|---|---|---|---|
| Local Docker | Người học | Cao trong phạm vi local | Rất phù hợp |
| Cloud Serverless | Elastic | Thấp hơn, tập trung vào solution | Phù hợp |
| Cloud Hosted | Elastic quản lý phần lớn | Khá cao | Phù hợp |
| Self-managed | Tổ chức/người dùng | Cao nhất | Học được nhiều nhưng phức tạp hơn |
| ECE | Tổ chức/người dùng | Cao | Thường dành cho quy mô tổ chức |
| ECK | Tổ chức trên Kubernetes | Cao | Phù hợp nếu đã biết Kubernetes |

---

## 6. Versioning and availability — Phiên bản và phạm vi khả dụng

Phần này giúp trả lời hai câu hỏi:

1. Tài liệu đang nói về version nào?
2. Tính năng đó có dùng được trên deployment của mình không?

### 6.1. Các thành phần dùng chung và độc lập version

Nhiều thành phần Elastic Stack như Elasticsearch và Kibana chia sẻ cùng cách đánh version và cần tương thích với nhau.

Tuy nhiên, một số sản phẩm được đánh version độc lập, ví dụ:

- Elastic Cloud Enterprise;
- Elastic Cloud on Kubernetes;
- language client và SDK.

Cloud console và Serverless project được Elastic tự động cập nhật, nên cách suy nghĩ về version của chúng khác với self-managed stack.

### 6.2. Tài liệu từ Elastic Stack 9

Từ Elastic Stack 9.0.0, Elastic không còn xuất bản một bộ tài liệu riêng biệt cho từng minor release. Thay vào đó, tài liệu của dòng 9.x được cập nhật liên tục.

Điều này giúp giảm nội dung trùng lặp, nhưng người đọc phải chú ý các nhãn availability để biết tính năng được thêm hoặc thay đổi ở version nào.

Tài liệu của các version cũ hơn vẫn được cung cấp trong khu vực previous versions.

### 6.3. Availability badge

Availability badge là nhãn cho biết phạm vi sử dụng của tính năng, có thể bao gồm:

- loại sản phẩm hoặc deployment được hỗ trợ;
- version bắt đầu có tính năng;
- trạng thái ổn định của tính năng;
- thời điểm tính năng bị deprecated.

Ví dụ, một tính năng có thể:

- GA trên Elastic Stack từ version 9.1;
- beta trên một loại Serverless project;
- không có trên deployment khác.

Vì vậy, nhìn thấy tính năng trong tài liệu không có nghĩa là môi trường nào cũng dùng được.

### 6.4. Các trạng thái thường gặp

| Trạng thái | Cách hiểu cho người mới |
|---|---|
| Preview/Experimental | Đang thử nghiệm, có thể thay đổi nhiều |
| Beta | Đã có thể trải nghiệm nhưng cần thận trọng |
| GA | Generally Available, sẵn sàng dùng chính thức |
| Deprecated | Không còn được khuyến nghị và có thể bị loại bỏ trong tương lai |
| Removed | Đã bị loại khỏi sản phẩm hoặc phạm vi được nêu |

### 6.5. Major, minor và patch

Một version thường được đọc theo dạng:

```text
major.minor.patch
9.4.4
```

- `9`: major version;
- `4`: minor version;
- `4`: patch version.

Availability badge thường thể hiện đến minor version, ví dụ `9.1`, không nhất thiết ghi từng patch. Tài liệu tương ứng với patch mới nhất của minor đó. Nếu đang chạy patch cũ, cần xem thêm release notes.

### 6.6. Checklist khi đọc một trang tài liệu

Trước khi áp dụng hướng dẫn, hãy kiểm tra:

- [ ] Mình đang dùng loại deployment nào?
- [ ] Version Elasticsearch/Kibana hiện tại là bao nhiêu?
- [ ] Tính năng có availability badge không?
- [ ] Tính năng đang ở preview, beta, GA hay deprecated?
- [ ] Hướng dẫn có áp dụng cho Serverless hay self-managed không?
- [ ] Có cần kiểm tra release notes của patch hiện tại không?

---

## 7. Training resources — Nguồn đào tạo

Elastic cung cấp các hình thức đào tạo theo vai trò và solution, dành cho cả người mới lẫn người đã có kinh nghiệm.

### Elastic Training

[Elastic Training](https://www.elastic.co/training) cung cấp khóa học trực tuyến theo lịch và khóa học on-demand. Nếu muốn học theo hướng kỹ sư Elasticsearch, tài liệu fundamentals gợi ý khóa **Elasticsearch Engineer**.

Khóa đào tạo chính thức phù hợp khi cần:

- lộ trình có cấu trúc;
- nội dung theo vai trò công việc;
- bài thực hành có hướng dẫn;
- accreditation sau khi hoàn thành một số chương trình.

### Demo Gallery

[Elastic Demo Gallery](https://www.elastic.co/demo-gallery) chứa các nội dung demo có thể lọc theo solution hoặc chủ đề.

Phù hợp khi muốn quan sát một use case cụ thể trước khi học chi tiết cách triển khai.

### Beginner's Crash Course

[Beginner's Crash Course to Elastic Stack](https://www.youtube.com/playlist?list=PL_mJOmq4zsHZYAyK606y7wjQtC0aoE6Es) là chuỗi video sáu phần dành cho người muốn học theo tốc độ riêng.

Video giúp quan sát thao tác thực tế, còn tài liệu chính thức phù hợp để tra cứu chính xác. Có thể kết hợp cả hai thay vì chỉ phụ thuộc vào một nguồn.

---

## 8. Other resources — Các nguồn khác

### Deploy and manage

[Deploy and manage](https://www.elastic.co/docs/deploy-manage) tập trung vào cách triển khai và quản lý môi trường Elastic.

Đây là nơi tìm hiểu sâu hơn sau khi đã chọn được deployment option, chẳng hạn Cloud, self-managed hoặc Kubernetes.

### Manage data

[Manage data](https://www.elastic.co/docs/manage-data) tập trung vào việc ingest và quản lý dữ liệu trong Elasticsearch.

Đây là bước tiếp theo khi đã hiểu luồng chung của Elastic Stack và muốn học cách dữ liệu thực sự được đưa vào, tổ chức và duy trì.

### How to use the documentation

[How to use the documentation](https://www.elastic.co/docs/get-started/howto-use-the-docs) giải thích cách tài liệu Elastic được tổ chức, cách nhận biết version và cách tìm đúng nội dung cho sản phẩm đang sử dụng.

Nguồn này đặc biệt quan trọng từ dòng 9.x vì tài liệu được cập nhật liên tục và sử dụng availability badge.

---

## 9. Tổng kết

Có thể ghi nhớ toàn bộ bài bằng bốn lớp:

```text
Elastic
  = nền tảng search, analytics và AI
  + solution cho Observability và Security

Elastic Stack
  = các sản phẩm đưa dữ liệu vào, lưu trữ, tìm kiếm và trực quan hóa

Deployment options
  = lựa chọn ai quản lý hạ tầng và quản lý ở mức nào

Versioning & availability
  = kiểm tra tính năng có phù hợp với version và deployment hay không
```

### Những điều cần nhớ nhất

- Elastic là hệ sinh thái; Elasticsearch là thành phần tìm kiếm và phân tích cốt lõi.
- Ba solution chính là Elasticsearch, Elastic Observability và Elastic Security.
- Elasticsearch xử lý dữ liệu; Kibana cung cấp giao diện; Agent, OpenTelemetry, ingest pipeline và Logstash hỗ trợ đưa dữ liệu vào.
- Elastic Agent đã thay Beats trong phần lớn use case mới.
- ELK chỉ là một phần/tên gọi lịch sử, còn Elastic Stack có phạm vi rộng hơn.
- Có thể dùng Cloud, local Docker, self-managed, ECE hoặc ECK tùy nhu cầu vận hành.
- Khi đọc docs phải kiểm tra cả version, deployment type và availability badge.

---

## Tài liệu tham khảo chính thức

- [Elastic fundamentals](https://www.elastic.co/docs/get-started)
- [Solutions overview](https://www.elastic.co/docs/get-started/introduction)
- [The Elastic Stack](https://www.elastic.co/docs/get-started/the-stack)
- [Deployment options](https://www.elastic.co/docs/get-started/deployment-options)
- [Versioning and availability](https://www.elastic.co/docs/get-started/versioning-availability)

> Nội dung trên là bản diễn giải tiếng Việt theo hướng dễ hiểu, không phải bản dịch nguyên văn. Phạm vi được giữ theo đúng các keyword và liên kết của bài Elastic fundamentals.
