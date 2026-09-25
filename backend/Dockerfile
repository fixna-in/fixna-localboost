# Multi-stage build for Fly.io / container hosts. Produces a runnable JAR without
# requiring a local mvn package on the deploy machine.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd --system --uid 10001 --create-home appuser
USER appuser
COPY --from=build /workspace/target/fixna-api.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
