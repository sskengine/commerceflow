FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /workspace
COPY . .
RUN mvn --batch-mode -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/${MODULE}-0.1.0-SNAPSHOT.jar app.jar
USER 10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

