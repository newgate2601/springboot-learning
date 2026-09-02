# Java Core

## Java Component

Java là ngôn ngữ lập trình kiểu tĩnh, hướng đối tượng và chạy chủ yếu trên JVM (Java Virtual Machine). Java thường được sử dụng để xây dựng backend, hệ thống doanh nghiệp, hệ thống xử lý giao dịch và các ứng dụng cần duy trì trong nhiều năm.

### Đặc điểm chính của Java

- **Statically typed:** kiểu dữ liệu được kiểm tra khi biên dịch. Nhiều lỗi như truyền sai kiểu tham số hoặc trả về sai kiểu có thể được phát hiện trước khi chạy chương trình.
- **Object-oriented:** hỗ trợ class, object, encapsulation, inheritance, polymorphism và abstraction. Java cũng hỗ trợ functional programming ở mức độ nhất định thông qua lambda, Stream API và functional interface.
- **Cross-platform:** source code được biên dịch thành bytecode. Bytecode có thể chạy trên hệ điều hành có JVM tương thích. Khả năng này không có nghĩa mọi chương trình Java tự động chạy đúng trên mọi hệ điều hành; code vẫn có thể phụ thuộc đường dẫn file, native library, encoding hoặc API riêng của hệ điều hành.
- **Automatic memory management:** JVM dùng Garbage Collector để thu hồi các object không còn được tham chiếu. Lập trình viên không phải giải phóng bộ nhớ thủ công, nhưng vẫn có thể gây memory leak nếu chương trình tiếp tục giữ tham chiếu đến dữ liệu không còn cần thiết.
- **Concurrent programming:** Java có `Thread`, `ExecutorService`, `CompletableFuture`, concurrent collections, lock, atomic type và virtual thread. Virtual thread phù hợp với server có nhiều tác vụ đồng thời thường xuyên chờ I/O; nó giúp tăng throughput chứ không làm một tác vụ đơn lẻ chạy nhanh hơn.
- **Hệ sinh thái lớn:** Java có Spring, Jakarta EE, Hibernate, Maven, Gradle và nhiều thư viện đã được sử dụng lâu năm trong hệ thống production.
- **Khả năng quan sát và vận hành tốt:** JVM cung cấp nhiều công cụ để xem thread, heap, Garbage Collector và hiệu năng, chẳng hạn JFR, `jcmd`, `jstack` và `jmap`.

### Điểm mạnh đáng chú ý của Java

Chỉ xét những điểm tạo khác biệt tương đối rõ:

- **Hệ sinh thái JVM và Spring đồng bộ:** Spring cung cấp một hệ thống tương đối thống nhất cho database, transaction, security, cache, message broker, metrics và cấu hình. Ngôn ngữ khác cũng có các thư viện tương ứng, nhưng không phải hệ sinh thái nào cũng tích hợp các phần này theo cùng một cách.
- **Tận dụng tài sản JVM đã có:** Java có thể dùng trực tiếp lượng lớn thư viện JAR, SDK và công cụ JVM. Kotlin, Scala và Clojure cũng có thể dùng phần lớn thư viện Java.
- **Thuận lợi khi nâng cấp hệ thống Java hiện tại:** có thể nâng JDK, thay framework hoặc áp dụng virtual thread mà không phải viết lại toàn bộ code Java và thư viện đang dùng.
- **Công cụ phân tích JVM sâu và thống nhất:** JFR, thread dump, heap dump và `jcmd` hỗ trợ phân tích thread, lock, bộ nhớ, Garbage Collector và hiệu năng trên cùng runtime.

Điểm mạnh lớn nhất của Java thường xuất hiện khi doanh nghiệp **đã dùng JVM, Spring hoặc có nhiều thư viện Java**. Nếu bắt đầu một dự án hoàn toàn mới và không có các điều kiện này, lợi thế của Java so với C#, Go hoặc TypeScript sẽ nhỏ hơn.

### Điểm yếu đáng chú ý của Java

- **Tốn RAM và khởi động chậm hơn các chương trình native nhỏ:** JVM và framework như Spring cần thời gian khởi tạo và bộ nhớ riêng. Bất lợi này dễ thấy ở CLI, serverless function và microservice rất nhỏ.
- **Code thường dài hơn Kotlin, Python hoặc TypeScript:** Java cần nhiều khai báo type, class và cấu trúc hơn. Với chức năng nhỏ, lượng code bổ sung có thể không đem lại lợi ích tương xứng.
- **Garbage Collector làm giảm khả năng kiểm soát thời điểm thu hồi bộ nhớ:** phù hợp với phần lớn backend nhưng không lý tưởng cho hệ thống cần kiểm soát bộ nhớ hoặc latency cực kỳ chặt như driver, embedded hay một số thành phần real-time. C, C++ và Rust phù hợp hơn cho nhóm bài toán này.
- **Framework enterprise có độ phức tạp cao:** Spring hỗ trợ nhiều chức năng nhưng người mới phải hiểu dependency injection, bean lifecycle, transaction proxy và cấu hình framework. Dùng Spring đầy đủ cho service quá nhỏ có thể làm dự án nặng hơn cần thiết.
- **Không có lợi thế ở một số lĩnh vực:** frontend web ưu tiên JavaScript/TypeScript; AI và data science ưu tiên Python; systems programming thường ưu tiên C/C++ hoặc Rust; CLI và cloud tool nhỏ thường thuận tiện với Go.

### Khi nào nên dùng Java?

Nên ưu tiên Java khi:

1. doanh nghiệp đã có đội ngũ, thư viện, pipeline và hệ thống giám sát JVM;
2. dự án cần dùng trực tiếp SDK hoặc thư viện Java hiện có;
3. backend cần nhiều thành phần Spring hoạt động cùng nhau;
4. đang mở rộng hoặc hiện đại hóa một hệ thống Java cũ;
5. chi phí chuyển sang ngôn ngữ khác lớn hơn lợi ích nhận được.

Ví dụ: công ty đã có hàng chục service Spring Boot, thư viện xác thực chung và dashboard JVM. Viết service mới bằng Java giúp dùng lại toàn bộ nền tảng. Chọn Go chỉ để giảm RAM có thể kéo theo việc phải xây lại thư viện, pipeline và năng lực trực production.

### Khi nào không nên ưu tiên Java?

- CLI, script hoặc automation nhỏ cần khởi động nhanh và phân phối gọn;
- serverless function ngắn, nhạy với cold start và bộ nhớ;
- frontend chạy trong trình duyệt;
- AI, notebook và thử nghiệm dữ liệu;
- embedded, driver hoặc phần mềm cần kiểm soát bộ nhớ rất chặt;
- dự án nhỏ trong khi đội ngũ và hạ tầng hiện tại đã chuẩn hóa tốt trên ngôn ngữ khác.

### Kết luận

Java không hơn rõ rệt chỉ vì có OOP, kiểu tĩnh, Garbage Collector, concurrency hoặc khả năng viết backend; nhiều ngôn ngữ khác cũng có các đặc điểm đó.

Khác biệt đáng cân nhắc nhất của Java là **hệ sinh thái JVM/Spring và khả năng tận dụng nền tảng Java đã tồn tại**. Nếu không cần hai lợi thế này, nên chọn ngôn ngữ dựa trên bài toán và kỹ năng đội ngũ thay vì mặc định chọn Java.

## Java setup environment
- Thiết lập các biến môi trường cho Java là cần thiết để giúp hệ thống xác định vị trí các công cụ chạy chương trình Java

JAVA_HOME sẽ trỏ tới thư mục, nơi JDK của máy (vd: C:\\Program Files\\Java\\jdk-17)  
  
