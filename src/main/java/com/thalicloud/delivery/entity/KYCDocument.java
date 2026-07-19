package com.thalicloud.delivery.entity;

import com.thalicloud.delivery.enums.DocumentSide;
import com.thalicloud.delivery.enums.DocumentStatus;
import com.thalicloud.delivery.enums.DocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

// Owned outright by delivery-service — auth-service has no entity mapped to
// this table, so it's untouched by auth-service's ddl-auto:create.
@Entity
@Table(
    name = "kyc_documents",
    uniqueConstraints = @UniqueConstraint(columnNames = {"partner_id", "type", "side"})
)
@Getter
@Setter
@NoArgsConstructor
public class KYCDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private DeliveryPartner partner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentType type;

    // Null for single-sided documents (PAN, VEHICLE_RC).
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private DocumentSide side;

    @Column(nullable = false, length = 255)
    private String fileUrl;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(length = 255)
    private String rejectReason;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        if (status == null) {
            status = DocumentStatus.PENDING;
        }
    }
}
