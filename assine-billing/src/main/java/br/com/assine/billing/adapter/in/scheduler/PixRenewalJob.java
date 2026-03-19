package br.com.assine.billing.adapter.in.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PixRenewalJob {

    private static final Logger log = LoggerFactory.getLogger(PixRenewalJob.class);

    // This would typically have dependencies to PaymentRepository and PaymentGateway
    // to find PIX subscriptions due for renewal and generate a new PaymentIntent.

    @Scheduled(cron = "0 0 2 * * *") // Runs every day at 2 AM
    public void processPixRenewals() {
        log.info("Starting Pix Renewal Job...");
        
        // 1. Fetch active subscriptions with payment_method = PIX that are due today
        // List<Subscription> dueSubscriptions = subscriptionClient.getDuePixSubscriptions();
        
        // 2. For each, call StripeGateway to generate new PaymentIntent (QR Code)
        // 3. Publish event PixPaymentGenerated(qrCode, subscriptionId)
        // 4. assine-notifications consumes and sends email
        
        log.info("Completed Pix Renewal Job.");
    }
}
