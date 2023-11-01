#!/bin/sh
# if the script gets a param, it will be considered as tenant-token
# can be either open of base64-ed
clear
echo "=============================================="

SCRIPT_DIR="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
JAR_FILE=build/libs/*0.0.1-SNAPSHOT.jar

DT_JAVA_AGENT=agents
DT_NO_AGENT=noagent
DT_PRE_AGENT=preinstrument
DT_GUI=web

dt_projects="clients books carts storage orders ratings payments dynapay ingest web"

echo "============ Building Agents ================="
cd $SCRIPT_DIR/../$DT_JAVA_AGENT
if [ $# -lt 3 ]; then ./push_docker.sh agents; else ./push_docker.sh agents "$1" "$2" "$3"; fi

echo "============ Building PreAgent ================="
cd $SCRIPT_DIR/../$DT_JAVA_AGENT
if [ $# -lt 3 ]; then ./push_docker.sh preinstrument; else ./push_docker.sh preinstrument "$1" "$2" "$3"; fi

echo "============ Building NoAgent ================="
cd $SCRIPT_DIR/../$DT_NO_AGENT
./push_docker.sh

echo "============ Building Angular ================="
cd $SCRIPT_DIR/../$DT_GUI
npm install
ng build

echo "============ Building GUI NoAgent ================="
cd $SCRIPT_DIR/../$DT_GUI/base/noagent
./push_docker.sh

cd $SCRIPT_DIR/../$DT_GUI/base/oneagent
echo "============ Building GUI OneAgent ================="
if [ $# -lt 3 ]; then ./push_docker.sh; else ./push_docker.sh "$1" "$2" "$3"; fi

echo "============ Building Projects ================="
cd $SCRIPT_DIR/..
./gradlew clean build -x test

for i in $dt_projects; do
  PROJ_DIR=$i

  cd $SCRIPT_DIR/../$PROJ_DIR
  $SCRIPT_DIR/push_docker.sh -p $i
  $SCRIPT_DIR/push_docker.sh -p $i -gyes
  $SCRIPT_DIR/push_docker.sh -p $i -gpre
  $SCRIPT_DIR/push_docker.sh -p $i -arm
  $SCRIPT_DIR/push_docker.sh -p $i -gyes -arm
  $SCRIPT_DIR/push_docker.sh -p $i -gpre -arm
done
