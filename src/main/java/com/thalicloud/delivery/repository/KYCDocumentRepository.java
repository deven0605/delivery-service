package com.thalicloud.delivery.repository;

import com.thalicloud.delivery.entity.KYCDocument;
import com.thalicloud.delivery.enums.DocumentSide;
import com.thalicloud.delivery.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KYCDocumentRepository extends JpaRepository<KYCDocument, UUID> {
    List<KYCDocument> findByPartnerId(UUID partnerId);

    Optional<KYCDocument> findByPartnerIdAndTypeAndSide(UUID partnerId, DocumentType type, DocumentSide side);
}
