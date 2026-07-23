package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.request.DispatchOrderRequest;
import com.thalicloud.delivery.dto.response.ApiResponse;
import com.thalicloud.delivery.dto.response.DispatchOrderResponse;
import com.thalicloud.delivery.service.DeliveryAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Order-service-facing seam: called by order-service's DeliveryDispatchClient
// when a vendor accepts an order. Same X-Internal-Key/permitAll pattern as
// InternalAssignmentController (kept as a separate controller since this
// isn't nested under a specific assignment id).
@RestController
@RequestMapping("/api/delivery/internal/dispatch")
@RequiredArgsConstructor
public class OrderDispatchController {

    private final DeliveryAssignmentService assignmentService;

    @Value("${internal.dispatch-key}")
    private String dispatchKey;

    @PostMapping
    public ResponseEntity<ApiResponse<DispatchOrderResponse>> dispatchOrder(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @Valid @RequestBody DispatchOrderRequest request) {
        if (key == null || !key.equals(dispatchKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid or missing X-Internal-Key"));
        }

        DispatchOrderResponse response = assignmentService.dispatchOrder(request);
        return ResponseEntity.ok(ApiResponse.success(response.message(), response));
    }
}
