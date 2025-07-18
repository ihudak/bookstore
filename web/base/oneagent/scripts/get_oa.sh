#!/bin/sh

# MINSIZE is 5 MB
export MINSIZE=500000
export AGENT_INSTALLER=/opt/Dynatrace-OneAgent-Linux.sh

PLATFORM=$(uname -p)
export PLATFORM
if [ "$PLATFORM" = "arm" ] || [ "$PLATFORM" = "arm64" ] || [ "$PLATFORM" = "aarch64" ]; then
  export PLATFORM="arm";
elif [ $PLATFORM = "x86" ] || [ $PLATFORM = "x86_64" ]; then
  export PLATFORM="x86";
else
  export PLATFORM="x86";
fi

wget -O "$AGENT_INSTALLER" "$DT_ENV_URL/api/v1/deployment/installer/agent/unix/default/latest?arch=$PLATFORM" --header="Authorization: Api-Token $DT_TOKEN"

FILESIZE=$(stat -c%s "$AGENT_INSTALLER")
export FILESIZE
if [ $FILESIZE -lt $MINSIZE ]; then
  echo "$AGENT_INSTALLER is too small. Please check it for errors";
  exit 1;
else
  echo "$AGENT_INSTALLER download is ok";
  stat -c%s "$AGENT_INSTALLER";
fi

/bin/sh $AGENT_INSTALLER --set-monitoring-mode=fullstack --set-app-log-content-access=true
