package com.thalicloud.delivery.dto.request;

import com.thalicloud.delivery.enums.IssueCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// M13 — Help & Support (FR-13.2). "Report an Issue" — assignmentId is
// optional (the form works with or without a specific delivery attached).
@Getter
@Setter
public class CreateSupportIssueRequest {

    @NotNull(message = "category is required")
    private IssueCategory category;

    @NotBlank(message = "description is required")
    @Size(max = 1000, message = "description must be at most 1000 characters")
    private String description;

    private UUID assignmentId;
}
