FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace
COPY . .
RUN chmod +x ./gradlew && ./gradlew --no-daemon --console=plain bootJar -x test

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
