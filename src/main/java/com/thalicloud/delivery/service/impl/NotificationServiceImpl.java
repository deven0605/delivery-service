package com.thalicloud.delivery.service.impl;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final PartnerNotificationRepository notificationRepository;
    private final DeliveryPartnerRepository partnerRepository;

    @Override
    @Transactional
    public void notify(UUID partnerId, NotificationType type, String title, String body, String referenceId) {
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
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(UUID partnerId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(notificationRepository
                .findByPartnerIdOrderByCreatedAtDesc(partnerId, pageRequest)
                .map(NotificationResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID partnerId) {
        return notificationRepository.countByPartnerIdAndReadFalse(partnerId);
    }

    @Override
    @Transactional
    public void markRead(UUID partnerId, UUID notificationId) {
        PartnerNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getPartner().getId().equals(partnerId)) {
            throw new ResourceNotFoundException("Notification not found");
        }
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllRead(UUID partnerId) {
        List<PartnerNotification> unread = notificationRepository.findByPartnerIdAndReadFalse(partnerId);
        for (PartnerNotification notification : unread) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}
