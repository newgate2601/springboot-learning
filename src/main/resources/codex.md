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

Thứ tự ưu tiên (rất quan trọng): Codex load theo thứ tự:
1. Global (máy bạn): Nó sẽ đọc: AGENTS.override.md (nếu có) >< nếu không có → AGENTS.md. Chỉ lấy 1 file duy nhất ở level này
2. Project (repo của bạn): bắt đầu từ project root (git root), đi dần xuống current working directory

                                           repo/
                                           ├── AGENTS.md
                                           ├── service/
                                           │    ├── AGENTS.md
                                           │    └── payment/
                                           │         └── AGENTS.md   ← bạn đang đứng ở đây

Codex sẽ đọc theo thứ tự: repo/AGENTS.md --> repo/service/AGENTS.md --> repo/service/payment/AGENTS.md 
3. Cách merge: sau khi đọc xong thì nối tất cả file lại thành 1 prompt duy nhất theo thứ tự Global → Root → Sub-folder. 
Nếu có rule nào trùng nhau thì rule ở file nào đọc sau sẽ override rule trước (theo thứ tự trên)
==> File càng gần chỗ bạn đang làm → càng “override” mạnh

👉 Khi bạn chạy Codex (CLI hoặc session mới): Thì ngay lúc đó Codex sẽ scan + đọc + merge toàn bộ AGENTS.md
→ tạo thành 1 prompt cố định ban đầu → AGENTS.md trở thành: SYSTEM CONTEXT (cố định cho session) + KHÔNG được reload lại tự động nữa
Codex KHÔNG: re-read AGENTS.md mỗi lần bạn gửi prompt/ auto detect file changes/ hot reload instruction --> cần tạo sesssion mới.

------------------------------------------------------------------------------------------------------------------------

👉 Skill = một workflow (quy trình làm việc) được đóng gói sẵn để tái sử dụng
Prompt bình thường, dùng 1 lần, prompt dài, tốn context, lặp lại, dễ sai do thiếu bước
Skill dùng lại nhiều lần, có cấu trúc rõ ràng, tái sử dụng workflow chuẩn, không cần nhồi context mỗi lần
Codex không cần bạn gọi skill, nó có thể tự discover và dùng
✅ Skills + MCP (rất quan trọng), nếu workflow cần jira/ kibana/ database,....
Skill + MCP = automation mạnh (Skill: “Create Jira ticket from bug” + MCP: kết nối Jira API)

👉 Skills hoạt động như thế nào?
Codex KHÔNG load full skill ngay từ đầu.
1. Load metadata nhẹ của tất cả skill (tên, mô tả, khi nào dùng, bước chính)
2. Khi bạn đưa prompt, Codex sẽ phân tích xem có skill nào phù hợp không để tự dùng
3. Nếu có skill phù hợp → Codex sẽ load full instruction + script + reference của skill đó để làm việc
4. Nếu không có skill nào phù hợp → Codex sẽ làm theo cách bình thường (không có skill)

👉 Cấu trúc của một Skill

                                        my-skill/
                                        ├── SKILL.md        # bắt buộc
                                        ├── scripts/        # optional
                                        ├── references/     # optional
                                        └── assets/         # optional

+ SKILL.md (quan trọng nhất) gồm: Metadata (mô tả skill) + Instructions (cách làm việc)

                                        # Skill: Fix Bug Workflow
                                        
                                        ## When to use
                                        Use this skill when a bug is reported with reproducible steps.
                                        
                                        ## Steps
                                        1. Reproduce bug
                                        2. Identify root cause
                                        3. Write failing test
                                        4. Fix bug
                                        5. Run tests
                                        6. Summarize fix    

+ scripts/: chứa script để tự động hóa 1 phần workflow
Codex có thể: gọi script, chạy CLI, tự động hóa workflow.

                                          scripts/
                                          ├── run-tests.sh
                                          ├── seed-data.sh

+ references/: Tài liệu tham khảo: coding convention + API docs + design docs (tài liệu để hiểu domain) --> codex sẽ hiểu context sâu hơn.
+ assets/: chứa file mẫu, template, config để Codex dùng làm reference khi làm việc.

------------------------------------------------------------------------------------------------------------------------

👉 [MCP](https://developers.openai.com/codex/mcp) trong Codex thực chất là “cầu nối chuẩn hoá” để agent (Codex) nói
chuyện với thế giới bên ngoài (tool/ server)
MCP = chuẩn giao tiếp giữa AI agent ↔ tools/context
Tool được mô tả theo dạng AI-readable (schema + description)
AI có thể tự chọn tool để gọi (không cần dev hardcode flow)

+ Codex (Host) = bộ não quyết định làm gì
+ MCP Client (trong Codex) = adapter giao tiếp giữa Codex và external tool
+ MCP Server = hệ thống bên ngoài (GitHub, Figma, DB, internal API...)
MCP định nghĩa cách 3 thằng này nói chuyện với nhau theo chuẩn chung, thay vì mỗi tool một kiểu.

👉 Hai loại MCP server (rất quan trọng)
STDIO server (local process): Chạy như một process local: Codex <--> stdin/stdout <--> Node process <--> API
+ Setup nhanh
+ Chạy local
+ Không cần deploy server

HTTP server (remote): Codex <--> HTTP request <--> Remote server <--> API
+ scalable
+ team share được
+ auth chuẩn (OAuth, token)

👉 Codex dùng MCP như thế nào (flow runtime)
1. Load tool metadata (Giống OpenAI function calling): Khi start, Codex sẽ connect tới MCP server để lấy metadata của tool (tên, mô tả, input schema)
→ giúp Codex hiểu tool có thể làm gì + khi nào dùng

                                           Khi start:
                                        Codex đọc config.toml
                                        connect tới MCP servers
                                        lấy về:
                                        {
                                        "tools": [
                                            {
                                            "name": "search_docs",
                                            "description": "Search developer docs",
                                            "input_schema": {...}
                                            }
                                            ]
                                        }

2. User prompt: Bạn gửi prompt bình thường, Codex sẽ phân tích xem có tool nào phù hợp để gọi không dựa trên metadata đã load
3. Tool call: Nếu có tool phù hợp, Codex sẽ tự động gọi tool đó bằng cách gửi input theo đúng schema mà MCP server yêu cầu
4. Tool execution: MCP server nhận request, thực hiện hành động (gọi API, query DB, etc.) và trả về kết quả cho Codex
5. Codex tiếp tục workflow: Sau khi nhận kết quả từ tool, Codex sẽ tiếp tục xử lý theo logic của prompt, 
có thể là phân tích kết quả, sửa code, hoặc gọi tool khác nếu cần.
                                        

