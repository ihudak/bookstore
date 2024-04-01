@ECHO OFF
REM do not build this yet

REM if the script gets a param, it will be considered as tenant-token
REM can be either open of base64-ed

SET BASH_INSTRUMENT=false
IF NOT [%3]==[] (
  SET BASH_INSTRUMENT=true
)

IF %BASH_INSTRUMENT%==false (
  docker image build --platform linux/amd64 -t ghcr.io/ihudak/node-angular-agents-x64:latest --build-arg AGENTS_PRELOAD=false .
  docker image build --platform linux/arm64/v8 -t ghcr.io/ihudak/node-angular-agents-arm64:latest --build-arg AGENTS_PRELOAD=false .

  docker image build --platform linux/amd64 -t ghcr.io/ihudak/node-angular-preinstrument-x64:latest --build-arg AGENTS_PRELOAD=true .
  docker image build --platform linux/arm64/v8 -t ghcr.io/ihudak/node-angular-preinstrument-arm64:latest --build-arg AGENTS_PRELOAD=true .
) ELSE (
  docker image build --platform linux/amd64 -t ghcr.io/ihudak/node-angular-agents-x64:latest ^
      --build-arg AGENTS_PRELOAD=false ^
      --build-arg TENANT_ID_SHELL=%1 ^
      --build-arg TENANT_LAYER_SHELL=%2 ^
      --build-arg TENANT_TOKEN_SHELL=%3 ^
      .
  docker image build --platform linux/arm64/v8 -t ghcr.io/ihudak/node-angular-agents-arm64:latest ^
      --build-arg AGENTS_PRELOAD=false ^
      --build-arg TENANT_ID_SHELL=%1 ^
      --build-arg TENANT_LAYER_SHELL=%2 ^
      --build-arg TENANT_TOKEN_SHELL=%3 ^
      .

  docker image build --platform linux/amd64 -t ghcr.io/ihudak/node-angular-preinstrument-x64:latest ^
      --build-arg AGENTS_PRELOAD=true ^
      --build-arg TENANT_ID_SHELL=%1 ^
      --build-arg TENANT_LAYER_SHELL=%2 ^
      --build-arg TENANT_TOKEN_SHELL=%3 ^
      .
  docker image build --platform linux/arm64/v8 -t ghcr.io/ihudak/node-angular-preinstrument-arm64:latest ^
      --build-arg AGENTS_PRELOAD=true ^
      --build-arg TENANT_ID_SHELL=%1 ^
      --build-arg TENANT_LAYER_SHELL=%2 ^
      --build-arg TENANT_TOKEN_SHELL=%3 ^
      .
)

docker push ghcr.io/ihudak/node-angular-agents-x64:latest
docker push ghcr.io/ihudak/node-angular-agents-arm64:latest
docker push ghcr.io/ihudak/node-angular-preinstrument-x64:latest
docker push ghcr.io/ihudak/node-angular-preinstrument-arm64:latest
