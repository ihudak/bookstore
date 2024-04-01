docker image build --platform=linux/amd64 -t ghcr.io/ihudak/java-noagent-x64:latest .
docker push ghcr.io/ihudak/java-noagent-x64:latest

docker image build --platform=linux/arm64 -t ghcr.io/ihudak/java-noagent-arm64:latest .
docker push ghcr.io/ihudak/java-noagent-arm64:latest
