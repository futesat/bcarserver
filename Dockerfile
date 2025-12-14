# Build stage
FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM amazoncorretto:21-alpine-jdk
WORKDIR /app
# Create necessary directories for the application
RUN mkdir -p /home/pi/bcarserver && \
    mkdir -p /var/log/boskicar

# Copy the built jar
COPY --from=build /app/target/bcarserver-*.jar /app/bcarserver.jar

# Expose the port
EXPOSE 3333

# Set default environment variables
ENV SERVER_PORT=3333

# Run the application
ENTRYPOINT ["java", "-jar", "/app/bcarserver.jar"]
