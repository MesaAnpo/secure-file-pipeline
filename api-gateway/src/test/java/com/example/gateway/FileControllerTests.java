package com.example.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import com.example.gateway.AuditService;
import software.amazon.awssdk.core.sync.RequestBody;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
public class FileControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private S3Client s3;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private AuditService audit;

    private ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    private ListOperations<String, String> listOps = mock(ListOperations.class);

    @BeforeEach
    void setup() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForList()).thenReturn(listOps);
    }

    @Test
    void statusReturnsNotFound() throws Exception {
        when(valueOps.get("result:unknown")).thenReturn(null);
        mockMvc.perform(get("/status/unknown").with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    void metricsEndpointWorks() throws Exception {
        when(valueOps.get("metrics:scans:total")).thenReturn(null);
        when(valueOps.get("metrics:scans:infected")).thenReturn(null);
        when(valueOps.get("metrics:scans:clean")).thenReturn(null);
        mockMvc.perform(get("/metrics").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk());
    }

    @Test
    void uploadSetsPendingStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", "hello".getBytes());
        String id = mockMvc.perform(multipart("/upload").file(file).with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        verify(s3).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(listOps).rightPush("scan_tasks", id);
        verify(valueOps).set("result:" + id, "pending");
        when(valueOps.get("result:" + id)).thenReturn("pending");
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

        when(valueOps.get("result:" + id)).thenReturn("pending");
        mockMvc.perform(get("/status/" + id).with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string("pending"));

        redisTemplate.opsForValue().set("result:" + id, "clean");
        when(valueOps.get("result:" + id)).thenReturn("clean");

        ResponseBytes<GetObjectResponse> bytes = ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), content);
        when(s3.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(bytes);

        mockMvc.perform(get("/download/" + id).with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content));

        verify(s3).getObjectAsBytes(any(GetObjectRequest.class));
    }
}
