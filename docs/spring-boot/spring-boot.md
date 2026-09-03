# Spring Boot

## Spring là gì?

Spring là một hệ sinh thái dành cho Java, trong đó **Spring Framework** là nền tảng chính. Spring giúp xây dựng ứng dụng từ nhiều object và module có thể phối hợp với nhau, đồng thời cung cấp sẵn cách xử lý nhiều vấn đề thường gặp của ứng dụng backend.

Spring không phải ngôn ngữ lập trình, web server, database hay ORM. Nó là framework chạy bên trong ứng dụng Java và có thể tích hợp với các công nghệ như servlet container, JDBC, JPA/Hibernate, database, cache và message broker.

### Spring có những thành phần quan trọng nào?

- **Core Container:** quản lý bean, vòng đời object và Dependency Injection thông qua `ApplicationContext`.
- **Spring MVC:** xử lý HTTP request, controller, REST API, validation và response.
- **Spring AOP:** hỗ trợ áp dụng các hành vi dùng chung như transaction, security hoặc cache thông qua proxy.
- **Data Access và Transaction:** hỗ trợ JDBC, transaction và tích hợp JPA/Hibernate.
- **Spring Testing:** hỗ trợ test các thành phần sử dụng Spring context và Spring MVC.

Ngoài Spring Framework còn có các project riêng trong hệ sinh thái:

- **Spring Data:** hỗ trợ truy cập nhiều loại database;
- **Spring Security:** authentication và authorization;
- **Spring Batch:** xử lý batch job;
- **Spring Cloud:** hỗ trợ một số bài toán của hệ thống phân tán;
- **Spring Boot:** đơn giản hóa việc cấu hình, khởi động và triển khai ứng dụng Spring.

Một project không bắt buộc phải dùng tất cả các thành phần trên. Chỉ nên thêm những module thực sự cần.

### Lợi ích của Spring

- Quản lý object và dependency tập trung bằng IoC Container và Dependency Injection.
- Giảm việc các class tự tạo và phụ thuộc chặt vào implementation cụ thể.
- Cung cấp cách làm tương đối thống nhất cho web, transaction, validation, security và data access.
- Dễ thay implementation và viết unit test hơn khi dependency được thiết kế rõ ràng.
- Có hệ sinh thái lớn, tài liệu nhiều và được sử dụng phổ biến trong backend Java.
- Tích hợp được với nhiều thư viện và hệ thống khác.

### Bất cập của Spring

- Có nhiều khái niệm cần học như bean, IoC, Dependency Injection, proxy, transaction và application context.
- Luồng chạy đôi khi không nhìn thấy trực tiếp vì framework tạo object và áp dụng proxy ở runtime.
- Cấu hình sai bean hoặc dependency có thể làm ứng dụng không khởi động được.
- Sử dụng annotation mà không hiểu cơ chế bên dưới dễ gây lỗi, đặc biệt với `@Transactional`, lazy loading và security.
- Dùng quá nhiều module cho một ứng dụng nhỏ có thể làm project phức tạp và tốn tài nguyên hơn cần thiết.
- Spring Framework thuần vẫn cần khá nhiều cấu hình và quản lý dependency thủ công.

### Vì sao có Spring Boot?

Spring Framework cung cấp các khả năng nền tảng, nhưng lập trình viên vẫn có thể phải tự chọn dependency, cấu hình Spring MVC, web server, JSON converter, datasource và transaction manager.

Spring Boot được tạo ra để giảm phần thiết lập đó bằng starter dependency, auto-configuration, embedded server và cấu hình theo quy ước.

```text
Ứng dụng Java
+ Spring Framework: IoC/DI, bean, MVC, transaction, AOP
+ Spring Boot: cấu hình và khởi động các thành phần Spring thuận tiện hơn
```

Spring Boot không thay thế Spring Framework. Nó sử dụng Spring Framework ở bên dưới và giúp lập trình viên bắt đầu, cấu hình, chạy và triển khai ứng dụng Spring nhanh hơn.
## Spring Boot là gì?

Spring Boot là một công cụ thuộc hệ sinh thái Spring, giúp tạo và chạy ứng dụng Spring nhanh hơn. Spring Boot không thay thế Spring Framework. Nó sử dụng Spring Framework ở bên dưới, sau đó bổ sung các quy ước, cấu hình tự động và công cụ đóng gói để giảm lượng cấu hình thủ công.

Ví dụ ngắn:

- Thêm `spring-boot-starter-web` thì Spring Boot tự chuẩn bị Spring MVC, JSON converter và web server nhúng để chạy REST API.
- Thêm `spring-boot-starter-data-jpa` và cấu hình database thì Spring Boot tự chuẩn bị nhiều phần liên quan đến JPA, datasource và transaction.
- Có thể đóng gói ứng dụng thành file JAR rồi chạy bằng `java -jar`, không cần deploy thủ công lên server riêng trong nhiều trường hợp.

Spring Boot thường được dùng để xây dựng:

- REST API và backend cho web hoặc mobile;
- ứng dụng xử lý nghiệp vụ;
- microservice;
- ứng dụng truy cập database;
- ứng dụng tích hợp cache, message broker và hệ thống bên ngoài;
- batch job hoặc command-line application.

Một ứng dụng Spring thông thường cần cấu hình nhiều thành phần như web server, JSON converter, kết nối database và cách tạo object. Spring Boot tự cấu hình phần lớn các thành phần đó dựa trên dependency và cấu hình đang có trong project.

Ví dụ, khi project có dependency Spring Web, Spring Boot có thể:

- cấu hình Spring MVC;
- cấu hình web server nhúng;
- cấu hình chuyển object Java thành JSON;
- khởi động ứng dụng web mà không cần deploy file WAR vào một server cài riêng.

