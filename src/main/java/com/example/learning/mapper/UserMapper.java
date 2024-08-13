package com.example.learning.mapper;

import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {
    UserEntity getEntityBy(UserSignUpRequest userSignUpRequest);
}
