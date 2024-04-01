#!/bin/bash

docker image build --platform linux/amd64 -t ghcr.io/ihudak/dt-postgres:latest .
docker push ghcr.io/ihudak/dt-postgres:latest

docker image build --platform linux/amd64 -t ghcr.io/ihudak/dt-postgres-x64:latest .
docker push ghcr.io/ihudak/dt-postgres-x64:latest

docker image build --platform linux/arm64 -t ghcr.io/ihudak/dt-postgres-arm64:latest .
docker push ghcr.io/ihudak/dt-postgres-arm64:latest

