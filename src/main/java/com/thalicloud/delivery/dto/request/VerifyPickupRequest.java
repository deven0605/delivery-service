package com.thalicloud.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

// FR-5.5 — the code is either typed in manually or extracted from a scanned
// QR code client-side; either way it arrives here as the same 4-digit string.
@Getter
@Setter
public class VerifyPickupRequest {

    @NotBlank(message = "code is required")
    @Pattern(regexp = "^\\d{4}$", message = "code must be exactly 4 digits")
    private String code;
}
