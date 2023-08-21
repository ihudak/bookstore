# create directories for the application and OneAgent
mkdir -p /opt/app && mkdir -p  /var/lib/dynatrace/oneagent && \
# install unzip (OA will be downloaded as a zip-archive)
apt-get update \
  && apt-get install -y unzip \
  && rm -rf /var/lib/apt/lists/* &&

# Set URL and creds to download OA
export DT_AGENT_DOWNLOAD_TENANT_URL=`cat /opt/env | grep DT_AGENT_DOWNLOAD_TENANT_URL | sed s/DT_AGENT_DOWNLOAD_TENANT_URL=//`
export DT_AGENT_DOWNLOAD_TOKEN=`cat /opt/env | grep DT_AGENT_DOWNLOAD_TOKEN | sed s/DT_AGENT_DOWNLOAD_TOKEN=//`

# Download OA (Java Agent only)
curl --request GET -sL \
--url "$DT_AGENT_DOWNLOAD_TENANT_URL/api/v1/deployment/installer/agent/unix/paas/latest?flavor=default&arch=x86&bitness=64&include=java&skipMetadata=true" \
--header 'accept: */*' \
--header "Authorization: Api-Token $DT_AGENT_DOWNLOAD_TOKEN" \
--output '/var/lib/dynatrace/oneagent/OneAgent.zip'

# Remove file with the confidential data
rm /opt/env

# Unzip OA
cd /var/lib/dynatrace/oneagent && unzip /var/lib/dynatrace/oneagent/OneAgent.zip && rm /var/lib/dynatrace/oneagent/OneAgent.zip

# Configure OA
# create a template of the config file
export OA_CONF_FILE=/var/lib/dynatrace/oneagent/agent/conf/standalone.conf
echo "tenant tenant_id"                             >> $OA_CONF_FILE
echo "tenantToken tenant_token"                     >> $OA_CONF_FILE
echo "serverAddress {tenant_url:443/communication}" >> $OA_CONF_FILE

# Get OA-ingest token
curl -X GET \$TENANT_URL/api/v1/deployment/installer/agent/connectioninfo \
  -H \"accept: application/json\" \
  -H \"Authorization: Api-Token \$OA_TOKEN\" | \
grep tenantToken | \
sed s/\ \ \"tenantToken\"\ :\ \"// | sed s/\",// > /opt/tenant.token
# token is in tenant.token file. Get it into an Env Var
export TENANT_TOKEN=$(cat /opt/tenant.token)
# remove the file with token
rm /opt/tenant.token

# Set OA data ingest endpoint and creds in the OA config file
sed -i s/tenant_id/\$TENANT_ID/g $OA_CONF_FILE
sed -i \"s|tenant_url|\$TENANT_URL|g\" $OA_CONF_FILE
sed -i s/tenant_token/\$TENANT_TOKEN/g $OA_CONF_FILE

# Don't forget to make this file executable
#chmod +x /opt/oneAgent.sh
