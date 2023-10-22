#!/bin/sh
## Workaround for the tailing slash in the URL
TENANT_URL=$(echo $TENANT_URL | sed -E 's/[ '\$'/'']+\$//')
export TENANT_URL
export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=$TENANT_URL/api/v2/otlp/v1/traces
export OTEL_EXPORTER_OTLP_TRACES_HEADERS=Authorization="Api-Token $OTEL_TOKEN"
