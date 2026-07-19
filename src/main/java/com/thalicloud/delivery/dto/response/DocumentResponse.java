package com.thalicloud.delivery.dto.response;

import com.thalicloud.delivery.entity.KYCDocument;
import com.thalicloud.delivery.enums.DocumentSide;
import com.thalicloud.delivery.enums.DocumentStatus;
import com.thalicloud.delivery.enums.DocumentType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DocumentResponse {
    private final UUID id;
    private final DocumentType type;
    private final DocumentSide side;
    private final String fileUrl;
    private final DocumentStatus status;
    private final String rejectReason;
    private final LocalDateTime uploadedAt;

    public static DocumentResponse from(KYCDocument doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .type(doc.getType())
                .side(doc.getSide())
                .fileUrl(doc.getFileUrl())
                .status(doc.getStatus())
                .rejectReason(doc.getRejectReason())
                .uploadedAt(doc.getUploadedAt())
                .build();
    }
}
