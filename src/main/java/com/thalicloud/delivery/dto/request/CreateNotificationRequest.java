package com.thalicloud.delivery.dto.request;

import com.thalicloud.delivery.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// M12 — Notifications. Internal/system-facing (see InternalNotificationController)
// — the seam a future auth-service KYC-approval flow or Ops announcement tool
// would call. Same "no such caller exists yet" gap as CreateAssignmentRequest.
@Getter
@Setter
public class CreateNotificationRequest {

    @NotNull(message = "partnerId is required")
    private UUID partnerId;

    @NotNull(message = "type is required")
    private NotificationType type;

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "body is required")
    private String body;

    private String referenceId;
}
