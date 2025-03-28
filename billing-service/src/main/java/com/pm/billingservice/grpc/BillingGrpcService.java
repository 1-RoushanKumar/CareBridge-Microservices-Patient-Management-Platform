package com.pm.billingservice.grpc;

// Import generated gRPC and protobuf classes

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc.BillingServiceImplBase;

// Import gRPC and logging dependencies
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC service implementation for handling billing-related RPCs.
 * Annotated with @GrpcService to register it with Spring's gRPC server.
 */
@GrpcService // Registers this class as a gRPC service with Spring Boot
public class BillingGrpcService extends BillingServiceImplBase {

    // Logger for tracking service activity
    private static final Logger log = LoggerFactory.getLogger(BillingGrpcService.class);

    /**
     * Implements the createBillingAccount RPC method defined in the protobuf file.
     *
     * @param billingRequest   The incoming gRPC request (auto-deserialized from protobuf).
     * @param responseObserver Stream observer to send the response(s) back to the client.
     */
    @Override
    public void createBillingAccount(BillingRequest billingRequest,
                                     StreamObserver<BillingResponse> responseObserver) {

        // Log the incoming request for debugging
        log.info("createBillingAccount request received: {}", billingRequest.toString());

        // =============================================
        // BUSINESS LOGIC WOULD GO HERE (e.g., database operations)
        // Example:
        // 1. Validate the request
        // 2. Save to database
        // 3. Perform calculations
        // =============================================

        // Build a mock response (in a real app, this would come from business logic)
        BillingResponse response = BillingResponse.newBuilder()
                .setAccountId("12345") // Mock account ID
                .setStatus("Active")   // Mock status
                .build();

        // Send the response back to the client
        responseObserver.onNext(response);

        // Notify the client that the RPC is complete
        responseObserver.onCompleted();
    }
}