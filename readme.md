# _OAuth2AuthorizationServerProperties_
    
    // Class sử dụng để full-customize cho Authorization Server
    @ConfigurationProperties(prefix = "spring.security.oauth2.authorizationserver")
    public class OAuth2AuthorizationServerProperties implements InitializingBean {
        // đại diện cho Authorization Server Issuer Identificaion
        private String issuer;
	
        // đại diện cho các Client đã đăng kí với OAuth2
	    private final Map<String, Client> client = new HashMap<>();
        
        // sử dụng để config các endpoint tới OAuth2
        private final Endpoint endpoint = new Endpoint();
    }

    // là inner class trong OAuth2AuthorizationServerProperties
    public class static Client {
        @NestedConfigurationProperty
        // thông tin client để đăng kí
        private final Registration registration = new Registration();
        
        // PKCE workflow khi dùng Auth Code flow
        private boolean requireProofKey = false;
    }


# _OAuth2AuthorizationServerConfiguration_
https://docs.spring.io/spring-authorization-server/docs/current/api/org/springframework/security/oauth2/server/authorization/config/annotation/web/configuration/OAuth2AuthorizationServerConfiguration.html
+ Đây là 1 class cung cấp _default-mini config_ cho OAuth2 Authorization Server

 
    @Configuration(proxyBeanMethods = false)
    public class OAuth2AuthorizationServerConfiguration {
        
        // tạo bean FilterChain để config auth cho OAuth2 endpoint
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
            // được config với các endpoint như: đọc thêm ở đây
            https://docs.spring.io/spring-authorization-server/reference/configuration-model.html#
        }
        
        // tạo bean JwtDecoder sử dụng để xác thực, giải mã JWK
        @Bean
        public static JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource);
    }

https://chatgpt.com/c/66e93a36-b5b0-8010-a1ec-b6d86921c196
