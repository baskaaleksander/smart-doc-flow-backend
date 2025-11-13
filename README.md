# 🧠 Smart Doc Flow (Backend)

**AI-powered document ingestion, review, and Q&A backend** built with **Spring Boot**, **JWT authentication**, **vector
search (Qdrant)**, and **S3-compatible storage**.  
Designed for production scalability, modular domain architecture, and enterprise-level security.

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-blue">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen">
  <img src="https://img.shields.io/badge/Build-Maven-orange">
  <img src="https://img.shields.io/badge/DB-PostgreSQL-336791">
  <img src="https://img.shields.io/badge/Queue-RabbitMQ-FF6600">
  <img src="https://img.shields.io/badge/Vector%20DB-Qdrant-00897B">
  <img src="https://img.shields.io/badge/Metrics-Prometheus-000000">
  <img src="https://img.shields.io/badge/License-MIT-lightgrey">
</p>

---

## 🚀 Elevator Pitch

- Upload PDFs and instantly transform them into searchable knowledge bases using AI-powered parsing and vector indexing.
- Secure, JWT-based authentication with refresh rotation and role-based access control.
- Modular, clean architecture built on **ports/adapters** for easy extensibility and testing.
- Full observability via Prometheus, Actuator, and structured MDC logging.
- Redis-backed caching for frequent user identity requests at `/auth/me` endpoint to improve response time under heavy
  load.

---

## 🌐 Live Demo / Screenshots

