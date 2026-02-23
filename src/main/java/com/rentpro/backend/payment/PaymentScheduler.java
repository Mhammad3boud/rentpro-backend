package com.rentpro.backend.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PaymentScheduler.class);

    private final RentPaymentService rentPaymentService;

    public PaymentScheduler(RentPaymentService rentPaymentService) {
        this.rentPaymentService = rentPaymentService;
    }

    // Run every day at 1:00 AM
    @Scheduled(cron = "0 0 1 * * ?")
    public void updateOverduePayments() {
        logger.info("Running scheduled task: updating overdue payments");
        rentPaymentService.updateOverduePayments();
        logger.info("Scheduled task completed: overdue payments updated");
    }
}
