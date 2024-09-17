# _OAuth2AuthorizationServerProperties_
    
    // Class sử dụng để config properties cho OAuth2 Authorization server
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