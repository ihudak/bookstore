#!/bin/bash
export IMAGE_NAME=bookstore-ai-container
export SSH_SCOPE_DIR=/home/ihudak/.ssh/bookstore
#export EXTRA_MOUNTS="/home/ihudak/dev/docs/activegate:ro /home/ihudak/dev/docs/ai-agent-knowledge-base:ro /home/ihudak/dev/docs/barista-fonts:ro /home/ihudak/dev/docs/bigtest-framework-shared:ro /home/ihudak/dev/docs/cloud-control:ro /home/ihudak/dev/docs/cluster:ro /home/ihudak/dev/docs/cluster-foundation:ro /home/ihudak/dev/docs/cluster-openapi-docs:ro /home/ihudak/dev/docs/clusterworkloadsimulator:ro /home/ihudak/dev/docs/mission-control:ro /mnt/c/workspaces/obsidian_vault:ro"
export EXTRA_MOUNTS="/home/ihudak/dev/dtmgd /home/ihudak/dev/docs/cluster:ro /home/ihudak/dev/docs/dynatrace-docs:ro"
./runme.sh build
#./runme.sh restricted ..
./runme.sh discovery ..
