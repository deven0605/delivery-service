package com.thalicloud.delivery.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

// FR-6.4/FR-6.5 — otp is required unless contactless is true, in which case a
// proof-of-delivery photo (already uploaded via DropPhotoUploadRequest) is
// required instead; that either/or isn't expressible declaratively here, so
// the service layer enforces it.
@Getter
@Setter
public class VerifyDeliveryRequest {

    @Pattern(regexp = "^\\d{4}$", message = "otp must be exactly 4 digits")
    private String otp;

    private boolean contactless;
}