Spring Boot không tự viết business logic. Lập trình viên vẫn phải viết controller, service, repository, entity, validation, xử lý lỗi và các rule nghiệp vụ.

### Spring Framework và Spring Boot khác nhau thế nào?

**Spring Framework** cung cấp các nền tảng chính như IoC Container, Dependency Injection, Spring MVC, transaction, validation và khả năng tích hợp dữ liệu.

**Spring Boot** giúp sử dụng các nền tảng đó thuận tiện hơn bằng:

- auto-configuration;
- starter dependency;
- embedded server;
- externalized configuration;
- công cụ theo dõi ứng dụng qua Actuator;
- quy ước cấu trúc và cách khởi động ứng dụng.

Có thể dùng Spring Framework mà không dùng Spring Boot, nhưng hiện nay phần lớn ứng dụng Spring mới sử dụng Spring Boot để giảm cấu hình ban đầu và thống nhất cách đóng gói, chạy ứng dụng.

## Thành phần quan trọng của Spring Boot

### Starter dependency

Starter là dependency tập hợp các thư viện thường đi cùng nhau cho một mục đích cụ thể. Nhờ starter, lập trình viên không phải tự tìm và thêm từng thư viện nhỏ.

Một số starter phổ biến:

- `spring-boot-starter-web`: xây dựng REST API hoặc web application theo Spring MVC;
- `spring-boot-starter-data-jpa`: truy cập relational database bằng Spring Data JPA và JPA provider;
- `spring-boot-starter-validation`: kiểm tra dữ liệu đầu vào;
- `spring-boot-starter-security`: xác thực và phân quyền;
- `spring-boot-starter-test`: các thư viện hỗ trợ test;
- `spring-boot-starter-actuator`: health check, metrics và thông tin vận hành.

Starter không phải là code thay cho ứng dụng. Nó chủ yếu cung cấp một tập dependency tương thích để sử dụng tính năng tương ứng.

### Auto-configuration

Auto-configuration là cơ chế Spring Boot tự cấu hình ứng dụng dựa trên:

- các class và thư viện có trong classpath;
- các bean mà lập trình viên đã khai báo;
- các property trong file cấu hình;
- loại ứng dụng đang chạy, ví dụ servlet web application hoặc non-web application.

Ví dụ, nếu project có Spring Data JPA và JDBC driver, đồng thời có thông tin datasource, Spring Boot có thể cấu hình `DataSource`, JPA và transaction manager phù hợp.

Auto-configuration thường có điều kiện. Một cấu hình chỉ được áp dụng nếu các điều kiện của nó đúng. Nếu lập trình viên đã khai báo bean riêng, nhiều auto-configuration sẽ không tạo bean mặc định nữa. Vì vậy, auto-configuration có thể được thay đổi hoặc ghi đè khi ứng dụng cần cấu hình riêng.

Auto-configuration không có nghĩa Spring Boot đoán được mọi yêu cầu. Khi ứng dụng có nhiều implementation cùng loại, nhiều datasource hoặc yêu cầu bảo mật đặc biệt, lập trình viên vẫn phải cấu hình rõ ràng.

### Embedded server

Embedded server là web server được đóng gói và chạy ngay bên trong ứng dụng Spring Boot. Với ứng dụng web Java, web server là thành phần nhận HTTP request từ client, ví dụ browser, mobile app hoặc Postman, rồi chuyển request đó vào ứng dụng để xử lý.

Trước đây, khi làm ứng dụng Java web, lập trình viên thường phải:

- cài một server riêng như Tomcat;
- đóng gói ứng dụng thành file WAR;
- deploy file WAR đó vào Tomcat;
- cấu hình Tomcat để chạy ứng dụng.

Spring Boot làm cách này đơn giản hơn. Khi dùng starter web, ví dụ `spring-boot-starter-web`, Spring Boot thường đưa sẵn Tomcat nhúng vào project. Lúc chạy ứng dụng, Tomcat cũng được khởi động cùng ứng dụng, nên không cần cài Tomcat riêng trong nhiều trường hợp.

Luồng chạy phổ biến:

```text
java -jar application.jar
        -> khởi động JVM
        -> khởi tạo Spring ApplicationContext
        -> khởi động embedded web server
        -> ứng dụng bắt đầu nhận HTTP request
```

Ví dụ, khi chạy một ứng dụng REST API bằng lệnh:

```bash
java -jar shop-api.jar
```

Spring Boot sẽ khởi động ứng dụng và web server nhúng. Nếu server chạy ở port `8080`, client có thể gọi API như:

```text
GET http://localhost:8080/products
```

Request này đi vào embedded server trước, sau đó được chuyển tới Spring MVC, rồi tới controller phù hợp, ví dụ `ProductController`.

Nói ngắn gọn, thay vì “mang ứng dụng đi bỏ vào server”, Spring Boot thường “mang server đi cùng ứng dụng”. Nhờ vậy ứng dụng có thể được đóng gói thành một file JAR và chạy trực tiếp.

Embedded server không có nghĩa là ứng dụng không cần server. Nó chỉ có nghĩa là server được nhúng trong ứng dụng, thay vì được cài và quản lý riêng bên ngoài.

### Externalized configuration

Spring Boot cho phép đặt cấu hình bên ngoài source code. Cùng một bản build có thể chạy ở nhiều môi trường bằng các giá trị cấu hình khác nhau.

Các nguồn cấu hình thường gặp:

- `application.properties`;
- `application.yml`;
- biến môi trường;
- command-line argument;
- file cấu hình bên ngoài JAR;
- secret hoặc config do nền tảng triển khai cung cấp.