PATH sẽ trỏ tới bin của JDK (vd: C:\\Program Files\\Java\\jdk-17\\bin), đúng hơn là trỏ tới thư mục chứa các chương trình thực thi (kết thúc bằng .exe) để giúp hđh tìm thấy các tệp thực thi để có thể chạy các lệnh Java như java, javac, jar,... nhanh hơn, gọn hơn mà không cần cung cấp đường dẫn đầy đủ.
- Nếu không thiết lập biến môi trường PATH thì mỗi lần compile, interpret chương trình Java thì phải cung cấp đường dẫn trỏ tới javac.exe, java.exe nằm trong bin của JDK để có thể chạy được chương trình. 

  
CLASSPATH sẽ trỏ tới jar/ zip file để có thể chạy chương trình.  
## Why java multi-platform
![Hình minh họa từ tài liệu gốc](images/why-java-multiplatform.png)  
- Platform khi chạy chương trình gồm cả kiến trúc CPU và hệ điều hành. Mỗi kiến trúc CPU có tập lệnh máy riêng; hệ điều hành cũng có ABI, system call và thư viện hệ thống riêng.
- Java tạo một lớp trung gian là **JVM bytecode**. File `.class` không chứa mã máy dành riêng cho Windows, Linux hay một kiến trúc CPU cụ thể. JVM tương thích trên từng platform chịu trách nhiệm tải, kiểm tra và thực thi bytecode đó.
- Vì vậy, cùng một file `.class` hoặc `.jar` thường có thể chạy trên nhiều platform mà không cần compile lại, miễn là platform có JVM tương thích và chương trình không phụ thuộc native library hay tài nguyên riêng của hệ điều hành.
- Cách nói chính xác không phải lúc nào Java cũng chỉ đi qua hai bước `compile -> interpret`. Khi chạy, JVM hiện đại thường kết hợp **interpreter** và **JIT compiler**.

![Hình minh họa từ tài liệu gốc](images/java-execution-flow.png)  
**Java compile flow — xảy ra khi build:**
- Java compiler (thường là `javac`, thuộc JDK) biên dịch source code `.java` thành JVM bytecode trong các file `.class`; nó không tạo trực tiếp mã máy cho một CPU cụ thể.
- Ví dụ, `javac HelloWorld.java` tạo ra `HelloWorld.class`. Với dự án Maven/Spring Boot, các file `.class` thường nằm trong `target/classes`; Gradle thường đặt chúng trong `build/classes`.
- Khi nhấn **Run** trong IDE, IDE thường compile các source đã thay đổi rồi mới khởi động JVM. Nếu file `.class` đã mới nhất, bước compile có thể được bỏ qua. Chạy lại chương trình vì thế không đồng nghĩa lúc nào cũng compile lại toàn bộ source code.
- Bytecode sinh ra không phụ thuộc trực tiếp vào hệ điều hành đang compile. Tuy nhiên, phiên bản JDK, compiler option hoặc compiler implementation khác nhau có thể tạo nội dung `.class` khác nhau dù dùng cùng source code.

**Java runtime flow — xảy ra khi JVM chạy chương trình:**
1. Lệnh `java HelloWorld` khởi động JVM; class loader tải các class cần thiết và JVM kiểm tra tính hợp lệ của bytecode.
2. Ban đầu, JVM có thể dùng **interpreter** để thực thi từng bytecode instruction. Interpreter thực hiện hành vi của bytecode ngay tại runtime; không nên mô tả đơn giản rằng nó luôn tạo và lưu một file machine code tương ứng.
3. JVM theo dõi quá trình chạy. Khi một method hoặc đoạn code được thực thi nhiều và trở thành *hot code*, **JIT compiler** biên dịch đoạn bytecode đó thành native machine code dành cho CPU/platform hiện tại.
4. Native code do JIT tạo được giữ trong vùng nhớ gọi là **code cache** của tiến trình JVM và được tái sử dụng trong lần chạy hiện tại. Thông thường nó không được lưu thành executable để dùng lại sau khi tắt rồi mở ứng dụng.
5. JVM vẫn có thể tiếp tục interpret phần code ít được sử dụng, đồng thời JIT compile và tối ưu phần code nóng. Vì vậy interpreter và JIT có thể cùng tham gia trong một lần chạy.

JVM không dịch toàn bộ ứng dụng sang machine code ngay khi khởi động và cũng không xử lý trực tiếp source `.java`. JVM tải, kiểm tra file `.class`, rồi thực thi bytecode theo luồng chạy thực tế. Một method thường chỉ được interpreter thực thi khi được gọi; nếu nó trở thành *hot code*, JIT sẽ compile nó thành native machine code cho những lần thực thi tiếp theo. Method hoặc nhánh không được chạy thì không cần được interpret hay JIT compile.

```text
Build time: .java --javac--> .class (JVM bytecode)
Runtime:    .class --class loader/verifier--> interpreter
                                           \-> JIT --> native machine code --> CPU
```

Với ngôn ngữ biên dịch ahead-of-time như C/C++, compiler thường tạo native executable cho một CPU, hệ điều hành và ABI mục tiêu. Chương trình đã được build đúng platform có thể chạy nhiều lần mà **không phải compile lại mỗi lần run**; chỉ cần build lại khi source thay đổi hoặc khi cần binary cho platform mục tiêu khác. Java chủ yếu phân phối bytecode dùng chung và đặt phần thích nghi với platform vào JVM tương ứng. Java cũng có thể dùng AOT/native-image trong một số trường hợp, nên đây là mô hình thực thi phổ biến chứ không phải quy tắc duy nhất.

JDK - Java Development kit: JDK là 1 dạng SDK (Software Development kit)  của Java, là môi trường phát triển phần mềm được sử dụng để phát triển các ứng dụng Java
- JDK cung cấp JVM/runtime cùng các công cụ phát triển như Java compiler (`javac`), Java launcher (`java`), document generator (`javadoc`), archiver (`jar`), debugger và monitoring tools. Lệnh `java` là launcher dùng để khởi động JVM, không phải tên của interpreter.  
- JIT (Just-in-time Compiler) biên dịch các phần bytecode được thực thi nhiều thành native machine code và giữ chúng trong code cache của tiến trình JVM để tăng tốc lần chạy hiện tại.  
- .jar - Java ARchive là 1 định dạng lưu trữ, tệp nén zip đặc biệt, sử dụng để đóng gói nhiều tệp .class (bytecode), libraries, các tệp cấu hình tài nguyên cần thiết (image, properties,...) thành 1 tệp duy nhất để giúp phân phối ứng dụng gọn gàng, dễ dàng hơn (zip mọi thứ cần để chạy Java application).  
- jar giúp dễ dàng quản lý, phân phối các ứng dụng/ java lib + giúp deploy dễ dàng hơn chỉ với 1 file .jar  
- Mục đích sử dụng là để đóng gói ứng dụng vì jar chứa toàn bộ tệp .class của Java nên để triển khai thì chỉ cần triển khai 1 tệp .jar thay vì triển khai từng tệp .class  
- jar là 1 tệp nén định dạng tương tự .zip

JRE - Java Runtime environment = JVM + Libraries (java.lang) + support file (properties, resource): rõ ràng cái tên nói lên tất cả, sử dụng với mục đích chính là để chạy java application
- Về cơ bản, nếu chỉ muốn chạy một Java application đã được build thì cần Java runtime tương thích và bytecode; không cần compiler. JVM có thể dùng cả interpreter và JIT compiler để thực thi bytecode.   
  → nếu không cần viết code lẫn compile code mà chỉ cần run Java application khi đã có sẵn application được viết sẵn rồi thì chỉ cần JRE.

