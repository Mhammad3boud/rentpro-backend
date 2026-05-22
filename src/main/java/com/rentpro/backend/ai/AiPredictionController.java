package com.rentpro.backend.ai;

import com.rentpro.backend.security.JwtUserContext;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/predictions")
public class AiPredictionController {

    private final AiPredictionService aiPredictionService;

    public AiPredictionController(AiPredictionService aiPredictionService) {
        this.aiPredictionService = aiPredictionService;
    }

    record PredictionDTO(
            UUID predictionId,
            UUID leaseId,
            String tenantName,
            String propertyName,
            String unitNumber,
            String predictionType,
            double riskScore,
            String riskLevel,
            String predictedAt
    ) {}

    // All predictions for the authenticated owner
    @GetMapping("/owner")
    public List<PredictionDTO> getOwnerPredictions(Authentication auth) {
        UUID ownerId = resolveUserId(auth);
        return aiPredictionService.getAllPredictionsForOwner(ownerId).stream()
                .map(p -> {
                    var lease = p.getLease();
                    String tenant = lease.getTenant() != null ? lease.getTenant().getFullName() : null;
                    String property = lease.getProperty() != null ? lease.getProperty().getPropertyName() : null;
                    String unit = lease.getUnit() != null ? lease.getUnit().getUnitNumber() : null;
                    return new PredictionDTO(
                            p.getPredictionId(),
                            lease.getLeaseId(),
                            tenant,
                            property,
                            unit,
                            p.getPredictionType().name(),
                            p.getRiskScore().doubleValue(),
                            p.getRiskLevel().name(),
                            p.getPredictedAt().toString()
                    );
                })
                .toList();
    }

    // Dashboard summary for the authenticated owner
    @GetMapping("/dashboard")
    public AiPredictionService.DashboardSummaryDTO getDashboardSummary(Authentication auth) {
        UUID ownerId = resolveUserId(auth);
        return aiPredictionService.getDashboardSummary(ownerId);
    }

    // Generate late payment prediction for a lease
    @PostMapping("/late-payment")
    public AiPrediction generateLatePaymentPrediction(Authentication auth,
                                                     @RequestParam UUID leaseId) {
        validateLeaseAccess(auth, leaseId);
        return aiPredictionService.generateLatePaymentPrediction(leaseId);
    }

    // Get latest prediction for a lease
    @GetMapping("/latest")
    public AiPrediction getLatestPrediction(Authentication auth,
                                           @RequestParam UUID leaseId,
                                           @RequestParam(defaultValue = "LATE_PAYMENT") PredictionType type) {
        validateLeaseAccess(auth, leaseId);
        return aiPredictionService.getLatest(leaseId, type);
    }

    // Get specific prediction by ID
    @GetMapping("/{predictionId}")
    public AiPrediction getPredictionById(@PathVariable UUID predictionId) {
        return aiPredictionService.getPredictionById(predictionId);
    }

    private UUID resolveUserId(Authentication auth) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        return UUID.fromString(ctx.userId());
    }

    private void validateLeaseAccess(Authentication auth, UUID leaseId) {
        aiPredictionService.validateLeaseAccess(resolveUserId(auth), leaseId);
    }
}
