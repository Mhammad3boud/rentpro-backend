package com.rentpro.backend.property;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PropertyPhotoRepository extends JpaRepository<PropertyPhoto, UUID> {
    List<PropertyPhoto> findByProperty_PropertyId(UUID propertyId);
}
