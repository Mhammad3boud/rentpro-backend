package com.rentpro.backend.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByUser_IdOrderByCreatedAtDesc(Long userId);
    List<Notification> findTop50ByUser_IdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
}
