# Cake Cashloan Lab — copy từ luồng OB Cake ví trả sau, giải thích chi tiết

## 0. File này là gì, khác gì với lab cũ

Đây là bản copy gần như y nguyên process `CAKE_CASHLOAN` ("Cashloan cake") từ project thật `lending-event-backbone` (luồng onboarding ví trả sau Cake) vào project lab này, để bạn vọc thử mà không đụng gì tới code backbone thật. Mọi thứ ở đây **tách biệt hoàn toàn** với lab `cashloan-feature-lab` đã làm trước đó:

| | Lab cũ (`cashloan-feature-lab`) | Lab này (`cake-cashloan`) |
|---|---|---|
| File BPMN | `processes/cashloan-feature-lab.bpmn` | `processes/cake-cashloan.bpmn` |
| Process key | `CAKE_FEATURE_LAB` | `CAKE_CASHLOAN` |
| Package Java | `com.example.learning.camunda` | `com.example.learning.camunda.cashloan` |
| Service/Controller | `CashloanDemo` / `CashloanDemoController` | `CakeCashloanService` / `CakeCashloanController` |
| Base API | `/api/v1/camunda-demo` | `/api/v1/cake-cashloan-demo` |
| Cách "xử lý nghiệp vụ" | External Task (`camunda:type="external"`) — Spring Boot phải chủ động lock + complete | `camunda:class` (JavaDelegate) — Camunda tự gọi class Java, không cần lock gì cả |

Điểm khác biệt lớn nhất giữa hai lab chính là cách BPMN gọi vào code Java, mình giải thích kỹ ở mục 1.

Toàn bộ 9 class Java xử lý (`delegate` package) mình viết mới hoàn toàn trong project lab này, class rỗng chỉ log ra để bạn thấy "đây là chỗ sau này cắm logic gọi hệ thống ngoài thật", tuyệt đối không đụng gì tới code backbone gốc.

**Lưu ý quan trọng:** mình sửa code/BPMN trực tiếp trên máy bạn qua kết nối remote, nhưng máy đó lại không cho phép tải dependency từ Maven Central (mạng bị chặn), nên mình **chưa build/compile thử được**. Bạn chạy `mvn compile` (hoặc chạy app) ở máy thật rồi báo lại nếu có lỗi gì nhé.

## 1. `camunda:class` chạy khác `camunda:type="external"` như thế nào

Ở lab cũ, mấy bước như "Phân loại segment" được đánh dấu `camunda:type="external" camunda:topic="lab-segment"`. Kiểu này giống như Camunda **treo một cái task lơ lửng** ở đó, ai muốn lấy thì phải chủ động gọi API `fetchAndLock`, xử lý xong thì gọi `complete`. Spring Boot đóng vai một "worker" đứng ngoài, phải chủ động đi lấy việc.

Còn ở process `CAKE_CASHLOAN` này, các bước tương tự (Phân loại segment, Precheck, Chấm điểm,...) lại được đánh dấu `camunda:class="com.example.learning.camunda.cashloan.delegate.XxxDelegate"`. Đây là kiểu **JavaDelegate** — khác hẳn về bản chất vận hành:

- Không có bước "lock" nào cả. Ngay khi token BPMN đi tới activity đó, engine **tự động new/lấy bean** class đó ra và gọi thẳng hàm `execute()` — y hệt như gọi một hàm Java bình thường trong code của bạn.
- Vì project mình dùng `camunda-bpm-spring-boot-starter` (chạy trên `camunda-engine-spring`), nên Camunda tạo bean này thông qua `applicationContext.getAutowireCapableBeanFactory().createBean(...)` — nghĩa là class **không cần** gắn `@Component`/`@Service` gì cả, Spring vẫn tự autowire được nếu bên trong bạn có field cần inject (mấy class demo ở đây chưa cần inject gì nên để trống constructor).
- Vì các Send Task này có thêm `camunda:asyncBefore="true"`, nên thực tế nó chạy trong một **Job** riêng do Job Executor (thread pool nền của Camunda) xử lý, KHÔNG chạy chung thread với request HTTP gọi API — điều này quan trọng để tách transaction: nếu class `execute()` bị lỗi, Job Executor sẽ tự động retry job đó theo cơ chế retry mặc định của Camunda (3 lần, có cooldown), chứ không làm hỏng luôn cái request HTTP đang treo.

