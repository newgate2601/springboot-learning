# Cashloan Feature Lab — giải thích chi tiết từng bước chạy

File này gộp lại toàn bộ những gì cần biết để hiểu **thật sự** chuyện gì xảy ra khi bạn bắn từng cái curl xuống, chứ không chỉ liệt kê API. Đọc xong file này là hiểu luôn cả Camunda lẫn Spring Boot đang làm gì phía sau, không cần đoán mò nữa.

Base URL cho mọi request: `http://localhost:8086/api/v1/camunda-demo`

---

## 1. Trước tiên, phải hiểu đúng cái "hình dạng" của hệ thống này

Từ lúc đổi qua `camunda-bpm-spring-boot-starter`, Camunda **không còn là 1 server riêng** nữa. Nó chỉ là 1 thư viện (jar) nằm chung trong app Spring Boot của bạn, chạy chung 1 tiến trình, chung 1 port `8086`. Không có chuyện Spring Boot gọi REST sang Camunda Run ở port 8080 như bản cũ nữa — giờ gọi Camunda tức là gọi thẳng vào hàm Java (`RuntimeService`, `TaskService`, `ExternalTaskService`, `HistoryService`), không đi qua mạng, không có độ trễ HTTP nào cả.

Nhưng đừng hiểu lầm là state của process nằm trong RAM của app. **State thật sự nằm hết trong Postgres** (mấy bảng `ACT_RU_*` cho dữ liệu đang chạy, `ACT_HI_*` cho lịch sử). App chỉ là cái "cửa sổ" để đọc/ghi vào đó. Vì vậy dù bạn chạy 1 hay 10 pod app, tụi nó vẫn phối hợp đúng với nhau — vì tất cả cùng nhìn vào 1 chỗ dữ liệu duy nhất, không đứa nào giữ riêng cái gì cho mình cả.

Có đúng 1 thứ chạy nền tự động, gọi là **Job Executor** — một nhóm thread mà `camunda-bpm-spring-boot-starter` tự bật lên lúc app khởi động, chạy độc lập hoàn toàn với các thread xử lý HTTP request. Việc của nó là xử lý mấy "job kỹ thuật" nội bộ của engine (sẽ nói kỹ ở phần `asyncBefore`/`asyncAfter` bên dưới) — **không phải** nghiệp vụ. Nghiệp vụ (complete task, correlate message...) thì vẫn 100% phải do bạn gọi API tay, không ai làm giùm.

---

## 2. Sơ đồ luồng, tên activity id để đối chiếu

```
lab_start (start event "Application init")
   ↓
lab_segment — subprocess "Phân loại segment"
   ├─ fork song song ─┬─→ lab_send_segment (external task, topic=lab-segment, asyncBefore)
   │                  └─→ lab_receive_segment (chờ message LabSegmentResult)
   └─ join (bắt buộc đủ CẢ HAI token mới đi tiếp)
   ↓
lab_segment_ok — gateway "Segment thành công?"
   ├─ No  → lab_rejected_segment (end, status=REJECTED_SEGMENT)
   └─ Yes ↓
lab_precheck (external task, topic=lab-precheck, asyncAfter)
   ↓
lab_precheck_ok — gateway "Precheck đạt?"
   ├─ No  → lab_rejected_precheck (end, status=REJECTED_PRECHECK)
   └─ Yes ↓
lab_need_scoring — gateway "Cần chấm điểm?"
   ├─ segmentType=VTP_OFF_NET → lab_skip_scoring (end, status=SKIPPED_SCORING)
   └─ còn lại (default)      ↓
lab_manual_review (user task, assignee=demo)
   ↓
lab_submit_message (message throw event, set labMessageThrown=true)
   ↓
lab_ready_for_scoring (end, status=READY_FOR_SCORING)
```

## 3. Chuỗi mapping biến — cái này là "xương sống" của cả bài

