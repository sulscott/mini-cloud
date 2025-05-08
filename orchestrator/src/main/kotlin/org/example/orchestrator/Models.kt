package org.example.orchestrator

/**
 * A data class in Kotlin is a concise way to define immutable data containers.
 * Kotlin automatically generates equals/hashcode, toString, copy, componentN.
 * They are the primary way to hold data.
 * https://kotlinlang.org/docs/data-classes.html
 */

/**
 * This represents a fully constructed cluster configuration.
 * It's the output of the DSL and can be serialized, validated, used to generate
 * Docker Compose files or other infra. It contains a list of Node objects. Think
 * of this like the compiled form of our DSL.
 */
data class Cluster(val nodes: List<Node>)


/**
 * This models a single node as configured in the dSL.
 * It includes a name and a list of services
 */
data class Node(
    val name: String,
    val services: List<Service>
)

/**
 * Represents a single microservice within a node.
 * Includes its name, port, replica count, communication protocol (e.g. "http" or "grpc"),
 * environment variables, service dependencies, and optional mesh-specific configuration.
 */
data class Service(
    val name: String,
    val port: Int,
    val replicas: Int,
    val protocol: String,
    val env: Map<String, String>,
    val dependsOn: List<String>,
    val mesh: Mesh?
)

/**
 * Defines optional service mesh-specific configuration for a given service.
 * This includes retry behavior, timeout policies, rate limiting, authentication requirements,
 * and routing logic (e.g. for path-based routing or traffic splitting).
 * Services without mesh config are treated as opt-out of the mesh.
 */
data class Mesh(
    val retries: Int = 1,
    val timeoutMs: Int = 1000,
    val rateLimitPerSecond: Int = 100,
    val authRequired: Boolean = false,
    val routes: List<Route> = emptyList()
)

/**
 * Defines a single route entry for a mesh-enabled service.
 * Each route can map a path prefix to a target service, optionally using traffic weights
 * to support features like A/B testing or staged rollouts.
 */
data class Route(
    val path: String,
    val target: String,
    val weight: Int = 100
)