Nói cách khác: **External Task = Camunda chờ ai đó bên ngoài tới lấy việc.** **JavaDelegate (`camunda:class`) = Camunda tự chạy code ngay tại chỗ, không chờ ai cả.**

## 2. Vậy làm sao mô phỏng "gọi hệ thống ngoài, chưa có kết quả ngay"?

Nhìn lướt BPMN thật bạn sẽ thấy: mỗi bước nghiệp vụ lớn (Phân loại segment, Precheck, Chấm điểm, Ra quyết định, Thẩm định,...) đều được vẽ thành một **Sub-process** riêng, bên trong luôn có cùng một khuôn mẫu:

```text
   (start subprocess)
        │
   Parallel Gateway (fork)
    ┌───┴────────────────┐
    │                    │
Send Task              Receive Task
(camunda:class,        (chờ message,
 chạy NGAY LẬP TỨC)      ví dụ "PRECHECK_RESULT")
    │                    │
    └───┬────────────────┘
   Parallel Gateway (join — chờ CẢ HAI nhánh xong)
        │
   (end subprocess, đi tiếp ra ngoài)
```

Đây chính xác là pattern giống hệt "Phân loại segment" ở lab cũ, chỉ khác cơ chế Send Task bên trái (class thay vì external task). Ý tưởng thiết kế rất hay: Send Task đóng vai "bắn request đi" (giả lập gọi API sang hệ thống thẩm định/chấm điểm/ekyc thật), còn Receive Task đóng vai "chờ webhook/callback trả kết quả về" dưới dạng một BPMN Message. Vì là Parallel Gateway (AND), process **chỉ đi tiếp khi cả Send Task đã chạy xong VÀ message đã được correlate** — tức là bạn (Spring Boot) phải chủ động gọi API correlate message thì process mới nhích tiếp, giống hệt việc hệ thống ngoài gọi callback báo kết quả về.

9 class delegate mình tạo (`EvaluateCustomerSegmentDelegate`, `PreCheckDelegate`, `CheckStandardizedProfileDelegate`, `PackageOfferDelegate`, `GetRepeatPackageDelegate`, `GetScoringDelegate`, `DecisionMakingDelegate`, `UnderwritingApplicationDelegate`, `SubmitApplicationDelegate`) chính là 9 "Send Task" đó. Riêng `PreCheckDelegate` được dùng lại tới **3 lần** ở 3 chỗ khác nhau trong process (Precheck lần 1, Precheck lần 2 nhánh chưa chuẩn hóa, Precheck lần 2 nhánh đã chuẩn hóa) — class phân biệt đang chạy ở đâu bằng `execution.getCurrentActivityId()`, log ra cho bạn thấy rõ.

Còn `submit_application` hơi khác: nó không phải Send Task trong Sub-process, mà là một `intermediateThrowEvent` (event ném message ra ngoài) gắn `camunda:class` trực tiếp trên `messageEventDefinition`. Kiểu này chạy xong `execute()` là **đi tiếp luôn**, không có Receive Task nào chờ cả — tức bước "Submit loan application" được coi là fire-and-forget, không cần bạn gọi API gì thêm sau nó.

## 3. Vì sao properties panel trong Modeler "không thấy gì được định nghĩa"?

Giống hệt câu hỏi bạn từng hỏi ở lab cũ. Ở các Send Task này, phần Input/Output Mapping trong Modeler trống trơn — đúng, vì BPMN gốc thật sự không map biến gì cả cho các Send Task. Chỗ duy nhất có cấu hình là tab **General** → thấy trường **Delegate reference** (chính là `camunda:class`) và ở tab kế bên có `camunda:asyncBefore = true`. Không có gì "ẩn" cả, các class demo mình viết cũng chỉ đọc biến process trực tiếp qua `execution.getVariable(...)`, không dựa vào input mapping nào.

