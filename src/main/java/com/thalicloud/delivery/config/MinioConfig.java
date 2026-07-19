package com.thalicloud.delivery.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Value("${minio.bucket}")
    private String bucket;

    @Bean
    public MinioClient minioClient(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey
    ) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    // Best-effort: a dev machine without MinIO running yet shouldn't prevent the
    // rest of the app (auth, profile reads) from starting up. Upload calls will
    // surface a clear error at request time instead.
    @Bean
    public ApplicationRunner ensureKycBucketExists(MinioClient minioClient) {
        return (ApplicationArguments args) -> {
            try {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("Created MinIO bucket '{}'", bucket);
                }
            } catch (Exception ex) {
                log.warn("Could not reach MinIO to verify bucket '{}': {}. " +
                        "Document uploads will fail until MinIO is available.", bucket, ex.getMessage());
            }
        };
    }
}
