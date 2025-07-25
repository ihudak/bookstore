#!/bin/sh

if [ $AGENT = $ONE_AGENT ] && [ -n "${DT_ENV_URL+x}" ] && [ -n "${DT_TOKEN+x}" ]; then
  . /opt/get_oa.sh;
  . /opt/config_oa.sh;
  exec java -jar -agentpath:/var/lib/dynatrace/oneagent/agent/lib64/liboneagentloader.so -Xshare:off /opt/app/app.jar -nofork;
elif [ $AGENT = $OTEL_AGENT ] && [ -n "${DT_ENV_URL+x}" ] && [ -n "${DT_TOKEN+x}" ]; then
  . /opt/get_otel.sh;
  exec java -javaagent:/opt/opentelemetry-javaagent.jar -Dotel.service.name=$SERVICE_FULL_NAME -jar /opt/app/app.jar;
else
  exec java -jar /opt/app/app.jar;
fi