## 4. Sơ đồ luồng thành công đầy đủ (happy path)

Đây là đường đi từ Start tới thành công, mình trace trực tiếp từ file BPMN gốc (chọn tất cả nhánh "đạt" ở mỗi gateway):

```text
StartEvent "Application init"
  → [Sub-process] Phân loại segment  (EvaluateCustomerSegmentDelegate)
      chờ message EVALUATE_CUSTOMER_SEGMENT_RESULT
  → Gateway "Check phân loại segment thành công?"      — Yes: isSegmentEvaluatedSuccess == true
  → [Sub-process] Precheck (lần 1)  (PreCheckDelegate)
      chờ message PRECHECK_RESULT
  → Gateway "Precheck qualified?"                        — Yes: isPrecheckPassed == true
  → Gateway "Lấy gói vay cũ? (Upsell/Vay lại)"            — No (default): needGetRepeatPackage != true
      (bỏ qua Sub-process "Lấy gói vay lại", đi thẳng)
  → [Sub-process] Phân gói  (PackageOfferDelegate)
      chờ message OFFER_PACKAGE_RESULT
  → Gateway "Phân gói thành công?"                        — Yes: packageOfferSuccess == true
  → User Task "Nhập số tiền + kỳ hạn" (cust.input.application.form_1)
  → [Sub-process] Check CHHS  (CheckStandardizedProfileDelegate)
      chờ message CHECK_STANDARDIZED_PROFILE_RESULT
  → Gateway "Check chuẩn hóa hồ sơ thành công?"           — Yes: checkStandardizedProfileSuccess == true
  → Gateway "VTP ngoại mạng?"                             — No: segmentType != 'VTP_OFF_NET'
  → Gateway "Chuẩn hóa hay ko?"                           — No (default): useStandardizedEkycFlow != true
  → User Task "Ekyc" (cust.ekyc)
  → Gateway "Có yêu cầu back lại hay không?"              — No (default): back != true
  → Gateway "Ekyc success?"                               — Yes: ekycSuccess == true
  → Gateway "Chuẩn hóa hay ko?" (lần 2)                   — No: useStandardizedEkycFlow != true (đã set ở trên)
  → [Sub-process] Precheck 2 - nhánh chưa chuẩn hóa  (PreCheckDelegate, lần 2)
      chờ message PRECHECK_RESULT (đúng message name như precheck lần 1)
  → Gateway "Precheck lần 2 pass?"                        — Yes: isPrecheckPassed == true
  → User Task "Nhập thông tin khách hàng" (cust.input.application.form)
  → Gateway "VTP ngoại mạng?"                             — No: segmentType != 'VTP_OFF_NET'
  → [Sub-process] Chấm điểm  (GetScoringDelegate)
      chờ message SCORING_RESULT
  → [Sub-process] Decision making  (DecisionMakingDelegate)
      chờ message DECISION_MAKING_RESULT
  → Gateway "Decision pass?"                              — Yes: isDecisionMakingPassed == true
  → Event "Submit loan application"  (SubmitApplicationDelegate — chạy đồng bộ, đi tiếp NGAY)
  → [Sub-process] Underwriting  (UnderwritingApplicationDelegate)
      chờ message UNDERWRITING_RESULT
  → Gateway "Underwriting pass?"                          — (Yes, mặc định là "No"!) underwritingPassed == true
  → User Task "Ký hợp đồng" (cust.sign-contract)
  → EndEvent "Chờ giải ngân" (pending_disbursement)  ✅ THÀNH CÔNG
```

