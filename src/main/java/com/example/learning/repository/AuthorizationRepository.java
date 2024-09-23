package com.example.learning.repository;

import java.util.Optional;

import com.example.learning.entity.AuthorizationEntity;
import org.apache.tomcat.util.http.parser.Authorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorizationRepository extends JpaRepository<AuthorizationEntity, String> {

    Optional<AuthorizationEntity> findByState(String state);
    Optional<AuthorizationEntity> findByAuthorizationCodeValue(String authorizationCode);
    Optional<AuthorizationEntity> findByAccessTokenValue(String accessToken);
    Optional<AuthorizationEntity> findByRefreshTokenValue(String refreshToken);
    @Query("select a from AuthorizationEntity a where a.state = :token" +
            " or a.authorizationCodeValue = :token" +
            " or a.accessTokenValue = :token" +
            " or a.refreshTokenValue = :token"
    )
    Optional<AuthorizationEntity> findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValue(@Param("token") String token);
}
