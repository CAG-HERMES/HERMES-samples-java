# Stage 1: Build the application
FROM maven:3.9.0-amazoncorretto-17 AS build

# Set the working directory
WORKDIR /app

# Copy the Maven wrapper and the pom.xml (to leverage Docker's cache)
COPY pom.xml .

# Fetch dependencies
RUN mvn dependency:go-offline

# Copy the entire source code
COPY src ./src

# Build the application (skip tests for faster build during the container creation)
RUN mvn clean package -DskipTests

# Stage 2: Create the final image
FROM openjdk:17-jdk-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port the Spring Boot app runs on (default is 8080)
EXPOSE 8080

# Set environment variables (Optional)
ENV SPRING_PROFILES_ACTIVE=prod

# Set the entry point for the container
ENTRYPOINT ["java", "-jar", "app.jar"]
