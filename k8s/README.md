# Kubernetes Manifests

The files in this directory provide a minimal deployment of Secure File Pipeline on Kubernetes.
Apply them in order using `kubectl apply -f <file>` or all at once:

```bash
kubectl apply -f k8s/
```

The manifests assume images are available in a container registry under `your-registry`.
Update the `image` fields to match your registry before deployment.