Ví dụ `application.yml`:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/shop
    username: shop_app
    password: ${DB_PASSWORD}
```

`${DB_PASSWORD}` yêu cầu Spring lấy giá trị từ nguồn cấu hình khác, thường là biến môi trường. Không nên ghi password thật, API key hoặc private key trực tiếp vào Git.

## Cấu trúc project cơ bản

Một project backend nhỏ có thể tổ chức như sau:

```text
src/main/java/com/example/shop
├── ShopApplication.java
├── controller
│   └── ProductController.java
├── service
│   └── ProductService.java
├── repository
│   └── ProductRepository.java
├── entity
│   └── Product.java
├── dto
│   ├── CreateProductRequest.java
│   └── ProductResponse.java
├── exception
│   └── GlobalExceptionHandler.java
└── config
    └── SecurityConfig.java

src/main/resources
├── application.yml
└── db/migration
```

Ý nghĩa các phần phổ biến:

- `controller`: nhận HTTP request, kiểm tra input ở mức API và trả HTTP response;
- `service`: chứa use case và business logic;
- `repository`: truy cập dữ liệu;
- `entity`: biểu diễn dữ liệu được ánh xạ với database khi dùng JPA;
- `dto`: dữ liệu dùng để nhận request hoặc trả response;
- `exception`: định nghĩa và chuyển exception thành response phù hợp;
- `config`: cấu hình bean, security, CORS hoặc các tích hợp khác.

Đây là cấu trúc đơn giản, không phải quy tắc bắt buộc. Khi hệ thống lớn, có thể chia package theo từng chức năng nghiệp vụ thay vì gom toàn bộ controller, service và repository của mọi chức năng vào các package chung.

Nên đặt class main ở package gốc, phía trên các package còn lại. Mặc định, component scanning bắt đầu từ package chứa class main và quét các package con.

## Khởi động ứng dụng

Class main cơ bản:

```java
@SpringBootApplication
public class ShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }
}
```

`SpringApplication.run(...)` khởi động ứng dụng và tạo `ApplicationContext`.

`@SpringBootApplication` kết hợp ba nhóm chức năng chính:

- đánh dấu đây là class cấu hình;
- bật auto-configuration;
- bật component scanning từ package hiện tại xuống các package con.

Luồng khởi động được đơn giản hóa như sau:

1. JVM chạy method `main`.
2. Spring Boot xác định loại ứng dụng.
3. Spring tạo và chuẩn bị `ApplicationContext`.
4. Spring quét component, đọc cấu hình và đăng ký bean.
5. Spring Boot áp dụng các auto-configuration phù hợp.
6. Spring tạo bean và inject dependency.
7. Nếu là web application, embedded server được khởi động.
8. Ứng dụng sẵn sàng nhận request.

Nếu một bean bắt buộc không tạo được, cấu hình sai hoặc dependency bị thiếu, ứng dụng thường dừng ngay khi khởi động thay vì tiếp tục chạy trong trạng thái thiếu thành phần.

## IoC Container, Bean và Dependency Injection

### IoC Container

IoC là viết tắt của **Inversion of Control**. Trong ứng dụng Spring, Spring container chịu trách nhiệm tạo, cấu hình, liên kết và quản lý vòng đời của các object được đăng ký với nó.

`ApplicationContext` là container được sử dụng phổ biến. Nó giữ thông tin về các bean và cung cấp thêm các khả năng như event, resource, môi trường cấu hình và tích hợp với nhiều phần khác của Spring.

### Bean

Bean là một object do Spring container tạo hoặc quản lý.

Không phải mọi object trong ứng dụng đều là bean. Object được tạo trực tiếp bằng `new` trong business code thường không được Spring quản lý. Vì vậy, các tính năng dựa vào container như dependency injection hoặc proxy có thể không hoạt động trên object đó.

Các annotation stereotype thường dùng để đăng ký bean:

- `@Component`: component chung;
- `@Service`: class xử lý nghiệp vụ;
- `@Repository`: class truy cập dữ liệu;
- `@Controller`: controller trả view;
- `@RestController`: controller thường trả dữ liệu JSON.

Các annotation này đều giúp Spring phát hiện class trong quá trình component scanning. Tên khác nhau thể hiện vai trò của class để code dễ đọc hơn. Một số annotation còn có hành vi bổ sung; ví dụ `@Repository` tham gia cơ chế chuyển đổi exception truy cập dữ liệu của Spring.

Có thể khai báo bean bằng Java configuration:

```java
@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
```

`@Bean` phù hợp khi cần đăng ký object của thư viện bên ngoài hoặc khi muốn kiểm soát rõ cách object được tạo.

### Dependency Injection

Dependency là object mà một object khác cần để thực hiện công việc. Dependency Injection là việc cung cấp dependency từ bên ngoài thay vì để object tự tạo dependency của nó.

Nên ưu tiên constructor injection cho dependency bắt buộc:

```java
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
}
```

Spring tìm một bean có kiểu phù hợp và truyền bean đó vào constructor khi tạo `ProductService`.

Ưu điểm của constructor injection:

- dependency được thể hiện rõ;
- object không thể được tạo trong trạng thái thiếu dependency bắt buộc;
- field có thể khai báo `final`;
- dễ viết unit test mà không cần khởi động Spring;
- tránh che giấu quá nhiều dependency như field injection.

Nếu có nhiều bean cùng implement một interface, Spring không biết phải inject bean nào. Có thể xử lý bằng `@Qualifier`, chọn một bean `@Primary`, hoặc inject collection nếu cần dùng tất cả implementation.

### Bean scope

Scope xác định số lượng instance và thời gian tồn tại của bean.

- `singleton`: mặc định, mỗi bean definition có một instance trong một Spring container;
- `prototype`: tạo instance mới mỗi lần container được yêu cầu cung cấp bean;
- `request`: một instance cho mỗi HTTP request;
- `session`: một instance cho mỗi HTTP session;
- `application`: một instance theo `ServletContext`;
- `websocket`: một instance theo WebSocket session.

Spring singleton không hoàn toàn giống Singleton design pattern toàn JVM. Nó là một instance cho mỗi bean definition trong một Spring container.

Bean singleton thường được nhiều thread sử dụng đồng thời trong web application. Không nên giữ dữ liệu thay đổi riêng của từng request trong field của singleton bean, nếu không có thiết kế thread-safe phù hợp.

## Configuration và profile

### `application.properties` và `application.yml`

Hai định dạng đều dùng để cấu hình Spring Boot. Nên chọn một định dạng chính trong project để dễ quản lý.

Properties:

```properties
server.port=8080
spring.application.name=shop-service
```

YAML:

```yaml
server:
  port: 8080