JVM - Java Virtual machine là 1 máy ảo sử dụng để chạy các java application dựa vào bytecode sau khi source code được biên dịch bởi Compiler
- 2 chức năng chính của JVM là: giúp java có thể chạy trên bất kỳ HĐH nào mà không cần thay đổi mã nguồn + quản lý, tối ưu hóa sử dụng bộ nhớ của chương trình  
- Mỗi hđh sẽ có JVM riêng để bytecode chuyển thành machine code phù hợp với hđh đó.  
- JVM được thiết kế để tạo ra 1 abstraction layer với HĐH, Hardware bên dưới giúp bytecode không cần phải thay đổi để đáp ứng với từng HĐH, khi chạy trên Window thì dùng Window OS API và POSIX OS API khi chạy trên Linux/ mac OS   
- Khi chạy chương trình, JVM sẽ call tới main method có trong code  
- JVM giúp quản lý quản lý bộ nhớ (garbage collection) giúp tự động giải phóng bộ nhớ mà chương trình không còn sử dụng, giảm leak memory + kiểm soát Heap, Stack + quản lý các threads để không gây xung đột khi run  
- Không thể cài riêng lẻ, nằm trong JRE.  
- JVM sử dụng Class Loader (lazy load + caching) + metaspace (permgen) + Executive engine
## JVM memory
- Method Area: là khu vực lưu trữ thông tin về class: thông tin về Superclass của nó, thông tin về modifier, variables, methods của class này,... của toàn bộ class sử dụng trong chương trình này (ở Class Level)  
- Heap: lưu trữ thông tin về các đối tượng, sử dụng toàn cục  
- Stack: mỗi 1 method call sẽ tạo 1 stack sử dụng để lưu trữ local variable, sử dụng cục bộ  
- PC Registers: lưu trữ địa chỉ của các lệnh thực thi hiện tại của 1 luồng + 1 luồng sẽ có 1 PC Register riêng  
- Native Method Stacks: đối với 1 luồng, 1 Native Method Stack được tạo ra để lưu trữ thông tin về phương thức gốc.
## JVM flow
Trước tiên là JVM sẽ chia vùng nhớ mà nó quản lý thành nhiều vùng dữ liệu khác nhau + mỗi vùng nhớ sẽ có vai trò riêng (stack, heap, method area,...)
- Method Area sử dụng để lưu trữ thông tin các class 

Sau khi compile các tệp .java thành các tệp .class chứa bytecode, tệp .class này sẽ trải qua nhiều bước như hình bên dưới:  
![Hình minh họa từ tài liệu gốc](images/jvm-architecture.png)  
Class Loader là 1 thành phần thuộc bộ JVM, chịu trách nhiệm tải các .class file vào RAM của JVM khi run chương trình + Class Loader sẽ chịu cách trách nhiệm như:  
  
**B1**: Class Loader sẽ đọc các .class file, lưu vào Method Area gồm các thông tin: tên của class, thông tin về Superclass của nó, thông tin về modifier, variables, methods của class này.
- Với mỗi .class file thì JVM sẽ lưu trữ các thông tin bằng cách: JVM sẽ tạo 1 object thuộc loại java.lang.Class để đại diện cho file này và lưu vào Heap + tại runtime sẽ sử dụng các thông tin này để có thể thực thi chương trình (qua các method getMethod(), getFields(),...)  
- Với ví dụ trên thì JVM sẽ tạo 1 object là Class<Demo> đại diện cho file này và lưu vào Heap, chứa các metadata về class này để sử dụng trong tương lai.

**B2**: JVM xác minh các bytecode về cấu trúc, định dạng xem có hợp lệ không  
**B3**: Khởi tạo giá trị cho static variable + gắn với giá trị trong static block (nếu có)

## Stack
- Khi mà chạy chương trình, JVM sẽ yêu cầu hđh cấp phát cho 1 vùng RAM để dùng cho việc chạy chương trình + để run application một cách tối ưu nhất, JVM chia vùng RAM này thành Stack và Heap  
- Khi declare variables/ objects/ call method,... thì JVM sẽ assign memory cho các hoạt động này từ Stack/ Heap.

Stack memory sử dụng để allocate (cấp phát) static memory (bộ nhớ tĩnh) 
- Stack bao gồm Primitive value nằm trong 1 method đang được call   
- 1 thread = 1 stack = n stack frame (mỗi method được call tạo 1 stack frame).

(Các Primitive value + các references tới object của method nằm trong Heap)
- Stack này sử dụng cơ chế LIFO (Last in first out) order tức là khi có new method được gọi, 1 new block được tạo ra và đặt lên đầu Stack + khi finish method thì block trên đầu Stack này sẽ bị remove đi và quay lại called method => lúc này các variables, references sẽ bị remove theo => auto allocated, deallocated khi finish method => tiết kiệm memory + auto memory management (Block này sẽ gồm các Primitive value + các references)  
- Việc access Stack sẽ nhanh hơn so với Heap (cụ thể là hàng trăm -> ngàn lần)  
- Stack là Thread-safe do mỗi method work trên cùng 1 Stack  
- Stack thì limit-size và size của Stack nhỏ hơn so với Heap  
- Khi số lượng memory của tổng các Stacks bị quá giới hạn memory, throw StackOverFlowException (thường là do gọi đệ quy vô hạn method) + Stack không thể resize được (static memory)  
- Kích thước của Stack là cố định, bất kể có ít hay nhiều biến sử dụng trên Stack, nó cũng giúp việc cấp phát, giải phóng bộ nhớ nhanh chóng, không cần quản lý phức tạp.  
  ![Hình minh họa từ tài liệu gốc](images/thread-stack-heap.png)
## Heap
Heap space sử dụng để allocated dynamic memory, có thể grow + shrink => flexible  
- Khi object mới được tạo, nó sẽ được tạo trong Heap + references tới object này thì nằm trong Stack  
- object nằm trong Heap thì là Global access, có thể access từ bất kì đâu trong application  
=> Non-safe Thread vì bất cứ thread nào cũng có thể access vào cùng 1 object trong Heap  
- Nếu Heap full thì application throw OutOfMemory  
- Heap không thể tự deallocated được do đó mới sinh ra khái niệm Young, Old, Permanent Generation + Heap phải cần Garbage Collector để deallocated những object không sử dụng trong 1 khoảng thời gian nhất định để optimize memory + vì việc deallocating Heap khó hơn so với Stack nên không thể auto như Stack được (Stack chỉ là add/ remove top of Stack)  
Memory leak tức memory không được deallocated sau khi hoàn thành function => memory sẽ đầy dần => OutOfMemory

  
![Hình minh họa từ tài liệu gốc](images/stack-heap-object.png)  
(Giả sử buildPerson thay trực tiếp thành constructor)  
**B1** Khi vào main(), Stack của main() được tạo + Stack của main() lúc này bao gồm
- Primitive value của int “id” = 23  
- References “person” được tạo trên Stack, pointer tới object nằm trong Heap

**B2** Khi call tới constructor của Person từ main() -> tạo 1 Stack của constructor đặt trên đầu của Stack trước đó (đặt lên đầu của main() Stack) + Stack của constructor lúc này bao gồm
- this point tới person object trong Heap  
- Primitive value của int “id” = 23  
- String name point tới actual string nằm trong String Pool (Heap)

**B3** Sau khi call thành công constructor, remove Stack của constructor này  
**B4** Sau khi constructor thành công thì method main() cũng hết, remove Stack của main() 

