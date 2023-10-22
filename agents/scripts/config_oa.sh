#!/bin/sh

log() {
  echo "$1" >&2
}

if [ "$(echo OA_TOKEN | cut -c 1-7)" != "dt0c01." ]; then
  log "Invalid OA token";
  exit 1;
fi

if [ ! -d "$OA_INSTALL_DIR" ]; then
  log "OneAgent is not installed.";
  exit 1;
fi

if [ ! -e "$OA_CONF_FILE" ]; then
  log "OneAgent config file does not exist.";
  exit 1;
fi

log "Getting OneAgent configuration..."
## Workaround for the tailing slash in the URL
TENANT_URL=$(echo "$TENANT_URL" | sed -E 's/[ '\$'/'']+\$//')
export TENANT_URL
# Get OA-ingest token
TENANT_TOKEN=$(curl -X GET "$TENANT_URL"/api/v1/deployment/installer/agent/connectioninfo \
  -H "accept: application/json" \
  -H "Authorization: Api-Token $OA_TOKEN" | \
grep tenantToken | \
sed s/\ \ \"tenantToken\"\ :\ \"// | sed s/\",//)
export TENANT_TOKEN

# Set OA data ingest endpoint and creds in the OA config file
sed -i s/tenant_id/"$TENANT_ID"/g "$OA_CONF_FILE"
sed -i "s|tenant_url|$TENANT_URL|g" "$OA_CONF_FILE"
sed -i s/tenant_token/"$TENANT_TOKEN"/g "$OA_CONF_FILE"

log "OA config done"
