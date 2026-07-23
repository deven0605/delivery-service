package com.thalicloud.delivery.dto.response;

import lombok.Builder;
import lombok.Getter;

// M13 — Help & Support (FR-13.1). Static content, not persisted — see
// SupportServiceImpl#FAQS.
@Getter
@Builder
public class FaqResponse {
    private final String id;
    private final String category;
    private final String question;
    private final String answer;
}
