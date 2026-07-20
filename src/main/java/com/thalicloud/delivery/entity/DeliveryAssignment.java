package com.thalicloud.delivery.entity;

import com.thalicloud.delivery.enums.AssignmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

// M4 — Delivery Request Handling. Owned outright by delivery-service (new
// table, no auth-service mirror needed — unlike DeliveryPartner this never
// needs to be read from the auth flow). Fields are a denormalized snapshot
// of the offer (kitchen name/distance, drop locality, payout, item count)
// rather than a live join to order-service's Order — order-service has no
// delivery-partner awareness yet (no partnerId/riderId column), and
// microservices shouldn't cross-DB-join anyway; orderId is kept purely as an
// external reference for display/logging.
@Entity
@Table(name = "delivery_assignments")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private DeliveryPartner partner;

    @Column(nullable = false, length = 100)
    private String orderId;

    // ── FR-4.2 — what the Incoming Request screen shows ─────────────────────
    @Column(nullable = false, length = 150)
    private String kitchenName;

    @Column(nullable = false)
    private double kitchenDistanceKm;

    @Column(nullable = false, length = 150)
    private String dropLocality;

    @Column(nullable = false)
    private long estimatedPayoutPaise;

    @Column(nullable = false)
    private double estimatedDistanceKm;

    @Column(nullable = false)
    private int itemCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private AssignmentStatus status = AssignmentStatus.OFFERED;

    // FR-4.8 — partner-supplied reason when they raise a cancellation.
    @Column(length = 255)
    private String cancellationReason;

    @Column(nullable = false)
    private LocalDateTime offeredAt;

    // FR-4.3 — offeredAt + assignment.offer-ttl-seconds.
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime respondedAt;

    @PrePersist
    protected void onCreate() {
        if (offeredAt == null) {
            offeredAt = LocalDateTime.now();
        }
        if (status == null) {
            status = AssignmentStatus.OFFERED;
        }
    }
}
