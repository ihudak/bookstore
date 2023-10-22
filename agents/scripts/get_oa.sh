#!/bin/sh
# clear the OA directory
export $OA_INSTALL_DIR=/var/lib/dynatrace/oneagent
rm -rf $OA_INSTALL_DIR && mkdir -p $OA_INSTALL_DIR

## Workaround for the tailing slash in the URL
export TENANT_URL=`echo $TENANT_URL | sed -E 's/[ '\$'/'']+\$//'`

export AGENT_ZIP=$OA_INSTALL_DIR/OneAgent.zip

# MINSIZE is 5 MB
export MINSIZE=500000

# define platform param for the Dynatrace API call
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
--header 'accept: application/octet-stream' \
--header "Authorization: Api-Token $OA_TOKEN" \
--output "$AGENT_ZIP"

echo "Checking if OneAgent.zip file is ok"
if [ ! -e "$AGENT_ZIP" ]; then
  echo "OneAgent.zip does not exist." >&2;
  exit 1;
fi

export FILESIZE=$(stat -c%s "$AGENT_ZIP")
if [ $FILESIZE -lt $MINSIZE ]; then
  echo "OneAgent.zip is too small. Please check it for errors" >&2;
  exit 1;
else
  echo "OneAgent.zip download is ok" >&2;
  stat -c%s "$AGENT_ZIP";
fi
echo "OA load done"

# Unzip OA
cd $OA_INSTALL_DIR && unzip $AGENT_ZIP && rm $AGENT_ZIP

echo "Getting OneAgent configuration..."
# create a template of the config file
export OA_CONF_FILE=$OA_INSTALL_DIR/agent/conf/standalone.conf
echo "tenant tenant_id"                             >> $OA_CONF_FILE
echo "tenantToken tenant_token"                     >> $OA_CONF_FILE
echo "serverAddress {tenant_url:443/communication}" >> $OA_CONF_FILE
echo ""                                             >> $OA_CONF_FILE
