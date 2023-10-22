FROM eclipse-temurin:17-focal
MAINTAINER dynatrace.com

ARG OTEL_SERVICE_NAME=BookStoreDeploy
ENV ONE_AGENT="oneAgent"
ENV OTEL_AGENT="otelAgent"

ARG TENANT_ID
ARG TENANT_LAYER=dev
ARG TENANT_TOKEN
ENV TENANT_URL_SHELL="https://$TENANT_ID.$TENANT_LAYER.dynatracelabs.com"
ENV OTEL_TOKEN_SHELL=$TENANT_TOKEN

ENV DOCKER_VER=1.1.0

# Prepare OTel config
ENV OTEL_RESOURCE_ATTRIBUTES="service.name=java-quickstart,service.version=1.0.1"
ENV OTEL_METRICS_EXPORTER=none
ENV OTEL_EXPORTER_OTLP_TRACES_PROTOCOL=http/protobuf

COPY scripts/*.sh /opt/
COPY scripts/standalone.conf /opt/
RUN chmod +x /opt/*.sh

# install unzip and otel bash agent
RUN apt-get update && \
    apt-get install -y zip unzip vim jq && \
    wget -O - https://raw.githubusercontent.com/plengauer/opentelemetry-bash/main/INSTALL.sh | sh -E && \
    rm -rf /var/lib/apt/lists/*

# create directory for the application
RUN mkdir -p /opt/app

ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005



ENTRYPOINT if [ -z ${AGENT+x} ]; then \
                java -jar /opt/app/app.jar; \
           elif [ $AGENT = $ONE_AGENT ] && [ -n "${TENANT_URL+x}" ] && [ -n "${OA_TOKEN+x}" ]; then \
                . /opt/set_bash_instrumentation.sh; \
                . /opt/get_oa.sh; \
                . /opt/config_oa.sh; \
                exec java -jar -agentpath:/var/lib/dynatrace/oneagent/agent/lib64/liboneagentloader.so -Xshare:off /opt/app/app.jar -nofork; \
           elif [ $AGENT = $OTEL_AGENT ] && [ -n "${TENANT_URL+x}" ] && [ -n "${OTEL_TOKEN+x}" ]; then \
                . /opt/set_bash_instrumentation.sh; \
                . /opt/get_otel.sh; \
                . /opt/config_otel.sh; \
                exec java -javaagent:/opt/opentelemetry-javaagent.jar -Dotel.service.name=$SVC_NAME -jar /opt/app/app.jar; \
           else \
                java -jar /opt/app/app.jar; \
           fi

EXPOSE 8080
EXPOSE 5005
