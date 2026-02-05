package com.rentpro.backend.property;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findAllByOwner_Id(Long ownerId);

    Optional<Property> findByIdAndOwner_Id(Long id, Long ownerId);
}
