package com.thalicloud.delivery.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Sends a single push through Expo's push service — the real-OS-push half of
 * FR-12, feeding off the token the app registers via
 * PUT /api/delivery/partners/me/device-token (see registerForPushNotificationsAsync
 * on the client). Best-effort, same spirit as OrderStatusCallbackClient: a
 * failure here never affects the in-app notification tray, which
 * NotificationServiceImpl has already persisted before calling this.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpoPushClient {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    // Real tokens look like "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]" — guards
    // against wasting a call on Expo Go/dev-build placeholders that were never
    // sent to registerForPushNotificationsAsync's getExpoPushTokenAsync.
    private static final String VALID_TOKEN_PREFIX = "ExponentPushToken";

    private final RestTemplate restTemplate;

    /**
     * @return false only when Expo explicitly reports the token as no longer
     * registered (app uninstalled, token rotated) — the caller should clear it
     * from the partner record. Any other outcome (success, transient/network
     * failure, malformed token) returns true so a stored token is never
     * dropped on a guess.
     */
    public boolean send(String expoPushToken, String title, String body, Map<String, String> data) {
        if (expoPushToken == null || !expoPushToken.startsWith(VALID_TOKEN_PREFIX)) {
            return true;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");

            Map<String, Object> payload = Map.of(
                    "to", expoPushToken,
                    "title", title,
                    "body", body,
                    "data", data,
                    "priority", "high",
                    "channelId", "default");

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    EXPO_PUSH_URL, new HttpEntity<>(payload, headers), Map.class);

            Object ticket = response.getBody() != null ? response.getBody().get("data") : null;
            if (ticket instanceof Map<?, ?> ticketMap && "error".equals(ticketMap.get("status"))) {
                Object details = ticketMap.get("details");
                String errorCode = details instanceof Map<?, ?> d ? String.valueOf(d.get("error")) : null;
                log.warn("Expo push rejected ({}): {}", errorCode, ticketMap.get("message"));
                return !"DeviceNotRegistered".equals(errorCode);
            }
            return true;
        } catch (Exception e) {
            log.warn("Expo push send failed: {}", e.getMessage());
            return true;
        }
    }
}
