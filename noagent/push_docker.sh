#!/bin/bash
docker image build --platform linux/amd64 -t ivangudak096/java-noagent-x64:latest .
docker push ivangudak096/java-noagent-x64:latest

docker image build --platform linux/arm64/v8 -t ivangudak096/java-noagent-arm64:latest .
docker push ivangudak096/java-noagent-arm64:latest
