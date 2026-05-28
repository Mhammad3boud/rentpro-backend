package com.rentpro.backend.push;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    List<DeviceToken> findByUser_UserId(UUID userId);
    void deleteByUser_UserIdAndToken(UUID userId, String token);
    void deleteByUser_UserId(UUID userId);
}
