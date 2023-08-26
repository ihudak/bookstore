#!/bin/bash

display_usage() {
  echo "Bad/no argument(s) supplied";
      echo "Sets agent instrumentation and architecture in the deployment yaml files"
      echo "Usage:";
      echo "   ./preset_deployment.sh -agents  -arm|-x64 {-minimizePorts} # takes docker image with OA and OTel agents";
      echo "   ./preset_deployment.sh -noagent -arm|-x64 {-minimizePorts} # takes docker image with no agents embedded";
      echo "   ./preset_deployment.sh -reset              # resets yamls";
      echo "                   -minimizePorts makes most k8s services ClusterIP to reduce allocating public IP address"
      echo;
      echo "Please supply at least either -agents, -noagent or -reset";
      echo "      optionally specify platform as -arm or -x64";
      echo "      default platform is x64";
	  echo;
      exit 1;
}

reset_settings() {
  # reset settings
  sed -i.bak "s/-noagent-x64:latest/-{AGENT}-{ARCH}:latest/g" *.yaml
  sed -i.bak "s/-agents-x64:latest/-{AGENT}-{ARCH}:latest/g" *.yaml
  sed -i.bak "s/-noagent-arm64:latest/-{AGENT}-{ARCH}:latest/g" *.yaml
  sed -i.bak "s/-agents-arm64:latest/-{AGENT}-{ARCH}:latest/g" *.yaml
  sed -i.bak "s/-x64:latest/-{ARCH}:latest/g" databases.yaml
  sed -i.bak "s/-arm64:latest/-{ARCH}:latest/g" databases.yaml
  sed -i.bak "s/bookstore-webapp-x64:latest/bookstore-webapp-{ARCH}:latest/g" bookstore.yaml
  sed -i.bak "s/bookstore-webapp-arm64:latest/bookstore-webapp-{ARCH}:latest/g" bookstore.yaml
  # setting LoadBalancer back
  sed -i.bak "s/type: ClusterIP # LoadBalancer/type: LoadBalancer # ClusterIP/g" *.yaml
  sed -i.bak -E "/^#.+nodePort: / s/^#//g" *.yaml
  rm *.bak
}

if [ $# -lt 1 ]; then
  display_usage;
elif [ $1 != "-agents" ] && [ $1 != "-noagent" ] && [ $1 != "-reset" ]; then
  display_usage;
elif [ $1 = "-reset" ]; then
  reset_settings
  echo "Reset Completed"
  exit 0
fi

if [ $1 = "-agents" ]; then AGENT=agents; else AGENT=noagent; fi

if [ $# -gt 1 ] && [ $2 = "-arm" ]; then PLATFORM="arm64"; else PLATFORM="x64"; fi

reset_settings
sed -i.bak "s/-{AGENT}-{ARCH}:latest/-$AGENT-$PLATFORM:latest/g" *.yaml
sed -i.bak "s/-{ARCH}:latest/-$PLATFORM:latest/g" databases.yaml
sed -i.bak "s/bookstore-webapp-{ARCH}:latest/bookstore-webapp-$PLATFORM:latest/g" bookstore.yaml

# set ClusterIP and comment nodePorts
if ([ $# -gt 2 ] && [ $3 = "-minimizePorts" ]) || ([ $# -gt 1 ] && [ $2 = "-minimizePorts" ]); then
    sed -i.bak "s/type: LoadBalancer # ClusterIP/type: ClusterIP # LoadBalancer/g" *.yaml
    sed -i.bak -E "/^      nodePort: / s/^#*/#/g" *.yaml
fi

rm *.bak
echo "Preparation done: $AGENT $PLATFORM"
