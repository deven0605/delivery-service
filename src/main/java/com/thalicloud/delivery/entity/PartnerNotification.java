package com.thalicloud.delivery.entity;

import com.thalicloud.delivery.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

// M12 — Notifications (FR-12.1-FR-12.3). Owned outright by delivery-service,
// same as DeliveryAssignment. In-app tray + unread count only — there's no
// FCM/Redis infra anywhere in this workspace (same gap noted on every other
// "Notifications" mention in the tech stack), so actual push delivery is out
// of scope; this is the row a future FCM sender would read from.
@Entity
@Table(name = "partner_notifications")
@Getter
@Setter
@NoArgsConstructor
public class PartnerNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private DeliveryPartner partner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    // FR-12.3 — "tapping navigates to the relevant screen": an assignment id,
    // payout id, etc. the app resolves against `type` client-side. Null for
    // types with no natural target (e.g. ANNOUNCEMENT).
    @Column(length = 100)
    private String referenceId;

    @Column(nullable = false)
    private boolean read;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
