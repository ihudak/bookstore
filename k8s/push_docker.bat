@ECHO OFF
REM CLS
ECHO.

REM ######## Project Configuration ##########
SET BASE_REPO=ghcr.io/ihudak
SET TAG=latest
REM ######## Project Configuration ##########

IF "%~1"=="" GOTO UNKNOWN
IF "%~2"=="" GOTO UNKNOWN

IF "%2"=="-noagent" (
    SET AGENT=noagent
) ELSE IF "%2"=="-agents"  (
    SET AGENT=agents
) ELSE IF "%2"=="-preinstrument" (
    SET AGENT=preinstrument
) ELSE GOTO UNKNOWN

IF "%~3"=="-arm" (
    SET PLATFORM=arm64
    SET PLATFORM_FULL=arm64
) ELSE (
    SET PLATFORM=x64
    SET PLATFORM_FULL=amd64
)

SET PROJECT=%1
SET IMG_NAME=%BASE_REPO%/%PROJECT%-%AGENT%-%PLATFORM%:%TAG%

ECHO ### Building %PROJECT% -=- %PLATFORM% -=- %AGENT%...

docker image build ^
    --platform=linux/%PLATFORM_FULL% ^
    -t %IMG_NAME% ^
    --build-arg BASE_REPO=%BASE_REPO% ^
    --build-arg AGENT=%AGENT% ^
    --build-arg PLATFORM=%PLATFORM% ^
    --build-arg BASE_IMG_TAG=%TAG% ^
    .
docker push %IMG_NAME%
GOTO DONE


:UNKNOWN
ECHO Bad/No Parameters
ECHO Usage:
ECHO    push_docker.bat <project> -agents  -arm/-x86  # makes docker image with OA and OTel agents
ECHO    push_docker.bat <project> -noagent -arm/-x86  # makes docker image with no agents embedded
ECHO    push_docker.bat <project> -preinstrument -arm/-x86  # makes docker image with agents preloaded
ECHO.
ECHO Please supply at least either -agents or -noagent
ECHO       optionally specify platform as -arm or -x64
ECHO.
EXIT 1

:DONE
