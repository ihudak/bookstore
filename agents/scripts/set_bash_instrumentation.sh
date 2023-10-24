#!/bin/sh

log() {
  echo "$1" >&2
}

## Set Otel env variables that come from the monitoring tenant
if [ -z ${TENANT_ID_SHELL+x} ] || [ -z ${TENANT_URL_SHELL+x} ] || [ -z ${OTEL_TOKEN_SHELL+x} ]; then
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
  TENANT_URL_SHELL=$(echo "$TENANT_URL_SHELL" | sed -E 's/[ '\$'/'']+\$//');
  export TENANT_URL_SHELL;

  # check if the token is base64-ed. If yes, unbase
  if [ "$(echo OTEL_TOKEN_SHELL | cut -c 1-7)" != "dt0c01." ]; then
    OTEL_TOKEN_SHELL=$(echo "$OTEL_TOKEN_SHELL" | base64 -d);
    export OTEL_TOKEN_SHELL;
  fi

  # set endpoints
  export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=$TENANT_URL_SHELL/api/v2/otlp/v1/traces;
  export OTEL_EXPORTER_OTLP_METRICS_ENDPOINT=$TENANT_URL_SHELL/api/v2/otlp/v1/metrics;
  export OTEL_EXPORTER_OTLP_LOGS_ENDPOINT=$TENANT_URL_SHELL/api/v2/otlp/v1/logs;

  # set authentication
  OTEL_EXPORTER_OTLP_TRACES_HEADERS=Authorization=$(echo "Api-Token $OTEL_TOKEN_SHELL" | jq -Rr @uri);
  export OTEL_EXPORTER_OTLP_TRACES_HEADERS;
  OTEL_EXPORTER_OTLP_METRICS_HEADERS=Authorization=$(echo "Api-Token $OTEL_TOKEN_SHELL" | jq -Rr @uri);
  export OTEL_EXPORTER_OTLP_METRICS_HEADERS;
  OTEL_EXPORTER_OTLP_LOGS_HEADERS=Authorization=$(echo "Api-Token $OTEL_TOKEN_SHELL" | jq -Rr @uri);
  export OTEL_EXPORTER_OTLP_LOGS_HEADERS;

  # Configure service
  export OTEL_SERVICE_NAME="BookStoreAppDocker $SVC_NAME";
  # turn on instrumenting
  . /usr/bin/opentelemetry_shell.sh;
  otel_instrument echo;
  otel_instrument log;
  # the first echo will give trace a name
  log "BookStoreAppDocker: $SVC_NAME:$(uname -p)"
  log "Otel-bash instrumentation complete"
fi
