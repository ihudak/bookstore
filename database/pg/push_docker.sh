#!/bin/bash

docker image build --platform linux/amd64 -t ivangudak096/dt-postgres:latest .
docker push ivangudak096/dt-postgres:latest

docker image build --platform linux/amd64 -t ivangudak096/dt-postgres-x64:latest .
docker push ivangudak096/dt-postgres-x64:latest

docker image build --platform linux/arm64 -t ivangudak096/dt-postgres-arm64:latest .
docker push ivangudak096/dt-postgres-arm64:latest

