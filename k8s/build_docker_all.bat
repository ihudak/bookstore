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
@REM SET DT_PROJECTS[9]=web

ECHO ============ Building Agents =================
cd %BATCH_DIR%\..\%DT_JAVA_AGENT%
IF [%3]==[] ( CALL push_docker.bat agents ) ELSE ( CALL push_docker.bat agents %1 %2 %3 )

ECHO ============ Building PreAgent ================
cd %BATCH_DIR%\..\%DT_JAVA_AGENT%
IF [%3]==[] ( CALL push_docker.bat preinstrument ) ELSE ( CALL push_docker.bat preinstrument %1 %2 %3 )

ECHO ============ Building NoAgent ================
cd %BATCH_DIR%\..\%DT_NO_AGENT%
CALL push_docker.bat

ECHO =========== Building Angular =================
cd %BATCH_DIR%\..\%DT_GUI%
CALL npm install
CALL ng build

ECHO ========= Building GUI NoAgent ===============
cd %BATCH_DIR%\..\%DT_GUI%\base\noagent
CALL push_docker.bat

REM ECHO ========= Building GUI OneAgent ===============
REM cd %BATCH_DIR%\..\%DT_GUI%\base\oneagent
REM IF [%3]==[] ( CALL push_docker.bat ) ELSE ( CALL push_docker.bat %1 %2 %3 )

ECHO ============= Building Java Projects ===================
cd %BATCH_DIR%\..
CALL .\gradlew.bat clean build

setlocal ENABLEDELAYEDEXPANSION
SET "x=0"
:SymLoop
if defined DT_PROJECTS[%x%] (
    SET PROJ=!!DT_PROJECTS[%x%]!!
    SET PROJ_DIR=..\!PROJ!

    CD %BATCH_DIR%\!PROJ_DIR!
    CALL %BATCH_DIR%\push_docker.bat !PROJ! -noagent
    CALL %BATCH_DIR%\push_docker.bat !PROJ! -agents
    CALL %BATCH_DIR%\push_docker.bat !PROJ! -preinstrument
    CALL %BATCH_DIR%\push_docker.bat !PROJ! -noagent -arm
    CALL %BATCH_DIR%\push_docker.bat !PROJ! -agents -arm
    CALL %BATCH_DIR%\push_docker.bat !PROJ! -preinstrument -arm
    SET /a "x+=1"
    GOTO :SymLoop
)
