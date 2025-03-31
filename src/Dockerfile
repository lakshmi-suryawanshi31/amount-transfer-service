# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-alpine

# Set the working directory in the container
WORKDIR /app

# Copy the project’s build output (JAR file) to the container
COPY build/libs/*.jar app.jar

# Expose the port that the application will run on
EXPOSE 18080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]