# Câu hỏi phỏng vấn Spring Boot trọng tâm

Tài liệu này tập trung vào các câu hỏi phỏng vấn Spring Boot thường gặp. Mỗi phần gồm: giải thích chi tiết, lưu ý, use case và ví dụ ngắn.

## 1. Dependency Injection (DI) là gì?

**Dependency Injection (DI)** là cách đưa dependency từ bên ngoài vào một object, thay vì object tự tạo dependency của nó.

Nếu `ProductService` cần `ProductRepository`, `ProductService` không nên tự tạo repository bằng `new`. Thay vào đó, Spring tạo `ProductRepository` và truyền vào `ProductService`.

Ví dụ:

```java
@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
}
```

Trong ví dụ trên, `ProductRepository` được inject qua constructor.

### Vì sao cần DI?

DI giúp class không phụ thuộc chặt vào cách tạo dependency. Class chỉ khai báo nó cần gì, còn việc tạo object và kết nối dependency do Spring Container xử lý.

Lợi ích:

- Code dễ test hơn vì có thể truyền mock dependency.
- Code ít phụ thuộc chặt vào implementation cụ thể.
- Dễ thay implementation mà không sửa nhiều nơi.
- Dependency của class rõ ràng hơn.

Ví dụ khi test:

```java
ProductRepository mockRepository = mock(ProductRepository.class);
ProductService service = new ProductService(mockRepository);
```

Nếu `ProductService` tự tạo `ProductRepository`, việc test sẽ khó hơn.

### `@Autowired` dùng để làm gì?

`@Autowired` là annotation báo cho Spring biết rằng cần inject dependency vào vị trí được đánh dấu. Spring sẽ tìm bean phù hợp trong `ApplicationContext`, thường dựa theo type, rồi truyền bean đó vào nơi cần dùng.

`@Autowired` có thể dùng ở nhiều vị trí:

- Trên constructor.
- Trên setter.
- Trên field.
- Trên method thông thường có tham số cần inject.

Ví dụ dùng trên constructor:

```java
@Service
public class ProductService {
    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
}
```

Từ Spring 4.3, nếu class chỉ có một constructor, có thể bỏ `@Autowired`:

```java
@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
}
```

Ví dụ dùng trên setter:

```java
@Autowired
public void setProductRepository(ProductRepository productRepository) {
    this.productRepository = productRepository;
}
```

Ví dụ dùng trên field:

```java
@Autowired
private ProductRepository productRepository;
```

Thứ tự tìm bean thường gặp:

```text
Spring thấy @Autowired
-> tìm bean phù hợp theo type
-> nếu có một bean phù hợp thì inject
-> nếu có nhiều bean cùng type thì cần @Qualifier hoặc @Primary
-> nếu không có bean phù hợp thì thường báo lỗi khi khởi động
```

Lưu ý: `@Autowired` là cách yêu cầu Spring inject dependency, còn inject bằng constructor, setter hay field là các kiểu DI khác nhau.

### Các kiểu DI phổ biến

#### Constructor Injection

Constructor Injection là cách truyền dependency qua constructor của class. Khi Spring tạo bean, Spring phải có đủ các dependency cần thiết để gọi constructor.

```java
@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductValidator productValidator;

    public ProductService(
            ProductRepository productRepository,
            ProductValidator productValidator
    ) {
        this.productRepository = productRepository;
        this.productValidator = productValidator;
    }
}
```

Đây là cách thường được khuyến nghị nhất trong Spring Boot.

Ưu điểm:

- Dependency bắt buộc được thể hiện rõ ngay trong constructor.
- Có thể dùng field `final`, giúp object ổn định sau khi tạo.
- Dễ viết unit test vì có thể tự truyền mock dependency.
- Nếu thiếu dependency, ứng dụng lỗi sớm khi khởi động.

Use case:

- Dependency bắt buộc phải có để class hoạt động.
- Service cần repository, validator, mapper hoặc client.
- Controller cần service.

Lưu ý:

- Nếu constructor có quá nhiều dependency, có thể class đang ôm quá nhiều trách nhiệm.
- Từ Spring 4.3, nếu class chỉ có một constructor, thường không cần ghi `@Autowired` trên constructor.

Ví dụ test:

```java
ProductRepository repository = mock(ProductRepository.class);
ProductValidator validator = mock(ProductValidator.class);

ProductService service = new ProductService(repository, validator);
```

#### Setter Injection

Setter Injection là cách Spring inject dependency thông qua method setter. Spring tạo object bean trước, sau đó tự gọi setter để truyền dependency vào object đó.

```java
@Service
public class ProductService {
    private ProductRepository productRepository;

    public ProductService() {
        // Spring tạo object ProductService trước
    }

    @Autowired
    public void setProductRepository(ProductRepository productRepository) {
        // Sau đó Spring gọi setter này để inject dependency
        this.productRepository = productRepository;
    }
}
```

Setter Injection thường dùng khi dependency không bắt buộc.

Ưu điểm:

