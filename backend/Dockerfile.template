# Multi-stage Maven build — copy this Dockerfile into each service directory
# and adjust the JAR name to match the service artifactId.

FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Replace 'service-name' with the actual artifactId (e.g. stop-service)
COPY --from=build /app/target/*-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
