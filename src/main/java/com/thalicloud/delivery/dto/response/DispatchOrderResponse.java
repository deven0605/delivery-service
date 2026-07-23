package com.thalicloud.delivery.dto.response;

import java.util.UUID;

public record DispatchOrderResponse(
        boolean assigned,
        UUID assignmentId,
        UUID partnerId,
        String message
) {}
