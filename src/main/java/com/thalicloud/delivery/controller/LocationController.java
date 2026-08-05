package com.thalicloud.delivery.controller;

import com.thalicloud.delivery.dto.request.LocationUpdateRequest;
import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.service.DeliveryPartnerService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
//
// Not a REST/HTTP controller — this is a STOMP @MessageMapping handler over
// the /ws WebSocket endpoint, so it is not picked up by springdoc/Swagger UI.
// The @Tag below is kept only for readability of this class; there is no
// @Operation-annotatable HTTP method here.
@Controller
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Live Location (WebSocket)", description = "STOMP/WebSocket handler for delivery partners publishing their live GPS location while on an active delivery. Not an HTTP REST endpoint — connect via STOMP over /ws and publish to /app/partner/{partnerId}/location using the authenticated partner's own id.")
public class LocationController {

    private final DeliveryPartnerService deliveryPartnerService;

    /** STOMP destination /app/partner/{partnerId}/location — not an HTTP endpoint, so no @Operation applies. */
    @MessageMapping("/partner/{partnerId}/location")
    public void publishLocation(
            @DestinationVariable String partnerId,
            @Payload LocationUpdateRequest request,
            Principal principal) {
        log.info("publishLocation: start, partnerId={}", partnerId);
        try {
            if (!(principal instanceof Authentication auth) || !(auth.getPrincipal() instanceof DeliveryPartner authenticatedPartner)) {
                log.warn("Rejected location update for {} — unauthenticated STOMP session", partnerId);
                log.info("publishLocation: end, partnerId={}, rejected=true", partnerId);
                return;
            }

            UUID authenticatedPartnerId = authenticatedPartner.getId();
            if (!authenticatedPartnerId.toString().equals(partnerId)) {
                log.warn("Rejected location update — session partner {} tried to publish as {}", authenticatedPartnerId, partnerId);
                log.info("publishLocation: end, partnerId={}, rejected=true", partnerId);
                return;
            }
            if (request.getLatitude() == null || request.getLongitude() == null) {
                log.info("publishLocation: end, partnerId={}, missingCoordinates=true", partnerId);
                return;
            }

            deliveryPartnerService.updateLocation(authenticatedPartnerId, request.getLatitude(), request.getLongitude());
            log.info("publishLocation: end, partnerId={}", partnerId);
        } catch (Exception e) {
            log.error("publishLocation: failed, partnerId={}", partnerId, e);
            throw e;
        }
    }
}
