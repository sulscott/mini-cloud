package org.example.orchestrator

import java.io.File

fun main() {
    val clusterConfig = cluster {
        node("local") {
            service("auth") {
                port = 8080
                replicas = 2
                protocol = "grpc"
                env("JWT_SECRET", "abc123")

                mesh {
                    retries = 3
                    timeoutMs = 2000
                    rateLimitPerSecond = 500
                    authRequired = true
                    route("/v1", target = "auth-v1", weight = 100)
                }
            }

            service("user") {
                port = 8081
                protocol = "grpc"
                dependsOn("auth")

                mesh {
                    authRequired = true
                }
            }

            service("document") {
                port = 8082
                protocol = "grpc"
                dependsOn("user")
            }

            service("approval") {
                port = 8083
                protocol = "grpc"
                dependsOn("document", "user")
            }

            service("notification") {
                port = 8084
                protocol = "rest"

                mesh {
                    rateLimitPerSecond = 10
                    route("/v1", target = "notification-v1", weight = 80)
                    route("/v2", target = "notification-v2", weight = 20)
                }
            }
        }
    }
    val yaml = generateComposeYaml(clusterConfig)
    File("output/docker-compose.generated.yml").writeText(yaml)

    val meshYaml = generateMeshConfigYaml(clusterConfig)
    File("output/mesh-config.generated.yaml").writeText(meshYaml)
}
