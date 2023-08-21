#!/bin/bash
npm install
ng build
docker image build --platform linux/amd64 -t ivangudak096/bookstore-webapp-x64:latest .
docker push ivangudak096/bookstore-webapp-x64:latest

docker image build --platform linux/arm64 -t ivangudak096/bookstore-webapp-arm64:latest .
docker push ivangudak096/bookstore-webapp-arm64:latest
