Scott Sullivan

# Mini Cloud with Kotlin-Based Service Mesh on Raspberry Pi Cluster

### May 2025

## Overview and Problem Statement

This project is a self‑hosted, multi‑node microservice platform running on a Raspberry Pi 5 cluster, designed to simulate and study production‑grade distributed systems in a constrained, local environment. The platform is built using Kotlin as the primary language, with a strong focus on Kotlin DSLs, service orchestration, distributed communication via a lightweight service mesh, and workflow management using Temporal. Each microservice runs independently, accompanied by a Kotlin‑based sidecar process that acts as a mesh agent to handle service discovery, routing, metrics collection, and secure inter‑service communication.

At the heart of the system is a Kotlin‑powered orchestrator DSL, which allows for declarative configuration of the service cluster layout. Developers define services, nodes, dependencies, and environment variables through an intuitive and type‑safe DSL. This DSL is used to generate runtime artifacts (e.g., Docker Compose files, environment configs, or mesh sidecar bindings), enabling rapid iteration and testing. The project includes a series of microservices that mirror real‑world, stateful interactions found in processes such as user authentication, document upload, asynchronous approval logic, and notification workflows.

A dedicated workflow‑engine service hosts Temporal workers that coordinate long‑lived and multi‑step workflows such as onboarding and approvals. These workflows call into multiple services, handling retries, delays, and distributed consistency without complex orchestration logic scattered across microservices. The mesh agent running as a sidecar provides observability hooks, health checks, and fault‑tolerant routing mechanisms, enabling a realistic simulation of a service mesh environment akin to Istio or Linkerd.

Observability and telemetry are first‑class concerns in this system. Each mesh agent exposes Prometheus‑compatible metrics endpoints, and traces or logs can be exported to a centralized backend like Grafana, Loki, or Jaeger. A custom dashboard will be built to visualize service health, traffic patterns, workflow execution status, and system‑level metrics across nodes — enabling a comprehensive view of the cluster’s behavior in real time.

This project serves both as a learning platform for modern Kotlin and infrastructure engineering, and as a testbed for exploring service mesh behavior, DSL construction, Temporal workflow modeling, and microservice orchestration. While initially targeted at Raspberry Pi hardware, the entire system is designed to be hardware‑agnostic and can be developed and tested locally using Docker, with eventual deployment to ARM64 Pis requiring minimal changes. The ultimate goal is to gain hands‑on mastery of distributed systems architecture in a modular, observable, and reproducible environment.

The code is probably not the best. The quantity of comments is, quite frankly, obscene. But hey, I want to document this well to keep learning.

Enjoy responsibly.

## Phases and Planning Information

- Local Implementation & Learning: Simulate a mesh and services using Docker on my local machine. Use Kotlin to define services, generate configs, and power the mesh.
    - Service Orchestration DSL (Kotlin) → generate Docker Compose YAML with sidecars for each service.
    - Microservices
        - Auth‑service → issue JWTs, stores user sessions, validates credentials or magic links
        - User‑service → stores personal info (name, address, contact), triggers onboarding workflow via Temporal after registration
        - Document‑service → handles upload of documents (i.e. ID, income, tax forms), can simulate virus scanning/OCR/human review steps
        - Approval‑service → Simulates background checks or approval logic, powered by a stateful Temporal workflow that runs across time.
        - Notification‑service → async worker for email/SMS/push, invoked by workflows or services
    - Kotlin‑Based Mesh Sidecar (core project) → service discover registry, request proxying, basic routing and retry logic, collect metrics, DSL‑driven configuration loading, structured logging, etc.
    - Observability Dashboard → Prometheus to scrape sidecar metrics, visualize in Grafana, export Temporal workflow metrics (i.e. time in each step, failure counts), visualize failed/completed workflows in Grafana

- Raspberry Pi Cluster Deployment (Future): Shift existing codebase and services onto actual Raspberry Pi nodes.
    - Rebuild Kotlin projects for ARM64
    - Use the same DSL config
    - Target output: Docker Compose files or systemd units for Pis
    - Use Wi‑Fi mesh or switch‑based networking between Pis
    - Run each node with its mesh agent and services

