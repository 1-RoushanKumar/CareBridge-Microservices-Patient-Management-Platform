package com.pm.billingservice.service;

import com.pm.billingservice.kafka.BillingEventProducer; // Import new producer
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.repository.BillingAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import billing.events.BillingAccountEvent; // Import the new Protobuf event

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class BillingAccountService {

    private static final Logger log = LoggerFactory.getLogger(BillingAccountService.class);
    private final BillingAccountRepository billingAccountRepository;
    private final BillingEventProducer billingEventProducer; // Inject the new producer

    public BillingAccountService(BillingAccountRepository billingAccountRepository,
                                 BillingEventProducer billingEventProducer) { // Add to constructor
        this.billingAccountRepository = billingAccountRepository;
        this.billingEventProducer = billingEventProducer;
    }

    @Transactional
    public BillingAccount createBillingAccount(String patientId, String name, String email) {
        UUID patientUuid = UUID.fromString(patientId);

        Optional<BillingAccount> existingAccount = billingAccountRepository.findByPatientId(patientUuid);
        if (existingAccount.isPresent()) {
            log.warn("Billing account already exists for patientId: {}. Returning existing account.", patientId);
            // Optionally, send an "Account Existed" event or update event if status changed
            return existingAccount.get();
        }

        BillingAccount account = new BillingAccount();
        account.setPatientId(patientUuid);
        account.setName(name);
        account.setEmail(email);
        account.setStatus("ACTIVE"); // Default initial status
        account.setCreatedAt(LocalDateTime.now());

        BillingAccount savedAccount = billingAccountRepository.save(account);
        log.info("Successfully created billing account ID {} for patientId {}", savedAccount.getId(), savedAccount.getPatientId());

        // *** PUBLISH THE BILLING ACCOUNT CREATED EVENT ***
        BillingAccountEvent event = BillingAccountEvent.newBuilder()
                .setAccountId(savedAccount.getId().toString())
                .setPatientId(savedAccount.getPatientId().toString())
                .setName(savedAccount.getName())
                .setEmail(savedAccount.getEmail())
                .setStatus(savedAccount.getStatus())
                .setEventType("BILLING_ACCOUNT_CREATED") // Specific event type
                .setTimestamp(LocalDateTime.now().toString()) // Or use a proper date formatter
                .build();
        billingEventProducer.sendBillingAccountEvent(event);

        return savedAccount;
    }

    // You might add an update method here that also publishes an event:
    @Transactional
    public BillingAccount updateBillingAccountStatus(UUID accountId, String newStatus) {
        BillingAccount account = billingAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Billing account not found: " + accountId)); // Use custom exception

        account.setStatus(newStatus);
        BillingAccount updatedAccount = billingAccountRepository.save(account);
        log.info("Updated billing account ID {} status to {}", updatedAccount.getId(), updatedAccount.getStatus());

        BillingAccountEvent event = BillingAccountEvent.newBuilder()
                .setAccountId(updatedAccount.getId().toString())
                .setPatientId(updatedAccount.getPatientId().toString())
                .setName(updatedAccount.getName())
                .setEmail(updatedAccount.getEmail())
                .setStatus(updatedAccount.getStatus())
                .setEventType("BILLING_ACCOUNT_UPDATED") // Specific event type
                .setTimestamp(LocalDateTime.now().toString())
                .build();
        billingEventProducer.sendBillingAccountEvent(event);

        return updatedAccount;
    }

    public Optional<BillingAccount> getBillingAccountByPatientId(UUID patientId) {
        return billingAccountRepository.findByPatientId(patientId);
    }
}