```
segmentSuccess  → requestSegmentSuccess  → isSegmentEvaluatedSuccess → (gateway "Segment thành công?" đọc)
segmentType     → requestedSegmentType   → requestType → segmentType → (gateway "Cần chấm điểm?" đọc)
precheckPassed  → requestPrecheck        → isPrecheckPassed          → (gateway "Precheck đạt?" đọc)
```

Nhớ kỹ chuỗi này, vì mỗi lần mapping là biến đổi tên (do input/output mapping khai trong BPMN), không phải cùng 1 tên biến chạy xuyên suốt đâu.

---

## 4. Đi từng bước — mỗi lần gọi curl, Camunda với Spring Boot làm gì

### Bước 0 — Trước khi bạn gọi gì cả (lúc app khởi động)

Chuyện này xảy ra 1 lần duy nhất, tự động, không liên quan gì tới curl:
- Engine tự tạo/nâng schema `ACT_*` trong DB `learning` (`camunda.bpm.database.schema-update=true`).
- Tự deploy `cashloan-feature-lab.bpmn` (quét theo `camunda.bpm.deployment-resource-pattern`), có dedup filtering nên dù restart app nhiều lần cũng không tạo ra nhiều bản definition trùng nhau, miễn nội dung file không đổi.
- Tạo tài khoản `admin` cho Cockpit/Tasklist nếu chưa có (`camunda.bpm.admin-user`).

### Bước 1 — `POST /lab/start`

```bash
curl --location 'http://localhost:8086/api/v1/camunda-demo/lab/start' \
--header 'Content-Type: application/json' \
--data '{
  "segmentSuccess": true,
  "precheckPassed": true,
  "segmentType": "VTP_ON_NET"
}'
```

Thread HTTP xử lý request này gọi thẳng `runtimeService.startProcessInstanceByKey("CAKE_FEATURE_LAB", variables)` — và chính thread đó chạy luôn một mạch code engine, trong đúng 1 transaction DB, làm hết những việc sau:

1. Tra `ProcessDefinition` mới nhất key `CAKE_FEATURE_LAB`, tạo 1 dòng execution gốc (root execution) — đây chính là `processInstanceId` bạn nhận về.
2. Ghi 3 biến `segmentSuccess`, `precheckPassed`, `segmentType` vào scope process gốc. Ba biến này **không hề qua input mapping nào cả**, cứ ghi thẳng, nên panel "Process variables" trong Modeler không liệt kê chúng đâu (Modeler chỉ suy ra biến từ mapping khai trong file BPMN) — nhưng chúng vẫn tồn tại thật trong instance, đọc `GET /lab/{id}` là thấy.
3. Chạy execution listener `event="start"` của process → set `labLifecycle=started`.
4. Rời `lab_start`, vào subprocess `lab_segment` — chạy input mapping của subprocess: `requestedSegmentType = ${segmentType}` (đọc biến cha, ghi thành biến local của subprocess). Chạy listener start của subprocess → `labSegmentLifecycle=started`.
5. Vào `lab_segment_fork` (parallel gateway), tách làm 2 token con:
   - **Token nhánh `lab_send_segment`**: task này có `camunda:asyncBefore="true"`, nghĩa là engine **không vào task ngay** — chỉ ghi 1 job "hẹn giờ" rồi dừng ở đó. Lúc này external task `lab-segment` **chưa hề tồn tại** trong DB.
   - **Token nhánh `lab_receive_segment`**: task này không đánh dấu async gì cả, nên engine vào ngay trong cùng transaction — tạo 1 dòng "đăng ký chờ message" tên `LabSegmentResult` gắn với đúng `processInstanceId` này, rồi dừng chờ.
6. Transaction commit. Response trả về `{ processInstanceId, statusUrl }` ngay lập tức — **không** đợi job hẹn giờ ở bước 5 chạy xong.

Ngay sau đó (không do bạn gọi gì), Job Executor nền tự lấy cái job hẹn giờ đó ra chạy trong 1 transaction riêng: mới thật sự "vào" `lab_send_segment`, chạy input mapping của task này (`requestSegmentSuccess = ${segmentSuccess}`, `requestType = ${requestedSegmentType}`), rồi tạo dòng `ACT_RU_EXT_TASK` thật với `topicName=lab-segment`. **Chỉ từ lúc này** thì `waitingExternalTaskTopic` mới có giá trị `lab-segment`.

