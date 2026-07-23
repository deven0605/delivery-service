package com.thalicloud.delivery.dto.response;

import lombok.Builder;
import lombok.Getter;

// M13 — Help & Support (FR-13.3). Backs the SOS/"Call Support" shortcut.
@Getter
@Builder
public class SupportConfigResponse {
    private final String supportPhoneNumber;
    private final String supportEmail;
}
