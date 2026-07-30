FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre

RUN useradd --system --create-home appuser
WORKDIR /app
COPY --from=build /app/target/bistro-dei-dotti-api-0.1.0.jar app.jar

# Holds menu.json and its backups; mount a volume here to keep edits across restarts.
RUN mkdir -p /app/data && chown appuser:appuser /app/data

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
