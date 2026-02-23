package com.rentpro.backend.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiPredictionRepository extends JpaRepository<AiPrediction, UUID> {

    Optional<AiPrediction> findTopByLease_LeaseIdAndPredictionTypeOrderByPredictedAtDesc(
            UUID leaseId, PredictionType predictionType
    );
}