Chú thích một chỗ dễ nhầm: ở gateway "Underwriting pass?", nhánh mặc định (`default` flow trong BPMN) lại là nhánh **No** (đi tới `underwriting_fail`), còn nhánh đi tiếp thành công lại là nhánh có điều kiện rõ ràng `underwritingPassed == true` nhưng không đặt tên "Yes" trên diagram. Modeler vẽ hơi lạ nhưng logic runtime hoàn toàn đúng — Camunda luôn ưu tiên đánh giá flow có điều kiện trước, chỉ rớt về default khi không flow nào match.

Ngoài happy path trên, process còn rất nhiều nhánh fail/nhánh phụ khác (VTP ngoại mạng đi tắt qua ekyc bắt buộc, khách có reoffer, cần đổi số tiền vay, lấy gói vay cũ, back lại giữa chừng ekyc,...) — BPMN gốc rất đầy đủ nghiệp vụ thật, nhưng mình không liệt kê hết ở đây để tránh loãng, bạn cứ mở Modeler nhìn trực quan là thấy ngay.

## 5. Vì sao API thiết kế "tổng quát" thay vì một API riêng cho từng bước

Lab cũ chỉ có 2-3 bước nên viết API riêng (`process-segment`, `process-precheck`, `correlate-segment`,...) là hợp lý, dễ đọc. Nhưng `CAKE_CASHLOAN` có tới **8 message khác nhau** cần correlate (ứng với 9 Send Task, trong đó Precheck dùng chung 1 message name cho cả 3 lần) và **7 User Task** khác nhau (nhập thông tin, ekyc, ký hợp đồng, reoffer,...). Nếu bắt chước style cũ, bạn sẽ phải viết ngót nghét 15 endpoint gần như giống hệt nhau chỉ khác tên message/task — vừa dư thừa code, vừa khó maintain khi BPMN thêm bớt bước sau này.

Nên `CakeCashloanController` chỉ có **2 API tổng quát**, truyền đúng tên là dùng được cho MỌI điểm chờ trong process:

- `POST /{id}/correlate` — body `{ "messageName": "...", "variables": {...} }` → dùng cho bất kỳ Receive Task / message catch nào.
- `POST /{id}/tasks/{taskDefinitionKey}/complete` — body là map biến → dùng cho bất kỳ User Task nào.

Muốn biết lúc nào gọi API nào, cứ gọi `GET /{id}` trước — response luôn có `waitingMessages` (danh sách message đang mở chờ) và `waitingTasks` (danh sách User Task đang mở chờ, kèm `taskDefinitionKey`). Đây cũng là cách hay để tự trace luồng thay vì phải nhớ thuộc lòng cả cái sơ đồ ở mục 4.

## 6. API tổng quan

Base URL: `http://localhost:8086/api/v1/cake-cashloan-demo`

| Method | Path | Tác dụng |
|---|---|---|
| POST | `/start` | Tạo process instance mới, trả về `processInstanceId` |
| GET | `/{id}` | Xem trạng thái: đã đi qua activity nào, đang chờ message/task nào, toàn bộ biến |
| POST | `/{id}/correlate` | Gửi kết quả cho một Receive Task đang chờ, qua tên message |
| POST | `/{id}/tasks/{taskDefinitionKey}/complete` | Hoàn thành một User Task đang chờ |

## 7. Full curl walkthrough — luồng thành công từ đầu tới cuối

Copy tuần tự vào Postman, nhớ thay `{id}` bằng `processInstanceId` lấy từ response bước 1 (nếu dùng Postman Tests tab, thêm `pm.environment.set("id", pm.response.json().processInstanceId);` ở request Start để tự động điền cho các request sau).

### Bước 1 — Start process

```bash
curl --location 'http://localhost:8086/api/v1/cake-cashloan-demo/start' \
  --header 'Content-Type: application/json' \
  --data '{}'
```

Response mẫu:

