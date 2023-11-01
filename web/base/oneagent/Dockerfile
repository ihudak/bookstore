FROM ubuntu:jammy
MAINTAINER dynatrace.com

ARG AGENTS_PRELOAD=false
ENV AGENTS_PRELOAD=$AGENTS_PRELOAD

ARG NODE_MAJOR=20

ARG OTEL_SERVICE_NAME=BookStoreDeploy
ARG TENANT_ID_SHELL
ARG TENANT_LAYER_SHELL=dev
ARG TENANT_TOKEN_SHELL
ENV TENANT_ID_SHELL=$TENANT_ID_SHELL
ENV TENANT_URL_SHELL="https://$TENANT_ID_SHELL.$TENANT_LAYER_SHELL.dynatracelabs.com"
ENV OTEL_TOKEN_SHELL=$TENANT_TOKEN_SHELL

ENV DOCKER_VER=1.1.0

COPY scripts/*.sh /opt/
RUN chmod +x /opt/*.sh

# set bash instrumentation
# install otel bash agent
RUN apt-get update && \
    apt-get install -y zip unzip vim jq ca-certificates curl gnupg curl wget curl && \
    wget -O - https://raw.githubusercontent.com/plengauer/opentelemetry-bash/main/INSTALL.sh | sh -E && \
    rm -rf /var/lib/apt/lists/*

# Install Node.js
RUN apt-get update && \
    apt-get install -y ca-certificates curl gnupg curl wget && \
    mkdir -p /etc/apt/keyrings && \
    curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg && \
    echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_$NODE_MAJOR.x nodistro main" | tee /etc/apt/sources.list.d/nodesource.list && \
    apt-get update && \
    apt-get install nodejs -y && \
    npm install npm@latest -g && \
    rm -rf /var/lib/apt/lists/* && \
    npm install -g @angular/cli


# install OneAgent
ARG ENV_FILE=env
COPY ${ENV_FILE} /opt/env
RUN if [ "$AGENTS_PRELOAD" = true ]; then \
      TENANT_URL=`cat /opt/env | grep DT_AGENT_DOWNLOAD_TENANT_URL | sed s/DT_AGENT_DOWNLOAD_TENANT_URL=// | sed -E 's/[ '$'/'']+$//'` ; \
      export TENANT_URL ; \
      OA_TOKEN=`cat /opt/env | grep DT_AGENT_DOWNLOAD_TOKEN | sed s/DT_AGENT_DOWNLOAD_TOKEN=// | base64 -d` ; \
      export OA_TOKEN ; \
      . /opt/set_bash_instrumentation.sh ; \
      /opt/get_oa.sh ; \
    fi && \
    rm /opt/env

# set the current working directory for all commands
WORKDIR /usr/src/app


# expose internal docker container port to external environment
EXPOSE 4200

# specify default command to run when we run the image
ENTRYPOINT . /opt/set_bash_instrumentation.sh && \
           if [ "$AGENTS_PRELOAD" = false ]; then /opt/get_oa.sh; fi && \
           if [ -z ${BOOKSTORE_BASE_SRV_URL+x} ] || [ "$DT_ACTIVE_WEB_ENV" != custom ] ; then \
               /usr/src/app/node_modules/.bin/ng serve --host 0.0.0.0 --disable-host-check -c ${DT_ACTIVE_WEB_ENV}; \
           else \
               sed -i "s|http://kubernetes.docker.internal/api|$BOOKSTORE_BASE_SRV_URL|g" ./src/environments/* ; \
               sed -i "s|http://localhost|$BOOKSTORE_BASE_SRV_URL|g" ./src/environments/* ; \
               /usr/src/app/node_modules/.bin/ng build ; \
               export DT_ACTIVE_WEB_ENV=production; \
               /usr/src/app/node_modules/.bin/ng serve --host 0.0.0.0 --disable-host-check -c ${DT_ACTIVE_WEB_ENV}; \
           fi
