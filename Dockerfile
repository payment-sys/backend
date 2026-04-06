FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY . .
RUN gradle bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]