Why Stack faster than Heap
- Java sử dụng Multi-thread, Stack là thread-safe nên kiểm soát dễ dàng, nhanh chóng >< Heap là global, cần các cơ chế đồng bộ hóa nên làm giảm hiệu năng.  
- Khi truy cập biến, CPU chỉ cần sử dụng con trỏ Stack Pointer để lấy giá trị >< Heap phải truy cập giá trị trong Stack, sau đó lại mới tìm đối tượng tương ứng trong Heap, ngoài ra nếu Heap bị phân mảnh thì quá trình này diễn ra lâu hơn.  
- Stack cấp phát trong các vùng nhớ đã sẵn của Stack >< Heap phải tìm vùng nhớ đủ để chứa giá trị đối tượng.
## Variable
Variable - biến, sử dụng để lưu trữ giá trị dữ liệu
- Mỗi biến đều có: kiểu dữ liệu + tên biến + giá trị

Variable Name là tên được đặt cho 1 vị trí bộ nhớ
- Tên biến gồm: A-Z, a-z, 0-9, (_) và ($)  
- Ký tự đầu tiên không được là số + biến không sử dụng khoảng trắng + tên biến phân biệt hoa thường + không giới hạn độ dài (khuyến khích 4-15)

Local Variable là biến được định nghĩa trong block/ method/ constructor
- Biến cục bộ được sử dụng, chỉ sử dụng được trong phạm vi trên, sau khi kết thúc hàm thì sẽ tự động dellocated.  
- Việc khởi tạo biến cục bộ là bắt buộc

Instance Variable là biến non-static được định nghĩa bên ngoài block/ method/ constructor
- Khi sử dụng biến thể hiện, các biến này được tạo ra khi tạo ra 1 object, bị hủy khi object này bị hủy + chỉ có thể truy cập thông qua object của lớp.  
- Việc khởi tạo biến thể hiện là không bắt buộc, nếu không khởi tạo thì giá trị mặc định của nó sẽ được gán.

Static Variable giống như Instance Variable, khác chỗ là nó là static.
- 1 Class chỉ có 1 phiên bản của biến tĩnh, bất kể là tạo bao nhiêu object.  
- Biến tĩnh được khởi tạo, và load vào Method Area khi bắt đầu chương trình, và tự động dellocated khi chương trình kết thúc.  
- Biến tĩnh không thể khai báo bên trong các block/ method/ constructor.  
- Việc khởi tạo biến tĩnh là không bắt buộc.  
- Nếu ta truy cập biến tĩnh qua object, Compiler sẽ tự động chuyển nó thành Class gọi biến tĩnh (và việc đặt như vậy là không khuyến khích)
## String
String là 1 class phổ biến trong java đại diện cho 1 tập ký tự + mỗi ký tự là 16bit + được lưu bằng 1 mảng các ký tự (byte\[\])
- String trong java là immutable, một khi nó đã được tạo ra thì nó sẽ không thể thay đổi + việc thay đổi nó sẽ trả về 1 đối tượng mới.  
  ![Hình minh họa từ tài liệu gốc](images/string-memory.png)

StringBuilder là 1 chuỗi có thể thay đổi được, tuy nhiên lại không thread-safe → chỉ nên sử dụng trong 1 thread, phù hợp với chuỗi thay đổi giá trị thường xuyên trong cùng 1 thread.

StringBuffer là 1 chuỗi có thể thay đổi được và thread-safe (nhiều thread có thể cùng thay đổi 1 object này vì có cơ chế synchronize tránh dẫm chân nhau).

String pool là ctdl được sử dụng rộng rãi nhất, là 1 vùng nhớ đặc biệt sử dụng để lưu trữ String bởi JVM.
- Các String giống nhau, thay vì tạo mới 1 String mới và tống vào RAM thì sẽ ánh xạ tới String có giá trị tương tự.

![Hình minh họa từ tài liệu gốc](images/string-pool.png)
- Lý do String pool dùng String + String dạng immutable là do multi-thread, thread 1 đang sử dụng chuỗi a chả hạn, và thread 2 khác cũng đang sử dụng chuỗi a, nhưng thread 1 đã làm thay đổi giá trị của chuỗi a -> thread 2 cũng sẽ thay đổi giá trị chuỗi a theo gây sai lệch -> giải pháp là dùng immutable để giúp thread safe.
## Inheritance
Inheritance - kế thừa là 1 class cho phép inherit feature (fields + methods) của 1 class khác
- Trong Java thì kế thừa có nghĩa là tạo mới 1 class mà dựa vào 1 class khác đã tồn tại.  
- Khi kế thừa thì ta có thể reuse methods, fields của class được kế thừa đó.  
- Ngoài việc kế thừa các member có sẵn này, ta cũng có thể bổ sung fields + methods bổ sung cho class mới này.  
- Khi gọi constructor của bất kỳ class nào, đầu tiên cũng là call tới super()/ super(....) + nếu không chủ động insert thì compiler cũng sẽ tự động insert.  
- Việc call tới constructor của Superclass khi Subclass được gọi là đúng, nhưng bản thân chỉ có Subclass object được khởi tạo + lý do của việc gọi tới SuperClass constructor là sử dụng để khởi tạo các giá trị thuộc tính nằm trong Superclass  
- Ngoài ra khi call tới constructor của Superclass thì constructor này cũng sẽ call tới constructor của Superclass của nó, nếu không có thì nó sẽ call tới constructor của Object vì Object là Superclass của tất cả các class trong Java.  
- Trong Java, class không được support Multi-inheritance với class mà phải thông qua Interface + Interface có thể extends được từ nhiều Interface khác.  
- Tuy nhiên, Subclass chỉ có thể access vào Public/ Protected member, không thể access vào Private member.

Why Inheritance
- Reuse giúp việc viết những code chung ở Superclass và các Subclass sẽ kế thừa, giúp không cần phải viết thêm code, tránh boiler-code.  
- Method overriding - Run Time Polymorphism đạt được thông qua kế thừa.  
- Abstraction cho phép tạo Abstract class để define các methods sử dụng cho các Subclass, thúc đẩy thêm tính Encapsulation giúp code dễ mở rộng, maintain.

Why not support multi-inheritance
- Lý do nằm ở việc mong muốn thiết kế đơn giản, rõ ràng để có thể dễ dàng maintain do vấn đề phát sinh nhiều Superclass cho 1 Subclass -> lúc maintain sẽ gặp khó khăn, các Subclass chịu ràng buộc lớn hơn (Class A1 kế thừa B1, C2 + A2 kế thừa B1, C1, D3,... khá phức tạp, việc sửa đổi B1 dẫn tới hành vi của A1, A2 thay đổi theo mặc dù chúng không có sự liên quan với nhau)  
- Diamond Inheritance Problem: khi mà class B, C cùng kế thừa từ A + class D kế thừa B, C nếu được phép multi-inheritance. Vấn đề xảy ra là nếu B, C cùng override lại check() chả hạn, vậy cùng lúc đó D không override method này thì implement của nó sẽ là của B hay C?
## Polymorphism
Polymorphism - đa hình tức là có many form (poly = many ; morphs = form)  
- Đa hình cho phép thực thi 1 single action theo nhiều cách khác nhau  
- Đa hình có thể đạt được thông qua define 1 interface/ abstract class/ 1 class bất kỳ và có nhiều Class implement interface này, mỗi method implement theo cách khác nhau tùy theo các Class implement chúng.

