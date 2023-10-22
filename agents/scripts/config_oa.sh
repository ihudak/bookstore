#!/bin/sh

if [ "$(echo OA_TOKEN | cut -c 1-7)" != "dt0c01." ]; then
  echo "Invalid OA token" >&2;
  exit 1;
fi

if [ ! -d "$OA_INSTALL_DIR" ]; then
  echo "OneAgent is not installed." >&2;
  exit 1;
fi

if [ ! -e "$OA_CONF_FILE" ]; then
  echo "OneAgent config file does not exist." >&2;
  exit 1;
fi

echo "Getting OneAgent configuration..."
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

echo "OA config done"
