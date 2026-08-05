package com.thalicloud.delivery.service.impl;

import com.thalicloud.delivery.client.ExpoPushClient;
import com.thalicloud.delivery.dto.response.NotificationResponse;
import com.thalicloud.delivery.dto.response.PageResponse;
import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.entity.PartnerNotification;
import com.thalicloud.delivery.enums.NotificationType;
import com.thalicloud.delivery.exception.ResourceNotFoundException;
import com.thalicloud.delivery.repository.DeliveryPartnerRepository;
import com.thalicloud.delivery.repository.PartnerNotificationRepository;
import com.thalicloud.delivery.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final PartnerNotificationRepository notificationRepository;
    private final DeliveryPartnerRepository partnerRepository;
    private final ExpoPushClient expoPushClient;

    @Override
    @Transactional
    public void notify(UUID partnerId, NotificationType type, String title, String body, String referenceId) {
        log.info("notify: start, partnerId={}, type={}", partnerId, type);
        try {
            DeliveryPartner partner = partnerRepository.findById(partnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found"));

            PartnerNotification notification = new PartnerNotification();
            notification.setPartner(partner);
            notification.setType(type);
            notification.setTitle(title);
            notification.setBody(body);
            notification.setReferenceId(referenceId);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);

            sendPush(partner, type, title, body, referenceId);
            log.info("notify: end, partnerId={}, type={}", partnerId, type);
        } catch (Exception e) {
            log.error("notify: failed, partnerId={}, type={}", partnerId, type, e);
            throw e;
        }
    }

    // Real OS push (FR-12) alongside the in-app tray above and the STOMP offer
    // channel elsewhere — wakes the partner's device even when the app is
    // backgrounded/killed. Best-effort: a partner with no registered device
    // (push unavailable, never opened the app on a dev build) simply falls
    // back to the in-app tray/STOMP path, same as ExpoPushClient's own
    // no-op-on-missing-token behavior.
    private void sendPush(DeliveryPartner partner, NotificationType type, String title, String body, String referenceId) {
        String token = partner.getExpoPushToken();
        if (token == null || token.isBlank()) return;

        Map<String, String> data = new HashMap<>();
        data.put("type", type.name());
        if (referenceId != null) data.put("referenceId", referenceId);

        boolean stillRegistered = expoPushClient.send(token, title, body, data);
        if (!stillRegistered) {
            partner.setExpoPushToken(null);
            partnerRepository.save(partner);
            log.info("Cleared stale Expo push token for partner {}", partner.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(UUID partnerId, int page, int size) {
        log.info("getNotifications: start, partnerId={}, page={}, size={}", partnerId, page, size);
        try {
            PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
            PageResponse<NotificationResponse> response = PageResponse.from(notificationRepository
                    .findByPartnerIdOrderByCreatedAtDesc(partnerId, pageRequest)
                    .map(NotificationResponse::from));
            log.info("getNotifications: end, partnerId={}, page={}, size={}", partnerId, page, size);
            return response;
        } catch (Exception e) {
            log.error("getNotifications: failed, partnerId={}, page={}, size={}", partnerId, page, size, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID partnerId) {
        log.info("getUnreadCount: start, partnerId={}", partnerId);
        try {
            long count = notificationRepository.countByPartnerIdAndReadFalse(partnerId);
            log.info("getUnreadCount: end, partnerId={}, count={}", partnerId, count);
            return count;
        } catch (Exception e) {
            log.error("getUnreadCount: failed, partnerId={}", partnerId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void markRead(UUID partnerId, UUID notificationId) {
        log.info("markRead: start, partnerId={}, notificationId={}", partnerId, notificationId);
        try {
            PartnerNotification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
            if (!notification.getPartner().getId().equals(partnerId)) {
                throw new ResourceNotFoundException("Notification not found");
            }
            if (!notification.isRead()) {
                notification.setRead(true);
                notificationRepository.save(notification);
            }
            log.info("markRead: end, partnerId={}, notificationId={}", partnerId, notificationId);
        } catch (Exception e) {
            log.error("markRead: failed, partnerId={}, notificationId={}", partnerId, notificationId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void markAllRead(UUID partnerId) {
        log.info("markAllRead: start, partnerId={}", partnerId);
        try {
            List<PartnerNotification> unread = notificationRepository.findByPartnerIdAndReadFalse(partnerId);
            for (PartnerNotification notification : unread) {
                notification.setRead(true);
            }
            notificationRepository.saveAll(unread);
            log.info("markAllRead: end, partnerId={}, count={}", partnerId, unread.size());
        } catch (Exception e) {
            log.error("markAllRead: failed, partnerId={}", partnerId, e);
            throw e;
        }
    }
}
