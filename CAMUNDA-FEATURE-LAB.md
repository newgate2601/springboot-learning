# Camunda 7 feature lab: đọc Modeler và chạy từng bước

Mở [`src/main/resources/processes/cashloan-feature-lab.bpmn`](src/main/resources/processes/cashloan-feature-lab.bpmn) bằng Camunda Modeler. Process ID là `CAKE_FEATURE_LAB`, tên hiển thị trong Cockpit là **Cashloan feature lab**. Đây là flow học tập độc lập với `CAKE_CASHLOAN` và `CAKE_CASHLOAN_LEARNING`: worker chỉ log và trả kết quả giả lập, flow dừng ở bước chuẩn bị chấm điểm.

## Chuẩn bị

1. Bật Camunda Run 7 tại `http://localhost:8080` và PostgreSQL của app tại cổng 5432.
2. Chạy `LearningApplication` với JDK 21 trong IDE hoặc dùng `.\mvnw.cmd spring-boot:run`. App dùng cổng 8086, tự deploy BPMN khi khởi động và poll external tasks mỗi khoảng 1 giây. Nếu Camunda Run khởi động sau app, khởi động lại app để deploy.
3. Trong Postman, chọn **Import → Raw text**, copy **một lệnh cURL** trong tài liệu, rồi chọn **Continue → Import**. Các lệnh gọi app `:8086`; app gọi Camunda REST `:8080/engine-rest`. Giữ app chạy để worker có thể xử lý.

Nếu muốn chạy test tự động với Camunda Run đang bật, đặt `CAMUNDA_DEMO_IT=true` rồi chạy `.\mvnw.cmd test`. Test tạo instance thật cho các nhánh segment fail, Precheck fail, ngoại mạng, review và message thủ công.

Mỗi request Start tạo **một process instance mới** và trả JSON dạng:

```json
{
  "processInstanceId": "<ID-do-Camunda-tao>",
  "statusUrl": "/api/v1/camunda-demo/lab/<ID-do-Camunda-tao>"
}
```

Start trả ID sau khi engine tạo instance, không đợi mọi bước hoàn tất. Trong các cURL sau, thay `INSTANCE_ID` bằng `processInstanceId` của **đúng instance đang thử**. Nếu Get status còn `RUNNING`, đợi khoảng 1–2 giây rồi gọi lại. `outcome` là trường app suy ra từ end event đã đi qua; trong BPMN không có biến tên `outcome`.

**Thứ tự học:** chạy case mặc định từ mục 1 đến mục 6; thử các nhánh thất bại ở mục 3, 4 và nhánh ngoại mạng ở mục 5 bằng instance mới. Cuối cùng thử receive task chờ message thủ công ở mục 2.

## Đi theo từng ô trên Modeler, chạy cURL và xem kết quả

