package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.request.LocationUpdateRequest;
import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.service.DeliveryPartnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

// FR-3.3 — STOMP destination /app/partner/{partnerId}/location. The
// {partnerId} path segment is only used to shape the URL the way the spec
// asks for; the partner actually acted on is always the STOMP session's own
// authenticated principal, so one partner can never publish location under
// another partner's id.
@Controller
@RequiredArgsConstructor
@Slf4j
public class LocationController {

    private final DeliveryPartnerService deliveryPartnerService;

    @MessageMapping("/partner/{partnerId}/location")
    public void publishLocation(
            @DestinationVariable String partnerId,
            @Payload LocationUpdateRequest request,
            Principal principal) {
        if (!(principal instanceof Authentication auth) || !(auth.getPrincipal() instanceof DeliveryPartner authenticatedPartner)) {
            log.warn("Rejected location update for {} — unauthenticated STOMP session", partnerId);
            return;
        }

        UUID authenticatedPartnerId = authenticatedPartner.getId();
        if (!authenticatedPartnerId.toString().equals(partnerId)) {
            log.warn("Rejected location update — session partner {} tried to publish as {}", authenticatedPartnerId, partnerId);
            return;
        }
        if (request.getLatitude() == null || request.getLongitude() == null) {
            return;
        }

        deliveryPartnerService.updateLocation(authenticatedPartnerId, request.getLatitude(), request.getLongitude());
    }
}
