package com.dotsolution.dot.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;

@Service
public class StorageService {

    @Value("${aws.s3.bucket:}")
    private String bucketName;

    @Value("${aws.s3.region:}")
    private String region;

    @Value("${aws.accessKeyId:}")
    private String accessKey;

    @Value("${aws.secretKey:}")
    private String secretKey;

    private S3Client s3Client;
    private Path localStorageLocation;
    private boolean useS3 = false;

    @PostConstruct
    public void init() {
        if (bucketName != null && !bucketName.trim().isEmpty()) {
            try {
                software.amazon.awssdk.services.s3.S3ClientBuilder builder = S3Client.builder()
                        .region(Region.of(region.isEmpty() ? "us-east-1" : region));
                
                if (accessKey != null && !accessKey.trim().isEmpty() &&
                    secretKey != null && !secretKey.trim().isEmpty()) {
                    builder.credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)
                    ));
                    System.out.println("[StorageService] Initializing AWS S3 client using static credentials.");
                } else {
                    builder.credentialsProvider(DefaultCredentialsProvider.create());
                    System.out.println("[StorageService] Initializing AWS S3 client using Default Credentials Provider (IAM Role).");
                }
                this.s3Client = builder.build();
                this.useS3 = true;
                System.out.println("[StorageService] Initialized AWS S3 client on bucket: " + bucketName + " in region: " + region);
            } catch (Exception e) {
                System.err.println("[StorageService] Failed to initialize S3 client, falling back to local storage: " + e.getMessage());
                setupLocalStorage();
            }
        } else {
            System.out.println("[StorageService] AWS S3 bucket not configured, using local storage fallback.");
            setupLocalStorage();
        }
    }

    private void setupLocalStorage() {
        this.localStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.localStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create local storage directory.", ex);
        }
    }

    public String storeFile(String subFolder, String fileName, MultipartFile file) {
        String cleanFileName = System.currentTimeMillis() + "_" + fileName;
        String key = subFolder + "/" + cleanFileName;

        if (useS3) {
            try {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(file.getContentType())
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
                
                // Return S3 URL
                return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region.isEmpty() ? "us-east-1" : region, key);
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
            }
        } else {
            try {
                Path targetDir = this.localStorageLocation.resolve(subFolder);
                Files.createDirectories(targetDir);
                Path targetLocation = targetDir.resolve(cleanFileName);
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                return targetLocation.toString();
            } catch (IOException e) {
                throw new RuntimeException("Failed to store file locally: " + e.getMessage(), e);
            }
        }
    }

    public byte[] loadFile(String path) {
        if (useS3 || path.startsWith("http")) {
            try {
                String key = extractS3Key(path);
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();
                return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
            } catch (Exception e) {
                throw new RuntimeException("Failed to load file from S3: " + e.getMessage(), e);
            }
        } else {
            try {
                Path filePath = Paths.get(path).normalize();
                return Files.readAllBytes(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load file locally: " + e.getMessage(), e);
            }
        }
    }

    public void deleteFile(String path) {
        if (useS3 || path.startsWith("http")) {
            try {
                String key = extractS3Key(path);
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();
                s3Client.deleteObject(deleteObjectRequest);
            } catch (Exception e) {
                System.err.println("Failed to delete S3 file: " + e.getMessage());
            }
        } else {
            try {
                Files.deleteIfExists(Paths.get(path));
            } catch (Exception e) {
                // Ignore local delete errors
            }
        }
    }

    private String extractS3Key(String url) {
        if (url.startsWith("http")) {
            String match = ".amazonaws.com/";
            int index = url.indexOf(match);
            if (index != -1) {
                return url.substring(index + match.length());
            }
        }
        return url;
    }
}
