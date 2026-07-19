package com.thalicloud.delivery.dto.request;

import com.thalicloud.delivery.enums.DocumentSide;
import com.thalicloud.delivery.enums.DocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// FR-2.4-FR-2.7 — bound from a JSON body (type/side fields + the base64-encoded file).
@Getter
@Setter
public class UploadDocumentRequest {

    @NotNull(message = "Document type is required")
    private DocumentType type;

    private DocumentSide side; // optional (FR-2.4/FR-2.5)

    @NotNull(message = "File is required")
    @Valid
    private Base64FileRequest file;
}
