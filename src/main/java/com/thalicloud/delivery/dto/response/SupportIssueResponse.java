package com.thalicloud.delivery.dto.response;

import com.thalicloud.delivery.entity.SupportIssue;
import com.thalicloud.delivery.enums.IssueCategory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

// M13 — Help & Support (FR-13.2).
@Getter
@Builder
public class SupportIssueResponse {
    private final UUID id;
    private final IssueCategory category;
    private final String description;
    private final UUID assignmentId;
    private final LocalDateTime createdAt;

    public static SupportIssueResponse from(SupportIssue issue) {
        return SupportIssueResponse.builder()
                .id(issue.getId())
                .category(issue.getCategory())
                .description(issue.getDescription())
                .assignmentId(issue.getAssignmentId())
                .createdAt(issue.getCreatedAt())
                .build();
    }
}
