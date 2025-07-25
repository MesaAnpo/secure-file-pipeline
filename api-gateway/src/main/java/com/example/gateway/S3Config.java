package com.example.gateway;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Configuration
public class S3Config {

    @Value("${S3_ENDPOINT:}")
    private String endpoint;

    @Value("${AWS_ACCESS_KEY_ID:dummy}")
    private String accessKeyId;

    @Value("${AWS_SECRET_ACCESS_KEY:dummy}")
    private String secretAccessKey;

    @Value("${S3_BUCKET:uploads}")
    private String bucket;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .region(Region.US_EAST_1);

        if (!endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint))
                .serviceConfiguration(
                    S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build());
        }
        S3Client client = builder.build();
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception e) {
            client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
        return client;
    }
}
