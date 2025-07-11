// src/main/java/com/pm/billingservice/model/Bill.java
package com.pm.billingservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bills") // New table for bills
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID billingAccountId; // Link to the patient's billing account

    @Column(unique = true, nullable = false) // An appointment should ideally only generate one bill
    private UUID appointmentId;    // Link to the specific appointment

    @Column(nullable = false)
    private UUID patientId;        // Denormalized for easier querying

    @Column(nullable = false)
    private UUID doctorId;         // Denormalized for easier querying

    @Column(nullable = false)
    private LocalDateTime issueDate; // When the bill was generated

    @Column(nullable = false)
    private LocalDateTime dueDate;   // When payment is expected (e.g., issueDate + 15 days)

    @Column(nullable = false)
    private double totalAmount;      // The amount to be paid

    @Column(nullable = false)
    private String currency;         // e.g., "INR", "USD"

    @Column(nullable = false)
    private String status;           // e.g., "PENDING", "PAID", "OVERDUE", "REFUNDED"

    @Column(nullable = true) // When the payment was received
    private LocalDateTime paidAt;

    // You might add details like service items here if you have a more granular billing system
    // @ElementCollection
    // @CollectionTable(name = "bill_line_items", joinColumns = @JoinColumn(name = "bill_id"))
    // private List<BillLineItem> lineItems;
}