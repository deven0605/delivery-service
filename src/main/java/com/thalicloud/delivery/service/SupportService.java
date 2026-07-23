package com.thalicloud.delivery.service;

import com.thalicloud.delivery.dto.response.FaqResponse;
import com.thalicloud.delivery.dto.response.SupportConfigResponse;
import com.thalicloud.delivery.dto.response.SupportIssueResponse;
import com.thalicloud.delivery.enums.IssueCategory;

import java.util.List;
import java.util.UUID;

public interface SupportService {

    // FR-13.1
    List<FaqResponse> getFaqs();

    // FR-13.3
    SupportConfigResponse getSupportConfig();

    // FR-13.2 — assignmentId is optional; if present, must belong to the caller.
    SupportIssueResponse reportIssue(UUID partnerId, IssueCategory category, String description, UUID assignmentId);

    List<SupportIssueResponse> getMyIssues(UUID partnerId);
}
