@ECHO OFF
REM if the script gets a param, it will be considered as tenant-token
REM can be either open of base64-ed

SET DT_JAVA_AGENT=agents
SET DT_PRE_AGENT=preinstrument

IF %1==%DT_PRE_AGENT% (
  SET IMG_NAME=%DT_PRE_AGENT%
  SET PRELOAD=true
) ELSE (
  SET IMG_NAME=%DT_JAVA_AGENT%
  SET PRELOAD=false
)

SET BASH_INSTRUMENT=false
IF NOT [%4]==[] (
  SET BASH_INSTRUMENT=true
)

IF %BASH_INSTRUMENT%==false (
    docker image build --platform linux/amd64 -t ghcr.io/ihudak/java-%IMG_NAME%-x64:latest ^
        --build-arg PLATFORM=x64 ^
        --build-arg AGENTS_PRELOAD=%PRELOAD% ^
        .
    docker image build --platform linux/arm64/v8 -t ghcr.io/ihudak/java-%IMG_NAME%-arm64:latest ^
        --build-arg PLATFORM=arm ^
        --build-arg AGENTS_PRELOAD=%PRELOAD% ^
        .
) ELSE (
    docker image build --platform linux/amd64 -t ghcr.io/ihudak/java-%IMG_NAME%-x64:latest ^
        --build-arg AGENTS_PRELOAD=%PRELOAD% ^
        --build-arg PLATFORM=x64 ^
        --build-arg TENANT_ID_SHELL=%2 ^
        --build-arg TENANT_LAYER_SHELL=%3 ^
        --build-arg TENANT_TOKEN_SHELL=%4 ^
        .
    docker image build --platform linux/arm64/v8 -t ghcr.io/ihudak/java-%IMG_NAME%-arm64:latest ^
        --build-arg AGENTS_PRELOAD=%PRELOAD% ^
        --build-arg PLATFORM=arm ^
        --build-arg TENANT_ID_SHELL=%2 ^
        --build-arg TENANT_LAYER_SHELL=%3 ^
        --build-arg TENANT_TOKEN_SHELL=%4 ^
        .
)

docker push ghcr.io/ihudak/java-%IMG_NAME%-x64:latest
docker push ghcr.io/ihudak/java-%IMG_NAME%-arm64:latest


REM push_docker.bat preinstrument pae32231 dev <token>
REM push_docker.bat agents pae32231 dev <token>
