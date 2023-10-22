FROM eclipse-temurin:17-focal
MAINTAINER dynatrace.com

ARG PLATFORM=x86
ARG OTEL_SERVICE_NAME=BookStoreDeploy

ARG TENANT_ID
ARG TENANT_LAYER=dev
ARG TENANT_TOKEN
ENV TENANT_URL_SHELL="https://$TENANT_ID.$TENANT_LAYER.dynatracelabs.com"
ENV OTEL_TOKEN_SHELL=$TENANT_TOKEN

ENV ONE_AGENT="oneAgent"
ENV OTEL_AGENT="otelAgent"

ENV DOCKER_VER=1.1.0

# Prepare OTel config
ENV OTEL_RESOURCE_ATTRIBUTES="service.name=java-quickstart,service.version=1.0.1"
ENV OTEL_METRICS_EXPORTER=none
ENV OTEL_EXPORTER_OTLP_TRACES_PROTOCOL=http/protobuf

COPY scripts/*.sh /opt/
RUN chmod +x /opt/*.sh

RUN apt-get update && \
    apt-get install -y zip unzip vim jq && \
    wget -O - https://raw.githubusercontent.com/plengauer/opentelemetry-bash/main/INSTALL.sh | sh -E && \
    rm -rf /var/lib/apt/lists/*

# create directory for the application
RUN mkdir -p /opt/app

ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005

ENTRYPOINT if [ -z ${AGENT+x} ]; then \
                java -jar /opt/app/app.jar; \
           else \
                /opt/start_app.sh; \
           fi

EXPOSE 8080
EXPOSE 5005