spring:
  application:
    name: shop-service
```

### Đọc cấu hình bằng `@ConfigurationProperties`

Khi có một nhóm cấu hình liên quan, nên ánh xạ chúng vào một class thay vì dùng nhiều `@Value` rời rạc.

```yaml
payment:
  base-url: https://payment.example.com
  timeout: 3s
```

```java
@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(
        URI baseUrl,
        Duration timeout
) {
}
```

`@ConfigurationProperties` giúp:

- gom cấu hình theo nhóm;
- chuyển String sang kiểu phù hợp như `Duration`, `URI`, số hoặc enum;
- dễ validation;
- dễ tìm nơi cấu hình được sử dụng.

Class properties cần được đăng ký bằng cách phù hợp, chẳng hạn `@ConfigurationPropertiesScan` hoặc `@EnableConfigurationProperties`.

### Profile

Profile cho phép bật cấu hình hoặc bean theo môi trường.

Ví dụ:

```text
application.yml
application-dev.yml
application-test.yml
application-prod.yml
```

Có thể kích hoạt profile bằng cấu hình như `spring.profiles.active=dev` hoặc bằng biến môi trường tương ứng.

Profile nên dùng cho khác biệt theo môi trường, không nên dùng để giấu quá nhiều nhánh business logic. Cũng không nên commit secret thật vào file profile production.

## Xây dựng REST API với Spring MVC

### Controller

`@RestController` đánh dấu class xử lý HTTP request và mặc định ghi giá trị trả về vào HTTP response body.

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody CreateProductRequest request) {
        ProductResponse result = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
```

Các annotation thường gặp:

- `@RequestMapping`: khai báo path hoặc rule chung;
- `@GetMapping`: xử lý HTTP GET;
- `@PostMapping`: xử lý HTTP POST;
- `@PutMapping`: xử lý HTTP PUT;
- `@PatchMapping`: xử lý HTTP PATCH;
- `@DeleteMapping`: xử lý HTTP DELETE;
- `@PathVariable`: lấy giá trị từ path;
- `@RequestParam`: lấy query parameter;
- `@RequestHeader`: lấy HTTP header;
- `@RequestBody`: đọc request body và chuyển thành object Java.

Spring thường dùng Jackson để chuyển JSON thành object Java và ngược lại khi Jackson có trong project.

### Luồng xử lý HTTP request

Luồng cơ bản trong Spring MVC:

```text
Client
  -> HTTP request
  -> Filter chain
  -> DispatcherServlet
  -> tìm Controller method phù hợp
  -> Controller
  -> Service
  -> Repository / hệ thống bên ngoài
  -> trả kết quả
  -> chuyển object thành JSON
  -> HTTP response
```

`DispatcherServlet` là front controller trung tâm của Spring MVC. Nó nhận request và phối hợp các thành phần để tìm handler, gọi controller và tạo response.

Controller nên tập trung vào HTTP concern: nhận request, gọi use case và trả response. Không nên chứa toàn bộ business logic hoặc truy cập database trực tiếp.

### DTO và Entity

Không nên mặc định dùng entity JPA trực tiếp làm request và response cho mọi API.

DTO giúp:

- chỉ công khai field cần thiết;
- tách API contract khỏi database schema;
- đặt validation theo từng request;
- tránh cập nhật ngoài ý muốn các field nhạy cảm;
- giảm vấn đề khi serialize quan hệ JPA;
- dễ thay đổi cấu trúc lưu trữ mà ít ảnh hưởng client.

Ví dụ request DTO:

```java
public record CreateProductRequest(
        @NotBlank String name,
        @NotNull @Positive BigDecimal price
) {
}
```

## Validation

Spring Boot có thể tích hợp Jakarta Bean Validation để kiểm tra dữ liệu.

Các constraint thường dùng:

- `@NotNull`: không được là `null`;
- `@NotBlank`: String không được `null`, rỗng hoặc chỉ có khoảng trắng;
- `@NotEmpty`: collection, map, array hoặc String không được rỗng;
- `@Size`: giới hạn độ dài hoặc số phần tử;
- `@Min`, `@Max`: giới hạn số nguyên;
- `@Positive`, `@PositiveOrZero`: số dương hoặc không âm;
- `@Email`: kiểm tra định dạng email;
- `@Pattern`: kiểm tra theo regular expression.

Để validation request body chạy, thường đặt `@Valid` trước tham số:

```java
public void create(@Valid @RequestBody CreateUserRequest request) {
}
```