Đây là lý do: gọi `/lab/start` xong mà `GET /lab/{id}` ngay lập tức, có khi chưa thấy `waitingExternalTaskTopic` đâu — phải đợi Job Executor chạy xong (thường dưới 1 giây, nhưng không phải tức thì).

### Bước 2 — `GET /lab/{id}` (kiểm tra trạng thái)

```bash
curl --location 'http://localhost:8086/api/v1/camunda-demo/lab/{{processInstanceId}}'
```

API này **chỉ đọc, không làm process chạy tiếp gì cả**. Bên trong nó query 4 chỗ:
- `HistoryService.createHistoricActivityInstanceQuery()` → ra `visitedActivities` + `outcome`.
- `TaskService.createTaskQuery()` → ra `waitingTaskId`/`waitingTaskName` (User Task đang chờ).
- `ExternalTaskService.createExternalTaskQuery()` → ra `waitingExternalTaskId`/`waitingExternalTaskTopic`.
- `HistoryService.createHistoricVariableInstanceQuery()` → ra toàn bộ `variables`.

Gọi bước này trước bước 3 để chắc `waitingExternalTaskTopic` đã là `lab-segment` (né trường hợp Job Executor chưa kịp chạy job hẹn giờ ở trên).

### Bước 3 — `POST /lab/{id}/process-segment`

```bash
curl --location --request POST 'http://localhost:8086/api/v1/camunda-demo/lab/{{processInstanceId}}/process-segment'
```

Y như bước 1, chính thread HTTP của request này chạy thẳng vào code Camunda, KHÔNG có network, KHÔNG có worker riêng nào đứng ra làm giùm — request của bạn CHÍNH LÀ worker luôn. Trong 1 transaction:

1. `externalTaskService.createExternalTaskQuery().processInstanceId(id).topicName("lab-segment").list()` — tìm đúng external task. Nếu Job Executor ở bước 1 chưa kịp chạy → list rỗng → ném `IllegalStateException` → HTTP 500, process giữ nguyên trạng thái, không hư gì.
2. `externalTaskService.lock(taskId, workerId, 30000)` — khoá task 30 giây bằng `workerId` (UUID sinh 1 lần lúc app khởi động, sống suốt đời app). Nếu task đã bị ai lock trước và chưa hết hạn → ném `ExternalTaskAlreadyLockedException`.
3. `runtimeService.getVariablesLocal(executionId)` — đọc đúng 2 biến local của token này: `requestSegmentSuccess`, `requestType` (không phải biến `segmentSuccess`/`segmentType` gốc, mà là bản đã map).
4. Tính `isSegmentEvaluatedSuccess` = giá trị `requestSegmentSuccess` (code lab này chỉ echo lại nguyên giá trị bạn gửi lúc start, không tính toán gì thêm), `segmentType` = giá trị `requestType`.
5. `externalTaskService.complete(taskId, workerId, output)` — set 2 biến trên làm local variable của token, rồi chạy output mapping của task: `segmentWorkerResult = ${isSegmentEvaluatedSuccess}` — đẩy 1 bản sao lên scope cha (subprocess) để quan sát, tách biệt với biến gateway thật sự dùng.

Token rời `lab_send_segment`, tới `lab_segment_join` — đứng chờ, vì `lab_segment_join` là **parallel gateway kiểu AND-join**: bắt buộc phải có đủ token từ CẢ HAI nhánh (`lab_send_segment` và `lab_receive_segment`) mới cho đi tiếp. Đây không phải hành vi tuỳ chỉnh được — là bản chất của loại gateway này. Nên **API `correlate-segment` (bước 4) là bắt buộc, không thể bỏ qua**, dù nó không mang theo dữ liệu quyết định gì cả — nó chỉ là "tín hiệu cho phép đi tiếp".

### Bước 4 — `POST /lab/{id}/correlate-segment`

