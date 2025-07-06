# Changelog

All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v1.0.0] – 2025-07-XX
- Public open-source release.
- Proprietary elements replaced with open components.
- Includes OIDC auth and audit trail.
- Provides Docker Compose and Kubernetes deployment examples.

## [v0.3.0] - 2025-08-01
- Move CI/CD pipeline to GitLab CI.
- Added corporate README and release notes

## [v0.4.0] - 2025-08-15
- Added Kubernetes manifests and updated documentation

## [v0.5.0] - 2025-09-01
- Switched file storage to S3/MinIO with new configuration options
- Added MinIO service to local Docker Compose
- Worker and gateway updated to stream files from S3

## [v0.6.0] - 2025-10-01
- Added basic authentication with configurable credentials
- Introduced rate limiting via Bucket4j
- Updated documentation with new environment variables

## [v0.7.0] - 2025-11-01
- Prometheus metrics exporter for worker
- Spring Boot Actuator with Prometheus registry
- Compose and Kubernetes manifests updated for metrics ports

## [v0.2.0] - 2025-07-06
### Added
- Security best practices in `SECURITY.md`
- Simplified CI workflow using Maven Docker image

## [v0.1.0] - 2025-06-01
### Added
- Initial release with API gateway and scanner worker

