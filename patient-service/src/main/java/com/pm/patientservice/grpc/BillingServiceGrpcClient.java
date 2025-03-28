package com.pm.patientservice.grpc;

// Import generated gRPC protocol buffer classes

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;

// Import gRPC core classes for channel management
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

// Import logging dependencies
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Import Spring framework annotations
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * gRPC client for communicating with the Billing Service's gRPC server.
 * This client uses a blocking (synchronous) stub for simplicity.
 */
@Service // Marks this class as a Spring service bean
public class BillingServiceGrpcClient {

    // Logger instance for tracking client activity
    private static final Logger log = LoggerFactory.getLogger(
            BillingServiceGrpcClient.class);

    /**
     * The blocking stub used for making synchronous gRPC calls.
     * Blocking means the thread waits for the server response before continuing.
     */
    private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;

    /**
     * Constructor that initializes the gRPC client connection.
     *
     * @param serverAddress The address of the billing service gRPC server
     *        (configurable via property 'billing.service.address', defaults to 'localhost')
     * @param serverPort The port of the billing service gRPC server
     *        (configurable via property 'billing.service.grpc.port', defaults to 9001)
     */
    public BillingServiceGrpcClient(
            @Value("${billing.service.address:localhost}") String serverAddress,
            @Value("${billing.service.grpc.port:9001}") int serverPort) {

        // Log connection attempt for debugging purposes
        log.info("Connecting to Billing Service GRPC service at {}:{}",
                serverAddress, serverPort);

        // Create a communication channel to the gRPC server
        ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress, serverPort)
                .usePlaintext() // Disables TLS/SSL for local development (not for production!)
                .build(); // Finalizes channel creation

        // Initialize the blocking stub using the channel
        blockingStub = BillingServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Creates a billing account by making a gRPC call to the billing service.
     *
     * @param patientId The ID of the patient to create a billing account for
     * @param name The name of the patient
     * @param email The email of the patient
     * @return BillingResponse containing the created account details
     */
    public BillingResponse createBillingAccount(String patientId, String name,
                                                String email) {

        // Build the gRPC request using protocol buffer's builder pattern
        BillingRequest request = BillingRequest.newBuilder()
                .setPatientId(patientId)  // Set patient ID
                .setName(name)           // Set patient name
                .setEmail(email)         // Set patient email
                .build();                // Finalize request construction

        // Make the gRPC call using the blocking stub
        BillingResponse response = blockingStub.createBillingAccount(request);

        // Log the response for debugging
        log.info("Received response from billing service via GRPC: {}", response);

        // Return the response to the caller
        return response;
    }
}