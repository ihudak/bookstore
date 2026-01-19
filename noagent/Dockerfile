ARG VER=21
ARG FLAVOR=noble

FROM eclipse-temurin:${VER}-jdk-${FLAVOR}
MAINTAINER dynatrace.com
ARG FLAVOR=noble

# install unzip and curl
RUN if [ "$FLAVOR" = "alpine" ]; then \
      apk add --no-cache --update zip curl jq python3 py3-pip py3-virtualenv strace icu-libs icu-data-full libiconv libstdc++ gcompat; \
    else \
      apt-get update && \
      apt-get install -y zip unzip vim jq python3 python3-pip python3-venv strace libc-bin locales locales-all && \
      rm -rf /var/lib/apt/lists/* ; \
    fi

#COPY --from=pae32231.dev.dynatracelabs.com/linux/oneagent-codemodules-musl:java / /
#ENV LD_PRELOAD="/opt/dynatrace/oneagent/agent/lib64/liboneagentproc.so"
RUN mkdir -p /opt/app
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8
ENV JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -Dfile.encoding=UTF-8"
ENV DOCKER_VER="1.7.3"

ENTRYPOINT export OTEL_SERVICE_NAME="$SERVICE_FULL_NAME" && \
           java -Dotel.service.name=$SERVICE_FULL_NAME -jar /opt/app/app.jar

EXPOSE 8080
EXPOSE 5005
