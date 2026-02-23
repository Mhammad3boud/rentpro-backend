package com.rentpro.backend.ai;

import com.rentpro.backend.security.JwtUserContext;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/predictions")
public class AiPredictionController {

    private final AiPredictionService aiPredictionService;

    public AiPredictionController(AiPredictionService aiPredictionService) {
        this.aiPredictionService = aiPredictionService;
    }

    // Generate late payment prediction for a lease
    @PostMapping("/late-payment")
    public AiPrediction generateLatePaymentPrediction(Authentication auth,
                                                     @RequestParam UUID leaseId) {
        // Verify user has access to this lease
        validateLeaseAccess(auth, leaseId);
        return aiPredictionService.generateLatePaymentPrediction(leaseId);
    }

    // Get latest prediction for a lease
    @GetMapping("/latest")
    public AiPrediction getLatestPrediction(Authentication auth,
                                           @RequestParam UUID leaseId,
                                           @RequestParam(defaultValue = "LATE_PAYMENT") PredictionType type) {
        // Verify user has access to this lease
        validateLeaseAccess(auth, leaseId);
        return aiPredictionService.getLatest(leaseId, type);
    }

    // Get specific prediction by ID
    @GetMapping("/{predictionId}")
    public AiPrediction getPredictionById(@PathVariable UUID predictionId) {
        return aiPredictionService.getPredictionById(predictionId);
    }

    private void validateLeaseAccess(Authentication auth, UUID leaseId) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        UUID userId = UUID.fromString(ctx.userId());
        // This would need to be implemented in the service layer
        // to check if the user has access to the specified lease
        aiPredictionService.validateLeaseAccess(userId, leaseId);
    }
}
