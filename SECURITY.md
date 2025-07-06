# Security Policy

This project is intended for internal use and demonstration purposes only.

Use this service as part of a multi-layered defense strategy.

Authentication can be delegated to an external OIDC provider by setting
`OIDC_ISSUER_URI`. All uploads, status checks and downloads are recorded in
`logs/audit.log` to provide a basic audit trail.

For production-grade virus scanning, we recommend deploying ClamAV with frequent database updates and isolating untrusted file execution.

## Recommended Practices

* **Process isolation** – run the gateway, worker and ClamAV in separate containers with minimal privileges. Enable seccomp/apparmor where possible.
* **File permissions** – uploaded files should be stored with `chmod 0600` so only the scanning service can read them.
* **Environment limits** – restrict CPU and memory usage for the worker container to avoid denial-of-service issues.

## Accepted File Types

Only plain text files (`.txt`) and common office documents (`.pdf`, `.docx`, `.xlsx`) are supported. Other file types are rejected with a `415` status code.

The upload size limit is 10&nbsp;MB per file. Larger files are rejected with a `413` status code.

## Updating ClamAV Signatures

In production the ClamAV database must be refreshed frequently. Run `freshclam` at startup and schedule a cron job, e.g. `0 * * * * freshclam --quiet`, to keep virus definitions up to date.