- Phù hợp với dependency optional.
- Có thể thay đổi dependency sau khi object được tạo.
- Dễ đọc nếu dependency chỉ là phần bổ sung, không phải lõi của class.

Use case:

- Logger/auditor tùy chọn.
- Component phụ trợ chỉ dùng trong một vài luồng.
- Cấu hình backward compatibility trong code cũ.

Lưu ý:

- Object có thể tồn tại trong trạng thái chưa đủ dependency nếu setter chưa được gọi.
- Không dùng được với field `final`.
- Nếu dependency là bắt buộc, constructor injection thường rõ ràng hơn.

Ví dụ dependency optional:

```java
@Autowired(required = false)
public void setNotificationService(NotificationService notificationService) {
    this.notificationService = notificationService;
}
```

Nếu không có bean `NotificationService`, ứng dụng vẫn có thể chạy.

#### Field Injection

Field Injection là cách Spring inject dependency trực tiếp vào field của class. Spring tạo object bean trước, sau đó tìm các field có `@Autowired` và gán dependency vào field đó, thường thông qua reflection.

```java
@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;
}
```

Ở ví dụ này, `@Autowired` được đặt trên field nên Spring sẽ inject trực tiếp vào field `productRepository`.

Cách này nhìn rất ngắn, nhưng ít được khuyến nghị trong code production.

Thứ tự dễ hiểu:

```text
Spring tạo object bean
-> tìm field có @Autowired
-> tìm bean phù hợp trong ApplicationContext
-> gán dependency trực tiếp vào field
```

Ưu điểm:

- Code ngắn.
- Dễ viết demo nhanh.
- Hay gặp trong code cũ hoặc ví dụ đơn giản.

Nhược điểm:

- Khó test unit vì dependency nằm ẩn bên trong field private.
- Không dùng được với `final`.
- Nhìn constructor không biết class cần dependency nào.
- Class dễ phình to mà không nhận ra vì cứ thêm field inject.

Ví dụ khó test hơn:

```java
@Autowired
private ProductRepository productRepository;
```

Muốn test class này, thường phải dùng Spring context hoặc reflection để gán mock, trong khi constructor injection chỉ cần truyền mock qua constructor.

Use case:

- Demo ngắn.
- Test tích hợp dùng Spring context.
- Code legacy đã dùng sẵn field injection.

### Nên chọn kiểu nào?

Trong phần lớn code Spring Boot, nên ưu tiên:

```text
Constructor Injection cho dependency bắt buộc
Setter Injection cho dependency optional
Field Injection hạn chế dùng trong production code mới
```

Ví dụ dễ nhớ:

```java
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private NotificationService notificationService;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Autowired(required = false)
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
}
```

`OrderRepository` là dependency bắt buộc nên đặt trong constructor. `NotificationService` là dependency optional nên có thể đặt qua setter.

### Use case thường gặp

- Inject repository vào service.
- Inject service vào controller.
- Inject client gọi external API vào service.
- Inject config, mapper, validator hoặc helper dùng chung.

### Lưu ý phỏng vấn

Nên nói rõ DI không chỉ là `@Autowired`. `@Autowired` chỉ là một cách để Spring biết cần inject dependency. Bản chất của DI là object không tự tạo dependency, mà nhận dependency từ bên ngoài.

## 2. Inversion of Control (IoC) là gì?

**Inversion of Control (IoC)** là nguyên lý đảo ngược quyền kiểm soát việc tạo và quản lý object.

Trong Java thông thường, code tự tạo object:

```java
ProductRepository repository = new ProductRepository();
ProductService service = new ProductService(repository);
```

Với Spring, mình khai báo class là bean. Spring Container sẽ tạo object, quản lý vòng đời và inject dependency.

```java
@Service
public class ProductService {
}
```

Lúc này, quyền kiểm soát việc tạo object được chuyển từ code của lập trình viên sang Spring Container.

### IoC và DI khác nhau thế nào?

IoC là nguyên lý tổng quát. DI là một kỹ thuật cụ thể để thực hiện IoC.

```text
IoC: Ai tạo và quản lý object? Spring Container.
DI: Object cần dependency thì lấy ở đâu? Spring inject vào.
```

### Use case thường gặp

IoC xuất hiện trong gần như toàn bộ ứng dụng Spring Boot:

- Spring tạo controller, service, repository.
- Spring inject dependency giữa các bean.
- Spring quản lý lifecycle của bean.
- Spring áp dụng proxy cho transaction, security, cache.

### Lưu ý phỏng vấn

Không nên trả lời IoC chỉ là `@Autowired`. `@Autowired` thuộc DI, còn IoC là ý tưởng lớn hơn: framework kiểm soát vòng đời object thay vì code tự kiểm soát.

## 3. Bean là gì?

**Bean** là object được Spring IoC Container tạo, quản lý và cung cấp cho các thành phần khác sử dụng.

