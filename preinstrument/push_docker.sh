#!/bin/bash
docker image build --platform linux/amd64 -t ivangudak096/java-preinstrument-x64:latest --build-arg PLATFORM=x86 .
docker image build --platform linux/arm64/v8 -t ivangudak096/java-preinstrument-arm64:latest --build-arg PLATFORM=arm .

docker push ivangudak096/java-preinstrument-x64:latest
docker push ivangudak096/java-preinstrument-arm64:latest