- Temporal workflow ideas: These workflow ideas will be used to gain knowledge on Temporal usage.
    - workflow OnboardUser
        - sendWelcomeEmail()
        - waitForUserProfileCompletion()
        - waitForDocumentUpload()
        - runApprovalWorkflow()
        - notifyUserOfResult()
    - workflow ApprovalWorkflow
        - runBasicElgibilityChecks()
        - Simulate async task (i.e. credit scoring, etc.)
        - Mark user as approved or rejected

## Learning Coverage by Component

| Component | Kotlin Focus | Systems Focus |
|---|---|---|
| orchestrator | DSLs, extension fns, builders | Service config, infra as code |
| mesh‑agent | Coroutines, networking, observability | Sidecars, discovery, metrics, mTLS |
| workflow‑engine | Interfaces, Temporal SDK, state mgmt | Durable workflows, retries, timers |
| auth‑service | REST APIs, JWT, test containers | Identity, secure flows |
| user‑service | Data modeling, coroutines, calls to workflows | Lifecycle triggering |
| document‑service | File APIs, async processing simulation | Real‑world multi‑step flows |
| approval‑service | External task mocking, status handling | Business rule orchestration |
| notification‑service | Background job worker, batching | Decoupled, async microservice comm |

## Milestones

### Complete the Orchestrator

### Scaffold out the microservices and verify they connect with orchestrator DSL

### Service Mesh and Proxy Sidecars

## Project Iteration

This is a mostly off‑the‑cuff accounting of how I worked through this project with pointers to code snippets, etc. It’s meant to roughly guide someone through my thought process and also serve as a reminder for me to keep track of things as I go. I may or may not edit this down later into a traditional design document. But right now a large amount of it is iterative.

### Orchestrator

#### Why create an orchestrator?

- Centralized, Declarative Configuration → Rather than writing multiple docker‑compose.yml files by hand, duplicating environment config across services, and keeping deployment details scattered, we can declare everything in a single, type‑safe Kotlin DSL and let the compiler and generator handle the rest.
- Separation of Concerns → Microservices contain app logic and the orchestrator manages infrastructure layout and runtime orchestration. Think of the orchestrator like a lightweight version of Terraform except it’s easier, type‑safe, and built in Kotlin which I’m learning.
- Dynamic Artifact Generation → the DSL orchestrator will be able to output the compose files, systemd service configs for Raspberry Pi nodes, Kubernetes manifests, mesh agent bindings (i.e. routing rules, certs), and workflow wiring for Temporal.
- Mesh Agent Configuration → the DSL will eventually be able to drive configuration for the Kotlin‑based sidecar mesh agents (i.e. routing tables, retry/backoff rules, MTLS cert paths, observability hooks). Without the DSL, you’d be duplicating and syncing those configs manually across services.
- Ease of Scaling → once more nodes are added, the DSL lets us redeploy in minutes.

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

Create a Kotlin annotation to help the compiler scope the DSL blocks and catch accidental nesting mistakes. See documentation here. This can go in its own file called ClusterDsl.kt. We’ll apply @ClusterDsl to each builder class like ClusterBuilder, NodeBuilder, etc.

Create the builders. Each builder is annotated with @ClusterDsl to prevent leaking methods into nested scopes like node {...}. Class → ServiceCluster.kt

Define the data classes. Class → Models.kt

Write the underlying cluster logic. Class → ServiceCluster.kt

Define the DSL entry point function. Class → DslEntry.kt

#### Test and add service() support inside NodeBuilder

Once we have basic support for clusters per the DSL above, we can add in support for service configurations within the nodes. See ServiceCluster.kt for details and code comments.

#### Generate a Docker Compose YAML file

- Create a basic docker compose yaml file from the DSL model using service name, image name, port mapping, env variables, and depends_on
- Create a utility function to convert the Cluster model into a String containing valid Compose YAML
- Call it in the main() after building the DSL
- Print or write it to a file like docker‑compose.generated.yml

### Microservices

