#!/bin/bash
./gradlew clean build
docker image build --platform linux/amd64 -t ivangudak096/dt-orders-service:latest .
docker push ivangudak096/dt-orders-service:latest

#docker image build --platform linux/arm64 -t ivangudak096/dt-orders-service:arm64 .
#docker push ivangudak096/dt-orders-service:arm64
