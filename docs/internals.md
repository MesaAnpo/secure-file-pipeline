# Internals

This document describes the code layout and scanning workflow.

## Structure

```
api-gateway/    - Spring Boot REST API
scanner-worker/ - Python worker consuming Redis jobs
infrastructure/ - Docker Compose, ClamAV and Redis setup
```

## Flow

1. A client uploads a file via `POST /upload`.
2. The gateway uploads the file to an S3 bucket and enqueues the object key on the Redis queue `scan_tasks`.
3. The worker waits for new items on that queue.
4. When a key is received, it downloads the object, scans the file with ClamAV and writes the result to `result:<id>` in Redis while incrementing Prometheus counters and the Redis metrics keys.
5. `GET /status/{id}` reads the value from Redis.
6. `GET /download/{id}` streams the object from S3 only if the stored result equals `clean`.
7. Prometheus metrics are exposed on port `8000` by the worker and at `/actuator/prometheus` on the gateway.

This simple pattern keeps the gateway responsive while offloading heavy scanning work to the worker container.

## Security Features

The API gateway requires HTTP Basic authentication with credentials supplied via
`API_USERNAME` and `API_PASSWORD`. A simple token bucket filter limits clients
to `RATE_LIMIT_PER_MINUTE` requests per minute to mitigate abuse.
