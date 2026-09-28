FROM maven:3.9.11-eclipse-temurin-21-alpine AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline
COPY src src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S portfolio && adduser -S portfolio -G portfolio
WORKDIR /app
COPY --from=build /workspace/target/artist-portfolio-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/data/uploads && chown -R portfolio:portfolio /app

USER portfolio
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
