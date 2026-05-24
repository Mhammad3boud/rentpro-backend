package com.rentpro.backend.lease;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class LeaseScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LeaseScheduler.class);

    private final LeaseRepository leaseRepository;

    public LeaseScheduler(LeaseRepository leaseRepository) {
        this.leaseRepository = leaseRepository;
    }

    @PostConstruct
    public void onStartup() {
        expireOverdueLeases();
    }

    // Run every day at 00:30 AM
    @Scheduled(cron = "0 30 0 * * ?")
    @Transactional
    public void expireOverdueLeases() {
        LocalDate today = LocalDate.now();
        List<Lease> expired = leaseRepository.findActiveExpiredLeases(today);
        if (expired.isEmpty()) return;

        for (Lease lease : expired) {
            lease.setLeaseStatus(LeaseStatus.EXPIRED);
        }
        leaseRepository.saveAll(expired);
        logger.info("Expired {} lease(s) with end date before {}", expired.size(), today);
    }
}
