package com.thalicloud.delivery.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// FR-6.5 — proof-of-delivery photo for a Contactless Drop, transported the
// same base64-in-JSON way as KYC documents (see Base64FileRequest).
@Getter
@Setter
public class DropPhotoUploadRequest {

    @NotNull(message = "File is required")
    @Valid
    private Base64FileRequest file;
}
