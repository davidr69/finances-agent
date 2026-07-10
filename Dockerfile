# Build stage
ARG APP_VERSION="1.1.0"

FROM docker.io/gradle:9-jdk25 AS builder
ARG APP_VERSION
WORKDIR /build
COPY . .
RUN gradle build -x test

# Runtime stage
FROM registry:5000/awscorretto:25
ARG APP_VERSION="1.1.0"
COPY --from=builder /build/build/libs/finances-agent-${APP_VERSION}.jar /app/finances-agent.jar
WORKDIR /app
USER nobody
ENTRYPOINT ["/bin/sh", "-c", "java -Xmx256M -Xss512K -jar finances-agent.jar"]
