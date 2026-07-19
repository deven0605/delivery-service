package com.thalicloud.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Base64;

// A file transported as a base64 string in a JSON body, in place of a
// multipart/form-data part — see DocumentStorageService for why.
@Getter
@Setter
public class Base64FileRequest {

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "File content type is required")
    private String contentType;

    @NotBlank(message = "File data is required")
    private String data; // base64-encoded, no "data:...;base64," prefix

    public byte[] decode() {
        return Base64.getDecoder().decode(data);
    }
}
