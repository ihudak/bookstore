ARG VER=21
ARG FLAVOR=noble

FROM eclipse-temurin:${VER}-jdk-${FLAVOR}
MAINTAINER dynatrace.com

ARG FLAVOR=noble

ARG OTEL_SERVICE_NAME=BookStoreDeploy
ENV ONE_AGENT="oneAgent"
ENV OTEL_AGENT="otelAgent"

ARG AGENTS_PRELOAD=false
ARG DT_ENV_URL
ARG DT_ENV_ID
ARG DT_TOKEN

ENV DT_ENV_ID=$DT_ENV_ID
ENV DT_ENV_URL=$DT_ENV_URL
ENV DT_TOKEN=$DT_TOKEN

ENV DOCKER_VER="1.7.7"

# Prepare OA config
ENV OA_INSTALL_DIR=/var/lib/dynatrace/oneagent
ENV AGENT_ZIP=$OA_INSTALL_DIR/OneAgent.zip
ENV OA_CONF_FILE=$OA_INSTALL_DIR/agent/conf/standalone.conf

# Prepare OTel config
ENV OTEL_RESOURCE_ATTRIBUTES="service.name=java-quickstart,service.version=1.1.0"
ENV OTEL_METRICS_EXPORTER=none
ENV OTEL_EXPORTER_OTLP_TRACES_PROTOCOL=http/protobuf

COPY scripts/*.sh /opt/
COPY scripts/standalone.conf /opt/
COPY scripts/*.properties /opt/
COPY scripts/*.json /opt/
RUN chmod +x /opt/*.sh

# install unzip and curl
RUN set -eux; \
        if command -v apk >/dev/null 2>&1; then \
          apk add --no-cache --update \
            zip curl jq python3 py3-pip py3-virtualenv strace icu-libs icu-data-full libstdc++ gcompat; \
        else \
          apt-get update; \
          apt-get install -y --no-install-recommends \
            zip unzip vim jq xxd python3 python3-pip python3-venv strace libc-bin locales locales-all; \
          rm -rf /var/lib/apt/lists/*; \
          sed -i 's/^# *en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen; \
          locale-gen; \
          update-locale LANG=en_US.UTF-8 LC_ALL=en_US.UTF-8; \
      # install otel bash
      wget -O - https://raw.githubusercontent.com/plengauer/opentelemetry-shell/main/INSTALL.sh | sh -E; \
    fi

# create directory for the application
RUN mkdir -p /opt/app

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV LC_CTYPE=C.UTF-8
ENV LANGUAGE=en_US:en
ENV JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
ENV JVM_XMX=512m

# Prepare OA. Download Java Agent
# oneAgent.sh will set tenant-specific settings on deploy
RUN . /opt/set_bash_instrumentation.sh && \
    if [ ${AGENTS_PRELOAD} = true ]; then \
      if [ -n "${DT_ENV_URL+x}" ] && [ -n "${DT_TOKEN+x}" ]; then \
          # check if the token is base64-ed. If yes, unbase
          if [ "$(echo $DT_TOKEN | cut -c 1-7)" != "dt0c01." ]; then \
            DT_TOKEN=$(echo "$DT_TOKEN" | base64 -d); \
            export DT_TOKEN; \
          fi; \
          /opt/get_otel.sh ; \
          /opt/get_oa.sh ; \
      fi; \
    fi
ENV AGENTS_PRELOAD=$AGENTS_PRELOAD

ENTRYPOINT export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -Xmx${JVM_XMX}" && \
           export OTEL_SERVICE_NAME="$SERVICE_FULL_NAME" && \
           . /opt/set_bash_instrumentation.sh && \
           /opt/deployment_event.sh && \
           if [ -z ${AGENT+x} ]; then \
                java -Dotel.service.name=$SERVICE_FULL_NAME -jar /opt/app/app.jar; \
           elif [ $AGENT = $ONE_AGENT ] && [ -n "${DT_ENV_URL+x}" ] && [ -n "${DT_TOKEN+x}" ]; then \
                if [ $AGENTS_PRELOAD = false ]; then \
                    . /opt/get_oa.sh; \
                    . /opt/config_oa.sh; \
                else \
                    [ -f /opt/config_oa.sh ] && { /opt/config_oa.sh; rm /opt/config_oa.sh; } || echo "config_oa.sh gone"; \
                fi; \
                exec java -Dotel.service.name=$SERVICE_FULL_NAME -jar -agentpath:/var/lib/dynatrace/oneagent/agent/lib64/liboneagentloader.so -Xshare:off /opt/app/app.jar -nofork; \
           elif [ $AGENT = $OTEL_AGENT ] && [ -n "${DT_ENV_URL+x}" ] && [ -n "${DT_TOKEN+x}" ]; then \
                if [ $AGENTS_PRELOAD = false ]; then . /opt/get_otel.sh; fi; \
                . /opt/config_otel.sh; \
                exec java -javaagent:/opt/opentelemetry-javaagent.jar -Dotel.service.name=$SERVICE_FULL_NAME -jar /opt/app/app.jar; \
           else \
                java -Dotel.service.name=$SERVICE_FULL_NAME -jar /opt/app/app.jar; \
           fi

EXPOSE 8080
EXPOSE 5005
