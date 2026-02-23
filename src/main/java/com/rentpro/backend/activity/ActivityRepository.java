package com.rentpro.backend.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    
    List<Activity> findByUser_UserIdOrderByCreatedAtDesc(UUID userId);
    
    Page<Activity> findByUser_UserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    List<Activity> findTop10ByUser_UserIdOrderByCreatedAtDesc(UUID userId);
    
    List<Activity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);
}