![Hình minh họa từ tài liệu gốc](images/overloading-overriding.png)  
Compile-time Polymorphism đạt được thông qua Overloading function/ constructor, xảy ra tại compile-time do lúc biên dịch đã check luôn rồi, nếu có vấn đề nó sẽ báo lỗi.
- Đại loại là các method cùng tên nhưng khác nhau về: số lượng param, kiểu dữ liệu của param, thứ tự param. (kiểu trả về khác nhau thì sẽ lỗi nếu giống các cái còn lại)  
- Mục đích là cung cấp nhiều cách sử dụng khác nhau cho cùng 1 chức năng (kiểu cùng hàm add() nhưng số lượng param, data type khác nhau nhưng mục đích đều là cộng)

Run-time Polymorphism đạt được thông qua Override method được resolve tại run-time (tức khi method được gọi, method sẽ được chọn để chạy dựa vào data type của object) + tuy nhiên nó lại gây thêm Overhead do JVM phải xử lý thêm để phát hiện method phù hợp.
- Quá trình này gọi là Dynamic method dispatch: JVM sẽ kiểm tra kiểu của đối tượng đang được call method (nó là A hoặc subclass của A -> pick method phù hợp dựa vào loại đối tượng)
- Giúp khả năng tái sử dụng mã, method nào cần override thì viết, không thì sử dụng lại giống với Superclass (apply với Class).  
- Qui các Class khác nhau về 1 Generic Type cụ thể để dễ dàng viết code handle 1 cho nhiều Type.  
- Dễ dàng maintain hơn khi sử dụng (List a = new ArrayList(), nếu muốn thay đổi datatype thì chỉ cần thay đổi vế phải, và các đoạn code bên dưới không cần phải thay đổi gì cả)  
**![Hình minh họa từ tài liệu gốc](images/access-modifiers.png)**  
## Interface
Interface giống Class, có fields, default methods, static method, cũng có method nhưng chỉ có phần signatures chứ không có phần body  
Static method của Interface cũng gần giống với Class, chỉ có điều là nó không thể được inheritance như với Class  
- Tất cả các fields, methods của Interface mặc định sẽ là public (riêng với fields thì dạng public static final)  
- Các methods của Interface không có giá trị, chỉ khi được implements bởi Class thông qua keyword implements  
- Khi 1 Class implements 1 Interface, bắt buộc phải implements toàn bộ method của Interface này + các method implements của Class phải giống so với signature methods của Interface (names, params, exceptions, …)   
- 1 Interface có thể extends từ nhiều Interface khác

Interface instance  
- Polymorphism: Khi ta create 1 Class object mà implements từ 1 Interface -> object này có thể được ref như 1 instance của Interface (giống với concept của Inheritance instantiation)  
Interface không có constructor nên ta không thể tạo object từ constructor được, mà phải tạo object từ Class, sau đó ref tới object này thông qua Interface   
- Ta có thể implements nhiều Interface trong 1 Class

Interface default method  
- Trước Java 8, Interface không thể trực tiếp write implements cho method của mình  
- Giả sử ta có 1 cái Open-source và trên toàn cầu, hàng nghìn doanh nghiệp đang sử dụng của bạn => khi ta update library bằng cách add 1 signature method lên Interface của library để support tính năng mới cho library => khiến hàng ngàn doanh nghiệp này phải viết lại code của các Class mà implements từ Interface này  
- Ngoài ra nếu 1 Function Interface đang sử dụng ở nhiều nơi => việc thêm concrete method cũng sẽ ảnh hưởng tới các code khác đang chạy  
=> Default method của Interface ra đời, cho phép Interface có thể trực tiếp write implements cho Interface mà các Class implements Interface này không cần phải viết implements cho Default method => Library có thể add thêm methods mà không ảnh hưởng tới những doanh nghiệp đang sử dụng  
(Nó support Backward Compatibility - khả năng tương thích ngược tức giúp hệ thống vận hành tốt với version cũ khi update version mới mà không yêu cầu sửa đổi đáng kể/ gây gián đoạn hệ thống đang vận hành)  
- Default method cũng có thể override bởi Class implements từ Interface này
## Abstraction
Abstraction (tính trừu tượng): là kỹ thuật ẩn đi các chi tiết triển khai phức tạp (tập trung vào cái method này để làm gì chứ không tập trung vào cách thức triển khai).
- Chỉ hiển thị các tính năng cần thiết (tôi call hàm addCart, nhưng tôi chỉ cần biết nó add sản phẩm vào cart, không cần biết là chi tiết code triển khai thế nào) → dùng library nhanh hơn/ dùng hàm có sẵn của người khác viết.  
- Abstraction là nguyên tắc thiết kế, không phải là cứ dùng interface/ abstract class là đạt được abstraction.

  **Abstract class**

Abstract class là 1 class được khai báo kèm với abstract keyword, abstract class gồm concrete method + abstract method  
- abstract keyword apply với class và method, not variables + có thể apply với top-level class hoặc inner class  
- Abstract class không thể initialize được mà phải gián tiếp qua Sub-non-abstract class của nó + tuy nhiên vẫn có constructor cho abstract class  
- Abstract class có final, static method nhưng apply với concrete method, còn bản thân abstract method là để implement nên final, static là vô lý  
- Nếu A là Abstract class có abstract method m1(), m2() -> tạo 1 Class B extends từ A thì vẫn có thể chỉ implements m1(), sau đó class C lại extends từ B để implements m2()
## Interface vs Abstract class
- Interface support multi-inheritance > ta chỉ có thể inheritance 1 class duy nhất  
- Interface cung cấp abstract method và bắt buộc các class phải implements hết đống method này >< Abstract class cung cấp thêm cả concrete method, ta có thể override hoặc không (mặc dù sau này Interface cung cấp static/ default method nhưng không dùng thế)  
- Interface là no state < Abstract class là have state -> các variable của nó có thể thay đổi theo thời gian trong khi Interface thì không	

**Overview hậu Java 8**  
Interface sinh ra là để define các công việc mà 1 class có thể làm, thay vì làm nó như thế nào  
- Trong Java 8 có thêm function về Interface là default + static method, tuy nhiên nó có nhiệm vụ khá đặc biệt, không phải ta cứ define class là ngay lập tức add default/ static method vào luôn Interface, mà mục đích của nó là để maintain   
=> Tức là nếu 1 Interface đã tồn tại lâu rồi, được sử dụng bởi rất nhiều các Class khác, nhưng nếu doanh nghiệp yêu cầu thêm 1 chức năng cho hệ thống, chả lẽ ta lại thêm abstract method vào Interface -> dẫn tới hàng chục class đang implement nó sẽ break structure ? Tệ hơn là nếu ta sử dụng 1 Library có sẵn của Java support, vào 1 ngày này đó nó add thêm 1 abstract method vào và project của hàng chục doanh nghiệp đang implement nó bị break structure nếu muốn tăng version của library?   
=> Default method của Interface sinh ra làm giải pháp cho vấn đề này, ta có thể add thêm 1 method vào 1 exists Interface mà ko break structure của các classes đang implement nó + có thể override lại method này nếu muốn  
=> Static method của Interface sinh ra cũng giống như Default method mà khác 1 chỗ là implement class ko thể override lại method này
- Ngoài ra rõ ràng Interface để define các công việc class có thể làm, nên bí lắm như case trên ta mới phải chấp nhận viết implement trực tiếp lên Interface, vì nó sẽ làm dài code, mục tiêu của Interface là muốn dev nhìn vào và biết ngay lập tức chức năng của function đó => việc viết implement sẽ cản trở tính Abstraction  
=> Vậy thì Interface có là 100% abstraction không => KHÔNG

