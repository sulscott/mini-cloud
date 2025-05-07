# Mini Cloud with Kotlin-Based Service Mesh on Raspberry Pi Cluster
**Scott Sullivan**  
**May 2025**

---

## Overview and Problem Statement

This project is a self-hosted, multi-node microservice platform running on a Raspberry Pi 5 cluster, designed to simulate and study production-grade distributed systems in a constrained, local environment. The platform is built using Kotlin as the primary language, with a strong focus on Kotlin DSLs, service orchestration, distributed communication via a lightweight service mesh, and workflow management using Temporal. Each microservice runs independently, accompanied by a Kotlin-based sidecar process that acts as a mesh agent to handle service discovery, routing, metrics collection, and secure inter-service communication.

At the heart of the system is a Kotlin-powered orchestrator DSL, which allows for declarative configuration of the service cluster layout. Developers define services, nodes, dependencies, and environment variables through an intuitive and type-safe DSL. This DSL is used to generate runtime artifacts (e.g., Docker Compose files, environment configs, or mesh sidecar bindings), enabling rapid iteration and testing. The project includes a series of microservices that mirror real-world, stateful interactions found in processes such as user authentication, document upload, asynchronous approval logic, and notification workflows.

A dedicated workflow-engine service hosts Temporal workers that coordinate long-lived and multi-step workflows such as onboarding and approvals. These workflows call into multiple services, handling retries, delays, and distributed consistency without complex orchestration logic scattered across microservices. The mesh agent running as a sidecar provides observability hooks, health checks, and fault-tolerant routing mechanisms, enabling a realistic simulation of a service mesh environment akin to Istio or Linkerd.

Observability and telemetry are first-class concerns in this system. Each mesh agent exposes Prometheus-compatible metrics endpoints, and traces or logs can be exported to a centralized backend like Grafana, Loki, or Jaeger. A custom dashboard will be built to visualize service health, traffic patterns, workflow execution status, and system-level metrics across nodes — enabling a comprehensive view of the cluster’s behavior in real time.

This project serves both as a learning platform for modern Kotlin and infrastructure engineering, and as a testbed for exploring service mesh behavior, DSL construction, Temporal workflow modeling, and microservice orchestration. While initially targeted at Raspberry Pi hardware, the entire system is designed to be hardware-agnostic and can be developed and tested locally using Docker, with eventual deployment to ARM64 Pis requiring minimal changes. The ultimate goal is to gain hands-on mastery of distributed systems architecture in a modular, observable, and reproducible environment.

> The code is probably not the best. The quantity of comments is, quite frankly, obscene. But hey, I want to document this well to keep learning.
>
> Enjoy responsibly.

---

## Phases and Planning Information

**Local Implementation & Learning**: Simulate a mesh and services using Docker on my local machine. Use Kotlin to define services, generate configs, and power the mesh.

**Service Orchestration DSL (Kotlin)** → generate Docker Compose YAML with sidecars for each service.

**Microservices**

- **Auth-service** → issue JWTs, stores user sessions, validates credentials or magic links
- **User-service** → stores personal info (name, address, contact), triggers onboarding workflow via Temporal after registration
- **Document-service** → handles upload of documents (i.e. ID, income, tax forms), can simulate virus scanning/OCR/human review steps
- **Approval-service** → Simulates background checks or approval logic, powered by a stateful Temporal workflow that runs across time.
- **Notification-service** → async worker for email/SMS/push, invoked by workflows or services

**Kotlin-Based Mesh Sidecar (core project)** → service discover registry, request proxying, basic routing and retry logic, collect metrics, DSL-driven configuration loading, structured logging, etc.

**Observability Dashboard** → Prometheus to scrape sidecar metrics, visualize in Grafana, export Temporal workflow metrics (i.e. time in each step, failure counts), visualize failed/completed workflows in Grafana

**Raspberry Pi Cluster Deployment (Future)**: Shift existing codebase and services onto actual Raspberry Pi nodes.

- Rebuild Kotlin projects for ARM64
- Use the same DSL config
- Target output: Docker Compose files or systemd units for Pis
- Use Wi-Fi mesh or switch-based networking between Pis
- Run each node with its mesh agent and services

**Temporal workflow ideas**: These workflow ideas will be used to gain knowledge on Temporal usage.

### workflow OnboardUser
```
sendWelcomeEmail()  
waitForUserProfileCompletion()  
waitForDocumentUpload()  
runApprovalWorkflow()  
notifyUserOfResult()  
```

### workflow ApprovalWorkflow
```
runBasicElgibilityChecks()  
Simulate async task (i.e. credit scoring, etc.)  
Mark user as approved or rejected  
```

---

## Learning Coverage by Component

| Component            | Kotlin Focus                            | Systems Focus                              |
|---------------------|------------------------------------------|--------------------------------------------|
| orchestrator         | DSLs, extension fns, builders            | Service config, infra as code              |
| mesh-agent           | Coroutines, networking, observability    | Sidecars, discovery, metrics, mTLS         |
| workflow-engine      | Interfaces, Temporal SDK, state mgmt     | Durable workflows, retries, timers         |
| auth-service         | REST APIs, JWT, test containers          | Identity, secure flows                     |
| user-service         | Data modeling, coroutines, workflows     | Lifecycle triggering                       |
| document-service     | File APIs, async processing              | Real-world multi-step flows                |
| approval-service     | External task mocking, status handling   | Business rule orchestration                |
| notification-service | Background job worker, batching          | Decoupled, async microservice comm         |

---

## Milestones

### Complete the Orchestrator

This is where the Kotlin DSL will come into the project. We’ll define the DSL syntax and then implement the underlying domain types.

### Scaffold out the microservices and verify they connect with orchestrator DSL

