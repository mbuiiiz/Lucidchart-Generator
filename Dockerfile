# stage 1: build frontend
FROM node:25-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
# install dependencies 
RUN npm install
# copy everthing in frontend
COPY frontend/ ./
# build frontend
RUN npm run build

# stage 2: build backend
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
# copy pom
COPY aetherxmlbridge/pom.xml .
RUN mvn dependency:go-offline
# copy backend source
COPY aetherxmlbridge/src ./src
# copy react(frontend) into sprinboot static folder
RUN mkdir -p src/main/resources/static
COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static
# build jar
RUN mvn clean package -DskipTests -B

# stage 3: running the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# copy the build jar file
COPY --from=build /app/target/aetherxmlbridge-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT [ "java", "-jar", "app.jar" ]
