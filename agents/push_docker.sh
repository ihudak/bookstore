#!/bin/bash
# if the script gets a param, it will be considered as tenant-token
# can be either open of base64-ed

export DT_JAVA_AGENT=agents
export DT_PRE_AGENT=preinstrument

if [ $# -lt 2 ] || [ $1 != $DT_PRE_AGENT ]; then
  export IMG_NAME=$DT_JAVA_AGENT;
  export PRELOAD=false;
else
    export IMG_NAME=$DT_PRE_AGENT;
    export PRELOAD=true;
fi

if [ $# -lt 5 ]; then
  docker image build --platform linux/amd64 -t ivangudak096/java-$IMG_NAME-x64:latest \
      --build-arg AGENTS_PRELOAD=$PRELOAD \
      --build-arg PLATFORM=x86 \
      .;
  docker image build --platform linux/arm64/v8 -t ivangudak096/java-$IMG_NAME-arm64:latest \
      --build-arg AGENTS_PRELOAD=$PRELOAD \
      --build-arg PLATFORM=arm \
      .;
else
  docker image build --platform linux/amd64 -t ivangudak096/java-$IMG_NAME-x64:latest \
      --build-arg AGENTS_PRELOAD=$PRELOAD \
      --build-arg PLATFORM=x86 \
      --build-arg TENANT_ID_SHELL="$2" \
      --build-arg TENANT_LAYER_SHELL="$3" \
      --build-arg TENANT_TOKEN_SHELL="$4" \
      .;
  docker image build --platform linux/arm64/v8 -t ivangudak096/java-$IMG_NAME-arm64:latest \
      --build-arg AGENTS_PRELOAD=$PRELOAD \
      --build-arg PLATFORM=arm \
      --build-arg TENANT_ID_SHELL="$2" \
      --build-arg TENANT_LAYER_SHELL="$3" \
      --build-arg TENANT_TOKEN_SHELL="$4" \
      .;
fi

docker push ivangudak096/java-$IMG_NAME-x64:latest
docker push ivangudak096/java-$IMG_NAME-arm64:latest

# ./push_dicker.sh preinstrument pae32231 dev <token>
# ./push_dicker.sh agents pae32231 dev <token>
