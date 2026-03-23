package com.rentpro.backend.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final String bucket;
    private final String region;
    private final String accessKey;
    private final String secretKey;
    private final String endpoint;
    private final String publicBaseUrl;
    private final String keyPrefix;
    private final boolean pathStyleAccess;
    private final S3Client s3Client;

    public S3StorageService(
            @Value("${app.storage.s3.bucket}") String bucket,
            @Value("${app.storage.s3.region}") String region,
            @Value("${app.storage.s3.access-key}") String accessKey,
            @Value("${app.storage.s3.secret-key}") String secretKey,
            @Value("${app.storage.s3.endpoint:}") String endpoint,
            @Value("${app.storage.s3.public-base-url:}") String publicBaseUrl,
            @Value("${app.storage.s3.key-prefix:rentpro}") String keyPrefix,
            @Value("${app.storage.s3.path-style-access:true}") boolean pathStyleAccess
    ) {
        this.bucket = requireNonBlank(bucket, "app.storage.s3.bucket");
        this.region = requireNonBlank(region, "app.storage.s3.region");
        this.accessKey = requireNonBlank(accessKey, "app.storage.s3.access-key");
        this.secretKey = requireNonBlank(secretKey, "app.storage.s3.secret-key");
        this.endpoint = endpoint == null ? "" : endpoint.trim();
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        this.keyPrefix = normalizeKeyPrefix(keyPrefix);
        this.pathStyleAccess = pathStyleAccess;
        this.s3Client = buildClient();
    }

    @Override
    public String uploadProfilePicture(String filename, MultipartFile file) throws IOException {
        String key = buildKey("profiles/" + filename);
        upload(key, file);
        return toPublicUrl(key);
    }

    @Override
    public String uploadMaintenancePhoto(String filename, MultipartFile file) throws IOException {
        String key = buildKey("maintenance/" + filename);
        upload(key, file);
        return toPublicUrl(key);
    }

    @Override
    public void deleteByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        String key = extractKey(fileUrl);
        if (key == null || key.isBlank()) {
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (Exception ignored) {
            // Keep delete operation best-effort.
        }
    }

    private void upload(String key, MultipartFile file) throws IOException {
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
    }

    private String toPublicUrl(String key) {
        if (!publicBaseUrl.isBlank()) {
            return stripTrailingSlash(publicBaseUrl) + "/" + key;
        }
        if (!endpoint.isBlank()) {
            return stripTrailingSlash(endpoint) + "/" + bucket + "/" + key;
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    private String extractKey(String fileUrl) {
        String trimmed = fileUrl.trim();

        if (!publicBaseUrl.isBlank()) {
            String base = stripTrailingSlash(publicBaseUrl) + "/";
            if (trimmed.startsWith(base)) {
                return trimmed.substring(base.length());
            }
        }

        if (!endpoint.isBlank()) {
            String endpointWithBucket = stripTrailingSlash(endpoint) + "/" + bucket + "/";
            if (trimmed.startsWith(endpointWithBucket)) {
                return trimmed.substring(endpointWithBucket.length());
            }
        }

        try {
            URI uri = new URI(trimmed);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }

            String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
            String bucketPrefix = bucket + "/";
            if (normalizedPath.startsWith(bucketPrefix)) {
                return normalizedPath.substring(bucketPrefix.length());
            }
            return normalizedPath;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private String buildKey(String suffix) {
        if (keyPrefix.isBlank()) {
            return suffix;
        }
        return keyPrefix + "/" + suffix;
    }

    private S3Client buildClient() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ));

        if (!endpoint.isBlank()) {
            builder = builder.endpointOverride(URI.create(endpoint));
            builder = builder.forcePathStyle(pathStyleAccess);
        }

        return builder.build();
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String requireNonBlank(String value, String propertyName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required property: " + propertyName);
        }
        return value.trim();
    }

    private String normalizeKeyPrefix(String prefix) {
        if (prefix == null) {
            return "";
        }
        String trimmed = prefix.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
