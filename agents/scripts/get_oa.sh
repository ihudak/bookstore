# clear the OA directory
rm -rf /var/lib/dynatrace/oneagent && mkdir -p /var/lib/dynatrace/oneagent

# Set URL and creds to download OA
## Workaround for the tailing slash in the URL
export TENANT_URL=`echo $TENANT_URL | sed -E 's/[ '\$'/'']+\$//'`
#
export AGENT_ZIP=/var/lib/dynatrace/oneagent/OneAgent.zip

# MINSIZE is 5 MB
export MINSIZE=500000

export PLATFORM=`uname -p`
if [ $PLATFORM = "arm64" ] || [ $PLATFORM = "aarch64" ]; then
  export PLATFORM="arm";
elif [ $PLATFORM = "x86_64" ]; then
  export PLATFORM="x86";
fi

echo "Downloading the latest OneAgent..."
# Download OA (Java Agent only)
curl --request GET -sL \
--url "$TENANT_URL/api/v1/deployment/installer/agent/unix/paas/latest?flavor=default&arch=$PLATFORM&bitness=64&include=java&skipMetadata=true" \
--header 'accept: */*' \
--header "Authorization: Api-Token $OA_TOKEN" \
--output "$AGENT_ZIP"

export FILESIZE=$(stat -c%s "$AGENT_ZIP")
if [ $FILESIZE -lt $MINSIZE ]; then
  echo "OneAgent.zip is too small. Please check it for errors";
  cat /var/lib/dynatrace/oneagent/OneAgent.zip;
  exit 1;
else
  echo "OneAgent.zip download is ok";
  stat -c%s "$AGENT_ZIP";
fi

# Unzip OA
cd /var/lib/dynatrace/oneagent && unzip $AGENT_ZIP && rm $AGENT_ZIP

echo "Getting OneAgent configuration..."
# Configure OA
# create a template of the config file
export OA_CONF_FILE=/var/lib/dynatrace/oneagent/agent/conf/standalone.conf
echo "tenant tenant_id"                             >> $OA_CONF_FILE
echo "tenantToken tenant_token"                     >> $OA_CONF_FILE
echo "serverAddress {tenant_url:443/communication}" >> $OA_CONF_FILE
echo ""                                             >> $OA_CONF_FILE

# Get OA-ingest token
export TENANT_TOKEN=$(curl -X GET $TENANT_URL/api/v1/deployment/installer/agent/connectioninfo \
  -H "accept: application/json" \
  -H "Authorization: Api-Token $OA_TOKEN" | \
grep tenantToken | \
sed s/\ \ \"tenantToken\"\ :\ \"// | sed s/\",//)

# Set OA data ingest endpoint and creds in the OA config file
sed -i s/tenant_id/$TENANT_ID/g $OA_CONF_FILE
sed -i "s|tenant_url|$TENANT_URL|g" $OA_CONF_FILE
sed -i s/tenant_token/$TENANT_TOKEN/g $OA_CONF_FILE

# Don't forget to make this file executable
#chmod +x /opt/oneAgent.sh
