package com.rentpro.backend.ai;

import com.rentpro.backend.lease.Lease;
import com.rentpro.backend.lease.LeaseRepository;
import com.rentpro.backend.payment.RentPayment.PaymentStatus;
import com.rentpro.backend.payment.RentPayment;
import com.rentpro.backend.payment.RentPaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AiPredictionService {

    private final AiPredictionRepository aiPredictionRepository;
    private final LeaseRepository leaseRepository;
    private final RentPaymentRepository rentPaymentRepository;

    public AiPredictionService(AiPredictionRepository aiPredictionRepository,
                               LeaseRepository leaseRepository,
                               RentPaymentRepository rentPaymentRepository) {
        this.aiPredictionRepository = aiPredictionRepository;
        this.leaseRepository = leaseRepository;
        this.rentPaymentRepository = rentPaymentRepository;
    }

    public AiPrediction generateLatePaymentPrediction(UUID leaseId) {

        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new RuntimeException("Lease not found"));

        List<RentPayment> payments = rentPaymentRepository.findByLease_LeaseId(leaseId);

        if (payments.isEmpty()) {
            // No history => Low risk by default
            return savePrediction(lease, PredictionType.LATE_PAYMENT,
                    new BigDecimal("0.00"), RiskLevel.LOW);
        }

        long total = payments.size();

        long overdueCount = payments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.OVERDUE)
                .count();

        long partialCount = payments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PARTIAL)
                .count();

        // Recent late flag: any OVERDUE in last 3 months (based on dueDate if present)
        LocalDate cutoff = LocalDate.now().minusMonths(3);
        boolean recentLate = payments.stream()
                .filter(p -> p.getDueDate() != null)
                .filter(p -> !p.getDueDate().isBefore(cutoff))
                .anyMatch(p -> p.getPaymentStatus() == PaymentStatus.OVERDUE);

        double lateRatio = (double) overdueCount / (double) total;     // 0..1
        double partialRatio = (double) partialCount / (double) total;  // 0..1

        // Weighted score => 0..100
        double score =
                (lateRatio * 60.0) +
                (partialRatio * 25.0) +
                (recentLate ? 15.0 : 0.0);

        if (score < 0) score = 0;
        if (score > 100) score = 100;

        RiskLevel level =
                score < 35 ? RiskLevel.LOW :
                score < 65 ? RiskLevel.MEDIUM :
                RiskLevel.HIGH;

        BigDecimal riskScore = BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);

        return savePrediction(lease, PredictionType.LATE_PAYMENT, riskScore, level);
    }

    public AiPrediction getLatest(UUID leaseId, PredictionType type) {
        return aiPredictionRepository
                .findTopByLease_LeaseIdAndPredictionTypeOrderByPredictedAtDesc(leaseId, type)
                .orElse(null);
    }

    private AiPrediction savePrediction(Lease lease, PredictionType type, BigDecimal score, RiskLevel level) {
        AiPrediction p = new AiPrediction(
                UUID.randomUUID(),
                lease,
                type,
                score,
                level,
                LocalDateTime.now()
        );
        return aiPredictionRepository.save(p);
    }

    public AiPrediction getPredictionById(UUID predictionId) {
        return aiPredictionRepository.findById(predictionId)
                .orElseThrow(() -> new RuntimeException("Prediction not found"));
    }

    public void validateLeaseAccess(UUID userId, UUID leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new RuntimeException("Lease not found"));
        
        // Check if user is the property owner or the tenant
        boolean isOwner = lease.getProperty().getOwner().getUserId().equals(userId);
        boolean isTenant = lease.getTenant().getUser().getUserId().equals(userId);
        
        if (!isOwner && !isTenant) {
            throw new RuntimeException("Unauthorized: You do not have access to this lease");
        }
    }
}
