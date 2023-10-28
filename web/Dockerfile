# choose base image to build off of
FROM node:lts-alpine
MAINTAINER dynatrace.com

ARG OTEL_SERVICE_NAME=BookStoreDeploy
ARG TENANT_ID_SHELL
ARG TENANT_LAYER_SHELL=dev
ARG TENANT_TOKEN_SHELL
ENV TENANT_ID_SHELL=$TENANT_ID_SHELL
ENV TENANT_URL_SHELL="https://$TENANT_ID_SHELL.$TENANT_LAYER_SHELL.dynatracelabs.com"
ENV OTEL_TOKEN_SHELL=$TENANT_TOKEN_SHELL

ENV DOCKER_VER=1.0.0

# set bash instrumentation
COPY ../agents/scripts/set_bash_instrumentation.sh /opt/
RUN chmod +x /opt/*.sh
# install unzip and otel bash agent
RUN apt-get update && \
    apt-get install -y vim jq && \
    wget -O - https://raw.githubusercontent.com/plengauer/opentelemetry-bash/main/INSTALL.sh | sh -E && \
    . /opt/set_bash_instrumentation.sh && \
    rm -rf /var/lib/apt/lists/*

# set the current working directory for all commands
WORKDIR /usr/src/app

ARG DT_ACTIVE_WEB_ENV
ENV DT_ACTIVE_WEB_ENV=$DT_ACTIVE_WEB_ENV

# copy these over first and run 'npm install' so the node_modules will be cached
# until the package.json / lock changes
COPY package.json .
COPY package-lock.json .
RUN npm install

# copy over all code files
COPY . .
RUN ng build

# expose internal docker container port to external environment
EXPOSE 4200

# specify default command to run when we run the image
ENTRYPOINT /usr/src/app/node_modules/.bin/ng serve --host 0.0.0.0 --disable-host-check -c ${DT_ACTIVE_WEB_ENV}
