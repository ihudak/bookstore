#!/bin/bash
# if the script gets a param, it will be considered as tenant-token
# can be either open of base64-ed

export DT_JAVA_AGENT=agents
export DT_PRE_AGENT=preinstrument

if [ $# -lt 1 ] || [ "$1" != $DT_PRE_AGENT ]; then
  export IMG_NAME=$DT_JAVA_AGENT;
  export PRELOAD=false;
else
  export IMG_NAME=$DT_PRE_AGENT;
  export PRELOAD=true;
fi

if [ $# -lt 5 ]; then
  docker image build --platform=linux/amd64 -t ghcr.io/ihudak/java-$IMG_NAME-x64:latest \
      --build-arg AGENTS_PRELOAD=$PRELOAD \
      --build-arg PLATFORM=x64 \
      .;
  docker image build --platform=linux/arm64 -t ghcr.io/ihudak/java-$IMG_NAME-arm64:latest \
      --build-arg AGENTS_PRELOAD=$PRELOAD \
      --build-arg PLATFORM=arm64 \
      .;
else
  docker image build --platform=linux/amd64 -t ghcr.io/ihudak/java-$IMG_NAME-x64:latest \
      --build-arg AGENTS_PRELOAD=$PRELOAD \
      --build-arg PLATFORM=x64 \
      --build-arg DT_ENV_ID="$2" \
      --build-arg DT_TOKEN="$3" \
      --build-arg DT_ENV_URL="$4" \
      .;
  docker image build --platform=linux/arm64 -t ghcr.io/ihudak/java-$IMG_NAME-arm64:latest \
      --build-arg AGENTS_PRELOAD=$PRELOAD \
      --build-arg PLATFORM=arm64 \
      --build-arg DT_ENV_ID="$2" \
      --build-arg DT_TOKEN="$3" \
      --build-arg DT_ENV_URL="$4" \
      .;
fi

docker push ghcr.io/ihudak/java-$IMG_NAME-x64:latest
docker push ghcr.io/ihudak/java-$IMG_NAME-arm64:latest

# ./push_dicker.sh preinstrument pae32231 dev <token>
# ./push_dicker.sh agents pae32231 dev <token>
