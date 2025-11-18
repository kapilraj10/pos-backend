package kapil.raj.pos.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import kapil.raj.pos.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final S3Client s3Client;

    @Value("${aws.bucket.name}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.access.key:}")
    private String awsAccessKey;

    @Value("${aws.secret.key:}")
    private String awsSecretKey;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private final Logger log = LoggerFactory.getLogger(FileUploadServiceImpl.class);

    @Override
    public String uploadFile(MultipartFile file) {
        String key = null;
        try {
            String originalFilename = file.getOriginalFilename();
            String filenameExtension = "";

            if (originalFilename != null && originalFilename.contains(".")) {
                filenameExtension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
            }

            key = UUID.randomUUID().toString() + (filenameExtension.isEmpty() ? "" : "." + filenameExtension);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            // Query bucket location; construct URL using ACTUAL bucket region to avoid IllegalLocationConstraintException.
            try {
                GetBucketLocationResponse loc = s3Client.getBucketLocation(GetBucketLocationRequest.builder().bucket(bucketName).build());
                String bucketRegion = loc.locationConstraintAsString();
                if (bucketRegion == null || bucketRegion.isEmpty()) {
                    bucketRegion = "us-east-1";
                }

                if (!bucketRegion.equalsIgnoreCase(region)) {
                    // build a regional client with same credentials (if available)
                    S3Client regionalClient;
                    if (awsAccessKey != null && !awsAccessKey.isBlank() && awsSecretKey != null && !awsSecretKey.isBlank()) {
                        regionalClient = S3Client.builder()
                                .region(Region.of(bucketRegion))
                                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(awsAccessKey.trim(), awsSecretKey.trim())))
                                .build();
                    } else {
                        regionalClient = S3Client.builder()
                                .region(Region.of(bucketRegion))
                                .build();
                    }

                    try (regionalClient) {
                        regionalClient.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
                    }
                } else {
                    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
                }

                // Always use the resolved bucketRegion (NOT configured region) in returned URL.
                return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, bucketRegion, key);
            } catch (SdkClientException sdkEx) {
                // fall through to outer catch to try local fallback
                throw sdkEx;
            }

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed", e);
        } catch (SdkClientException sdkEx) {
            // fallback to local storage
            try {
                log.warn("S3 upload failed ({}). Falling back to local storage at {}", sdkEx.getMessage(), uploadDir);
                Path dirPath = Paths.get(uploadDir);
                Files.createDirectories(dirPath);
                Path target = dirPath.resolve(key == null ? UUID.randomUUID().toString() : key);
                Files.write(target, file.getBytes());
                return target.toAbsolutePath().toUri().toString();
            } catch (IOException io) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file locally after S3 failure", io);
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 upload failed", e);
        }
    }

    // (left for potential future use if we need a default region fallback)
    private String bucketRegionOrDefault(String r) {
        return (r == null || r.isBlank()) ? "us-east-1" : r;
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isEmpty()) return false;
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            return true;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete file", e);
        }
    }
}
