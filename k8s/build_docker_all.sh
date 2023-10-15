#!/bin/sh
# if the script gets a param, it will be considered as tenant-token
# can be either open of base64-ed
clear
echo "=============================================="

SCRIPT_DIR="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
JAR_FILE=build/libs/*0.0.1-SNAPSHOT.jar

DT_JAVA_AGENT=agents
DT_NO_AGENT=noagent
DT_GUI=web

dt_projects="clients books carts storage orders ratings payments dynapay ingest"

cd $SCRIPT_DIR/../$DT_JAVA_AGENT
echo "============ Building Agents ================="
if [ $# -lt 2 ]; then
  ./push_docker.sh;
else
  ./push_docker.sh $1;
fi

cd $SCRIPT_DIR/../$DT_NO_AGENT
echo "============ Building NoAgent ================="
./push_docker.sh
cd $SCRIPT_DIR/../$DT_GUI
echo "============ Building GUI ================="
./push_docker.sh

echo "============ Building Projects ================="
cd $SCRIPT_DIR/..
./gradlew clean build -x test

for i in $dt_projects; do
  PROJ_DIR=$i

  cd $SCRIPT_DIR/../$PROJ_DIR
  $SCRIPT_DIR/push_docker.sh $i
  $SCRIPT_DIR/push_docker.sh $i -gyes
  $SCRIPT_DIR/push_docker.sh $i -arm
  $SCRIPT_DIR/push_docker.sh $i -gyes -arm
done
