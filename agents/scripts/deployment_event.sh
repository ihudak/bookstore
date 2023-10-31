#!/bin/bash

export ENTITIES_URL="$TENANT_URL/api/v2/entities?pageSize=500&entitySelector=type(SERVICE),entityName($SVC_NAME)"
export EVENTS_URL="$TENANT_URL/api/v2/events/ingest"

if [ "$(echo $ENTITIES_TOKEN | cut -c 1-7)" != "dt0c01." ] || [ "$(echo $EVENTS_TOKEN | cut -c 1-7)" != "dt0c01." ]; then
  echo Tokens for the deployment event are not ready
  exit 0; # tokens are not provided
fi

# Get entity id for the deployed service
DT_SERVICE_ID=$(curl --location "$ENTITIES_URL" --header "Authorization: Api-Token $ENTITIES_TOKEN" | jq '.entities[0].entityId' | sed -e s/\"//g)
export DT_SERVICE_ID

# Submit deployment event for the service
EVENT_BODY=$(jq --null-input \
  --arg deploymentVersion "$DT_RELEASE_VERSION" \
  --arg deploymentTitle "Deployed version $DT_RELEASE_VERSION" \
  --arg startTime "$(date +%s%N | cut -b1-11)00" \
  --arg endTime "$(date +%s%N | cut -b1-11)99" \
  --arg service "entityId($DT_SERVICE_ID)" \
  --arg serviceName "$SVC_NAME" \
  '{
     "eventType": "CUSTOM_DEPLOYMENT",
     "title": $deploymentTitle,
     "startTime": $startTime,
     "endTime": $endTime,
     "timeout": 1,
     "entitySelector": $service,
     "properties": {
           "deploymentName":"Bookstore Carts deployment",
           "deploymentVersion":$deploymentVersion,
           "deploymentProject":$serviceName
     }
   }')
export EVENT_BODY

curl --location "$EVENTS_URL" \
  --header 'Content-Type: application/json' \
  --header "Authorization: Api-Token $EVENTS_TOKEN" \
  --data "${EVENT_BODY}"

