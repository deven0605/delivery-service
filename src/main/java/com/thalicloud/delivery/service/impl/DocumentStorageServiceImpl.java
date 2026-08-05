package com.thalicloud.delivery.service.impl;

import com.thalicloud.delivery.exception.FileValidationException;
import com.thalicloud.delivery.service.DocumentStorageService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentStorageServiceImpl implements DocumentStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${document.max-file-size-bytes}")
    private long maxFileSizeBytes;

    @Value("${document.allowed-content-types}")
    private String allowedContentTypesCsv;

    @Override
    public String upload(String objectKey, byte[] data, String contentType) {
        log.info("upload: start, objectKey={}, contentType={}", objectKey, contentType);
        validate(data, contentType);

        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(in, data.length, -1)
                            .contentType(contentType)
                            .build()
            );

            // Bucket is private (KYC documents are sensitive PII) — a presigned URL
            // lets the app render the upload immediately without a public bucket.
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
            log.info("upload: end, objectKey={}", objectKey);
            return url;
        } catch (RuntimeException ex) {
            log.error("upload: failed, objectKey={}", objectKey, ex);
            throw new IllegalStateException("Could not upload document to storage. Please retry.", ex);
        } catch (Exception ex) {
            // MinioClient's putObject/getPresignedObjectUrl declare several checked
            // storage-SDK exceptions — surfaced uniformly as a retryable failure.
            log.error("upload: failed, objectKey={}", objectKey, ex);
            throw new IllegalStateException("Could not upload document to storage. Please retry.", ex);
        }
    }

    private void validate(byte[] data, String contentType) {
        if (data == null || data.length == 0) {
            throw new FileValidationException("No file provided.");
        }
        if (data.length > maxFileSizeBytes) {
            throw new FileValidationException("File exceeds the 5 MB size limit.");
        }
        List<String> allowed = Arrays.asList(allowedContentTypesCsv.split(","));
        if (contentType == null || !allowed.contains(contentType)) {
            throw new FileValidationException("Unsupported file format. Please upload a JPG, PNG, or PDF.");
        }
    }
}
