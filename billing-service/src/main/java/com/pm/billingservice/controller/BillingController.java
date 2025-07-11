// src/main/java/com/pm/billingservice/controller/BillingController.java
package com.pm.billingservice.controller;

import com.pm.billingservice.model.Bill;
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.model.PaymentTransaction;
import com.pm.billingservice.service.BillService;
import com.pm.billingservice.service.BillingAccountService;
import com.pm.billingservice.service.PaymentService; // NEW Import
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Map; // For request body

@RestController
@RequestMapping("/billing")
public class BillingController {

    private final BillingAccountService billingAccountService;
    private final BillService billService;
    private final PaymentService paymentService; // NEW: Inject PaymentService

    public BillingController(BillingAccountService billingAccountService,
                             BillService billService,
                             PaymentService paymentService) { // NEW: Add to constructor
        this.billingAccountService = billingAccountService;
        this.billService = billService;
        this.paymentService = paymentService; // NEW: Assign
    }

    // Existing endpoints for billing accounts and getting bills...
    // Example:
    @GetMapping("/accounts/patient/{patientId}")
    public ResponseEntity<BillingAccount> getBillingAccountByPatientId(@PathVariable UUID patientId) {
        return billingAccountService.getBillingAccountByPatientId(patientId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/bills/{billId}")
    public ResponseEntity<Bill> getBillById(@PathVariable UUID billId) {
        return billService.getBillById(billId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // NEW ENDPOINT: Pay a Bill
    @PostMapping("/bills/{billId}/pay")
    public ResponseEntity<PaymentTransaction> payBill(@PathVariable UUID billId,
                                                      @RequestBody Map<String, String> paymentDetails) {
        String paymentMethod = paymentDetails.get("paymentMethod");
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            return ResponseEntity.badRequest().body(null); // Or custom error response
        }
        try {
            PaymentTransaction transaction = paymentService.processPayment(billId, paymentMethod);
            return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
        } catch (RuntimeException e) { // Catch specific exceptions from service
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Or detailed error
        }
    }

    // NEW ENDPOINT: Get payment transactions for a bill
    @GetMapping("/bills/{billId}/payments")
    public ResponseEntity<List<PaymentTransaction>> getBillPayments(@PathVariable UUID billId) {
        List<PaymentTransaction> transactions = paymentService.getTransactionsForBill(billId);
        return ResponseEntity.ok(transactions);
    }
}