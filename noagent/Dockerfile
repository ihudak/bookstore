ARG VER=21
ARG FLAVOR=noble

FROM eclipse-temurin:${VER}-jdk-${FLAVOR}
MAINTAINER dynatrace.com
ARG FLAVOR=noble

# install unzip and curl
RUN set -eux; \
    if command -v apk >/dev/null 2>&1; then \
      apk add --no-cache --update \
        zip curl icu-libs icu-data-full libstdc++ gcompat; \
    else \
      apt-get update; \
      apt-get install -y --no-install-recommends \
        zip unzip vim libc-bin locales locales-all; \
      rm -rf /var/lib/apt/lists/*; \
      sed -i 's/^# *en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen; \
      locale-gen; \
      update-locale LANG=en_US.UTF-8 LC_ALL=en_US.UTF-8; \
    fi

#COPY --from=pae32231.dev.dynatracelabs.com/linux/oneagent-codemodules-musl:java / /
#ENV LD_PRELOAD="/opt/dynatrace/oneagent/agent/lib64/liboneagentproc.so"
RUN mkdir -p /opt/app
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV LC_CTYPE=C.UTF-8
ENV LANGUAGE=en_US:en
ENV JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -XX:MaxRAMPercentage=75"
ENV DOCKER_VER="1.8.7"

ENTRYPOINT export OTEL_SERVICE_NAME="$SERVICE_FULL_NAME" && \
           java -Dotel.service.name=$SERVICE_FULL_NAME -jar /opt/app/app.jar

EXPOSE 8080
EXPOSE 5005
