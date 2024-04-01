#!/bin/bash

docker image build --platform=linux/amd64 -t ghcr.io/ihudak/dt-mysql:latest .
docker push ghcr.io/ihudak/dt-mysql:latest

docker image build --platform=linux/amd64 -t ghcr.io/ihudak/dt-mysql-x64:latest .
docker push ghcr.io/ihudak/dt-mysql-x64:latest

docker image build --platform=linux/arm64 -t ghcr.io/ihudak/dt-mysql-arm64:latest .
docker push ghcr.io/ihudak/dt-mysql-arm64:latest

