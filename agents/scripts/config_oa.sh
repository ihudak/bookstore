#!/bin/sh

if [ "$(echo $DT_TOKEN | cut -c 1-7)" != "dt0c01." ]; then
  echo "Invalid OA token";
  exit 1;
fi

if [ ! -d "$OA_INSTALL_DIR" ]; then
  echo "OneAgent is not installed.";
  exit 1;
fi

if [ ! -e "$OA_CONF_FILE" ]; then
  echo "OneAgent config file does not exist.";
  exit 1;
fi

echo "Getting OneAgent configuration..."
## Workaround for the tailing slash in the URL
DT_ENV_URL=$(echo "$DT_ENV_URL" | sed -E 's/[ '\$'/'']+\$//')
export DT_ENV_URL
# Get OA-ingest token
TENANT_TOKEN=$(curl -X GET "$DT_ENV_URL"/api/v1/deployment/installer/agent/connectioninfo \
  -H "accept: application/json" \
  -H "Authorization: Api-Token $DT_TOKEN" | \
grep tenantToken | \
sed s/\ \ \"tenantToken\"\ :\ \"// | sed s/\",//)
export TENANT_TOKEN

# Set OA data ingest endpoint and creds in the OA config file
sed -i s/tenant_id/"$DT_ENV_ID"/g "$OA_CONF_FILE"
sed -i "s|tenant_url|$DT_ENV_URL|g" "$OA_CONF_FILE"
sed -i s/tenant_token/"$TENANT_TOKEN"/g "$OA_CONF_FILE"

echo "OA config done"
