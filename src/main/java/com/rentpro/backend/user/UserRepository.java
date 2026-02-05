package com.rentpro.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByOwner_Id(Long ownerId);

    Optional<User> findByIdAndOwner_Id(Long id, Long ownerId); 
}
