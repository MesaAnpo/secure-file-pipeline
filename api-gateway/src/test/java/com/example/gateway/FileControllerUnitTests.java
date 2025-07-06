package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StringRedisTemplate redis;
    @MockBean
    private S3Client s3;

    private ValueOperations<String, String> valueOps = mock(ValueOperations.class);

    private void stubRedis(String key, String value) {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(key)).thenReturn(value);
    }

    @Test
    void downloadForbiddenWhenNotClean() throws Exception {
        stubRedis("result:123", "pending");
        mockMvc.perform(get("/download/123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void downloadReturnsNotFoundWhenMissing() throws Exception {
        stubRedis("result:123", "clean");
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());
        mockMvc.perform(get("/download/123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void metricsReturnZeros() throws Exception {
        stubRedis("metrics:scans:total", null);
        stubRedis("metrics:scans:infected", null);
        stubRedis("metrics:scans:clean", null);
        mockMvc.perform(get("/metrics").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"totalScans\":\"0\",\"infectedFiles\":\"0\",\"cleanFiles\":\"0\"}"));
    }
}
