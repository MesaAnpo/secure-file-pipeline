# Deployment Guide

This project can run either with Docker Compose for development or on Kubernetes for production.

## Docker Compose

The fastest way to start is via the included Compose file:

```bash
cd infrastructure
docker-compose up
```

This starts Redis, ClamAV, a local MinIO server, the API gateway and the worker. Uploaded files are stored in the `uploads` bucket instead of a shared volume.
Prometheus metrics are available on `localhost:8000/metrics` for the worker and `localhost:8080/actuator/prometheus` for the gateway.

## Kubernetes

For a production setup each service can be deployed as a Deployment with its own Service. A minimal example looks like this:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway
spec:
  replicas: 1
  selector:
    matchLabels: { app: gateway }
  template:
    metadata:
      labels: { app: gateway }
    spec:
      containers:
        - name: gateway
          image: your-registry/gateway:latest
          env:
            - name: SPRING_DATA_REDIS_HOST
              value: redis
            - name: API_USERNAME
              value: admin
            - name: API_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: gateway-secret
                  key: password
```

Redis and ClamAV deployments must also be created. The worker container uses the same environment variables defined in `.env.example`.

Ready-to-use manifests are available under [`k8s/`](../k8s). Apply them with:

```bash
kubectl apply -f k8s/
```