Chọn ô hoặc đường nối để xem panel **Properties**. Mở dấu `+` của subprocess **Phân loại segment** để thấy các ô bên trong. Các expression `${...}` là Camunda Expression Language (JUEL): engine đánh giá chúng **khi token đến đúng chỗ cấu hình**, không phải lúc bạn mở file trong Modeler. Cùng cú pháp có thể đọc biến, trả Boolean cho gateway hoặc gọi phương thức để ghi biến. [Tài liệu Expression Language](https://docs.camunda.org/manual/7.24/user-guide/process-engine/expression-language/unified-expression-language/).

### 1. Process và Start Event “Application init”

**Click vùng trống của sơ đồ để chọn process.** General có ID `CAKE_FEATURE_LAB`, Name `Cashloan feature lab`, `Executable=true` và History TTL `30`. ID là key app dùng để start đúng definition; Executable cho phép engine chạy; TTL là số ngày phục vụ chính sách dọn dẹp lịch sử sau khi instance kết thúc, tùy engine có bật cleanup. TTL không phải bộ hẹn giờ của flow.

Process có Documentation giải thích mục tiêu học tập, cùng extension properties `demoCategory=cashloan-feature-lab` và `owner=training`. Hai property này hiện **không được code đọc**, nên chỉ là metadata minh họa trong Modeler; xóa chúng không đổi đường chạy. Documentation cũng chỉ để người đọc mô hình hiểu, không thực thi. Trên process còn có execution listener `start` chạy `${execution.setVariable('labLifecycle', 'started')}` và listener `end` đổi biến thành `ended`. Chúng là móc chạy tự động lúc vòng đời process bắt đầu/kết thúc; trong lab chỉ ghi dấu, không gọi worker và không quyết định nhánh. Xem Get status khi instance còn chờ review sẽ thấy `started`; sau khi kết thúc thấy `ended`.

**Vì sao có listener và property ở đây?** Execution listener là callback gắn vào vòng đời process/activity/đường nối: `start` khi vào, `end` khi rời, `take` khi đi qua đường nối. Nó có thể ghi audit hoặc khởi tạo biến; lab chỉ ghi `labLifecycle` để bạn quan sát. Nếu xóa listener, các gateway vẫn chọn cùng nhánh nhưng không còn dấu vết này. Extension properties là cặp key/value tùy ý; Camunda không tự hiểu `demoCategory` hay `owner` là luật nghiệp vụ. Chúng chỉ có tác dụng nếu code chủ động đọc. Documentation cũng chỉ lưu lời giải thích trong BPMN. History TTL `30` phục vụ dọn dẹp lịch sử sau khi instance kết thúc nếu engine bật cleanup; nó không tạo timer hay ép task hoàn tất. [History cleanup](https://docs.camunda.org/manual/7.24/user-guide/process-engine/history/history-cleanup/).

**Click Start Event** `lab_start`: trong file BPMN, ô này chỉ có ID, tên **Application init** và Documentation. Nó **không khai báo** `mockSegmentSuccess`, `mockPrecheckPassed`, `mockSegmentType`, `mockAutoCorrelate`; cũng không có form hoặc Input/Output mapping. Vì vậy bạn không thấy bốn tên `mock...` trong ô Start Event là đúng.

Panel **Process variables** trong ảnh của bạn đang liệt kê 5 tên `isSegmentEvaluatedSuccess`, `reviewCompleted`, `segmentType`, `segmentWorkerResult`, `workerResult`. Chúng trùng với các **output parameter** khai báo ở subprocess/task trong BPMN. Đây là danh sách Modeler đọc được từ mô hình lúc thiết kế; nó **không phải** danh sách đầy đủ biến của một instance đang chạy. Biến được app truyền qua REST khi Start và biến listener ghi lúc chạy có thể không hiện trong danh sách đó.

**Nguồn của bốn biến mock là Spring Boot app**, không phải cấu hình của ô Application init: controller nhận body cURL, rồi `CashloanDemo.startProcess()` tạo `variables` trong request gọi Camunda `/process-definition/key/CAKE_FEATURE_LAB/start`. Các trường được chuyển như sau:

- `segmentSuccess` → Boolean `mockSegmentSuccess`, mặc định `true`. Worker phân loại dùng nó để giả lập thành công/thất bại.
- `precheckPassed` → Boolean `mockPrecheckPassed`, mặc định `true`. Worker Precheck dùng nó để giả lập đạt/không đạt.
- `segmentType` → String `mockSegmentType`, mặc định `VTP_ON_NET`. Giá trị `VTP_OFF_NET` sẽ chọn nhánh ngoại mạng.
- `autoCorrelate` → Boolean `mockAutoCorrelate`, mặc định `true`. `false` buộc receive task chờ bạn tự gửi message.

**Cách đọc expression trước khi chạy:** Camunda 7 dùng Expression Language (JUEL), không phải JavaScript trong Postman. Engine đánh giá `${...}` đúng lúc token tới vị trí có cấu hình. Tên biến trong scope hiện tại có thể dùng trực tiếp; `execution` là đối tượng thực thi BPMN, còn `task` là user task trong task listener. Cú pháp này có thể **đọc giá trị** (`${mockSegmentType}`), **trả Boolean để chọn nhánh** (`${isPrecheckPassed == true}`), hoặc **gọi phương thức ghi biến** (`${execution.setVariable('labLifecycle', 'started')}`). Vì vậy phải xem expression nằm ở input mapping, condition hay listener trước khi hiểu nó làm gì. [Expression Language](https://docs.camunda.org/manual/7.24/user-guide/process-engine/expression-language/unified-expression-language/).

Tạo case mặc định:

```bash
curl --location 'http://localhost:8086/api/v1/camunda-demo/lab/start' --header 'Content-Type: application/json' --data '{}'
```

Camunda nhận và lưu bốn biến mock **cùng lúc tạo instance**, process listener đặt `labLifecycle=started`, rồi token từ Start Event đi vào subprocess. Response cURL này chỉ có `processInstanceId` và `statusUrl`. Dùng cURL Get status ở mục 6 để xem `variables.mockSegmentSuccess`, `variables.mockPrecheckPassed`, `variables.mockSegmentType`, `variables.mockAutoCorrelate` ở **runtime**; các giá trị đó không cần xuất hiện trong panel Process variables của Modeler. Mình đã kiểm tra một instance thật trong Camunda History: cả bốn biến mock đều được lưu.

Từ đây, mở ô **Phân loại segment** trong Modeler để theo token vào nhóm bước đầu tiên.

### 2. Subprocess “Phân loại segment”: fork, Send Task, Receive Task và join

**Chọn subprocess `lab_segment`.** Nó gom các bước phân loại trong cùng một phạm vi, không phải một worker task tự thân. Input mapping `requestedSegmentType = ${mockSegmentType}` chạy **lúc vào**: nếu request gửi `VTP_OFF_NET`, biến `requestedSegmentType` trong phạm vi subprocess nhận đúng giá trị đó. Có `${...}` nghĩa là **đọc biến**; viết chữ `mockSegmentType` không có `${...}` sẽ tạo literal String `"mockSegmentType"`. Output mapping chạy **lúc rời** subprocess: `isSegmentEvaluatedSuccess = ${isSegmentEvaluatedSuccess}` và `segmentType = ${segmentType}` đưa hai kết quả ra ngoài để gateway trên sơ đồ chính đọc. Listener start/end ghi `labSegmentLifecycle=started/ended`. Listener end chỉ chạy khi subprocess hoàn tất. Documentation giải thích cặp nhánh song song.

**Mở subprocess**, đi từ start event con đến Parallel Gateway `lab_segment_fork`. Gateway này không đọc biến và không kiểm tra điều kiện; nó tách một token thành hai nhánh cùng hoạt động:

1. **Send Task “Gửi phân loại”** (`lab_send_segment`) có Implementation = External và Topic `lab-segment`. Engine tạo external task; Spring Boot worker fetch đúng topic, log và gọi Complete qua REST. `Async before=true` tạo một **job trước khi vào task**; job executor của Camunda tiếp tục token tới việc tạo external task. Mục đích của async là tạo ranh giới giao dịch và điểm retry, nên Start có thể trả ID trước khi worker log. Job executor của Camunda và external worker Spring Boot là hai thành phần khác nhau.

   Input mapping của send task là `requestType = ${requestedSegmentType}`: copy giá trị từ subprocess để worker thấy tên input dành cho task. Worker log `mappedInput=requestType`, đồng thời đọc `mockSegmentSuccess` và `mockSegmentType` để giả lập kết quả; nó trả Boolean `isSegmentEvaluatedSuccess` và String `segmentType`. Không có thuật toán phân loại thật. Output mapping `segmentWorkerResult = ${isSegmentEvaluatedSuccess}` sao chép Boolean đã trả để bạn quan sát mapping; gateway không đọc `segmentWorkerResult`.

   Extension properties của task là `demoFeature=send-task-external`, `demoOwner=segment-worker`. Worker yêu cầu `includeExtensionProperties`, log map này và ghi biến `labExtensionFeature=send-task-external`. Đây là ví dụ property **được code sử dụng**, khác hai property ở process. Execution listeners start/end ghi `labSendEvent=started/ended`. Documentation nói task chỉ log, complete và correlate message. Sau khi hoàn thành external task, worker tự gửi message `LabSegmentResult` nếu `mockAutoCorrelate=true`; nếu `false`, nó log đang chờ.

2. **Receive Task “Nhận kết quả”** (`lab_receive_segment`) có Message reference `Message_LabSegmentResult`; tên message dùng khi correlate là `LabSegmentResult`. Token đứng đây cho tới khi nhận đúng message của đúng `processInstanceId`. Nó không tự kiểm tra `mockSegmentSuccess`, không phải user task và không có `waitingTaskId` trong Tasklist. Documentation mô tả điểm chờ. Message tới thì token đi tiếp, không tạo biến kết quả mới; kết quả segment đã được worker trả ở nhánh send.

**Parallel Gateway `lab_segment_join`** có hai đường vào, phải đợi cả token send và token receive. Worker hoàn thành task nhưng chưa có message thì vẫn chưa sang Precheck; message tới nhưng worker chưa xong cũng vẫn đợi. End Event con `lab_segment_end` chỉ kết thúc subprocess, không phải toàn process. Khi đó output mapping của subprocess đưa kết quả ra gateway kế tiếp.

**Vì sao cần Input/Output mapping?** Trong `camunda:inputParameter`, `name` là tên biến **bên trong activity**, còn value có thể là literal hoặc expression đọc scope bên ngoài. Mapping chạy khi token **vào**. Trong `camunda:outputParameter`, `name` là biến cần ghi ở scope **bên ngoài**, còn value được lấy/tính từ activity khi token **rời**. Cơ chế này cho phép cùng một task dùng tên input `requestType` trong nhiều process, dù nơi gọi đặt biến gốc khác nhau. Ở lab, đường đi là `mockSegmentType → requestedSegmentType → requestType`; worker trả `isSegmentEvaluatedSuccess`, output mapping tạo thêm `segmentWorkerResult`, rồi subprocess đưa kết quả ra gateway. `segmentWorkerResult` là bản sao để quan sát, không phải biến điều kiện của gateway. Value có `${...}` đọc/tính expression; value như `REJECTED_SEGMENT` không có `${...}` là literal. History API có thể còn hiển thị dấu vết biến từ scope đã kết thúc, nên xem cùng `visitedActivities` để biết biến được tạo ở bước nào. [Input/output mapping](https://docs.camunda.org/manual/7.24/user-guide/process-engine/variables/#inputoutput-variable-mapping).

**Vì sao vừa External Task vừa Async?** External Task là công việc Camunda giao cho **worker ở ngoài engine**. Implementation `External` tạo task; Topic `lab-segment` là tên worker phải đăng ký để fetch. Nếu sửa Topic trên Modeler nhưng không sửa worker, task sẽ chờ mãi. Async before là một **ranh giới giao dịch bên trong engine**: Camunda lưu trạng thái, tạo job trước Send Task, rồi job executor tiếp tục. Nó cho điểm lưu/retry và có thể làm Start trả ID trước khi task xuất hiện. Async không tự xử lý external task; job executor Camunda khác worker Spring Boot. Flow gốc `cake-ob-fixed` dùng `camunda:class`, nghĩa là engine cần Java class trên classpath của chính engine. Lab dùng External vì Camunda Run và app chạy riêng. [External tasks](https://docs.camunda.org/manual/7.24/user-guide/process-engine/external-tasks/) và [async continuations](https://docs.camunda.org/manual/7.24/user-guide/process-engine/transactions-in-processes/#asynchronous-continuations).

**Vì sao có Receive Task và correlation?** BPMN message có ID `Message_LabSegmentResult` để file BPMN tham chiếu, còn name `LabSegmentResult` là giá trị runtime gửi tới Camunda Message Correlation API. App phải gửi đúng `messageName` và `processInstanceId` để đánh thức đúng execution đang chờ. Đây là cách một sự kiện bên ngoài báo “kết quả đã về”; nó không tự đọc biến rồi tiếp tục. Hai nhánh send/receive cùng phải xong trước join: chỉ có kết quả worker mà thiếu message thì chưa chạy Precheck. Message `LabSubmitted` gần cuối là message khác, không đánh thức receive task này.

Muốn nhìn receive task thực sự chờ, tạo **instance mới** với tự correlate tắt:

```bash
curl --location 'http://localhost:8086/api/v1/camunda-demo/lab/start' --header 'Content-Type: application/json' --data '{"autoCorrelate":false}'
```

Đợi worker log `Completed lab-segment`, rồi gọi Get status ở mục 6. Bạn sẽ thấy `outcome=RUNNING`, `visitedActivities` có `lab_receive_segment`, chưa có `lab_precheck` và chưa có `waitingTaskId`. Trong Cockpit, token đang ở receive task. Sau đó gửi message cho **ID của instance này**:

```bash
curl --location --request POST 'http://localhost:8086/api/v1/camunda-demo/lab/INSTANCE_ID/correlate-segment'
```

App gọi Camunda `/message` với `messageName=LabSegmentResult` và `processInstanceId=INSTANCE_ID`. Receive task kết thúc; khi nhánh send cũng xong, join cho token đi tiếp. Message `LabSubmitted` ở gần cuối flow là message **khác**, không thể dùng để đánh thức receive task này. Cần đúng tên và đúng instance để correlation thành công.

Khi hai nhánh đã hội tụ, output mapping của subprocess đưa kết quả tới gateway **Segment thành công?** ngay bên ngoài ô lớn.

### 3. Gateway “Segment thành công?” và end event phân loại thất bại

**Chọn Exclusive Gateway `lab_segment_ok`**, rồi chọn riêng đường **Yes**: condition của đường nối là `${isSegmentEvaluatedSuccess == true}`. Expression này trả Boolean; `==` là so sánh, không phải gán giá trị. Biến được đọc là kết quả worker đã qua output mapping của subprocess, **không phải** `mockSegmentSuccess` đầu vào. Đường **No** (`lab_segment_no`) là Default flow: khi Yes không đúng, engine đi No. Gateway không tự tạo biến.

**Gateway để làm gì?** Parallel Gateway ở mục 2 không đọc biến: fork tạo hai token, join đợi đủ hai token. Exclusive Gateway ở đây thì chọn **một** đường theo condition expression đặt trên **sequence flow**, không đặt trong worker. Worker chỉ tạo biến cho expression đọc. Default flow là đường dự phòng khi các condition khác không đúng, vì thế No không cần thêm `${isSegmentEvaluatedSuccess == false}`. `==` so sánh, còn một expression như `${execution.setVariable(...)}` ở listener **ghi biến**; cùng cú pháp `${...}` nhưng mục đích khác. Nếu tên biến trong condition sai hoặc không có, engine có thể lỗi hoặc đi khác dự kiến; kiểm tra biến worker trả và job/incident trong Cockpit khi sửa flow.

Thử nhánh thất bại bằng **instance mới**:

```bash
curl --location 'http://localhost:8086/api/v1/camunda-demo/lab/start' --header 'Content-Type: application/json' --data '{"segmentSuccess":false}'
```

Worker trả `isSegmentEvaluatedSuccess=false`. Sau khi send/receive cùng hoàn tất, gateway chọn No tới end event `lab_rejected_segment`. End event có input mapping literal `status=REJECTED_SEGMENT`; đây là chữ cố định, không phải expression đọc biến. Get status sẽ trả `outcome=lab_rejected_segment`, `variables.status=REJECTED_SEGMENT`; `visitedActivities` không có `lab_precheck` và không tạo user task.

Với giá trị mặc định `segmentSuccess=true`, đường Yes được chọn và token sang ô **Precheck** dưới đây.

### 4. Send Task “Precheck” và gateway “Precheck đạt?”

Chỉ instance có `isSegmentEvaluatedSuccess=true` mới tới Send Task `lab_precheck`. Nó có Implementation = External, Topic `lab-precheck`: worker app fetch theo topic, log và complete. Input mapping `requestPrecheck = ${mockPrecheckPassed}` chuẩn bị Boolean lúc vào task. Worker log `mappedInput=requestPrecheck`, đọc `mockPrecheckPassed` và trả Boolean `isPrecheckPassed`. Output mapping `workerResult = ${isPrecheckPassed}` sao chép kết quả để quan sát; gateway phía sau dùng `isPrecheckPassed`, không dùng `workerResult`.

Task này có `Async after=true`: sau khi task hoàn tất, Camunda tạo job **trước khi đi đường ra tới gateway**. Mục đích vẫn là ranh giới giao dịch/điểm retry, nhưng nằm **sau** task, khác Async before của send segment. Worker đã log `Completed lab-precheck` mà Get status chưa có end event thì có thể job executor còn đang tiếp tục. Extension property `demoFeature=async-after-and-output-mapping` được worker đọc và ghi vào `labExtensionFeature`; nó ghi đè giá trị `send-task-external` của bước trước. Execution listeners start/end ghi `labPrecheckEvent=started/ended`. Documentation mô tả input/output/async.

**So sánh hai ranh giới Async:** `Async before` ở send segment lưu trạng thái **trước khi task bắt đầu**; `Async after` ở Precheck lưu trạng thái **sau khi task xong nhưng trước khi lấy đường ra**. Chúng chia quá trình thành các giao dịch để engine có điểm retry khi job lỗi. Chúng không làm Spring Boot worker chạy nhanh hơn và không tự Complete external task. Vì vậy nếu worker đã log Complete mà gateway chưa chạy, hãy đợi job executor; nếu app worker tắt, external task vẫn chờ dù job executor hoạt động.

**Gateway `lab_precheck_ok`** có condition trên đường Yes `${isPrecheckPassed == true}`; đường No là Default flow. `true` đi tới gateway chấm điểm, `false` tới end event `lab_rejected_precheck`. Gateway không đọc `requestPrecheck`: đó là biến minh họa mapping vào task.

Thử Precheck không đạt bằng **instance mới**:

```bash
curl --location 'http://localhost:8086/api/v1/camunda-demo/lab/start' --header 'Content-Type: application/json' --data '{"precheckPassed":false}'
```

Segment vẫn đạt, worker Precheck trả `isPrecheckPassed=false`, gateway chọn No. End event gán literal `status=REJECTED_PRECHECK`. Get status cuối có `outcome=lab_rejected_precheck`, `variables.status=REJECTED_PRECHECK`; `visitedActivities` có `lab_precheck` nhưng không có `lab_manual_review`. Có thể tìm `requestPrecheck`, `workerResult`, `labPrecheckEvent` trong History.

Nếu `precheckPassed=true`, gateway Yes đưa token tới quyết định **Cần chấm điểm?**.

### 5. Gateway “Cần chấm điểm?”: ngoại mạng hoặc review

Gateway `lab_need_scoring` có Documentation ghi quy tắc ngoại mạng. Chọn đường **Ngoại mạng** để thấy condition `${segmentType == 'VTP_OFF_NET'}`: nó so sánh String **kết quả worker segment** với chữ cố định `VTP_OFF_NET`. Nếu đúng, engine tới end event `lab_skip_scoring`, input mapping đặt `status=SKIPPED_SCORING`. Nếu sai, đường **Có** (`lab_need_scoring_yes`) là Default flow và đi vào user task review. Default flow là đường dự phòng khi các condition khác không đúng, nên không cần viết một expression phủ định riêng.

Trên chính đường **Có** có execution listener event `take`: `${execution.setVariable('labRoute', 'scoring')}`. Đây là expression **gọi phương thức để ghi biến** lúc token đi qua đường nối, khác expression condition chỉ trả true/false. Vì thế `labRoute=scoring` xuất hiện ở nhánh review, không xuất hiện ở nhánh ngoại mạng.

**Condition, default và listener không thay thế nhau:** condition của đường Ngoại mạng trả true/false để engine **chọn** nhánh; đường Có là default nếu condition kia không đúng; listener `take` của đường Có chạy **sau khi nhánh đó được chọn** để ghi dấu đường đã đi. Khi `segmentType=VTP_ON_NET`, so sánh `${segmentType == 'VTP_OFF_NET'}` là false, nên chọn Có và mới ghi `labRoute=scoring`.

Thử ngoại mạng bằng **instance mới**:

```bash
curl --location 'http://localhost:8086/api/v1/camunda-demo/lab/start' --header 'Content-Type: application/json' --data '{"segmentType":"VTP_OFF_NET"}'
```

Worker trả `segmentType=VTP_OFF_NET`; sau khi Precheck đạt, gateway chọn Ngoại mạng. Get status cuối có `outcome=lab_skip_scoring`, `variables.status=SKIPPED_SCORING`. Flow kết thúc trước user task, nên không cần cURL Complete Review.

Với segment mặc định `VTP_ON_NET`, điều kiện Ngoại mạng không khớp; token đi đường Có tới **Review chấm điểm**.

### 6. User Task “Review chấm điểm”, message throw event và kết thúc

Với case mặc định ở mục 1, `segmentType=VTP_ON_NET` nên gateway chấm điểm chọn đường Có. **User Task `lab_manual_review`** có Assignee `demo`: Camunda tạo task cho người dùng `demo` trong Tasklist. Đây là điểm chờ **người/API hoàn tất**, khác external task do worker xử lý và receive task chờ message. Lab chưa tính điểm thật.

Input mapping của task đặt literal `reviewInstruction=Hoàn tất task để đi tới message throw event` khi vào task. Task listener event `create` chạy `${task.setVariable('labReviewEvent', 'created')}` lúc Camunda tạo task; `task` là đối tượng task hiện tại. Listener event `complete` đổi `labReviewEvent=completed` khi task được hoàn tất. Output mapping lúc rời task ghi `reviewCompleted` với literal `true` trong BPMN, nên giá trị hiện tại là **String `"true"`**, không phải Boolean; để tạo Boolean cần expression trả Boolean. Extension property `demoFeature=user-task-and-task-listeners` hiện chỉ là metadata của user task: external worker không fetch task này nên không đọc property. Documentation giải thích task/listener.

**Các cấu hình này phục vụ gì?** Assignee `demo` cho Tasklist biết task thuộc về ai, nhưng không tự làm review. Task listener khác execution listener: `create`/`complete` phản ứng với vòng đời **user task**, còn execution listener `start`/`end`/`take` phản ứng với process, activity hoặc đường nối. `labReviewEvent=created` chứng minh đã tới điểm chờ người; `completed` chứng minh người/API đã hoàn tất. Nếu instance kết thúc ở segment hoặc Precheck, user task chưa được tạo nên cả hai task listener không chạy. Extension property trên user task chỉ để minh họa ô Properties vì worker không đọc loại task này; Documentation cũng chỉ là ghi chú. Chữ `true` trong output mapping không có `${...}` là literal String, khác Boolean `true` do worker trả; điều này quan trọng nếu dùng biến đó làm condition về sau.

Để xem instance đang chờ tại đâu, import cURL này và thay ID từ response Start:

```bash
curl --location 'http://localhost:8086/api/v1/camunda-demo/lab/INSTANCE_ID'
```

Get status lấy `visitedActivities` từ Camunda History, `variables` từ History variable instances, và tra user task hiện hành để trả `waitingTaskId`/`waitingTaskName` nếu có. Khi đang chờ review, response có `outcome=RUNNING`, `waitingTaskName=Review chấm điểm`, `labReviewEvent=created`. Ví dụ rút gọn; response thật có thêm activity và biến, thứ tự activity có thể khác:

```json
{
  "processInstanceId": "<ID>",
  "outcome": "RUNNING",
  "visitedActivities": ["lab_start", "lab_segment", "...", "lab_precheck", "lab_manual_review"],
  "waitingTaskId": "<TASK_ID>",
  "waitingTaskName": "Review chấm điểm",
  "variables": {
    "isSegmentEvaluatedSuccess": true,
    "segmentType": "VTP_ON_NET",
    "isPrecheckPassed": true,
    "labReviewEvent": "created"
  }
}
```

Hoàn tất task bằng Tasklist hoặc cURL sau. URL cần **process instance ID**, không phải `waitingTaskId`; app tự tìm user task `lab_manual_review` thuộc instance đó:

```bash
curl --location --request POST 'http://localhost:8086/api/v1/camunda-demo/lab/INSTANCE_ID/complete-review'
```

App gọi Camunda Task Complete API; task listener `complete` và output mapping chạy. Token qua Intermediate Throw Event `lab_submit_message` có Message `LabSubmitted` và expression `${execution.setVariable('labMessageThrown', true)}`. Expression ghi Boolean chứng minh event đã chạy; lab không có process chờ nhận message `LabSubmitted`. Sau đó token tới end event `lab_ready_for_scoring`, input mapping ghi `status=READY_FOR_SCORING`; process listener end đổi `labLifecycle=ended`.

Response Complete Review hoặc lần Get status kế tiếp có `outcome=lab_ready_for_scoring`, `variables.status=READY_FOR_SCORING`, `labReviewEvent=completed`, `reviewCompleted="true"`, `labMessageThrown=true`, `labRoute=scoring`, `labLifecycle=ended` và `visitedActivities` có `lab_submit_message`. `waitingTaskId` biến mất. Nếu task đã hoàn tất hoặc instance ở nhánh khác, API báo không có review task.
