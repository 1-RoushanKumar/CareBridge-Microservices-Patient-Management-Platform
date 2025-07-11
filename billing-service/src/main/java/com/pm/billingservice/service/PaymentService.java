// src/main/java/com/pm/billingservice/service/PaymentService.java
package com.pm.billingservice.service;

import billing.events.PaymentReceivedEvent;
import com.pm.billingservice.kafka.BillingEventProducer;
import com.pm.billingservice.model.Bill;
import com.pm.billingservice.model.PaymentTransaction;
import com.pm.billingservice.repository.BillRepository;
import com.pm.billingservice.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final BillRepository billRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final BillingEventProducer billingEventProducer;

    public PaymentService(BillRepository billRepository,
                          PaymentTransactionRepository paymentTransactionRepository,
                          BillingEventProducer billingEventProducer) {
        this.billRepository = billRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.billingEventProducer = billingEventProducer;
    }

    @Transactional
    public PaymentTransaction processPayment(UUID billId, String paymentMethod) {
        // In a real scenario, this method would interact with a payment gateway.
        // For now, we simulate a successful payment.

        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found with ID: " + billId)); // Use custom exception

        if ("PAID".equalsIgnoreCase(bill.getStatus())) {
            log.warn("Bill {} is already paid. Skipping payment processing.", billId);
            // Optionally, return an existing successful transaction or throw a specific exception
            throw new IllegalArgumentException("Bill " + billId + " is already paid.");
        }
        if (bill.getTotalAmount() <= 0) {
            log.warn("Attempted to pay a bill with zero or negative amount: {}. Bill ID: {}", bill.getTotalAmount(), billId);
            throw new IllegalArgumentException("Cannot process payment for a bill with zero or negative amount.");
        }

        // 1. Create a Payment Transaction Record
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setBillId(bill.getId());
        transaction.setBillingAccountId(bill.getBillingAccountId());
        transaction.setAmount(bill.getTotalAmount()); // Assume full payment for simplicity
        transaction.setCurrency(bill.getCurrency());
        transaction.setPaymentMethod(paymentMethod);
        transaction.setTransactionStatus("SUCCESS"); // Simulate success
        // In real integration, gatewayTransactionId would come from the gateway
        transaction.setGatewayTransactionId("GATEWAY_TXN_" + UUID.randomUUID().toString().substring(0, 8));
        // transaction.setTransactionDate is handled by @PrePersist

        PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);
        log.info("Recorded payment transaction ID {} for bill ID {}", savedTransaction.getId(), billId);

        // 2. Update Bill Status
        bill.setStatus("PAID");
        bill.setPaidAt(LocalDateTime.now());
        billRepository.save(bill);
        log.info("Updated bill ID {} status to PAID.", billId);

        // 3. Publish PaymentReceivedEvent
        PaymentReceivedEvent event = PaymentReceivedEvent.newBuilder()
                .setTransactionId(savedTransaction.getId().toString())
                .setBillId(savedTransaction.getBillId().toString())
                .setBillingAccountId(savedTransaction.getBillingAccountId().toString())
                .setPatientId(bill.getPatientId().toString()) // Get patientId from the bill
                .setAmount(savedTransaction.getAmount())
                .setCurrency(savedTransaction.getCurrency())
                .setPaymentMethod(savedTransaction.getPaymentMethod())
                .setEventType("PAYMENT_RECEIVED")
                .setTimestamp(LocalDateTime.now().toString())
                .build();
        billingEventProducer.sendPaymentReceivedEvent(event);

        return savedTransaction;
    }

    // Method to get transactions for a bill
    @Transactional(readOnly = true)
    public List<PaymentTransaction> getTransactionsForBill(UUID billId) {
        return paymentTransactionRepository.findByBillId(billId);
    }
}