#!/bin/bash
## Workaround for the tailing slash in the URL
export TENANT_URL=`echo $TENANT_URL | sed -E 's/[ '\$'/'']+\$//'`

echo "getting new OTel Java agent"
rm -f /opt/opentelemetry-javaagent.jar && \
wget -O /opt/opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
echo "OTel load done"
## Set Otel env variables that come from the monitoring tenant
export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=$TENANT_URL/api/v2/otlp/v1/traces
export OTEL_EXPORTER_OTLP_TRACES_HEADERS=Authorization="Api-Token $OTEL_TOKEN"
