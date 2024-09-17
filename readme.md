# _OAuth2AuthorizationServerProperties_
+ Class sử dụng để customize properties cho Authorization Server
+ Dạng file config: application.yaml


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
+ Đây là 1 class cung cấp config cơ bản cho OAuth2 Authorization Server
+ Nhưng trên thực tế thì hiếm khi dùng, mà thường dùng qua OAuth2AuthorizationServerConfigurer

 
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

# _OAuth2AuthorizationServerConfigurer_
+ Đây là class phổ biến nhất để customize đầy đủ cho OAuth2 Authorization Server

    
    // sử dụng để config apply auth với OAuth2
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers("/**").permitAll()
                        .anyRequest().authenticated())
                .apply(authorizationServerConfigurer); // (*) sử dụng để apply với filter chain
        return http.build();
    }

### _AuthorizationServerSettings_ class
+ Config uri protocol endpoint và iss
+ Nếu không chỉ rõ iss cho config AuthorizationServerSettings
+ _AuthorizationServerContext_ class thì nắm giữ thông tin về Authorization Server tại runtime cấp khả năng access 
vào AuthorizationServerSettings và current iss
+ Nếu không config iss trong AuthorizationServerSettings class thì có thể lấy nó trong current request qua 
_AuthorizationServerContextHolder_ class (associate với current request sử dụng Thread Local) để lấy được _AuthorizationServerContext_


    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
    authorizationServerConfigurer.authorizationServerSettings(authorizationServerSettings());

    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .build();
    }

### _OAuth2ClientAuthenticationConfigurer_ class
+ Sử dụng để config OAuth2 client authentication request của client
+ Support xử lí thêm phần pre-processing, main-processing, post-processing logic trong 1 client authentication request
+ OAuth2ClientAuthenticationConfigurer sẽ config OAuth2ClientAuthenticationFilter và đăng kí với OAuth2 authorization 
server SecurityFilterChain @Bean + OAuth2ClientAuthenticationFilter là Filter mà processes client authentication requests.
+ Sau cùng, nó sẽ convert current HttpServletRequest thành 1 instance của OAuth2ClientAuthenticationToken để các step tiếp theo 
dựa vào instance này để authen


    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
    .clientAuthentication(clientAuthentication ->
			clientAuthentication
				.authenticationConverter(authenticationConverter)   
				.authenticationConverters(authenticationConvertersConsumer) 
				.authenticationProvider(authenticationProvider) 
				.authenticationProviders(authenticationProvidersConsumer)   
				.authenticationSuccessHandler(authenticationSuccessHandler) 
				.errorResponseHandler(errorResponseHandler) 
		);