```bash
curl --location --request POST 'http://localhost:8086/api/v1/camunda-demo/lab/{{processInstanceId}}/correlate-segment'
```

Thread request này gọi `runtimeService.createMessageCorrelation("LabSegmentResult").processInstanceId(id).correlate()`. `.processInstanceId(id)` thu hẹp tìm kiếm chỉ trong đúng instance này (không thì Camunda có thể correlate nhầm sang 1 instance khác đang cùng chờ message tên đó). Nếu tìm thấy đúng đăng ký chờ (thường có sẵn từ transaction start, vì nhánh receive task không bị async chặn) → trigger, hoàn tất `lab_receive_segment`, token tới `lab_segment_join`. Không thấy → ném `MismatchingMessageCorrelationException`.

**Điểm hay ở đây**: cái nào (bước 3 hoặc bước 4) chạy **sau cùng** — tức là cái làm đủ 2 token tại join — thì ngay trong transaction của chính API đó, engine đi tiếp luôn: đóng subprocess, chạy output mapping (`isSegmentEvaluatedSuccess`, `segmentType` đẩy ra scope process chính), rồi tới gateway `lab_segment_ok` ("Segment thành công?") — gateway này check đúng 1 biến `${isSegmentEvaluatedSuccess == true}` (nhánh `Yes` có điều kiện, nhánh `No` là default nên không cần điều kiện gì). Nếu đạt, đi tiếp vào `lab_precheck` — mà task này chỉ có `camunda:asyncAfter`, **không có** `asyncBefore`, nên việc "vào" nó cũng chạy đồng bộ luôn, ngay trong transaction đó, tạo external task `lab-precheck` thật liền — không cần Job Executor can thiệp gì cả!

Nên nhiều khi response của chính bước 3 hoặc bước 4 (cái gọi sau) đã thấy `waitingExternalTaskTopic=lab-precheck` luôn rồi, khỏi cần gọi `GET` chờ thêm.

### Bước 5 — `GET /lab/{id}` (đợi `waitingExternalTaskTopic=lab-precheck`)

```bash
curl --location 'http://localhost:8086/api/v1/camunda-demo/lab/{{processInstanceId}}'
```

### Bước 6 — `POST /lab/{id}/process-precheck`

```bash
curl --location --request POST 'http://localhost:8086/api/v1/camunda-demo/lab/{{processInstanceId}}/process-precheck'
```

Y hệt logic bước 3: tìm task topic `lab-precheck`, lock, đọc `requestPrecheck` (map từ `precheckPassed`), tính `isPrecheckPassed`, complete.

Nhưng có 1 điểm **ngược lại** với `lab_send_segment`: task này đánh dấu `asyncAfter`, tức là ranh giới async nằm ở chỗ **rời** task, không phải lúc **vào** task. Nghĩa là: `externalTaskService.complete(...)` ở bước này hoàn tất external task xong là **dừng luôn trong transaction đó** — việc đánh giá gateway `lab_precheck_ok` ("Precheck đạt?") **không** chạy ngay, mà engine ghi thêm 1 job hẹn giờ khác, để Job Executor nền xử lý tiếp sau đó (y như cách nó xử lý `lab_send_segment` lúc đầu, chỉ khác là lần này job nằm ở đầu ra thay vì đầu vào).

Vậy nên response ngay sau khi gọi `process-precheck` có thể **chưa** thấy kết quả rẽ nhánh liền (chưa thấy `SKIPPED_SCORING` hay `waitingTaskName`) — phải đợi Job Executor chạy job đó xong (cũng rất nhanh, nhưng vẫn có độ trễ y như bước 1).

### Bước 7 — `GET /lab/{id}` (đợi `waitingTaskName=Review chấm điểm`, hoặc outcome=`SKIPPED_SCORING` nếu `segmentType=VTP_OFF_NET`)

```bash
curl --location 'http://localhost:8086/api/v1/camunda-demo/lab/{{processInstanceId}}'
```

