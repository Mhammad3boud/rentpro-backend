package com.rentpro.backend.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, UUID> {

    List<ContractTemplate> findByOwner_UserId(UUID ownerId);

    Optional<ContractTemplate> findByOwner_UserIdAndIsDefault(UUID ownerId, boolean isDefault);

    List<ContractTemplate> findByOwner_UserIdOrderByCreatedAtDesc(UUID ownerId);
}
