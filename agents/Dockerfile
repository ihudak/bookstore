FROM eclipse-temurin:17-focal
MAINTAINER dynatrace.com

ARG PLATFORM=x86
ENV ONE_AGENT="oneAgent"
ENV OTEL_AGENT="otelAgent"

ENV DOCKER_VER=1.1.0

# Prepare OTel config
ENV OTEL_RESOURCE_ATTRIBUTES="service.name=java-quickstart,service.version=1.0.1"
ENV OTEL_METRICS_EXPORTER=none
ENV OTEL_EXPORTER_OTLP_TRACES_PROTOCOL=http/protobuf

COPY scripts/*.sh /opt/
RUN chmod +x /opt/*.sh

# install zip
RUN apt-get update && apt-get install -y zip unzip && rm -rf /var/lib/apt/lists/*
# create directory for the application
RUN mkdir -p /opt/app

ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005

ENTRYPOINT if [ -z ${AGENT+x} ]; then \
                java -jar /opt/app/app.jar; \
           elif [ $AGENT = $ONE_AGENT ]; then \
                . /opt/get_oa.sh; \
                java -jar -agentpath:/var/lib/dynatrace/oneagent/agent/lib64/liboneagentloader.so -Xshare:off /opt/app/app.jar -nofork; \
           elif [ $AGENT = $OTEL_AGENT ]; then \
                . /opt/get_otel.sh; \
                java -javaagent:/opt/opentelemetry-javaagent.jar -Dotel.service.name=$SVC_NAME -jar /opt/app/app.jar; \
           else \
                java -jar /opt/app/app.jar; \
           fi

EXPOSE 8080
EXPOSE 5005