Lúc này, sau khi Job Executor chạy xong job hẹn giờ ở bước 6, gateway `lab_precheck_ok` mới thật sự được đánh giá:
- `isPrecheckPassed=false` → `lab_rejected_precheck`, kết thúc.
- `true` → tới gateway `lab_need_scoring` ("Cần chấm điểm?"), check `${segmentType == 'VTP_OFF_NET'}`:
  - Đúng → `lab_skip_scoring`, kết thúc ngay (`SKIPPED_SCORING`), **không cần gọi thêm gì nữa**.
  - Không đúng (default) → chạy execution listener của sequence flow (`labRoute=scoring`), tạo User Task `lab_manual_review`, gán `assignee=demo`, chạy task listener `event=create` → `labReviewEvent=created`.

### Bước 8 — `POST /lab/{id}/complete-review`

```bash
curl --location --request POST 'http://localhost:8086/api/v1/camunda-demo/lab/{{processInstanceId}}/complete-review'
```

`taskService.createTaskQuery().processInstanceId(id).taskDefinitionKey("lab_manual_review").singleResult()` — tìm đúng task, không có thì báo lỗi. Có thì `taskService.complete(taskId)`: chạy task listener `event=complete` → `labReviewEvent=completed`, ghi output mapping `reviewCompleted=true`, token rời task này, tới `lab_submit_message` (intermediate throw event) — chạy expression gắn trên message event definition: `${execution.setVariable('labMessageThrown', true)}`, rồi tới thẳng `lab_ready_for_scoring` (end event, `status=READY_FOR_SCORING`). Toàn bộ đoạn cuối này không có async nào chặn nên chạy hết luôn trong 1 transaction của chính request `complete-review`.

### Bước 9 — `GET /lab/{id}` (xác nhận kết quả cuối)

```bash
curl --location 'http://localhost:8086/api/v1/camunda-demo/lab/{{processInstanceId}}'
```

`outcome` phải là `lab_ready_for_scoring`, `visitedActivities` có đủ toàn bộ các activity đã đi qua, `variables` có `labMessageThrown=true`, `labReviewEvent=completed`, `labLifecycle=ended`...

---

## 5. Full curl — luồng thành công từ đầu tới cuối

Copy nguyên khối, import vào Postman (Import → Raw text), chạy lần lượt:

```bash
curl --location 'http://localhost:8086/api/v1/camunda-demo/lab/start' \
--header 'Content-Type: application/json' \
--data '{
  "segmentSuccess": true,
  "precheckPassed": true,
  "segmentType": "VTP_ON_NET"
}'
```
```bash
curl --location 'http://localhost:8086/api/v1/camunda-demo/lab/{{processInstanceId}}'
```
```bash
curl --location --request POST 'http://localhost:8086/api/v1/camunda-demo/lab/{{processInstanceId}}/process-segment'
```
```bash
curl --location --request POST 'http://localhost:8086/api/v1/camunda-demo/lab/{{processInstanceId}}/correlate-segment'
```
```bash
curl --location 'http://localhost:8086/api/v1/camunda-demo/lab/{{processInstanceId}}'
```
```bash
curl --location --request POST 'http://localhost:8086/api/v1/camunda-demo/lab/{{processInstanceId}}/process-precheck'
```
```bash
curl --location 'http://localhost:8086/api/v1/camunda-demo/lab/{{processInstanceId}}'
```
```bash
curl --location --request POST 'http://localhost:8086/api/v1/camunda-demo/lab/{{processInstanceId}}/complete-review'
```
```bash
curl --location 'http://localhost:8086/api/v1/camunda-demo/lab/{{processInstanceId}}'
```

**Tip Postman**: qua tab **Tests** của request Start, thêm:
```javascript
const res = pm.response.json();
pm.environment.set("processInstanceId", res.processInstanceId);
```
rồi chọn 1 Environment bất kỳ trước khi chạy — từ đó các request sau tự lấy đúng `{{processInstanceId}}`, khỏi phải copy tay.

