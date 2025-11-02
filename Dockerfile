FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml ./

RUN mvn -q -T 1C dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --from=builder /app/target/smart-doc-flow-backend-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=docker

USER app

ENTRYPOINT ["java", "-jar", "/app/app.jar"]