Một object Java bình thường không tự động là bean. Nó chỉ trở thành bean khi được Spring biết đến, ví dụ thông qua `@Component`, `@Service`, `@Repository`, `@Controller`, `@Bean` hoặc cấu hình khác.

Ví dụ:

```java
@Service
public class ProductService {
}
```

`ProductService` là bean vì Spring quét thấy annotation `@Service`.

### Bean khác object thường thế nào?

Object thường:

```java
ProductService service = new ProductService();
```

Bean:

```java
ApplicationContext context = SpringApplication.run(App.class, args);
ProductService service = context.getBean(ProductService.class);
```

Với bean, Spring có thể:

- Tạo object.
- Inject dependency.
- Quản lý lifecycle.
- Áp dụng proxy.
- Quản lý scope.

### Use case thường gặp

- `ProductController` là bean để nhận HTTP request.
- `ProductService` là bean để xử lý nghiệp vụ.
- `ProductRepository` là bean để truy cập database.
- `PasswordEncoder` là bean để dùng chung trong security.

### Lưu ý

Không phải class nào cũng nên là bean. Chỉ nên đưa vào Spring Container những object cần được quản lý, cần inject dependency hoặc cần dùng lại ở nhiều nơi.

DTO, request object, response object và entity thường không cần là bean.

## 4. Khác biệt giữa `@Component`, `@Service`, `@Repository`, `@Controller`, `@RestController`, `@Bean`

Các annotation này đều có thể liên quan đến việc tạo bean, nhưng không nên hiểu là “giống nhau hết”. Mỗi annotation có ý nghĩa riêng để Spring và lập trình viên hiểu class đó thuộc tầng nào, được xử lý theo cơ chế nào.

Nếu chỉ cần tạo bean, dùng `@Component` cũng được. Nhưng trong dự án thực tế, nên dùng annotation đúng vai trò để code rõ nghĩa hơn và tận dụng hành vi riêng của Spring.

### `@Component`

`@Component` là annotation tổng quát nhất để đánh dấu một class là Spring bean. Các annotation như `@Service`, `@Repository`, `@Controller` đều là dạng chuyên biệt của `@Component`.

```java
@Component
public class FileStorage {
}
```

Đặc điểm:

- Được Spring component scan phát hiện và tạo bean.
- Không nói rõ class thuộc tầng nghiệp vụ, dữ liệu hay web.
- Phù hợp với component kỹ thuật hoặc helper dùng chung.

Use case:

- Helper xử lý file.
- Mapper thủ công.
- Client gọi API bên ngoài.
- Component dùng chung không thuộc rõ controller, service hoặc repository.

Lưu ý: Không nên dùng `@Component` cho mọi class chỉ vì nó chạy được. Nếu class là service, repository hoặc controller, nên dùng annotation chuyên biệt để người đọc hiểu đúng vai trò.

### `@Service`

`@Service` là annotation chuyên biệt cho tầng service, nơi chứa use case và business logic. Nó vẫn tạo bean giống `@Component`, nhưng ý nghĩa rõ hơn: class này xử lý nghiệp vụ.

```java
@Service
public class OrderService {
    public void placeOrder() {
        // business logic
    }
}
```

Use case:

- Xử lý use case.
- Kiểm tra rule nghiệp vụ.
- Điều phối nhiều repository hoặc external service.

Đặc điểm:

- Giúp phân biệt tầng nghiệp vụ với helper thông thường.
- Là nơi thường đặt `@Transactional`.
- Thường được controller gọi vào.
- Thường điều phối repository, mapper, validator, external client.

Ví dụ:

```text
OrderController -> OrderService -> OrderRepository
```

Lưu ý: `@Service` không tự làm method có transaction. Muốn có transaction vẫn cần cấu hình transaction hoặc dùng `@Transactional`.

### `@Repository`

`@Repository` là annotation chuyên biệt cho tầng truy cập dữ liệu. Nó biểu thị class hoặc interface này làm việc với database, persistence hoặc storage.

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
```

Đặc điểm:

- Được Spring tạo bean hoặc hỗ trợ tạo implementation, tùy kiểu repository.
- Biểu thị rõ tầng data access.
- Có thể tham gia cơ chế exception translation của Spring, chuyển một số exception tầng persistence thành `DataAccessException`.

Use case:

- Truy vấn database.
- Lưu, cập nhật, xóa entity.
- Định nghĩa query method.

Ví dụ:

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
}
```

Lưu ý: Với Spring Data JPA, interface extends `JpaRepository` thường vẫn được Spring tạo bean dù không cần ghi `@Repository`. Nhưng annotation này vẫn quan trọng khi viết repository thủ công hoặc muốn biểu thị rõ vai trò.

### `@Controller`

`@Controller` dùng cho class xử lý HTTP request trong Spring MVC, thường dùng khi ứng dụng trả về view/template HTML.

```java
@Controller
public class PageController {
    @GetMapping("/home")
    public String home() {
        return "home";
    }
}
```

Đặc điểm:

