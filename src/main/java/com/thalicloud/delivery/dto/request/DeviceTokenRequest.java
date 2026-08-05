package com.thalicloud.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// M12/FR-12 — registers/replaces this device's Expo push token (see
// registerForPushNotificationsAsync on the client and DeliveryPartnerController's
// PUT /device-token).
@Getter
@Setter
public class DeviceTokenRequest {

    @NotBlank(message = "expoPushToken is required")
    private String expoPushToken;
}