Validation chỉ xác nhận dữ liệu có đúng rule được khai báo hay không. Nó không thay thế business validation. Ví dụ `@Email` có thể kiểm tra định dạng, nhưng kiểm tra email đã tồn tại trong hệ thống hay chưa vẫn thuộc business logic và thường cần truy cập dữ liệu.

## Xử lý exception

API nên trả lỗi có cấu trúc thống nhất thay vì để mỗi controller tự tạo một dạng lỗi khác nhau.

Có thể xử lý lỗi tập trung bằng `@RestControllerAdvice` và `@ExceptionHandler`:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ProductNotFoundException ex) {
        ApiError error = new ApiError("PRODUCT_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

Nên phân biệt:

- lỗi input: thường trả `400 Bad Request`;
- chưa xác thực: thường trả `401 Unauthorized`;
- đã xác thực nhưng không có quyền: thường trả `403 Forbidden`;
- không tìm thấy resource: thường trả `404 Not Found`;
- xung đột trạng thái hoặc dữ liệu: có thể trả `409 Conflict`;
- lỗi ngoài dự kiến ở server: trả `500 Internal Server Error` và không làm lộ stack trace hoặc thông tin nhạy cảm cho client.

Không nên bắt `Exception` ở mọi service rồi trả kết quả thành công giả. Exception cần được xử lý ở tầng phù hợp; lỗi kỹ thuật nên được log với đủ context và chuyển thành response an toàn.

## Truy cập database với Spring Data JPA

### JPA, Hibernate và Spring Data JPA

Ba khái niệm này không giống nhau:

- **JPA/Jakarta Persistence** là specification mô tả cách ánh xạ object Java với dữ liệu quan hệ và cách thao tác persistence;
- **Hibernate ORM** là một implementation phổ biến của JPA;
- **Spring Data JPA** xây dựng trên JPA, giúp giảm code repository và tích hợp với Spring.

### Entity

```java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    protected Product() {
    }

    // constructor, getter và method nghiệp vụ
}
```

Các annotation cơ bản:

- `@Entity`: class là JPA entity;
- `@Table`: cấu hình table;
- `@Id`: primary key;
- `@GeneratedValue`: cách sinh primary key;
- `@Column`: cấu hình column;
- `@OneToOne`, `@OneToMany`, `@ManyToOne`, `@ManyToMany`: quan hệ giữa entity.

Entity không chỉ là DTO chứa dữ liệu. Trong thiết kế có domain logic, entity có thể bảo vệ trạng thái hợp lệ bằng constructor và method nghiệp vụ. Tuy nhiên, không nên đưa HTTP request, JSON response hoặc dependency tới controller vào entity.

### Repository

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByName(String name);

    Page<Product> findByPriceGreaterThan(
            BigDecimal price,
            Pageable pageable
    );
}
```

Spring Data JPA tạo implementation cho repository interface ở runtime. `JpaRepository` cung cấp các thao tác phổ biến như lưu, tìm theo id, xóa, phân trang và sắp xếp.

Query có thể được tạo từ tên method, viết bằng `@Query`, Criteria API, Specification hoặc công cụ khác tùy độ phức tạp. Với query dài hoặc khó đọc, không nên cố nhồi toàn bộ ý nghĩa vào tên method.

### Entity lifecycle và persistence context

Trong JPA, entity có thể ở các trạng thái như transient, managed, detached và removed. Entity đang được quản lý trong persistence context có thể được theo dõi thay đổi. Khi transaction được flush/commit, thay đổi phù hợp được đồng bộ xuống database.

Điều này giải thích vì sao trong một transaction, có trường hợp load entity, thay đổi field rồi không cần gọi `save` để JPA tạo câu lệnh update. Cơ chế đó thường được gọi là dirty checking.

Không nên dựa vào hành vi này mà bỏ qua ranh giới transaction hoặc viết code khó hiểu. Service nên thể hiện rõ use case đang đọc hay thay đổi dữ liệu.

### Lazy loading và N+1 query

Quan hệ entity có thể được tải lazy. Dữ liệu liên quan chỉ được truy vấn khi code truy cập quan hệ đó. Lazy loading giúp tránh tải dữ liệu không cần thiết, nhưng cũng có thể gây lỗi khi persistence context đã đóng hoặc tạo nhiều query không mong muốn.

N+1 query thường xảy ra khi:

1. chạy một query lấy danh sách N entity;
2. với mỗi entity, code truy cập một quan hệ lazy;
3. JPA chạy thêm N query để lấy quan hệ đó.

Không nên sửa mọi vấn đề bằng cách chuyển tất cả quan hệ thành eager. Nên thiết kế query theo dữ liệu use case cần, có thể dùng fetch join, entity graph, projection hoặc query DTO.

### Database migration

Trong môi trường thực tế, nên quản lý thay đổi schema bằng migration tool như Flyway hoặc Liquibase. Migration được lưu theo phiên bản trong source control và chạy theo thứ tự.

Không nên phụ thuộc vào việc ORM tự động thay đổi schema production bằng `ddl-auto=update`, vì thay đổi có thể khó kiểm soát, khó review và không đủ cho migration dữ liệu phức tạp.

## Transaction

Transaction là một nhóm thao tác dữ liệu được xử lý như một đơn vị. Với relational database, transaction thường gắn với các thuộc tính ACID: atomicity, consistency, isolation và durability.

Ví dụ chuyển tiền gồm trừ tài khoản A và cộng tài khoản B. Nếu bước thứ hai thất bại, bước thứ nhất không nên được commit riêng.

Spring hỗ trợ transaction khai báo bằng `@Transactional`:

```java
@Service
public class TransferService {

    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        Account from = accountRepository.findById(fromId).orElseThrow();
        Account to = accountRepository.findById(toId).orElseThrow();

        from.withdraw(amount);
        to.deposit(amount);
    }
}
```

Ranh giới transaction thường đặt ở service/use case vì nơi đó biết một nghiệp vụ gồm những thao tác nào.

Các điểm cần nhớ:

- mặc định, Spring thường rollback với `RuntimeException` và `Error`; checked exception cần cấu hình nếu muốn rollback;
- `readOnly = true` thể hiện transaction chủ yếu dùng để đọc, nhưng không phải cơ chế bảo mật ngăn tuyệt đối mọi lệnh ghi;
- transaction không tự bao phủ remote API, message broker và database thành một transaction duy nhất;
- giữ transaction mở trong lúc gọi network lâu có thể giữ connection và lock quá lâu;
- `@Transactional` thường hoạt động qua proxy, vì vậy gọi method transactional từ một method khác trong cùng object có thể không đi qua proxy và không áp dụng transaction như mong đợi;
- method `private` không phải điểm vào phù hợp để kỳ vọng proxy transaction thông thường chặn lời gọi;
- isolation và propagation chỉ nên chỉnh khi hiểu yêu cầu dữ liệu cụ thể.

## Spring AOP và proxy

Spring dùng AOP để áp dụng hành vi dùng chung tại các điểm phù hợp mà không phải viết lại cùng một đoạn code trong mọi class. Transaction, method security, cache và một số tính năng khác thường được triển khai bằng proxy.

Proxy là object đứng trước bean thật. Khi code gọi method thông qua proxy, proxy có thể thực hiện thêm hành vi trước hoặc sau khi gọi bean thật.

```text
Caller -> Spring proxy -> transaction/security/cache logic -> bean thật
```

Hệ quả quan trọng:

- annotation trên method không phải lúc nào cũng có tác dụng nếu lời gọi không đi qua proxy;
- self-invocation là trường hợp phổ biến cần chú ý;
- không nên mặc định gắn AOP vào mọi logic chỉ để giảm vài dòng code, vì luồng chạy sẽ khó nhìn thấy hơn;
- business logic chính vẫn nên được viết rõ trong service hoặc domain object.

## Spring Security cơ bản

Spring Security xử lý hai vấn đề chính:

- **authentication**: xác định người dùng hoặc hệ thống gọi là ai;
- **authorization**: xác định đối tượng đã xác thực được phép làm gì.

Khi thêm Spring Security, request thường đi qua security filter chain trước khi tới controller.

```text
HTTP request
  -> Security filter chain
  -> xác thực
  -> kiểm tra quyền
  -> Controller
