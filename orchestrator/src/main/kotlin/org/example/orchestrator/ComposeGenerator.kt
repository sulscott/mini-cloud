package org.example.orchestrator

/**
 * Extension function on StringBuilder to append an indented line.
 * @param level Number of indentation levels (2 spaces per level)
 * @param line The line of text to append
 */
fun StringBuilder.indent(level: Int, line: String) {
    appendLine("  ".repeat(level) + line)
}

/**
 * Converts a Cluster model into a Docker Compose YAML string.
 * This includes services with image, ports, environment variables, and dependencies.
 */
fun generateComposeYaml(cluster: Cluster): String {
    val builder = StringBuilder()

    // Compose version header
    builder.appendLine("version: '3.8'")
    builder.appendLine("services:")

    // Flatten all services across all nodes
    cluster.nodes.flatMap { it.services }.forEach { service ->
        // First-level indent (under 'services:')
        builder.indent(1, "${service.name}:")

        // Docker image name convention: service-name-service:latest
        // Add build context for local image build
        builder.indent(2, "build:")
        builder.indent(3, "context: ../../${service.name}-service")
        builder.indent(2, "image: ${service.name}-service:latest")


        // Port mapping: host:container
        builder.indent(2, "ports:")
        // Map host port to internal container port (spring boot always runs on 8080 inside of containers by default)
        // To check the above, you can run netstat -tuln while inside the running container.
        builder.indent(3, "- \"${service.port}:8080\"")

        // Optional environment variables
        if (service.env.isNotEmpty()) {
            builder.indent(2, "environment:")
            service.env.forEach { (key, value) ->
                builder.indent(3, "$key: \"$value\"")
            }
        }

        // Optional dependency list
        if (service.dependsOn.isNotEmpty()) {
            builder.indent(2, "depends_on:")
            service.dependsOn.forEach { dependency ->
                builder.indent(3, "- $dependency")
            }
        }

        builder.appendLine() // Add blank line between services
    }

    return builder.toString()
}
