#!/bin/bash

display_usage() {
  echo "Sets agent instrumentation and architecture in the deployment yaml files"
  echo "Usage:";
  echo "   ${0} -gyes -ax64 -pmin -n bookstore  # takes docker image with OA and OTel agents";
  echo "   ${0} -reset              # resets yamls";
  echo "Flags:";
  echo " -g - agent: yes/no/pre. default = yes - preloads otel and dynatrace java agents. pre - use preinstrumented images";
  echo " -a - architecture: arm/x64. default = x64 - sets architecture";
  echo " -p - ports for k8s services: all/min. default = all - defines whether all k8s should get an external IP. Min - only ingest";
  echo " -n - namespace. default bookstore";
  echo;
  echo "Please supply at least one flag";
  echo;
  exit 0;
}

reset_settings() {
  echo Resetting the config...
  # reset settings
  sed -i.bak "s/-noagent-x64:latest/-{AGENT}-{ARCH}:latest/g" *.yaml
  sed -i.bak "s/-agents-x64:latest/-{AGENT}-{ARCH}:latest/g" *.yaml
  sed -i.bak "s/-preinstrument-x64:latest/-{AGENT}-{ARCH}:latest/g" *.yaml
  sed -i.bak "s/-noagent-arm64:latest/-{AGENT}-{ARCH}:latest/g" *.yaml
  sed -i.bak "s/-agents-arm64:latest/-{AGENT}-{ARCH}:latest/g" *.yaml
  sed -i.bak "s/-preinstrument-arm64:latest/-{AGENT}-{ARCH}:latest/g" *.yaml
  sed -i.bak "s/-x64:latest/-{ARCH}:latest/g" databases.yaml
  sed -i.bak "s/-arm64:latest/-{ARCH}:latest/g" databases.yaml
  sed -i.bak "s/bookstore-webapp-x64:latest/bookstore-webapp-{ARCH}:latest/g" bookstore.yaml
  sed -i.bak "s/bookstore-webapp-arm64:latest/bookstore-webapp-{ARCH}:latest/g" bookstore.yaml
  # setting LoadBalancer back
  sed -i.bak "s/type: ClusterIP # LoadBalancer/type: LoadBalancer # ClusterIP/g" *.yaml
  sed -i.bak -E "/^#.+nodePort: / s/^#//g" *.yaml
  sed -i.bak -E "/name:.*$/ s/name:.*$/name: bookstore/g" namespace.yaml
  sed -i.bak -E "/namespace:.*$/ s/namespace:.*$/namespace: bookstore/g" *.yaml
  rm *.bak
  echo Reset complete
}

get_params() {
  ag="yes"       # default - with agents
  ar="x64"       # default - x64
  po="all"       # default - all k8s have their public IPs
  ns="bookstore" # default namespace
  rs="no"        # no reset by default
  hl="no"        # help
  xx="boo"       # placeholder for x flag (can be used to force x64 arch)
  while getopts hg:a:x:p:n:r flag
  do
      case "${flag}" in
        h) hl="yes";;
        g) ag=${OPTARG};;
        a) ar=${OPTARG};;
        x) xx=${OPTARG};;
        p) po=${OPTARG};;
        n) ns=${OPTARG};;
        r) rs="yes";;
        *) echo unsupported flag ${flag}
      esac
  done

  # fixing most common typos
  if [ $ag != "yes" ] && [ $ag != "y" ] && [ $ag != "pre" ] && [ $ag != "p" ]; then ag="no";    fi
  if [ $ar = "arm" ] || [ $ar = "rm64" ] || [ $ar = "rm" ];                    then ar="arm64"; fi # cover -aarm64, -arm64, -aarm, -arm
  if [ $ar != "arm64" ];                                                       then ar="x64";   fi
  if [ $xx = "86" ] || [ $xx = "64" ];                                         then ar="x64";   fi
  if [ $po != "min" ];                                                         then po="all";   fi
  if [ $ns = "" ];                                                             then ns="bookstore"; fi

  if [ $hl = "yes" ]; then # user wanted help. ignoring everything else
    display_usage;
  elif [ $rs = "yes" ]; then # resetting. ignoring everything else
    reset_settings;
    exit 0;
  else # doing the job. show configuration first
    echo Parameters configured:
    echo " agents:    " $ag;
    echo " arch:      " $ar;
    echo " ports:     " $po;
    echo " namespace: " $ns;
  fi
}

# Main Script
get_params "$@";
reset_settings # before doing anything - reset yamls

# set agents and platform
if [ $ag = "no" ]; then
  ag="noagent";
elif [ $ag = "pre" ] || [ $ag = "p" ]; then
  ag="preinstrument";
else
  ag="agents";
fi
sed -i.bak "s/-{AGENT}-{ARCH}:latest/-$ag-$ar:latest/g" *.yaml
sed -i.bak "s/-{ARCH}:latest/-$ar:latest/g" databases.yaml
sed -i.bak "s/bookstore-webapp-{ARCH}:latest/bookstore-webapp-$ar:latest/g" bookstore.yaml

# set namespace
sed -i.bak -E "/name:.*$/ s/name:.*$/name: ${ns}/g" namespace.yaml
sed -i.bak -E "/namespace:.*$/ s/namespace:.*$/namespace: ${ns}/g" *.yaml

# set ClusterIP and comment nodePorts
if [ $po = "min" ]; then
    sed -i.bak "s/type: LoadBalancer # ClusterIP/type: ClusterIP # LoadBalancer/g" *.yaml
    sed -i.bak -E "/^      nodePort: / s/^#*/#/g" *.yaml
fi

rm *.bak
echo "Preparation done: $ag $ar"
