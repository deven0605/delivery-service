package com.thalicloud.delivery.service;

public interface DocumentStorageService {

    /**
     * Validates (FR-2.6: size &le; 5 MB, JPG/PNG/PDF) and uploads {@code data} to
     * object storage under {@code objectKey}, returning a URL the app can display.
     */
    String upload(String objectKey, byte[] data, String contentType);
}
