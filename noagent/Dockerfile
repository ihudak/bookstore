#FROM openjdk:21
FROM eclipse-temurin:21-jdk-alpine
#COPY --from=pae32231.dev.dynatracelabs.com/linux/oneagent-codemodules-musl:java / /
#ENV LD_PRELOAD="/opt/dynatrace/oneagent/agent/lib64/liboneagentproc.so"
RUN mkdir -p /opt/app
ENV JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
ENV DOCKER_VER="1.5.0"

ENTRYPOINT export OTEL_SERVICE_NAME="$SERVICE_FULL_NAME" && \
           java -Dotel.service.name=$SERVICE_FULL_NAME -jar /opt/app/app.jar

EXPOSE 8080
EXPOSE 5005