#!/bin/bash

if [ $# -lt 4 ]; then
  docker image build --platform=linux/amd64 -t ghcr.io/ihudak/node-angular-agents-x64:latest --build-arg AGENTS_PRELOAD=false .;
  docker image build --platform=linux/arm64 -t ghcr.io/ihudak/node-angular-agents-arm64:latest --build-arg AGENTS_PRELOAD=false .;

  docker image build --platform=linux/amd64 -t ghcr.io/ihudak/node-angular-preinstrument-x64:latest --build-arg AGENTS_PRELOAD=true .;
  docker image build --platform=linux/arm64 -t ghcr.io/ihudak/node-angular-preinstrument-arm64:latest --build-arg AGENTS_PRELOAD=true .;
else
  docker image build --platform=linux/amd64 -t ghcr.io/ihudak/node-angular-agents-x64:latest \
      --build-arg AGENTS_PRELOAD=false \
      --build-arg DT_ENV_ID="$1" \
      --build-arg DT_TOKEN="$2" \
      --build-arg DT_ENV_URL="$3" \
      .;
  docker image build --platform=linux/arm64 -t ghcr.io/ihudak/node-angular-agents-arm64:latest \
      --build-arg AGENTS_PRELOAD=false \
      --build-arg DT_ENV_ID="$1" \
      --build-arg DT_TOKEN="$2" \
      --build-arg DT_ENV_URL="$3" \
      .;

  docker image build --platform=linux/amd64 -t ghcr.io/ihudak/node-angular-preinstrument-x64:latest \
      --build-arg AGENTS_PRELOAD=true \
      --build-arg DT_ENV_ID="$1" \
      --build-arg DT_TOKEN="$2" \
      --build-arg DT_ENV_URL="$3" \
      .;
  docker image build --platform=linux/arm64 -t ghcr.io/ihudak/node-angular-preinstrument-arm64:latest \
      --build-arg AGENTS_PRELOAD=true \
      --build-arg DT_ENV_ID="$1" \
      --build-arg DT_TOKEN="$2" \
      --build-arg DT_ENV_URL="$3" \
      .;
fi

docker push ghcr.io/ihudak/node-angular-agents-x64:latest
docker push ghcr.io/ihudak/node-angular-agents-arm64:latest
docker push ghcr.io/ihudak/node-angular-preinstrument-x64:latest
docker push ghcr.io/ihudak/node-angular-preinstrument-arm64:latest
