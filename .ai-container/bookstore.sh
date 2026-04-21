#!/bin/bash
export IMAGE_NAME=bookstore-ai-container
export SSH_SCOPE_DIR=/home/ihudak/.ssh/bookstore
#export EXTRA_MOUNTS=""
./runme.sh build
./runme.sh restricted ..
#./runme.sh discovery ..
