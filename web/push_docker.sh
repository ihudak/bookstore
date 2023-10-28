#!/bin/bash
npm install
ng build

if [ $# -lt 4 ]; then
  docker image build --platform linux/amd64 -t ivangudak096/bookstore-webapp-x64:latest .;
  docker image build --platform linux/arm64/v8 -t ivangudak096/bookstore-webapp-arm64:latest .;
else
  docker image build --platform linux/amd64 -t ivangudak096/bookstore-webapp-x64:latest \
      --build-arg TENANT_ID_SHELL="$1" \
      --build-arg TENANT_LAYER_SHELL="$2" \
      --build-arg TENANT_TOKEN_SHELL="$3" \
      .;
  docker image build --platform linux/arm64/v8 -t ivangudak096/bookstore-webapp-arm64:latest \
      --build-arg TENANT_ID_SHELL="$1" \
      --build-arg TENANT_LAYER_SHELL="$2" \
      --build-arg TENANT_TOKEN_SHELL="$3" \
      .;
fi

docker push ivangudak096/bookstore-webapp-x64:latest
docker push ivangudak096/bookstore-webapp-arm64:latest
