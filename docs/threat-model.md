# Threat Model

This document outlines key assets and potential threats affecting the file scanning service. It is not exhaustive but serves as a starting point for further analysis.

## Assets
- Uploaded files stored in the S3 bucket
- Scan results stored in Redis
- Audit logs written to `logs/audit.log`

## Threats
- Unauthorized access to uploaded files or scan results
- Denial of service through large or numerous uploads
- Compromise of credentials for the OIDC provider or S3

## Mitigations
- Authentication via HTTP Basic or OIDC
- Rate limiting of all requests
- Network segregation for the worker and Redis
- Regular updates of ClamAV signatures

Further threat modeling should assess additional attack vectors and integration points as the system evolves.
