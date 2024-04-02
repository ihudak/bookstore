ARG BASE_REPO=ghcr.io/ihudak
ARG AGENT=agents
ARG BASE_IMG_TAG=latest
#FROM ${BASE_REPO}/node-angular-${AGENT}:${BASE_IMG_TAG}
FROM ${BASE_REPO}/node-angular-noagent:${BASE_IMG_TAG}
MAINTAINER dynatrace.com

# set the current working directory for all commands
WORKDIR /usr/src/app

# copy over all code files
COPY . .
RUN npm install && /usr/src/app/node_modules/.bin/ng build