Muốn test nhánh khác thì đổi body ở request Start:
- `segmentSuccess: false` → dừng ở `lab_rejected_segment` sau bước correlate-segment, khỏi cần gọi các bước sau.
- `precheckPassed: false` → dừng ở `lab_rejected_precheck` sau bước process-precheck.
- `segmentType: "VTP_OFF_NET"` → dừng ở `lab_skip_scoring` sau bước process-precheck, khỏi cần complete-review.

---

## 6. Chạy nhiều pod thì sao — có bị lỗi gì không?

Không. Như nói ở mục 1, engine không giữ state trong RAM app — mọi thứ nằm trong Postgres. Pod nào nhận request cũng đọc/ghi đúng chỗ đó, không cần "dính" đúng 1 pod theo từng request (không cần sticky session).

Cái duy nhất cần tránh đụng nhau là khi 2 pod cùng lúc cố lock/chạy đúng 1 job hoặc 1 external task — Camunda giải quyết bằng **optimistic locking** ngay trên row DB (cột kiểu `LOCK_EXP_TIME_`, `WORKER_ID_`, kèm version). Hai pod cùng `UPDATE` 1 dòng thì chỉ 1 cái thành công, cái kia update trúng 0 dòng, tự bỏ qua — không bao giờ có chuyện 2 pod complete trùng 1 task.

Vài điểm cần lưu ý khi thật sự scale ra nhiều pod (không phải lỗi, chỉ là vận hành):
- Nhiều pod cùng start lần đầu → mỗi pod đều cố chạy `schema-update` — nên tách việc tạo schema ra 1 job/migration riêng chạy 1 lần, đừng để mọi pod pod tự làm.
- Deploy BPMN lúc nhiều pod start cùng lúc → an toàn nhờ dedup filtering có sẵn, không lo tạo trùng definition.
- Job Executor chạy trên mọi pod hơi phí nếu cluster đông — hệ thống lớn có thể tách riêng: vài pod chỉ nhận REST, tắt job executor (`camunda.bpm.job-execution.enabled=false`), vài pod chuyên chạy job.

---

## 7. Xem trực quan bằng Cockpit/Tasklist

Đã có sẵn `camunda-bpm-spring-boot-starter-webapp`, chạy chung server, không cần deploy gì thêm:

- Cockpit (xem sơ đồ + token đang đứng đâu, biến, lịch sử): `http://localhost:8086/camunda/app/cockpit/`
- Tasklist (xem/complete User Task `Review chấm điểm` bằng tay): `http://localhost:8086/camunda/app/tasklist/`

Login: `admin` / `Admin@12345` (khai trong `application.yaml`, mục `camunda.bpm.admin-user`). Vào Cockpit chọn process **"Cashloan feature lab"**, click vào instance vừa tạo bằng curl ở mục 5 là thấy ngay sơ đồ tô đúng vị trí hiện tại.

---

## 8. Mấy lỗi hay gặp khi gọi sai thứ tự

| Tình huống | Kết quả |
|---|---|
| Gọi `process-segment` khi chưa có external task `lab-segment` (Job Executor chưa kịp chạy) | HTTP 500, `IllegalStateException`, process giữ nguyên trạng thái |
| Gọi `correlate-segment` khi instance không chờ message đó (gọi 2 lần, hoặc quá sớm) | Lỗi correlation, process giữ nguyên trạng thái |
| Gọi `process-precheck` khi chưa tới lượt (gateway segment chưa xử lý xong, hoặc `waitingExternalTaskTopic` chưa phải `lab-precheck`) | HTTP 500, process giữ nguyên trạng thái |
| Gọi `complete-review` khi chưa có User Task (chưa tới bước review, hoặc `VTP_OFF_NET` đã bị bỏ qua review) | HTTP 500, process giữ nguyên trạng thái |
| External task đã bị lock bởi lần gọi khác chưa hết hạn (30s) | Lỗi lock, phải đợi hết hạn hoặc lần gọi trước complete xong |

Tất cả các lỗi trên đều **không làm hỏng process instance** — nó chỉ đứng yên đúng chỗ, gọi lại đúng thứ tự là chạy tiếp bình thường.
