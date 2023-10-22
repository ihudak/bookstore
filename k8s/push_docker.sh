#!/bin/sh
######## Project Configuration ##########
BASE_REPO=ivangudak096
TAG=latest
######## Project Configuration ##########

display_usage() {
  echo "Usage:";
  echo "   ${0} <project> -gyes -ax64  # makes docker image with OA and OTel agents installed on deploy, x64 architecture";
  echo "   ${0} <project> -gpre -ax64  # makes docker image with OA and OTel agents preloaded, x64 architecture";
  echo "   ${0} <project> -gno  -arm   # makes docker image with no agents embedded, arm architecture";
  echo "Flags:";
  echo " -g - agent: yes/no. default = no - preloads otel and dynatrace java agents";
  echo " -a - architecture: arm/x64. default = x64 - sets architecture";
  echo;

  exit 0;
}

get_params() {
  ag="no"        # default - with agents
  ar="x64"       # default - x64
  hl="no"        # help
  xx="boo"       # placeholder for x flag (can be used to force x64 arch)
  while getopts hg:a:x: flag
  do
      case "${flag}" in
        h) hl="yes";;
        g) ag=${OPTARG};;
        a) ar=${OPTARG};;
        x) xx=${OPTARG};;
        *) echo unsupported flag ${flag}
      esac
  done

  # fixing most common typos
  if [ $ag != "yes" ] && [ $ag != "y" ] && [ $ag != "pre" ] && [ $ag != "p" ]; then ag="no";    fi
  if [ $ar = "arm" ] || [ $ar = "rm64" ] || [ $ar = "rm" ];                    then ar="arm64"; fi # cover -aarm64, -arm64, -aarm, -arm
  if [ $ar != "arm64" ];                                                       then ar="x64";   fi
  if [ $xx = "86" ] || [ $xx = "64" ];                                         then ar="x64";   fi

  if [ $hl = "yes" ]; then # user wanted help. ignoring everything else
    display_usage;
  else # doing the job. show configuration first
    echo Parameters configured:
    echo " agents:    " $ag;
    echo " arch:      " $ar;
  fi
}

# Main Script
get_params "$@";

if [ $ag = "no" ]; then
  AGENT=noagent;
elif [ $ag = "pre" ] || [ $ag = "p" ]; then
  AGENT=preinstrument;
else
  AGENT=agents;
fi

if [ $ar = "-arm64" ]; then
  PLATFORM="arm64";
  PLATFORM_FULL="arm64/v8";
else
  PLATFORM="x64";
  PLATFORM_FULL="amd64";
fi

PROJECT=$1
IMG_NAME=$BASE_REPO/$PROJECT-$AGENT-$PLATFORM:$TAG

echo "### Building "$PROJECT " -=- " $PLATFORM " -=- " $AGENT...

# ./gradlew clean build
docker image build \
  --platform linux/$PLATFORM_FULL \
  -t $IMG_NAME \
  --build-arg BASE_REPO=$BASE_REPO \
  --build-arg AGENT=$AGENT \
  --build-arg PLATFORM=$PLATFORM \
  --build-arg BASE_IMG_TAG=$TAG \
  .
docker push $IMG_NAME
