## Build stage
FROM eclipse-temurin:21-jdk AS builder
ARG PROFILE=dev
WORKDIR /workspace

# Gradle wrapper and build files
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

# Pre-fetch dependencies (optional, non-fatal)
RUN chmod +x ./gradlew \
    && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# App sources
COPY src ./src

# Build jar with specified profile
RUN ./gradlew --no-daemon clean bootJar -x test -Pprofile=${PROFILE}


## Dev runtime stage
FROM eclipse-temurin:21-jre AS dev

ENV TZ=Asia/Seoul \
    JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE=dev

WORKDIR /app

RUN useradd -ms /bin/bash spring

COPY --from=builder /workspace/build/libs/*-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

USER spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]


## Prod runtime stage
FROM eclipse-temurin:21-jre AS prod

ENV TZ=Asia/Seoul \
    JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE=prod

WORKDIR /app

RUN useradd -ms /bin/bash spring

COPY --from=builder /workspace/build/libs/*-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

USER spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]