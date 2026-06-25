# Open Transit Hub (In Progress)

A real-time public transit tracker rebuilt on the **OC Transpo GTFS-RT API** —
replacing the retired OCTranspo API 2.0 (shut down April 30, 2025).

## Why the old project broke

OCTranspo's legacy REST API (`api.octranspo1.com/v2.0`) was decommissioned.
The new standard is **GTFS-RT** (General Transit Feed Specification Realtime),
hosted on Azure API Management. This project migrates to that standard while
keeping your existing Spring Boot microservice + Docker/Kubernetes architecture.

---

## Architecture

```
React Frontend (Vite + TypeScript)
       │
       ▼  REST / JSON
Spring Cloud API Gateway  :8080
       │
   ┌───┼───────────────┐
   ▼   ▼               ▼               ▼
Stop   Arrivals     Vehicles        Alerts
Svc    Svc          Svc             Svc
:8081  :8082        :8083           :8084
   │       │            │
   ▼       ▼            ▼
GTFS    GTFS-RT      GTFS-RT       RSS feed
static  TripUpdates  VehiclePos    octranspo.com
(zip)   (protobuf)   (protobuf)
```

---

## Quick start

### 1. Get a GTFS-RT API key

Register for free at [nextrip-public-api.developer.azure-api.net](https://nextrip-public-api.developer.azure-api.net/).

### 2. Run with Docker Compose

```bash
export GTFS_RT_SUBSCRIPTION_KEY=your-key-here
cd docker
docker-compose up
```

Frontend → http://localhost:3000  
API Gateway → http://localhost:8080/api

### 3. Run locally (no Docker)

Start each Spring Boot service in separate terminals:

```bash
# Terminal 1
cd backend/stop-service && mvn spring-boot:run

# Terminal 2
cd backend/arrivals-service
GTFS_RT_SUBSCRIPTION_KEY=your-key mvn spring-boot:run

# Terminal 3
cd backend/vehicles-service
GTFS_RT_SUBSCRIPTION_KEY=your-key mvn spring-boot:run

# Terminal 4
cd backend/alerts-service && mvn spring-boot:run

# Terminal 5
cd backend/api-gateway && mvn spring-boot:run

# Terminal 6 — frontend
cd frontend && npm install && npm run dev
```

### 4. Run on Kubernetes

```bash
# Create namespace and secret
kubectl create namespace transit-hub
kubectl create secret generic gtfs-rt-secret \
  --namespace transit-hub \
  --from-literal=subscription-key=your-key

# Build and push images
docker build -t transithub/stop-service:latest     backend/stop-service
docker build -t transithub/arrivals-service:latest backend/arrivals-service
docker build -t transithub/vehicles-service:latest backend/vehicles-service
docker build -t transithub/alerts-service:latest   backend/alerts-service
docker build -t transithub/api-gateway:latest      backend/api-gateway
docker build -t transithub/frontend:latest         frontend

# Deploy
kubectl apply -f kubernetes/manifests.yml
```

---

## API Reference

All endpoints are available through the API Gateway at `http://localhost:8080`.

| Endpoint | Description |
|---|---|
| `GET /api/stops/nearby?lat=45.42&lon=-75.69` | Stops within 500m |
| `GET /api/stops/search?q=rideau` | Full-text stop search |
| `GET /api/stops/{code}` | Single stop by code (e.g. `3009`) |
| `GET /api/arrivals/{stopCode}` | Next arrivals at a stop (GTFS-RT) |
| `GET /api/vehicles?route=7` | Live bus positions (GTFS-RT) |
| `GET /api/alerts` | Current service alerts (RSS) |
| `GET /api/alerts?route=7` | Alerts for a specific route |

---

## What changed from the old project

| Component | Before | After |
|---|---|---|
| Data API | `api.octranspo1.com/v2.0` (dead) | OC Transpo GTFS-RT on Azure |
| Frontend | Angular | React + Vite + TypeScript |
| Arrivals data | XML REST | Protobuf GTFS-RT TripUpdates |
| Vehicle positions | Not available | GTFS-RT VehiclePositions |
| Stop data | API call per stop | GTFS static zip (cached in memory) |
| Alerts | Not available | RSS feed parser |
| Map | Not available | Leaflet + OpenStreetMap |

---

## Environment variables

| Variable | Services | Description |
|---|---|---|
| `GTFS_RT_SUBSCRIPTION_KEY` | arrivals-service, vehicles-service | Azure API Management key |
| `VITE_API_BASE_URL` | frontend (build time) | API Gateway URL |
