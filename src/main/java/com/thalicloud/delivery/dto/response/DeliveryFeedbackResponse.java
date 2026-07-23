package com.thalicloud.delivery.dto.response;

import com.thalicloud.delivery.entity.DeliveryAssignment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

// M10 — Ratings & Feedback (FR-10.2). Recent customer feedback tied to an
// individual delivery, read-only.
@Getter
@Builder
public class DeliveryFeedbackResponse {
    private final UUID assignmentId;
    private final String orderId;
    private final String kitchenName;
    private final String dropLocality;
    private final int rating;
    private final String feedback;
    private final LocalDateTime ratedAt;

    public static DeliveryFeedbackResponse from(DeliveryAssignment a) {
        return DeliveryFeedbackResponse.builder()
                .assignmentId(a.getId())
                .orderId(a.getOrderId())
                .kitchenName(a.getKitchenName())
                .dropLocality(a.getDropLocality())
                .rating(a.getCustomerRating())
                .feedback(a.getCustomerFeedback())
                .ratedAt(a.getRatedAt())
                .build();
    }
}