Add no-functionality microservices which will eventually be filled with business logic. Ensure that the DSL from the previous milestone is able to spin up these services in Docker. Dockerfiles added, JARs built, and images generated. Spring Boot. All services wired and validated with actuator/health

### Service Mesh and Proxy Sidecars

Get the service mesh running before business logic. Figure out sidecar injection, inter-service routing via localhost, metrics exposure (i.e. /metrics), mTLS or health checks, future temporal workflow service-to-service calls.

---

## Project Iteration

This is a mostly off-the-cuff accounting of how I worked through this project which pointers to code snippets, etc. It’s meant to roughly guide someone through my thought process and also service as a reminder for me to keep track of things as I go. I may or may not edit this down later into a traditional design document. But right now a large amount of it is iterative.

### Orchestrator

#### Why create an orchestrator?

- **Centralized, Declarative Configuration** → Rather than writing multiple docker-compose.yml files by hand, duplicating environment config across services, and keeping deployment details scattered, we can declare everything in a single, type-safe Kotlin DSL and let the compiler and generator handle the rest.
- **Separation of Concerns** → Microservices contain app logic and the orchestrator manages infrastructure layout and runtime orchestration. Think of the orchestrator like a lightweight version of Terraform except it’s easier, type-safe, and built in Kotlin which I’m learning.
- **Dynamic Artifact Generation** → the DSL orchestrator will be able to output the compose files, systemd service configs for Raspberry Pi nodes, Kubernetes manifests, mesh agent bindings (i.e. routing rules, certs), and workflow wiring for Temporal.
- **Mesh Agent Configuration** → the DSL will eventually be able to drive configuration for the Kotlin-based sidecar mesh agents (i.e. routing tables, retry/backoff rules, MTLS cert paths, observability hooks). Without the DSL, you’d be duplicating and syncing those configs manually across services.
- **Ease of Scaling** → once more nodes are added, the DSL lets us redeploy in minutes.

#### Define the DSL Syntax

```kotlin
fun main() {
    cluster {
        node("local") {
            // services will go here
        }

        node("remote-1") {
            // more services
        }
    }
}
```

#### Model the Domain Types

Create a Kotlin annotation to help the compiler scope the DSL blocks and catch accidental nesting mistakes. See documentation here. This can go in its own file called ClusterDsl.kt. We’ll apply `@ClusterDsl` to each builder class like `ClusterBuilder`, `NodeBuilder`, etc.

- Create the builders → ServiceCluster.kt
- Define the data classes → Models.kt
- Write the cluster logic → ServiceCluster.kt
- Define DSL entry point → DslEntry.kt

#### Test and add `service()` support inside `NodeBuilder`

Add in support for service configurations. See `ServiceCluster.kt` for details and comments.

#### Generate a Docker Compose YAML file

- Create YAML from DSL model
- Add utility function to convert `Cluster` → Compose YAML
- Write it to `docker-compose.generated.yml`

---

## Microservices

Scaffold each Spring Boot service:

- Define directory structures
- Add Dockerfiles
- Build JARs → `./gradlew bootJar`
- Build images → `docker build -t document-service:latest .`
- Generate Compose → `./gradlew run`
- Validate → `docker compose -f docker-compose.generated.yml config`
- Run → `docker compose -f docker-compose.generated.yml up`
- Test with actuator: `curl http://localhost:8080/actuator/health`, etc.

---

## Service Mesh - Sidecar Proxy

A custom Kotlin service mesh using sidecar pattern.

This will be a custom service mesh in Kotlin using the sidecar pattern. This mesh will route traffic between services, perform service discovery without hardcoded hostnames or IPs, and support retries, observability (metrics/tracing), and secure communication. It will be defined declaratively via the existing Kotlin DSL.

A service mesh is a dedicated infrastructure layer that handles service-to-service communication in a microservices architecture. It separates network-level concerns like routing, retries, and metrics from business logic.


**Feature Table**

| Feature                 | What It Means                           | Why It Matters                               |
|------------------------|------------------------------------------|-----------------------------------------------|
| 🔍 Service Discovery    | Find the right service instance          | Avoid hardcoded addresses, scale freely       |
| 🔄 Load Balancing       | Distribute traffic across replicas       | Enables horizontal scaling                    |
| 🛑 Resilience (Retries) | Retry on failure or timeout              | Improves reliability and fault tolerance      |
| 🔐 Secure Traffic       | Encrypt communication (optional mTLS)    | Defends against snooping/man-in-the-middle   |
| 📈 Observability        | Expose metrics and traces                | Enables monitoring, debugging, and dashboards |

Sidecars are small companion processes that run alongside a microservice in the same Docker container or pod. Instead of writing logic for retries, service lookups, or metrics into each service, the sidecar proxies requests and handles these things automatically. This allows microservices to stay lean and focused, mesh behavior is able to be updated independently, and it encourages better separation of concerns.

I’m creating a custom mesh rather than using Istio or Linkerd because this is, first and foremost, a learning project. By using DSL for the mesh config, we’ll have a single source of truth for which services exist, where they run, how they talk to each other, and what environment/config they need.


---

### Technical Overview

1. **Generate mesh config YAML**
  - Update orchestrator DSL to support per-service mesh metadata
  - Output mesh-config.yml per service
  - Validate one (e.g., auth-service)

2. **Test sidecar using generated config**
  - Run single mesh-agent next to one real service
  - Point it to generated config
  - Validate proxying

3. **Full integration**
  - Update orchestrator to generate full docker-compose
  - Launch multi-service w/ agents
  - Add metrics, retry, etc.

---

### Project Setup for service mesh

- Create new Kotlin project → `mesh-agent`
- Set up Ktor embedded server 

### Generate mesh config YAML  
TK
