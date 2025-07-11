// src/main/java/com/pm/billingservice/model/PaymentTransaction.java
package com.pm.billingservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID billId; // Links to the Bill that was paid

    @Column(nullable = false)
    private UUID billingAccountId; // Denormalized for easier querying

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private String currency; // e.g., "INR", "USD"

    @Column(nullable = false)
    private String paymentMethod; // e.g., "CREDIT_CARD", "DEBIT_CARD", "UPI", "NET_BANKING", "CASH"

    @Column(nullable = false)
    private String transactionStatus; // e.g., "SUCCESS", "FAILED", "PENDING", "REFUNDED"

    @Column(nullable = true) // This would come from the payment gateway
    private String gatewayTransactionId;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    // Optional: Reference to the gateway-specific response if storing
    // @Column(columnDefinition = "TEXT")
    // private String gatewayResponse;

    @PrePersist
    protected void onCreate() {
        this.transactionDate = LocalDateTime.now();
    }
}