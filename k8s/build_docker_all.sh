#!/bin/sh
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
./push_docker.sh
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
  $SCRIPT_DIR/push_docker.sh $i -noagent
  $SCRIPT_DIR/push_docker.sh $i -agents
  $SCRIPT_DIR/push_docker.sh $i -noagent -arm
  $SCRIPT_DIR/push_docker.sh $i -agents -arm
done

