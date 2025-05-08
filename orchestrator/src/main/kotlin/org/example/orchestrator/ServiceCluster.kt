package org.example.orchestrator


/**
 * Top-level builder class for defining a service cluster using the Kotlin DSL.
 *
 * A Cluster consists of one or more Nodes, each of which may contain multiple Services.
 * This builder allows DSL consumers to configure these nodes declaratively using nested blocks.
 *
 * Example:
 * ```
 * cluster {
 *     node("local") {
 *         service("auth") {
 *             port = 8080
 *             replicas = 2
 *             env("JWT_SECRET", "abc123")
 *             mesh {
 *                 retries = 3
 *                 timeoutMs = 2000
 *                 route("/v1", "auth-v1", 80)
 *                 route("/v2", "auth-v2", 20)
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * This class is invoked by the top-level `cluster {}` DSL entrypoint and builds
 * a `Cluster` data object by collecting and compiling all nested node and service definitions.
 */
@ClusterDsl
class ClusterBuilder {
    private val nodeBuilders = mutableListOf<NodeBuilder>()

    // name --> the identifier of the node (i.e. "local", "remote-1", etc.)
    // block --> the DSL body, scoped to a NodeBuilder instance
    fun node(name: String, block: NodeBuilder.() -> Unit) {
        val builder = NodeBuilder(name) // <-- create NodeBuilder instance
        builder.block() // <-- apply the block to it
        nodeBuilders += builder // <-- add it to the nodes list
    }

    // converts the ClusterBuilder into a pure data class - the final model that could be serialized, validated, or turned into YAML/JSON
    // Maps each NodeBuilder to a Node by calling .build
    fun build(): Cluster = Cluster(nodeBuilders.map { it.build() })
}


/**
 * Builder class for defining a single node within a service cluster using the DSL.
 *
 * A node represents a logical grouping of services, typically mapping to a physical or
 * virtual machine (e.g., a Raspberry Pi, an EC2 instance, or a Kubernetes node).
 * Nodes help structure service placement and allow future targeting of deployments per node.
 *
 * This builder allows configuration of one or more services nested under a given node name.
 *
 * Called by the top-level `ClusterBuilder`, this class collects all services for a node
 * and constructs a `Node` data object during the final build step.
 */
@ClusterDsl
class NodeBuilder(private val name: String) {
    private val serviceBuilders = mutableListOf<ServiceBuilder>()

    fun service(name: String, block: ServiceBuilder.() -> Unit) {
        val builder = ServiceBuilder(name) // <-- create a ServiceBuilder instance
        builder.block() // <-- apply the block to it
        serviceBuilders += builder
    }

    fun build(): Node = Node(
        name = name,
        services = serviceBuilders.map { it.build() }
    )
}

/**
 * Builder class for defining a single service within a node using the DSL.
 *
 * A service represents a deployable unit such as an API, background worker,
 * or microservice that exposes a network endpoint. This builder supports
 * configuring ports, replicas, environment variables, dependencies, and
 * service mesh integration.
 *
 * Services can optionally attach a mesh sidecar by including a `mesh` block,
 * enabling routing, retries, timeouts, rate limits, and authentication policies.
 *
 * When `build()` is called, this builder produces a validated, immutable `Service`
 * model that can be used for generating infrastructure or runtime configurations.
 */
@ClusterDsl
class ServiceBuilder(private val name: String) {
    var port: Int = 80
    var replicas: Int = 1
    var protocol: String = "http"

    private val envVars = mutableMapOf<String, String>()
    private val dependencies = mutableListOf<String>()
    private var meshBuilder: MeshBuilder? = null

    fun env(key: String, value: String) {
        envVars[key] = value
    }

    fun dependsOn(vararg serviceNames: String) {
        dependencies += serviceNames
    }

    fun mesh(block: MeshBuilder.() -> Unit) {
        val builder = MeshBuilder()
        builder.block()
        meshBuilder = builder
    }

    fun build(): Service {
        // if this condition fails, it throws an IllegalArgumentException
        require(port in 1..65535) {
            "Port number for service '$name' must be between 1 and 65535 (got $port"
        }

        // if this condition fails, it throws an IllegalArgumentException
        require(replicas > 0) {
            "Replicas for service '$name' must be greater than 0 (got $replicas)"
        }

        return Service(
            name = name,
            port = port,
            replicas = replicas,
            protocol = protocol,
            env = envVars.toMap(),
            dependsOn = dependencies.toList(),
            mesh = meshBuilder?.build() // <-- mesh can be added or left off
        )
    }
}

/**
 * Builder class for defining mesh-specific configuration for a service.
 *
 * This allows a service to opt into service mesh features such as:
 * - Retry policy (`retries`)
 * - Request timeout in milliseconds (`timeoutMs`)
 * - Rate limiting (`rateLimitPerSecond`)
 * - Authentication enforcement (`authRequired`)
 * - Route-based traffic splitting via the `route()` function
 *
 * This builder constructs a `Mesh` model, which is attached to a `Service`
 * and can later be serialized into a mesh-specific config file (e.g., mesh-config.yaml)
 * or used to configure the behavior of a mesh-agent sidecar process.
 */
@ClusterDsl
class MeshBuilder {
    var retries: Int = 1
    var timeoutMs: Int = 1000
    var rateLimitPerSecond: Int = 100
    var authRequired: Boolean = false
    private val routes = mutableListOf<Route>()

    /**
     * Defines a route for proxying traffic within the mesh.
     *
     * @param path The incoming request path (e.g. "/api/user")
     * @param target The logical destination service name (e.g. "user")
     * @param weight The percentage of traffic to send to this route (used for traffic splitting)
     *
     * Multiple routes can be defined to support weighted routing (e.g., canary or blue/green deploys).
     */
    fun route(path: String, target: String, weight: Int = 100) {
        routes += Route(path, target, weight)
    }

    fun build(): Mesh = Mesh(
        retries = retries,
        timeoutMs = timeoutMs,
        rateLimitPerSecond = rateLimitPerSecond,
        authRequired = authRequired,
        routes = routes
    )
}