## Set Otel env variables (Static)
export OTEL_RESOURCE_ATTRIBUTES="service.name=java-quickstart,service.version=1.0.1"
export OTEL_METRICS_EXPORTER=none
export OTEL_EXPORTER_OTLP_TRACES_PROTOCOL=http/protobuf
## Set Otel env variables that come from the monitoring tenant
export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=$TENANT_URL/api/v2/otlp/v1/traces
export OTEL_EXPORTER_OTLP_TRACES_HEADERS=Authorization="Api-Token $OTEL_TOKEN"
wget -O /opt/opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/$OTEL_VER/opentelemetry-javaagent.jar
# Do not forget to make this file executable
###chmod +x /opt/otel.sh
