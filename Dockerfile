FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy pom first to leverage Docker layer caching for dependencies
COPY pom.xml .

# Pre-download dependencies
RUN mvn -B dependency:resolve dependency:resolve-plugins

# Copy the rest of the source and build the application
COPY src src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built application
COPY --from=build /workspace/target/ecommerce-service-0.0.1-SNAPSHOT.jar app.jar

# Expose application port
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
