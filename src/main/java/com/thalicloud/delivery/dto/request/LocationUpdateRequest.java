package com.thalicloud.delivery.dto.request;

import lombok.Getter;
import lombok.Setter;

// FR-3.3 — STOMP payload published to /app/partner/{partnerId}/location.
@Getter
@Setter
public class LocationUpdateRequest {
    private Double latitude;
    private Double longitude;
}
