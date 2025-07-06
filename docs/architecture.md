# Architecture

This project follows a microservices pattern with a Java API gateway and a Python worker that communicates via Redis. The worker uses ClamAV to scan files and stores results back in Redis for retrieval by the gateway.

```mermaid
graph LR
    client --> gateway
    gateway --> redis
    redis --> worker
    worker --> clamav
```

The worker exposes Prometheus counters on port `8000` while the API exposes standard Spring metrics at `/actuator/prometheus`.