**Use Case**  
**Interface** thì sử dụng để define các chức năng có thể làm của Class chứ không hề define cách thức hoạt động của function + việc add thêm 1 abstract method vào 1 Interface đã tồn tại và sử dụng rồi thì gây nhiều vấn đề (giả sử 100 class đang implement + add thêm 1 abstract method vào thì 100 class này bị break structure, phải implement cái method vừa add vào -> sử dụng default/ static method)

**Abstract class** thì có thể sử dụng cả concrete method + abstract method + field => nên sử dụng khi các class sử dụng chung các thuộc tính (thời gian tạo, thời gian update bản ghi,…) và cùng sử dụng các method có chung implement (set thời gian tạo mới,...) + việc add thêm 1 method như concrete method là cho phép trong abstract class + nó không làm break structure của các class khác

## Upcasting và Downcasting

Casting không làm thay đổi object trong Heap; nó chỉ thay đổi kiểu reference mà compiler dùng để xác định những member nào có thể được truy cập.

```java
class Animal {
    void speak() {
        System.out.println("Animal sound");
    }
}

class Dog extends Animal {
    @Override
    void speak() {
        System.out.println("Woof");
    }

    void fetch() {
        System.out.println("Fetching the ball");
    }
}
```

### Upcasting: subtype → supertype

```java
Dog dog = new Dog();
Animal animal = dog; // tự động upcast, không cần (Animal)

animal.speak(); // "Woof" — gọi method override của Dog
// animal.fetch(); // compile error: Animal không khai báo fetch()
```

- Object thực tế vẫn là `Dog`; chỉ reference `animal` có kiểu compile-time là `Animal`.
- Upcasting an toàn và được Java thực hiện tự động vì mọi `Dog` đều là một `Animal`.
- Compiler chỉ cho truy cập member được khai báo bởi kiểu reference `Animal`. Riêng instance method bị override vẫn được chọn theo kiểu object thực tế tại runtime — đây là polymorphism.
- Upcasting giúp code làm việc với abstraction thay vì phụ thuộc vào từng implementation cụ thể.

Ví dụ thực tế: xử lý nhiều phương thức thanh toán qua cùng một interface.

```java
interface PaymentMethod {
    void pay(long amount);
}

class CreditCard implements PaymentMethod {
    public void pay(long amount) {
        System.out.println("Pay by credit card: " + amount);
    }
}

class BankTransfer implements PaymentMethod {
    public void pay(long amount) {
        System.out.println("Pay by bank transfer: " + amount);
    }
}

void checkout(PaymentMethod method, long amount) {
    method.pay(amount);
}

checkout(new CreditCard(), 500_000);   // CreditCard được upcast
checkout(new BankTransfer(), 500_000); // BankTransfer được upcast
```

`checkout()` không cần biết implementation cụ thể, nên có thể bổ sung phương thức thanh toán mới mà không phải thay đổi logic hiện tại.

### Downcasting: supertype → subtype

Downcasting dùng khi reference có kiểu cha nhưng cần truy cập hành vi chỉ có ở kiểu con. Java yêu cầu cast tường minh vì compiler không thể đảm bảo object thực tế có đúng kiểu con hay không.

```java
Animal animal = new Dog();
Dog dog = (Dog) animal; // hợp lệ vì object thực tế là Dog
dog.fetch();
```

Cast sai vẫn compile được nhưng gây `ClassCastException` tại runtime:

```java
Animal animal = new Animal();
Dog dog = (Dog) animal; // ClassCastException
```

Nên kiểm tra bằng pattern matching của `instanceof` trước khi downcast:

```java
void play(Animal animal) {
    if (animal instanceof Dog dog) {
        dog.fetch();
    }
}
```

Tóm lại:

- Upcasting: tự động, an toàn và thường dùng để đạt polymorphism.
- Downcasting: tường minh, có thể lỗi tại runtime và chỉ nên dùng khi thực sự cần hành vi riêng của subtype.
- Nếu phải liên tục dùng `instanceof` và downcast, nên cân nhắc đưa hành vi chung vào interface/abstract class để thiết kế hướng đa hình hơn.

			**Autoboxing vs Unboxing**  
Primitive lưu trực tiếp giá trị trong Stack
- Có kiểu dữ liệu cơ bản, có sẵn trong Java (int, float, double, char, boolean, byte, short, long)   
- Có giá trị mặc định (int thì là 0, boolean là false,...)  
- So sánh ==.  
- Dùng khai báo trực tiếp.  
- Khi gán biến a cho biến b, bản chất là copy giá trị và đặt trong stack → 1 cái thay đổi không làm thay đổi cái còn lại.  
- for qua 1m element rồi tính toán dùng primitive nhanh hơn (không phải phân bổ trong Heap, ko bị autoboxing, unboxing qua lại) nhiều so với reference tới 5-7 lần.

Reference lưu địa chỉ tham chiếu đến đối tượng trong Stack, giá trị thực sự nằm trong Heap.
- Có kiểu dữ liệu tham chiếu đến đối tượng trong bộ nhớ (class, array, interface, String, wrapper classes).  
- Mặc định là null nếu không gán.  
- So sánh qua equals().  
- Khai báo qua new().  
- Khi gán biến a cho biến b, bản chất là copy tham và đặt trong stack → thay đổi object làm thay đổi cả a, b.

Autoboxing, Unboxing là quá trình mà Compiler chuyển đổi tự động qua lại giữa Primitive type và Wrapper type một cách ngầm định.
- Trước Java 5 thì cần phải chuyển đổi thủ công, và giờ đã support trong java để tránh xấu code  
- Hiệu suất có thể giảm do phải chuyển đổi giữa các kiểu.  
- Tăng áp lực lên Garbage Collection do phải dọn Heap.

![Hình minh họa từ tài liệu gốc](images/boxing-unboxing-types.png)  
Autoboxing là chuyển từ kiểu Primitive sang Wrapper bằng cách copy giá trị ban đầu, đặt vào trong vùng Heap
- Việc chuyển đổi này tốn thời gian và tốn dung lượng thêm do phải lưu cả Stack lẫn Heap

![Hình minh họa từ tài liệu gốc](images/boxing-memory.png)  
  
Unboxing là chuyển từ kiểu Wrapper sang Primitive bằng cách copy giá trị trong Heap về Stack.
- Nếu giá trị của Wrapper là null thì sẽ NullPointerException

![Hình minh họa từ tài liệu gốc](images/unboxing-memory.png)  

			     **Anonymous Class**  
Anonymous Class là 1 class không được define class name, tên của nó được sinh ra sau quá trình compile
- Sử dụng để define class, đồng thời khởi tạo đối tượng luôn  
- Thông thường không nên sử dụng do không thể reuse được Anonymous class, còn làm dài code -> nếu sử dụng thì cũng nên override ít method lại.  
- Áp dụng được cả với Interface  
- Sử dụng constructor của Superclass để khởi tạo (có param thì cũng phải truyền param vào)

Local Class 
- Local Class giúp tạo các đối tượng mà không cần định nghĩa trước class, coi Class bị ghi đè là Superclass  
- Ở ví dụ bên dưới thì đang kế thừa abstract class, còn nếu là class thường thì có thể override lại bất kỳ method nào.  
  

