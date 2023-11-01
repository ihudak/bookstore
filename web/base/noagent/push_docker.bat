@ECHO OFF

docker image build --platform linux/amd64 -t ivangudak096/node-angular-noagent-x64:latest .
docker image build --platform linux/arm64/v8 -t ivangudak096/node-angular-noagent-arm64:latest .

docker push ivangudak096/node-angular-noagent-x64:latest
docker push ivangudak096/node-angular-noagent-arm64:latest
