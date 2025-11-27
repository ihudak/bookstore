#!/bin/bash

display_usage() {
  echo "Sets agent instrumentation in the deployment yaml files"
  echo "Usage:";
  echo "   ${0} -gyes -pmin -fubuntu -nbookstore  # installs agents on deploy; minimizes external port usage; bookstore namespace";
  echo "   ${0} -gpre -pall -falpine -nbookstore  # takes agents prebuilt in the docker image; every service will be open (callable from outside of the k8s)";
  echo "   ${0} -r                   # resets yamls";
  echo "Flags:";
  echo " -g - agent: yes/no/pre. default = yes - preloads otel and dynatrace java agents. pre - use preinstrumented images";
  echo " -p - ports for k8s services: all/min. default = min - defines whether all k8s should get an external IP. Min - only ingest";
  echo " -f - flavor: alpine/ubuntu. default = ubuntu";
  echo " -n - namespace. default bookstore";
  echo;
  echo "Please supply at least one flag";
  echo;
  exit 0;
}

reset_settings() {
  echo Resetting the config...
  # reset flavor
  # reset settings
  sed -i.bak "s/:latest/:{FLAVOR}/g" *.yaml
  sed -i.bak "s/:noble/:{FLAVOR}/g" *.yaml
  sed -i.bak "s/:alpine/:{FLAVOR}/g" *.yaml
  # databases can have only latest tag
  sed -i.bak "s/dt-postgres:{FLAVOR}/dt-postgres:latest/g" databases.yaml
  sed -i.bak "s/dt-mysql:{FLAVOR}/dt-mysql:latest/g" databases.yaml
  # sidecar containers must always be alpine:latest
  sed -i.bak "s/image: alpine:{FLAVOR}/image: alpine:latest/g" ingest.yaml
  sed -i.bak "s/image: alpine:{FLAVOR}/image: alpine:latest/g" web.yaml

  # reset settings
  sed -i.bak "s/-noagent:{FLAVOR}/-{AGENT}:{FLAVOR}/g" *.yaml
  sed -i.bak "s/-agents:{FLAVOR}/-{AGENT}:{FLAVOR}/g" *.yaml
  sed -i.bak "s/-preinstrument:{FLAVOR}/-{AGENT}:{FLAVOR}/g" *.yaml
#  sed -i.bak "s/bookstore-webapp:latest/bookstore-webapp:latest/g" bookstore.yaml

  # web app is available in latest flavor and noagent only
  sed -i.bak "s/web-{AGENT}:{FLAVOR}/web-noagent:latest/g" web.yaml

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
  po="min"       # default - all k8s have their public IPs
  fl="latest"    # default - latest (ubuntu for Java, Alpine for Node.js)
  ns="bookstore" # default namespace
  rs="no"        # no reset by default
  hl="no"        # help
  while getopts hg:a:x:p:f:n:r flag
  do
      case "${flag}" in
        h) hl="yes";;
        g) ag=${OPTARG};;
        p) po=${OPTARG};;
        f) fl=${OPTARG};;
        n) ns=${OPTARG};;
        r) rs="yes";;
        *) echo unsupported flag ${flag}
      esac
  done

  # fixing most common typos
  if [ $ag != "yes" ] && [ $ag != "y" ] && [ $ag != "pre" ] && [ $ag != "p" ]; then ag="no";    fi
  if [ $po != "min" ];                                                         then po="all";   fi
  if [ $fl != "alpine" ];                                                      then fl="latest";    fi
  if [ $ns = "" ];                                                             then ns="bookstore"; fi

  if [ $hl = "yes" ]; then # user wanted help. ignoring everything else
    display_usage;
  elif [ $rs = "yes" ]; then # resetting. ignoring everything else
    reset_settings;
    exit 0;
  else # doing the job. show configuration first
    echo Parameters configured:
    echo " agents:    " $ag;
    echo " ports:     " $po;
    echo " flavor:    " $fl;
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

sed -i.bak "s/-{AGENT}:{FLAVOR}/-$ag:$fl/g" *.yaml

# set namespace
sed -i.bak -E "/name:.*$/ s/name:.*$/name: ${ns}/g" namespace.yaml
sed -i.bak -E "/namespace:.*$/ s/namespace:.*$/namespace: ${ns}/g" *.yaml

# set ClusterIP and comment nodePorts
if [ $po = "min" ]; then
    sed -i.bak "s/type: LoadBalancer # ClusterIP/type: ClusterIP # LoadBalancer/g" *.yaml
    sed -i.bak -E "/^      nodePort: / s/^#*/#/g" *.yaml
fi

rm *.bak
echo "Preparation done: $ag"
