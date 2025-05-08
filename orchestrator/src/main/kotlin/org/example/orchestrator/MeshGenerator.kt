package org.example.orchestrator

import org.example.orchestrator.utilities.indent

/**
 * Converts a Cluster model into a mesh-config.yaml string used by mesh-agent sidecars.
 * Includes routing, retries, timeouts, and auth info for each service.
 */
fun generateMeshConfigYaml(cluster: Cluster): String {
    val builder = StringBuilder()
    builder.appendLine("services:")

    cluster.nodes.flatMap { it.services }.forEach { service ->
        val mesh = service.mesh ?: return@forEach

        builder.indent(1, "${service.name}:")
        builder.indent(2, "port: ${service.port}")
        builder.indent(2, "retries: ${mesh.retries}")
        builder.indent(2, "timeoutMs: ${mesh.timeoutMs}")
        builder.indent(2, "rateLimitPerSecond: ${mesh.rateLimitPerSecond}")
        builder.indent(2, "authRequired: ${mesh.authRequired}")

        if (mesh.routes.isNotEmpty()) {
            builder.indent(2, "routes:")
            mesh.routes.forEach { route ->
                builder.indent(3, "- path: \"${route.path}\"")
                builder.indent(4, "target: \"${route.target}\"")
                builder.indent(4, "weight: ${route.weight}")
            }
        }

        builder.appendLine()
    }

    return builder.toString()
}
