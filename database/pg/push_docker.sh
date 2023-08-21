#!/bin/bash

docker image build --platform linux/amd64 -t ihudak/dt-postgres:latest .
docker push ihudak/dt-postgres:latest

docker image build --platform linux/amd64 -t ihudak/dt-postgres-x64:latest .
docker push ihudak/dt-postgres-x64:latest

docker image build --platform linux/arm64 -t ihudak/dt-postgres-arm64:latest .
docker push ihudak/dt-postgres-arm64:latest