```json
{
  "processInstanceId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "statusUrl": "/api/v1/cake-cashloan-demo/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

Ngay tại thời điểm request này trả về, `EvaluateCustomerSegmentDelegate` gần như đã chạy xong rồi (vì Job Executor xử lý job `asyncBefore` cực nhanh, tính bằng mili-giây) — bạn có thể thấy log của nó trong console ngay sau khi gọi xong.

### Bước 2 — Xem trạng thái, kiểm tra đang chờ gì

```bash
curl 'http://localhost:8086/api/v1/cake-cashloan-demo/{id}'
```

Kỳ vọng thấy: `"waitingMessages": ["EVALUATE_CUSTOMER_SEGMENT_RESULT"]`.

### Bước 3 — Trả kết quả phân loại segment

```bash
curl --request POST 'http://localhost:8086/api/v1/cake-cashloan-demo/{id}/correlate' \
  --header 'Content-Type: application/json' \
  --data '{
    "messageName": "EVALUATE_CUSTOMER_SEGMENT_RESULT",
    "variables": {
      "isSegmentEvaluatedSuccess": true,
      "segmentType": "VTP_ON_NET"
    }
  }'
```

### Bước 4 — Precheck lần 1

```bash
curl --request POST 'http://localhost:8086/api/v1/cake-cashloan-demo/{id}/correlate' \
  --header 'Content-Type: application/json' \
  --data '{
    "messageName": "PRECHECK_RESULT",
    "variables": {
      "isPrecheckPassed": true,
      "needGetRepeatPackage": false
    }
  }'
```

### Bước 5 — Phân gói

```bash
curl --request POST 'http://localhost:8086/api/v1/cake-cashloan-demo/{id}/correlate' \
  --header 'Content-Type: application/json' \
  --data '{
    "messageName": "OFFER_PACKAGE_RESULT",
    "variables": {
      "packageOfferSuccess": true
    }
  }'
```

### Bước 6 — User Task "Nhập số tiền + kỳ hạn"

```bash
curl --request POST 'http://localhost:8086/api/v1/cake-cashloan-demo/{id}/tasks/cust.input.application.form_1/complete' \
  --header 'Content-Type: application/json' \
  --data '{
    "loanAmount": 20000000,
    "tenor": 6
  }'
```

(`loanAmount`, `tenor` chỉ là dữ liệu demo cho thật, gateway phía sau không đọc hai biến này.)

### Bước 7 — Check CHHS (hồ sơ chuẩn hóa)

```bash
curl --request POST 'http://localhost:8086/api/v1/cake-cashloan-demo/{id}/correlate' \
  --header 'Content-Type: application/json' \
  --data '{
    "messageName": "CHECK_STANDARDIZED_PROFILE_RESULT",
    "variables": {
      "checkStandardizedProfileSuccess": true,
      "useStandardizedEkycFlow": false
    }
  }'
```

### Bước 8 — User Task "Ekyc"

```bash
curl --request POST 'http://localhost:8086/api/v1/cake-cashloan-demo/{id}/tasks/cust.ekyc/complete' \
  --header 'Content-Type: application/json' \
  --data '{
    "back": false,
    "ekycSuccess": true
  }'
```

### Bước 9 — Precheck lần 2 (nhánh chưa chuẩn hóa)

```bash
curl --request POST 'http://localhost:8086/api/v1/cake-cashloan-demo/{id}/correlate' \
  --header 'Content-Type: application/json' \
  --data '{
    "messageName": "PRECHECK_RESULT",
    "variables": {
      "isPrecheckPassed": true
    }
  }'
```

Để ý: message name giống hệt bước 4 (`PRECHECK_RESULT`). Camunda vẫn correlate đúng vì nó chỉ cần khớp `processInstanceId` + đang có một Receive Task nào đó của instance này thật sự subscribe message này — lúc này chỉ còn đúng một chỗ đang mở (Precheck lần 2), nên không sợ nhầm sang Precheck lần 1 (đã đóng từ bước 4 rồi).

### Bước 10 — User Task "Nhập thông tin khách hàng"

```bash
curl --request POST 'http://localhost:8086/api/v1/cake-cashloan-demo/{id}/tasks/cust.input.application.form/complete' \
  --header 'Content-Type: application/json' \
  --data '{}'
