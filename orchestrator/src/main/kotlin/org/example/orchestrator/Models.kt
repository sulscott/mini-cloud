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

data class Service(
    val name: String,
    val port: Int,
    val replicas: Int,
    val env: Map<String, String>,
    val dependsOn: List<String>
)