- Được Spring MVC nhận diện là controller.
- Method có thể mapping với URL bằng `@GetMapping`, `@PostMapping`, `@RequestMapping`.
- Giá trị `String` trả về thường được hiểu là tên view/template.

Use case:

- Web app render HTML.
- Trả về template như Thymeleaf.

Ví dụ `return "home"` thường nghĩa là render template `home.html`, không phải trả chuỗi `"home"` trực tiếp ra response body.

### `@RestController`

`@RestController` dùng cho REST API, thường trả dữ liệu trực tiếp như JSON thay vì view.

```java
@RestController
public class ProductController {
    @GetMapping("/products")
    public List<String> products() {
        return List.of("Book", "Laptop");
    }
}
```

`@RestController` tương đương:

```java
@Controller
@ResponseBody
```

Đặc điểm:

- Được Spring MVC nhận diện là controller.
- Mọi method mặc định trả dữ liệu vào HTTP response body.
- Object Java thường được convert thành JSON nhờ Jackson.

Use case:

- REST API cho web frontend.
- API cho mobile app.
- API cho hệ thống khác gọi.

Ví dụ:

```java
return List.of("Book", "Laptop");
```

Với `@RestController`, kết quả thường là JSON:

```json
["Book", "Laptop"]
```

### `@Bean`

`@Bean` không đặt trên class như `@Component`. Nó đặt trên method trong class cấu hình để Spring lấy object trả về từ method đó và đăng ký thành bean.

```java
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

Đặc điểm:

- Dùng khi muốn tự kiểm soát cách tạo object.
- Phù hợp với class đến từ thư viện bên ngoài, không thể sửa source để thêm annotation.
- Method name thường trở thành bean name, ví dụ `passwordEncoder`.

Use case:

- Tạo bean từ thư viện bên ngoài.
- Bean cần logic khởi tạo riêng.
- Bean không thể sửa source code để thêm `@Component`.

Ví dụ không thể thêm `@Component` vào `BCryptPasswordEncoder` vì đó là class thư viện. Vì vậy ta khai báo bằng `@Bean`.

### Vì sao không dùng `@Component` cho tất cả?

Dùng `@Component` cho tất cả vẫn có thể chạy, nhưng code sẽ mất nhiều thông tin quan trọng.

Ví dụ:

```java
@Component
public class OrderService {
}
```

Code trên vẫn tạo bean, nhưng người đọc không biết đây là service nghiệp vụ hay component kỹ thuật. Viết rõ hơn:

```java
@Service
public class OrderService {
}
```

Tương tự, với controller REST API nên dùng `@RestController`, vì nếu chỉ dùng `@Component`, Spring MVC không xem class đó là controller xử lý request.

### So sánh nhanh

```text
@Component      Bean chung, dùng cho component kỹ thuật hoặc helper
@Service        Bean tầng nghiệp vụ, nơi xử lý use case/business logic
@Repository     Bean tầng dữ liệu, có ý nghĩa data access và exception translation
@Controller     Controller Spring MVC, thường trả view/template
@RestController Controller REST API, mặc định trả response body/JSON
@Bean           Khai báo bean bằng method, thường dùng cho class thư viện hoặc object cần custom init
```

## 5. `@Qualifier` và `@Primary` dùng để làm gì?

Khi Spring thấy có nhiều bean cùng type, nó không biết nên inject bean nào. `@Qualifier` và `@Primary` dùng để xử lý trường hợp đó.

Ví dụ có interface:

```java
public interface PaymentService {
    void pay();
}
```

Có hai implementation:

```java
@Service
public class MomoPaymentService implements PaymentService {
    public void pay() {}
}

