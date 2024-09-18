package com.example.learning.security_config;

import com.example.learning.repository.client.ClientRepository;
import com.example.learning.repository.client.JpaRegisteredClientRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.UUID;

// https://www.appsdeveloperblog.com/spring-authorization-server-tutorial/
// config model: https://docs.spring.io/spring-authorization-server/reference/1.4/configuration-model.html
// iss: https://datatracker.ietf.org/doc/html/rfc8414#section-2
// OAuth2 Client authentication: https://datatracker.ietf.org/doc/html/rfc6749#section-2.3
@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class OAuth2Config {
    private final ClientRepository clientRepository;

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
                .apply(authorizationServerConfigurer);

        authorizationServerConfigurer
                .registeredClientRepository(registeredClientRepository())
                .authorizationServerSettings(authorizationServerSettings());
//                .clientAuthentication(clientAuthentication -> {
//                    clientAuthentication
//                            .authenticationConverter(authenticationConverter());
////                            .authenticationConverters(authenticationConvertersConsumer)
////                            .authenticationProvider(authenticationProvider());
////                            .authenticationProviders(authenticationProvidersConsumer)
////                            .authenticationSuccessHandler(authenticationSuccessHandler)
////                            .errorResponseHandler(errorResponseHandler);
//                });
        return http.build();
    }

//    public AuthenticationConverter authenticationConverter() {
//        return new AuthenticationConverter() {
//            @Override
//            public Authentication convert(HttpServletRequest request) {
//                String authorizationHeader = request.getHeader("kaka");
//                if (Objects.isNull(authorizationHeader)) {
//                    System.out.println("NULL VL");
//                    throw new RuntimeException("null roi`");
//                    // Xử lý header Authorization
//                }
//                System.out.println("KO NULL");
//                return null;
//            }
//        };
//    }

//    public AuthenticationProvider authenticationProvider(){
//        return new OAuth2ClientCredentialsAuthenticationProvider(new InMemoryOAuth2AuthorizationService(),
//                new OAuth2AccessTokenGenerator());
//    }

    // config uri protocol endpoint + iss
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .build();
    }

    // register client
    private RegisteredClientRepository registeredClientRepository() {
        return new JpaRegisteredClientRepository(clientRepository);
    }


//    private RegisteredClientRepository registeredClientRepository() {
//        return new InMemoryRegisteredClientRepository(registeredClients());
//    }


//    private List<RegisteredClient> registeredClients() {
//        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
//                .clientId("client2")
//                .clientSecret("{noop}myClientSecretValue")
//                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
//                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
//                .scope("scope-a")
//                .build();
//        return List.of(registeredClient);
//    }
}
//curl --location 'http://localhost:8080/oauth2/token' \
//        --header 'Authorization: Basic Y2xpZW50MTpteUNsaWVudFNlY3JldFZhbHVl' \
//        --header 'Content-Type: application/x-www-form-urlencoded' \
//        --data-urlencode 'grant_type=client_credentials'

////{
////        "access_token": "eyJraWQiOiI1OTM2YmJiNi01M2JhLTQ4NDUtYjllYS05ZWRlOGIzY2FmMDciLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJjbGllbnQxIiwiYXVkIjoiY2xpZW50MSIsIm5iZiI6MTcyNjUxNzg3MiwiaXNzIjoiaHR0cDovL2F1dGgtc2VydmVyOjgwMDAiLCJleHAiOjE3MjY1MTgxNzIsImlhdCI6MTcyNjUxNzg3MiwianRpIjoiZDc1NDkzNDEtNDAyYS00OTU2LTg4NzQtZGFjYTM1OTYzMmE0In0.fpfLs_jfF5VFarYeFEf9nwugFaNvNktkSdPJlBp_GYb8O6cdNR88MYBnZ438SqBQlxbIPdFvkv52vFAxyMbqaIIaapmVkEqAEsodC9Zlf-GLZpPduhLVneNzyY3eJydFtoDUMmnk9cCa4obruDi2UC8RUC05-8_XeJ9bl01Tof7llM1bfWftFH4ztaYkopguRrir3a0kIMD_9Xh7JJ7sZqGhYIC-gQ6IisZmCJKnUPx1yst7YRQmUw_MrAYPciMowfAyW-688dbIx-r4ZvYtbf4C2RnuhZpLPftTA4Am1u4ZZ1fClBu_WDjFbR8PfOMjIqAfwijMAUSTRcagBBVArA",
////        "token_type": "Bearer",
////        "expires_in": 300
////        }
