#!/bin/sh

log() {
  echo "$1" >&2
}

## Set Otel env variables that come from the monitoring tenant
if [ -z ${DT_ENV_ID+x} ] || [ -z ${DT_ENV_URL+x} ] || [ -z ${DT_TOKEN+x} ]; then
  # Cannot monitor
  export OTEL_SHELL_TRACES_ENABLE=FALSE;
  export OTEL_SHELL_METRICS_ENABLE=FALSE;
  export OTEL_SHELL_LOGS_ENABLE=FALSE;
else
  # Monitoring is possible
  export OTEL_SHELL_TRACES_ENABLE=TRUE;
  export OTEL_SHELL_METRICS_ENABLE=TRUE;
  export OTEL_SHELL_LOGS_ENABLE=TRUE;

  # fix tenant url (remove tailing slash if exists)
  DT_ENV_URL=$(echo "$DT_ENV_URL" | sed -E 's/[ '\$'/'']+\$//');
  export DT_ENV_URL;

  # check if the token is base64-ed. If yes, unbase
  if [ "$(echo $DT_TOKEN | cut -c 1-7)" != "dt0c01." ]; then
    DT_TOKEN=$(echo "$DT_TOKEN" | base64 -d);
    export DT_TOKEN;
  fi

  # set endpoints
  export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=$DT_ENV_URL/api/v2/otlp/v1/traces;
  export OTEL_EXPORTER_OTLP_METRICS_ENDPOINT=$DT_ENV_URL/api/v2/otlp/v1/metrics;
  export OTEL_EXPORTER_OTLP_LOGS_ENDPOINT=$DT_ENV_URL/api/v2/otlp/v1/logs;

  # set authentication
  OTEL_EXPORTER_OTLP_TRACES_HEADERS=Authorization=$(echo "Api-Token $DT_TOKEN" | jq -Rr @uri);
  export OTEL_EXPORTER_OTLP_TRACES_HEADERS;
  OTEL_EXPORTER_OTLP_METRICS_HEADERS=Authorization=$(echo "Api-Token $DT_TOKEN" | jq -Rr @uri);
  export OTEL_EXPORTER_OTLP_METRICS_HEADERS;
  OTEL_EXPORTER_OTLP_LOGS_HEADERS=Authorization=$(echo "Api-Token $DT_TOKEN" | jq -Rr @uri);
  export OTEL_EXPORTER_OTLP_LOGS_HEADERS;

  # Configure service
  export OTEL_SERVICE_NAME="BookStoreAppDocker $SERVICE_FULL_NAME";
  # turn on instrumenting
  . /usr/bin/opentelemetry_shell.sh;
  otel_instrument echo;
  otel_instrument log;
  # the first echo will give trace a name
  log "BookStoreAppDocker: $SERVICE_FULL_NAME:$(uname -p)"
  log "Otel-bash instrumentation complete"
fi