```

Cấu hình hiện đại thường khai báo `SecurityFilterChain` bean:

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}
```

Đây chỉ là khung cấu hình. Ứng dụng thực tế còn phải chọn cơ chế authentication như session, OAuth 2.0 Resource Server/JWT, OpenID Connect hoặc cơ chế phù hợp khác.

Các lưu ý quan trọng:

- không tự viết thuật toán mã hóa password;
- password phải được hash bằng `PasswordEncoder` phù hợp, không lưu dạng plain text;
- JWT có chữ ký không đồng nghĩa payload được mã hóa;
- CORS và CSRF là hai vấn đề khác nhau;
- không tắt CSRF theo thói quen, cần dựa vào cách client gửi credential và loại ứng dụng;
- kiểm tra quyền ở server, không dựa vào việc frontend ẩn nút;
- không log password, token hoặc secret;
- phân quyền nên dùng nguyên tắc cấp quyền tối thiểu cần thiết.

Security là lĩnh vực có rủi ro cao. Phần này chỉ cung cấp nền tảng; cấu hình production cần bám theo tài liệu hiện hành và mô hình xác thực cụ thể của hệ thống.

## Logging và Actuator

Spring Boot cung cấp cơ chế logging mặc định và có thể cấu hình mức log theo package.

```yaml
logging:
  level:
    root: INFO
    com.example.shop: DEBUG
```

Nên log các thông tin giúp chẩn đoán:

- request hoặc operation identifier;
- tên use case;
- id của resource liên quan;
- kết quả và thời gian xử lý;
- exception cùng context cần thiết.

Không nên log:

- password;
- access token hoặc refresh token;
- secret key;
- thông tin cá nhân đầy đủ nếu không thật sự cần;
- toàn bộ request/response một cách mặc định ở production.

Actuator bổ sung endpoint phục vụ theo dõi và quản lý ứng dụng. Ví dụ phổ biến:

- health: trạng thái hoạt động;
- info: thông tin ứng dụng được cho phép công khai;
- metrics: số liệu runtime và ứng dụng;
- loggers: xem hoặc điều chỉnh log level khi được cấu hình;
- mappings: thông tin mapping web;
- threaddump và heapdump: hỗ trợ chẩn đoán, cần bảo vệ rất chặt.

Endpoint HTTP thường có prefix `/actuator`, ví dụ `/actuator/health`. Không nên public toàn bộ endpoint Actuator. Chỉ expose endpoint cần thiết và bảo vệ bằng network policy, authentication và authorization phù hợp.

## Testing

Không phải test nào cũng cần khởi động toàn bộ Spring application.

### Unit test

Unit test kiểm tra một class hoặc nhóm logic nhỏ, thường không khởi động Spring context.

```java
class ProductServiceTest {

    @Test
    void shouldRejectNegativePrice() {
        ProductRepository repository = mock(ProductRepository.class);
        ProductService service = new ProductService(repository);

        assertThrows(IllegalArgumentException.class,
                () -> service.createProduct("Book", new BigDecimal("-1")));
    }
}
```

Unit test chạy nhanh và phù hợp để kiểm tra business rule.

