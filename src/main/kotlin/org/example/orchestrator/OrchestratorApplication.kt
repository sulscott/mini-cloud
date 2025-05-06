package org.example.orchestrator

import java.io.File

fun main() {
    val clusterConfig = cluster {
        node("local") {
            service("auth") {
                port = 8080
                replicas = 2
                env("JWT_SECRET", "abc123")
            }

            service("user") {
                port = 8081
                dependsOn("auth")
            }
        }
    }
    val yaml = generateComposeYaml(clusterConfig)
    File("output/docker-compose.generated.yml").writeText(yaml)
}