Local Inner Class
- Local Inner Class giúp define 1 class bên trong 1 method  
## Exception
Exception - ngoại lệ, là sự kiện xảy ra trong quá trình thực thi chương trình, làm gián đoạn luồng thực thi  
![Hình minh họa từ tài liệu gốc](images/exception-hierarchy.png)
- Throwable là lớp lớn nhất trong hệ này, dành cho tất cả các object mà có thẻ sử dụng kèm với throw, try-catch.  
- Error là class đại diện cho 1 lỗi nghiêm trọng xảy ra ở cấp độ JVM mà chương trình không nên bắt trong quá trình thực thi, thường liên quan tới vấn đề môi trường chạy JVM, một số có thể xử lý, một số không thể xử lý (như StackOverFlow, OutOfMemory,...)  
- Exception là class đại diện cho ngoại lệ trong quá trình thực thi chương trình có thể dự đoán được, và nên được xử lý ngoại lệ này để chương trình tiếp tục hoạt động mà không gặp gián đoạn.  
- Unchecked Exception/ RuntimeException là các ngoại lệ xảy ra bất ngờ, thường là do logic code nên không cần bắt try-catch (NullPointerException, ArrrayIndexOutOfBoundsException,...), không thể dự đoán được tại compile + thế thì cái nào cũng phải try-catch :))  
- Checked Exception là các ngoại lệ có thể xảy ra, được dự đoán trước → buộc phải try-catch để xử lý ngoại lệ/ throws để khai báo ngoại lệ (FileNotFoundException, IOException,...), lỗi này có thể dự đoán trước được nên buộc phải xử lý để tránh lỗi.
- throw sử dụng để ném ra 1 ngoại lệ >< throws sử dụng để khai báo các ngoại lệ có thể xảy ra trong 1 method (buộc các method khác sử dụng phải xử lý Checked exception).
## Lambda Expression
Function Interface là 1 interface chỉ chứa 1 abstract method   
- Java 8 đã cung cấp @FunctionalInterface annotation để giúp đánh dấu 1 interface là 1 Function interface (việc thêm này không bắt buộc, nhưng hỗ trợ tại compile-time nếu cố tình thêm >= 2 abstract method trong cùng 1 Function interface)  
- Sử dụng default/ static method là chấp nhận được, miễn là chỉ có 1 abstract method  
- Function interface sinh ra để hỗ trợ Lambda expression, giúp viết code ngắn gọn, dễ đọc hơn → đánh dấu bước chuyển mình của Java từ OOP thuần sang kết hợp với Function Programing

Lambda expression là 1 anonymous function (yêu cầu param, body của method trong phần triển khai, có thể trả về hoặc không)
- Trước java 8, phải viết rất dài dòng với Anonymous class
- Hạn chế lớn nhất của Lambda là chỉ sử dụng được cùng với Function interface.  
- Lambda triển khai abstract method duy nhất, từ đó biểu diễn nhanh 1 implement của Function interface.   
- Stream, Collection cung cấp các lib để sử dụng kèm với Lambda để mã nguồn ngắn gọn, dễ đọc hơn.

Method references là 1 dạng đặc biệt của Lambda expression bằng cách tham chiếu tới 1 method/ constructor đã có sẵn.
- Sử dụng toán tử “::” để truyền tham chiếu.  
- Sử dụng trong Stream, Optional, Comparator,... (các nơi chấp nhận Lambda) để giúp code ngắn gọn hơn.

Static method references (Class::staticMethod) sử dụng để tham chiếu tới 1 static method   
Particular Instance method references (object::instanceMethod) sử dụng để tham chiếu tới 1 method của Class thông qua 1 object cụ thể.  
Arbitrary Instance method references (Class:instanceMethod) sử dụng để tham chiếu tới 1 method của 1 Class cụ thể.  
Constructor references (Class::new) sử dụng để tham chiếu tới 1 constructor của 1 Class bất kỳ.

Comparable sử dụng để define thứ tự giữa các object của 1 Class.  
- Là 1 interface cung cấp method mà Class muốn so sánh phải implement để quyết định thứ tự của các object + sau đó sử dụng Arrays.sort() để sắp xếp tập đối tượng dựa vào triển khai vừa cung cấp.  
- Do Class cần sắp xếp cần tự implement -> sai nguyên tắc SRP  
    
  

Comparator cũng được sử dụng để define thứ tự giữa các object của 1 Class  
- Nó khác so với Comparable ở chỗ là Comparator là 1 Function Interface, cần tạo 1 class khác để triển khai cách thức sắp xếp, thay vì class muốn sắp xếp cần tự implement như Comparable.  
- Do Class cần sắp xếp không cần tự implement mà cần triển khai bởi 1 class khác -> đúng nguyên tắc SRP.  
- Là 1 Function Interface -> sử dụng dễ dàng với Lambda expression.  
## Stream
Stream là một tính năng quan trọng cho xử lý tập hợp dữ liệu theo phong cách lập trình hàm.
- Tạo chuỗi các thao tác xử lý liên tiếp (input → xử lý → output làm input cho thao tác tiếp theo cho tới cuối cùng) + code nhìn ngắn gọn hơn.

![Hình minh họa từ tài liệu gốc](images/stream-pipeline.png)
- Stream hỗ trợ các hoạt động tuần tự (stream) sử dụng 1 thread / song song (parallelStream) sử dụng nhiều thread.  
- Parallel Stream cho phép xử lý dữ liệu song song (parallel processing) bằng cách tự động chia nhỏ dữ liệu thành nhiều phần và xử lý trên nhiều luồng (threads) cùng lúc, tận dụng đa lõi CPU.  
- Tự động chia công việc thành các task nhỏ  
- Phân phối cho ForkJoinPool (thread pool)  
- Gộp kết quả lại khi hoàn thành

   

![Hình minh họa từ tài liệu gốc](images/parallel-stream.png)
- Stream không lưu trữ các element của Collection/ Array, mà chỉ thực hiện các phép toán tổng hợp + khi lấy Collection/ Array cũ thì nhận thấy giá trị của chúng không thay đổi, mà chỉ trả về 1 Collection/ Array mới → thread safe.  
## Single Repository principle
Single Repository principle tức là 1 module (class, function, module,...) nên chỉ có 1 repository - 1 lý do để thay đổi code ở đây + nếu 1 module có nhiều hơn 1 lý do để sửa đổi, nó đang vi phạm SRP.  
- SRP là về tính gắn kết cao, nhóm những thứ thay đổi vì cùng lý do → dễ đọc, tạo ra hệ thống linh hoạt, dễ bảo trì, mở rộng.

Giả sử ta có 1 Class là Employee.class có 3 method + có 2 method đang sử dụng chung 1 private method khác  
![Hình minh họa từ tài liệu gốc](images/srp-employee.png)
- Doanh nghiệp yêu cầu thay đổi cách thức tính lương cho CFO (actor) -> calculateSalary() function cần thay đổi + ta phải thay đổi getRegularHours() function để đáp ứng nhu cầu của doanh nghiệp  
- Tuy nhiên calculateHours() cũng đang call tới getRegularHours() + getRegularHours() function này đang cần được thay đổi để đáp ứng sự thay đổi của calculateSalary() function => sự thay đổi của calculateHours() sẽ gián tiếp ảnh hưởng tới getRegularHours() bởi chúng đang call cùng 1 function bị thay đổi mặc dù không có sự yêu cầu thay đổi cách thức tính giờ cho HR => calculateHours() sẽ bị lỗi mặc dù trước đó đã hết bug => VIOLATES SINGLE REPOSITORY PRINCIPLE

![Hình minh họa từ tài liệu gốc](images/srp-change-impact.png)  
=> Suy cho cùng thì các Class, method khác nhau mà sửa đổi thì không được ảnh hưởng tới nhau => Việc kiểm thử, bảo trì code trở nên tốt hơn.	

