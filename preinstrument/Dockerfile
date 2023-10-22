FROM eclipse-temurin:17-focal
MAINTAINER dynatrace.com

ARG PLATFORM=x86
ARG OTEL_SERVICE_NAME=BookStoreDeploy
ENV ONE_AGENT="oneAgent"
ENV OTEL_AGENT="otelAgent"

ARG TENANT_ID
ARG TENANT_LAYER=dev
ARG TENANT_TOKEN
ENV TENANT_URL_SHELL="https://$TENANT_ID.$TENANT_LAYER.dynatracelabs.com"
ENV OTEL_TOKEN_SHELL=$TENANT_TOKEN

ENV DOCKER_VER=1.0.1

# Prepare OTel config
ENV OTEL_RESOURCE_ATTRIBUTES="service.name=java-quickstart,service.version=1.0.1"
ENV OTEL_METRICS_EXPORTER=none
ENV OTEL_EXPORTER_OTLP_TRACES_PROTOCOL=http/protobuf

COPY ../agents/scripts/*.sh /opt/
RUN chmod +x /opt/*.sh

# install unzip and otel bash agent
RUN apt-get update && \
    apt-get install -y zip unzip vim jq && \
    wget -O - https://raw.githubusercontent.com/plengauer/opentelemetry-bash/main/INSTALL.sh | sh -E && \
    rm -rf /var/lib/apt/lists/*

# create directory for the application
RUN mkdir -p /opt/app

ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005


RUN echo "export TENANT_URL=\`echo \$TENANT_URL | sed -E 's/[ '\$'/'']+\$//'\`"              >> /opt/otel.sh && \
    echo "export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=\$TENANT_URL/api/v2/otlp/v1/traces"      >> /opt/otel.sh && \
    echo "export OTEL_EXPORTER_OTLP_TRACES_HEADERS=Authorization=\"Api-Token \$OTEL_TOKEN\"" >> /opt/otel.sh && \
    chmod +x /opt/otel.sh && \
    wget -O /opt/opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# Prepare OA. Download Java Agent
# oneAgent.sh will set tenant-specific settings on deploy
ARG ENV_FILE=env
COPY ${ENV_FILE} /opt/env
RUN mkdir -p  /var/lib/dynatrace/oneagent && \
    export DT_AGENT_DOWNLOAD_TENANT_URL=`cat /opt/env | grep DT_AGENT_DOWNLOAD_TENANT_URL | sed s/DT_AGENT_DOWNLOAD_TENANT_URL=// | sed -E 's/[ '$'/'']+$//'` && \
    export DT_AGENT_DOWNLOAD_TOKEN=`cat /opt/env | grep DT_AGENT_DOWNLOAD_TOKEN | sed s/DT_AGENT_DOWNLOAD_TOKEN=// | base64 -d` &&  \
    curl -v --request GET -sL \
    --url "$DT_AGENT_DOWNLOAD_TENANT_URL/api/v1/deployment/installer/agent/unix/paas/latest?flavor=default&arch=$PLATFORM&bitness=64&include=java&skipMetadata=true" \
    --header 'accept: */*' \
    --header "Authorization: Api-Token $DT_AGENT_DOWNLOAD_TOKEN" \
    --output '/var/lib/dynatrace/oneagent/OneAgent.zip' && \
    rm /opt/env && \
    cd /var/lib/dynatrace/oneagent && unzip /var/lib/dynatrace/oneagent/OneAgent.zip && rm /var/lib/dynatrace/oneagent/OneAgent.zip && \
    export OA_CONF_FILE=/var/lib/dynatrace/oneagent/agent/conf/standalone.conf && \
    export TENANT_URL=`echo $TENANT_URL | sed -E 's/[ '$'/'']+$//'` && \
    echo "tenant tenant_id"                             >> $OA_CONF_FILE && \
    echo "tenantToken tenant_token"                     >> $OA_CONF_FILE && \
    echo "serverAddress {tenant_url:443/communication}" >> $OA_CONF_FILE && \
    echo ""                                             >> $OA_CONF_FILE && \
    echo "export TENANT_URL=\`echo \$TENANT_URL | sed -E 's/[ '\$'/'']+\$//'\`"             >> /opt/oneAgent.sh && \
    echo "curl -X GET \$TENANT_URL/api/v1/deployment/installer/agent/connectioninfo \\"     >> /opt/oneAgent.sh && \
    echo "  -H \"accept: application/json\" \\"                                             >> /opt/oneAgent.sh && \
    echo "  -H \"Authorization: Api-Token \$OA_TOKEN\" | \\"                                >> /opt/oneAgent.sh && \
    echo "grep tenantToken | \\"                                                            >> /opt/oneAgent.sh && \
    echo "sed s/\ \\ \\\"tenantToken\\\"\\ :\\ \\\"// | sed s/\\\",// > /opt/tenant.token"  >> /opt/oneAgent.sh && \
    echo "export TENANT_TOKEN=\`cat /opt/tenant.token\`"                                    >> /opt/oneAgent.sh && \
    echo "rm /opt/tenant.token"                                                             >> /opt/oneAgent.sh && \
    echo "sed -i s/tenant_id/\$TENANT_ID/g $OA_CONF_FILE"                                   >> /opt/oneAgent.sh && \
    echo "sed -i \"s|tenant_url|\$TENANT_URL|g\" $OA_CONF_FILE"                             >> /opt/oneAgent.sh && \
    echo "sed -i s/tenant_token/\$TENANT_TOKEN/g $OA_CONF_FILE"                             >> /opt/oneAgent.sh && \
    echo ""                                                                                 >> /opt/oneAgent.sh && \
    chmod +x /opt/oneAgent.sh




ENTRYPOINT if [ -z ${AGENT+x} ]; then \
                java -jar /opt/app/app.jar; \
           elif [ $AGENT = $ONE_AGENT ]; then \
                [ -f /opt/oneAgent.sh ] && { /opt/oneAgent.sh; rm /opt/oneAgent.sh; } || echo "oneAgent.sh gone"; \
                java -jar -agentpath:/var/lib/dynatrace/oneagent/agent/lib64/liboneagentloader.so -Xshare:off /opt/app/app.jar -nofork; \
           elif [ $AGENT = $OTEL_AGENT ]; then \
                . /opt/otel.sh; \
                java -javaagent:/opt/opentelemetry-javaagent.jar -Dotel.service.name=$SVC_NAME -jar /opt/app/app.jar; \
           else \
                java -jar /opt/app/app.jar; \
           fi

EXPOSE 8080
EXPOSE 5005
