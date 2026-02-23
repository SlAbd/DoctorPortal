# DoctorPortal — Microservices with Spring Cloud Netflix Eureka

A learning project demonstrating **service discovery** using Spring Cloud Netflix Eureka inside a microservices architecture.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Eureka Server                        │
│              http://localhost:8761                      │
│         (@EnableEurekaServer)                           │
└──────────────┬──────────────┬───────────────────────────┘
               │   registers  │
   ┌───────────▼───┐  ┌───────▼──────┐  ┌────────────────┐
   │ DoctorService │  │DeseasesService│  │ PatientService │
   │  port: 8081   │  │  port: 8082  │  │  port: 8083    │
   │  /doctors     │  │  /deseases   │  │  /doctors      │
   │  /location    │  │  /location   │  │  /location     │
   └───────────────┘  └──────────────┘  └────────────────┘
```

---

## Services

| Service           | Artifact           | Port | Role                      |
|-------------------|--------------------|------|---------------------------|
| **EurekaServer**  | EurekaServer       | 8761 | Service registry (server) |
| **DoctorService** | DoctorService      | 8081 | Eureka client             |
| **DeseasesService** | DeseasesService  | 8082 | Eureka client             |
| **PatientService** | PatientService    | 8083 | Eureka client             |

All services use **Spring Boot 3.4.2**, **Spring Cloud 2024.0.0**, and **Java 17**.

---

## How Eureka Works in This Project

### 1. The Eureka Server — `EurekaServer`

The server is the **central registry**. It keeps a live table of every service instance that is currently running.

```java
@SpringBootApplication
@EnableEurekaServer          // activates the registry dashboard and API
public class EurekaServerApplication { ... }
```

`application.properties`:
```properties
spring.application.name=EurekaServer
server.port=8761

# The server does NOT register itself or fetch its own registry
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

The Eureka dashboard is available at **http://localhost:8761** once the server is running.

---

### 2. Eureka Clients — DoctorService, DeseasesService, PatientService

Each client service **automatically** registers itself with the Eureka server thanks to the `spring-cloud-starter-netflix-eureka-client` dependency on the classpath. No extra annotation is required (auto-configuration handles it).

Example — `DoctorService/src/main/resources/application.yml`:
```yaml
spring:
  application:
    name: doctor-service   # the name under which it registers in Eureka

server:
  port: 8081

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka   # points to the registry
```

The same pattern is repeated for `deseases-service` (port 8082) and `patient-service` (port 8083).

---

### 3. Registration Lifecycle

```
Client starts
     │
     ▼
POST /eureka/apps/{appName}  ──►  Eureka Server stores the instance
     │
     ▼  (every 30 s by default)
PUT  /eureka/apps/{appName}/{instanceId}  ──►  Heartbeat (renew lease)
     │
     ▼  (on graceful shutdown)
DELETE /eureka/apps/{appName}/{instanceId}  ──►  De-registration
```

If a client stops sending heartbeats, Eureka **evicts** it after 90 seconds (3 missed renewals).

---

### 4. What the `/location` Endpoint Demonstrates

Every client exposes a `/location` endpoint that returns its own registered name and port:

```java
@Value("${spring.application.name}")
private String appName;

@Value("${server.port}")
private String port;

@GetMapping("/location")
public String location() {
    return appName + ":" + port;   // e.g. "doctor-service:8081"
}
```

This is useful to verify which instance answered a request — handy when running **multiple instances** of the same service (load balancing scenario).

---

## Prerequisites

- Java 17+
- Maven 3.8+

---

## Running the Project

> **Important:** always start the Eureka Server first so that clients can register on startup.

### Step 1 — Start the Eureka Server

```bash
cd EurekaServer
./mvnw spring-boot:run
```

Open **http://localhost:8761** in your browser to see the Eureka dashboard.

### Step 2 — Start the client services (each in its own terminal)

```bash
# Terminal 2
cd DoctorService
./mvnw spring-boot:run

# Terminal 3
cd DeseasesService
./mvnw spring-boot:run

# Terminal 4
cd PatientService
./mvnw spring-boot:run
```

After a few seconds, all three services appear in the Eureka dashboard under their respective application names (`DOCTOR-SERVICE`, `DESEASES-SERVICE`, `PATIENT-SERVICE`).

---

## Available Endpoints

| Service           | Endpoint              | Response                     |
|-------------------|-----------------------|------------------------------|
| DoctorService     | GET /doctors          | `"list of doctors"`          |
| DoctorService     | GET /location         | `"doctor-service:8081"`      |
| DeseasesService   | GET /deseases         | `"list of deseases"`         |
| DeseasesService   | GET /location         | `"deseases-service:8082"`    |
| PatientService    | GET /patients         | `"list of patients"`          |
| PatientService    | GET /location         | `"patient-service:8083"`     |
| EurekaServer      | GET / (dashboard)     | Eureka web UI                |
| EurekaServer      | GET /eureka/apps      | All registered instances     |

---

## Key Concepts Illustrated

| Concept | Where it appears |
|---|---|
| `@EnableEurekaServer` | `EurekaServerApplication.java` |
| Auto Eureka client registration | `spring-cloud-starter-netflix-eureka-client` in each client `pom.xml` |
| Service naming (`spring.application.name`) | Each `application.yml` |
| `defaultZone` — pointing to the registry | Each client `application.yml` |
| Self-exclusion of the server from the registry | `register-with-eureka=false` / `fetch-registry=false` in `application.properties` |
| Heartbeat / lease renewal | Built-in Spring Cloud behaviour (every 30 s) |

---

## Project Structure

```
DoctorPortal/
├── EurekaServer/          # Service registry
│   └── src/main/
│       ├── java/.../EurekaServerApplication.java
│       └── resources/application.properties
├── DoctorService/         # Eureka client — port 8081
│   └── src/main/
│       ├── java/.../DoctorServiceApplication.java
│       ├── java/.../MainController.java
│       └── resources/application.yml
├── DeseasesService/       # Eureka client — port 8082
│   └── src/main/
│       ├── java/.../DeseasesServiceApplication.java
│       ├── java/.../MainController.java
│       └── resources/application.yml
└── PatientService/        # Eureka client — port 8083
    └── src/main/
        ├── java/.../PatientServiceApplication.java
        ├── java/.../MainController.java
        └── resources/application.yml
```