### Slice test

Slice test chỉ khởi động phần context liên quan:

- `@WebMvcTest`: test Spring MVC controller;
- `@DataJpaTest`: test JPA repository và mapping;
- các test slice khác tùy module Spring Boot đang dùng.

Slice test nhẹ hơn `@SpringBootTest` và giúp xác định rõ phần đang được kiểm tra.

### Integration test

`@SpringBootTest` tải đầy đủ application context. Nó phù hợp để kiểm tra nhiều thành phần hoạt động cùng nhau, nhưng chậm hơn và không nên thay thế toàn bộ unit test.

Với database hoặc hạ tầng thật, có thể dùng Testcontainers để chạy dependency trong container phục vụ test. Điều này giúp test gần môi trường production hơn so với mock database, nhưng yêu cầu môi trường chạy container và tốn thời gian hơn.

Một chiến lược thực tế:

- nhiều unit test cho business logic;
- slice test cho controller, repository hoặc serialization;
- một lượng vừa đủ integration test cho các luồng quan trọng;
- end-to-end test cho một số hành trình chính của hệ thống.

## Build và chạy ứng dụng

Spring Boot hỗ trợ Maven và Gradle. Project Maven thường có `pom.xml`; project Gradle thường có `build.gradle` hoặc `build.gradle.kts`.

Các lệnh Maven Wrapper thường gặp:

```bash
./mvnw test
./mvnw spring-boot:run
./mvnw clean package
java -jar target/application.jar
```

Trên Windows có thể dùng `mvnw.cmd`.

Gradle Wrapper:

```bash
./gradlew test
./gradlew bootRun
./gradlew bootJar
java -jar build/libs/application.jar
```

Wrapper giúp project quy định phiên bản Maven hoặc Gradle cần dùng. Nên commit wrapper vào source control.

File executable JAR của Spring Boot thường chứa application class và các dependency cần thiết. Có thể chạy bằng `java -jar` với Java runtime tương thích.

## Quy trình xử lý một request hoàn chỉnh

Ví dụ client tạo sản phẩm:

```text
POST /api/products
Content-Type: application/json

{
  "name": "Keyboard",
  "price": 1200000
}
```

Luồng xử lý có thể diễn ra như sau:

1. Embedded server nhận kết nối HTTP.
2. Request đi qua các filter, bao gồm security filter nếu có.
3. `DispatcherServlet` tìm controller method phù hợp.
4. Jackson chuyển JSON thành `CreateProductRequest`.
5. Bean Validation kiểm tra các annotation trên request DTO.
6. Controller gọi `ProductService`.
7. Service kiểm tra business rule và bắt đầu transaction nếu có `@Transactional`.
8. Service tạo hoặc thay đổi entity.
9. Repository/JPA thực hiện thao tác với database.
10. Transaction commit; nếu lỗi phù hợp xảy ra thì rollback.
11. Service trả kết quả cho controller.
12. Jackson chuyển response DTO thành JSON.
13. Server trả HTTP status, header và body cho client.

Nếu exception xảy ra, global exception handler có thể chuyển exception thành error response thống nhất.

## Các lỗi người mới thường gặp

### Đặt class ngoài phạm vi component scanning

Spring không tìm thấy controller hoặc service vì class nằm ngoài package gốc được quét. Nên đặt class main ở package gốc hoặc cấu hình package scanning rõ ràng.

### Tự tạo Spring bean bằng `new`

Nếu tự `new ProductService(...)` trong code runtime thay vì để Spring tạo, object đó không tự nhận các cơ chế container hoặc proxy mà người viết đang kỳ vọng.

### Field injection ở khắp nơi

Field injection viết ngắn nhưng giấu dependency, khó unit test và khiến object có thể tồn tại trước khi field được inject. Constructor injection thường rõ ràng hơn.

### Controller chứa business logic

Khi controller vừa xử lý HTTP, vừa tính nghiệp vụ, vừa truy vấn database, code khó test và khó tái sử dụng. Nên chuyển use case vào service hoặc domain layer phù hợp.

### Trả entity trực tiếp cho mọi API

Việc này dễ làm lộ field, tạo vòng lặp JSON, phụ thuộc API vào database model và gây lazy loading ngoài ý muốn. DTO thường an toàn và ổn định hơn.

### Dùng `EAGER` để sửa lỗi lazy loading

Chuyển mọi quan hệ sang eager có thể tạo query lớn, tải thừa dữ liệu và phát sinh vấn đề hiệu năng khác. Nên thiết kế query theo use case.

### Không hiểu transaction proxy

`@Transactional` có thể không hoạt động như mong đợi khi gọi nội bộ cùng class. Cần hiểu lời gọi có đi qua Spring proxy hay không.

### Dùng `ddl-auto=update` cho production

Tự động sửa schema không thay thế quy trình migration có version và review. Nên dùng Flyway hoặc Liquibase cho production.

### Đặt secret trong source code

Secret đã commit vào Git không còn được xem là an toàn chỉ vì sau đó xóa file. Cần rotate secret và chuyển sang secret manager hoặc cơ chế cấu hình an toàn.

### Khởi động toàn bộ context cho mọi test

Lạm dụng `@SpringBootTest` làm test chậm và khó xác định lỗi thuộc thành phần nào. Nên chọn unit test, slice test hoặc integration test theo phạm vi cần kiểm tra.

### Public toàn bộ Actuator endpoint

Một số endpoint cung cấp thông tin nhạy cảm về ứng dụng. Chỉ expose endpoint cần thiết và bảo vệ chúng.

## Checklist cho một ứng dụng Spring Boot cơ bản

