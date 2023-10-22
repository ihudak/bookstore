@ECHO OFF
REM if the script gets a param, it will be considered as tenant-token
REM can be either open of base64-ed
CLS
ECHO.
ECHO ==============================================

SET BATCH_DIR=%~dp0
SET JAR_FILE=build\libs\*0.0.1-SNAPSHOT.jar

SET DT_JAVA_AGENT=agents
SET DT_NO_AGENT=noagent
SET DT_PRE_AGENT=preinstrument
SET DT_GUI=web

SET DT_PROJECTS[0]=clients
SET DT_PROJECTS[1]=books
SET DT_PROJECTS[2]=carts
SET DT_PROJECTS[3]=storage
SET DT_PROJECTS[4]=orders
SET DT_PROJECTS[5]=ratings
SET DT_PROJECTS[6]=payments
SET DT_PROJECTS[7]=dynapay
SET DT_PROJECTS[8]=ingest

cd %BATCH_DIR%\..\%DT_JAVA_AGENT%
ECHO ============ Building Agents =================
IF [%4]==[] (
  CALL push_docker.bat agents
) ELSE (
  CALL push_docker.bat agents %1 %2 %3
)
timeout 3
ECHO ============ Building PreAgent ================
IF [%4]==[] (
  CALL push_docker.bat preinstrument
) ELSE (
  CALL push_docker.bat preinstrument %1 %2 %3
)
timeout 3
cd %BATCH_DIR%\..\%DT_NO_AGENT%
ECHO ============ Building NoAgent ================
CALL push_docker.bat
timeout 3
cd %BATCH_DIR%\..\%DT_GUI%
ECHO ============= Building GUI ===================
CALL push_docker.bat
timeout 3

ECHO ============= Building Java Projects ===================
cd %BATCH_DIR%\..
CALL .\gradlew.bat clean build -x test
timeout 3

setlocal ENABLEDELAYEDEXPANSION
SET "x=0"
:SymLoop
if defined DT_PROJECTS[%x%] (
    SET PROJ=!!DT_PROJECTS[%x%]!!
    SET PROJ_DIR=..\!PROJ!

    CD %BATCH_DIR%\!PROJ_DIR!
    CALL %BATCH_DIR%\push_docker.bat !PROJ! -noagent
    timeout 3
    CALL %BATCH_DIR%\push_docker.bat !PROJ! -agents
    timeout 3
    CALL %BATCH_DIR%\push_docker.bat !PROJ! -preinstrument
    timeout 3
    CALL %BATCH_DIR%\push_docker.bat !PROJ! -noagent -arm
    timeout 3
    CALL %BATCH_DIR%\push_docker.bat !PROJ! -agents -arm
    timeout 3
    CALL %BATCH_DIR%\push_docker.bat !PROJ! -preinstrument -arm
    timeout 3
    SET /a "x+=1"
    GOTO :SymLoop
)
