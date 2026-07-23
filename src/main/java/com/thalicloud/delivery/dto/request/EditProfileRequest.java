package com.thalicloud.delivery.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

// M11 — Profile & Documents (FR-11.3). Unlike PersonalDetailsRequest (the
// onboarding wizard step — full name/dob required, selfie mandatory), this
// edits only the two non-KYC fields a partner may change any time post-
// registration: both optional, and only the fields actually sent are updated.
@Getter
@Setter
public class EditProfileRequest {

    @Email(message = "Enter a valid email address")
    private String email;

    @Valid
    private Base64FileRequest profilePhoto;
}
