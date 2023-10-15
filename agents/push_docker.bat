@ECHO OFF
REM if the script gets a param, it will be considered as tenant-token
REM can be either open of base64-ed

IF "%1"=="" (
    docker image build --platform linux/amd64 -t ivangudak096/java-agents-x64:latest --build-arg PLATFORM=x86 .
    docker image build --platform linux/arm64/v8 -t ivangudak096/java-agents-arm64:latest --build-arg PLATFORM=arm .
) ELSE (
    docker image build --platform linux/amd64 -t ivangudak096/java-agents-x64:latest ^
        --build-arg PLATFORM=x86 ^
        --build-arg TENANT_ID=pae32231 ^
        --build-arg TENANT_LAYER=dev ^
        --build-arg TENANT_TOKEN=%1 ^
        .
    docker image build --platform linux/arm64/v8 -t ivangudak096/java-agents-arm64:latest ^
        --build-arg PLATFORM=arm ^
        --build-arg TENANT_ID=pae32231 ^
        --build-arg TENANT_LAYER=dev ^
        --build-arg TENANT_TOKEN=%1 ^
        .
)

docker push ivangudak096/java-agents-x64:latest
docker push ivangudak096/java-agents-arm64:latest
