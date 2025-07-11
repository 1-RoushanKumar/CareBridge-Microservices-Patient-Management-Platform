// src/main/java/com/pm/billingservice/service/BillService.java
package com.pm.billingservice.service;

import com.pm.billingservice.model.Bill;
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.repository.BillRepository;
import com.pm.billingservice.repository.BillingAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class BillService {

    private static final Logger log = LoggerFactory.getLogger(BillService.class);
    private final BillRepository billRepository;
    private final BillingAccountRepository billingAccountRepository; // To get patient's billing account

    public BillService(BillRepository billRepository, BillingAccountRepository billingAccountRepository) {
        this.billRepository = billRepository;
        this.billingAccountRepository = billingAccountRepository;
    }

    @Transactional
    public Bill generateBillForAppointment(
            UUID appointmentId, UUID patientId, UUID doctorId,
            LocalDateTime completionDateTime, double baseFeeAmount, String currency) {

        // Check if a bill already exists for this appointment to ensure idempotency
        Optional<Bill> existingBill = billRepository.findByAppointmentId(appointmentId);
        if (existingBill.isPresent()) {
            log.warn("Bill already exists for appointmentId: {}. Skipping generation.", appointmentId);
            return existingBill.get();
        }

        // Retrieve the BillingAccount for the patient
        BillingAccount billingAccount = billingAccountRepository.findByPatientId(patientId)
                .orElseThrow(() -> {
                    log.error("Billing account not found for patientId: {}. Cannot generate bill for appointment: {}", patientId, appointmentId);
                    return new RuntimeException("Billing account not found for patient: " + patientId);
                });

        Bill bill = new Bill();
        bill.setBillingAccountId(billingAccount.getId());
        bill.setAppointmentId(appointmentId);
        bill.setPatientId(patientId);
        bill.setDoctorId(doctorId);
        bill.setIssueDate(LocalDateTime.now());
        bill.setDueDate(LocalDateTime.now().plusDays(15)); // Example: due in 15 days
        bill.setTotalAmount(baseFeeAmount); // Use the base fee from the event
        bill.setCurrency(currency);
        bill.setStatus("PENDING"); // Initial status for a new bill

        Bill savedBill = billRepository.save(bill);
        log.info("Generated new bill ID {} for appointment ID {} (Patient ID {})",
                savedBill.getId(), savedBill.getAppointmentId(), savedBill.getPatientId());

        // Optional: Publish a BillGeneratedEvent if other services need to know about new bills
        // e.g., Notification Service to send a bill notification to the patient.
        // You would need a new Protobuf message for this too, and a producer.
        // billingEventProducer.sendBillGeneratedEvent(...);

        return savedBill;
    }

    // You can add methods for getting bills, updating status (e.g., mark as paid) etc.
    public Optional<Bill> getBillById(UUID billId) {
        return billRepository.findById(billId);
    }
}