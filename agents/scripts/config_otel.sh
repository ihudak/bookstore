#!/bin/sh
## Workaround for the tailing slash in the URL
DT_ENV_URL=$(echo $DT_ENV_URL | sed -E 's/[ '\$'/'']+\$//')
export DT_ENV_URL

export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=$DT_ENV_URL/api/v2/otlp/v1/traces
export OTEL_EXPORTER_OTLP_METRICS_ENDPOINT=$DT_ENV_URL/api/v2/otlp/v1/metrics
export OTEL_EXPORTER_OTLP_LOGS_ENDPOINT=$DT_ENV_URL/api/v2/otlp/v1/logs
export OTEL_EXPORTER_OTLP_TRACES_HEADERS=Authorization="Api-Token $DT_TOKEN"
export OTEL_EXPORTER_OTLP_METRICS_HEADERS=Authorization="Api-Token $DT_TOKEN"
export OTEL_EXPORTER_OTLP_LOGS_HEADERS=Authorization="Api-Token $DT_TOKEN"
