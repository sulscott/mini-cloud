package org.example.orchestrator

/**
 * This is the DSL entrypoint function. This is what users call in their config files.
 * block: ClusterBuilder.() -> Unit
 * This means you're passing a lambda with receiver.
 * That receiver is an instance of ClusterBuilder
 */
fun cluster(block: ClusterBuilder.() -> Unit): Cluster {
    val builder = ClusterBuilder() // <-- instantiate a new ClusterBuilder which will collect nodes as they're declared
    builder.block() // <-- invoke the block in the context of the builder
    return builder.build() // <-- convert the stateful builder into an immutable Cluster data model
}