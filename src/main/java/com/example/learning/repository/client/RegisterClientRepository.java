package com.example.learning.repository.client;

import com.example.learning.entity.ClientEntity;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
//@AllArgsConstructor
@Slf4j
public class RegisterClientRepository implements RegisteredClientRepository {
    private final ClientJpaRepository clientJpaRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RegisterClientRepository(ClientJpaRepository clientJpaRepository) {
        Assert.notNull(clientJpaRepository, "clientRepository cannot be null");
        this.clientJpaRepository = clientJpaRepository;

        ClassLoader classLoader = RegisterClientRepository.class.getClassLoader();
        List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
        this.objectMapper.registerModules(securityModules);
        this.objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        log.warn("save success client !!!");
        this.clientJpaRepository.save(toEntity(registeredClient));
    }

    @Override
    public RegisteredClient findById(String id) {
        log.warn("find success client by id !!!");
        return this.clientJpaRepository.findById(Long.valueOf(id)).map(this::toObject).orElse(null);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        log.warn("find by client id success !!!");
        Optional<ClientEntity> clientEntity = clientJpaRepository.findByClientId(clientId);
        return clientEntity.map(this::toObject).orElse(null);
    }

    private ClientEntity toEntity(RegisteredClient registeredClient) {
        ClientEntity entity = new ClientEntity();
        entity.setClientId(registeredClient.getClientId());
//        entity.setClientSecret("{noop}" + registeredClient.getClientSecret());
        entity.setClientSecret(registeredClient.getClientSecret());
        return entity;
    }

    private RegisteredClient toObject(ClientEntity client) {
        RegisteredClient.Builder builder = RegisteredClient.withId(String.valueOf(client.getId()))
                .clientId(client.getClientId())
//                .clientSecret("{noop}" + client.getClientSecret())
                .clientSecret(client.getClientSecret())
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(new AuthorizationGrantType("custom_password"));
        return builder.build();
    }
}
