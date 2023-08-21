#!/bin/bash
docker image build --platform linux/amd64 -t ivangudak096/java-agents-x64:latest --build-arg PLATFORM=x86 .
docker push ivangudak096/java-agents-x64:latest

docker image build --platform linux/arm64/v8 -t ivangudak096/java-agents-arm64:latest --build-arg PLATFORM=arm .
docker push ivangudak096/java-agents-arm64:latest
