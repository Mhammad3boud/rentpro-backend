package com.rentpro.backend.ai;

import com.rentpro.backend.lease.Lease;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_predictions")
public class AiPrediction {

    @Id
    @Column(name = "prediction_id")
    private UUID predictionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lease_id", nullable = false)
    private Lease lease;

    @Enumerated(EnumType.STRING)
    @Column(name = "prediction_type", nullable = false)
    private PredictionType predictionType;

    // DECIMAL(5,2) -> BigDecimal
    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "predicted_at", nullable = false)
    private LocalDateTime predictedAt;

    public AiPrediction() {}

    public AiPrediction(UUID predictionId, Lease lease, PredictionType predictionType,
                        BigDecimal riskScore, RiskLevel riskLevel, LocalDateTime predictedAt) {
        this.predictionId = predictionId;
        this.lease = lease;
        this.predictionType = predictionType;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.predictedAt = predictedAt;
    }

    public UUID getPredictionId() { return predictionId; }
    public void setPredictionId(UUID predictionId) { this.predictionId = predictionId; }

    public Lease getLease() { return lease; }
    public void setLease(Lease lease) { this.lease = lease; }

    public PredictionType getPredictionType() { return predictionType; }
    public void setPredictionType(PredictionType predictionType) { this.predictionType = predictionType; }

    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public LocalDateTime getPredictedAt() { return predictedAt; }
    public void setPredictedAt(LocalDateTime predictedAt) { this.predictedAt = predictedAt; }
}
