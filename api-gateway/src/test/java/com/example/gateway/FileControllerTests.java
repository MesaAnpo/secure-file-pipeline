package com.example.gateway;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FileControllerTests {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:6-alpine")
            .withExposedPorts(6379);

    @Container
    static GenericContainer<?> minio = new GenericContainer<>("minio/minio")
            .withCommand("server /data --console-address :9001")
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withExposedPorts(9000);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
        registry.add("S3_ENDPOINT", () -> "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
        registry.add("AWS_ACCESS_KEY_ID", () -> "minioadmin");
        registry.add("AWS_SECRET_ACCESS_KEY", () -> "minioadmin");
        registry.add("S3_BUCKET", () -> "uploads");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private S3Client s3;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeAll
    void createBucket() {
        s3.createBucket(CreateBucketRequest.builder().bucket("uploads").build());
    }

    @Test
    void statusReturnsNotFound() throws Exception {
        mockMvc.perform(get("/status/unknown").with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    void metricsEndpointWorks() throws Exception {
        mockMvc.perform(get("/metrics").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk());
    }

    @Test
    void uploadSetsPendingStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", "hello".getBytes());
        String id = mockMvc.perform(multipart("/upload").file(file).with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String status = redisTemplate.opsForValue().get("result:" + id);
        org.assertj.core.api.Assertions.assertThat(status).isEqualTo("pending");
    }

    @Test
    void uploadStatusDownloadFlow() throws Exception {
        byte[] content = "hello".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", content);
        String id = mockMvc.perform(multipart("/upload").file(file).with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/status/" + id).with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string("pending"));

        redisTemplate.opsForValue().set("result:" + id, "clean");

        mockMvc.perform(get("/download/" + id).with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content));
    }
}
