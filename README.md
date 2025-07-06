## Secure File Gateway

This open source distribution is a sanitized adaptation of ExampleCorp's internal secure file pipeline. Proprietary dependencies have been removed to make the project self-contained. The gateway now uses OIDC for authentication and MinIO for object storage.

This repository contains ExampleCorp's internal file scanning module. It runs as part of our secure document pipeline and combines a Java Spring Boot API gateway with a Python worker that integrates ClamAV via Redis. The code is published for reference under the MIT license.

## Architecture

The gateway accepts file uploads and stores them in an S3-compatible bucket. It enqueues scan jobs in Redis. The worker consumes these jobs, downloads files from S3, scans them with ClamAV, and writes the result back to Redis while updating Prometheus counters for observability.

More details and diagrams can be found in [docs/architecture.md](docs/architecture.md).

## Getting Started

1. Copy `.env.example` to `.env` and adjust values if needed.
2. From the `infrastructure` directory run `docker-compose up`.
3. The API will be available at `http://localhost:8080`. Swagger UI is exposed
   at `http://localhost:8080/swagger-ui.html` for interactive documentation.

### Environment Variables

```
REDIS_HOST=redis
CLAMAV_HOST=clamav
SCAN_QUEUE=scan_tasks
SPRING_REDIS_HOST=redis
S3_BUCKET=uploads
S3_ENDPOINT=http://minio:9000
API_USERNAME=admin
API_PASSWORD=admin
RATE_LIMIT_PER_MINUTE=60
METRICS_PORT=8000
OIDC_ISSUER_URI=
```

These are read by the worker and gateway containers.

## API Usage

Upload a file:

```bash
curl -u $API_USERNAME:$API_PASSWORD -F file=@test.txt http://localhost:8080/upload
```

Check status:

```bash
curl -u $API_USERNAME:$API_PASSWORD http://localhost:8080/status/<id>
```

Download a clean file:

```bash
curl -u $API_USERNAME:$API_PASSWORD -O http://localhost:8080/download/<id>
```

View Prometheus metrics:

```bash
curl http://localhost:8000/metrics        # worker metrics
curl -u $API_USERNAME:$API_PASSWORD http://localhost:8080/actuator/prometheus
```

## Development and Production

The provided Docker Compose setup is suitable for local development. For production deployments we ship Kubernetes manifests under [`k8s/`](k8s). See [docs/deploy.md](docs/deploy.md) for notes on both approaches and [docs/internals.md](docs/internals.md) for code organisation details.

## CI/CD

Automated tests and Docker images are built using our internal GitLab CI setup. The pipeline is defined in `.gitlab-ci.yml`.

## License

The code is provided under the MIT license to allow reuse and customization within and outside ExampleCorp.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on setting up your environment and submitting pull requests.

Changes are tracked in [CHANGELOG.md](CHANGELOG.md).

Security considerations and a basic threat model can be found in
[docs/threat-model.md](docs/threat-model.md).

## Open Source Adaptation Note

Audit integrations, token revocation hooks and advanced monitoring
from ExampleCorp's internal version are stripped or replaced with
simple stubs in this repository. The service is intended to be used as
part of a broader defense strategy alongside additional security
controls.
