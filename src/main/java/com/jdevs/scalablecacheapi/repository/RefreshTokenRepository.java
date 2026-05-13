package com.jdevs.scalablecacheapi.repository;

import com.jdevs.scalablecacheapi.entity.RefreshToken;
import com.jdevs.scalablecacheapi.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByUserAndRevokedFalse(AppUser user);
}
