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

            service("document") {
                port = 8082
                dependsOn("user")
            }

            service("approval") {
                port = 8083
                dependsOn("document", "user")
            }

            service("notification") {
                port = 8084
            }
        }
    }
    val yaml = generateComposeYaml(clusterConfig)
    File("output/docker-compose.generated.yml").writeText(yaml)
}
