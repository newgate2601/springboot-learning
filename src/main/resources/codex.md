👉 Prompt = cách bạn điều khiển Codex
Prompt chính là “task description” bạn gửi cho Codex.
Ví dụ:

“Explain how the transform module works” → giống hỏi ChatGPT
“Add CLI option --json” → giống giao task cho dev junior

👉 Điểm khác biệt:

ChatGPT: chỉ trả lời
Codex: vừa suy nghĩ + vừa hành động (edit code, run command, đọc file...)

------------------------------------------------------------------------------------------------------------------------

👉 Agent loop: cách Codex hoạt động: Khi bạn gửi prompt, Codex không trả lời ngay 1 lần rồi xong, mà nó làm như này:
1. Hiểu yêu cầu
2. Quyết định cần làm gì
    - đọc file nào?
    - sửa file nào?
    - chạy command gì?
3. Thực hiện
4. Kiểm tra kết quả
5. Lặp lại cho đến khi xong

------------------------------------------------------------------------------------------------------------------------

👉 Cho Codex khả năng "tự verify":

Bug: API trả sai dữ liệu user
Repro:
1. Run: npm start
2. Call GET /users/1
3. Expected: name = "A"
4. Actual: name = null
Fix bug và đảm bảo: Test pass + Run lint

👉 Chia task nhỏ + nếu không biết chia thì hỏi Codex

❌ Không nên:Build full e-commerce system

✅ Nên: chia nhỏ task:
1. Create product API
2. Add validation
3. Write unit test
Lý do: Codex bị giới hạn context + Task nhỏ → dễ test → dễ review

👉 Prompt chuẩn nên có format:
Task: gì cần làm
Context:
- file nào liên quan
- business logic

Steps:
1. ...
2. ...

Validation:
- run test
- curl API
- expected output

------------------------------------------------------------------------------------------------------------------------

👉 Threads = phiên làm việc
❗ Không chạy 2 thread sửa cùng file vì sẽ conflict giống Git

Thread = toàn bộ:
+ Prompts của bạn
+ Hành động Codex đã làm
+ Kết quả

Hiểu đơn giản:
1 thread = 1 task đang chạy

👉 Trạng thái thread
+ Running: Codex đang làm việc
+ Paused: bạn dừng
+ Resume: làm tiếp

👉 Local thread:
Chạy trên máy bạn:
+ Đọc file local
+ Sửa code trực tiếp
+ Run command (npm, mvn, etc.)
+ Có sandbox để: tránh phá hệ thống ngoài project

👉 Cloud thread:
+ Chạy trên server:
+ Clone repo từ GitHub
+ Làm việc độc lập
+ Không ảnh hưởng máy bạn
Dùng khi:
+ Muốn chạy song song
+ Delegate task
+ Không muốn nặng máy

------------------------------------------------------------------------------------------------------------------------

👉 Context gồm:
+ File đang mở
+ Code trong repo
+ Output command
+ Lịch sử hành động

👉 Model có context window (giới hạn token) 
Codex xử lý khi quá giới hạn thì sẽ Context compaction: tóm tắt (summarize) + Bỏ thông tin ít quan trọng.
Sau nhiều lần tóm tắt: Có thể mất chi tiết + Hiểu sai logic --> nên Chia task nhỏ + context quan trọng thì nhắc lại.

------------------------------------------------------------------------------------------------------------------------

👉 AGENTS.md – một file cực kỳ quan trọng nếu bạn dùng Codex như một “AI dev teammate lâu dài”
AGENTS.md = “rule book” cho Codex trong project của bạn
+ Nó là file markdown nằm trong repo
+ Codex sẽ đọc file này trước khi bắt đầu làm việc
+ Nó giúp Codex hiểu cách team bạn code, build, review
+ Nên viết Ngắn, Rule rõ ràng, Chỉ chứa thứ hay bị sai / quan trọng

👉 AGENTS.md không phải tất cả: Bạn nên kết hợp:
+ pre-commit hooks
+ eslint / checkstyle
+ type checker
+ test automation

💡 Ý nghĩa:
AGENTS.md = hướng dẫn
tooling = enforcement

👉 Dùng AGENTS.md để làm gì? Bạn dùng nó để ép Codex làm đúng “chuẩn team” mỗi lần chạy.
✅ Build + test rules: Codex sẽ tự biết cách build, test mà không cần bạn nhắc lại mỗi lần

                                                ## Build & Test
                                                - Use: ./mvnw clean install
                                                - Run tests before committing
                                                - Do not skip integration tests

✅ Coding convention: 

                                                ## Conventions
                                                - Use Lombok for DTOs
                                                - Do not use flag parameters
                                                - Follow existing package structure

✅ Review expectations: Codex sẽ code theo mindset reviewer luôn

                                                ## Code Review
                                                - Avoid N+1 queries
                                                - Use pagination for list APIs
                                                - Validate input with @Valid

✅ Directory guidance: Tránh việc Codex “đoán sai” structure project

                                                ## Project Structure
                                                - Controllers: /controller
                                                - Services: /service
                                                - Repositories: /repository
                                                - DTOs: /dto

👉 Tư duy quan trọng: Feedback loop, đây là phần hay nhất: Khi Codex làm sai → bạn sửa → bắt Codex update AGENTS.md
Ví dụ thực tế:

Codex làm sai:
❌ Dùng raw SQL thay vì JPA
❌ Tạo DTO không đúng format
❌ Query thiếu pagination
Bạn nói: "Đừng dùng native query, luôn dùng JPA Repository"
Sau đó: "Update AGENTS.md để rule này áp dụng cho future tasks"
✔️ Từ lần sau: Codex không lặp lại lỗi nữa

👉 Khi nào nên update AGENTS.md?
🔁 1. Lỗi lặp lại Codex sai cùng 1 kiểu nhiều lần → thêm rule
📚 2. Đọc quá nhiều file (waste context): Codex scan cả repo lung tung → thêm hướng dẫn:

                                            ## File Reading Priority
                                            - Start from /service and /repository
                                            - Ignore /docs unless explicitly needed

🔁 3. PR feedback lặp lại: Bạn review code và luôn comment kiểu: “thiếu validate”, “thiếu log”, “naming chưa chuẩn”
→ đưa hết vào AGENTS.md

👉 Ví dụ mini chuẩn cho bạn (Spring Boot)

                                            ## Build
                                            - Use ./mvnw clean install
                                            
                                            ## Conventions
                                            - Use Lombok for DTOs
                                            - Do not use flag parameters
                                            - Use MapStruct for mapping
                                            
                                            ## Database
                                            - Use Spring Data JPA
                                            - Avoid native queries unless necessary
                                            
                                            ## API
                                            - Always validate input with @Valid
                                            - Use pagination for list endpoints
                                            
                                            ## Structure
                                            - controller -> service -> repository

👉 Codex load và merge AGENTS.md tức là cơ chế “đọc rule” phía sau. Hiểu được đoạn này thì bạn sẽ biết viết AGENTS.md
đúng cách để điều khiển Codex rất chính xác.
Codex không chỉ đọc 1 file AGENTS.md mà nó có thể đọc nhiều file AGENTS.md trong repo, sau đó merge lại thành 1 “rule book” duy nhất.
Bao gồm:
+ Global (máy bạn)
+ Repo
+ Sub-folder (thư mục con)
+ https://developers.openai.com/codex/guides/agents-md
+ https://developers.openai.com/codex/guides/agents-md
+ 
