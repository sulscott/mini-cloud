package org.example.orchestrator

@ClusterDsl
class ClusterBuilder {
    private val nodes = mutableListOf<NodeBuilder>()

    // name --> the identifier of the node (i.e. "local", "remote-1", etc.)
    // block --> the DSL body, scoped to a NodeBuilder instance
    fun node(name: String, block: NodeBuilder.() -> Unit) {
        val builder = NodeBuilder(name) // <-- create NodeBuilder instance
        builder.block() // <-- apply the block to it
        nodes += builder // <-- add it to the nodes list
    }

    // converts the ClusterBuilder into a pure data class - the final model that could be serialized, validated, or turned into YAML/JSON
    // Maps each NodeBuilder to a Node by calling .build
    fun build(): Cluster = Cluster(nodes.map { it.build() })
}

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

@ClusterDsl
class ServiceBuilder(private val name: String) {
    var port: Int = 80
    var replicas: Int = 1
    private val envVars = mutableMapOf<String, String>()
    private val dependencies = mutableListOf<String>()

    fun env(key: String, value: String) {
        envVars[key] = value
    }

    fun dependsOn(vararg serviceNames: String) {
        dependencies += serviceNames
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
            env = envVars.toMap(),
            dependsOn = dependencies.toList()
        )
    }
}