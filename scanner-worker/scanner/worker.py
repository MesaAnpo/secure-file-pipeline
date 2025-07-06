"""Background worker that scans files stored in S3/MinIO."""

import os
import logging
import tempfile

import redis
import clamd
import boto3
from prometheus_client import Counter, start_http_server

QUEUE_NAME = os.getenv("SCAN_QUEUE", "scan_tasks")
REDIS_HOST = os.getenv("REDIS_HOST", "redis")
CLAMAV_HOST = os.getenv("CLAMAV_HOST", "clamav")
S3_BUCKET = os.getenv("S3_BUCKET", "uploads")
S3_ENDPOINT = os.getenv("S3_ENDPOINT")
METRICS_PORT = int(os.getenv("METRICS_PORT", "8000"))

s3 = boto3.client("s3", endpoint_url=S3_ENDPOINT)  # credentials from env

METRIC_TOTAL = "metrics:scans:total"
METRIC_INFECTED = "metrics:scans:infected"
METRIC_CLEAN = "metrics:scans:clean"

PROM_TOTAL = Counter("scans_total", "Total files scanned")
PROM_INFECTED = Counter("scans_infected_total", "Files found infected")
PROM_CLEAN = Counter("scans_clean_total", "Files found clean")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
logger = logging.getLogger(__name__)

STATUS_CLEAN = "CLEAN"
STATUS_INFECTED = "INFECTED"


def scan_file(path: str) -> str:
    """Return ``STATUS_CLEAN`` or ``STATUS_INFECTED`` for the given file."""
    cd = clamd.ClamdNetworkSocket(host=CLAMAV_HOST)
    try:
        with open(path, "rb") as f:
            result = cd.instream(f)
    except Exception as exc:  # pragma: no cover - network error
        logger.error("ClamAV scan failed: %s", exc)
        return STATUS_INFECTED
    if not result:
        return STATUS_CLEAN
    status, _ = next(iter(result.values()))
    return STATUS_CLEAN if status == "OK" else STATUS_INFECTED


def _download(key: str) -> str:
    """Download object from S3 and return local path."""
    tmpdir = tempfile.mkdtemp(prefix="scan-")
    dest = os.path.join(tmpdir, os.path.basename(key))
    s3.download_file(S3_BUCKET, key, dest)
    return dest


def handle_job(r: redis.Redis, key: str) -> None:
    """Process a single object key retrieved from the queue."""
    local_path = _download(key)
    logger.info("Scanning file %s", key)
    result = scan_file(local_path)
    if result == STATUS_INFECTED:
        logger.warning("Infected: %s", key)
        r.incr(METRIC_INFECTED)
        PROM_INFECTED.inc()
    else:
        r.incr(METRIC_CLEAN)
        PROM_CLEAN.inc()
    r.incr(METRIC_TOTAL)
    PROM_TOTAL.inc()
    r.set(f"result:{os.path.basename(key)}", result.lower())


def main() -> None:
    start_http_server(METRICS_PORT)
    logger.info("Metrics exposed on port %s", METRICS_PORT)
    r = redis.Redis(host=REDIS_HOST)
    while True:
        _, job = r.blpop(QUEUE_NAME)
        handle_job(r, job.decode())


if __name__ == '__main__':
    main()
