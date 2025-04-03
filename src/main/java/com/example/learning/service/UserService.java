package com.example.learning.service;

import com.example.learning.dto.IdNameResponse;
import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import com.example.learning.mapper.UserMapper;
import com.example.learning.inboxoutbox.outbox.handler.OutboxService;
import com.example.learning.inboxoutbox.common.payload.Custom2Dto;
import com.example.learning.inboxoutbox.common.payload.Custom3Dto;
import com.example.learning.inboxoutbox.common.payload.CustomDto;
import com.example.learning.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final OutboxService outboxService;

    @Transactional(readOnly = true)
    public List<UserEntity> getUsers(){
        List<UserEntity> userEntities = userRepository.findAll();
        return Objects.nonNull(userEntities) ? userEntities : new ArrayList<>();
    }

    @Transactional
    public IdNameResponse signUp(UserSignUpRequest userSignUpRequest){
        UserEntity userEntity = userMapper.getEntityBy(userSignUpRequest);
        userRepository.save(userEntity);

        CustomDto customDto = CustomDto.builder()
            .id(1L)
            .name("name")
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

        outboxService.persistOutbox("1", "oke", customDto);
        return IdNameResponse.builder()
                .id(userEntity.getId())
                .name(userEntity.getUsername())
                .build();
    }
}
