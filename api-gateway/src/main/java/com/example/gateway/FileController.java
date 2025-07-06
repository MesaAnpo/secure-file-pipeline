package com.example.gateway;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@RestController
public class FileController {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private S3Client s3;

    @Autowired
    private AuditService audit;

    @Value("${S3_BUCKET:uploads}")
    private String bucket;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String id = UUID.randomUUID().toString();
        s3.putObject(
                PutObjectRequest.builder().bucket(bucket).key(id).build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        redisTemplate.opsForList().rightPush("scan_tasks", id);
        redisTemplate.opsForValue().set("result:" + id, "pending");
        audit.log("upload:" + id + ",size=" + file.getSize());
        return ResponseEntity.ok(id);
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<String> status(@PathVariable String id) {
        String status = redisTemplate.opsForValue().get("result:" + id);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        audit.log("status:" + id + "," + status);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> download(@PathVariable String id) throws IOException {
        String status = redisTemplate.opsForValue().get("result:" + id);
        if (status == null || !"clean".equalsIgnoreCase(status)) {
            return ResponseEntity.status(403).body("File not available");
        }
        audit.log("download:" + id);
        try {
            ResponseBytes<GetObjectResponse> obj = s3.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(id).build());
            return ResponseEntity.ok(obj.asByteArray());
        } catch (NoSuchKeyException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, String>> metrics() {
        Map<String, String> m = new HashMap<>();
        m.put("totalScans", getMetric("metrics:scans:total"));
        m.put("infectedFiles", getMetric("metrics:scans:infected"));
        m.put("cleanFiles", getMetric("metrics:scans:clean"));
        return ResponseEntity.ok(m);
    }

    private String getMetric(String key) {
        String v = redisTemplate.opsForValue().get(key);
        return v == null ? "0" : v;
    }
}
