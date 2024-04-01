npm install
ng build

docker image build --platform linux/amd64 -t ghcr.io/ihudak/web-agents-x64:latest --build-arg AGENT=agents .
docker image build --platform linux/arm64/v8 -t ghcr.io/ihudak/web-agents-arm64:latest --build-arg AGENT=agents .

docker image build --platform linux/amd64 -t ghcr.io/ihudak/web-preinstrument-x64:latest --build-arg AGENT=preinstrument .
docker image build --platform linux/arm64/v8 -t ghcr.io/ihudak/web-preinstrument-arm64:latest --build-arg AGENT=preinstrument .


docker push ghcr.io/ihudak/web-agents-x64:latest
docker push ghcr.io/ihudak/bookstore-webapp-agents-arm64:latest

docker push ghcr.io/ihudak/web-preinstrument-x64:latest
docker push ghcr.io/ihudak/web-preinstrument-arm64:latest
