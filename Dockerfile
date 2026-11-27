# Build stage
ARG APP_VERSION="1.3.1"

FROM docker.io/gradle:9-jdk27 AS builder
ARG APP_VERSION
WORKDIR /build
COPY . .
RUN gradle build -x test

# Runtime stage
FROM registry:5000/awscorretto:27-alpine
ARG APP_VERSION="1.3.1"
COPY --from=builder /build/build/libs/finances-agent-${APP_VERSION}.jar /app/finances-agent.jar
WORKDIR /app
USER nobody
ENTRYPOINT ["/bin/sh", "-c", "java -Xmx256M -Xss512K -jar finances-agent.jar"]