@Service
public class VnpayPaymentService implements PaymentService {
    public void pay() {}
}
```

Nếu inject như sau, Spring sẽ bị mơ hồ:

```java
public OrderService(PaymentService paymentService) {
    this.paymentService = paymentService;
}
```

Vì có hai bean cùng type `PaymentService`.

### `@Qualifier`

`@Qualifier` chỉ rõ bean cần inject.

```java
public OrderService(@Qualifier("momoPaymentService") PaymentService paymentService) {
    this.paymentService = paymentService;
}
```

Use case:

- Có nhiều implementation cùng interface.
- Cần chọn chính xác bean cho từng nơi inject.
- Mỗi use case dùng một implementation khác nhau.

### `@Primary`

`@Primary` đánh dấu bean mặc định nếu có nhiều bean cùng type.

```java
@Primary
@Service
public class MomoPaymentService implements PaymentService {
    public void pay() {}
}
```

Khi inject `PaymentService` mà không ghi `@Qualifier`, Spring ưu tiên `MomoPaymentService`.

Use case:

- Có một implementation được dùng phổ biến nhất.
- Các implementation khác chỉ dùng ở vài nơi đặc biệt.

### Khác nhau

```text
@Primary   Chọn bean mặc định
@Qualifier Chọn bean cụ thể tại điểm inject
```

Nếu vừa có `@Primary` vừa có `@Qualifier`, `@Qualifier` thường rõ ràng hơn vì nó chỉ định trực tiếp bean cần dùng.

## 6. Bean Lifecycle là gì?

**Bean Lifecycle** là vòng đời của một bean từ lúc được Spring tạo ra đến lúc bị hủy.

Luồng cơ bản:

```text
Tạo object
-> Inject dependency
-> Gọi callback khởi tạo
-> Bean sẵn sàng sử dụng
-> Gọi callback hủy khi context đóng
```

Ví dụ:

```java
@Component
public class CacheLoader {
    @PostConstruct
    public void init() {
        System.out.println("Load cache");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("Clear resource");
    }
}
```

`@PostConstruct` chạy sau khi bean được tạo và dependency đã được inject. `@PreDestroy` chạy trước khi bean bị hủy.

### Các giai đoạn quan trọng

**Instantiate:** Spring tạo object.

**Populate properties:** Spring inject dependency.

**Initialization callback:** Spring gọi logic khởi tạo như `@PostConstruct`.

**Ready to use:** Bean sẵn sàng được inject và sử dụng.

**Destruction callback:** Khi `ApplicationContext` đóng, Spring gọi logic dọn dẹp như `@PreDestroy`.

### Use case thường gặp

- Load cache sau khi app khởi động.
- Kiểm tra config bắt buộc.
- Mở hoặc đóng resource.
- Đăng ký listener.

### Lưu ý

Không nên nhét business logic chính vào `@PostConstruct`. Logic trong đó chạy khi app khởi động, nếu lỗi có thể làm ứng dụng không start được.

Với prototype bean, Spring tạo bean nhưng không quản lý đầy đủ bước destroy như singleton. Vì vậy cần cẩn thận nếu prototype bean giữ resource.

## 7. Sự khác biệt giữa các scope của Bean: Singleton, Prototype, Request, Session

**Bean scope** quyết định Spring tạo bao nhiêu instance của một bean và instance đó sống trong bao lâu.

### Singleton

Singleton là scope mặc định. Spring tạo một instance duy nhất cho mỗi bean trong một `ApplicationContext`.

```java
@Service
public class ProductService {
}
```

Use case:

- Service.
- Repository.
- Component không lưu state riêng theo user hoặc request.

Lưu ý:

- Singleton không có nghĩa là global singleton của toàn JVM.
- Singleton trong Spring là một bean instance trong một Spring context.
- Không nên lưu dữ liệu request/user vào field của singleton bean.

Ví dụ không nên:

```java
@Service
public class UserService {
    private Long currentUserId;
}
```

Nếu nhiều request chạy đồng thời, dữ liệu có thể bị ghi đè.

### Prototype

Prototype tạo instance mới mỗi lần bean được request từ Spring Container.

```java
@Scope("prototype")
@Component
public class ReportBuilder {
}
```

Use case:

- Object có state riêng cho từng lần sử dụng.
- Builder hoặc processor ngắn hạn.

Lưu ý: Nếu inject prototype bean vào singleton bean trực tiếp, prototype thường chỉ được tạo một lần tại lúc singleton được tạo.

### Request

Request scope tạo một instance cho mỗi HTTP request.

```java
@RequestScope
@Component
public class RequestContext {
}
```

Use case:

- Lưu thông tin tạm trong một request.
- Trace id.
- Thông tin user trong phạm vi request.

Lưu ý: Chỉ dùng được trong web application có request context.

### Session

Session scope tạo một instance cho mỗi HTTP session.

```java
@SessionScope
@Component
public class CartSession {
}
```

Use case:

- Giỏ hàng theo session.
- Dữ liệu tạm theo user session.

Lưu ý: Cẩn thận khi lưu nhiều dữ liệu trong session vì tốn memory server và khó scale nếu hệ thống chạy nhiều instance.

### So sánh nhanh

```text
Singleton  Một instance trong Spring context
Prototype  Instance mới mỗi lần request bean
Request    Một instance cho mỗi HTTP request
Session    Một instance cho mỗi HTTP session
```

## 8. `@ControllerAdvice` và `@ExceptionHandler` là gì?

`@ExceptionHandler` dùng để khai báo method xử lý một loại exception cụ thể.

`@ControllerAdvice` hoặc `@RestControllerAdvice` dùng để gom logic xử lý exception cho nhiều controller.

Ví dụ:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("PRODUCT_NOT_FOUND", ex.getMessage()));
    }
}
```

Khi bất kỳ controller nào ném `ProductNotFoundException`, method trên có thể xử lý và trả response lỗi thống nhất.

### Vì sao cần xử lý lỗi tập trung?

Nếu không có global exception handler, mỗi controller có thể phải tự viết try-catch. Điều này làm code lặp lại, response lỗi không thống nhất và khó bảo trì.

Use case:

- Trả lỗi validation thống nhất.
- Trả `404` khi không tìm thấy resource.
- Trả `400` khi request sai.
- Trả `401` hoặc `403` cho lỗi bảo mật.
- Che giấu lỗi nội bộ, không trả stack trace ra client.

