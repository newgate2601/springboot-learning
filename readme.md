# Support multi-tenant class

### DisposableBean
+ Đây là 1 Functional interface sử dụng define 1 loạt các logic sẽ xảy ra khi Bean mà implement interface này bị destroy
+ Bị destroy nghĩa là việc xóa bean có chủ đích như chủ động tắt application chả hạn
+ Thường được sử dụng để dọn dẹp tài nguyên sau khi application bị tắt, với example multi-tenant của tôi thì sẽ dọn sạch 
Datasource trước khi tắt application (vì quá nhiều database connection nên phải dọn)


    @Component
    @Slf4j
    public class ConnectionKiller implements DisposableBean {
        @Override
        public void destroy() throws Exception { // log ra console sau khi hữu ý tắt application
            log.info("ConnectionKiller destroy !!!");
        }
    }

### ThreadLocal
+ Đây là 1 class mà thiết kế để lưu giá trị riêng cho mỗi Thread mà không ảnh hưởng tới các Thread khác (thread safe)
+ Tuy nhiên phải cẩn thận khi sử dụng, nếu không chủ động xóa các giá trị trong LocalThread có thể gây leak memory -> cần dùng remove() sau khi sử dụng xong

### RequestInterceptor
+ Đây là 1 Interface sử dụng để thêm thông tin vào Feign request như thêm thắt Header, request param trước khi gửi sang service khác

    
    @Component
    public class FeignTenantInterceptor implements RequestInterceptor {

        @Override
        public void apply(RequestTemplate template) {
            template.header(TenantProperties.TENANT_HEADER, TenantContext.getTenantId());
        }
    }

# Main multi-tenant support class

### MultiTenantConnectionProvider<T>
+ Đây là 1 interface sử dụng để các triển khai của nó có thể quản lý connection cho multi-tenant trong application


    public interface MultiTenantConnectionProvider <T> extends org.hibernate.service.Service, org.hibernate.service.spi.Wrapped {
        // trả về connetion default mà khi không chỉ định tenant
        java.sql.Connection getAnyConnection() throws java.sql.SQLException;
        
        // giải phóng 1 connection cụ thể, trả về connection pool
        void releaseAnyConnection(java.sql.Connection connection) throws java.sql.SQLException;

        // lấy connection dựa vào thông tin tenant (thường là tenantId) để giúp mỗi tenant sẽ sử dụng connection của riêng họ
        java.sql.Connection getConnection(T t) throws java.sql.SQLException;

        void releaseConnection(T t, java.sql.Connection connection) throws java.sql.SQLException;
        
        // nếu là true, chủ động tự release connection khi không còn được sử dụng
        boolean supportsAggressiveRelease();
    }

### AbstractDataSourceBasedMultiTenantConnectionProviderImpl <T>
+ Đây là 1 class triển khai basic nhất của MultiTenantConnectionProvider <T> dựa trên DataSource


    public abstract class AbstractDataSourceBasedMultiTenantConnectionProviderImpl<T> 
                                                                implements MultiTenantConnectionProvider<T> {
        // lấy DataSource mà không có thông tin tenant cụ thể
        protected abstract DataSource selectAnyDataSource();

        // lấy DataSource dựa vào thông tin tenant (thường là tenantId)
        protected abstract DataSource selectDataSource(T tenantIdentifier);
    }

### CurrentTenantIdentifierResolver<T>
+ Đây là 1 interface để hỗ trợ xác định request là tenant nào trong request context hiện tại


    public interface CurrentTenantIdentifierResolver<T> {

        // trả về thông tin tenant của request context hiện tại (thường là tenantId)
	    T resolveCurrentTenantIdentifier();

        
	    boolean validateExistingCurrentSessions();


	    default boolean isRoot(T tenantId) {
		    return false;
	    }
    }
