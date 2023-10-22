#!/bin/bash
# if the script gets a param, it will be considered as tenant-token
# can be either open of base64-ed
if [ $# -lt 2 ]; then
  docker image build --platform linux/amd64 -t ivangudak096/java-preinstrument-x64:latest --build-arg PLATFORM=x86 .
  docker image build --platform linux/arm64/v8 -t ivangudak096/java-preinstrument-arm64:latest --build-arg PLATFORM=arm .
else
  docker image build --platform linux/amd64 -t ivangudak096/java-preinstrument-x64:latest \
      --build-arg PLATFORM=x86 \
      --build-arg TENANT_ID=pae32231 \
      --build-arg TENANT_LAYER=dev \
      --build-arg TENANT_TOKEN=$1 \
      .;
  docker image build --platform linux/arm64/v8 -t ivangudak096/java-preinstrument-arm64:latest \
      --build-arg PLATFORM=arm \
      --build-arg TENANT_ID=pae32231 \
      --build-arg TENANT_LAYER=dev \
      --build-arg TENANT_TOKEN=$1 \
      .;
fi

docker push ivangudak096/java-preinstrument-x64:latest
docker push ivangudak096/java-preinstrument-arm64:latest
