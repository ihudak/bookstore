# create directories for the application and OneAgent
mkdir -p /opt/app && mkdir -p  /var/lib/dynatrace/oneagent && \
# install unzip (OA will be downloaded as a zip-archive)
apt-get update \
  && apt-get install -y unzip \
  && rm -rf /var/lib/apt/lists/*

# Set URL and creds to download OA
## Workaround for the tailing slash in the URL
export TENANT_URL=`echo $TENANT_URL | sed -E 's/[ '\$'/'']+\$//'`

export PLATFORM=`uname -p`
if [ $PLATFORM = "arm64" ] || [ $PLATFORM = "aarch64" ]; then
  export PLATFORM="arm";
elif [ $PLATFORM = "x86_64" ]; then
  export PLATFORM="x86";
fi


# Download OA (Java Agent only)
curl --request GET -sL \
--url "$TENANT_URL/api/v1/deployment/installer/agent/unix/paas/latest?flavor=default&arch=$PLATFORM&bitness=64&include=java&skipMetadata=true" \
--header 'accept: */*' \
--header "Authorization: Api-Token $OA_TOKEN" \
--output '/var/lib/dynatrace/oneagent/OneAgent.zip'

# Unzip OA
cd /var/lib/dynatrace/oneagent && unzip /var/lib/dynatrace/oneagent/OneAgent.zip && rm /var/lib/dynatrace/oneagent/OneAgent.zip

# Configure OA
# create a template of the config file
export OA_CONF_FILE=/var/lib/dynatrace/oneagent/agent/conf/standalone.conf

# Get OA-ingest token
export TENANT_TOKEN=$(curl -X GET \$TENANT_URL/api/v1/deployment/installer/agent/connectioninfo \
  -H \"accept: application/json\" \
  -H \"Authorization: Api-Token \$OA_TOKEN\" | \
grep tenantToken | \
sed s/\ \ \"tenantToken\"\ :\ \"// | sed s/\",//)

# Set OA data ingest endpoint and creds in the OA config file
sed -i s/tenant_id/\$TENANT_ID/g $OA_CONF_FILE
sed -i \"s|tenant_url|\$TENANT_URL|g\" $OA_CONF_FILE
sed -i s/tenant_token/\$TENANT_TOKEN/g $OA_CONF_FILE

# Don't forget to make this file executable
#chmod +x /opt/oneAgent.sh