```

### Bước 11 — Chấm điểm

```bash
curl --request POST 'http://localhost:8086/api/v1/cake-cashloan-demo/{id}/correlate' \
  --header 'Content-Type: application/json' \
  --data '{
    "messageName": "SCORING_RESULT",
    "variables": {}
  }'
```

### Bước 12 — Ra quyết định

```bash
curl --request POST 'http://localhost:8086/api/v1/cake-cashloan-demo/{id}/correlate' \
  --header 'Content-Type: application/json' \
  --data '{
    "messageName": "DECISION_MAKING_RESULT",
    "variables": {
      "isDecisionMakingPassed": true
    }
  }'
```

Ngay sau bước này, `submit_application` (SubmitApplicationDelegate) chạy đồng bộ luôn — không cần gọi API gì thêm cho nó, process tự trôi thẳng sang Sub-process Underwriting.

### Bước 13 — Thẩm định hồ sơ (Underwriting)

```bash
curl --request POST 'http://localhost:8086/api/v1/cake-cashloan-demo/{id}/correlate' \
  --header 'Content-Type: application/json' \
  --data '{
    "messageName": "UNDERWRITING_RESULT",
    "variables": {
      "underwritingPassed": true
    }
  }'
```

### Bước 14 — Ký hợp đồng

```bash
curl --request POST 'http://localhost:8086/api/v1/cake-cashloan-demo/{id}/tasks/cust.sign-contract/complete' \
  --header 'Content-Type: application/json' \
  --data '{}'
```

### Bước 15 — Kiểm tra kết quả cuối

```bash
curl 'http://localhost:8086/api/v1/cake-cashloan-demo/{id}'
```

Kỳ vọng: `"outcome": "pending_disbursement"` — nghĩa là hồ sơ đã đi hết luồng, đang ở trạng thái "Chờ giải ngân" 🎉.

## 8. Đọc response `GET /{id}` như thế nào

```json
{
  "processInstanceId": "...",
  "outcome": "RUNNING",
  "visitedActivities": ["StartEvent_1", "evaluate_customer_segment", "..."],
  "waitingTasks": [
    { "taskId": "...", "taskDefinitionKey": "cust.ekyc", "name": "Ekyc" }
  ],
  "waitingMessages": ["PRECHECK_RESULT"],
  "variables": { "isSegmentEvaluatedSuccess": true, "segmentType": "VTP_ON_NET" }
}
```

| Field | Ý nghĩa |
|---|---|
| `outcome` | `RUNNING` nếu chưa xong, hoặc id của End Event cuối cùng nếu đã kết thúc (`pending_disbursement` = thành công, các `*_fail` khác = dừng ở nhánh lỗi) |
| `visitedActivities` | toàn bộ activity đã đi qua theo thứ tự thời gian (đọc từ History) |
| `waitingTasks` | User Task đang mở, gọi `POST /{id}/tasks/{taskDefinitionKey}/complete` |
| `waitingMessages` | tên message đang có Receive Task chờ, gọi `POST /{id}/correlate` |
| `variables` | toàn bộ biến hiện có của process (đọc từ History, gồm cả biến do delegate set và biến bạn truyền qua API) |

Nếu gọi correlate/complete sai thời điểm (ví dụ message chưa mở, hoặc task đã complete rồi), Camunda sẽ trả lỗi 500 kèm message dạng "cannot correlate message... no subscriptions" hoặc "task does not exist" — cứ gọi lại `GET /{id}` để xem thực tế đang đứng ở đâu.

## 9. Xem trực quan trên Cockpit

Giống hệt lab cũ, mở `http://localhost:8086/camunda`, đăng nhập `admin` / `Admin@12345`, vào Cockpit, gõ process definition `CAKE_CASHLOAN` (name "Cashloan cake") để xem sơ đồ instance đang chạy tới đâu — activity nào đang sáng đèn (active) chính là activity engine đang đứng chờ. Cách này trực quan hơn nhiều so với đọc `visitedActivities` dạng text.
