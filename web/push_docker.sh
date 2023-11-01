#!/bin/bash
npm install
ng build

docker image build --platform linux/amd64 -t ivangudak096/web-agents-x64:latest --build-arg AGENT=agents .
docker image build --platform linux/arm64/v8 -t ivangudak096/web-agents-arm64:latest --build-arg AGENT=agents .

docker image build --platform linux/amd64 -t ivangudak096/web-preinstrument-x64:latest --build-arg AGENT=preinstrument .
docker image build --platform linux/arm64/v8 -t ivangudak096/web-preinstrument-arm64:latest --build-arg AGENT=preinstrument .


docker push ivangudak096/web-agents-x64:latest
docker push ivangudak096/bookstore-webapp-agents-arm64:latest

docker push ivangudak096/web-preinstrument-x64:latest
docker push ivangudak096/web-preinstrument-arm64:latest