### `@ControllerAdvice` và `@RestControllerAdvice`

`@RestControllerAdvice` tương đương `@ControllerAdvice` kết hợp `@ResponseBody`. Với REST API, thường dùng `@RestControllerAdvice`.

Ví dụ response lỗi:

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "Product not found"
}
```

### Lưu ý

- Không nên trả trực tiếp message kỹ thuật hoặc stack trace cho client.
- Nên chuẩn hóa format lỗi.
- Nên log lỗi server-side với lỗi `5xx`.
- Exception nghiệp vụ nên rõ nghĩa, ví dụ `ProductNotFoundException`, `InsufficientStockException`.

## 9. 5 nhóm HTTP status là gì?

HTTP status code được chia thành 5 nhóm chính.

### 1xx: Informational

Nhóm thông tin. Request đã được nhận và server đang tiếp tục xử lý.

Ít gặp trong REST API thông thường.

Ví dụ:

```text
100 Continue
```

### 2xx: Success

Request xử lý thành công.

Status phổ biến:

```text
200 OK
201 Created
204 No Content
```

Use case:

- `200 OK`: lấy dữ liệu thành công.
- `201 Created`: tạo resource thành công.
- `204 No Content`: xóa thành công nhưng không trả body.

Ví dụ:

```text
POST /products -> 201 Created
```

### 3xx: Redirection

Client cần thực hiện thêm hành động, thường là chuyển hướng.

Status phổ biến:

```text
301 Moved Permanently
302 Found
304 Not Modified
```

Use case:

- Redirect URL.
- Cache validation với `304`.

REST API thường ít dùng redirect hơn web truyền thống.

### 4xx: Client Error

Lỗi do request từ client không hợp lệ hoặc client không có quyền.

Status phổ biến:

```text
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity
```

Use case:

- `400`: request sai format hoặc thiếu field.
- `401`: chưa đăng nhập hoặc token không hợp lệ.
- `403`: đã đăng nhập nhưng không có quyền.
- `404`: không tìm thấy resource.
- `409`: xung đột dữ liệu, ví dụ email đã tồn tại.
- `422`: dữ liệu đúng format nhưng sai rule nghiệp vụ.

### 5xx: Server Error

Lỗi phía server. Client gửi request hợp lệ nhưng server xử lý thất bại.

Status phổ biến:

```text
500 Internal Server Error
502 Bad Gateway
503 Service Unavailable
504 Gateway Timeout
```

Use case:

- Lỗi code không dự kiến.
- Database lỗi.
- External service không phản hồi.
- Hệ thống quá tải.

### Lưu ý khi trả HTTP status

Không nên lúc nào cũng trả `200` rồi nhét lỗi vào body. HTTP status nên phản ánh kết quả xử lý để client, gateway, monitoring và log hiểu đúng.

Ví dụ nên:

```text
GET /products/999 -> 404 Not Found
```

Không nên:

```json
{
  "success": false,
  "status": 200,
  "message": "Product not found"
}
```

## 10. `save()`, `saveAndFlush()` và `flush()` khác nhau thế nào?

Các method này thường gặp khi dùng Spring Data JPA và Hibernate.

### `save()`

`save()` dùng để lưu entity. Tuy nhiên, trong JPA, gọi `save()` không luôn đồng nghĩa SQL được gửi xuống database ngay lập tức.

```java
productRepository.save(product);
```

Trong transaction, Hibernate có thể giữ thay đổi trong persistence context và chỉ flush xuống database khi cần, thường là trước khi commit transaction hoặc trước một số query.

Use case:

- Lưu entity thông thường.
- Không cần database thấy dữ liệu ngay trong cùng thời điểm.

### `flush()`

`flush()` ép Hibernate đồng bộ các thay đổi đang có trong persistence context xuống database ngay tại thời điểm gọi.

```java
productRepository.flush();
```

Lưu ý quan trọng: `flush()` không commit transaction. Nếu transaction rollback sau đó, dữ liệu đã flush vẫn bị rollback.

Use case:

- Muốn SQL được gửi xuống database sớm.
- Muốn phát hiện lỗi constraint sớm, ví dụ unique key.
- Cần database trigger hoặc constraint chạy trước một bước xử lý tiếp theo.

### `saveAndFlush()`

`saveAndFlush()` là lưu entity rồi flush ngay.

```java
productRepository.saveAndFlush(product);
```

Nó gần giống:

```java
productRepository.save(product);
productRepository.flush();
```

Use case:

- Cần lưu và đẩy SQL xuống database ngay.
- Cần biết lỗi database sớm trong method.

### So sánh nhanh

```text
save()         Lưu entity vào persistence context, có thể chưa SQL ngay
flush()        Đẩy các thay đổi hiện có xuống database, không commit
saveAndFlush() save() rồi flush() ngay
```

### Lưu ý phỏng vấn

`flush()` không phải commit. Commit là kết thúc transaction thành công. Flush chỉ đồng bộ SQL xuống database trong transaction hiện tại.

## 11. `@Transactional` là gì và các mode quan trọng?

`@Transactional` dùng để chạy một method trong transaction. Transaction giúp một nhóm thao tác dữ liệu thành công cùng nhau hoặc thất bại cùng nhau.

Ví dụ:

```java
@Transactional
public void placeOrder(CreateOrderRequest request) {
    orderRepository.save(order);
    inventoryService.decreaseStock(request.getProductId());
    paymentService.createPayment(order);
}
```

Nếu có lỗi phù hợp xảy ra, transaction có thể rollback để tránh dữ liệu bị lưu dở dang.

### Propagation

Propagation quyết định method sẽ dùng transaction hiện tại hay tạo transaction mới.

#### `REQUIRED`

Đây là mặc định. Nếu đã có transaction thì dùng transaction hiện tại. Nếu chưa có thì tạo transaction mới.

```java
@Transactional(propagation = Propagation.REQUIRED)
public void createOrder() {}
```

Use case: phần lớn service method ghi dữ liệu.

#### `REQUIRES_NEW`

Luôn tạo transaction mới. Nếu đang có transaction, transaction cũ bị tạm dừng.

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveAuditLog() {}
```

