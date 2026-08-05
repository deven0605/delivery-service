package com.thalicloud.delivery.entity;

import com.thalicloud.delivery.enums.DutyStatus;
import com.thalicloud.delivery.enums.PartnerLifecycleState;
import com.thalicloud.delivery.enums.VehicleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

// Mirrors com.thalicloud.auth.entity.DeliveryPartner — same shared
// delivery_partners table (auth-service owns ddl-auto:create for it; this
// service only reads/writes columns, same split as vendor-service <-> Vendor).
@Entity
@Table(name = "delivery_partners")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryPartner implements UserDetails {

    public static final String USERNAME_PREFIX = "PARTNER:";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(unique = true, nullable = false, length = 15)
    private String phone;

    @Column(length = 100)
    private String name;

    private LocalDate dob;

    @Column(length = 20)
    private String gender;

    @Column(length = 150)
    private String email;

    @Column(length = 2000)
    private String profilePhotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private VehicleType vehicleType;

    @Column(length = 20)
    private String vehicleNumber;

    @Column(length = 100)
    private String vehicleModel;

    // ── M2.4 Bank Details — either the bank trio or upiId is sufficient (FR-2.8) ──
    @Column(length = 100)
    private String bankAccountHolderName;

    @Column(length = 30)
    private String bankAccountNumber;

    @Column(length = 15)
    private String bankIfscCode;

    @Column(length = 100)
    private String upiId;

    // ── M12/FR-12 push notifications — Expo push token for this partner's
    // current device, overwritten on every app foreground (see
    // registerForPushNotificationsAsync on the client). Cleared automatically
    // when Expo reports it as DeviceNotRegistered (see ExpoPushClient). ──────
    @Column(length = 200)
    private String expoPushToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private PartnerLifecycleState lifecycleState;

    @Column(nullable = false)
    private boolean registrationComplete;

    // ── M3.1 Availability ───────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private DutyStatus dutyStatus = DutyStatus.OFFLINE;

    private Double currentLatitude;

    private Double currentLongitude;

    private LocalDateTime lastLocationAt;

    // ── M3.2 Dashboard — no rating engine yet; starts at the platform default. ──
    private Double rating = 5.0;

    // ── M4.2 — FR-4.8: raw counters backing a future cancellation-rate metric. ──
    @Column(nullable = false)
    private int totalAssignments;

    @Column(nullable = false)
    private int cancelledAssignments;

    // ── M7 — Cash on Delivery (COD) Collection ──────────────────────────────
    @Column(nullable = false)
    private long cashInHandPaise;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ── UserDetails ───────────────────────────────────────────────────────────

    @Override
    public String getUsername() {
        return USERNAME_PREFIX + phone;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_DELIVERY_PARTNER"));
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
