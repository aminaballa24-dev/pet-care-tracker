# Pet Care Service

## 1. What this is

This is a small HTTP service made for **Track B — Kubernetes**. The goal of this project is to run the same Java application in two Kubernetes Pods and show load balancing, health checks, and a rolling update.

The project is written in **Java using Spring Boot**.

The service has two endpoints:
- `/` shows the app name, version, and hostname.
- `/health` is used for the Kubernetes liveness and readiness probes.

```text
Client → Traefik Ingress
             ↓
       ClusterIP Service
          ↙        ↘
       Pod 1      Pod 2
       :8080      :8080
```

## 2. Run it from scratch

### Prerequisites

The following should be installed:
- Docker
- k3d
- kubectl
- Java 21 if the application is built locally instead of through Docker

### Create the cluster

If you already have a `cos` cluster and want to start completely fresh, you can delete it first:

```bash
k3d cluster delete cos
```

This deletes the existing `cos` cluster, so skip this command if you want to keep it.

Create the cluster:

```bash
k3d cluster create cos --port 8081:80@loadbalancer
```

### Build the Docker image

```bash
docker build -t pet-care-service:v1 .
```

The Dockerfile uses `eclipse-temurin:21.0.12_8-jdk` as the build-stage image and `eclipse-temurin:21.0.12_8-jre` as the runtime image. The versions are pinned instead of using `latest`.

Import the image into k3d:

```bash
k3d image import pet-care-service:v1 -c cos
```

Apply the Kubernetes files:

```bash
kubectl apply -f k8s/
```

Wait until both Pods are ready:

```bash
kubectl get pods -w
```

Then check the endpoints:

```bash
kubectl get endpoints
```

Test the service through the Ingress:

```bash
for i in 1 2 3 4; do curl -s localhost:8081/; echo; done
```

### Test the rolling update

Change the version in `k8s/configmap.yaml` from `v1` to `v2` and
change the `app-version` annotation in `k8s/deployment.yaml` from `v1` to `v2`.

Then apply the changes:

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl rollout status deployment/pet-care-service
```

Check the new version:

```bash
curl -s localhost:8081/
```

To return to the previous version, change the ConfigMap back to `v1` and run:

```bash
kubectl apply -f k8s/configmap.yaml
kubectl rollout undo deployment/pet-care-service
kubectl rollout status deployment/pet-care-service
```

Then check again:

```bash
curl -s localhost:8081/
```

### Cleanup

When finished, the cluster can be removed with:

```bash
k3d cluster delete cos
```

## 3. Evidence

### Docker image sizes

The build-stage JDK image was about **726 MB**.

The final `pet-care-service:v1` image was about **499 MB**.

The final image also runs as the non-root user `appuser`.

### Kubernetes Pods

The final deployment used two replicas. The actual output from my cluster was:

```text
NAME                                READY   STATUS    RESTARTS   AGE
pet-care-service-85dcfff76d-zjl68   1/1     Running   0          32m
pet-care-service-85dcfff76d-zznkt   1/1     Running   0          33m
```

### Kubernetes endpoints

```text
Warning: v1 Endpoints is deprecated in v1.33+; use discovery.k8s.io/v1 EndpointSlice
NAME               ENDPOINTS                         AGE
kubernetes         172.21.0.3:6443                   15h
pet-care-service   10.42.0.12:8080,10.42.0.13:8080   17m
```

The two endpoints show that the Service has two Pods available.

### Load balancing

Running the curl command several times returned two different hostnames:

```text
{"app":"pet-care-service","version":"v1","hostname":"pet-care-service-6688f4c8b-wtv7n"}
{"app":"pet-care-service","version":"v1","hostname":"pet-care-service-6688f4c8b-4jjtk"}
{"app":"pet-care-service","version":"v1","hostname":"pet-care-service-6688f4c8b-wtv7n"}
{"app":"pet-care-service","version":"v1","hostname":"pet-care-service-6688f4c8b-4jjtk"}
```

This shows that requests were reaching both Pods.

A rolling update from `v1` to `v2` was also tested successfully.
The deployment was then rolled back to `v1`.

## 4. One decision

I decided to use a ConfigMap for the app version instead of putting the 
version directly in the Docker image. This makes it easier to change the 
version in Kubernetes. I used two Pods so that Kubernetes can send requests
to both of them. For a real application, I would probably also use a database
and more detailed health checks. However, I think the project was enough 
to demonstrate the main Kubernetes features.