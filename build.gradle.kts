plugins {
    kotlin("jvm") version "1.9.25"
    application
}

group = "org.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

application {
    // Adjust this if your main function lives elsewhere
    mainClass.set("org.example.orchestrator.OrchestratorApplicationKt")
}

tasks.test {
    useJUnitPlatform()
}
