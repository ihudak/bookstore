FROM node:lts-alpine
MAINTAINER dynatrace.com

ARG AGENTS_PRELOAD=false
ENV AGENTS_PRELOAD=$AGENTS_PRELOAD

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
#RUN apk --no-cache add curl jq && \
#    wget -O - https://raw.githubusercontent.com/plengauer/opentelemetry-bash/main/INSTALL.sh | sh -E && \
#    . /opt/set_bash_instrumentation.sh


# install OneAgent
RUN if [ "$AGENTS_PRELOAD" = true ]; then /opt/get_oa.sh; fi

# set the current working directory for all commands
WORKDIR /usr/src/app


# expose internal docker container port to external environment
EXPOSE 4200

# specify default command to run when we run the image
ENTRYPOINT if [ "$AGENTS_PRELOAD" = false ]; then /opt/get_oa.sh; fi && \
           if [ -z ${BOOKSTORE_BASE_SRV_URL+x} ] || [ "$DT_ACTIVE_WEB_ENV" != custom ] ; then \
               /usr/src/app/node_modules/.bin/ng serve --host 0.0.0.0 --disable-host-check -c ${DT_ACTIVE_WEB_ENV}; \
           else \
               sed -i "s|http://kubernetes.docker.internal/api|$BOOKSTORE_BASE_SRV_URL|g" ./src/environments/* ; \
               sed -i "s|http://localhost|$BOOKSTORE_BASE_SRV_URL|g" ./src/environments/* ; \
               /usr/src/app/node_modules/.bin/ng build ; \
               export DT_ACTIVE_WEB_ENV=production; \
               /usr/src/app/node_modules/.bin/ng serve --host 0.0.0.0 --disable-host-check -c ${DT_ACTIVE_WEB_ENV}; \
           fi
