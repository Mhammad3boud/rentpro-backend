package com.rentpro.backend.ai;

import com.rentpro.backend.lease.Lease;
import com.rentpro.backend.lease.LeaseRepository;
import com.rentpro.backend.lease.LeaseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PredictionScheduler {

    private static final Logger log = LoggerFactory.getLogger(PredictionScheduler.class);

    private final LeaseRepository leaseRepository;
    private final AiPredictionService predictionService;

    public PredictionScheduler(LeaseRepository leaseRepository, AiPredictionService predictionService) {
        this.leaseRepository = leaseRepository;
        this.predictionService = predictionService;
    }

    /** Runs every night at 02:00 server time. */
    @Scheduled(cron = "0 0 2 * * *")
    public void refreshAllPredictions() {
        List<Lease> activeLeases = leaseRepository.findByLeaseStatus(LeaseStatus.ACTIVE);
        log.info("Prediction scheduler: refreshing {} active leases", activeLeases.size());

        int success = 0;
        int failed = 0;

        for (Lease lease : activeLeases) {
            try {
                predictionService.generateLatePaymentPrediction(lease.getLeaseId());
                success++;
            } catch (Exception e) {
                log.warn("Failed to generate prediction for lease {}: {}", lease.getLeaseId(), e.getMessage());
                failed++;
            }
        }

        log.info("Prediction scheduler done: {} succeeded, {} failed", success, failed);
    }
}
