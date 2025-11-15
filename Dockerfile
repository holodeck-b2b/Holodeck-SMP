# Multi-stage Dockerfile for a Maven-built Java app
# Build stage
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy only what we need to cache dependencies
COPY pom.xml .
# If you have a multi-module build or additional parent poms, add them as needed
RUN mvn -B -e dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the jar built by Maven (adjust the pattern if your artifact name differs)
COPY --from=build /workspace/target/*.jar /app/app.jar

# Expose default port; change if your app uses a different port
EXPOSE 8080

# ENTRYPOINT
ENTRYPOINT ["java","-jar","/app/app.jar"]