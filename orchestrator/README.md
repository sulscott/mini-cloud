# Mini Cloud with Kotlin-Based Service Mesh on Raspberry Pi Cluster

**Scott Sullivan**
May 2025

---

## Overview and Problem Statement

This project is a self-hosted, multi-node microservice platform running on a Raspberry Pi 5 cluster, designed to simulate and study production-grade distributed systems in a constrained, local environment. The platform is built using Kotlin as the primary language, with a strong focus on Kotlin DSLs, service orchestration, distributed communication via a lightweight service mesh, and workflow management using Temporal. Each microservice runs independently, accompanied by a Kotlin-based sidecar process that acts as a mesh agent to handle service discovery, routing, metrics collection, and secure inter-service communication.

At the heart of the system is a Kotlin-powered orchestrator DSL, which allows for declarative configuration of the service cluster layout. Developers define services, nodes, dependencies, and environment variables through an intuitive and type-safe DSL. This DSL is used to generate runtime artifacts (e.g., Docker Compose files, environment configs, or mesh sidecar bindings), enabling rapid iteration and testing. The project includes a series of microservices that mirror real-world, stateful interactions found in processes such as user authentication, document upload, asynchronous approval logic, and notification workflows.

A dedicated workflow-engine service hosts Temporal workers that coordinate long-lived and multi-step workflows such as onboarding and approvals. These workflows call into multiple services, handling retries, delays, and distributed consistency without complex orchestration logic scattered across microservices. The mesh agent running as a sidecar provides observability hooks, health checks, and fault-tolerant routing mechanisms, enabling a realistic simulation of a service mesh environment akin to Istio or Linkerd.

Observability and telemetry are first-class concerns in this system. Each mesh agent exposes Prometheus-compatible metrics endpoints, and traces or logs can be exported to a centralized backend like Grafana, Loki, or Jaeger. A custom dashboard will be built to visualize service health, traffic patterns, workflow execution status, and system-level metrics across nodes — enabling a comprehensive view of the cluster’s behavior in real time.

This project serves both as a learning platform for modern Kotlin and infrastructure engineering, and as a testbed for exploring service mesh behavior, DSL construction, Temporal workflow modeling, and microservice orchestration. While initially targeted at Raspberry Pi hardware, the entire system is designed to be hardware-agnostic and can be developed and tested locally using Docker, with eventual deployment to ARM64 Pis requiring minimal changes. The ultimate goal is to gain hands-on mastery of distributed systems architecture in a modular, observable, and reproducible environment.

The code is probably not the best. The quantity of comments is, quite frankly, obscene. But hey, I want to document this well to keep learning.
Enjoy responsibly.

---

## Phases and Planning Information

### Local Implementation & Learning

Simulate a mesh and services using Docker on my local machine. Use Kotlin to define services, generate configs, and power the mesh.

* **Service Orchestration DSL (Kotlin)** → generate Docker Compose YAML with sidecars for each service.

### Microservices

* **Auth-service** → issue JWTs, stores user sessions, validates credentials or magic links
* **User-service** → stores personal info (name, address, contact), triggers onboarding workflow via Temporal after registration
* **Document-service** → handles upload of documents (i.e. ID, income, tax forms), can simulate virus scanning/OCR/human review steps
* **Approval-service** → Simulates background checks or approval logic, powered by a stateful Temporal workflow that runs across time.
* **Notification-service** → async worker for email/SMS/push, invoked by workflows or services

### Kotlin-Based Mesh Sidecar (core project)

* service discover registry
* request proxying
* basic routing and retry logic
* collect metrics
* DSL-driven configuration loading
* structured logging, etc.

### Observability Dashboard

* Prometheus to scrape sidecar metrics
* Visualize in Grafana
* Export Temporal workflow metrics (i.e. time in each step, failure counts)
* Visualize failed/completed workflows in Grafana

### Raspberry Pi Cluster Deployment (Future)

Shift existing codebase and services onto actual Raspberry Pi nodes.

* Rebuild Kotlin projects for ARM64
* Use the same DSL config
* Target output: Docker Compose files or systemd units for Pis
* Use Wi-Fi mesh or switch-based networking between Pis
* Run each node with its mesh agent and services

---

## Temporal Workflow Ideas

These workflow ideas will be used to gain knowledge on Temporal usage.

### `workflow OnboardUser`

```
sendWelcomeEmail()
waitForUserProfileCompletion()
waitForDocumentUpload()
runApprovalWorkflow()
notifyUserOfResult()
```

### `workflow ApprovalWorkflow`

```
runBasicElgibilityChecks()
Simulate async task (i.e. credit scoring, etc.)
Mark user as approved or rejected
```

---

## Learning Coverage by Component

| Component            | Kotlin Focus                           | Systems Focus                      |
| -------------------- | -------------------------------------- | ---------------------------------- |
| orchestrator         | DSLs, extension fns, builders          | Service config, infra as code      |
| mesh-agent           | Coroutines, networking, observability  | Sidecars, discovery, metrics, mTLS |
| workflow-engine      | Interfaces, Temporal SDK, state mgmt   | Durable workflows, retries, timers |
| auth-service         | REST APIs, JWT, test containers        | Identity, secure flows             |
| user-service         | Data modeling, coroutines, workflows   | Lifecycle triggering               |
| document-service     | File APIs, async processing simulation | Real-world multi-step flows        |
| approval-service     | External task mocking, status handling | Business rule orchestration        |
| notification-service | Background job worker, batching        | Decoupled, async microservice comm |

---

## Milestones

### 1. Complete the Orchestrator

This is where the Kotlin DSL will come into the project. We’ll define the DSL syntax and then implement the underlying domain types.

### 2. Dolor sit amet

Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat.

### 3. Consectetur adipiscing elit

Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat.

---

## Project Iteration

This is a mostly off-the-cuff accounting of how I worked through this project which pointers to code snippets, etc. It’s meant to roughly guide someone through my thought process and also service as a reminder for me to keep track of things as I go. I may or may not edit this down later into a traditional design document. But right now a large amount of it is iterative.

---

### Orchestrator

#### Define the DSL Syntax

Start with the following syntax once the project is set up. This won’t immediately compile until we build out the underlying domain types.

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

Create a Kotlin annotation to help the compiler scope the DSL blocks and catch accidental nesting mistakes. See documentation here. This can go in its own file called `ClusterDsl.kt`. We’ll apply `@ClusterDsl` to each builder class like `ClusterBuilder`, `NodeBuilder`, etc.

Create the builders. Each builder is annotated with `@ClusterDsl` to prevent leaking methods into nested scopes like `node { ... }`.  → `ServiceCluster.kt`

Define the data classes  → `Models.kt`

Write the underlying cluster logic  → `ServiceCluster.kt`

Define the DSL entry point function  → `DslEntry.kt`

---

### Test and Add `service()` Support Inside `NodeBuilder`

Once we have basic support for clusters per the DSL above, we can add in support for service configurations within the nodes. See `ServiceCluster.kt` for details and code comments.

---

### Generate a Docker Compose YAML File

* Create a basic Docker Compose YAML file from the DSL model using service name, image name, port mapping, env variables, and `depends_on`
* Create a utility function to convert the Cluster model into a String containing valid Compose YAML
* Call it in the `main()` after building the DSL
* Print or write it to a file like `docker-compose.generated.yml`
