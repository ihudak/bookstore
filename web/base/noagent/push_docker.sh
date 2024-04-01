#!/bin/bash
docker image build --platform=linux/amd64 -t ghcr.io/ihudak/node-angular-noagent-x64:latest .
docker image build --platform=linux/arm64 -t ghcr.io/ihudak/node-angular-noagent-arm64:latest .

docker push ghcr.io/ihudak/node-angular-noagent-x64:latest
docker push ghcr.io/ihudak/node-angular-noagent-arm64:latest
