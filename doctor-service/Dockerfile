# Use the Maven image with Eclipse Temurin JDK 21 for the build stage
FROM maven:3.9.9-eclipse-temurin-21 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven project file to the container
COPY pom.xml .

# Download the project dependencies to the local repository
RUN mvn dependency:go-offline -B

# Copy the source code to the container
COPY src ./src

# Build the project and package it into a JAR file
RUN mvn clean package -DskipTests

# Use the OpenJDK 21 image for the runtime stage
FROM eclipse-temurin:21-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the build stage to the runtime stage
COPY --from=builder ./app/target/doctor-service-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 8081

# Set the entry point to run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]