Use case: ghi audit log dù transaction chính có thể rollback.

Lưu ý: Nếu dùng sai, dữ liệu phụ có thể commit dù nghiệp vụ chính thất bại.

#### `SUPPORTS`

Nếu có transaction thì tham gia. Nếu không có thì chạy không transaction.

```java
@Transactional(propagation = Propagation.SUPPORTS)
public Product getProduct() {}
```

Use case: method đọc dữ liệu, có thể chạy trong hoặc ngoài transaction.

#### `MANDATORY`

Bắt buộc phải có transaction trước đó. Nếu không có, Spring ném lỗi.

```java
@Transactional(propagation = Propagation.MANDATORY)
public void updateInventory() {}
```

Use case: method nội bộ phải luôn thuộc transaction của use case lớn.

#### `NOT_SUPPORTED`

Luôn chạy ngoài transaction. Nếu đang có transaction, transaction hiện tại bị tạm dừng.

Use case: gọi tác vụ không cần transaction hoặc không muốn giữ transaction lâu.

#### `NEVER`

Không được chạy trong transaction. Nếu đang có transaction, Spring ném lỗi.

Use case: hiếm gặp, dùng khi logic bắt buộc không được nằm trong transaction.

#### `NESTED`

Chạy trong nested transaction nếu có transaction hiện tại, thường dựa trên savepoint. Nếu nested rollback, transaction ngoài có thể vẫn tiếp tục.

Use case: rollback một phần trong một transaction lớn.

Lưu ý: Không phải transaction manager nào cũng hỗ trợ `NESTED`.

### Isolation

Isolation quyết định mức độ transaction này nhìn thấy dữ liệu từ transaction khác.

Các vấn đề thường nhắc:

- Dirty read: đọc dữ liệu chưa commit.
- Non-repeatable read: đọc cùng một row hai lần ra kết quả khác nhau.
- Phantom read: query cùng điều kiện hai lần nhưng số row khác nhau.

Các mức phổ biến:

```text
READ_UNCOMMITTED  Có thể dirty read
READ_COMMITTED    Chỉ đọc dữ liệu đã commit
REPEATABLE_READ   Giảm non-repeatable read
SERIALIZABLE      Chặt nhất, dễ giảm hiệu năng
```

Use case:

- Hầu hết ứng dụng dùng default của database.
- Chỉ chỉnh isolation khi có vấn đề cạnh tranh dữ liệu rõ ràng.

### Rollback

Mặc định, Spring rollback với unchecked exception như `RuntimeException` và `Error`. Checked exception thường không rollback nếu không cấu hình thêm.

Ví dụ rollback với checked exception:

```java
@Transactional(rollbackFor = IOException.class)
public void importData() throws IOException {
}
```

### readOnly

`readOnly = true` báo rằng transaction chủ yếu để đọc.

```java
@Transactional(readOnly = true)
public Product getProduct(Long id) {
    return productRepository.findById(id).orElseThrow();
}
```

Use case:

- Method chỉ đọc dữ liệu.
- Giúp provider/database có cơ hội tối ưu.
- Giúp thể hiện rõ ý định của method.

### timeout

`timeout` giới hạn thời gian transaction.

```java
@Transactional(timeout = 5)
public void process() {}
```

Use case:

- Tránh transaction chạy quá lâu.
- Hữu ích với tác vụ có nguy cơ treo.

### Lưu ý quan trọng

`@Transactional` thường hoạt động thông qua proxy. Vì vậy, self-invocation có thể không kích hoạt transaction.

Ví dụ dễ lỗi:

```java
@Service
public class OrderService {
    public void create() {
        this.saveOrder();
    }

    @Transactional
    public void saveOrder() {
    }
}
```

