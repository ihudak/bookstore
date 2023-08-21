#!/bin/bash

docker image build --platform linux/amd64 -t ihudak/dt-mysql:latest .
docker push ihudak/dt-mysql:latest

docker image build --platform linux/amd64 -t ihudak/dt-mysql-x64:latest .
docker push ihudak/dt-mysql-x64:latest

docker image build --platform linux/arm64 -t ihudak/dt-mysql-arm64:latest .
docker push ihudak/dt-mysql-arm64:latest

