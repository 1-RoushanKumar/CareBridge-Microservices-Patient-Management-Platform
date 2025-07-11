// src/main/java/com/pm/billingservice/repository/PaymentTransactionRepository.java
package com.pm.billingservice.repository;

import com.pm.billingservice.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    List<PaymentTransaction> findByBillId(UUID billId);
    List<PaymentTransaction> findByBillingAccountId(UUID billingAccountId);
    // You might add more queries like findByTransactionStatus, etc.
}