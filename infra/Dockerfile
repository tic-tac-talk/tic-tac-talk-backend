FROM amazoncorretto:21-alpine-jdk AS builder

ARG SERVICE_NAME
WORKDIR /app
COPY . .
WORKDIR /app/${SERVICE_NAME}
RUN chmod +x ./gradlew
RUN ./gradlew bootJar


FROM amazoncorretto:21-alpine-jdk

RUN apk add --no-cache curl

WORKDIR /app
ARG SERVICE_NAME
COPY --from=builder /app/${SERVICE_NAME}/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]