- **Live Demo:** https://smartdocflow.baskaaleksander.com
- **Screenshots:**
  ![SmartDocFlow Demo](https://raw.githubusercontent.com/baskaaleksander/smart-doc-flow-backend/main/assets/smartdocflowshowcase.gif)

---

## 🏗️ Architecture Overview

The backend exposes REST APIs under `/api` and WebSocket endpoints for live interactions.  
Each document is processed (PDF → text/OCR → embeddings) and indexed in **Qdrant** for retrieval-augmented chat (RAG).  
Storage is handled by **S3/MinIO**, background processing via **RabbitMQ**, and persistence in **PostgreSQL**.

```mermaid
flowchart LR
    User[User]
    FE["Frontend (Next.js)"]
    BE["Smart Doc Flow Backend (Spring Boot)"]
    PG[("PostgreSQL")]
    MQ[("RabbitMQ")]
    S3[("MinIO / S3")]
    VDB[("Qdrant Vector DB")]
    REDIS[("Redis Cache")]
    OAI[("OpenAI via Spring AI")]
    Obs[("Actuator / Prometheus")]

    User -->|HTTPS| FE
    FE -->|"REST / WebSocket"| BE
    BE --> PG
    BE --> MQ
    BE --> S3
    BE --> VDB
    BE --> REDIS
    BE --> OAI
    BE --> Obs
```

```mermaid
sequenceDiagram
  participant U as User
  participant FE as Frontend
  participant BE as Backend (Spring Boot)
  participant S3 as MinIO/S3
  participant MQ as RabbitMQ
  participant OCR as OCR Worker
  participant V as Qdrant
  participant OAI as OpenAI

  U->>FE: Upload PDF
  FE->>BE: POST /documents/upload
  BE->>S3: Store file
  BE->>MQ: Publish "doc.uploaded"
  MQ->>OCR: Consume "doc.uploaded"
  OCR->>S3: Read file
  OCR->>BE: Send extracted text
  BE->>OAI: Create embeddings
  BE->>V: Upsert vectors
  U->>FE: Ask question
  FE->>BE: POST /documents/{id}/conversations
  BE->>V: Vector search
  BE->>OAI: RAG prompt with context
  OAI-->>BE: Answer
  BE-->>FE: Streamed response
```

---

## ⚙️ Tech Stack

**Languages & Frameworks:** Java 21, Spring Boot 3.4, Spring Security, Web, Data JPA, WebSocket, Lombok, MapStruct  
**AI & Retrieval:** Spring AI (OpenAI models + embeddings), Qdrant, JDBC Chat Memory  
**Data & Infra:** PostgreSQL, RabbitMQ, MinIO/S3, Redis, Apache PDFBox, OCR adapters  
**DevOps & Observability:** Maven, Actuator, Micrometer Prometheus, Logback (MDC), JUnit 5, Mockito, Testcontainers

---

## ✨ Core Features

- 🔐 **Secure Authentication:** JWT access + refresh token rotation, role-based RBAC
- 📄 **Document Lifecycle:** Upload, storage, metadata, presigned downloads, deletion, and statistics
- 💬 **AI Conversations:** RAG-based per-document chat with contextual Q&A
- 👥 **User Management:** Profile updates, password resets, and admin dashboards
- ⚡ **Redis Cache:** Frequently used `/auth/me` endpoint is cached in Redis to reduce DB load and latency under high
  concurrency.
- 📊 **Observability:** Health metrics via Actuator & Prometheus, structured logs with correlation IDs

---

## 🧰 Running Locally (Recommended)

You **don’t need to install Java, PostgreSQL, RabbitMQ, MinIO, Redis or Qdrant manually**.  
Everything runs via Docker with a single command.

### Prerequisites

- **Docker**
- **Docker Compose** (v2+; often bundled with Docker Desktop)

### 1. Clone the repo

```bash
git clone https://github.com/baskaaleksander/smart-doc-flow-backend.git
cd smart-doc-flow-backend
```

### 2. Configure environment

Create a `.env` file in the project root (adapt values as needed):

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/postgres
POSTGRES_DB=postgres
DB_USERNAME=docroot
DB_PASSWORD=change-me

# OpenAI / AI Providers
OPENAI_API_KEY=sk-...

# JWT Tokens
JWT_ACCESS_SECRET=a-strong-secret
JWT_ACCESS_EXPIRATION=900000          # 15 min
JWT_REFRESH_SECRET=a-strong-secret
JWT_REFRESH_EXPIRATION=604800000      # 7 days

# Object Storage (MinIO)
MINIO_INTERNAL_URL=http://localhost:9000
MINIO_EXTERNAL_URL=http://localhost:9000
MINIO_ACCESS_NAME=minioadmin
MINIO_ACCESS_SECRET=change-me

# RabbitMQ
RABBIT_MQ_HOST=localhost
RABBIT_MQ_USERNAME=guest
RABBIT_MQ_PASS=guest
RABBIT_MQ_PORT=5672

# Qdrant
QDRANT_HOST=localhost

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis-password

# Cryptography (Conversations)
CONVERSATION_SECRET=base64-encoded-key
CONVERSATION_FINGERPRINT_SECRET=base64-encoded-key

# Email (SMTP)
EMAIL_USERNAME=your-email@example.com
EMAIL_PASSWORD=app-specific-password

# CORS / Frontend
FRONTEND_URL=http://localhost:3000
```

> .env.local file is also provided if needed

> The provided `docker-compose.yml` reads from this `.env` file.

Select Spring profile in Dockerfile or delete that line to use a default one.

```bash
ENV SPRING_PROFILES_ACTIVE=demo
```

### 3. Start the whole stack

```bash
docker compose up --build
```

This single command will:

- Build and run the **Smart Doc Flow backend** on `http://localhost:8080`
- Start **PostgreSQL** on `localhost:5432`
- Start **MinIO** (S3-compatible) on `http://localhost:9000` (console `:9001`)
- Start **Qdrant** on `http://localhost:6333`
- Start **RabbitMQ** on `amqp://localhost:5672` (management UI on `http://localhost:15672`)
- Start **Redis** on `localhost:6379`

Once the containers are healthy, the API will be available at:

```txt
http://localhost:8080/api
```

To stop everything:

```bash
docker compose down
```

> Data for Postgres, MinIO and Qdrant is persisted in Docker volumes (`postgres_data`, `minio_data`, `qdrant_data`).

---

## 🧠 Optional: Manual (Non-Docker) Setup

If you prefer to run services manually instead of Docker:

### Prerequisites

- Java 21
- Maven (or `mvnw`)
- PostgreSQL ≥ 14
- RabbitMQ
- MinIO (S3-compatible)
- Qdrant (vector DB)
- Redis
- OpenAI API key (for Spring AI)

### Local Quickstart (manual)

```bash
# 1. Start PostgreSQL, RabbitMQ, MinIO, Qdrant, Redis manually
# 2. Set required environment variables (similar to .env above)
# 3. Run the backend
./mvnw spring-boot:run

# API available at:
http://localhost:8080/api
```

---

## 🌍 Environment Variables (Summary)

Key variables (whether from `.env` or your shell):

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/postgres
POSTGRES_DB=postgres
DB_USERNAME=docroot
DB_PASSWORD=change-me

# OpenAI / AI Providers
OPENAI_API_KEY=sk-...

# JWT Tokens
JWT_ACCESS_SECRET=a-strong-secret
JWT_ACCESS_EXPIRATION=900000          # 15 min
JWT_REFRESH_SECRET=a-strong-secret
JWT_REFRESH_EXPIRATION=604800000      # 7 days

# Object Storage (MinIO)
MINIO_URL=http://localhost:9000
MINIO_ACCESS_NAME=minioadmin
MINIO_ACCESS_SECRET=change-me

# RabbitMQ
RABBIT_MQ_HOST=localhost
RABBIT_MQ_USERNAME=guest
RABBIT_MQ_PASS=guest
RABBIT_MQ_PORT=5672

# Qdrant
QDRANT_HOST=localhost

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis-password

# Cryptography (Conversations)
CONVERSATION_SECRET=base64-encoded-key
CONVERSATION_FINGERPRINT_SECRET=base64-encoded-key

# Email (SMTP)
EMAIL_USERNAME=your-email@example.com
EMAIL_PASSWORD=app-specific-password
EMAIL_ADDRESS=no-reply@example.com
EMAIL_SMTP=smtp.resend.com

# CORS / Frontend
FRONTEND_URL=http://localhost:3000
```

> You can also use .env.local file

---

## 🧠 API Quick Tour

> 📘 **Swagger UI:**  
> Interactive documentation available at https://smartdocflowapi.baskaaleksander.com/api/swagger-ui/index.html (or
`/api/swagger-ui/index.html`) once
> the application is running.

**Auth Notes:**

- Send `Authorization: Bearer <access_token>` in headers.
- Refresh tokens rotate automatically and are stored in secure, HTTP-only cookies.
- CSRF protection is disabled for stateless APIs.
- CORS allows requests only from `FRONTEND_URL`.

---

## 🧪 Testing & Quality

- **Unit Tests:** JUnit 5 + Mockito
- **Integration Tests:** Testcontainers (PostgreSQL, RabbitMQ, MinIO, Qdrant)

**Commands**

```bash
./mvnw test         # Run unit tests
./mvnw verify       # Run integration tests
./mvnw package      # Build JAR
```

---

## 🚢 Deployment

- **Containerization:** Build with Maven and deploy the generated fat JAR, or reuse the Docker image from
  `docker compose`.
- **Runtime:** Default port `8080`; base path `/api`.
- **Scalability:** Horizontally scalable with shared PostgreSQL, RabbitMQ, Qdrant, and MinIO.
- **Reverse Proxy:** Recommended setup with Nginx and HTTPS (TLS).

---

## 🔐 Security & Compliance

- **JWT Auth:** Access & refresh rotation with persistent refresh token store.
- **RBAC:** Method-level `@PreAuthorize` enforcement per role (ADMIN, REVIEWER, USER).
- **Secrets:** Loaded from environment variables only.
- **CORS & CSRF:** Stateless CORS; CSRF disabled for APIs.
- **Actuator Exposure:** Restricted to internal network in production.
- **Transport Security:** Enforce HTTPS/TLS in all environments.

---

## 📈 Performance & Observability

- **Vector Search:** Chunked document embeddings in Qdrant for low-latency retrieval.
- **Async Processing:** RabbitMQ streams for OCR and embedding pipelines.
- **Metrics:** Micrometer → Prometheus for monitoring.
- **Logging:** Logback + MDC (requestId, user, clientIp) for full request tracing.
- **Actuator:** Health, metrics, and system info endpoints enabled.
- **Redis Cache:** Reduces latency and DB queries for frequent `/auth/me` requests

---

## 🧾 License

This project is licensed under the **MIT License** — see the [LICENSE](./LICENSE) file for details.
