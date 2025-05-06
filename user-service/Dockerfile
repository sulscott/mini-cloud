FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copy built JAR into image
COPY build/libs/*.jar app.jar

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
