package com.example.learning.mapper;

import com.example.learning.dto.UpdateUserRequest;
import com.example.learning.dto.UserSignUpRequest;
import com.example.learning.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper
public interface UserMapper {
    UserEntity getEntityBy(UserSignUpRequest userSignUpRequest);
    void updateEntity(@MappingTarget UserEntity userEntity, UpdateUserRequest userSignUpRequest);
}
