# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B
ARG CACHE_BUST=20261116-v4
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
VOLUME /tmp
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

# Default to demo profile (H2 in-memory) so the app runs self-contained on Render
ENV SPRING_PROFILES_ACTIVE=demo

ENTRYPOINT ["sh", "-c", "if [ \"$SPRING_PROFILES_ACTIVE\" = 'local' ] || [ -z \"$SPRING_PROFILES_ACTIVE\" ]; then export SPRING_PROFILES_ACTIVE=demo; fi; exec java -Djava.security.egd=file:/dev/./urandom -jar app.jar --spring.profiles.active=$SPRING_PROFILES_ACTIVE"]