Gọi `this.saveOrder()` bên trong cùng class có thể bỏ qua proxy, làm `@Transactional` không hoạt động như mong đợi.

## 12. Fetch Type LAZY và EAGER khác nhau thế nào?

Fetch type quyết định khi load một entity thì relationship liên quan có được load ngay hay không.

### LAZY

LAZY nghĩa là chưa load relationship ngay. Khi nào truy cập vào relationship thì Hibernate mới query thêm.

```java
@OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
private List<OrderItem> items;
```

Use case:

- Relationship có nhiều dữ liệu.
- Không phải lúc nào cũng cần dữ liệu liên quan.
- Tránh query quá nặng.

Ví dụ: Load `Order` trước, chỉ khi gọi `order.getItems()` mới load danh sách item.

### EAGER

EAGER nghĩa là load relationship ngay khi load entity chính.

```java
@ManyToOne(fetch = FetchType.EAGER)
private Customer customer;
```

Use case:

- Relationship nhỏ.
- Gần như lúc nào cũng cần dữ liệu liên quan.

Lưu ý: EAGER dễ làm query nặng, load thừa dữ liệu và khó kiểm soát khi entity có nhiều quan hệ.

### So sánh nhanh

```text
LAZY   Cần mới load
EAGER  Load ngay
```

### Lưu ý quan trọng

LAZY có thể gây `LazyInitializationException` nếu truy cập relationship sau khi transaction/session đã đóng.

Ví dụ:

```java
Order order = orderRepository.findById(id).orElseThrow();
return order.getItems().size();
```

Nếu `items` là LAZY và session đã đóng trước khi truy cập, có thể lỗi.

Cách xử lý thường gặp:

- Fetch đúng dữ liệu cần ngay trong query.
- Dùng `JOIN FETCH`.
- Dùng DTO projection.
- Map dữ liệu trong transaction.

## 13. N+1 query là gì và xử lý thế nào?

**N+1 query** là vấn đề khi ứng dụng chạy 1 query để lấy danh sách entity chính, sau đó chạy thêm N query để lấy dữ liệu liên quan cho từng entity.

Ví dụ có 10 orders:

```text
1 query  lấy danh sách 10 orders
10 query lấy items cho từng order
```

Tổng cộng 11 query. Nếu có 1000 orders thì có thể thành 1001 query.

### Ví dụ gây N+1

```java
List<Order> orders = orderRepository.findAll();

for (Order order : orders) {
    System.out.println(order.getItems().size());
}
```

Nếu `items` là LAZY, mỗi lần gọi `order.getItems()` có thể phát sinh thêm một query.

### Vì sao nguy hiểm?

N+1 query thường không lộ rõ khi dữ liệu ít. Nhưng khi dữ liệu tăng, API có thể chậm đột ngột vì số lượng query tăng theo số record.

Dấu hiệu:

- Log SQL có rất nhiều query giống nhau.
- API list dữ liệu chậm bất thường.
- Database bị tải cao.
- Mỗi item trong danh sách lại query thêm dữ liệu con.

### Cách xử lý

#### Dùng `JOIN FETCH`

```java
@Query("select distinct o from Order o join fetch o.items")
List<Order> findAllWithItems();
```

Cách này lấy `Order` và `items` trong một query.

Use case:

- Cần load entity chính và relationship ngay.
- Relationship cần dùng trong response.

Lưu ý: Với collection, cần cẩn thận phân trang vì join có thể làm nhân số dòng.

#### Dùng `@EntityGraph`

```java
@EntityGraph(attributePaths = "items")
List<Order> findAll();
```

`@EntityGraph` cho phép chỉ định relationship cần fetch mà không viết JPQL thủ công.

Use case:

- Muốn giữ method repository ngắn gọn.
- Cần fetch thêm một số quan hệ cụ thể.

#### Dùng DTO Projection

```java
public interface OrderSummary {
    Long getId();
    BigDecimal getTotalAmount();
}
```

Use case:

- API chỉ cần một phần dữ liệu.
- Không cần load toàn bộ entity và relationship.

DTO projection thường giúp query nhẹ hơn và tránh load thừa dữ liệu.

#### Batch fetching

Có thể cấu hình Hibernate batch fetch để giảm số query khi load LAZY relationship.

Ví dụ:

```yaml
spring:
  jpa:
    properties:
      hibernate.default_batch_fetch_size: 50
```

Thay vì query từng relationship một, Hibernate có thể gom nhiều id vào một query `IN`.

### Lưu ý khi trả lời phỏng vấn

Không nên nói cứ đổi LAZY thành EAGER là giải quyết N+1. EAGER có thể làm hệ thống load thừa dữ liệu và phát sinh vấn đề hiệu năng khác.

Cách tốt hơn là fetch đúng dữ liệu theo từng use case:

- API cần relationship thì dùng `JOIN FETCH`, `@EntityGraph` hoặc DTO query.
- API không cần relationship thì giữ LAZY.
- Danh sách lớn thì cẩn thận với join fetch collection và pagination.
