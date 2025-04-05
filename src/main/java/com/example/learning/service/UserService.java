package com.example.learning.service;

import com.example.learning.dto.IdNameResponse;
import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import com.example.learning.inboxoutbox.common.payload.Custom2Dto;
import com.example.learning.inboxoutbox.common.payload.Custom3Dto;
import com.example.learning.inboxoutbox.common.payload.CustomDto;
import com.example.learning.inboxoutbox.outbox.handler.OutboxService;
import com.example.learning.mapper.UserMapper;
import com.example.learning.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional(readOnly = true)
    public List<UserEntity> getUsers() {
        List<UserEntity> userEntities = userRepository.findAll();
        return Objects.nonNull(userEntities) ? userEntities : new ArrayList<>();
    }

    @Transactional
    @SneakyThrows
    public IdNameResponse signUp(UserSignUpRequest userSignUpRequest) {
        UserEntity userEntity = userMapper.getEntityBy(userSignUpRequest);
        userRepository.save(userEntity);
        outboxService.persistOutbox(userEntity.getId().toString(), "sign-up", "my-topic", createMessage());
        return IdNameResponse.builder()
                .id(userEntity.getId())
                .name(userEntity.getUsername())
                .build();
    }

//    @Transactional
//    @SneakyThrows
//    public IdNameResponse signUp(UserSignUpRequest userSignUpRequest) {
//        UserEntity userEntity = userMapper.getEntityBy(userSignUpRequest);
//        userRepository.save(userEntity);
//        sendMessageToKafka(objectMapper.writeValueAsString(createMessage()));
//        if (Objects.nonNull(userEntity.getId())) {
//            throw new RuntimeException("database fail !!!");
//        }
//        return IdNameResponse.builder()
//                .id(userEntity.getId())
//                .name(userEntity.getUsername())
//                .build();
//    }

    private void sendMessageToKafka(String message) {
        kafkaTemplate.send("my-topic", message);
    }

    private CustomDto createMessage() {
        return CustomDto.builder()
                .id(1L)
                .name(UUID.randomUUID().toString())
                .list(List.of(
                        Custom2Dto.builder()
                                .id(2L)
                                .code("code 1")
                                .name("name 1")
                                .build(),
                        Custom2Dto.builder()
                                .id(3L)
                                .code("code 2")
                                .name("name 2")
                                .build()
                ))
                .custom(
                        Custom3Dto.builder()
                                .id(4L)
                                .custom("custom")
                                .build()
                )
                .build();
    }
}
