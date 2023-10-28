npm install
ng build

REM docker image build --platform linux/amd64 -t ivangudak096/bookstore-webapp-x64:latest .
REM docker image build --platform linux/arm64 -t ivangudak096/bookstore-webapp-arm64:latest .

SET BASH_INSTRUMENT=false
IF NOT [%3]==[] (
  SET BASH_INSTRUMENT=true
)

IF %BASH_INSTRUMENT%==false (
    docker image build --platform linux/amd64 -t ivangudak096/bookstore-webapp-x64:latest .
    docker image build --platform linux/arm64/v8 -t ivangudak096/bookstore-webapp-arm64:latest .
) ELSE (
    docker image build --platform linux/amd64 -t ivangudak096/bookstore-webapp-x64:latest ^
        --build-arg TENANT_ID_SHELL=%1 ^
        --build-arg TENANT_LAYER_SHELL=%2 ^
        --build-arg TENANT_TOKEN_SHELL=%3 ^
        .
    docker image build --platform linux/arm64/v8 -t ivangudak096/bookstore-webapp-arm64:latest ^
        --build-arg TENANT_ID_SHELL=%1 ^
        --build-arg TENANT_LAYER_SHELL=%2 ^
        --build-arg TENANT_TOKEN_SHELL=%3 ^
        .
)

docker push ivangudak096/bookstore-webapp-x64:latest
docker push ivangudak096/bookstore-webapp-arm64:latest
