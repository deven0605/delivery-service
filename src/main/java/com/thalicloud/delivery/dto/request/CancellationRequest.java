package com.thalicloud.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// FR-4.8 — partner-raised cancellation (e.g. vehicle breakdown).
@Getter
@Setter
public class CancellationRequest {

    @NotBlank(message = "A reason is required")
    private String reason;
}
