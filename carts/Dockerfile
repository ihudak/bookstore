ARG BASE_REPO=ghcr.io/ihudak
ARG AGENT=agents
ARG BASE_IMG_TAG=latest
FROM ${BASE_REPO}/java-${AGENT}:${BASE_IMG_TAG}
MAINTAINER dynatrace.com

ARG JAR_FILE=build/libs/*0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} /opt/app/app.jar
ENV SERVICE_FULL_NAME=BookStore-Carts
