package com.thalicloud.delivery.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// FR-2.1 — bound from a JSON body (personal details fields + the required
// selfie, base64-encoded) via @RequestBody.
@Getter
@Setter
public class PersonalDetailsRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dob;

    private String gender; // optional (FR-2.1)

    @Email(message = "Enter a valid email address")
    private String email; // optional (FR-2.1)

    // Required — validated manually in the service (bean validation can't
    // reliably assert "non-null nested object" the way we want the message worded).
    @Valid
    private Base64FileRequest profilePhoto;
}
