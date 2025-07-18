@ECHO OFF
REM do not build this yet

REM if the script gets a param, it will be considered as tenant-token
REM can be either open of base64-ed

SET BASH_INSTRUMENT=false
IF NOT [%3]==[] (
  SET BASH_INSTRUMENT=true
)

IF %BASH_INSTRUMENT%==false (
  docker image build --platform=linux/amd64 -t ghcr.io/ihudak/node-angular-agents-x64:latest --build-arg AGENTS_PRELOAD=false .
  docker image build --platform=linux/arm64 -t ghcr.io/ihudak/node-angular-agents-arm64:latest --build-arg AGENTS_PRELOAD=false .

  docker image build --platform=linux/amd64 -t ghcr.io/ihudak/node-angular-preinstrument-x64:latest --build-arg AGENTS_PRELOAD=true .
  docker image build --platform=linux/arm64 -t ghcr.io/ihudak/node-angular-preinstrument-arm64:latest --build-arg AGENTS_PRELOAD=true .
) ELSE (
  docker image build --platform=linux/amd64 -t ghcr.io/ihudak/node-angular-agents-x64:latest ^
      --build-arg AGENTS_PRELOAD=false ^
      --build-arg DT_ENV_ID=%1 ^
      --build-arg DT_ENV_URL=%2 ^
      --build-arg DT_TOKEN=%3 ^
      .
  docker image build --platform=linux/arm64 -t ghcr.io/ihudak/node-angular-agents-arm64:latest ^
      --build-arg AGENTS_PRELOAD=false ^
      --build-arg DT_ENV_ID=%1 ^
      --build-arg DT_ENV_URL=%2 ^
      --build-arg DT_TOKEN=%3 ^
      .

  docker image build --platform=linux/amd64 -t ghcr.io/ihudak/node-angular-preinstrument-x64:latest ^
      --build-arg AGENTS_PRELOAD=true ^
      --build-arg DT_ENV_ID=%1 ^
      --build-arg DT_ENV_URL=%2 ^
      --build-arg DT_TOKEN=%3 ^
      .
  docker image build --platform=linux/arm64 -t ghcr.io/ihudak/node-angular-preinstrument-arm64:latest ^
      --build-arg AGENTS_PRELOAD=true ^
      --build-arg DT_ENV_ID=%1 ^
      --build-arg DT_ENV_URL=%2 ^
      --build-arg DT_TOKEN=%3 ^
      .
)

docker push ghcr.io/ihudak/node-angular-agents-x64:latest
docker push ghcr.io/ihudak/node-angular-agents-arm64:latest
docker push ghcr.io/ihudak/node-angular-preinstrument-x64:latest
docker push ghcr.io/ihudak/node-angular-preinstrument-arm64:latest
