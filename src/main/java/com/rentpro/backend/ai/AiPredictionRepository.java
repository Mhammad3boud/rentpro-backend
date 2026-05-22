package com.rentpro.backend.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiPredictionRepository extends JpaRepository<AiPrediction, UUID> {

    Optional<AiPrediction> findTopByLease_LeaseIdAndPredictionTypeOrderByPredictedAtDesc(
            UUID leaseId, PredictionType predictionType
    );

    List<AiPrediction> findByLease_Property_Owner_UserIdOrderByPredictedAtDesc(UUID ownerId);
}