To start, we just want to set up the scaffolding of each of our applications so that we can test the orchestrator. This is how we’d register a new project. That process looks like this:

- Define the directory structures
- Scaffold each service (Spring Boot Kotlin)
- Add basic Dockerfiles
- Build JARs
    - From within directory, run: ./gradlew bootJar
- Build Docker Images
    - From within directory, run: docker build -t document-service:latest .
- Generate the Compose from DSL → ./gradlew run
- Test the DSL output
    - Validate the file → docker compose -f docker-compose.generated.yml config
    - Run the services → docker compose -f docker-compose.generated.yml up
    - Check → curl http://localhost:8080/actuator/health & curl http://localhost:8081/actuator/health, etc.

#### Auth‑service

Issues JWTs, stores user sessions, validates credentials.

#### User‑service

Stores personal info (name, email, contact info)

#### Document‑service

Handles uploads of documents, mocks OCR

#### Approval‑service

Simulates background checks or approval logic, powered by stateful Temporal workflow

#### Notification‑service

Async worker for email/sms/push invoked by workflows

### Service Mesh - Sidecar Proxy

This will be a custom service mesh in Kotlin using the sidecar pattern. This mesh will route traffic between services, perform service discovery without hardcoded hostnames or IPs, and support retries, observability (metrics/tracing), and secure communication. It will be defined declaratively via the existing Kotlin DSL.

A service mesh is a dedicated infrastructure layer that handles service‑to‑service communication in a microservices architecture. It separates network‑level concerns like routing, retries, and metrics from business logic.

| Feature | What It Means | Why It Matters |
|---|---|---|
| 🔍 Service Discovery | Find the right service instance | Avoid hardcoded addresses, scale freely |
| 🔄 Load Balancing | Distribute traffic across replicas | Enables horizontal scaling |
| 🛑 Resilience (Retries) | Retry on failure or timeout | Improves reliability and fault tolerance |
| 🔐 Secure Traffic | Encrypt communication (optional mTLS) | Defends against snooping/man‑in‑the‑middle |
| 📈 Observability | Expose metrics and traces | Enables monitoring, debugging, and dashboards |

Sidecars are small companion processes that run alongside a microservice in the same Docker container or pod. Instead of writing logic for retries, service lookups, or metrics into each service, the sidecar proxies requests and handles these things automatically. This allows microservices to stay lean and focused, mesh behavior is able to be updated independently, and it encourages better separation of concerns.

I’m creating a custom mesh rather than using Istio or Linkerd because this is, first and foremost, a learning project. By using DSL for the mesh config, we’ll have a single source of truth for which services exist, where they run, how they talk to each other, and what environment/config they need.

#### Project Setup

- Create new Kotlin project → mesh‑agent
- Set up Ktor with embedded server

#### Generate mesh config YAML

- Update the orchestrator DSL to support per‑service mesh metadata (i.e. paths, retries, etc.)
- Extend the orchestrator to output a mesh‑config.yml per service
- Validate the YAML format and content for one example service

#### Test Sidecar Using Generated Configs

Run a single mesh‑agent next to one real service

- Point it to the generated mesh‑config.yml
- Validate that request to the mesh proxy to the service correctly

#### Full Integration

- Update the orchestrator to generate full docker‑compose.yml including mesh agents and configs
- Launch multi‑service setup with agents from a single DSL run
- Add metrics endpoints, retry config, etc.

### Project Setup for service mesh

1. Create new Kotlin project → mesh‑agent

2. Set up Ktor with embedded server

### Generate mesh config YAML

1. Update the orchestrator DSL

2. Then update the ServiceCluster.kt file. Add in the MeshBuilder and update ServiceBuilder accordingly.

3. Then Update the data classes to include Mesh and Route:

4. Then build a MeshGenerator.kt class which generates a mesh YAML config, similar to what we’re doing with the ComposeGenerator.kt class.

5. Update the OrchestratorApplication.kt file to write a generated mesh yaml file to output, similar to what we did for the Docker Compose file.

6. Test that the output looks correct. It will be saved in the parent mini‑cloud directory

#### Test Sidecar Using Generated Configs

What we’ll do: TK
