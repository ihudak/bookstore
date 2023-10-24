#!/bin/sh

echo "getting new OTel Java agent"
rm -f /opt/opentelemetry-javaagent.jar && \
wget -O /opt/opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
echo "OTel load done"
