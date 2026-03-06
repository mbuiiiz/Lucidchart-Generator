# stage 1: build backend
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
# copy pom
COPY aetherxmlbridge/pom.xml .
RUN mvn dependency:go-offline
# copy backend source
COPY aetherxmlbridge/src ./src
# build jar
RUN mvn clean package -DskipTests -B

# stage 2: running the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# copy the build jar file
COPY --from=build /app/target/aetherxmlbridge-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT [ "java", "-jar", "app.jar" ]
