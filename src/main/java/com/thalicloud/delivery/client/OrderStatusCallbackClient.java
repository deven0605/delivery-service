package com.thalicloud.delivery.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Notifies order-service that an order's delivery status advanced, driven by
 * a partner's pickup/delivery OTP match (see DeliveryAssignmentServiceImpl's
 * verifyPickup/verifyDelivery). Best-effort — a failure here never rolls back
 * the assignment's own state; the partner-facing verification has already
 * succeeded by the time this is called.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusCallbackClient {

    private final RestTemplate restTemplate;

    @Value("${services.order-service.base-url}")
    private String orderServiceBaseUrl;

    @Value("${internal.dispatch-key}")
    private String dispatchKey;

    public void updateOrderStatus(String orderId, String status) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Key", dispatchKey);
            headers.set("Content-Type", "application/json");

            restTemplate.exchange(
                    orderServiceBaseUrl + "/api/orders/internal/" + orderId + "/status",
                    org.springframework.http.HttpMethod.PATCH,
                    new HttpEntity<>(new StatusBody(status), headers),
                    Void.class);
        } catch (Exception e) {
            log.warn("Order status callback failed for order {} -> {}: {}", orderId, status, e.getMessage());
        }
    }

    private record StatusBody(String status) {}
}
