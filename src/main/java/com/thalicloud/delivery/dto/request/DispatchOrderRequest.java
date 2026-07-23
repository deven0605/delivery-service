package com.thalicloud.delivery.dto.request;

import com.thalicloud.delivery.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

// Called by order-service's DeliveryDispatchClient when a vendor accepts an
// order. Unlike CreateAssignmentRequest, no exact partnerId is supplied —
// order-service doesn't own delivery-partner data, so DispatchOrderServiceImpl
// picks the first available ONLINE partner itself (no real geo-matching engine
// exists yet anywhere in this workspace; see its own docs).
@Getter
@Setter
public class DispatchOrderRequest {

    @NotBlank(message = "orderId is required")
    private String orderId;

    @NotBlank(message = "kitchenName is required")
    private String kitchenName;

    @NotBlank(message = "kitchenContactNumber is required")
    private String kitchenContactNumber;

    @NotNull(message = "dropLatitude is required")
    private Double dropLatitude;

    @NotNull(message = "dropLongitude is required")
    private Double dropLongitude;

    @NotBlank(message = "dropAddress is required")
    private String dropAddress;

    @NotBlank(message = "customerName is required")
    private String customerName;

    @NotBlank(message = "customerContactNumber is required")
    private String customerContactNumber;

    @Positive(message = "itemCount must be > 0")
    private int itemCount;

    @NotNull(message = "paymentMethod is required")
    private PaymentMethod paymentMethod;

    private Long codAmountPaise;

    @PositiveOrZero(message = "deliveryChargePaise must be >= 0")
    private long deliveryChargePaise;
}
