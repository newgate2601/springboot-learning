# Câu hỏi phỏng vấn Java Core

## Tài liệu tham khảo

- [Google Doc gốc - Đề cương phỏng vấn Java](https://docs.google.com/document/d/1Ooc9-hxPGU8b4fIZHDfNgQtrwvcQYTM7791WME1YFwY/edit?tab=t.0)
- [Java SE Documentation](https://docs.oracle.com/en/java/javase/)
- [Java Language Specification](https://docs.oracle.com/en/java/javase/21/docs/specs/jls/se21/html/index.html)
- [Java Tutorials - Object-Oriented Programming Concepts](https://docs.oracle.com/javase/tutorial/java/concepts/index.html)
- [Java Collections Framework](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/package-summary.html)
- [Object API - equals/hashCode](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html)
- [String API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html)
- [java.util.concurrent API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/package-summary.html)
- [Java Virtual Machine Specification](https://docs.oracle.com/javase/specs/jvms/se21/html/index.html)

## Tổng quan

Tài liệu này chỉ tập trung vào **Java Core**: ngôn ngữ Java, OOP, JVM, Collections, Generics, Exception, String, Serialization và Concurrency. Các chủ đề framework, database, message broker, network security, deployment hoặc system design nên tách thành tài liệu riêng.

Khi trả lời phỏng vấn Java Core, nên đi theo thứ tự:

1. Định nghĩa khái niệm bằng 1-2 câu.
2. Nói cơ chế chính hoặc điểm khác nhau quan trọng.
3. Nêu ví dụ thực tế trong code Java.
4. Nhắc lỗi hay gặp nếu có.

Ví dụ: khi hỏi `equals()` và `hashCode()`, không chỉ nói "dùng để so sánh object". Cần nói thêm contract, liên hệ `HashMap/HashSet`, và lỗi khi object mutable làm key.

## 1. Java là gì?

Java là ngôn ngữ lập trình hướng đối tượng, kiểu tĩnh, được biên dịch thành bytecode và chạy trên JVM.

Các đặc điểm chính:

- **Object-oriented:** code được tổ chức quanh class và object.
- **Statically typed:** kiểu dữ liệu được kiểm tra lúc compile.
- **Platform independent:** cùng bytecode có thể chạy trên nhiều hệ điều hành nếu có JVM phù hợp.
- **Automatic memory management:** JVM có Garbage Collector để thu hồi object không còn dùng.
- **Rich standard library:** Java có sẵn collection, I/O, networking, concurrency, date/time, stream,...

Ví dụ thực tế: một chương trình Java có thể được build thành file `.jar`. File này chạy được trên Windows/Linux/macOS nếu máy có Java runtime tương thích.

```text
Source code .java -> bytecode .class -> JVM -> machine code/runtime execution
```

Điểm cần nói cẩn thận: Java không phải lúc nào cũng "chỉ thông dịch". JVM hiện đại thường kết hợp interpreter và JIT compiler để tối ưu code chạy nhiều lần.

## 2. JDK, JRE và JVM khác nhau thế nào?

| Thành phần | Ý nghĩa | Dùng khi nào |
| --- | --- | --- |
| JVM | Máy ảo chạy bytecode Java | Luôn cần khi chạy chương trình Java |
| JRE | Môi trường chạy Java, gồm JVM + thư viện runtime | Dùng để chạy app Java đã build |
| JDK | Bộ phát triển Java, gồm runtime + compiler + tool | Dùng để viết, compile, debug, build app Java |

JVM là phần thực thi bytecode. JRE cung cấp môi trường chạy. JDK cung cấp cả môi trường chạy và công cụ phát triển như `javac`, `jar`, `javadoc`, `jshell`, debugger, monitoring tool.

Ví dụ:

```bash
javac Hello.java
java Hello
```

- `javac` thuộc JDK, dùng để compile `.java` thành `.class`.
- `java` là launcher dùng để khởi động JVM chạy class đã compile.

Trong môi trường dev, cần JDK. Trong môi trường production chỉ chạy ứng dụng, nhiều trường hợp chỉ cần runtime image tương ứng.

## 3. Vì sao Java được gọi là platform independent?

Java source code không compile trực tiếp thành machine code dành riêng cho Windows, Linux hoặc macOS. Java compiler tạo ra bytecode `.class`. JVM trên từng hệ điều hành sẽ chạy bytecode đó.

```text
Hello.java --javac--> Hello.class --JVM Windows/Linux/macOS--> chạy được
```

Vì vậy Java thường được mô tả bằng ý "write once, run anywhere".

Tuy nhiên, không nên hiểu tuyệt đối. Một chương trình Java vẫn có thể phụ thuộc platform nếu:

- Dùng đường dẫn file kiểu Windows như `C:\data\file.txt`.
- Dùng native library `.dll`, `.so`, `.dylib`.
- Phụ thuộc encoding, timezone, locale của hệ điều hành.
- Gọi command hệ điều hành qua `ProcessBuilder`.

Ví dụ thực tế: service Java backend đóng gói thành `.jar` có thể chạy trên laptop Windows và server Linux. Nhưng nếu code hard-code đường dẫn Windows, chạy trên Linux sẽ lỗi.

## 4. Java compile và run như thế nào?

Quá trình cơ bản gồm 2 giai đoạn lớn: build time và runtime.

```text
Build time:
.java --javac--> .class

Runtime:
.class --ClassLoader--> JVM memory --Interpreter/JIT--> CPU execution
```

Ở build time:

- Compiler kiểm tra syntax và type.
- Source `.java` được chuyển thành bytecode `.class`.
- Với Maven/Gradle, bytecode thường nằm trong `target/classes` hoặc `build/classes`.

Ở runtime:

- JVM khởi động.
- ClassLoader tải class cần dùng.
- Bytecode Verifier kiểm tra bytecode hợp lệ.
- Execution Engine chạy bytecode bằng interpreter và JIT compiler.

JIT compiler là phần quan trọng. Ban đầu JVM có thể interpret bytecode. Khi một method chạy nhiều lần và trở thành hot code, JIT compile method đó thành native machine code để chạy nhanh hơn trong lần gọi sau.

## 5. Primitive type và reference type khác nhau thế nào?

Java có hai nhóm type lớn:

- **Primitive type:** lưu giá trị đơn giản.
- **Reference type:** biến lưu reference trỏ tới object/array.

Primitive type gồm:

| Nhóm | Type |
| --- | --- |
| Số nguyên | `byte`, `short`, `int`, `long` |
| Số thực | `float`, `double` |
| Ký tự | `char` |
| Logic | `boolean` |

Reference type gồm class, interface, array, enum, record,...

```java
int age = 20;
String name = "Tony";
User user = new User();
```

- `age` là primitive, chứa trực tiếp giá trị `20`.
- `name` và `user` là reference, chứa reference tới object.

Điểm khác nhau quan trọng:

| Tiêu chí | Primitive | Reference |
| --- | --- | --- |
| Có thể `null`? | Không | Có |
| Có method? | Không | Có method qua object |
| Dùng trong generic? | Không trực tiếp | Có |
| Ví dụ | `int`, `double` | `Integer`, `String`, `User` |

Ví dụ thực tế: `List<int>` không hợp lệ trong Java, phải dùng `List<Integer>` vì generic cần reference type.

## 6. Wrapper class là gì?

Wrapper class là class bọc primitive thành object.

| Primitive | Wrapper |
| --- | --- |
| `int` | `Integer` |
| `long` | `Long` |
| `double` | `Double` |
| `float` | `Float` |
| `boolean` | `Boolean` |
| `char` | `Character` |
| `byte` | `Byte` |
| `short` | `Short` |

Wrapper cần thiết vì nhiều API Java làm việc với object, ví dụ Collections và Generics.

```java
List<Integer> numbers = new ArrayList<>();
numbers.add(10);
```

Ở đây `10` là primitive `int`, nhưng `ArrayList<Integer>` cần `Integer`, nên Java tự autoboxing.

Điểm cần nhớ:

- Wrapper có thể `null`.
- Wrapper object có overhead memory lớn hơn primitive.
- So sánh wrapper nên dùng `equals()` hoặc `Objects.equals()`, không nên dùng `==` nếu muốn so sánh giá trị.

## 7. Autoboxing và Unboxing là gì?

**Autoboxing** là Java tự chuyển primitive thành wrapper object. **Unboxing** là Java tự chuyển wrapper object về primitive.

```java
Integer a = 10; // autoboxing: int -> Integer
int b = a;      // unboxing: Integer -> int
```

Ví dụ thực tế:

```java
List<Integer> scores = new ArrayList<>();
scores.add(100);          // autoboxing
int first = scores.get(0); // unboxing
```

Lỗi hay gặp là unboxing `null`.

```java
Integer value = null;
int number = value; // NullPointerException
```

Khi code nhận dữ liệu có thể thiếu, ví dụ field `Integer age` từ database hoặc JSON, không nên unbox thẳng sang `int` nếu chưa check `null`.

## 8. Vì sao `Integer a = 127; Integer b = 127; a == b` có thể là `true`?

Java cache một số wrapper value nhỏ để tiết kiệm object. Với `Integer`, range cache mặc định thường là `-128` đến `127`.

```java
Integer a = 127;
Integer b = 127;
System.out.println(a == b); // true

Integer c = 128;
Integer d = 128;
System.out.println(c == d); // thường là false
```

`==` với object so sánh hai reference có trỏ cùng object không. Vì `127` được cache, `a` và `b` có thể trỏ cùng object. Với `128`, thường tạo object khác nhau nên `==` là `false`.

Muốn so sánh giá trị wrapper, dùng:

```java
Objects.equals(c, d);
```

`Objects.equals()` an toàn hơn `c.equals(d)` nếu một bên có thể `null`.

## 9. `float` và `double` khác nhau thế nào?

`float` và `double` đều là kiểu số thực dấu phẩy động, nhưng khác kích thước và độ chính xác.

| Tiêu chí | `float` | `double` |
| --- | --- | --- |
| Kích thước | 32 bit | 64 bit |
| Độ chính xác | Khoảng 6-7 chữ số có nghĩa | Khoảng 15-16 chữ số có nghĩa |
| Literal mặc định | Cần hậu tố `f/F` | Số thập phân mặc định là `double` |
| Dùng khi | Cần tiết kiệm memory, chấp nhận sai số cao hơn | Mặc định cho số thực thông thường |

```java
float price1 = 10.5f;
double price2 = 10.5;
```

Không nên dùng `float` hoặc `double` để tính tiền vì số thập phân không luôn biểu diễn chính xác trong hệ nhị phân.

```java
System.out.println(0.1 + 0.2); // 0.30000000000000004
```

Với tiền tệ hoặc số cần chính xác theo hệ thập phân, dùng `BigDecimal`.

```java
BigDecimal total = new BigDecimal("0.1").add(new BigDecimal("0.2"));
System.out.println(total); // 0.3
```

## 10. Java là pass by value hay pass by reference?

Java luôn là **pass by value**.

Với primitive, Java truyền bản copy của giá trị.

```java
void change(int x) {
    x = 100;
}

int a = 10;
change(a);
System.out.println(a); // 10
```

Với object, Java truyền bản copy của reference. Hai reference cùng trỏ tới một object, nên method có thể sửa state của object đó.

```java
void rename(User user) {
    user.setName("Alice");
}

User u = new User("Bob");
rename(u);
System.out.println(u.getName()); // Alice
```

Nhưng nếu gán lại parameter sang object khác, biến bên ngoài không đổi.

```java
void reassign(User user) {
    user = new User("Alice");
}

User u = new User("Bob");
reassign(u);
System.out.println(u.getName()); // Bob
```

Kết luận: Java không truyền reference gốc; Java truyền bản copy của reference.

## 11. Reference type và object type khác nhau thế nào?

**Reference type** là kiểu của biến tại compile time. **Object type** là kiểu thật của object tại runtime.

```java
List<String> names = new ArrayList<>();
```

Trong ví dụ này:

- Reference type là `List<String>`.
- Object type là `ArrayList`.

Reference type quyết định method nào được gọi hợp lệ khi compile.

```java
List<String> list = new ArrayList<>();
list.add("A");
// list.trimToSize(); // lỗi compile vì List không có method này
```

Object type quyết định implementation thật khi method được override.

```java
Animal animal = new Dog();
animal.sound(); // chạy Dog.sound() nếu Dog override sound()
```

Ví dụ thực tế: thường khai báo bằng interface như `List`, `Map`, `PaymentProcessor` để code ít phụ thuộc implementation cụ thể hơn.

## 12. OOP trong Java gồm những tính chất nào?

OOP trong Java thường được nói qua 4 tính chất:

| Tính chất | Ý nghĩa |
| --- | --- |
| Encapsulation | Che giấu dữ liệu bên trong object, kiểm soát truy cập qua method |
| Inheritance | Class con kế thừa field/method từ class cha |
| Polymorphism | Cùng một lời gọi method nhưng object khác nhau có hành vi khác nhau |
| Abstraction | Ẩn chi tiết triển khai, chỉ lộ ra contract cần dùng |

Ví dụ:

```java
interface PaymentProcessor {
    void pay(long amount);
}

class CardPaymentProcessor implements PaymentProcessor {
    public void pay(long amount) {
        System.out.println("Pay by card: " + amount);
    }
}
```

Code gọi chỉ cần biết `PaymentProcessor`, không cần biết chi tiết thanh toán bằng thẻ được xử lý bên trong thế nào. Đây là abstraction và polymorphism.

## 13. Encapsulation là gì?

Encapsulation là đóng gói dữ liệu và hành vi liên quan trong một class, đồng thời giới hạn truy cập trực tiếp vào state bên trong object.

Ví dụ không tốt:

```java
class BankAccount {
    public long balance;
}
```

Ai cũng có thể sửa `balance`, kể cả gán số âm.

Cách tốt hơn:

```java
class BankAccount {
    private long balance;

    public void deposit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        balance += amount;
    }

    public long getBalance() {
        return balance;
    }
}
```

Lợi ích:

- Bảo vệ object khỏi trạng thái sai.
- Tập trung logic validate trong class.
- Dễ thay đổi implementation bên trong mà ít ảnh hưởng code bên ngoài.

Encapsulation không có nghĩa class nào cũng phải có getter/setter cho mọi field. Nếu setter cho phép sửa state tùy ý, object vẫn có thể mất kiểm soát.

## 14. Inheritance là gì? Khi nào không nên dùng?

Inheritance cho phép class con kế thừa field/method từ class cha.

```java
class Animal {
    void eat() {
        System.out.println("eat");
    }
}

class Dog extends Animal {
    void bark() {
        System.out.println("bark");
    }
}
```

Inheritance phù hợp khi quan hệ thật sự là "is-a".

```text
Dog is an Animal
```

Không nên dùng inheritance chỉ để tái sử dụng code nếu quan hệ không đúng. Khi đó composition thường tốt hơn.

Ví dụ: `Car` không nên extends `Engine`. Xe hơi có động cơ, nhưng xe hơi không phải động cơ.

```java
class Car {
    private Engine engine;
}
```

Lỗi thiết kế inheritance thường làm class con phụ thuộc quá nhiều vào chi tiết class cha, khó sửa và khó test.

## 15. Polymorphism là gì?

Polymorphism là khả năng dùng cùng một interface/class cha để làm việc với nhiều implementation khác nhau.

```java
interface NotificationSender {
    void send(String message);
}

class EmailSender implements NotificationSender {
    public void send(String message) {
        System.out.println("Email: " + message);
    }
}

class SmsSender implements NotificationSender {
    public void send(String message) {
        System.out.println("SMS: " + message);
    }
}
```

Code sử dụng:

```java
void notifyUser(NotificationSender sender) {
    sender.send("Hello");
}
```

`notifyUser()` không cần biết sender là email hay SMS. Nó chỉ cần biết object đó có method `send`.

Trong Java có hai dạng thường gặp:

- **Compile-time polymorphism:** method overloading.
- **Runtime polymorphism:** method overriding.

## 16. Overloading và Overriding khác nhau thế nào?

| Tiêu chí | Overloading | Overriding |
| --- | --- | --- |
| Xảy ra ở đâu | Cùng class hoặc class con | Class con override method class cha/interface |
| Signature | Phải khác parameter list | Phải cùng signature |
| Quyết định lúc nào | Compile time | Runtime |
| Mục tiêu | Cùng tên method, nhận input khác nhau | Thay đổi hành vi method kế thừa |

Overloading:

```java
int add(int a, int b) {
    return a + b;
}

int add(int a, int b, int c) {
    return a + b + c;
}
```

Overriding:

```java
class Dog extends Animal {
    @Override
    void sound() {
        System.out.println("woof");
    }
}
```

Lưu ý: return type một mình không đủ để overload method. Parameter list phải khác.

## 17. Abstract class và interface khác nhau thế nào?

| Tiêu chí | Abstract class | Interface |
| --- | --- | --- |
| Mục đích chính | Chia sẻ state/logic chung cho các class cùng họ | Định nghĩa contract/hành vi |
| Field instance | Có | Không có instance field |
| Constructor | Có | Không có constructor như class |
| Kế thừa | Một class chỉ extends một class | Một class implements được nhiều interface |
| Method có body | Có | Có `default` và `static` method |

Abstract class phù hợp khi nhiều class con có code chung thật sự.

```java
abstract class BaseEntity {
    private Long id;

    public Long getId() {
        return id;
    }
}
```

Interface phù hợp khi cần contract.

```java
interface Exporter {
    void export(String content);
}
```

Tư duy chọn:

- Nếu cần nói "class này có khả năng làm gì", dùng interface.
- Nếu cần chia sẻ state/logic chung giữa các class cùng nhóm, cân nhắc abstract class.
- Không dùng abstract class chỉ vì muốn tránh viết lại vài dòng code nhỏ.

## 18. Access modifier trong Java gồm những loại nào?

Access modifier kiểm soát phạm vi truy cập của class, field, method, constructor.

| Modifier | Cùng class | Cùng package | Subclass khác package | Ngoài package |
| --- | --- | --- | --- | --- |
| `private` | Có | Không | Không | Không |
| default/package-private | Có | Có | Không | Không |
| `protected` | Có | Có | Có qua inheritance | Không trực tiếp như public |
| `public` | Có | Có | Có | Có |

Ví dụ:

```java
public class User {
    private String password;
    protected String role;
    String internalCode; // package-private
}
```

Best practice:

- Field thường để `private`.
- Method public chỉ nên là API thật sự cần cho bên ngoài.
- Dùng package-private cho class/helper nội bộ trong cùng package.
- Không public mọi thứ chỉ để "cho dễ gọi", vì sau này khó kiểm soát thay đổi.

## 19. `static` là gì?

`static` nghĩa là thành phần thuộc về class, không thuộc riêng object instance.

```java
class Counter {
    static int total;
    int value;
}
```

- `total` dùng chung cho class `Counter`.
- `value` thuộc từng object `Counter`.

```java
Counter a = new Counter();
Counter b = new Counter();

a.total++;
b.total++;

System.out.println(Counter.total); // 2
```

`static` thường dùng cho:

- Constant: `public static final int MAX_SIZE = 100;`
- Utility method: `Math.max(a, b)`
- Factory method: `List.of(...)`
- Shared class-level data, nhưng cần cẩn thận thread-safety.

Không nên lạm dụng `static` cho business logic vì khó test và dễ tạo global state.

## 20. `final`, `finally`, `finalize()` khác nhau thế nào?

| Từ khóa/method | Ý nghĩa |
| --- | --- |
| `final` | Không cho gán lại biến, override method, hoặc extend class |
| `finally` | Block chạy sau `try/catch`, thường để cleanup |
| `finalize()` | Method cũ liên quan GC, đã deprecated, không nên dùng |

Ví dụ `final`:

```java
final int maxRetry = 3;
// maxRetry = 4; // lỗi compile
```

Ví dụ `finally`:

```java
try {
    process();
} finally {
    cleanup();
}
```

Không nên dùng `finalize()` để đóng file/socket/database connection. Dùng `try-with-resources`.

```java
try (InputStream input = Files.newInputStream(path)) {
    // read
}
```

## 21. `String` trong Java có gì đặc biệt?

`String` là immutable object. Sau khi tạo, nội dung string không đổi.

```java
String s = "Java";
s = s + " Core";
```

Dòng thứ hai không sửa object `"Java"` cũ. Nó tạo string mới và biến `s` trỏ sang object mới.

Lợi ích của immutable `String`:

- An toàn hơn khi chia sẻ giữa nhiều nơi.
- Có thể dùng làm key trong `HashMap`.
- Hỗ trợ string pool.
- Giảm rủi ro object bị sửa ngoài ý muốn.

Điểm cần nhớ: immutable không có nghĩa biến reference là immutable. Biến vẫn có thể trỏ sang string khác nếu không khai báo `final`.

## 22. String Pool là gì?

String pool là vùng JVM dùng để lưu các string literal và tái sử dụng object string giống nhau.

```java
String a = "java";
String b = "java";

System.out.println(a == b); // true
```

`a` và `b` cùng trỏ tới literal `"java"` trong string pool.

Nhưng:

```java
String c = new String("java");
System.out.println(a == c); // false
System.out.println(a.equals(c)); // true
```

`new String("java")` tạo object mới trên heap, nên reference khác.

`intern()` đưa string về pool:

```java
String d = c.intern();
System.out.println(a == d); // true
```

Trong code thực tế, không nên tự lạm dụng `intern()` nếu chưa hiểu memory trade-off. Quan trọng nhất là biết dùng `equals()` để so sánh nội dung string.

## 23. `String`, `StringBuilder`, `StringBuffer` khác nhau thế nào?

| Loại | Mutable? | Thread-safe? | Khi nào dùng |
| --- | --- | --- | --- |
| `String` | Không | An toàn vì immutable | Text ít thay đổi |
| `StringBuilder` | Có | Không | Ghép chuỗi trong một thread |
| `StringBuffer` | Có | Có synchronized | Code cũ hoặc cần mutable string thread-safe |

Ví dụ ghép chuỗi trong loop:

```java
StringBuilder builder = new StringBuilder();
for (String item : items) {
    builder.append(item).append(",");
}
String result = builder.toString();
```

Nếu dùng `String` cộng liên tục trong loop lớn, có thể tạo nhiều object trung gian không cần thiết.

## 24. `==` và `equals()` khác nhau thế nào?

`==` so sánh:

- Với primitive: so sánh giá trị.
- Với object: so sánh reference, tức hai biến có trỏ cùng object không.

`equals()` so sánh logic equality do class định nghĩa.

```java
String a = new String("java");
String b = new String("java");

System.out.println(a == b);      // false
System.out.println(a.equals(b)); // true
```

Với class tự định nghĩa, nếu không override `equals()`, implementation mặc định từ `Object` gần giống so sánh reference.

Ví dụ thực tế: hai `User` object lấy từ hai request khác nhau có thể cùng `id`. Nếu logic hệ thống coi cùng `id` là cùng user, cần override `equals()` theo `id`.

## 25. `equals()` và `hashCode()` có contract gì?

Contract quan trọng:

- Nếu `a.equals(b)` là `true`, thì `a.hashCode()` phải bằng `b.hashCode()`.
- Nếu `a.hashCode()` bằng `b.hashCode()`, chưa chắc `a.equals(b)` là `true`.
- Nếu object không đổi theo logic equality, `hashCode()` nên ổn định trong lúc object nằm trong hash collection.

Vì sao quan trọng? `HashMap` và `HashSet` dùng `hashCode()` để tìm bucket, rồi dùng `equals()` để so sánh object trong bucket.

Ví dụ lỗi:

```java
class User {
    Long id;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User other)) return false;
        return Objects.equals(id, other.id);
    }

    // quên hashCode()
}
```

Nếu quên `hashCode()`, hai user bằng nhau theo `equals()` có thể rơi vào bucket khác nhau, làm `HashSet` chứa trùng logic.

Nên dùng:

```java
@Override
public int hashCode() {
    return Objects.hash(id);
}
```

## 26. Vì sao không nên dùng object mutable làm key trong HashMap?

`HashMap` dùng `hashCode()` của key để xác định bucket. Nếu field dùng trong `hashCode()` bị đổi sau khi put vào map, object có thể đang nằm ở bucket cũ nhưng khi get lại tính ra bucket mới.

```java
Map<User, String> map = new HashMap<>();

User user = new User(1L, "a@test.com");
map.put(user, "active");

user.setEmail("b@test.com"); // nếu email nằm trong hashCode()

System.out.println(map.get(user)); // có thể null
```

Cách tránh:

- Dùng key immutable như `String`, `Long`, `UUID`.
- Nếu object làm key, field dùng trong `equals/hashCode` nên immutable.
- Không sửa key sau khi đưa vào `HashMap/HashSet`.

## 27. Shallow copy và deep copy khác nhau thế nào?

**Shallow copy** tạo object mới, nhưng các object con bên trong vẫn dùng chung reference. **Deep copy** tạo object mới và copy luôn các object con cần độc lập.

Ví dụ:

```java
class Order {
    List<String> items;
}
```

Nếu shallow copy `Order`, hai order có thể cùng trỏ tới một `items`. Sửa list ở bản copy thì bản gốc cũng thấy thay đổi.

| Tiêu chí | Shallow copy | Deep copy |
| --- | --- | --- |
| Object ngoài | Tạo mới | Tạo mới |
| Object con | Dùng chung | Tạo bản sao riêng |
| Tốc độ | Nhanh hơn | Chậm hơn |
| Memory | Ít hơn | Nhiều hơn |
| Rủi ro | Dễ bị ảnh hưởng lẫn nhau | Ít rủi ro hơn nếu cần độc lập |

Ví dụ thực tế: copy giỏ hàng. Nếu bản nháp order dùng chung list item với cart gốc, sửa order có thể làm cart đổi theo. Khi đó cần deep copy list item.

## 28. Exception trong Java là gì?

Exception là cơ chế báo lỗi bất thường trong quá trình chạy chương trình.

Java exception hierarchy cơ bản:

```text
Throwable
├── Error
└── Exception
    ├── Checked Exception
    └── RuntimeException (Unchecked Exception)
```

| Loại | Ví dụ | Ý nghĩa |
| --- | --- | --- |
| Checked exception | `IOException`, `SQLException` | Compile bắt xử lý hoặc khai báo |
| Unchecked exception | `NullPointerException`, `IllegalArgumentException` | Không bắt buộc catch |
| Error | `OutOfMemoryError`, `StackOverflowError` | Lỗi nghiêm trọng, thường không nên catch chung |

Ví dụ:

```java
try {
    String content = Files.readString(path);
} catch (IOException e) {
    throw new IllegalStateException("Cannot read file: " + path, e);
}
```

Best practice:

- Không catch exception rồi bỏ qua.
- Giữ cause gốc khi wrap exception.
- Message nên rõ ngữ cảnh.
- Không dùng exception cho flow bình thường nếu có cách kiểm tra rõ ràng.

## 29. Checked exception và unchecked exception khác nhau thế nào?

Checked exception bị compiler kiểm tra. Method gọi code có checked exception phải catch hoặc khai báo `throws`.

```java
void read() throws IOException {
    Files.readString(Path.of("input.txt"));
}
```

Unchecked exception là subclass của `RuntimeException`, compiler không bắt buộc xử lý.

```java
void validateAge(int age) {
    if (age < 0) {
        throw new IllegalArgumentException("age must be >= 0");
    }
}
```

Khi nào dùng:

- Checked exception: caller có khả năng xử lý hợp lý, ví dụ đọc file fail thì hỏi lại path khác.
- Unchecked exception: lỗi lập trình, input không hợp lệ, trạng thái chương trình sai.

Trong code application hiện đại, nhiều team ưu tiên unchecked exception cho business validation để API không bị ngập `throws`, nhưng vẫn cần nhất quán.

## 30. `throw` và `throws` khác nhau thế nào?

`throw` dùng để ném một exception cụ thể.

```java
throw new IllegalArgumentException("invalid age");
```

`throws` dùng trong method signature để khai báo method có thể ném exception.

```java
void importFile(Path path) throws IOException {
    Files.readString(path);
}
```

| Tiêu chí | `throw` | `throws` |
| --- | --- | --- |
| Vị trí | Trong body method | Trên khai báo method |
| Mục đích | Ném exception thật | Báo cho caller biết exception có thể xảy ra |
| Đi kèm | Object exception | Type exception |

## 31. `try-with-resources` là gì?

`try-with-resources` tự động đóng resource sau khi block kết thúc. Resource phải implement `AutoCloseable` hoặc `Closeable`.

```java
try (BufferedReader reader = Files.newBufferedReader(path)) {
    return reader.readLine();
}
```

Khi ra khỏi block, `reader.close()` được gọi tự động dù có exception hay không.

Resource thường gặp:

- File stream.
- Socket.
- Database connection/result set.
- Reader/writer.

Lợi ích:

- Tránh quên close resource.
- Code ngắn hơn `finally`.
- Xử lý suppressed exception tốt hơn so với tự viết thủ công.

## 32. Serialization là gì?

Serialization là quá trình chuyển object thành dạng có thể lưu hoặc truyền đi. Deserialization là chiều ngược lại.

Trong Java Core, native serialization dùng marker interface `Serializable`.

```java
class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
}
```

`serialVersionUID` dùng để kiểm tra compatibility giữa class khi serialize và deserialize. `transient` dùng để bỏ qua field khi serialize.

```java
class LoginData implements Serializable {
    private String username;
    private transient String rawPassword;
}
```

Ví dụ thực tế: không nên serialize password raw. Nếu object chứa token tạm hoặc dữ liệu nhạy cảm không nên lưu, dùng `transient` hoặc thiết kế DTO riêng.

Lưu ý: Java native serialization có rủi ro security nếu deserialize dữ liệu không tin cậy. Trong backend hiện đại, JSON/Avro/Protobuf thường phổ biến hơn cho giao tiếp hệ thống.

## 33. Collection Framework là gì?

Java Collection Framework là bộ interface và class để lưu, duyệt, tìm kiếm, sắp xếp dữ liệu.

| Interface | Ý nghĩa | Ví dụ implementation |
| --- | --- | --- |
| `List` | Danh sách có thứ tự, cho phép trùng | `ArrayList`, `LinkedList` |
| `Set` | Tập phần tử không trùng | `HashSet`, `LinkedHashSet`, `TreeSet` |
| `Queue` | Hàng đợi | `ArrayDeque`, `PriorityQueue` |
| `Deque` | Queue hai đầu | `ArrayDeque`, `LinkedList` |
| `Map` | Key-value | `HashMap`, `LinkedHashMap`, `TreeMap` |

```java
List<String> names = new ArrayList<>();
Set<String> uniqueTags = new HashSet<>();
Map<Long, User> usersById = new HashMap<>();
```

Tư duy chọn:

- Cần index và thứ tự: `List`.
- Cần unique: `Set`.
- Cần key-value: `Map`.
- Cần FIFO/LIFO: `Queue/Deque`.
- Cần sorted tự động: `TreeSet/TreeMap`.

## 34. Array và ArrayList khác nhau thế nào?

| Tiêu chí | Array | ArrayList |
| --- | --- | --- |
| Size | Cố định | Động |
| Primitive | Lưu trực tiếp được | Không, cần wrapper |
| Generic | Không như collection | Có `ArrayList<T>` |
| Access theo index | `O(1)` | `O(1)` |
| Insert/delete giữa | Không đổi size được | `O(n)` do shift |
| API tiện ích | Ít | Nhiều method hơn |

Array:

```java
int[] numbers = new int[3];
numbers[0] = 10;
```

ArrayList:

```java
List<Integer> numbers = new ArrayList<>();
numbers.add(10);
numbers.add(20);
```

`ArrayList` bên trong vẫn dùng array. Khi capacity không đủ, nó tạo array mới lớn hơn và copy phần tử sang.

Ví dụ thực tế:

- Dùng array khi size cố định, cần hiệu năng tốt, hoặc làm thuật toán.
- Dùng `ArrayList` khi danh sách thay đổi size và cần API tiện như `add`, `remove`, `contains`.

## 35. ArrayList và LinkedList khác nhau thế nào?

| Tiêu chí | ArrayList | LinkedList |
| --- | --- | --- |
| Cấu trúc bên trong | Dynamic array | Doubly linked list |
| Access theo index | `O(1)` | `O(n)` |
| Add cuối | Amortized `O(1)` | `O(1)` |
| Insert/delete giữa | `O(n)` do shift | `O(n)` để tìm node |
| Memory overhead | Ít hơn | Nhiều hơn vì node có prev/next |

Nhiều người nghĩ `LinkedList` luôn nhanh hơn khi insert/delete. Điều này không đúng trong nhiều trường hợp thực tế, vì muốn insert/delete ở giữa vẫn phải tìm tới node trước, mất `O(n)`.

Ví dụ thực tế:

- Danh sách user trả từ database: thường dùng `ArrayList`.
- Cần thao tác queue/deque hai đầu: dùng `ArrayDeque` thường tốt hơn `LinkedList`.

Trong phần lớn code Java application, `ArrayList` là lựa chọn mặc định tốt hơn nếu không có yêu cầu đặc biệt.

## 36. HashSet, LinkedHashSet và TreeSet khác nhau thế nào?

| Loại | Thứ tự duyệt | Cách xác định trùng | Complexity chính |
| --- | --- | --- | --- |
| `HashSet` | Không đảm bảo | `hashCode()` + `equals()` | Trung bình `O(1)` |
| `LinkedHashSet` | Theo thứ tự insert | `hashCode()` + `equals()` | Trung bình `O(1)` |
| `TreeSet` | Sorted | `compareTo()` hoặc `Comparator` | `O(log n)` |

```java
Set<String> set = new HashSet<>();
set.add("B");
set.add("A");
set.add("B");
System.out.println(set.size()); // 2
```

Chọn nhanh:

- Chỉ cần unique và nhanh: `HashSet`.
- Cần unique và giữ thứ tự thêm: `LinkedHashSet`.
- Cần unique và luôn sorted: `TreeSet`.

Lưu ý với `TreeSet`: nếu `compare()` trả về `0`, TreeSet xem hai phần tử là trùng, dù `equals()` có thể khác. Vì vậy comparator cần nhất quán với logic equality nếu dùng trong domain quan trọng.

## 37. HashMap hoạt động như thế nào?

`HashMap` lưu key-value bằng array bucket. Mỗi key được hash để xác định bucket.

Khi `put(key, value)`:

1. Gọi `hashCode()` của key.
2. Tính bucket index.
3. Nếu bucket trống, thêm entry.
4. Nếu bucket có entry, dùng `equals()` để xem key đã tồn tại chưa.
5. Nếu key đã tồn tại, update value; nếu chưa, thêm entry mới.

```java
Map<String, Integer> count = new HashMap<>();
count.put("java", 1);
count.put("java", 2);
System.out.println(count.get("java")); // 2
```

Điểm quan trọng:

- Trung bình `put/get/remove` là `O(1)`.
- Không đảm bảo thứ tự duyệt.
- Cho phép một key `null`.
- Không thread-safe.
- Khi nhiều key rơi vào cùng bucket thì xảy ra collision.
- Từ Java 8, bucket quá dài có thể chuyển từ linked list sang tree nếu đủ điều kiện.

Key nên là `Long`, `String`, `UUID` hoặc object immutable.

## 38. HashMap, LinkedHashMap, TreeMap và ConcurrentHashMap khác nhau thế nào?

| Loại | Thứ tự | Thread-safe? | Complexity chính | Khi nào dùng |
| --- | --- | --- | --- | --- |
| `HashMap` | Không đảm bảo | Không | `O(1)` trung bình | Map mặc định |
| `LinkedHashMap` | Insert order hoặc access order | Không | `O(1)` trung bình | Cần giữ thứ tự, LRU đơn giản |
| `TreeMap` | Sorted theo key | Không | `O(log n)` | Cần key sorted |
| `ConcurrentHashMap` | Không đảm bảo | Có cho concurrent access | `O(1)` trung bình | Nhiều thread đọc/ghi |

`LinkedHashMap` giữ thứ tự insert:

```java
Map<String, Integer> map = new LinkedHashMap<>();
map.put("A", 1);
map.put("C", 3);
map.put("B", 2);
System.out.println(map.keySet()); // [A, C, B]
```

`TreeMap` sorted theo key:

```java
Map<String, Integer> map = new TreeMap<>();
map.put("B", 2);
map.put("A", 1);
System.out.println(map.keySet()); // [A, B]
```

Không dùng `HashMap` thường cho shared mutable state giữa nhiều thread. Khi nhiều thread cùng ghi, có thể race condition hoặc dữ liệu không nhất quán.

## 39. Iterator là gì?

`Iterator` là object dùng để duyệt collection từng phần tử.

```java
Iterator<String> iterator = names.iterator();
while (iterator.hasNext()) {
    String name = iterator.next();
    System.out.println(name);
}
```

Nếu đang duyệt collection mà xóa trực tiếp từ collection, có thể gặp `ConcurrentModificationException`.

```java
for (String name : names) {
    // names.remove(name); // dễ lỗi
}
```

Nên xóa bằng iterator:

```java
Iterator<String> iterator = names.iterator();
while (iterator.hasNext()) {
    if (iterator.next().startsWith("A")) {
        iterator.remove();
    }
}
```

Hoặc dùng `removeIf()`:

```java
names.removeIf(name -> name.startsWith("A"));
```

## 40. Fail-fast và fail-safe iterator khác nhau thế nào?

Fail-fast iterator cố gắng phát hiện collection bị sửa trong lúc duyệt và ném `ConcurrentModificationException`.

Ví dụ thường gặp:

- `ArrayList`
- `HashMap`
- `HashSet`

Fail-safe hoặc weakly consistent iterator cho phép duyệt khi collection bị sửa, thường bằng cách duyệt snapshot hoặc cấu trúc concurrent.

Ví dụ:

- `CopyOnWriteArrayList`
- `ConcurrentHashMap`

```java
List<String> list = new CopyOnWriteArrayList<>();
list.add("A");
list.add("B");

for (String item : list) {
    list.add("C"); // không lỗi, nhưng iterator có thể không thấy C trong vòng hiện tại
}
```

`ConcurrentModificationException` không phải cơ chế bảo vệ thread-safety tuyệt đối. Nó là cơ chế phát hiện lỗi duyệt/sửa ở mức best-effort.

## 41. Comparable và Comparator khác nhau thế nào?

`Comparable` định nghĩa thứ tự tự nhiên ngay trong class.

```java
class User implements Comparable<User> {
    private Long id;

    @Override
    public int compareTo(User other) {
        return this.id.compareTo(other.id);
    }
}
```

`Comparator` định nghĩa cách sort từ bên ngoài.

```java
users.sort(Comparator.comparing(User::getName)
        .thenComparing(User::getId));
```

| Tiêu chí | Comparable | Comparator |
| --- | --- | --- |
| Method | `compareTo()` | `compare()` |
| Vị trí | Trong class | Bên ngoài class |
| Số cách sort | Một natural order chính | Nhiều cách sort khác nhau |

Không nên viết compare bằng phép trừ vì có thể overflow.

```java
return Integer.compare(a.getAge(), b.getAge());
```

## 42. Generics là gì?

Generics cho phép class, interface, method làm việc với type tham số, giúp type-safe hơn và giảm cast thủ công.

Không dùng generic:

```java
List list = new ArrayList();
list.add("Java");
String value = (String) list.get(0);
```

Dùng generic:

```java
List<String> list = new ArrayList<>();
list.add("Java");
String value = list.get(0);
```

Lợi ích:

- Compiler kiểm tra type sớm.
- Ít lỗi `ClassCastException` runtime.
- Code dễ đọc hơn.

```java
Map<Long, User> usersById = new HashMap<>();
```

Nhìn type là biết key là `Long`, value là `User`.

## 43. Type erasure là gì?

Java generics chủ yếu tồn tại ở compile time. Sau compile, nhiều thông tin generic bị xóa. Cơ chế này gọi là type erasure.

```java
List<String> names = new ArrayList<>();
List<Integer> numbers = new ArrayList<>();

System.out.println(names.getClass() == numbers.getClass()); // true
```

Runtime đều thấy chủ yếu là `ArrayList`, không phải hai class riêng `ArrayList<String>` và `ArrayList<Integer>`.

Hệ quả:

- Không thể `new T()` trực tiếp.
- Không thể tạo generic array như `new List<String>[10]`.
- Không overload hai method chỉ khác generic parameter.

```java
// lỗi vì sau erasure signature bị trùng
// void process(List<String> names) {}
// void process(List<Integer> numbers) {}
```

Type erasure giúp Java giữ backward compatibility với code trước khi generics xuất hiện.

## 44. Wildcard `? extends` và `? super` dùng khi nào?

Quy tắc dễ nhớ: **PECS**.

```text
Producer Extends, Consumer Super
```

Nếu collection chủ yếu dùng để đọc ra `T`, dùng `? extends T`.

```java
double sum(List<? extends Number> numbers) {
    double total = 0;
    for (Number n : numbers) {
        total += n.doubleValue();
    }
    return total;
}
```

`List<? extends Number>` có thể nhận `List<Integer>`, `List<Double>`,...

Nếu collection chủ yếu dùng để ghi `T` vào, dùng `? super T`.

```java
void addIntegers(List<? super Integer> numbers) {
    numbers.add(1);
    numbers.add(2);
}
```

Ví dụ thực tế: API copy dữ liệu từ source sang destination.

```java
void copy(List<? extends Number> source, List<? super Number> destination) {
    destination.addAll(source);
}
```

## 45. Lambda expression là gì?

Lambda là cú pháp ngắn để tạo implementation cho functional interface.

Functional interface là interface có đúng một abstract method.

```java
@FunctionalInterface
interface Converter {
    int convert(String value);
}
```

Dùng anonymous class:

```java
Converter converter = new Converter() {
    public int convert(String value) {
        return Integer.parseInt(value);
    }
};
```

Dùng lambda:

```java
Converter converter = value -> Integer.parseInt(value);
```

Ví dụ thực tế:

```java
users.sort((a, b) -> a.getName().compareTo(b.getName()));
```

Lambda giúp code ngắn hơn khi truyền behavior vào method, đặc biệt với collection, stream, callback.

## 46. Functional interface là gì?

Functional interface là interface có một abstract method, dùng làm target type cho lambda/method reference.

| Interface | Method | Ý nghĩa |
| --- | --- | --- |
| `Predicate<T>` | `boolean test(T t)` | Kiểm tra điều kiện |
| `Function<T, R>` | `R apply(T t)` | Chuyển T thành R |
| `Consumer<T>` | `void accept(T t)` | Nhận T và xử lý |
| `Supplier<T>` | `T get()` | Cung cấp T |
| `Runnable` | `void run()` | Task không input/output |

```java
Predicate<User> activeUser = user -> user.isActive();
Function<User, String> userName = user -> user.getName();
```

Annotation `@FunctionalInterface` không bắt buộc, nhưng nên dùng để compiler kiểm tra interface có đúng một abstract method.

## 47. Stream API là gì?

Stream API giúp xử lý dữ liệu theo pipeline: source -> intermediate operations -> terminal operation.

```java
List<String> names = users.stream()
        .filter(User::isActive)
        .map(User::getName)
        .sorted()
        .toList();
```

Trong ví dụ:

- `users` là source.
- `filter`, `map`, `sorted` là intermediate operations.
- `toList()` là terminal operation.

Điểm cần nhớ:

- Stream không lưu dữ liệu, nó xử lý luồng dữ liệu từ source.
- Intermediate operation thường lazy, chỉ chạy khi có terminal operation.
- Stream nên tránh side effect phức tạp.
- Không phải cứ dùng stream là tốt hơn loop.

Ví dụ thực tế: lấy danh sách email của user active từ danh sách user đã có trong memory.

## 48. `map()` và `flatMap()` khác nhau thế nào?

`map()` chuyển mỗi phần tử thành một giá trị khác.

```java
List<String> names = users.stream()
        .map(User::getName)
        .toList();
```

`flatMap()` chuyển mỗi phần tử thành một stream rồi làm phẳng kết quả.

```java
List<Item> items = orders.stream()
        .flatMap(order -> order.getItems().stream())
        .toList();
```

Nếu dùng `map()`:

```java
Stream<List<Item>> result = orders.stream()
        .map(Order::getItems);
```

Kết quả là stream của list item, chưa phải stream item.

Ví dụ thực tế: từ danh sách order, cần gom tất cả item để tính tổng số lượng bán. Đây là trường hợp hợp với `flatMap()`.

## 49. Optional là gì? Nên dùng thế nào?

`Optional<T>` là container biểu diễn giá trị có thể có hoặc không.

```java
Optional<User> user = findUserById(id);
```

Mục tiêu của `Optional` là làm việc với kết quả có thể vắng mặt rõ ràng hơn thay vì trả về `null`.

```java
String name = findUserById(id)
        .map(User::getName)
        .orElse("Unknown");
```

Nên dùng:

- Return type của method query có thể không có kết quả.
- Pipeline xử lý giá trị có thể thiếu.

Không nên lạm dụng:

- Không nên dùng `Optional` làm field entity/DTO thông thường.
- Không nên dùng `Optional` cho parameter chỉ để thay `null`.
- Không gọi `get()` trực tiếp nếu chưa check `isPresent()`.

## 50. Class loading trong JVM diễn ra như thế nào?

Class loading là quá trình JVM tải class vào memory khi cần dùng.

Các giai đoạn chính:

```text
Loading -> Linking -> Initialization
```

### Loading

ClassLoader đọc bytecode `.class` từ classpath, jar hoặc nguồn khác, rồi tạo metadata class trong JVM.

### Linking

Linking gồm:

- **Verification:** kiểm tra bytecode hợp lệ.
- **Preparation:** cấp phát và gán giá trị mặc định cho static field.
- **Resolution:** chuyển symbolic reference thành direct reference khi cần.

### Initialization

JVM chạy static initializer và gán giá trị thật cho static field.

```java
class Config {
    static int timeout = 30;

    static {
        System.out.println("init Config");
    }
}
```

Nếu class `Config` chưa được dùng, JVM có thể chưa initialize nó. Khi gọi `Config.timeout`, class được active use và initialization xảy ra.

## 51. ClassLoader trong Java là gì?

ClassLoader là thành phần chịu trách nhiệm tải class vào JVM.

| ClassLoader | Vai trò |
| --- | --- |
| Bootstrap ClassLoader | Tải class lõi của Java |
| Platform ClassLoader | Tải class platform/module |
| Application ClassLoader | Tải class từ application classpath/module path |
| Custom ClassLoader | Tải class theo cách riêng |

Java dùng parent delegation model: class loader thường hỏi parent trước, nếu parent không tải được thì mới tự tải.

Mục đích:

- Tránh class lõi bị thay thế tùy tiện.
- Chia vùng tải class theo tầng.
- Hỗ trợ plugin/container/custom runtime.

Ví dụ thực tế: application server, test framework, build tool hoặc plugin system có thể dùng class loader riêng để cô lập dependency.

## 52. JVM memory gồm những vùng nào?

Các vùng thường được hỏi:

| Vùng nhớ | Lưu gì | Ghi chú |
| --- | --- | --- |
| Heap | Object và array | Chia sẻ giữa thread, GC quản lý |
| Stack | Stack frame của method call | Mỗi thread có stack riêng |
| Metaspace | Metadata class | Nằm trong native memory từ Java 8 |
| PC Register | Vị trí instruction đang chạy | Mỗi thread có một |
| Native Method Stack | Stack cho native method | Dùng khi gọi JNI/native |

Ví dụ:

```java
void process() {
    int age = 20;
    User user = new User();
}
```

- `age` là local primitive trong stack frame.
- `user` là local reference trong stack frame.
- Object `new User()` nằm trên heap.

## 53. Heap và Stack khác nhau thế nào?

| Tiêu chí | Stack | Heap |
| --- | --- | --- |
| Lưu gì | Local variable, method call frame, reference local | Object và array |
| Phạm vi | Mỗi thread có stack riêng | Chia sẻ giữa các thread |
| Quản lý | Tự push/pop theo method call | GC quản lý |
| Tốc độ | Thường nhanh hơn | Chậm hơn stack |
| Lỗi thường gặp | `StackOverflowError` | `OutOfMemoryError` |

Stack hoạt động theo method call. Khi method được gọi, JVM tạo stack frame. Khi method kết thúc, frame bị pop.

Heap chứa object sống độc lập với method call, miễn là còn reference tới object đó.

```java
User createUser() {
    User user = new User();
    return user;
}
```

Object `User` vẫn có thể sống sau khi method kết thúc nếu caller nhận reference trả về.

## 54. Garbage Collection là gì?

Garbage Collection (GC) là cơ chế JVM thu hồi memory của object không còn reachable.

Object còn reachable nếu có đường tham chiếu từ GC roots tới object đó.

GC roots thường gồm:

- Local variable trong stack frame đang chạy.
- Static field.
- Thread đang sống.
- JNI reference.

```java
User user = new User();
user = null;
```

Nếu không còn reference nào khác tới object `User`, object đó có thể được GC thu hồi.

Điểm cần nhớ:

- GC không chạy ngay lập tức khi object không còn dùng.
- `System.gc()` chỉ là gợi ý, không nên phụ thuộc.
- Java vẫn có thể memory leak nếu chương trình còn giữ reference không cần thiết.

## 55. Strong, soft, weak và phantom reference là gì?

Java có nhiều mức reference ảnh hưởng tới GC.

| Loại reference | Ý nghĩa |
| --- | --- |
| Strong reference | Reference bình thường; còn strong reference thì object chưa bị GC |
| Soft reference | Có thể bị GC khi JVM thiếu memory |
| Weak reference | Có thể bị GC ở lần GC tiếp theo nếu không còn strong reference |
| Phantom reference | Dùng để theo dõi object sắp bị thu hồi, không lấy lại object được |

Ví dụ strong reference:

```java
User user = new User();
```

Ví dụ weak reference:

```java
WeakReference<User> ref = new WeakReference<>(new User());
User user = ref.get();
```

Ví dụ thực tế: `WeakHashMap` dùng weak reference cho key. Khi key không còn được tham chiếu mạnh ở nơi khác, entry có thể bị GC dọn.

Không cần dùng các reference đặc biệt trong code hằng ngày, nhưng nên biết concept khi làm cache, framework, memory-sensitive library.

## 56. Memory leak trong Java là gì?

Memory leak trong Java xảy ra khi object không còn cần thiết nhưng vẫn còn reference tới nó, khiến GC không thu hồi được.

```java
class Cache {
    static Map<String, byte[]> data = new HashMap<>();
}
```

Nếu map này cứ thêm dữ liệu và không bao giờ xóa, heap sẽ tăng dần.

Các nguyên nhân phổ biến:

- Static collection giữ object lâu hơn cần thiết.
- Cache không giới hạn.
- Listener/callback không unregister.
- `ThreadLocal` không gọi `remove()`.
- Inner class/lambda giữ reference tới object lớn.
- Resource không đóng làm giữ buffer/native memory.

Cách giảm:

- Dùng cache có max size/TTL.
- Clear collection khi không dùng.
- Unregister listener.
- Remove ThreadLocal trong `finally`.
- Dùng profiler/heap dump để tìm object giữ memory.

## 57. Thread là gì?

Thread là đơn vị thực thi trong process. Một Java application có thể có nhiều thread chạy cùng lúc.

```java
Thread thread = new Thread(() -> {
    System.out.println("run task");
});
thread.start();
```

Lưu ý: gọi `start()` mới tạo luồng thực thi mới. Gọi `run()` trực tiếp chỉ là gọi method bình thường trên thread hiện tại.

```java
thread.run(); // không tạo thread mới
```

Tạo thread thủ công nhiều quá có thể tốn memory và context switching. Trong production thường dùng executor/thread pool hoặc virtual thread tùy use case.

## 58. Thread và Runnable khác nhau thế nào?

`Thread` đại diện cho luồng thực thi. `Runnable` đại diện cho task cần chạy.

```java
Runnable task = () -> System.out.println("task");
Thread thread = new Thread(task);
thread.start();
```

| Tiêu chí | Extend Thread | Implement Runnable |
| --- | --- | --- |
| Bản chất | Vừa là thread vừa chứa task | Chỉ mô tả task |
| Kế thừa | Không extends class khác được | Linh hoạt hơn |
| Tái sử dụng | Kém hơn | Dễ đưa vào executor/thread khác |
| Khuyến nghị | Ít dùng | Nên dùng hơn |

Ví dụ thực tế: nếu cần gửi 100 email, không nên tạo 100 class extends `Thread`. Nên tạo task `Runnable/Callable` và đưa vào executor.

## 59. Thread lifecycle gồm những trạng thái nào?

Các trạng thái chính trong `Thread.State`:

| State | Ý nghĩa |
| --- | --- |
| `NEW` | Thread object đã tạo nhưng chưa `start()` |
| `RUNNABLE` | Đang sẵn sàng chạy hoặc đang chạy |
| `BLOCKED` | Chờ lấy monitor lock |
| `WAITING` | Chờ vô thời hạn |
| `TIMED_WAITING` | Chờ có thời hạn |
| `TERMINATED` | Đã kết thúc |

```java
Thread thread = new Thread(() -> {});
System.out.println(thread.getState()); // NEW
thread.start();
```

Khi debug thread dump:

- Nhiều thread `BLOCKED`: có thể đang tranh lock.
- Nhiều thread `WAITING`: có thể đang chờ queue/latch/condition.
- Nhiều thread `TIMED_WAITING`: có thể đang sleep, wait timeout, hoặc chờ I/O timeout.

## 60. `sleep()`, `wait()`, `notify()`, `notifyAll()` khác nhau thế nào?

| Method | Thuộc class | Có cần synchronized? | Có nhả lock không? |
| --- | --- | --- | --- |
| `sleep()` | `Thread` | Không | Không |
| `wait()` | `Object` | Có | Có |
| `notify()` | `Object` | Có | Không nhả ngay |
| `notifyAll()` | `Object` | Có | Không nhả ngay |

`sleep()` chỉ tạm dừng thread hiện tại trong một khoảng thời gian.

```java
Thread.sleep(1000);
```

`wait()` khiến thread chờ trên monitor của object và nhả lock.

```java
synchronized (lock) {
    while (!ready) {
        lock.wait();
    }
}
```

Nên dùng `while` khi wait để kiểm tra lại condition sau khi tỉnh dậy.

Trong code hiện đại, thường ưu tiên abstraction như `BlockingQueue`, `CountDownLatch`, `Semaphore`, `CompletableFuture` thay vì tự viết `wait/notify`.

## 61. `synchronized` là gì?

`synchronized` dùng để bảo vệ critical section, đảm bảo tại một thời điểm chỉ một thread giữ monitor lock tương ứng.

```java
class Counter {
    private int count;

    public synchronized void increment() {
        count++;
    }
}
```

Nếu nhiều thread cùng gọi `increment()`, mỗi lần chỉ một thread được vào method.

`synchronized` giải quyết hai vấn đề:

- **Mutual exclusion:** không cho nhiều thread cùng sửa shared state cùng lúc.
- **Visibility:** thay đổi của thread này được thread khác thấy đúng khi vào/ra synchronized theo cùng lock.

Không nên synchronized quá rộng vì làm giảm concurrency. Chỉ khóa phần cần bảo vệ shared state.

## 62. `volatile` là gì?

`volatile` đảm bảo visibility của biến giữa các thread. Khi một thread ghi biến volatile, thread khác đọc sẽ thấy giá trị mới hơn theo rule của Java Memory Model.

```java
class Worker {
    private volatile boolean running = true;

    void stop() {
        running = false;
    }

    void run() {
        while (running) {
            // do work
        }
    }
}
```

`volatile` phù hợp với flag trạng thái đơn giản.

Nhưng `volatile` không làm thao tác phức tạp thành atomic.

```java
volatile int count = 0;
count++; // vẫn không atomic
```

Nếu cần atomic counter, dùng `AtomicInteger`.

```java
AtomicInteger count = new AtomicInteger();
count.incrementAndGet();
```

## 63. Race condition là gì?

Race condition xảy ra khi kết quả chương trình phụ thuộc vào thứ tự chạy không kiểm soát được giữa nhiều thread.

```java
class Counter {
    int count = 0;

    void increment() {
        count++;
    }
}
```

Nếu hai thread cùng gọi `increment()`:

```text
Thread A đọc count = 0
Thread B đọc count = 0
Thread A ghi count = 1
Thread B ghi count = 1
```

Kết quả đúng phải là `2`, nhưng thực tế là `1`.

Cách xử lý:

- Dùng `synchronized`.
- Dùng `Lock`.
- Dùng atomic class như `AtomicInteger`.
- Tránh shared mutable state nếu có thể.

## 64. Deadlock là gì?

Deadlock xảy ra khi các thread chờ nhau giữ lock và không thread nào tiếp tục được.

```text
Thread A giữ lock1, chờ lock2
Thread B giữ lock2, chờ lock1
```

Nếu một chỗ khóa `lock1 -> lock2`, chỗ khác khóa `lock2 -> lock1`, có thể deadlock.

Cách giảm rủi ro:

- Luôn lấy lock theo cùng một thứ tự.
- Giữ lock trong thời gian ngắn.
- Tránh gọi code bên ngoài khi đang giữ lock.
- Dùng `tryLock()` với timeout nếu phù hợp.
- Dùng thread dump để phân tích khi production bị treo.

## 65. ExecutorService và ThreadPool là gì?

`ExecutorService` quản lý việc chạy task bằng một nhóm thread. Thay vì tự tạo thread cho từng task, mình submit task vào executor.

```java
ExecutorService executor = Executors.newFixedThreadPool(4);

Future<Integer> future = executor.submit(() -> calculate());
Integer result = future.get();

executor.shutdown();
```

Lợi ích:

- Tái sử dụng thread.
- Giới hạn số thread chạy đồng thời.
- Quản lý queue task.
- Nhận kết quả qua `Future`.

Ví dụ thực tế: xử lý 1000 file. Nếu tạo 1000 thread, hệ thống dễ quá tải. Dùng pool 8 thread để xử lý dần giúp ổn định hơn.

Lưu ý: cần shutdown executor khi không dùng nữa, nếu không application có thể không thoát.

## 66. Runnable và Callable khác nhau thế nào?

| Tiêu chí | Runnable | Callable |
| --- | --- | --- |
| Method | `run()` | `call()` |
| Return value | Không | Có |
| Checked exception | Không khai báo trực tiếp | Có thể throws |
| Dùng với | `Thread`, `ExecutorService` | `ExecutorService` |

`Runnable`:

```java
Runnable task = () -> System.out.println("send email");
```

`Callable`:

```java
Callable<Integer> task = () -> calculateScore();
Future<Integer> future = executor.submit(task);
Integer score = future.get();
```

Ví dụ thực tế:

- Ghi log async không cần kết quả: `Runnable`.
- Gọi API tính phí và cần lấy kết quả: `Callable`.

## 67. Future và CompletableFuture khác nhau thế nào?

`Future` đại diện cho kết quả sẽ có trong tương lai, nhưng API khá hạn chế. Muốn lấy kết quả thường gọi `get()`, có thể block thread hiện tại.

```java
Future<String> future = executor.submit(() -> callApi());
String result = future.get();
```

`CompletableFuture` hỗ trợ pipeline async tốt hơn.

```java
CompletableFuture.supplyAsync(() -> callApi())
        .thenApply(response -> parse(response))
        .thenAccept(data -> save(data));
```

| Tiêu chí | Future | CompletableFuture |
| --- | --- | --- |
| Lấy kết quả | `get()` thường block | Có thể chain callback |
| Compose nhiều task | Khó | Dễ hơn với `thenCompose`, `thenCombine` |
| Hoàn thành thủ công | Không tiện | Có thể `complete()` |
| Exception handling | Qua `get()` | `exceptionally`, `handle`, `whenComplete` |

Ví dụ thực tế: gọi hai API độc lập rồi gộp kết quả có thể dùng `CompletableFuture.thenCombine()`.

## 68. AtomicInteger là gì?

`AtomicInteger` là class hỗ trợ thao tác int atomic mà không cần tự dùng `synchronized` trong nhiều trường hợp đơn giản.

```java
AtomicInteger counter = new AtomicInteger();
counter.incrementAndGet();
```

Các thao tác như increment, compare-and-set được thực hiện thread-safe.

Ví dụ thực tế: đếm số request đang xử lý trong memory.

```java
AtomicInteger activeRequests = new AtomicInteger();

activeRequests.incrementAndGet();
try {
    process();
} finally {
    activeRequests.decrementAndGet();
}
```

Lưu ý: atomic class tốt cho biến đơn lẻ. Nếu cần cập nhật nhiều field cùng lúc theo một invariant, vẫn cần lock hoặc thiết kế khác.

## 69. ThreadLocal là gì?

`ThreadLocal` cho phép mỗi thread có một bản copy riêng của biến.

```java
ThreadLocal<String> currentUser = new ThreadLocal<>();

try {
    currentUser.set("tony");
    String user = currentUser.get();
} finally {
    currentUser.remove();
}
```

Mỗi thread đọc `currentUser.get()` sẽ nhận giá trị riêng của thread đó.

Use case:

- Lưu context tạm theo thread.
- Trace id/log correlation id.
- Formatter không thread-safe trong code cũ.

Rủi ro:

- Trong thread pool, thread được tái sử dụng. Nếu không `remove()`, dữ liệu request cũ có thể còn lại và gây leak hoặc sai context.

## 70. Immutable object là gì?

Immutable object là object sau khi tạo thì state không thay đổi.

Ví dụ `String`, `Integer`, `BigDecimal` là immutable.

Tự viết immutable class:

```java
final class Money {
    private final BigDecimal amount;

    Money(BigDecimal amount) {
        this.amount = amount;
    }

    BigDecimal amount() {
        return amount;
    }
}
```

Nguyên tắc:

- Class nên `final` hoặc không cho subclass phá invariant.
- Field là `private final`.
- Không có setter.
- Nếu field là mutable object, cần defensive copy.

Ví dụ thực tế: object `Money`, `Email`, `DateRange` nên immutable để tránh bị sửa ngoài ý muốn.

## 71. Defensive copy là gì?

Defensive copy là tạo bản sao khi nhận hoặc trả về object mutable để tránh bên ngoài sửa state bên trong class.

Ví dụ không an toàn:

```java
class Team {
    private final List<String> members;

    Team(List<String> members) {
        this.members = members;
    }

    List<String> getMembers() {
        return members;
    }
}
```

Code bên ngoài vẫn có thể sửa list.

Cách tốt hơn:

```java
class Team {
    private final List<String> members;

    Team(List<String> members) {
        this.members = List.copyOf(members);
    }

    List<String> getMembers() {
        return members;
    }
}
```

Ví dụ thực tế: class cấu hình nhận danh sách permission. Nếu trả thẳng list mutable, code khác có thể thêm quyền ngoài luồng kiểm soát.

## 72. Enum trong Java là gì?

Enum dùng để biểu diễn một tập giá trị cố định.

```java
enum OrderStatus {
    NEW,
    PAID,
    CANCELLED
}
```

Lợi ích:

- Type-safe hơn string constant.
- Tránh typo như `"PAIDD"`.
- Có thể có field, constructor, method.
- Dùng tốt trong `switch`.

Enum có thể có field:

```java
enum Role {
    ADMIN("Administrator"),
    USER("Normal user");

    private final String description;

    Role(String description) {
        this.description = description;
    }
}
```

Ví dụ thực tế: status đơn hàng, role user, loại thanh toán, mức log.

## 73. Record trong Java là gì?

Record là loại class ngắn gọn để chứa dữ liệu immutable dạng carrier.

```java
record UserDto(Long id, String name) {
}
```

Compiler tự sinh:

- Constructor.
- Accessor `id()`, `name()`.
- `equals()`.
- `hashCode()`.
- `toString()`.

Record phù hợp cho:

- DTO.
- Response object đơn giản.
- Value object đơn giản.
- Key tổng hợp trong map.

Lưu ý:

- Record field là final theo concept.
- Record không phù hợp cho entity có lifecycle phức tạp, mutable state nhiều.
- Nếu field bên trong là mutable object như `List`, cần cân nhắc defensive copy trong compact constructor.

## 74. Annotation trong Java là gì?

Annotation là metadata gắn vào class, method, field, parameter,... để compiler, tool hoặc runtime đọc và xử lý.

```java
@Override
public String toString() {
    return "User";
}
```

`@Override` giúp compiler kiểm tra method thật sự override method cha.

Annotation phổ biến trong Java Core:

- `@Override`
- `@Deprecated`
- `@SuppressWarnings`
- `@FunctionalInterface`

Annotation tự nó không làm gì nếu không có compiler/tool/runtime code đọc annotation đó.

## 75. Reflection là gì?

Reflection cho phép chương trình kiểm tra và thao tác class, method, field tại runtime.

```java
Class<?> clazz = User.class;
System.out.println(clazz.getName());
```

Có thể lấy method:

```java
Method method = clazz.getDeclaredMethod("getName");
```

Use case:

- Framework mapping object.
- Serialization/deserialization.
- Dependency injection framework.
- Testing tool.
- Annotation processing runtime.

Nhược điểm:

- Chậm hơn gọi trực tiếp.
- Dễ phá encapsulation nếu set accessible.
- Lỗi chuyển từ compile time sang runtime.
- Khó đọc và khó refactor hơn.

Ví dụ thực tế: thư viện JSON có thể dùng reflection để đọc field/getter của object và convert sang JSON.

## 76. Package, import và module khác nhau thế nào?

`package` là namespace để tổ chức class.

```java
package com.example.user;
```

`import` giúp dùng class ở package khác mà không cần viết full name.

```java
import java.util.List;
```

`module` là đơn vị đóng gói lớn hơn package, xuất hiện từ Java 9 trong Java Platform Module System.

```java
module com.example.app {
    requires java.base;
    exports com.example.api;
}
```

Trong code hằng ngày:

- Package giúp tổ chức source code.
- Import giúp code ngắn hơn.
- Module dùng khi muốn kiểm soát dependency/export ở mức module, thường gặp trong library hoặc project modularized.

## 77. `var` trong Java là gì?

`var` cho phép compiler tự suy luận type của local variable.

```java
var name = "Tony"; // String
var count = 10;    // int
```

`var` chỉ dùng cho local variable có initializer. Không dùng cho field, method parameter, return type thông thường.

Không hợp lệ:

```java
// var value; // không biết type
```

Nên dùng khi type rõ từ vế phải:

```java
var users = new ArrayList<User>();
```

Không nên dùng nếu làm code khó đọc:

```java
var result = service.process(data);
```

`var` không biến Java thành dynamic language. Type vẫn được xác định tại compile time.

## 78. `switch` hiện đại trong Java có gì mới?

Java hiện đại hỗ trợ switch expression, có thể trả về giá trị.

```java
String label = switch (status) {
    case NEW -> "New order";
    case PAID -> "Paid order";
    case CANCELLED -> "Cancelled order";
};
```

So với switch cũ:

- Ít cần `break`.
- Có thể gán kết quả trực tiếp.
- Code ngắn và ít lỗi fall-through hơn.

Nhiều dòng dùng `yield`:

```java
String label = switch (status) {
    case NEW -> "New";
    case PAID -> {
        logPaid();
        yield "Paid";
    }
    default -> "Unknown";
};
```

Ví dụ thực tế: convert enum status sang label hiển thị hoặc xử lý nhánh theo loại command.

## 79. Sealed class là gì?

Sealed class/interface giới hạn những class nào được phép kế thừa/implement.

```java
sealed interface PaymentResult permits Success, Failure {
}

final class Success implements PaymentResult {
}

final class Failure implements PaymentResult {
}
```

Lợi ích:

- Kiểm soát hierarchy.
- Compiler biết tập subclass hợp lệ.
- Hợp với domain có số loại cố định.

Subclass của sealed type phải khai báo một trong:

- `final`
- `sealed`
- `non-sealed`

Ví dụ thực tế: kết quả xử lý có thể chỉ là `Success`, `ValidationFailure`, `SystemFailure`. Sealed interface giúp domain model rõ hơn.

## 80. Virtual thread là gì?

Virtual thread là lightweight thread trong Java hiện đại, giúp chạy số lượng lớn tác vụ blocking I/O với chi phí thấp hơn platform thread truyền thống.

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> callExternalApi());
}
```

Phù hợp khi:

- Có nhiều task chờ I/O.
- Code đang viết theo style blocking.
- Muốn tăng khả năng phục vụ nhiều request/task đồng thời mà không chuyển toàn bộ sang reactive.

Không phù hợp để làm CPU-bound task nhanh hơn. Nếu task chủ yếu tính toán CPU, số core vẫn là giới hạn chính.

Ví dụ thực tế: service gọi nhiều API bên ngoài hoặc đọc nhiều file/network I/O có thể hưởng lợi từ virtual thread, nhưng vẫn phải giới hạn connection pool, timeout và rate limit.

## Checklist ôn Java Core

- Nắm chắc primitive/reference, wrapper, boxing/unboxing, `==` vs `equals()`.
- Hiểu OOP: encapsulation, inheritance, polymorphism, abstraction.
- Biết khi nào dùng interface, abstract class, composition.
- Nắm `String`, string pool, immutability, `StringBuilder`.
- Hiểu contract `equals/hashCode` và tác động tới `HashMap/HashSet`.
- Biết collection chính: `ArrayList`, `LinkedList`, `HashSet`, `TreeSet`, `HashMap`, `TreeMap`, `ConcurrentHashMap`.
- Nắm generics, type erasure, wildcard `extends/super`.
- Biết exception hierarchy, checked/unchecked, try-with-resources.
- Hiểu JVM ở mức class loading, heap/stack/metaspace, GC, memory leak.
- Nắm concurrency: thread, lifecycle, synchronized, volatile, race condition, deadlock, executor, atomic, ThreadLocal.
- Biết các feature Java hiện đại ở mức concept: record, switch expression, sealed class, virtual thread.
