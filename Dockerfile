# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/target/*.jar app.jar

# Create data directory for H2 file-based DB and set permissions
RUN mkdir -p /data && chown appuser:appgroup /data

# Set environment defaults
ENV DB_URL=jdbc:h2:mem:trainingdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
ENV DB_USERNAME=sa
ENV DB_PASSWORD=
ENV DB_DRIVER=org.h2.Driver
ENV MAIL_ENABLED=false
ENV BASE_URL=http://localhost:8080

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
