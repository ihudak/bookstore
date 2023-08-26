#!/bin/sh
######## Project Configuration ##########
BASE_REPO=ivangudak096
TAG=latest
######## Project Configuration ##########

display_usage() {
  echo "Bad/no argument(s) supplied";
      echo "Usage:";
      echo "   ./push_docker.sh <project> -agents  -arm|-x64  # makes docker image with OA and OTel agents";
      echo "   ./push_docker.sh <project> -noagent -arm|-x64  # makes docker image with no agents embedded";
      echo;
      echo "Please supply at least either -agents or -noagent";
      echo "      optionally specify platform as -arm or -x64";
	  echo;
      exit 1;
}

if [ $# -lt 2 ]; then
  display_usage;
elif [ $2 != "-agents" ] && [ $2 != "-noagent" ]; then
  display_usage;
fi

if [ $2 = "-agents" ]; then AGENT=agents; else AGENT=noagent; fi

if [ $# -gt 2 ] && [ $3 = "-arm" ]; then
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
