package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.request.DispatchOrderRequest;
import com.thalicloud.delivery.dto.response.ApiResponse;
import com.thalicloud.delivery.dto.response.DispatchOrderResponse;
import com.thalicloud.delivery.service.DeliveryAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/api/delivery/internal/dispatch")
@RequiredArgsConstructor
@Tag(name = "Internal - Order Dispatch", description = "Internal service-to-service endpoint (not for external/UI clients) called by order-service when a vendor accepts an order, to find and assign the nearest available delivery partner. Guarded by a shared X-Internal-Key header rather than partner JWT auth.")
public class OrderDispatchController {

    private final DeliveryAssignmentService assignmentService;

    @Value("${internal.dispatch-key}")
    private String dispatchKey;

    @Operation(summary = "Dispatch an order to a delivery partner", description = "Internal endpoint invoked by order-service to find an available delivery partner and create an assignment offer for the given order. Requires a valid X-Internal-Key header.")
    @PostMapping
    public ResponseEntity<ApiResponse<DispatchOrderResponse>> dispatchOrder(
            @RequestHeader(value = "X-Internal-Key", required = false) String key,
            @Valid @RequestBody DispatchOrderRequest request) {
        log.info("dispatchOrder: start, orderId={}", request.getOrderId());
        try {
            if (key == null || !key.equals(dispatchKey)) {
                ResponseEntity<ApiResponse<DispatchOrderResponse>> denied = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid or missing X-Internal-Key"));
                log.info("dispatchOrder: end, orderId={}, denied=true", request.getOrderId());
                return denied;
            }

            DispatchOrderResponse response = assignmentService.dispatchOrder(request);
            ResponseEntity<ApiResponse<DispatchOrderResponse>> result = ResponseEntity.ok(ApiResponse.success(response.message(), response));
            log.info("dispatchOrder: end, orderId={}", request.getOrderId());
            return result;
        } catch (Exception e) {
            log.error("dispatchOrder: failed, orderId={}", request.getOrderId(), e);
            throw e;
        }
    }
}
