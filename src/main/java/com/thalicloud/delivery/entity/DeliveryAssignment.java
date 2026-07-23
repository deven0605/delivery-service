package com.thalicloud.delivery.entity;

import com.thalicloud.delivery.enums.AssignmentStatus;
import com.thalicloud.delivery.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.security.SecureRandom;
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

    // ── M8.1 — Earnings breakdown (FR-8.2). baseFarePaise + distanceFarePaise +
    // incentivePaise == estimatedPayoutPaise, enforced at creation time. ────
    @Column(nullable = false)
    private long baseFarePaise;

    @Column(nullable = false)
    private long distanceFarePaise;

    @Column(nullable = false)
    private long incentivePaise;

    @Column(nullable = false)
    private double estimatedDistanceKm;

    @Column(nullable = false)
    private int itemCount;

    // ── M5 — Pickup Navigation & Verification ───────────────────────────────
    @Column(nullable = false)
    private double kitchenLatitude;

    @Column(nullable = false)
    private double kitchenLongitude;

    // FR-5.3 — a masked/proxy number, never the kitchen staff's real line.
    @Column(nullable = false, length = 20)
    private String kitchenContactNumber;

    // FR-5.5 — shown to the kitchen on the Vendor app/portal, never returned
    // to the partner app via AssignmentResponse; only compared server-side.
    @Column(nullable = false, length = 4)
    private String pickupCode;

    // FR-5.4
    private LocalDateTime arrivedAtKitchenAt;

    // FR-5.6
    private LocalDateTime pickedUpAt;

    // ── M6 — Drop Navigation & Delivery Confirmation ────────────────────────
    @Column(nullable = false)
    private double dropLatitude;

    @Column(nullable = false)
    private double dropLongitude;

    // FR-6.2 — a masked/proxy number, never the customer's real line.
    @Column(nullable = false, length = 20)
    private String customerContactNumber;

    // FR-6.4 — delivered via SMS/push to the customer, never returned to the
    // partner app via AssignmentResponse; only compared server-side.
    @Column(nullable = false, length = 4)
    private String deliveryOtp;

    // FR-6.1
    private LocalDateTime outForDeliveryAt;

    // FR-6.3
    private LocalDateTime arrivedAtDropAt;

    // FR-6.5 — set when the customer was unreachable; verifyDelivery then
    // requires proofOfDeliveryPhotoUrl instead of the OTP.
    @Column(nullable = false)
    private boolean contactlessDrop;

    @Column(length = 2000)
    private String proofOfDeliveryPhotoUrl;

    // FR-6.6
    private LocalDateTime deliveredAt;

    // ── M7 — Cash on Delivery (COD) Collection ──────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentMethod paymentMethod = PaymentMethod.PREPAID;

    // FR-7.1 — only set (and non-null) when paymentMethod is COD.
    private Long codAmountPaise;

    // FR-7.2 — gates verifyDelivery when paymentMethod is COD.
    @Column(nullable = false)
    private boolean codCollected;

    private LocalDateTime codCollectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private AssignmentStatus status = AssignmentStatus.OFFERED;

    // FR-4.8 — partner-supplied reason when they raise a cancellation.
    @Column(length = 255)
    private String cancellationReason;

    // ── M10 — Ratings & Feedback. Customer-submitted, only ever set on a
    // DELIVERED assignment (there's no order-service/customer-app integration
    // in this workspace yet, so this arrives via the internal seam — see
    // InternalAssignmentController — same gap as everywhere else marked "internal"). ──
    private Integer customerRating;

    @Column(length = 500)
    private String customerFeedback;

    private LocalDateTime ratedAt;

    @Column(nullable = false)
    private LocalDateTime offeredAt;

    // FR-4.3 — offeredAt + assignment.offer-ttl-seconds.
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime respondedAt;

    // M9 — Delivery History (FR-9.2). Kept distinct from respondedAt: for a
    // later-cancelled delivery, respondedAt gets overwritten with the
    // cancellation moment, so this is the only place "when was it accepted"
    // survives through to history detail.
    private LocalDateTime acceptedAt;

    private static final SecureRandom PICKUP_CODE_RNG = new SecureRandom();

    @PrePersist
    protected void onCreate() {
        if (offeredAt == null) {
            offeredAt = LocalDateTime.now();
        }
        if (status == null) {
            status = AssignmentStatus.OFFERED;
        }
        if (pickupCode == null) {
            pickupCode = String.format("%04d", PICKUP_CODE_RNG.nextInt(10000));
        }
        if (deliveryOtp == null) {
            deliveryOtp = String.format("%04d", PICKUP_CODE_RNG.nextInt(10000));
        }
    }
}