- Class main đặt ở package gốc hợp lý.
- Dependency được quản lý bằng Maven hoặc Gradle và không thêm starter thừa.
- Sử dụng constructor injection cho dependency bắt buộc.
- Controller, business logic và data access được tách vai trò rõ.
- Request/response dùng DTO khi cần bảo vệ API contract.
- Input được validation và business rule được kiểm tra ở tầng phù hợp.
- Exception được chuyển thành response thống nhất.
- Transaction bao quanh đúng một use case thay đổi dữ liệu.
- Query được kiểm tra để tránh N+1 và tải thừa dữ liệu.
- Database schema được quản lý bằng migration.
- Secret không nằm trong source code hoặc log.
- Security rule được khai báo rõ và test.
- Có unit test, slice test và integration test theo mức cần thiết.
- Actuator endpoint được expose có chọn lọc và được bảo vệ.
- Log đủ để chẩn đoán nhưng không chứa dữ liệu nhạy cảm.
- Ứng dụng được build thành artifact có thể tái sử dụng giữa các môi trường; khác biệt môi trường nằm ở cấu hình.

## Nên học tiếp theo thứ tự nào?

1. Tạo project bằng Spring Initializr và chạy được endpoint đơn giản.
2. Hiểu `ApplicationContext`, bean, component scanning và constructor injection.
3. Viết REST API với controller, DTO, validation và exception handler.
4. Học SQL cơ bản trước hoặc song song với Spring Data JPA.
5. Hiểu entity mapping, repository, persistence context và transaction.
6. Viết unit test, `@WebMvcTest`, `@DataJpaTest` và integration test.
7. Học Spring Security theo cơ chế authentication hệ thống thực sự sử dụng.
8. Học logging, metrics, Actuator và cách triển khai ứng dụng.
9. Sau khi nắm chắc nền tảng mới học cache, message broker, scheduling, WebFlux, distributed transaction hoặc microservice pattern.

Không nên bắt đầu bằng quá nhiều annotation nâng cao. Cần hiểu rõ object nào được Spring tạo, dependency nào được inject, request đi qua những tầng nào và transaction bắt đầu/kết thúc ở đâu.

## Kết luận

Các phần quan trọng nhất của Spring Boot đối với người mới là:

- Spring Boot giúp cấu hình và chạy Spring application nhanh hơn;
- Spring container tạo và quản lý bean;
- Dependency Injection giúp class nhận dependency từ bên ngoài;
- auto-configuration cấu hình theo dependency, bean và property hiện có;
- Spring MVC xử lý HTTP request qua controller;
- validation kiểm tra cấu trúc input, service xử lý business rule;
- Spring Data JPA hỗ trợ truy cập relational database;
- transaction bảo vệ tính nhất quán của một nhóm thao tác dữ liệu;
- Spring Security xử lý authentication và authorization;
- test, logging và Actuator là phần cần thiết để ứng dụng có thể vận hành ổn định.

Spring Boot giảm cấu hình thủ công nhưng không loại bỏ nhu cầu hiểu luồng chạy. Khi gặp lỗi, nên lần theo đúng chuỗi: cấu hình -> bean -> dependency injection -> proxy -> request -> service -> transaction -> database -> response.

## Nguồn tham khảo

### Spring Boot

- Spring Boot Reference Documentation: [https://docs.spring.io/spring-boot/reference/](https://docs.spring.io/spring-boot/reference/)
- Using the `@SpringBootApplication` Annotation: [https://docs.spring.io/spring-boot/reference/using/using-the-springbootapplication-annotation.html](https://docs.spring.io/spring-boot/reference/using/using-the-springbootapplication-annotation.html)
- Auto-configuration: [https://docs.spring.io/spring-boot/reference/using/auto-configuration.html](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html)
- Externalized Configuration: [https://docs.spring.io/spring-boot/reference/features/external-config.html](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- Profiles: [https://docs.spring.io/spring-boot/reference/features/profiles.html](https://docs.spring.io/spring-boot/reference/features/profiles.html)
- Testing Spring Boot Applications: [https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
- Spring Boot Actuator: [https://docs.spring.io/spring-boot/reference/actuator/](https://docs.spring.io/spring-boot/reference/actuator/)

### Spring Framework

- IoC Container và Bean: [https://docs.spring.io/spring-framework/reference/core/beans.html](https://docs.spring.io/spring-framework/reference/core/beans.html)
- Dependency Injection: [https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html)
- Bean Scopes: [https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html](https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html)
- Spring Web MVC: [https://docs.spring.io/spring-framework/reference/web/webmvc.html](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- Validation: [https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html)
- Transaction Management: [https://docs.spring.io/spring-framework/reference/data-access/transaction.html](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)
- Spring AOP: [https://docs.spring.io/spring-framework/reference/core/aop.html](https://docs.spring.io/spring-framework/reference/core/aop.html)

### Data, Security và hướng dẫn thực hành

- Spring Data JPA Reference Documentation: [https://docs.spring.io/spring-data/jpa/reference/](https://docs.spring.io/spring-data/jpa/reference/)
- Spring Security Reference Documentation: [https://docs.spring.io/spring-security/reference/](https://docs.spring.io/spring-security/reference/)
- Building REST Services with Spring: [https://spring.io/guides/tutorials/rest/](https://spring.io/guides/tutorials/rest/)
- Accessing Data with JPA: [https://spring.io/guides/gs/accessing-data-jpa/](https://spring.io/guides/gs/accessing-data-jpa/)
- Testing the Web Layer: [https://spring.io/guides/gs/testing-web/](https://spring.io/guides/gs/testing-web/)
- Spring Initializr: [https://start.spring.io/](https://start.spring.io/)
