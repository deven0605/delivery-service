package com.thalicloud.delivery.dto.request;

import lombok.Getter;
import lombok.Setter;

// FR-5.4 — "Arrived at Kitchen". Location is best-effort: the client gates
// button-enablement itself (200m geofence, or a manual delay-based fallback
// when a GPS fix isn't available/accurate), so these fields are optional here
// rather than required.
@Getter
@Setter
public class ArrivalRequest {

    private Double latitude;

    private Double longitude;
}
