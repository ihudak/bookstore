#!/bin/sh

EVENTS_URL="$DT_ENV_URL/api/v2/events/ingest"

if [ "$(echo "$DT_TOKEN" | cut -c 1-7)" != "dt0c01." ]; then
  echo "Token for the deployment event is not ready";
  exit 0; # token is not provided
fi

make_and_send_event() {
  entitySelector="$1"
  startTime="$2"
  endTime="$3"
  isPreinstrumented="$4"

  EVENT_BODY=$(jq --null-input \
    --arg deploymentVersion "$DT_RELEASE_VERSION" \
    --arg releaseBuildVersion "$DT_RELEASE_BUILD_VERSION" \
    --arg deploymentTitle "Deployed version $DT_RELEASE_VERSION" \
    --arg startTime "$startTime" \
    --arg endTime "$endTime" \
    --arg entitySelector "$entitySelector" \
    --arg serviceName "$SERVICE_FULL_NAME" \
    --arg instrumentation "$AGENT" \
    --arg isPreinstrumented "$isPreinstrumented" \
    --arg deploymentName "Bookstore $SERVICE_FULL_NAME deployment" \
    '{
       "eventType": "CUSTOM_DEPLOYMENT",
       "title": $deploymentTitle,
       "startTime": $startTime,
       "endTime": $endTime,
       "timeout": 7,
       "entitySelector": $entitySelector,
       "properties": {
             "deploymentName":$deploymentName,
             "deploymentVersion":$deploymentVersion,
             "releaseBuildVersion":$releaseBuildVersion,
             "instrumentation":$instrumentation,
             "isPreinstrumented":$isPreinstrumented,
             "deploymentProject":$serviceName
       }
     }')

  curl -X 'POST' "$EVENTS_URL" \
    -H 'accept: application/json; charset=utf-8' \
    -H 'Content-Type: application/json; charset=utf-8' \
    -H "Authorization: Api-Token $DT_TOKEN" \
    -d "$EVENT_BODY"
}


if [ -z ${AGENT+x} ]; then
      echo "Not monitored case";
      exit 0;
elif [ $AGENT = $ONE_AGENT ] && [ -n "${DT_ENV_URL+x}" ] && [ -n "${DT_TOKEN+x}" ]; then
      echo "OneAgent case";

      # event for host
      make_and_send_event "type(HOST),entityName.startsWith($DT_HOST_NAME_PREFIX),state(RUNNING)" "$(date +%s%N | cut -b1-11)00" "$(date +%s%N | cut -b1-11)99" "$AGENTS_PRELOAD";

      # event for service
      make_and_send_event "type(SERVICE),entityName.startsWith($DT_SERVICE_NAME)" "$(date +%s%N | cut -b1-11)00" "$(date +%s%N | cut -b1-11)99" "$AGENTS_PRELOAD";
elif [ $AGENT = $OTEL_AGENT ] && [ -n "${DT_ENV_URL+x}" ] && [ -n "${DT_TOKEN+x}" ]; then
      echo "OpenTelemetry case";
      # event for service
      make_and_send_event "type(SERVICE),entityName.startsWith($SERVICE_FULL_NAME)" "$(date +%s%N | cut -b1-11)00" "$(date +%s%N | cut -b1-11)99" "true";
else
      echo "Not configured for monitoring";
      exit 0;
fi
