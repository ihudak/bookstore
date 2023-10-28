#!/bin/bash

docker image build --platform linux/amd64 -t ivangudak096/dt-mysql:latest .
docker push ivangudak096/dt-mysql:latest

docker image build --platform linux/amd64 -t ivangudak096/dt-mysql-x64:latest .
docker push ivangudak096/dt-mysql-x64:latest

docker image build --platform linux/arm64 -t ivangudak096/dt-mysql-arm64:latest .
docker push ivangudak096/dt-mysql-arm64:latest

