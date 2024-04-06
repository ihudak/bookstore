#!/bin/sh

echo "Configuring OTELOTELOTELOTELOTELOTEL!!!!"

echo "foo$TENANT_ID_SHELL foo"
echo "foo$TENANT_URL_SHELL foo"
echo "foo$OTEL_OTEL_TOKEN_SHELL foo"

## Set Otel env variables that come from the monitoring tenant
if [ -z ${TENANT_ID_SHELL+x} ] || [ -z ${TENANT_URL_SHELL+x} ] || [ -z ${OTEL_TOKEN_SHELL+x} ]; then
  # Cannot monitor
  export OTEL_SHELL_TRACES_ENABLE=FALSE;
  export OTEL_SHELL_METRICS_ENABLE=FALSE;
  export OTEL_SHELL_LOGS_ENABLE=FALSE;
  echo "can't instrument"
  return 0
fi

# Monitoring is possible
export OTEL_SHELL_TRACES_ENABLE=TRUE
export OTEL_SHELL_METRICS_ENABLE=TRUE
export OTEL_SHELL_LOGS_ENABLE=TRUE

# fix tenant url (remove tailing slash if exists)
TENANT_URL_SHELL=$(echo "$TENANT_URL_SHELL" | sed -E 's/[ '\$'/'']+\$//')
export TENANT_URL_SHELL

# check if the token is base64-ed. If yes, unbase
if [ "$(echo $OTEL_TOKEN_SHELL | cut -c 1-7)" != "dt0c01." ]; then
  OTEL_TOKEN_SHELL=$(echo "$OTEL_TOKEN_SHELL" | base64 -d);
fi
export OTEL_TOKEN_SHELL

# set endpoints
export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=$TENANT_URL_SHELL/api/v2/otlp/v1/traces
export OTEL_EXPORTER_OTLP_METRICS_ENDPOINT=$TENANT_URL_SHELL/api/v2/otlp/v1/metrics
export OTEL_EXPORTER_OTLP_LOGS_ENDPOINT=$TENANT_URL_SHELL/api/v2/otlp/v1/logs

# set authentication
OTEL_EXPORTER_OTLP_TRACES_HEADERS=Authorization=$(echo "Api-Token $OTEL_TOKEN_SHELL" | jq -Rr @uri)
export OTEL_EXPORTER_OTLP_TRACES_HEADERS
OTEL_EXPORTER_OTLP_METRICS_HEADERS=Authorization=$(echo "Api-Token $OTEL_TOKEN_SHELL" | jq -Rr @uri)
export OTEL_EXPORTER_OTLP_METRICS_HEADERS
OTEL_EXPORTER_OTLP_LOGS_HEADERS=Authorization=$(echo "Api-Token $OTEL_TOKEN_SHELL" | jq -Rr @uri)
export OTEL_EXPORTER_OTLP_LOGS_HEADERS

# set service name for the deployment procedure
export OTEL_SERVICE_NAME="BookStoreAppDocker $SERVICE_FULL_NAME"

# Configure service
# turn on instrumenting
echo "Stating OTELOTELOTELOTELOTELOTEL!!!"
. /usr/bin/opentelemetry_shell.sh
otel_instrument echo
# the first echo will give trace a name
echo "Otel-bash instrumentation complete"
echo "BookStoreAppDocker: $SERVICE_FULL_NAME:$(uname -p)"
