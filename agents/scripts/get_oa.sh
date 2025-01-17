#!/bin/sh

# clear the OA directory
rm -rf $OA_INSTALL_DIR && mkdir -p $OA_INSTALL_DIR

# MINSIZE is 5 MB
export MINSIZE=500000

# define platform param for the Dynatrace API call
PLATFORM=$(uname -p);
export PLATFORM;
if [ "$PLATFORM" = "arm" ] || [ "$PLATFORM" = "arm64" ] || [ "$PLATFORM" = "aarch64" ]; then
  export PLATFORM="arm";
elif [ $PLATFORM = "x86" ] || [ $PLATFORM = "x86_64" ] || [ $PLATFORM = "x64" ]; then
  export PLATFORM="x86";
else
  export PLATFORM="x86";
fi

echo "Downloading the latest OneAgent..."
# Download OA (Java Agent only)
curl --request GET -sL \
--url "$TENANT_URL/api/v1/deployment/installer/agent/unix/paas/latest?flavor=musl&arch=arm&bitness=64&include=java&skipMetadata=true" \
--header 'accept: application/octet-stream' \
--header "Authorization: Api-Token $OA_TOKEN" \
--output "$AGENT_ZIP"

echo "Checking if OneAgent.zip file is ok"
if [ ! -e "$AGENT_ZIP" ]; then
  echo "OneAgent.zip does not exist.";
  exit 1;
fi

FILESIZE=$(stat -c%s "$AGENT_ZIP")
export FILESIZE
if [ $FILESIZE -lt $MINSIZE ]; then
  echo "$AGENT_ZIP is too small. Please check it for errors";
  exit 1;
else
  echo "$AGENT_ZIP download is ok";
  stat -c%s "$AGENT_ZIP";
fi

# Unzip OA
cd "$OA_INSTALL_DIR" && unzip "$AGENT_ZIP" && rm "$AGENT_ZIP"

# copy the template of the config file
cp /opt/standalone.conf "$OA_INSTALL_DIR"/agent/conf/

echo "OA load done"