One class should has 1 public method  => Ta có bao nhiêu public method trong 1 Class cũng được, miễn là chúng nên được liên quan với nhau + nếu có yêu cầu sự thay đổi từ 1 function sẽ không ảnh hưởng tới các function còn lại => chỉ có function được sửa thì được sửa lại còn các function khác trong hệ thống vẫn hoạt động bình thường

Single Repository Principle còn là việc tách nhỏ Class ra theo function/ tách nhỏ 1 function của Class ra thành nhiều phần (module hóa - chia để trị) + để các function same same về mặt chức năng vào 1 Class (function tạo mới sản phẩm, function chỉnh sửa sản phẩm)
- Thay vì đọc 1 class ngàn dòng, các method được chia nhỏ ra và đặt vào các Class khác => khi ta modify, maintain, read sẽ dễ dàng hơn cho mình   
- Ví dụ như: ta tách thành Controller (tác dụng để điều hướng), Service (xử lý business logic), Repository (tương tác với database) chứ không ai để hết logic ở 1 class khi mà Request tới => khi maintain ta sửa business logic thì vào Service sửa,…  
- Ngoài ra cũng có liên quan tới Aspect_1 do việc đã tách class rồi, nên việc sửa đổi ở Class này sẽ không ảnh hưởng tới Class kia.  
- Việc chia code như này, 2 dev làm 2 module khác nhau khi merge code sẽ không gây ra conflict.  
- Dễ bảo trì, sửa 1 tính năng không ảnh hưởng tới tính năng khác + các class ít phụ thuộc vào nhau >< nếu vi phạm thì khi đặt tên khó đặt vì bên trong có nhiều logic khác biệt/ class quá dài.
## Open-closed Principle
Open-closed Principle đề cập tới vấn đề yêu cầu doanh nghiệp, khi 1 business logic yêu cầu được thay đổi/ thêm mới thì ta không cần phải thay đổi các đoạn code hiện có.
- Có thể giải quyết bằng cách tạo mới 1 Subclass, override function cần thay đổi đó/ thêm mới function + cũng có thể sử dụng đa hình với Superclass + có thể thêm hẳn method mới.  
- Không sửa code cũ → giảm nguy cơ gây bug  
- Open-closed Principle thường apply với sản phẩm khi đã lên Production  
- Ví dụ ta có chức năng đăng bài viết nhưng chỉ bao gồm text ở version 1, sau đó ở version 2 thì doanh nghiệp yêu cầu chức năng đăng bài viết cần thêm ảnh thì việc ta cần làm là tạo mới 1 function khác kế thừa từ function ở version 1 và chỉnh sửa -> version 1 và version 2 vẫn có thể work độc lập mà không ảnh hưởng gì -> không cần phải kiểm thử các chức năng cũ vì nó không hề có sự thay đổi gì  
- Tuy nhiên việc override xảy ra vấn đề khi chức năng đăng bài của version 1 thay đổi thì cũng sẽ ảnh hưởng tới version 2 => tight coupling (Superclass thay đổi thì Subclass cũng ảnh hưởng theo) => nên implements Interface chung + inject class của version 1 vào và call tới method cần thay đổi.
## Liskov Substitution Principle
[https://theobjectorientedway.com/chapters/liskov-substitution-principle.html](https://theobjectorientedway.com/chapters/liskov-substitution-principle.html)  
[https://www.baeldung.com/cs/liskov-substitution-principle](https://www.baeldung.com/cs/liskov-substitution-principle)  
[https://www.youtube.com/watch?v=7hXi0N1oWFU\&t=586s](https://www.youtube.com/watch?v=7hXi0N1oWFU&t=586s)

Liskov Substitution Principle đề cập tới việc object của Subclass có thể sử dụng để thay thế cho object của Superclass mà không ảnh hưởng tới tính đúng đắn của chương trình
- Liên quan nhiều tới ngữ nghĩa hơn: chả hạn có SuperClass là Bird có method fly(), và SubClass gồm penguin, eagle, macaw,... thì penguin đang vi phạm do việc kế thừa fly() là vô lý, nó buộc phải throw exception hoặc hành động sai phạm đại loại thế mà không đúng ngữ nghĩa của SuperClass đã đề ra, vô tình làm ảnh hưởng tới chương trình khi call tới method này.
## Interface Segregation Principle
ISP: 1 interface lớn chứa nhiều method không liên quan tới nhau, nên chia nhỏ ra để tránh trường hợp 1 class implement nhưng không đáp ứng đủ toàn bộ method.
- Các class không bắt buộc phải implement vào các interface mà chúng không sử dụng.

(Ví dụ như trong createUser() public function thì có call tới genSalt() private function nhưng ta có viết genSalt() trên Interface ? Rõ ràng là không nên viết vì chả sử dụng genSalt ở đâu cả ở trên Module khác, mà chỉ sử dụng ở functional đăng ký user)
- 1 Interface không nên quá cồng kềnh, khi 1 Class implements nó thì buộc phải implements đủ các function đó, nếu không thực sự implements function nào thì rõ ràng đã thừa function không cần thiết trong 1 Interface  
- Việc apply LSP tối đa sẽ cho 1 Interface sẽ chỉ có 1 function duy nhất (Function Interface) nhưng các method có cùng tính chất, tư tưởng thì nên đặt cùng 1 Interface 

  
Ví dụ dưới đây nói rằng BurgerOrderService không phục vụ khoai tây chiên, combo nên để 2 implements method kia sẽ không được implements một cách đúng đắn => vi phạm cả L và I principle  
  
=> Việc cần làm là segregation thành 2 method, BurgurOrderService sẽ implements chỉ 1 method của Interface  
## Dependency Inversion Principle
Tight-coupling ám chỉ mối quan hệ giữa các Class quá chặt chẽ với nhau => khi thay đổi logic của 1 Class thì các Module đang sử dụng Class này sẽ bị break   
Loosely-coupled là giảm bớt sự phụ thuộc giữa các Class với nhau.  
Dependency Inversion Principle: các module cấp cao không nên phụ thuộc vào các module cấp thấp (phụ thuộc vào interface/ abstract class thay vì implement trực tiếp của chúng).  
  
    

Dependency Inversion Principle (DIP - nguyên lý đảo ngược phụ thuộc) là nguyên tắc, kỹ thuật cung cấp các phụ thuộc cho 1 đối tượng từ bên ngoài, thay vì để đối tượng tự tạo ra chúng.  
  
Dependency Injection (DI - tiêm phụ thuộc) là cách triển khai của DIP, cung cấp các dependency từ bên ngoài thay vì tạo bên trong.
- Construction injection sử dụng tại thời điểm khởi tạo object >< Setter injection sử dụng tại runtime >< Method injection sử dụng cho 1 method cụ thể  
- Áp dụng DI đạt được DIP, giúp hệ thống giảm sự phụ thuộc, dễ code, dễ maintain.
- @Autowire trong Spring Boot bản chất chính là Field Injection, inject sau khi mà construction chạy + có thể bị thay đổi qua reflection + khó unit test, phải reflection để mock inject + circular dependency khi nhấn chạy không phát hiện lỗi

Inversion of Control (IoC - đảo ngược điều kiện): là 1 nguyên lý thiết kế trong đó luồng điều khiển của chương trình đảo ngược so với lập trình truyền thống.
- Lập trình truyền thống thì developer chủ động khởi tạo object rồi gọi method >< IoC thì framework/ container tạo, quản lý vòng đời của object, gọi code của bạn khi cần.  
- DI là cách triển khai phổ biến của IoC.
